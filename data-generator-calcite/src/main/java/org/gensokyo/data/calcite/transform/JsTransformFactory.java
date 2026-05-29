/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.transform;

import org.gensokyo.data.calcite.V2TransformFactory;
import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.JsTransformVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.TransformVO;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Row-local GraalJS transform: binds {@code row} only, with sandboxed host access and timeouts.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public class JsTransformFactory implements V2TransformFactory {

    private static final String INPUT_TABLE = "input";
    private static final String ROW_BINDING = "row";
    private static final String LANGUAGE = "js";
    private static final String JS_ECMASCRIPT_VERSION_OPTION = "js.ecmascript-version";

    private static final HostAccess SANDBOX_HOST_ACCESS = HostAccess.newBuilder(HostAccess.NONE)
            .allowMapAccess(true)
            .build();

    private static final Engine ENGINE = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .allowExperimentalOptions(true)
            .build();

    /**
     * Returns whether this factory handles {@link JsTransformVO}.
     *
     * @param transform transform configuration
     * @return {@code true} for JavaScript transforms
     */
    @Override
    public boolean supports(TransformVO transform) {
        return transform instanceof JsTransformVO;
    }

    /**
     * Executes the configured script once per row in table {@code input}, mutating the bound {@code row} map.
     *
     * @param transform JavaScript transform definition
     * @param context   execution context containing table {@code input}
     * @return input schema and rows after script execution
     */
    @Override
    public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
        JsTransformVO jsTransform = (JsTransformVO) transform;
        RowSchema inputSchema = context.getSchemas().get(INPUT_TABLE);
        List<Row> inputRows = context.getData().get(INPUT_TABLE);
        if (inputSchema == null || inputRows == null) {
            throw new IllegalArgumentException("JS transform requires table '" + INPUT_TABLE + "' in execution context");
        }

        String script = jsTransform.getScript();
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("JS transform script must not be blank");
        }
        validateScriptSize(script);

        int timeoutMs = jsTransform.getTimeoutMs() != null
                ? jsTransform.getTimeoutMs()
                : JsTransformVO.DEFAULT_TIMEOUT_MS;
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("JS transform timeoutMs must be positive");
        }

        Source source;
        try {
            source = Source.newBuilder(LANGUAGE, script, "row-transform.js").build();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to build JS transform source", e);
        }
        List<Row> outputRows = new ArrayList<>(inputRows.size());
        for (Row inputRow : inputRows) {
            outputRows.add(executeForRow(source, inputRow, timeoutMs));
        }
        return new CalciteRowTransformer.TransformResult(inputSchema, outputRows);
    }

    private static Row executeForRow(Source source, Row inputRow, int timeoutMs) {
        // Copy row values so scripts mutate an isolated map per execution.
        Map<String, Object> rowValues = new LinkedHashMap<>(inputRow.values());
        try (Context jsContext = Context.newBuilder(LANGUAGE)
                .engine(ENGINE)
                .allowHostAccess(SANDBOX_HOST_ACCESS)
                .allowIO(IOAccess.NONE)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .allowHostClassLookup(className -> false)
                .option(JS_ECMASCRIPT_VERSION_OPTION, "2022")
                .build()) {
            jsContext.getBindings(LANGUAGE).putMember(ROW_BINDING, rowValues);
            runWithTimeout(jsContext, source, timeoutMs);
        } catch (PolyglotException e) {
            throw new IllegalStateException("JS transform execution failed: " + e.getMessage(), e);
        }
        return new Row(rowValues);
    }

    private static void runWithTimeout(Context jsContext, Source source, int timeoutMs) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Callable<Void> task = () -> {
                jsContext.eval(source);
                return null;
            };
            Future<Void> future = executor.submit(task);
            try {
                future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                jsContext.close(true);
                throw new IllegalStateException("JS transform timed out after " + timeoutMs + " ms", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("JS transform execution failed", cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("JS transform interrupted", e);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static void validateScriptSize(String script) {
        if (script.getBytes(StandardCharsets.UTF_8).length > JsTransformVO.MAX_SCRIPT_BYTES) {
            throw new IllegalArgumentException("JS transform script exceeds maximum size of "
                    + JsTransformVO.MAX_SCRIPT_BYTES + " bytes");
        }
    }
}

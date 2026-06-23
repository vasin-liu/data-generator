/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.udf;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executes a callable GraalJS script UDF body against an argument list with a sandboxed
 * context and per-call timeout. The script body receives an {@code args} array and must
 * {@code return} a scalar value; it reuses the same isolation profile as the JS row transform.
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
public final class GraalJsScriptUdfExecutor {

    private static final String LANGUAGE = "js";
    private static final String JS_ECMASCRIPT_VERSION_OPTION = "js.ecmascript-version";

    // Allow only list access so Java argument lists surface as JS arrays; deny all host classes.
    private static final HostAccess SANDBOX_HOST_ACCESS = HostAccess.newBuilder(HostAccess.NONE)
            .allowListAccess(true)
            .build();

    private static final Engine ENGINE = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .allowExperimentalOptions(true)
            .build();

    static {
        // Pay the one-time GraalJS language/runtime initialization (multi-second in interpreter-only
        // mode) at class load — i.e. Spring startup — so the first real, timeout-bounded UDF call does
        // not absorb cold-start latency and spuriously trip the per-call budget.
        warmUp();
    }

    /**
     * Evaluates a script UDF body once with the supplied arguments.
     *
     * @param scriptBody function body referencing {@code args} and returning a scalar
     * @param arguments  positional call arguments
     * @param timeoutMs  positive execution budget in milliseconds
     * @return the converted scalar result (may be {@code null})
     * @throws IllegalArgumentException when inputs are invalid
     * @throws IllegalStateException    when the script fails or times out
     */
    public Object execute(String scriptBody, List<Object> arguments, int timeoutMs) {
        if (scriptBody == null || scriptBody.isBlank()) {
            throw new IllegalArgumentException("Script UDF body must not be blank");
        }
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Script UDF timeoutMs must be positive");
        }
        // Wrap the body as a callable function so the script can return a value.
        String wrapped = "(function(args){\n" + scriptBody + "\n})";
        Source source;
        try {
            source = Source.newBuilder(LANGUAGE, wrapped, "script-udf.js").build();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to build script UDF source", e);
        }
        try (Context jsContext = Context.newBuilder(LANGUAGE)
                .engine(ENGINE)
                .allowHostAccess(SANDBOX_HOST_ACCESS)
                .allowIO(IOAccess.NONE)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .allowHostClassLookup(className -> false)
                .option(JS_ECMASCRIPT_VERSION_OPTION, "2022")
                .build()) {
            return runWithTimeout(jsContext, source, arguments == null ? List.of() : arguments, timeoutMs);
        } catch (PolyglotException e) {
            throw new IllegalStateException("Script UDF execution failed: " + e.getMessage(), e);
        }
    }

    private static void warmUp() {
        // Mirror the real execution path (shared engine, sandboxed context, callable wrapper) so the
        // expensive first-time initialization is triggered here rather than inside a timed call.
        try (Context jsContext = Context.newBuilder(LANGUAGE)
                .engine(ENGINE)
                .allowHostAccess(SANDBOX_HOST_ACCESS)
                .allowIO(IOAccess.NONE)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .allowHostClassLookup(className -> false)
                .option(JS_ECMASCRIPT_VERSION_OPTION, "2022")
                .build()) {
            Source source = Source.newBuilder(LANGUAGE, "(function(args){ return args.length; })", "warmup.js").build();
            jsContext.eval(source).execute(List.of());
        } catch (Throwable ignored) {
            // Warm-up is best-effort; any failure (including Errors like NoClassDefFoundError) must not
            // abort class initialization and block startup. Real calls still work, paying cold-start once.
        }
    }

    private static Object runWithTimeout(Context jsContext, Source source, List<Object> arguments, int timeoutMs) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Callable<Object> task = () -> {
                Value function = jsContext.eval(source);
                if (!function.canExecute()) {
                    throw new IllegalStateException("Script UDF body did not evaluate to a callable function");
                }
                return toJava(function.execute(arguments));
            };
            Future<Object> future = executor.submit(task);
            try {
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                jsContext.close(true);
                throw new IllegalStateException("Script UDF timed out after " + timeoutMs + " ms", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("Script UDF execution failed", cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Script UDF interrupted", e);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static Object toJava(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isNumber()) {
            // Preserve integral values as long; fall back to double otherwise.
            return value.fitsInLong() ? value.asLong() : value.asDouble();
        }
        return value.toString();
    }
}

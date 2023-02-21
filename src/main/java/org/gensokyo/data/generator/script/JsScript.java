/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.script;

import com.oracle.truffle.js.runtime.JSContextOptions;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.generator.domain.Context;
import org.gensokyo.data.generator.domain.ScriptPO;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.util.RandomKit;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

/**
 * javascript脚本执行器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/5 , Version 1.0.0
 */
@Slf4j
public class JsScript implements Script {
    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private ScriptPO script;
    private Context ctx;
    private org.graalvm.polyglot.Context jsCtx;
    private static final String LANGUAGE = "js";

    public JsScript(final ScriptPO script, final Context ctx) {
        this.script = Objects.requireNonNull(script);
        this.ctx = Objects.requireNonNull(ctx);
        System.setProperty("polyglot.js.nashorn-compat", "true");
        var engine = Engine.newBuilder()
                //允许从远程加载脚本文件
                .option("js.load-from-url", "true")
                .option("engine.WarnInterpreterOnly", "false")
                .allowExperimentalOptions(true)
                .build();
        this.jsCtx = org.graalvm.polyglot.Context.newBuilder(LANGUAGE)
                .allowAllAccess(true)
                .allowHostClassLoading(true)
                .allowIO(true)
                .engine(engine)
                .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022")
                .build();
    }

    @Override
    public Object eval(Object dataset, Object... args) {
        if (StringUtils.hasText(script.getContent())) {
            try {
                Source js = createScriptSource(script.getContent());
                Value value = jsCtx.eval(js);
                if (value.canExecute()) {
                    return value.execute(this.ctx, dataset, args).as(List.class);
                } else {
                    log.error("当前脚本无法执行：{}", script);
                    throw new DataGeneratorException("当前脚本无法执行");
                }
            } catch (Exception e) {
                log.error(String.format("执行脚本 [%s] 出现异常：", script), e);
                throw new DataGeneratorException("执行脚本出现异常", e);
            }
        }

        //脚本为空时，返回自身的数据集
        return dataset;
    }

    private Source createScriptSource(String script) throws IOException {
        Source source = null;
        //远程文件链接
        if (StringUtils.startsWithIgnoreCase(script, "http")) {
            source = Source.newBuilder(LANGUAGE, new URL(script)).build();
        }
        //服务器本地文件
        if (Objects.isNull(source) && StringUtils.endsWithIgnoreCase(script, ".js")) {
            Resource resource = resolver.getResource(script);
            if (Objects.requireNonNull(resource).exists()) {
                source = Source.newBuilder(LANGUAGE, resource.getFile()).build();
            } else {
                log.error("指定的脚本文件不存在：[{}]", script);
                throw new DataGeneratorException("指定的脚本文件不存在");
            }
        }
        //内联脚本字符串
        if (Objects.isNull(source)) {
            source = Source.newBuilder(LANGUAGE, script, RandomKit.alpha(5)).build();
        }

        return Objects.requireNonNull(source, "无法解析给定的脚本内容，请检查配置是否正确");
    }

    @Override
    public void close() {
        jsCtx.close();
        //set null
        this.resolver = null;
        this.script = null;
        this.ctx = null;
        this.jsCtx = null;
    }
}

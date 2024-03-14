/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import com.oracle.truffle.js.runtime.JSContextOptions;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.stage.ScriptStagePO;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;
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
 * JavaScript脚本处理类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
public class JsScript implements Script {
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final org.graalvm.polyglot.Context jsCtx;
    private static final String LANGUAGE = "js";

    public JsScript() {
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

    @SuppressWarnings("unchecked")
    @Override
    public org.gensokyo.data.value.Value eval(final ScriptStagePO spo,
                                              final org.gensokyo.data.value.Value dataset,
                                              Object... args) {
        if (StrKit.isNotBlank(spo.getContent())) {
            var dv = dataset.get();
            try {
                Source js = createScriptSource(spo.getContent());
                Value value = jsCtx.eval(js);
                if (value.canExecute()) {
                    var result = value.execute(dv, args).as(List.class);
                    if (CollectKit.isNotEmpty(result)) {
                        return DatasetKit.extractCollection(result);
                    }
                } else {
                    throw new DataGeneratorException(String.format("当前脚本无法执行，脚本类型：%s，脚本内容：%s，执行对象值为：%s",
                            spo.getScriptType(), spo.getContent(), dv));
                }
            } catch (Exception e) {
                throw new DataGeneratorException(String.format("执行脚本出现异常，脚本类型：%s，脚本内容：%s，执行对象值为：%s",
                        spo.getScriptType(), spo.getContent(), dv), e);
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
                log.error("指定的脚本文件不存在：{}", script);
                throw new DataGeneratorException("指定的脚本文件不存在");
            }
        }
        //内联脚本字符串
        if (Objects.isNull(source)) {
            source = Source.newBuilder(LANGUAGE, script, RandomKit.alpha(5)).build();
        }

        return Objects.requireNonNull(source,
                String.format("无法解析给定的脚本内容，脚本类型：JAVASCRIPT，脚本内容：%s，请检查配置是否正确", script));
    }
}

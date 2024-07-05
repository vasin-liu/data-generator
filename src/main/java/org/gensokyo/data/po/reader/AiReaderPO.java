/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po.reader;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.ai.parser.ListOutputParser;
import org.gensokyo.data.ai.parser.OutputParser;
import org.gensokyo.data.constant.AiProvider;

/**
 * AI数据读取配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/1 , Version 1.0.0
 */
@Getter
@Setter
public class AiReaderPO extends ReaderPO {

    private AiProvider provider = AiProvider.OLLAMA;

    private String model = "llama3";

    private String template = """
            你是一个数据生成助手，{subject}，{format}
            请仅输出生成结果，不需要其他说明
            """;

    private Class<? extends OutputParser<?>> parser = ListOutputParser.class;

    private String subject;
}

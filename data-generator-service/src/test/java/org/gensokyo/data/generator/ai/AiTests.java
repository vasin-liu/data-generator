/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.gensokyo.data.ai.chat.ChatResponse;
import org.gensokyo.data.ai.chat.Generation;
import org.gensokyo.data.ai.chat.prompt.Prompt;
import org.gensokyo.data.ai.chat.prompt.PromptTemplate;
import org.gensokyo.data.ai.model.ModelOptionsUtils;
import org.gensokyo.data.ai.ollama.OllamaChatClient;
import org.gensokyo.data.ai.ollama.api.OllamaApi;
import org.gensokyo.data.ai.ollama.api.OllamaOptions;
import org.gensokyo.data.ai.parser.ListOutputParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * AI测试类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/27 , Version 1.0.0
 */
class AiTests {

    private static void assumeOllamaAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 11434), 1000);
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "Ollama is not available on localhost:11434");
        }
    }

    @Test
    void case1() throws JsonProcessingException {
        assumeOllamaAvailable();
        OllamaApi api = new OllamaApi("http://localhost:11434");
        OllamaOptions options = OllamaOptions.create()
                .withModel("llama3")
                .withTemperature(0.7f);
        OllamaChatClient client = new OllamaChatClient(api, options);

        ListOutputParser parser = new ListOutputParser(new DefaultConversionService());
        String format = parser.getFormat();
        String template = """
				List five {subject}
				{format}
				""";
        PromptTemplate promptTemplate = new PromptTemplate(template,
                Map.of("subject", "ice cream flavors.", "format", format));
        Prompt prompt = new Prompt(promptTemplate.createMessage());
        Generation generation = client.call(prompt).getResult();

        List<String> r = parser.parse(generation.getOutput().getContent());

        System.out.println("===> " + ModelOptionsUtils.OBJECT_MAPPER.writeValueAsString(r));
    }

    @Test
    void case2() throws JsonProcessingException {
        assumeOllamaAvailable();
        OllamaApi api = new OllamaApi("http://localhost:11434");
        OllamaOptions options = OllamaOptions.create()
                .withModel("llama3")
                .withTemperature(0.7f);
        OllamaChatClient client = new OllamaChatClient(api, options);
        ListOutputParser parser = new ListOutputParser(new DefaultConversionService());
        String template = """
                你是一个数据生成助手，{subject}，{format}
                请仅输出生成结果，不需要其他说明
                """;
        PromptTemplate promptTemplate = new PromptTemplate(template,
                Map.of("subject", "列出5个项目培训计划标题", "format", parser.getFormat()));
        Prompt prompt = new Prompt(promptTemplate.createMessage());

        System.out.println("===> " + ModelOptionsUtils.OBJECT_MAPPER.writeValueAsString(prompt));

        ChatResponse resp = client.call(prompt);

        List<String> r = parser.parse(resp.getResult().getOutput().getContent());

        System.out.println("===> " + ModelOptionsUtils.OBJECT_MAPPER.writeValueAsString(r));
    }
}

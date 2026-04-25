/*
 * Copyright 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
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
 * AI integration tests.
 */
class AiTests {

    private static void assumeOllamaAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 11434), 1000);
        }
        catch (Exception ex) {
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

        List<String> result = parser.parse(generation.getOutput().getContent());

        System.out.println("===> " + ModelOptionsUtils.OBJECT_MAPPER.writeValueAsString(result));
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
                Please list five {subject}.
                {format}
                """;
        PromptTemplate promptTemplate = new PromptTemplate(template,
                Map.of("subject", "popular Chinese desserts", "format", parser.getFormat()));
        Prompt prompt = new Prompt(promptTemplate.createMessage());

        System.out.println("===> " + ModelOptionsUtils.OBJECT_MAPPER.writeValueAsString(prompt));

        ChatResponse response = client.call(prompt);
        List<String> result = parser.parse(response.getResult().getOutput().getContent());

        System.out.println("===> " + ModelOptionsUtils.OBJECT_MAPPER.writeValueAsString(result));
    }
}
/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.ollama;

import org.gensokyo.data.ai.chat.ChatClient;
import org.gensokyo.data.ai.chat.ChatResponse;
import org.gensokyo.data.ai.chat.Generation;
import org.gensokyo.data.ai.chat.StreamingChatClient;
import org.gensokyo.data.ai.chat.messages.Message;
import org.gensokyo.data.ai.chat.messages.MessageType;
import org.gensokyo.data.ai.chat.metadata.ChatGenerationMetadata;
import org.gensokyo.data.ai.chat.metadata.Usage;
import org.gensokyo.data.ai.chat.prompt.ChatOptions;
import org.gensokyo.data.ai.chat.prompt.Prompt;
import org.gensokyo.data.ai.model.ModelOptionsUtils;
import org.gensokyo.data.ai.ollama.api.OllamaApi;
import org.gensokyo.data.ai.ollama.api.OllamaOptions;
import org.gensokyo.kit.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Base64;
import java.util.List;

/**
 * Ollama会话客户端
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public class OllamaChatClient implements ChatClient, StreamingChatClient {

    private final OllamaApi chatApi;

    private final OllamaOptions defaultOptions;

    public OllamaChatClient(OllamaApi chatApi) {
        this(chatApi, OllamaOptions.create().withModel(OllamaOptions.DEFAULT_MODEL));
    }

    public OllamaChatClient(OllamaApi chatApi, OllamaOptions defaultOptions) {
        Assert.notNull(chatApi, "OllamaApi must not be null");
        Assert.notNull(defaultOptions, "DefaultOptions must not be null");
        this.chatApi = chatApi;
        this.defaultOptions = defaultOptions;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        OllamaApi.ChatResponse response = this.chatApi.chat(ollamaChatRequest(prompt, false));

        var generator = new Generation(response.message().content());
        if (response.promptEvalCount() != null && response.evalCount() != null) {
            generator = generator
                    .withGenerationMetadata(ChatGenerationMetadata.from("unknown", extractUsage(response)));
        }
        return new ChatResponse(List.of(generator));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {

        Flux<OllamaApi.ChatResponse> response = this.chatApi.streamingChat(ollamaChatRequest(prompt, true));

        return response.map(chunk -> {
            Generation generation = (chunk.message() != null) ? new Generation(chunk.message().content())
                    : new Generation("");
            if (Boolean.TRUE.equals(chunk.done())) {
                generation = generation
                        .withGenerationMetadata(ChatGenerationMetadata.from("unknown", extractUsage(chunk)));
            }
            return new ChatResponse(List.of(generation));
        });
    }

    private Usage extractUsage(OllamaApi.ChatResponse response) {
        return new Usage() {

            @Override
            public Long getPromptTokens() {
                return response.promptEvalCount().longValue();
            }

            @Override
            public Long getGenerationTokens() {
                return response.evalCount().longValue();
            }
        };
    }

    OllamaApi.ChatRequest ollamaChatRequest(Prompt prompt, boolean stream) {

        List<OllamaApi.Message> ollamaMessages = prompt.getInstructions()
                .stream()
                .filter(message -> message.getMessageType() == MessageType.USER
                        || message.getMessageType() == MessageType.ASSISTANT
                        || message.getMessageType() == MessageType.SYSTEM)
                .map(m -> {
                    var messageBuilder = OllamaApi.Message.builder(toRole(m)).withContent(m.getContent());

                    if (!CollectionUtils.isEmpty(m.getMedia())) {
                        messageBuilder
                                .withImages(m.getMedia().stream().map(media -> this.fromMediaData(media.data())).toList());
                    }
                    return messageBuilder.build();
                })
                .toList();

        // runtime options
        OllamaOptions runtimeOptions = null;
        if (prompt.getOptions() != null) {
            if (prompt.getOptions() instanceof ChatOptions runtimeChatOptions) {
                runtimeOptions = ModelOptionsUtils.copyToTarget(runtimeChatOptions, ChatOptions.class,
                        OllamaOptions.class);
            } else {
                throw new IllegalArgumentException("Prompt options are not of type ChatOptions: "
                        + prompt.getOptions().getClass().getSimpleName());
            }
        }

        OllamaOptions mergedOptions = ModelOptionsUtils.merge(runtimeOptions, this.defaultOptions, OllamaOptions.class);

        // Override the model.
        if (!StringUtils.hasText(mergedOptions.getModel())) {
            throw new IllegalArgumentException("Model is not set!");
        }

        String model = mergedOptions.getModel();
        return OllamaApi.ChatRequest.builder(model)
                .withStream(stream)
                .withMessages(ollamaMessages)
                .withOptions(mergedOptions)
                .build();
    }

    private String fromMediaData(Object mediaData) {
        if (mediaData instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        } else if (mediaData instanceof String text) {
            return text;
        } else {
            throw new IllegalArgumentException("Unsupported media data type: " + mediaData.getClass().getSimpleName());
        }

    }

    private OllamaApi.Message.Role toRole(Message message) {
        return switch (message.getMessageType()) {
            case USER -> OllamaApi.Message.Role.USER;
            case ASSISTANT -> OllamaApi.Message.Role.ASSISTANT;
            case SYSTEM -> OllamaApi.Message.Role.SYSTEM;
            default -> throw new IllegalArgumentException("Unsupported message type: " + message.getMessageType());
        };
    }

}

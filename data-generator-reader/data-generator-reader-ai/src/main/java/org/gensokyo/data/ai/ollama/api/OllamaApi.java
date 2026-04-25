/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.ollama.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.kit.Assert;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Ollama接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
@Slf4j
public class OllamaApi {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";

    public static final String REQUEST_BODY_NULL_ERROR = "The request body can not be null.";

    private final WebClient webClient;

    public OllamaApi() {
        this(DEFAULT_BASE_URL);
    }

    public OllamaApi(String baseUrl) {
        Consumer<HttpHeaders> defaultHeaders = headers -> {
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        };

        this.webClient = WebClient.builder().baseUrl(baseUrl).defaultHeaders(defaultHeaders).build();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerateRequest(
            @JsonProperty("model") String model,
            @JsonProperty("prompt") String prompt,
            @JsonProperty("format") String format,
            @JsonProperty("options") Map<String, Object> options,
            @JsonProperty("system") String system,
            @JsonProperty("template") String template,
            @JsonProperty("context") List<Integer> context,
            @JsonProperty("stream") Boolean stream,
            @JsonProperty("raw") Boolean raw) {

        public GenerateRequest(String model, String prompt, Boolean stream) {
            this(model, prompt, null, null, null, null, null, stream, null);
        }

        public GenerateRequest(String model, String prompt, boolean enableJsonFormat, Boolean stream) {
            this(model, prompt, (enableJsonFormat) ? "json" : null, null, null, null, null, stream, null);
        }

        public static Builder builder(String prompt) {
            return new Builder(prompt);
        }

        public static class Builder {

            private String model;
            private final String prompt;
            private String format;
            private Map<String, Object> options;
            private String system;
            private String template;
            private List<Integer> context;
            private Boolean stream;
            private Boolean raw;

            public Builder(String prompt) {
                this.prompt = prompt;
            }

            public Builder withModel(String model) {
                this.model = model;
                return this;
            }

            public Builder withFormat(String format) {
                this.format = format;
                return this;
            }

            public Builder withOptions(Map<String, Object> options) {
                this.options = options;
                return this;
            }

            public Builder withOptions(OllamaOptions options) {
                this.options = options.toMap();
                return this;
            }

            public Builder withSystem(String system) {
                this.system = system;
                return this;
            }

            public Builder withTemplate(String template) {
                this.template = template;
                return this;
            }

            public Builder withContext(List<Integer> context) {
                this.context = context;
                return this;
            }

            public Builder withStream(Boolean stream) {
                this.stream = stream;
                return this;
            }

            public Builder withRaw(Boolean raw) {
                this.raw = raw;
                return this;
            }

            public GenerateRequest build() {
                return new GenerateRequest(model, prompt, format, options, system, template, context, stream, raw);
            }

        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerateResponse(
            @JsonProperty("model") String model,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("response") String response,
            @JsonProperty("done") Boolean done,
            @JsonProperty("context") List<Integer> context,
            @JsonProperty("total_duration") Duration totalDuration,
            @JsonProperty("load_duration") Duration loadDuration,
            @JsonProperty("prompt_eval_count") Integer promptEvalCount,
            @JsonProperty("prompt_eval_duration") Duration promptEvalDuration,
            @JsonProperty("eval_count") Integer evalCount,
            @JsonProperty("eval_duration") Duration evalDuration) {
    }

    public GenerateResponse generate(GenerateRequest completionRequest) {
        Assert.notNull(completionRequest, REQUEST_BODY_NULL_ERROR);
        Assert.isTrue(!completionRequest.stream(), "Stream mode must be disabled.");

        return this.webClient.post()
                .uri("/api/generate")
                .bodyValue(completionRequest)
                .retrieve()
                .bodyToMono(GenerateResponse.class)
                .block();
    }

    public Flux<GenerateResponse> generateStreaming(GenerateRequest completionRequest) {
        Assert.notNull(completionRequest, REQUEST_BODY_NULL_ERROR);
        Assert.isTrue(completionRequest.stream(), "Request must set the steam property to true.");

        return webClient.post()
                .uri("/api/generate")
                .bodyValue(completionRequest)
                .retrieve()
                .bodyToFlux(GenerateResponse.class)
                .handle((data, sink) -> {
                    if (log.isTraceEnabled()) {
                        log.trace(Objects.toString(data));
                    }
                    sink.next(data);
                });
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Message(
            @JsonProperty("role") Role role,
            @JsonProperty("content") String content,
            @JsonProperty("images") List<String> images) {


        public enum Role {

            @JsonProperty("system") SYSTEM,

            @JsonProperty("user") USER,

            @JsonProperty("assistant") ASSISTANT;

        }

        public static Builder builder(Role role) {
            return new Builder(role);
        }

        public static class Builder {

            private final Role role;
            private String content;
            private List<String> images;

            public Builder(Role role) {
                this.role = role;
            }

            public Builder withContent(String content) {
                this.content = content;
                return this;
            }

            public Builder withImages(List<String> images) {
                this.images = images;
                return this;
            }

            public Message build() {
                return new Message(role, content, images);
            }

        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatRequest(
            @JsonProperty("model") String model,
            @JsonProperty("messages") List<Message> messages,
            @JsonProperty("stream") Boolean stream,
            @JsonProperty("format") String format,
            @JsonProperty("options") Map<String, Object> options) {

        public static Builder builder(String model) {
            return new Builder(model);
        }

        public static class Builder {
            private final String model;
            private List<Message> messages = List.of();
            private boolean stream = false;
            private String format;
            private Map<String, Object> options = Map.of();

            public Builder(String model) {
                Assert.notNull(model, "The model can not be null.");
                this.model = model;
            }

            public Builder withMessages(List<Message> messages) {
                this.messages = messages;
                return this;
            }

            public Builder withStream(boolean stream) {
                this.stream = stream;
                return this;
            }

            public Builder withFormat(String format) {
                this.format = format;
                return this;
            }

            public Builder withOptions(Map<String, Object> options) {
                Objects.requireNonNull(options, "The options can not be null.");

                this.options = OllamaOptions.filterNonSupportedFields(options);
                return this;
            }

            public Builder withOptions(OllamaOptions options) {
                Objects.requireNonNull(options, "The options can not be null.");
                this.options = OllamaOptions.filterNonSupportedFields(options.toMap());
                return this;
            }

            public ChatRequest build() {
                return new ChatRequest(model, messages, stream, format, options);
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatResponse(
            @JsonProperty("model") String model,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("message") Message message,
            @JsonProperty("done") Boolean done,
            @JsonProperty("total_duration") Duration totalDuration,
            @JsonProperty("load_duration") Duration loadDuration,
            @JsonProperty("prompt_eval_count") Integer promptEvalCount,
            @JsonProperty("prompt_eval_duration") Duration promptEvalDuration,
            @JsonProperty("eval_count") Integer evalCount,
            @JsonProperty("eval_duration") Duration evalDuration) {
    }

    public ChatResponse chat(ChatRequest chatRequest) {
        Assert.notNull(chatRequest, REQUEST_BODY_NULL_ERROR);
        Assert.isTrue(!chatRequest.stream(), "Stream mode must be disabled.");

        return this.webClient.post()
                .uri("/api/chat")
                .bodyValue(chatRequest)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block();
    }

    public Flux<ChatResponse> streamingChat(ChatRequest chatRequest) {
        Assert.notNull(chatRequest, REQUEST_BODY_NULL_ERROR);
        Assert.isTrue(chatRequest.stream(), "Request must set the steam property to true.");

        return webClient.post()
                .uri("/api/chat")
                .bodyValue(chatRequest)
                .retrieve()
                .bodyToFlux(ChatResponse.class)
                .handle((data, sink) -> {
                    if (log.isTraceEnabled()) {
                        log.trace(Objects.toString(data));
                    }
                    sink.next(data);
                });
    }
}

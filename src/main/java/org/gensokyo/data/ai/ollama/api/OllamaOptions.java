/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.ollama.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.gensokyo.data.ai.chat.prompt.ChatOptions;
import org.gensokyo.data.ai.embedding.EmbeddingOptions;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ollama选项参数
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaOptions implements ChatOptions, EmbeddingOptions {

    public static final String DEFAULT_MODEL = OllamaModel.LLAMA3.id();

    @JsonProperty("numa")
    private Boolean useNUMA;

    @JsonProperty("num_ctx")
    private Integer numCtx = 1024;

    @JsonProperty("num_batch")
    private Integer numBatch;

    @JsonProperty("num_gqa")
    private Integer numGQA;

    @JsonProperty("num_gpu")
    private Integer numGPU;

    @JsonProperty("main_gpu")
    private Integer mainGPU;

    @JsonProperty("low_vram")
    private Boolean lowVRAM;

    @JsonProperty("f16_kv")
    private Boolean f16KV;

    @JsonProperty("logits_all")
    private Boolean logitsAll;

    @JsonProperty("vocab_only")
    private Boolean vocabOnly;

    @JsonProperty("use_mmap")
    private Boolean useMMap;

    @JsonProperty("use_mlock")
    private Boolean useMLock;

    @JsonProperty("rope_frequency_base")
    private Float ropeFrequencyBase;

    @JsonProperty("rope_frequency_scale")
    private Float ropeFrequencyScale;

    @JsonProperty("num_thread")
    private Integer numThread;

    @JsonProperty("num_keep")
    private Integer numKeep;

    @JsonProperty("seed")
    private Integer seed;

    @JsonProperty("num_predict")
    private Integer numPredict;

    @JsonProperty("top_k")
    private Integer topK;

    @JsonProperty("top_p")
    private Float topP;

    @JsonProperty("tfs_z")
    private Float tfsZ;

    @JsonProperty("typical_p")
    private Float typicalP;

    @JsonProperty("repeat_last_n")
    private Integer repeatLastN;

    @JsonProperty("temperature")
    private Float temperature = 0.7f;

    @JsonProperty("repeat_penalty")
    private Float repeatPenalty;

    @JsonProperty("presence_penalty")
    private Float presencePenalty;

    @JsonProperty("frequency_penalty")
    private Float frequencyPenalty;

    @JsonProperty("mirostat")
    private Integer mirostat;

    @JsonProperty("mirostat_tau")
    private Float mirostatTau;

    @JsonProperty("mirostat_eta")
    private Float mirostatEta;

    @JsonProperty("penalize_newline")
    private Boolean penalizeNewline;

    @JsonProperty("stop")
    private List<String> stop;

    @Getter
    @JsonProperty("model")
    private String model;

    public OllamaOptions withModel(String model) {
        this.model = model;
        return this;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public OllamaOptions withUseNUMA(Boolean useNUMA) {
        this.useNUMA = useNUMA;
        return this;
    }

    public OllamaOptions withNumCtx(Integer numCtx) {
        this.numCtx = numCtx;
        return this;
    }

    public OllamaOptions withNumBatch(Integer numBatch) {
        this.numBatch = numBatch;
        return this;
    }

    public OllamaOptions withNumGQA(Integer numGQA) {
        this.numGQA = numGQA;
        return this;
    }

    public OllamaOptions withNumGPU(Integer numGPU) {
        this.numGPU = numGPU;
        return this;
    }

    public OllamaOptions withMainGPU(Integer mainGPU) {
        this.mainGPU = mainGPU;
        return this;
    }

    public OllamaOptions withLowVRAM(Boolean lowVRAM) {
        this.lowVRAM = lowVRAM;
        return this;
    }

    public OllamaOptions withF16KV(Boolean f16KV) {
        this.f16KV = f16KV;
        return this;
    }

    public OllamaOptions withLogitsAll(Boolean logitsAll) {
        this.logitsAll = logitsAll;
        return this;
    }

    public OllamaOptions withVocabOnly(Boolean vocabOnly) {
        this.vocabOnly = vocabOnly;
        return this;
    }

    public OllamaOptions withUseMMap(Boolean useMMap) {
        this.useMMap = useMMap;
        return this;
    }

    public OllamaOptions withUseMLock(Boolean useMLock) {
        this.useMLock = useMLock;
        return this;
    }

    public OllamaOptions withRopeFrequencyBase(Float ropeFrequencyBase) {
        this.ropeFrequencyBase = ropeFrequencyBase;
        return this;
    }

    public OllamaOptions withRopeFrequencyScale(Float ropeFrequencyScale) {
        this.ropeFrequencyScale = ropeFrequencyScale;
        return this;
    }

    public OllamaOptions withNumThread(Integer numThread) {
        this.numThread = numThread;
        return this;
    }

    public OllamaOptions withNumKeep(Integer numKeep) {
        this.numKeep = numKeep;
        return this;
    }

    public OllamaOptions withSeed(Integer seed) {
        this.seed = seed;
        return this;
    }

    public OllamaOptions withNumPredict(Integer numPredict) {
        this.numPredict = numPredict;
        return this;
    }

    public OllamaOptions withTopK(Integer topK) {
        this.topK = topK;
        return this;
    }

    public OllamaOptions withTopP(Float topP) {
        this.topP = topP;
        return this;
    }

    public OllamaOptions withTfsZ(Float tfsZ) {
        this.tfsZ = tfsZ;
        return this;
    }

    public OllamaOptions withTypicalP(Float typicalP) {
        this.typicalP = typicalP;
        return this;
    }

    public OllamaOptions withRepeatLastN(Integer repeatLastN) {
        this.repeatLastN = repeatLastN;
        return this;
    }

    public OllamaOptions withTemperature(Float temperature) {
        this.temperature = temperature;
        return this;
    }

    public OllamaOptions withRepeatPenalty(Float repeatPenalty) {
        this.repeatPenalty = repeatPenalty;
        return this;
    }

    public OllamaOptions withPresencePenalty(Float presencePenalty) {
        this.presencePenalty = presencePenalty;
        return this;
    }

    public OllamaOptions withFrequencyPenalty(Float frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
        return this;
    }

    public OllamaOptions withMirostat(Integer mirostat) {
        this.mirostat = mirostat;
        return this;
    }

    public OllamaOptions withMirostatTau(Float mirostatTau) {
        this.mirostatTau = mirostatTau;
        return this;
    }

    public OllamaOptions withMirostatEta(Float mirostatEta) {
        this.mirostatEta = mirostatEta;
        return this;
    }

    public OllamaOptions withPenalizeNewline(Boolean penalizeNewline) {
        this.penalizeNewline = penalizeNewline;
        return this;
    }

    public OllamaOptions withStop(List<String> stop) {
        this.stop = stop;
        return this;
    }

    public void setUseNUMA(Boolean useNUMA) {
        this.useNUMA = useNUMA;
    }

    public void setNumCtx(Integer numCtx) {
        this.numCtx = numCtx;
    }

    public void setNumBatch(Integer numBatch) {
        this.numBatch = numBatch;
    }

    public void setNumGQA(Integer numGQA) {
        this.numGQA = numGQA;
    }

    public void setNumGPU(Integer numGPU) {
        this.numGPU = numGPU;
    }

    public void setMainGPU(Integer mainGPU) {
        this.mainGPU = mainGPU;
    }

    public void setLowVRAM(Boolean lowVRAM) {
        this.lowVRAM = lowVRAM;
    }

    public void setF16KV(Boolean f16kv) {
        f16KV = f16kv;
    }

    public void setLogitsAll(Boolean logitsAll) {
        this.logitsAll = logitsAll;
    }

    public void setVocabOnly(Boolean vocabOnly) {
        this.vocabOnly = vocabOnly;
    }

    public void setUseMMap(Boolean useMMap) {
        this.useMMap = useMMap;
    }

    public void setUseMLock(Boolean useMLock) {
        this.useMLock = useMLock;
    }

    public void setRopeFrequencyBase(Float ropeFrequencyBase) {
        this.ropeFrequencyBase = ropeFrequencyBase;
    }

    public void setRopeFrequencyScale(Float ropeFrequencyScale) {
        this.ropeFrequencyScale = ropeFrequencyScale;
    }

    public void setNumThread(Integer numThread) {
        this.numThread = numThread;
    }

    public void setNumKeep(Integer numKeep) {
        this.numKeep = numKeep;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    public void setNumPredict(Integer numPredict) {
        this.numPredict = numPredict;
    }

    @Override
    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    @Override
    public Float getTopP() {
        return topP;
    }

    public void setTopP(Float topP) {
        this.topP = topP;
    }

    public void setTfsZ(Float tfsZ) {
        this.tfsZ = tfsZ;
    }

    public void setTypicalP(Float typicalP) {
        this.typicalP = typicalP;
    }

    public void setRepeatLastN(Integer repeatLastN) {
        this.repeatLastN = repeatLastN;
    }

    @Override
    public Float getTemperature() {
        return temperature;
    }

    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }

    public void setRepeatPenalty(Float repeatPenalty) {
        this.repeatPenalty = repeatPenalty;
    }

    public void setPresencePenalty(Float presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public void setFrequencyPenalty(Float frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public void setMirostat(Integer mirostat) {
        this.mirostat = mirostat;
    }

    public void setMirostatTau(Float mirostatTau) {
        this.mirostatTau = mirostatTau;
    }

    public void setMirostatEta(Float mirostatEta) {
        this.mirostatEta = mirostatEta;
    }

    public void setPenalizeNewline(Boolean penalizeNewline) {
        this.penalizeNewline = penalizeNewline;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    public Map<String, Object> toMap() {
        try {
            var json = new ObjectMapper().writeValueAsString(this);
            return new ObjectMapper().readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static OllamaOptions create() {
        return new OllamaOptions();
    }

    public static Map<String, Object> filterNonSupportedFields(Map<String, Object> options) {
        return options.entrySet().stream()
                .filter(e -> !e.getKey().equals("model"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

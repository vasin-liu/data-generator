/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.ollama.api;

/**
 * Ollama模型枚举
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public enum OllamaModel {

    LLAMA3("llama3"),

    LLAMA2("llama2"),

    MISTRAL("mistral"),

    DOLPHIN_PHI("dolphin-phi"),

    PHI("phi"),

    NEURAL_CHAT("neural-chat"),

    STARLING_LM("starling-lm"),

    CODELLAMA("codellama"),

    ORCA_MINI("orca-mini"),

    LLAVA("llava"),

    GEMMA("gemma"),

    LLAMA2_UNCENSORED("llama2-uncensored");

    private final String id;

    OllamaModel(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }
}

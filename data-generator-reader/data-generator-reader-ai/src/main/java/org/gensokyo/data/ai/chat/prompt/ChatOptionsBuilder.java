/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.prompt;

/**
 * 会话设置构建器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/27 , Version 1.0.0
 */
public class ChatOptionsBuilder {

    private class ChatOptionsImpl implements ChatOptions {

        private Float temperature;

        private Float topP;

        private Integer topK;

        @Override
        public Float getTemperature() {
            return temperature;
        }

        public void setTemperature(Float temperature) {
            this.temperature = temperature;
        }

        @Override
        public Float getTopP() {
            return topP;
        }

        public void setTopP(Float topP) {
            this.topP = topP;
        }

        @Override
        public Integer getTopK() {
            return topK;
        }

        public void setTopK(Integer topK) {
            this.topK = topK;
        }

    }

    private final ChatOptionsImpl options = new ChatOptionsImpl();

    private ChatOptionsBuilder() {
    }

    public static ChatOptionsBuilder builder() {
        return new ChatOptionsBuilder();
    }

    public ChatOptionsBuilder withTemperature(Float temperature) {
        options.setTemperature(temperature);
        return this;
    }

    public ChatOptionsBuilder withTopP(Float topP) {
        options.setTopP(topP);
        return this;
    }

    public ChatOptionsBuilder withTopK(Integer topK) {
        options.setTopK(topK);
        return this;
    }

    public ChatOptions build() {
        return options;
    }

}

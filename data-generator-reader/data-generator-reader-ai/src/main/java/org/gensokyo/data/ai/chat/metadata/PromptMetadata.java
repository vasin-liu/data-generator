/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.metadata;

import org.gensokyo.kit.Assert;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * 提示词元数据接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
@FunctionalInterface
public interface PromptMetadata extends Iterable<PromptMetadata.PromptFilterMetadata> {


    static PromptMetadata empty() {
        return of();
    }


    static <T> PromptMetadata of(PromptFilterMetadata... array) {
        return of(Arrays.asList(array));
    }


    static PromptMetadata of(Iterable<PromptFilterMetadata> iterable) {
        Assert.notNull(iterable, "An Iterable of PromptFilterMetadata must not be null");
        return iterable::iterator;
    }

    default Optional<PromptFilterMetadata> findByPromptIndex(int promptIndex) {

        Assert.isTrue(promptIndex > -1, "Prompt index [%d] must be greater than equal to 0".formatted(promptIndex));

        return StreamSupport.stream(this.spliterator(), false)
                .filter(promptFilterMetadata -> promptFilterMetadata.getPromptIndex() == promptIndex)
                .findFirst();
    }

    interface PromptFilterMetadata {

        static PromptFilterMetadata from(int promptIndex, Object contentFilterMetadata) {

            return new PromptFilterMetadata() {

                @Override
                public int getPromptIndex() {
                    return promptIndex;
                }

                @Override
                @SuppressWarnings("unchecked")
                public <T> T getContentFilterMetadata() {
                    return (T) contentFilterMetadata;
                }
            };
        }

        int getPromptIndex();

        <T> T getContentFilterMetadata();

    }

}

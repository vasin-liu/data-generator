/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.calcite.AiRuntimeBridge;
import org.gensokyo.data.calcite.runtime.AiGenerateResult;
import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Routes AI source materialization to the first supporting runtime bridge delegate.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
public class CompositeAiRuntimeBridge implements AiRuntimeBridge {

    private final List<AiRuntimeBridge> delegates;

    /**
     * @param delegates ordered runtime bridges; first match wins
     */
    public CompositeAiRuntimeBridge(List<AiRuntimeBridge> delegates) {
        Assert.notEmpty(delegates, "AI runtime bridge delegates must not be empty");
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public boolean supports(AiProviderVO provider) {
        return resolveDelegate(provider) != null;
    }

    @Override
    public Object generate(AiSourceVO source) {
        return generateTraced(source).payload();
    }

    @Override
    public AiGenerateResult generateTraced(AiSourceVO source) {
        Assert.notNull(source, "AI source must not be null");
        AiRuntimeBridge delegate = resolveDelegate(source.getProvider());
        if (delegate == null) {
            throw unsupportedProvider(source.getProvider());
        }
        return delegate.generateTraced(source);
    }

    private AiRuntimeBridge resolveDelegate(AiProviderVO provider) {
        if (provider == null || !StringUtils.hasText(provider.getType())) {
            return null;
        }
        for (AiRuntimeBridge delegate : delegates) {
            if (delegate.supports(provider)) {
                return delegate;
            }
        }
        return null;
    }

    private UnsupportedOperationException unsupportedProvider(AiProviderVO provider) {
        String providerType = provider == null || provider.getType() == null
                ? "null"
                : provider.getType().trim().toUpperCase(Locale.ROOT);
        String supported = delegates.stream()
                .map(CompositeAiRuntimeBridge::describeDelegate)
                .collect(Collectors.joining(", "));
        return new UnsupportedOperationException(
                "AI provider type [" + providerType + "] is not supported. Configured bridges: " + supported);
    }

    private static String describeDelegate(AiRuntimeBridge delegate) {
        return delegate.getClass().getSimpleName();
    }
}

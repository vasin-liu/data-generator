package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.parser.*;

import org.gensokyo.data.model.v2.AiSourceVO;
import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.SourceVO;

import java.util.Locale;
import java.util.Set;

public class AiSourceFactory implements V2SourceFactory {
    private static final Set<String> LOCAL_PROVIDER_TYPES = Set.of("INLINE", "STATIC", "ECHO");

    private final AiRuntimeBridge aiRuntimeBridge;

    public AiSourceFactory() {
        this(null);
    }

    public AiSourceFactory(AiRuntimeBridge aiRuntimeBridge) {
        this.aiRuntimeBridge = aiRuntimeBridge;
    }

    @Override
    public boolean supports(SourceVO source) {
        if (!(source instanceof AiSourceVO aiSource)) {
            return false;
        }
        // INLINE/ECHO/STATIC stay available even when a remote bridge is wired for OLLAMA.
        if (isLocalProvider(aiSource.getProvider())) {
            return true;
        }
        return aiRuntimeBridge != null && aiRuntimeBridge.supports(aiSource.getProvider());
    }

    @Override
    public RowSource create(String name, SourceVO source) {
        return new AiRowSource(name, (AiSourceVO) source, aiRuntimeBridge);
    }

    private boolean isLocalProvider(AiProviderVO provider) {
        return provider != null && provider.getType() != null
                && LOCAL_PROVIDER_TYPES.contains(provider.getType().trim().toUpperCase(Locale.ROOT));
    }
}

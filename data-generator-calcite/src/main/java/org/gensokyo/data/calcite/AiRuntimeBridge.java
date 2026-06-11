package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;

public interface AiRuntimeBridge {
    boolean supports(AiProviderVO provider);

    Object generate(AiSourceVO source);
}

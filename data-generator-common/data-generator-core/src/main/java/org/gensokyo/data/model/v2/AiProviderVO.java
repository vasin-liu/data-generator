package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class AiProviderVO implements Serializable {
    private String type;
    private Map<String, Object> options = new LinkedHashMap<>();
}

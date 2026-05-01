package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class InlineDataSourceVO implements Serializable {
    private String name;
    private String type;
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private Map<String, String> properties = new LinkedHashMap<>();
}

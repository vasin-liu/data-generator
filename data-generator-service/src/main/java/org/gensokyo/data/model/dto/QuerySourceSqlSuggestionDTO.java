package org.gensokyo.data.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuerySourceSqlSuggestionDTO implements Serializable {
    private String scenario;
    private String primarySource;
    private List<String> sourceOrder;
    private List<String> aliases;
    private String sql;
}

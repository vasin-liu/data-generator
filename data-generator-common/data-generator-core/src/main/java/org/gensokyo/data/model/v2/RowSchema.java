package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class RowSchema implements Serializable {
    private List<ColumnDef> columns = new ArrayList<>();

    public ColumnDef column(String name) {
        if (name == null) {
            return null;
        }
        return columns.stream()
                .filter(column -> name.equalsIgnoreCase(column.getName()))
                .findFirst()
                .orElse(null);
    }

    public boolean contains(String name) {
        return Objects.nonNull(column(name));
    }
}

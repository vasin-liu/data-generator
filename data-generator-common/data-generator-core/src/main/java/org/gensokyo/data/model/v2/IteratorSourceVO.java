package org.gensokyo.data.model.v2;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.iterator.IteratorVO;

@Getter
@Setter
@AutoService(SourceVO.class)
@JsonSubType("ITERATOR")
public class IteratorSourceVO extends SourceVO {
    public IteratorSourceVO() {
        setType("iterator");
    }

    private IteratorVO iterator;
}

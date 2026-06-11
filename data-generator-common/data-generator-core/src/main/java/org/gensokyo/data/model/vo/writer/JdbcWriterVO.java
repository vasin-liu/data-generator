package org.gensokyo.data.model.vo.writer;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.v2.InlineDataSourceVO;

@Getter
@Setter
@AutoService(WriterVO.class)
@JsonSubType(Const.WriterType.JDBC)
public class JdbcWriterVO extends WriterVO {
    public JdbcWriterVO() {
        setType(Const.WriterType.JDBC);
    }

    private InlineDataSourceVO dataSource;
}

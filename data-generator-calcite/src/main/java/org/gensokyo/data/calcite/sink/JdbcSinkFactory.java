package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.codec.*;

import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class JdbcSinkFactory implements V2SinkFactory {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver;

    public JdbcSinkFactory(NamedParameterJdbcTemplate jdbcTemplate,
                           RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver) {
        this.jdbcTemplate = jdbcTemplate;
        this.runtimeJdbcEndpointResolver = runtimeJdbcEndpointResolver;
    }

    @Override
    public boolean supports(WriterVO writer) {
        return writer instanceof JdbcWriterVO;
    }

    @Override
    public RowSink create(WriterVO writer) {
        return new JdbcRowSinkAdapter(jdbcTemplate, (JdbcWriterVO) writer, runtimeJdbcEndpointResolver);
    }
}

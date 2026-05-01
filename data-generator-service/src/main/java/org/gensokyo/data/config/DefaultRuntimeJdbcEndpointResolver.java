package org.gensokyo.data.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.kit.character.StrKit;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Objects;

@RequiredArgsConstructor
public class DefaultRuntimeJdbcEndpointResolver implements RuntimeJdbcEndpointResolver {
    private final ObjectProvider<DynamicRoutingDataSource> dynamicRoutingDataSourceProvider;

    @Override
    public String resolveSourceDataSourceId(QuerySourceVO source) {
        if (source == null) {
            return null;
        }
        if (StrKit.isNotBlank(source.getDataSourceId())) {
            return source.getDataSourceId();
        }
        return ensureInlineDataSource(source.getDataSource(), source.getDataSourceId());
    }

    @Override
    public String resolveSinkDataSourceId(JdbcWriterVO writer) {
        if (writer == null) {
            return null;
        }
        if (StrKit.isNotBlank(writer.getDataSourceId())) {
            return writer.getDataSourceId();
        }
        return ensureInlineDataSource(writer.getDataSource(), writer.getDataSourceId());
    }

    private String ensureInlineDataSource(InlineDataSourceVO inline, String fallback) {
        if (inline == null || StrKit.isBlank(inline.getName())) {
            return fallback;
        }
        DynamicRoutingDataSource routing = dynamicRoutingDataSourceProvider.getIfAvailable();
        if (routing == null) {
            throw new IllegalStateException("DynamicRoutingDataSource is required for inline JDBC endpoint loading");
        }
        if (!routing.getDataSources().containsKey(inline.getName())) {
            routing.addDataSource(inline.getName(), createDataSource(inline));
        }
        return inline.getName();
    }

    private DruidDataSource createDataSource(InlineDataSourceVO inline) {
        if (StrKit.isBlank(inline.getUrl())) {
            throw new IllegalArgumentException("Inline datasource url must not be blank");
        }
        if (StrKit.isBlank(inline.getDriverClassName())) {
            throw new IllegalArgumentException("Inline datasource driverClassName must not be blank");
        }
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(inline.getUrl());
        dataSource.setUsername(inline.getUsername());
        dataSource.setPassword(inline.getPassword());
        dataSource.setDriverClassName(inline.getDriverClassName());
        dataSource.setValidationQuery("SELECT 1");
        if (Objects.nonNull(inline.getProperties())) {
            inline.getProperties().forEach((key, value) -> {
                if (StrKit.isNotBlank(key) && value != null) {
                    dataSource.addConnectionProperty(key, value);
                }
            });
        }
        return dataSource;
    }
}

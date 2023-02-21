/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.writer;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.generator.constant.Const;
import org.gensokyo.data.generator.domain.WriterPO;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.util.TemplateKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JDBC数据库数据写入器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@Slf4j
public class JdbcWriter extends AbstractWriter {
    private static final String SQL_TEMPLATE = "INSERT INTO %s (%s) VALUES(%s)";
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public void setJdbcTemplate(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public JdbcWriter(final WriterPO wpo) {
        super(Objects.requireNonNull(wpo));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public long write(final List<Map<String, Object>> data) {
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(wpo.getDataSourceId()));
            var template = wpo.getTemplate();
            var sql = String.format(SQL_TEMPLATE, wpo.getTarget(), template, toPreparedStatement(template));
            int[] rows = jdbcTemplate.batchUpdate(sql, batchValues(Objects.requireNonNull(data)));
            return rows.length;
        } catch (Exception e) {
            log.error("写入数据库出现异常：", e);
            throw new DataGeneratorException("写入数据库出现异常", e);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    private String toPreparedStatement(String template) {
        return Splitter.on(Const.COMMA).splitToList(template)
                .stream()
                .map(String::trim)
                .map(Const.COLON::concat)
                .collect(Collectors.joining(Const.COMMA));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object>[] batchValues(List<Map<String, Object>> data) {
        return data.toArray(new Map[0]);
    }
}

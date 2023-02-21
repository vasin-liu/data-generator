/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.reader;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.generator.dataset.Dataset;
import org.gensokyo.data.generator.dataset.ReadableDataset;
import org.gensokyo.data.generator.domain.Context;
import org.gensokyo.data.generator.domain.ReaderPO;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.factory.ScriptFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Objects;

/**
 * JDBC数据库数据读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@Slf4j
public class JdbcReader extends AbstractReader {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public JdbcReader(final ReaderPO rpo, final ScriptFactory scriptFactory) {
        super(Objects.requireNonNull(rpo), Objects.requireNonNull(scriptFactory));
    }

    @Override
    public Dataset read(final Context ctx) {
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(rpo.getDataSourceId()));
            var re = jdbcTemplate.queryForList((String) Objects.requireNonNull(rpo.getDataSet()));
            var data = evalScript(ctx, List.copyOf(re));
            return ReadableDataset.of(data);
        } catch (Exception e) {
            throw new DataGeneratorException("读取数据库数据出现异常", e);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }
}

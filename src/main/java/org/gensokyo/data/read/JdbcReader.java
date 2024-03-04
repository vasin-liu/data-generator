/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.ReadStagePO;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;

/**
 * Jdbc数据读取类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
public class JdbcReader extends AbstractReader {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    protected JdbcReader(ReadStagePO.ReaderPO rpo) {
        super(rpo);
    }

    @Override
    public Value read(final Value input) {
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(rpo.getDataSourceId()));
            var re = jdbcTemplate.queryForList((String) Objects.requireNonNull(rpo.getDataSet()));
            return ListValue.fromMapList(re);
        } catch (Exception e) {
            throw new DataGeneratorException("读取数据库数据出现异常", e);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

}

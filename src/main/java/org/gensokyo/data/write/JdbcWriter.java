/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.write;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.google.common.base.Splitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.WriterContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.writer.JdbcWriterPO;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JDBC数据写入类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcWriter<T extends JdbcWriterPO> implements Writer<T> {
    private static final String SQL_TEMPLATE = "INSERT INTO %s (%s) VALUES(%s)";
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    @DSTransactional
    // 使用spring的事务注解会导致数据源切换失败
    // @Transactional(rollbackFor = Exception.class)
    @Override
    public long write(final WriterContext<T> ctx, final List<Map<String, Object>> dataset) {
        var wpo = ctx.writer();
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(wpo.getDataSourceId()));
            var template = wpo.getTemplate();
            var sql = String.format(SQL_TEMPLATE, wpo.getTarget(), template, toPreparedStatement(template));
            int[] rows = namedParameterJdbcTemplate.batchUpdate(sql, batchValues(Objects.requireNonNull(dataset)));
            return rows.length;
        } catch (Exception e) {
            throw new DataGeneratorException(String.format("写入数据集出现异常，数据库类型为：%s ，数据源编号为：%s ，目标表名为：%s，写入模板为：%s。",
                    wpo.getWriterType(), wpo.getDataSourceId(), wpo.getTarget(), wpo.getTemplate()), e);
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

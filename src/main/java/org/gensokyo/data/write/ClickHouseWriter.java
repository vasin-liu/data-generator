/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.write;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.clickhouse.jdbc.ClickHouseStatement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.WriterContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.writer.ClickHouseWriterPO;
import org.gensokyo.data.util.DatasetKit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ClickHouse文件流写入器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ClickHouseWriter<T extends ClickHouseWriterPO> implements Writer<T> {

    private static final String SQL_TEMPLATE = "INSERT INTO %s (%s) FORMAT CSV ";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public long write(final WriterContext<T> ctx, final List<Map<String, Object>> dataset) {
        Connection conn = null;
        var wpo = ctx.writer();
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(wpo.getDataSourceId()));
            //通过jdbcTemplate.getDataSource().getConnection()总是创建新的链接，不会复用已有链接，因此如果不手动关闭则会造成链接泄露
            //DataSourceUtils.getConnection()它首先查看当前是否存在事务管理上下文，并尝试从事务管理上下文获取连接，如果获取失败，
            //直接从数据源中获取连接。在获取连接后，如果当前拥有事务上下文，则将连接绑定到事务上下文中。如果处于事务上下文中，
            //那么不需要显示关闭或者释放连接，但是如果 DataSourceUtils 在没有事务上下文的方法中使用 getConnection() 获取连接，
            //依然会造成数据连接泄漏，因此需要手动释放：DataSourceUtils.releaseConnection(conn,jdbcTemplate.getDataSource())
            conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
            Statement stmt = conn.createStatement();
            if (stmt.isWrapperFor(ClickHouseStatement.class)) {
                ClickHouseStatement chs = stmt.unwrap(ClickHouseStatement.class);
                chs.write()
                        //.format(ClickHouseFormat.CSV)
                        //.use("pd_dts")
                        .query(String.format(SQL_TEMPLATE, wpo.getTarget(), wpo.getTemplate()))
                        .set("format_csv_delimiter", Const.VERTICAL)
                        .data(DatasetKit.buildBulkData(wpo.getTemplate(), dataset, Const.VERTICAL, "\"\""))
                        //.table(wpo.getTarget())
                        .executeAndWait();
                return dataset.size();
            }
        } catch (Exception e) {
            throw new DataGeneratorException(String.format("写入数据集出现异常，数据库类型为：%s ，数据源编号为：%s ，目标表名为：%s，写入模板为：%s。",
                    wpo.getType(), wpo.getDataSourceId(), wpo.getTarget(), wpo.getTemplate()), e);
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            DynamicDataSourceContextHolder.clear();
        }
        return 0L;

    }

}

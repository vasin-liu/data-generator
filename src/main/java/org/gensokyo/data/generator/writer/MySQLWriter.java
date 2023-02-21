/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.writer;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.generator.domain.WriterPO;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.util.DatasetKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MySQL文件流写入器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/1 , Version 1.0.0
 */
@Slf4j
public class MySQLWriter extends AbstractWriter {
    private static final String SQL_TEMPLATE = """
            LOAD DATA LOCAL INFILE 'data.csv' INTO TABLE %s 
            CHARACTER SET UTF8 FIELDS TERMINATED BY ',' 
            ENCLOSED BY '' ESCAPED BY '\\\\' LINES TERMINATED BY '\\n' STARTING BY '' (%s)
            """;
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MySQLWriter(WriterPO wpo) {
        super(wpo);
    }

    @SuppressWarnings({"resource"})
    @Override
    public long write(List<Map<String, Object>> data) {
        Connection conn = null;
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(wpo.getDataSourceId()));
            //通过jdbcTemplate.getDataSource().getConnection()总是创建新的链接，不会复用已有链接，因此如果不手动关闭则会造成链接泄露
            //DataSourceUtils.getConnection()它首先查看当前是否存在事务管理上下文，并尝试从事务管理上下文获取连接，如果获取失败，
            //直接从数据源中获取连接。在获取连接后，如果当前拥有事务上下文，则将连接绑定到事务上下文中。如果处于事务上下文中，
            //那么不需要显示关闭或者释放连接，但是如果 DataSourceUtils 在没有事务上下文的方法中使用 getConnection() 获取连接，
            //依然会造成数据连接泄漏，因此需要手动释放：DataSourceUtils.releaseConnection(conn,jdbcTemplate.getDataSource())
            conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
            //LOAD DATA LOCAL INFILE 'sql.csv' IGNORE INTO TABLE test.test (a,b,c,d,e,f)
            var sql = String.format(SQL_TEMPLATE, wpo.getTarget(), wpo.getTemplate());
            PreparedStatement ps = conn.prepareStatement(sql);
            if (ps.isWrapperFor(com.mysql.cj.jdbc.JdbcStatement.class)) {
                com.mysql.cj.jdbc.JdbcPreparedStatement jps = ps.unwrap(com.mysql.cj.jdbc.JdbcPreparedStatement.class);
                jps.setLocalInfileInputStream(DatasetKit.buildBulkData(wpo, data));
                return jps.executeUpdate();
            }
        } catch (Exception e) {
            log.error("写入数据库出现异常：", e);
            throw new DataGeneratorException("写入数据库出现异常", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            DynamicDataSourceContextHolder.clear();
        }
        return 0L;
    }
}

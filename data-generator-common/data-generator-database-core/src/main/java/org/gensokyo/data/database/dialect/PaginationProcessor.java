/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.database.dialect;

import org.gensokyo.data.database.DbType;
import org.gensokyo.data.database.SqlConsts;


/**
 * 分页处理器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/6 , Version 1.0.0
 */
public interface PaginationProcessor {


    /**
     * 处理构建 limit 和 offset
     *
     * @param dialect      数据方言
     * @param sql          已经构建的 sql
     * @param limit    用户传入的 limit 参数 可能为 null
     * @param offset  用户传入的 offset 参数，可能为 null
     */
    StringBuilder process(Dialect dialect, StringBuilder sql, Long limit, Long offset);


    /**
     * MySql 的处理器
     * 适合 {@link DbType#MYSQL,DbType#MARIADB,DbType#H2,DbType#CLICK_HOUSE,DbType#XCloud}
     */
    PaginationProcessor MYSQL = (dialect, sql, limit, offset) -> {
        if (limit != null && offset != null) {
            sql.append(SqlConsts.LIMIT).append(offset).append(SqlConsts.DELIMITER).append(limit);
        } else if (limit != null) {
            sql.append(SqlConsts.LIMIT).append(limit);
        }
        return sql;
    };
    /**
     * Postgresql 的处理器
     * 适合  {@link DbType#POSTGRE_SQL,DbType#SQLITE,DbType#H2,DbType#HSQL,DbType#KINGBASE_ES,DbType#PHOENIX}
     * 适合  {@link DbType#SAP_HANA,DbType#IMPALA,DbType#HIGH_GO,DbType#VERTICA,DbType#REDSHIFT}
     * 适合  {@link DbType#OPENGAUSS,DbType#TDENGINE,DbType#UXDB}
     */
    PaginationProcessor POSTGRESQL = (dialect, sql,  limit, offset) -> {
        if (limit != null && offset != null) {
            sql.append(SqlConsts.LIMIT).append(limit).append(SqlConsts.OFFSET).append(offset);
        } else if (limit != null) {
            sql.append(SqlConsts.LIMIT).append(limit);
        }
        return sql;
    };
    /**
     * derby 的处理器
     * 适合  {@link DbType#DERBY,DbType#ORACLE_12C,DbType#SQLSERVER ,DbType#POSTGRE_SQL}
     */
    PaginationProcessor DERBY = (dialect, sql,  limit, offset) -> {
        if (limit != null && offset != null) {
            // OFFSET ** ROWS FETCH NEXT ** ROWS ONLY")
            sql.append(SqlConsts.OFFSET).append(offset).append(SqlConsts.ROWS_FETCH_NEXT).append(limit).append(SqlConsts.ROWS_ONLY);
        } else if (limit != null) {
            sql.append(SqlConsts.OFFSET).append(0).append(SqlConsts.ROWS_FETCH_NEXT).append(limit).append(SqlConsts.ROWS_ONLY);
        }
        return sql;
    };
    /**
     * derby 的处理器
     * 适合  {@link DbType#DERBY,DbType#ORACLE_12C,DbType#SQLSERVER ,DbType#POSTGRE_SQL}
     */
    PaginationProcessor SQLSERVER = (dialect, sql,  limit, offset) -> {
        if (limit != null && offset != null) {
            // OFFSET ** ROWS FETCH NEXT ** ROWS ONLY")
            sql.append(SqlConsts.OFFSET).append(offset).append(SqlConsts.ROWS_FETCH_NEXT).append(limit).append(SqlConsts.ROWS_ONLY);
        } else if (limit != null) {
            sql.insert(6, SqlConsts.TOP + limit);
        }
        return sql;
    };
    /**
     * Informix 的处理器
     * 适合  {@link DbType#INFORMIX}
     * 文档 {@link <a href="https://www.ibm.com/docs/en/informix-servers/14.10?topic=clause-restricting-return-values-skip-limit-first-options">https://www.ibm.com/docs/en/informix-servers/14.10?topic=clause-restricting-return-values-skip-limit-first-options</a>}
     */
    PaginationProcessor INFORMIX = (dialect, sql,  limit, offset) -> {
        if (limit != null && offset != null) {
            // SELECT SKIP 2 FIRST 1 * FROM
            sql.insert(6, SqlConsts.SKIP + offset + SqlConsts.FIRST + limit);
        } else if (limit != null) {
            sql.insert(6, SqlConsts.FIRST + limit);
        }
        return sql;
    };
    /**
     * SINODB 的处理器
     * 适合  {@link DbType#SINODB}
     */
    PaginationProcessor SINODB = (dialect, sql,  limit, offset) -> {
        if (limit != null && offset != null) {
            // SELECT SKIP 2 FIRST 1 * FROM
            sql.insert(6, SqlConsts.SKIP + offset + SqlConsts.FIRST + limit);
        } else if (limit != null) {
            sql.insert(6, SqlConsts.FIRST + limit);
        }
        return sql;
    };
    /**
     * Firebird 的处理器
     * 适合  {@link DbType#FIREBIRD}
     */
    PaginationProcessor FIREBIRD = (dialect, sql,  limit, offset) -> {
        if (limit != null && offset != null) {
            // ROWS 2 TO 3
            sql.append(SqlConsts.ROWS).append(offset).append(SqlConsts.TO).append(offset + limit);
        } else if (limit != null) {
            sql.insert(6, SqlConsts.FIRST + limit);
        }
        return sql;
    };
    /**
     * Oracle11g及以下数据库的处理器
     * 适合  {@link DbType#ORACLE,DbType#DM,DbType#GAUSS}
     */
    PaginationProcessor ORACLE = (dialect, sql,  limit, offset) -> {
        if (limit != null) {
            if (offset == null) {
                offset = 0L;
            }
            StringBuilder newSql = new StringBuilder("SELECT * FROM (SELECT TEMP_DATAS.*, ROWNUM RN FROM (");
            newSql.append(sql);
            newSql.append(") TEMP_DATAS WHERE ROWNUM <= ")
                    .append(offset + limit)
                    .append(") WHERE RN > ")
                    .append(offset);
            return newSql;
        }
        return sql;
    };
    /**
     * Sybase 处理器
     * 适合  {@link DbType#SYBASE}
     */
    PaginationProcessor SYBASE = (dialect, sql,  limit, offset) -> {
        if (limit != null && offset != null) {
            //SELECT TOP 1 START AT 3 * FROM
            sql.insert(6, SqlConsts.TOP + limit + SqlConsts.START_AT + (offset + 1));
        } else if (limit != null) {
            sql.insert(6, SqlConsts.TOP + limit);
        }
        return sql;
    };

    PaginationProcessor DB2105 = (dialect, sql, limit, offset) -> {
        StringBuilder limitSqlFragment = new StringBuilder(
                "select * from ( select u_.*,rownumber() over()  as rn from ( ");
        limitSqlFragment.append(sql);
        limitSqlFragment.append(" )u_  ) temp_ where temp_.rn between ");

        if (limit != null && offset != null) {
            limitSqlFragment.append(offset + 1);
            limitSqlFragment.append(" and ");
            limitSqlFragment.append(limit + offset);
        } else if (limit != null) {
            limitSqlFragment.append("1 and ");
            limitSqlFragment.append(limit);
        } else {
            return sql;
        }
        return limitSqlFragment;
    };

}

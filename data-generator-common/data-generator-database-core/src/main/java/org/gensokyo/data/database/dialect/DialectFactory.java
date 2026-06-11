/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.database.dialect;


import org.gensokyo.data.database.DbType;
import org.gensokyo.data.database.dialect.impl.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 方言工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/6 , Version 1.0.0
 */
public class DialectFactory {

    private DialectFactory() {
    }

    /**
     * 数据库类型和方言的映射关系，可以通过其读取指定的方言，亦可能通过其扩展其他方言
     * 比如，在 mybatis-flex 实现的方言中有 bug 或者 有自己的独立实现，可以添加自己的方言实现到
     * 此 map 中，用于覆盖系统的方言实现
     */
    private static final Map<DbType, Dialect> DIALECT_MAP = new EnumMap<>(DbType.class);
    /**
     * 通过设置当前线程的数据库类型，以达到在代码执行时随时切换方言的功能
     */
    private static final ThreadLocal<DbType> DB_TYPE_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前线程的 dbType
     *
     * @param dbType 数据库类型
     */
    public static void setDbType(DbType dbType) {
        DB_TYPE_THREAD_LOCAL.set(dbType);
    }

    /**
     * 获取当前线程的 dbType
     *
     * @return dbType
     */
    public static DbType getDbType() {
        return DB_TYPE_THREAD_LOCAL.get();
    }


    /**
     * 清除当前线程的 dbType
     */
    public static void clearDbType() {
        DB_TYPE_THREAD_LOCAL.remove();
    }


    /**
     * 可以为某个 dbType 注册（新增或覆盖）自己的方言
     *
     * @param dbType  数据库类型
     * @param dialect 方言的实现
     */
    public static void registerDialect(DbType dbType, Dialect dialect) {
        DIALECT_MAP.put(dbType, dialect);
    }

    /**
     * 获取方言
     *
     * @return IDialect
     */
    public static Dialect getDialect() {
        DbType dbType = Objects.requireNonNull(DB_TYPE_THREAD_LOCAL.get());
        return DIALECT_MAP.computeIfAbsent(dbType, DialectFactory::createDialect);
    }

    private static Dialect createDialect(DbType dbType) {
        return switch (dbType) {
            case MYSQL, H2, MARIADB, GBASE, OSCAR, XUGU, OCEAN_BASE, CUBRID, GOLDILOCKS, CSIIDB, HIVE, DORIS ->
                    new CommonsDialectImpl(KeywordWrap.BACK_QUOTE, PaginationProcessor.MYSQL);
            case CLICK_HOUSE -> new ClickhouseDialectImpl(KeywordWrap.NONE, PaginationProcessor.MYSQL);
            case GBASE_8S -> new CommonsDialectImpl(KeywordWrap.NONE, PaginationProcessor.MYSQL);
            case DM -> new DmDialectImpl();
            case ORACLE -> new OracleDialectImpl();
            case GAUSS -> new CommonsDialectImpl(KeywordWrap.DOUBLE_QUOTATION, PaginationProcessor.ORACLE);
            case POSTGRE_SQL, SQLITE, HSQL, KINGBASE_ES, PHOENIX, SAP_HANA, IMPALA, HIGH_GO, VERTICA, REDSHIFT,
                 OPENGAUSS, UXDB, LEALONE ->
                    new CommonsDialectImpl(KeywordWrap.DOUBLE_QUOTATION, PaginationProcessor.POSTGRESQL);
            case TDENGINE -> new CommonsDialectImpl(KeywordWrap.BACK_QUOTE, PaginationProcessor.POSTGRESQL);
            case ORACLE_12C -> new OracleDialectImpl(PaginationProcessor.DERBY);
            case FIREBIRD, DB2 -> new CommonsDialectImpl(KeywordWrap.NONE, PaginationProcessor.DERBY);
            case DB2_1005 -> new OracleDialectImpl(KeywordWrap.NONE, PaginationProcessor.DB2105);
            case SQLSERVER -> new SqlserverDialectImpl(KeywordWrap.SQUARE_BRACKETS, PaginationProcessor.SQLSERVER);
            case INFORMIX -> new CommonsDialectImpl(KeywordWrap.NONE, PaginationProcessor.INFORMIX);
            case SINODB -> new CommonsDialectImpl(KeywordWrap.DOUBLE_QUOTATION, PaginationProcessor.SINODB);
            case SYBASE -> new CommonsDialectImpl(KeywordWrap.DOUBLE_QUOTATION, PaginationProcessor.SYBASE);
            default -> new CommonsDialectImpl();
        };
    }
}

/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.database.dialect.impl;

import org.gensokyo.data.database.dialect.PaginationProcessor;
import org.gensokyo.data.database.dialect.KeywordWrap;

import java.util.Set;

/**
 * Sqlserver方言实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/6 , Version 1.0.0
 */
public class SqlserverDialectImpl extends CommonsDialectImpl {

    public static final Set<String> KEYWORDS = Set.of("ADD", "ALL", "ALTER", "AND", "ANY", "AS",
            "ASC", "BACKUP", "BEGIN", "BETWEEN", "BREAK", "BROWSE", "BULK", "BY", "CASCADE", "CASE", "CHECK", "CHECKPOINT",
            "CLOSE", "CLUSTERED", "COALESCE", "COLUMN", "COMMIT", "COMMITTED", "COMPUTE", "CONFIRM", "CONNECT",
            "CONSTRAINT", "CONTAINS", "CONTAINSTABLE", "CONTINUE", "CONVERT", "CREATE", "CROSS", "CURRENT", "CURRENT_DATE",
            "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATABASE", "DBCC", "DEALLOCATE", "DECLARE",
            "DEFAULT", "DELETE", "DENY", "DESC", "DISTINCT", "DISTRIBUTED", "DOUBLE", "DROP", "DUMP", "ELSE", "END",
            "ERRLVL", "ESCAPE", "EXCEPT", "EXEC", "EXECUTE", "EXISTS", "EXIT", "EXTERNAL", "FETCH", "FILE", "FILLFACTOR",
            "FOR", "FOREIGN", "FREETEXT", "FREETEXTTABLE", "FROM", "FULL", "FUNCTION", "GOTO", "GRANT", "GROUP", "HAVING",
            "HOLDLOCK", "IDENTITY", "IDENTITY_INSERT", "IDENTITYCOL", "IF", "IN", "INDEX", "INNER", "INSERT", "INTERSECT",
            "INTO", "IS", "JOIN", "KEY", "KILL", "LEFT", "LIKE", "LINENO", "LOAD", "MAX", "MIN", "NATIONAL", "NOCHECK",
            "NONCLUSTERED", "NOT", "NULL", "NULLIF", "OF", "OFF", "OFFSETS", "ON", "OPEN", "OPENDATASOURCE", "OPENQUERY",
            "OPENROWSET", "OPTION", "OR", "ORDER", "OUTER", "OVER", "PERCENT", "PIPE", "PLAN", "PRECISION", "PREPARE",
            "PRIMARY", "PRINT", "PRIVILEGES", "PROC", "PROCEDURE", "PUBLIC", "RAISERROR", "READ", "READTEXT", "RECONFIGURE",
            "REFERENCES", "REPEATABLE", "RESTORE", "RESTRICT", "RETURN", "REVOKE", "RIGHT", "ROLLBACK", "ROWCOUNT",
            "ROWGUIDCOL", "RULE", "SAVE", "SCHEMA", "SECURITYAUDIT", "SELECT", "SEMANTICKEYPHRASETABLE",
            "SEMANTICSIMILARITYDETAILSTABLE", "SEMANTICSIMILARITYTABLE", "SESSION_USER", "SET", "SETUSER", "SHUTDOWN",
            "SOME", "STATISTICS", "SYSTEM_USER", "TABLE", "TABLESAMPLE", "TEXTSIZE", "THEN", "TO", "TOP", "TRAN",
            "TRANSACTION", "TRIGGER", "TRUNCATE", "TSEQUAL", "UNION", "UNIQUE", "UNPIVOT", "UPDATE", "UPDATETEXT", "USE",
            "USER", "USING", "VALUES", "VARYING", "VIEW", "WAITFOR", "WHEN", "WHERE", "WHILE", "WITH", "WITHIN GROUP",
            "WRITETEXT", "XACT_ABORT", "XLOCK");

    public SqlserverDialectImpl() {
        this(PaginationProcessor.SQLSERVER);
    }

    public SqlserverDialectImpl(PaginationProcessor paginationProcessor) {
        this(new KeywordWrap(true, false, KEYWORDS, "[", "]"), paginationProcessor);
    }

    public SqlserverDialectImpl(KeywordWrap keywordWrap, PaginationProcessor paginationProcessor) {
        super(keywordWrap, paginationProcessor);
    }
}

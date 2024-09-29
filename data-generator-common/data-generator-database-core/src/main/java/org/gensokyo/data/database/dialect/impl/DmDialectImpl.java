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
 * 达梦方言实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/6 , Version 1.0.0
 */
public class DmDialectImpl extends CommonsDialectImpl {

    //https://docs.oracle.com/cd/A97630_01/appdev.920/a42525/apb.htm
    public static final Set<String> KEYWORDS = Set.of(
            "MODIFY", "EXCLUSIVE", "NOAUDIT", "SESSION", "FILE", "NOTFOUND", "SHARE", "NOWAIT", "SQLBUF", "SUCCESSFUL", "AUDIT",
            "OFFLINE", "SYNONYM", "SYSDATE", "IMMEDIATE", "ONLINE", "INCREMENT", "TRIGGER", "COLUMN", "INITIAL", "UID", "COMMENT",
            "PRIVILEGES", "RAW", "USER", "VALIDATE", "LOCK", "ROW",
            "LONG", "MAXEXTENTS", "WHENEVER", "ROWS", "ADMIN", "FOUND", "MOUNT", "AFTER", "CYCLE",
            "NEXT", "ALLOCATE", "GO", "ANALYZE", "DATAFILE", "NOARCHIVELOG",
            "ARCHIVE", "GROUPS", "NOCACHE", "ARCHIVELOG", "DEC", "INCLUDING", "NOCYCLE",
            "NOMAXVALUE", "DISABLE", "INITRANS", "NOMINVALUE", "BACKUP", "DISMOUNT",
            "INSTANCE", "NONE", "DOUBLE", "INT", "NOORDER", "BECOME", "DUMP", "KEY", "NORESETLOGS", "BEFORE",
            "EACH", "LANGUAGE", "NORMAL", "BLOCK", "ENABLE", "LAYER", "NOSORT", "LINK", "NUMERIC", "CACHE",
            "ESCAPE", "LISTS", "OFF", "CANCEL", "EVENTS", "LOGFILE", "OLD", "CASCADE", "EXCEPT", "MANAGE", "ONLY", "CHANGE",
            "EXCEPTIONS", "MANUAL", "CHARACTER", "EXEC", "OPTIMAL", "CHECKPOINT", "EXPLAIN", "MAXDATAFILES",
            "OWN", "EXECUTE", "MAXINSTANCES", "COBOL", "EXTENT", "MAXLOGFILES", "PARALLEL",
            "EXTERNALLY", "MAXLOGHISTORY", "PCTINCREASE", "COMPILE", "MAXLOGMEMBERS", "PCTUSED", "CONSTRAINT",
            "FLUSH", "MAXTRANS", "PLAN", "CONSTRAINTS", "FREELIST", "MAXVALUE", "PLI", "CONTENTS", "FREELISTS",
            "PRECISION", "CONTINUE", "FORCE", "MINEXTENTS", "PRIMARY", "CONTROLFILE", "FOREIGN", "MINVALUE",
            "FORTRAN", "MODULE", "PROFILE", "SQLSTATE", "TRACING", "QUOTA",
            "STATEMENT_ID", "TRANSACTION", "READ", "SCN", "STATISTICS", "TRIGGERS", "SECTION", "STOP", "TRUNCATE",
            "RECOVER", "SEGMENT", "STORAGE", "UNDER", "REFERENCES", "SEQUENCE", "UNLIMITED", "REFERENCING", "SHARED",
            "SWITCH", "UNTIL", "RESETLOGS", "SNAPSHOT", "SYSTEM", "RESTRICTED", "SOME", "USING", "REUSE",
            "SORT", "TABLESPACE", "ROLE", "TEMPORARY", "WRITE", "ROLES", "THREAD",
            "SQLERROR", "TIME", "ABORT", "BETWEEN", "CRASH", "DIGITS", "ACCEPT", "BINARY_INTEGER", "CREATE", "DISPOSE", "ACCESS",
            "BODY", "CURRENT", "DISTINCT", "ADD", "BOOLEAN", "CURRVAL", "DO", "ALL", "BY", "CURSOR", "DROP", "ALTER", "CASE", "DATABASE",
            "ELSE", "AND", "CHAR", "DATA_BASE", "ELSIF", "ANY", "CHAR_BASE", "DATE", "END", "ARRAY", "CHECK", "DBA", "ENTRY", "ARRAYLEN",
            "CLOSE", "DEBUGOFF", "EXCEPTION", "AS", "CLUSTER", "DEBUGON", "EXCEPTION_INIT", "ASC", "CLUSTERS", "DECLARE", "EXISTS",
            "ASSERT", "COLAUTH", "DECIMAL", "EXIT", "ASSIGN", "COLUMNS", "DEFAULT", "FALSE", "AT", "COMMIT", "DEFINITION", "FETCH",
            "AUTHORIZATION", "COMPRESS", "DELAY", "FLOAT", "AVG", "CONNECT", "DELETE", "FOR", "BASE_TABLE", "CONSTANT", "DELTA", "FORM",
            "BEGIN", "COUNT", "DESC", "FROM", "FUNCTION", "NEW", "RELEASE", "SUM", "GENERIC", "NEXTVAL", "REMR", "TABAUTH",
            "GOTO", "NOCOMPRESS", "RENAME", "TABLE", "GRANT", "NOT", "RESOURCE", "TABLES", "GROUP", "NULL", "RETURN", "TASK", "HAVING",
            "NUMBER", "REVERSE", "TERMINATE", "IDENTIFIED", "NUMBER_BASE", "REVOKE", "THEN", "IF", "OF", "ROLLBACK", "TO", "IN", "ON",
            "ROWID", "TRUE", "INDEX", "OPEN", "ROWLABEL", "TYPE", "INDEXES", "OPTION", "ROWNUM", "UNION", "INDICATOR", "OR", "ROWTYPE",
            "UNIQUE", "INSERT", "ORDER", "RUN", "UPDATE", "INTEGER", "OTHERS", "SAVEPOINT", "USE", "INTERSECT", "OUT", "SCHEMA", "VALUES",
            "INTO", "PACKAGE", "SELECT", "VARCHAR", "IS", "PARTITION", "SEPARATE", "VARCHAR2", "LEVEL", "PCTFREE", "SET", "VARIANCE",
            "LIKE", "POSITIVE", "SIZE", "VIEW", "LIMITED", "PRAGMA", "SMALLINT", "VIEWS", "LOOP", "PRIOR", "SPACE", "WHEN", "MAX", "PRIVATE",
            "SQL", "WHERE", "MIN", "PROCEDURE", "SQLCODE", "WHILE", "MINUS", "PUBLIC", "SQLERRM", "WITH", "MLSLABEL", "RAISE", "START",
            "WORK", "MOD", "RANGE", "STATEMENT", "XOR", "MODE", "REAL", "STDDEV", "NATURAL", "RECORD", "SUBTYPE", "GEN", "KP", "L",
            "NA", "NC", "ND", "NL", "NM", "NR", "NS", "NT", "NZ", "TTC", "UPI", "O", "S", "XA"
    );

    public DmDialectImpl() {
        //达梦 默认情况下，是支持 MySQL 的分页语法的
        this(PaginationProcessor.MYSQL);
    }

    public DmDialectImpl(PaginationProcessor paginationProcessor) {
        //只有以上的关键字时，会添加 "" 包裹
        this(new KeywordWrap(false, false, KEYWORDS, "\"", "\""), paginationProcessor);
    }

    public DmDialectImpl(KeywordWrap keywordWrap, PaginationProcessor paginationProcessor) {
        super(keywordWrap, paginationProcessor);
    }
}
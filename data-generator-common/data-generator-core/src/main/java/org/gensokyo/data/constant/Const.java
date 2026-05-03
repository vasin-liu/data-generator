/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.constant;

/**
 * 全局常量定义
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 1.0.0
 */
public final class Const {
    private Const() {
        throw new UnsupportedOperationException();
    }

    public static final int NEGATIVE_ONE = -1;
    public static final int AMOUNT = 1000;
    public static final int BATCH_SIZE = 1000;

    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String DOT = ".";
    public static final String VERTICAL = "|";
    public static final String NULL = "\\N";
    public static final String LF = "\n";

    public static final String SCRIPT_VAR_FAKER = "faker";
    public static final String SCRIPT_VAR_DATASET = "dataset";
    public static final String SCRIPT_VAR_ARGS = "args";

    public static final String R_OK = "成功";
    public static final String R_FAIL = "失败";

    public static final String DEFAULT_ZONE_ID = "Asia/Shanghai";

    public interface File {
        String UTF8_BOM = "\uFEFF";
    }

    public interface StageType {
        String READ = "READ";

        String SELECT = "SELECT";

        String SCRIPT = "SCRIPT";

        String WRITE = "WRITE";
    }

    public interface ScriptType {
        String PLAIN = "PLAIN";
    }

    public interface ReaderType {
        String CONSTANT = "CONSTANT";
    }

    public interface WriterType {
        String CONSOLE = "CONSOLE";

        String JDBC = "JDBC";

        String CSV = "CSV";

        String JSON = "JSON";
    }

    public interface ReaderSelectStrategyType {
        String EQUAL = "EQUAL";

        String WEIGHT = "WEIGHT";
    }

    public interface ValueSelectStrategyType {
        String REPEAT_RANDOM = "REPEAT_RANDOM";

        String REPEAT_ORDER = "REPEAT_ORDER";

        String ONCE_RANDOM = "ONCE_RANDOM";

        String ONCE_ORDER = "ONCE_ORDER";

        String MULTIPLE_ORDER = "MULTIPLE_ORDER";
    }
}

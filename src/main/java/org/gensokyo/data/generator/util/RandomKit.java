/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.util;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 随机工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/5 , Version 1.0.0
 */
public final class RandomKit {

    private static final String ALPHA_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final char[] ALPHA_UPPER_CHAR = ALPHA_UPPER.toCharArray();
    private static final String ALPHA_LOWER = ALPHA_UPPER.toLowerCase(Locale.ROOT);
    private static final char[] ALPHA_LOWER_CHAR = ALPHA_UPPER.toLowerCase(Locale.ROOT).toCharArray();
    private static final String DIGITS = "0123456789";
    private static final char[] DIGITS_CHAR = DIGITS.toCharArray();
    private static final String ALPHA = ALPHA_UPPER + ALPHA_LOWER;
    private static final char[] ALPHA_CHAR = ALPHA.toCharArray();
    private static final String ALPHA_NUMERIC = ALPHA_UPPER + ALPHA_LOWER + DIGITS;
    private static final char[] ALPHA_NUMERIC_CHAR = ALPHA_NUMERIC.toCharArray();
    private static final SecureRandom random = new SecureRandom();
    private static final SnowFlake snowFlake = new SnowFlake(1, 1);

    private RandomKit() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public static <T> T choiceOne(List<T> data) {
        if (Objects.isNull(data) || data.isEmpty()) {
            return null;
        }
        int idx = random.nextInt(data.size());
        return data.get(idx);
    }

    public static String str(String symbols, int length) {
        return str(Objects.requireNonNull(symbols).toCharArray(), length);
    }

    public static String str(char[] symbols, int length) {
        if (length < 1) {
            throw new IllegalArgumentException();
        }
        if (symbols.length < 2) {
            throw new IllegalArgumentException();
        }
        char[] buf = new char[length];
        for (int idx = 0; idx < length; ++idx) {
            buf[idx] = symbols[random.nextInt(symbols.length)];
        }
        return new String(buf);
    }

    public static String alpha(int length) {
        return str(ALPHA_CHAR, length);
    }

    public static String alphaUpper(int length) {
        return str(ALPHA_UPPER_CHAR, length);
    }

    public static String alphaLower(int length) {
        return str(ALPHA_LOWER_CHAR, length);
    }

    public static String alphanumeric(int length) {
        return str(ALPHA_NUMERIC_CHAR, length);
    }

    public static String numeric(int length) {
        return str(DIGITS_CHAR, length);
    }

    public static String numeric(int origin, int bound) {
        return String.valueOf(random.nextInt(origin, bound));
    }

    public static long id() {
        return snowFlake.nextId();
    }
}

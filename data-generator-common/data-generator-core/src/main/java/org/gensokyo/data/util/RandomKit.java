/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.util;

import org.jspecify.annotations.Nullable;
import org.gensokyo.data.constant.Chinese;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * 随机工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/5 , Version 1.0.0
 */
public class RandomKit {

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
    protected static final SecureRandom RANDOM = new SecureRandom();
    protected static final SnowFlake SNOW_FLAKE = new SnowFlake(1, 1);

    protected RandomKit() {
        throw new UnsupportedOperationException();
    }

    public static SecureRandom random() {
        return RANDOM;
    }

    public static SnowFlake snowFlake() {
        return SNOW_FLAKE;
    }

    @Nullable
    public static <T> T choiceOne(Collection<T> data) {
        if (Objects.isNull(data) || data.isEmpty()) {
            return null;
        }
        List<T> list = List.copyOf(data);
        int idx = RANDOM.nextInt(list.size());
        return list.get(idx);
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
            buf[idx] = symbols[RANDOM.nextInt(symbols.length)];
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
        return String.valueOf(RANDOM.nextInt(origin, bound));
    }

    public static long id() {
        return SNOW_FLAKE.nextId();
    }

    public static int nextInt(int origin, int bound) {
        return RANDOM.nextInt(origin, bound);
    }

    public static String text(int minLength, int maxLength) {
        int length = RANDOM.nextInt(minLength, maxLength);
        int size = Chinese.WORD.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(Chinese.WORD[RANDOM.nextInt(size)]);
        }

        return sb.toString();
    }

    public static List<Integer> seq(int start, int end) {
        return seq(start, end, 1);
    }

    public static List<Integer> seq(int start, int end, int step) {
        if (start > end) {
            throw new IllegalArgumentException("start must be less than or equal to end");
        }
        AtomicInteger idx = new AtomicInteger(start);
        return IntStream.range(start, end + 1)
                .map(i -> idx.getAndAdd(step))
                .boxed()
                .toList();
    }
}

/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;
import org.gensokyo.kit.collect.CollectKit;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 接口响应结果
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/29 , Version 1.0.0
 */
@Getter
public class R<T> implements Serializable {

    private final int code;
    private final String message;
    private final boolean success;
    private final T data;

    private R(int code, String message, boolean success, T data) {
        this.code = code;
        this.message = message;
        this.success = success;
        this.data = data;
    }

    public static <E> Ok<E> ok(String message, E data) {
        return new Ok<>(message, data);
    }

    public static <E> Ok<E> ok(E data) {
        return new Ok<>(data);
    }

    public static <E> Ok<E> ok(String message) {
        return new Ok<>(message, null);
    }

    public static <E> Failure<E> fail(String message, E data) {
        return new Failure<>(message, data);
    }

    public static <E> Failure<E> fail(E data) {
        return new Failure<>(data);
    }

    public static <E> Failure<E> fail(String message) {
        return new Failure<>(message, null);
    }

    public static <E> Failure<E> fail() {
        return new Failure<>();
    }

    @Getter
    @Setter
    public static class Ok<E> extends R<E> {

        private Ok(String message, E data) {
            super(HttpStatus.OK.value(), message, true, data);
        }

        private Ok(E data) {
            this(Const.R_OK, data);
        }
    }

    @Getter
    @Setter
    public static class Failure<E> extends R<E> {

        private Failure(String message, E data) {
            super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, false, data);
        }

        private Failure(E data) {
            this(Const.R_FAIL, data);
        }

        private Failure() {
            this(null);
        }
    }

    @Getter
    @Setter
    public static class Page<E> extends R<List<E>> {

        protected List<E> records = Collections.emptyList();

        /**
         * 总数
         */
        protected long total = 0;
        /**
         * 每页显示条数，默认 10
         */
        protected long size = 10;

        /**
         * 当前页
         */
        protected long current = 1;

        private Page(long current, long size, long total, String message, List<E> data) {
            super(HttpStatus.OK.value(), message, true, data);
            this.current = current;
            this.size = size;
            this.total = total;
            if (CollectKit.isNotEmpty(data)) {
                records.addAll(data);
            }
        }

        private Page(long current, long size, long total, List<E> data) {
            this(current, size, total, Const.R_OK, data);
        }

        private Page(long current, long total, List<E> data) {
            this(current, 10, total, data);
        }
    }
}

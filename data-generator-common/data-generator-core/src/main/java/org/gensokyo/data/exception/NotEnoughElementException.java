/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.exception;

import java.util.function.Supplier;

/**
 * 没有足够的元素异常
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
public class NotEnoughElementException extends RuntimeException implements Supplier<NotEnoughElementException> {

    public NotEnoughElementException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEnoughElementException(String message) {
        super(message);
    }

    public NotEnoughElementException(Throwable cause) {
        super(cause);
    }

    @Override
    public NotEnoughElementException get() {
        return this;
    }
}

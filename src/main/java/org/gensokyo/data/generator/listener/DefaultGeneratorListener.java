/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.listener;

/**
 * 默认生成器监听器实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/29 , Version 1.0.0
 */
public class DefaultGeneratorListener implements GeneratorListener {
    @Override
    public void onReady() {
        //do nothing
    }

    @Override
    public void onProcessing(Long count) {
        //do nothing
    }

    @Override
    public void onComplete(Long total) {
        //do nothing
    }

    @Override
    public void onError(Throwable ex) {
        //do nothing
    }
}

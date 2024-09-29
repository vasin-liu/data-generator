/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.faker;

import net.datafaker.providers.base.AbstractProvider;
import net.datafaker.providers.base.BaseProviders;
import org.gensokyo.data.util.RandomKit;

import java.util.List;
import java.util.Objects;

/**
 * 雪花算法
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/2 , Version 1.0.0
 */
public class SnowflakeProvider extends AbstractProvider<BaseProviders> {
    private final List<String> viidBaseType = List.of("01", "02", "03", "99");
    private final List<String> viidSemanticType = List.of("01", "02", "03", "04", "05", "06", "07", "99");

    public SnowflakeProvider(BaseProviders faker) {
        super(faker);
    }

    public long next() {
        return RandomKit.id();
    }

    public String viid(String deviceId, String baseType, String passTime, String semanticType) {
        return Objects.requireNonNull(deviceId)
               + checkBaseType(baseType)
               + passTime
               + RandomKit.numeric(1, 99999)
               + checkSemanticType(semanticType)
               + RandomKit.numeric(1, 99999);
    }

    private String checkBaseType(String baseType) {
        if (viidBaseType.contains(baseType)) {
            return baseType;
        }
        throw new UnsupportedOperationException();
    }

    private String checkSemanticType(String semanticType) {
        if (viidSemanticType.contains(semanticType)) {
            return semanticType;
        }
        throw new UnsupportedOperationException();
    }
}

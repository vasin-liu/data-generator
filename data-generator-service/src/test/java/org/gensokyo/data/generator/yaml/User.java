/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.yaml;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/11 , Version 1.0.0
 */
@Getter
@Setter
public class User {
    private String name;
    private int age;
    private Pet pet;
}

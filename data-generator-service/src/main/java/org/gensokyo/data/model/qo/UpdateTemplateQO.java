/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.qo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 模板更新请求
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/17 , Version 1.0.0
 */
@Data
public class UpdateTemplateQO implements Serializable {

    @NotNull(message = "文件编号不能为空")
    private Long id;

    @NotBlank(message = "文件内容不能为空")
    private String yaml;
}

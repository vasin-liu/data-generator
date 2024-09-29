/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

/**
 * 模板数据传输对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/17 , Version 1.0.0
 */
@Data
@NoArgsConstructor
public class TemplateDTO implements Serializable {

    public TemplateDTO(TemplatePO entity) {
        Assert.notNull(entity, "参数 'entity' 不能为空");
        BeanUtils.copyProperties(entity, this, "jsonContent");
        var vo = JsonKit.read(entity.getJsonContent(), TemplateVO.class);
        this.setJsonContent(vo);
    }

    private Long id;

    private String name;

    private String fileName;

    private String fileExt;

    private TemplateVO jsonContent;

    private String yamlContent;

    private String status;


}

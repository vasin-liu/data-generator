/*
 * Copyright 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 */
package org.gensokyo.data.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.kit.Assert;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class TemplateDTO implements Serializable {

    public TemplateDTO(TemplatePO entity) {
        Assert.notNull(entity, "Parameter 'entity' must not be null");
        BeanUtils.copyProperties(entity, this, "contentJson");
        this.contentJson = decode(entity.getContentJson());
    }

    private String id;

    private String name;

    private String fileName;

    private String fileExt;

    private String pathMd5;

    private String contentMd5;

    private Object contentJson;

    private String contentYaml;

    private Object decode(String contentJson) {
        try {
            return TemplateJsonCodec.read(contentJson);
        } catch (Exception ignored) {
            return TemplateJsonCodec.read(contentJson, TemplateV2DraftVO.class);
        }
    }
}

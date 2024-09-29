/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.data.model.vo.stage.ParamVO;

import java.util.List;

/**
 * 数据库迭代器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/16 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(IteratorVO.class)
@JsonSubType(value = "DATABASE")
public class DatabaseIteratorVO extends IteratorVO {

    /**
     * 参数配置
     */
    private List<ParamVO> params;

    /**
     * 数据源ID，数据集所在数据源的唯一标识
     */
    private String dataSourceId;

    /**
     * 分页查询语句
     */
    private String sql;

    /**
     * 当前页码数
     */
    private int pageIndex = 1;

    /**
     * 每页记录数
     */
    private int pageSize = 100;

    /**
     * 最大记录数
     */
    private long maxRows = Const.AMOUNT;
}

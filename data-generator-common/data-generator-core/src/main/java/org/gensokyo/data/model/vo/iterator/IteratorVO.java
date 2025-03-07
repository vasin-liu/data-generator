/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.iterator;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.model.vo.condition.OtherwiseVO;
import org.gensokyo.data.model.vo.condition.WhenVO;
import org.gensokyo.data.model.vo.stage.StageVO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 因子配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/16 , Version 1.0.0
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME
        , include = JsonTypeInfo.As.EXISTING_PROPERTY
        , property = "type"
        , visible = true
        // 反序列化时，如果没有匹配到子类，则使用默认实现类，即NumberIteratorVO，
        // 除了NumberIteratorVO，其他子类都需要在配置时指定type属性
        //,defaultImpl = NumberIteratorVO.class
)
public class IteratorVO implements Serializable {

    /**
     * 迭代器类型
     */
    private String type;

    /**
     * 数据行处理阶段
     */
    private List<StageVO> stages = new ArrayList<>();

    /**
     * 嵌套迭代器配置，默认为数字迭代器
     */
    private IteratorVO iterator;

    /**
     * 条件分支列表
     */
    private List<WhenVO> choose;

    /**
     * 其他条件
     */
    private OtherwiseVO otherwise;

    /**
     * 每次迭代暂停时间，单位：秒，默认为0
     */
    private Integer pause = 0;
}

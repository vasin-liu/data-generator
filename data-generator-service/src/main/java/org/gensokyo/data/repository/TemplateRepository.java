/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.repository;

import org.gensokyo.data.model.po.TemplatePO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 模板仓库接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/17 , Version 1.0.0
 */
public interface TemplateRepository extends JpaRepository<TemplatePO, Long> {

    List<TemplatePO> findByName(String name);

    List<TemplatePO> findByNameContaining(String name);

    List<TemplatePO> findByNameLike(String name);

    List<TemplatePO>findByNameStartingWith(String prefix);

    List<TemplatePO> findByNameEndingWith(String suffix);
}

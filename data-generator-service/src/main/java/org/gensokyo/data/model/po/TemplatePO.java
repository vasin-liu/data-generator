/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 模板存储类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/17 , Version 1.0.0
 */
@Getter
@Setter
@Entity
@Table(name = "template")
public class TemplatePO implements Serializable {

    /**
     * 路径MD5字符串
     */
    @Id
    private Long id;

    /**
     * 模板属性名称
     */
    @Column(name = "name")
    private String name;

    /**
     * 文件名称
     */
    @Column(name = "file_name")
    private String fileName;

    /**
     * 文件后缀
     */
    @Column(name = "file_ext")
    private String fileExt;

    /**
     * 文件路径MD5字符串
     */
    @Column(name = "path_md5")
    private String pathMd5;

    /**
     * 文件内容MD5字符串
     */
    @Column(name = "content_md5")
    private String contentMd5;

    /**
     * 文件内容，JSON格式
     */
    @Column(columnDefinition = "CLOB", name = "content_json")
    private String contentJson;

    /**
     * 文件内容，YAML格式
     */
    @Column(columnDefinition = "CLOB", name = "content_yaml")
    private String contentYaml;
}

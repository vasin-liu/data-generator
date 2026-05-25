/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * Persisted JDBC datasource definition for operator console (D1).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Getter
@Setter
@Entity
@Table(name = "datasource_config")
public class DataSourceConfigPO implements Serializable {

    @Id
    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "url", length = 1024, nullable = false)
    private String url;

    @Column(name = "username", length = 256)
    private String username;

    @Column(name = "password", length = 256)
    private String password;

    @Column(name = "driver_class_name", length = 512, nullable = false)
    private String driverClassName;

    @Column(name = "driver_jar_path", length = 1024)
    private String driverJarPath;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

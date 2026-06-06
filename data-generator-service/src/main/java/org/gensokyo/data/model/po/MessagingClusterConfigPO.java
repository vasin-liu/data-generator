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
 * Persisted Kafka or Elasticsearch cluster definition (operator console).
 *
 * @author Gensokyo
 * @since 2026-06-04
 */
@Getter
@Setter
@Entity
@Table(name = "messaging_cluster_config")
public class MessagingClusterConfigPO implements Serializable {

    @Id
    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "cluster_type", length = 32, nullable = false)
    private String clusterType;

    @Column(name = "config_json", columnDefinition = "CLOB", nullable = false)
    private String configJson;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

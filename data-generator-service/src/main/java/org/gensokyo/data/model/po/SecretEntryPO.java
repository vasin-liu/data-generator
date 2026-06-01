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
 * Persisted secret value for {@code passwordSecretRef} resolution.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@Entity
@Table(name = "secret_entry")
public class SecretEntryPO implements Serializable {

    @Id
    @Column(name = "name", length = 256)
    private String name;

    @Column(name = "secret_value", length = 2048, nullable = false)
    private String secretValue;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

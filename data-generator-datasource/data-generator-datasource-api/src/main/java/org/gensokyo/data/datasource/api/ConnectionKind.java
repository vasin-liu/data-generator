/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

/**
 * Discriminator for entries in the unified connection catalog namespace (D-01, D-02).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public enum ConnectionKind {

    /** JDBC relational datasource. */
    JDBC,

    /** Apache Kafka cluster producer/consumer endpoint. */
    KAFKA,

    /** Elasticsearch REST cluster endpoint. */
    ELASTICSEARCH
}

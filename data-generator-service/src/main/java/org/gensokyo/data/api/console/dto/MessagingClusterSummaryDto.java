/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.messaging.MessagingClusterConfigService;

import java.util.List;
import java.util.Map;

/**
 * Console row for a persisted Kafka or Elasticsearch cluster.
 *
 * @author Gensokyo
 * @since 2026-06-04
 */
public record MessagingClusterSummaryDto(
        String name,
        String clusterType,
        Boolean enabled,
        String updatedAt,
        List<String> bootstrapServers,
        List<String> uris,
        String username,
        String clientId,
        String acks,
        String compressionType,
        Integer retries,
        String securityProtocol,
        String saslMechanism,
        Map<String, String> properties,
        String pathPrefix,
        Integer connectionTimeoutMs,
        Integer socketTimeoutMs,
        Boolean socketKeepAlive,
        Boolean hasSaslJaasConfig,
        Boolean hasPassword,
        Boolean hasApiKey) {

    /**
     * @param summary service summary
     * @return API DTO
     */
    public static MessagingClusterSummaryDto from(MessagingClusterConfigService.MessagingClusterSummary summary) {
        return new MessagingClusterSummaryDto(
                summary.name(),
                summary.clusterType(),
                summary.enabled(),
                summary.updatedAt() != null ? summary.updatedAt().toString() : null,
                summary.bootstrapServers(),
                summary.uris(),
                summary.username(),
                summary.clientId(),
                summary.acks(),
                summary.compressionType(),
                summary.retries(),
                summary.securityProtocol(),
                summary.saslMechanism(),
                summary.properties(),
                summary.pathPrefix(),
                summary.connectionTimeoutMs(),
                summary.socketTimeoutMs(),
                summary.socketKeepAlive(),
                summary.hasSaslJaasConfig(),
                summary.hasPassword(),
                summary.hasApiKey());
    }
}

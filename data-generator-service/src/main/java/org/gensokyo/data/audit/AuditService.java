/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.audit;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.AuditEventPO;
import org.gensokyo.data.repository.AuditEventRepository;
import org.gensokyo.data.security.ConsoleActorHolder;
import org.gensokyo.data.util.RandomKit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Append-only audit log for operator actions.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository repository;

    /**
     * Records an audit event.
     *
     * @param action       action code (e.g. TEMPLATE_PUBLISH)
     * @param resourceType resource category
     * @param resourceId   resource identifier
     * @param detail       optional structured detail (never include secrets)
     */
    @Transactional
    public void record(String action, String resourceType, String resourceId, Map<String, Object> detail) {
        AuditEventPO row = new AuditEventPO();
        row.setId(RandomKit.snowFlake().nextId());
        row.setOccurredAt(Instant.now());
        row.setActor(ConsoleActorHolder.currentActor());
        row.setAction(action);
        row.setResourceType(resourceType);
        row.setResourceId(resourceId);
        if (detail != null && !detail.isEmpty()) {
            row.setDetailJson(TemplateJsonCodec.write(detail));
        }
        repository.saveAndFlush(row);
    }
}

/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.model.po.UdfArtifactPO;
import org.gensokyo.data.repository.UdfArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Re-enters persisted published UDFs into the Template V2 runtime merge view on startup (D-02).
 *
 * <p>Because the active registry is {@link JdbcUdfRegistry}, which reads straight from the database, no
 * record copy is needed on reload — the rehydration is simply a {@code refresh()} of the runtime registry
 * provider, exactly as a fresh publish triggers. On {@link ApplicationReadyEvent} this reloader logs the
 * count of {@code PUBLISHED} rows (never their payload) and refreshes the runtime so persisted UDFs become
 * resolvable without re-upload. The provider is injected {@link Nullable} so minimal/test contexts without a
 * runtime provider skip the refresh safely.
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
@Component
public class UdfStartupReloader {

    private static final Logger log = LoggerFactory.getLogger(UdfStartupReloader.class);

    private final UdfArtifactRepository repository;
    private final TemplateV2RuntimeRegistryProvider runtimeRegistryProvider;

    /**
     * @param repository              artifact repository used to count published rows for the reload log
     * @param runtimeRegistryProvider refreshable runtime registry (nullable in minimal contexts)
     */
    public UdfStartupReloader(UdfArtifactRepository repository,
                              @Nullable TemplateV2RuntimeRegistryProvider runtimeRegistryProvider) {
        this.repository = repository;
        this.runtimeRegistryProvider = runtimeRegistryProvider;
    }

    /**
     * Refreshes the runtime registry once the application is ready so persisted published UDFs re-enter the
     * Template V2 merge view (D-02).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void reloadPublishedUdfs() {
        List<UdfArtifactPO> published = repository.findByState(UdfLifecycleState.PUBLISHED.name());
        log.info("UDF startup reload: {} published artifact(s) re-entering the runtime merge view", published.size());
        if (runtimeRegistryProvider != null) {
            runtimeRegistryProvider.refresh();
        }
    }
}

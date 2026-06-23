/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.model.po.UdfArtifactPO;
import org.gensokyo.data.repository.UdfArtifactRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

/**
 * Unit tests for {@link UdfStartupReloader}: the ready-event refresh path and its null-provider safety (D-02).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
class UdfStartupReloaderTests {

    @Test
    void refreshesRuntimeOnceForPublishedRows() {
        UdfArtifactRepository repository = Mockito.mock(UdfArtifactRepository.class);
        TemplateV2RuntimeRegistryProvider provider = Mockito.mock(TemplateV2RuntimeRegistryProvider.class);
        UdfArtifactPO published = new UdfArtifactPO();
        published.setUdfId("com.example.sql");
        published.setVersion("1.0.0");
        published.setType(UdfType.SQL.jsonName());
        published.setState(UdfLifecycleState.PUBLISHED.name());
        Mockito.when(repository.findByState(UdfLifecycleState.PUBLISHED.name()))
                .thenReturn(List.of(published));

        UdfStartupReloader reloader = new UdfStartupReloader(repository, provider);
        reloader.reloadPublishedUdfs();

        Mockito.verify(provider, Mockito.times(1)).refresh();
    }

    @Test
    void nullProviderIsNoOp() {
        UdfArtifactRepository repository = Mockito.mock(UdfArtifactRepository.class);
        Mockito.when(repository.findByState(UdfLifecycleState.PUBLISHED.name())).thenReturn(List.of());

        UdfStartupReloader reloader = new UdfStartupReloader(repository, null);
        Assertions.assertDoesNotThrow(reloader::reloadPublishedUdfs);
    }
}

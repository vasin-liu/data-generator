/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.cache;

import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link Templates} classpath reload (no Spring context).
 *
 * @author Gensokyo
 * @since 2026-05-28
 */
@ExtendWith(MockitoExtension.class)
class TemplatesClasspathReloadTest {

    @Mock
    private TemplateRepository repository;

    @Test
    void reloadAll_parsesBuiltinClasspathTemplates() {
        Templates templates = new Templates(new DataGeneratorProperties(), new JacksonParser(), repository);
        when(repository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<TemplatePO> loaded = templates.reloadAll();

        assertTrue(loaded.size() >= 50, "expected built-in templates, got " + loaded.size());
        ArgumentCaptor<List<TemplatePO>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertTrue(captor.getValue().size() >= 50);
        verify(repository).deleteAll();
    }
}

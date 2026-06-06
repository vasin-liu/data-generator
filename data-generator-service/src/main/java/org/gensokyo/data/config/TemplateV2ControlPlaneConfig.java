/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.config;

import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateV2ControlPlaneService;
import org.gensokyo.data.template.TemplateV2DefinitionResolver;
import org.gensokyo.data.template.TemplateV2PlanExplainService;
import org.gensokyo.data.yaml.YamlParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring beans for Template V2 control-plane operations (validate, explain, preview).
 *
 * @author Gensokyo
 * @since 2026-06-06
 */
@Configuration
public class TemplateV2ControlPlaneConfig {

    /**
     * Template V2 control-plane orchestration (validate, explain, preview).
     *
     * @param repository              template persistence
     * @param yamlParser              YAML parser
     * @param templateV2Runner        bounded V2 runner
     * @param dataGeneratorProperties service properties
     * @return control-plane service
     */
    @Bean
    @ConditionalOnMissingBean(TemplateV2ControlPlaneService.class)
    public TemplateV2ControlPlaneService templateV2ControlPlaneService(
            TemplateRepository repository,
            YamlParser yamlParser,
            TemplateV2Runner templateV2Runner,
            DataGeneratorProperties dataGeneratorProperties) {
        return new TemplateV2ControlPlaneService(
                repository,
                yamlParser,
                new TemplateV2DefinitionResolver(yamlParser),
                new TemplateV2PlanExplainService(),
                templateV2Runner,
                dataGeneratorProperties);
    }
}

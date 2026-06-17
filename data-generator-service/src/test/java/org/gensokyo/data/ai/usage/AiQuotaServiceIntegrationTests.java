/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.calcite.runtime.AiExecutionScope;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.repository.AiQuotaDailyUsageRepository;
import org.gensokyo.data.repository.AiQuotaScopeDailyUsageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for {@link AiQuotaService}.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class AiQuotaServiceIntegrationTests {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-12T10:15:30Z");

    @Autowired
    private AiQuotaDailyUsageRepository platformRepository;

    @Autowired
    private AiQuotaScopeDailyUsageRepository scopedRepository;

    @Autowired
    private AiPricingService pricingService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DataGeneratorProperties properties;

    @AfterEach
    void cleanup() {
        platformRepository.deleteAll();
        scopedRepository.deleteAll();
        properties.getAiRuntime().getQuota().setEnabled(false);
        properties.getAiRuntime().getQuota().setMaxCallsPerDay(0L);
        properties.getAiRuntime().getQuota().setMaxTokensPerDay(0L);
        properties.getAiRuntime().getQuota().setAlertsEnabled(false);
        properties.getAiRuntime().getQuota().setWebhooksEnabled(false);
        properties.getAiRuntime().getQuota().setWebhooks(new java.util.ArrayList<>());
        properties.getAiRuntime().getQuota().setScopeOverrides(new java.util.ArrayList<>());
        AiExecutionScope.clear();
    }

    @Test
    void enforcesDailyCallQuotaAcrossServiceInstances() {
        properties.getAiRuntime().getQuota().setEnabled(true);
        properties.getAiRuntime().getQuota().setMaxCallsPerDay(1L);

        AiQuotaService quotaService = new AiQuotaService(
                properties,
                platformRepository,
                scopedRepository,
                pricingService,
                mock(AuditService.class),
                mock(AiQuotaWebhookNotifier.class),
                transactionTemplate,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        quotaService.beforeCall("OLLAMA");
        quotaService.recordUsage("OLLAMA", "qwen2", 10L, 5L);

        AiQuotaExceededException failure = Assertions.assertThrows(
                AiQuotaExceededException.class,
                () -> quotaService.beforeCall("OLLAMA"));
        Assertions.assertTrue(failure.getMessage().contains("call quota exceeded"));
    }

    @Test
    void statusReportsRemainingQuota() {
        properties.getAiRuntime().getQuota().setEnabled(true);
        properties.getAiRuntime().getQuota().setMaxCallsPerDay(5L);
        properties.getAiRuntime().getQuota().setMaxTokensPerDay(100L);

        AiQuotaService quotaService = new AiQuotaService(
                properties,
                platformRepository,
                scopedRepository,
                pricingService,
                mock(AuditService.class),
                mock(AiQuotaWebhookNotifier.class),
                transactionTemplate,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        quotaService.beforeCall("OLLAMA");
        quotaService.recordUsage("OLLAMA", "qwen2", 20L, 10L);

        var status = quotaService.status();

        Assertions.assertTrue(status.enabled());
        Assertions.assertEquals("2026-06-12", status.usageDate());
        Assertions.assertEquals(1L, status.usedCalls());
        Assertions.assertEquals(4L, status.remainingCalls());
        Assertions.assertEquals(70L, status.remainingTokens());
    }

    @Test
    void enforcesProviderScopedCallQuota() {
        properties.getAiRuntime().getQuota().setEnabled(true);
        DataGeneratorProperties.AiQuotaScopeOverride override = new DataGeneratorProperties.AiQuotaScopeOverride();
        override.setScopeType("PROVIDER");
        override.setScopeKey("OPENAI");
        override.setMaxCallsPerDay(1L);
        properties.getAiRuntime().getQuota().setScopeOverrides(java.util.List.of(override));

        AiQuotaService quotaService = new AiQuotaService(
                properties,
                platformRepository,
                scopedRepository,
                pricingService,
                mock(AuditService.class),
                mock(AiQuotaWebhookNotifier.class),
                transactionTemplate,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        quotaService.beforeCall("OPENAI");
        Assertions.assertThrows(AiQuotaExceededException.class, () -> quotaService.beforeCall("OPENAI"));
        Assertions.assertDoesNotThrow(() -> quotaService.beforeCall("OLLAMA"));
    }

    @Test
    void enforcesTemplateScopedCallQuota() {
        properties.getAiRuntime().getQuota().setEnabled(true);
        DataGeneratorProperties.AiQuotaScopeOverride override = new DataGeneratorProperties.AiQuotaScopeOverride();
        override.setScopeType("TEMPLATE");
        override.setScopeKey("9001");
        override.setMaxCallsPerDay(1L);
        properties.getAiRuntime().getQuota().setScopeOverrides(java.util.List.of(override));

        AiQuotaService quotaService = new AiQuotaService(
                properties,
                platformRepository,
                scopedRepository,
                pricingService,
                mock(AuditService.class),
                mock(AiQuotaWebhookNotifier.class),
                transactionTemplate,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        try {
            AiExecutionScope.bind(9001L, "demo-template");
            quotaService.beforeCall("OPENAI");
            Assertions.assertThrows(AiQuotaExceededException.class, () -> quotaService.beforeCall("OPENAI"));
        }
        finally {
            AiExecutionScope.clear();
        }
    }

    @Test
    void emitsAuditWarnWhenCrossingThreshold() {
        properties.getAiRuntime().getQuota().setEnabled(true);
        properties.getAiRuntime().getQuota().setAlertsEnabled(true);
        properties.getAiRuntime().getQuota().setWarnAtPercent(50);
        properties.getAiRuntime().getQuota().setMaxCallsPerDay(2L);
        AuditService auditService = mock(AuditService.class);

        AiQuotaService quotaService = new AiQuotaService(
                properties,
                platformRepository,
                scopedRepository,
                pricingService,
                auditService,
                mock(AiQuotaWebhookNotifier.class),
                transactionTemplate,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        quotaService.beforeCall("OLLAMA");
        verify(auditService, atLeastOnce()).record(eq("AI_QUOTA_WARN"), eq("AI_QUOTA"), eq(AiQuotaScopeKeys.PLATFORM), org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    void enforcesTenantScopedCallQuota() {
        properties.getAiRuntime().getQuota().setEnabled(true);
        DataGeneratorProperties.AiQuotaScopeOverride override = new DataGeneratorProperties.AiQuotaScopeOverride();
        override.setScopeType("TENANT");
        override.setScopeKey("acme");
        override.setMaxCallsPerDay(1L);
        properties.getAiRuntime().getQuota().setScopeOverrides(java.util.List.of(override));

        AiQuotaService quotaService = new AiQuotaService(
                properties,
                platformRepository,
                scopedRepository,
                pricingService,
                mock(AuditService.class),
                mock(AiQuotaWebhookNotifier.class),
                transactionTemplate,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        try {
            AiExecutionScope.bind(9001L, "demo-template", "acme");
            quotaService.beforeCall("OPENAI");
            Assertions.assertThrows(AiQuotaExceededException.class, () -> quotaService.beforeCall("OPENAI"));
        }
        finally {
            AiExecutionScope.clear();
        }
    }

    @Test
    void dispatchesWebhookWhenQuotaWarns() {
        properties.getAiRuntime().getQuota().setEnabled(true);
        properties.getAiRuntime().getQuota().setWebhooksEnabled(true);
        properties.getAiRuntime().getQuota().setWarnAtPercent(50);
        properties.getAiRuntime().getQuota().setMaxCallsPerDay(2L);
        DataGeneratorProperties.AiQuotaWebhookEndpoint endpoint = new DataGeneratorProperties.AiQuotaWebhookEndpoint();
        endpoint.setUrl("http://localhost:9999/quota-hook");
        properties.getAiRuntime().getQuota().setWebhooks(java.util.List.of(endpoint));
        AiQuotaWebhookNotifier webhookNotifier = mock(AiQuotaWebhookNotifier.class);

        AiQuotaService quotaService = new AiQuotaService(
                properties,
                platformRepository,
                scopedRepository,
                pricingService,
                mock(AuditService.class),
                webhookNotifier,
                transactionTemplate,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        quotaService.beforeCall("OLLAMA");
        verify(webhookNotifier, atLeastOnce()).notifyEvent(any(AiQuotaAlertEvent.class));
    }
}

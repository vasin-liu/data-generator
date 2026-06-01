/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Cron helpers for {@link TaskScheduleService}.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
final class TaskScheduleSupport {

    private TaskScheduleSupport() {
    }

    /**
     * Validates and parses a Spring six-field cron expression.
     *
     * @param cronExpression cron text
     * @return parsed expression
     */
    static CronExpression parseCron(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new IllegalArgumentException("cronExpression must not be blank");
        }
        try {
            return CronExpression.parse(cronExpression.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression, e);
        }
    }

    /**
     * Computes the next fire time strictly after {@code after}.
     *
     * @param cronExpression cron text
     * @param after          lower bound instant
     * @return next trigger instant
     */
    static Instant nextTriggerAfter(String cronExpression, Instant after) {
        CronExpression cron = parseCron(cronExpression);
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime cursor = ZonedDateTime.ofInstant(after == null ? Instant.now() : after, zone);
        ZonedDateTime next = cron.next(cursor);
        if (next == null) {
            throw new IllegalArgumentException("Cron expression has no next execution: " + cronExpression);
        }
        return next.toInstant();
    }
}

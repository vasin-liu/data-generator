/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.DistributedQueueMetricsDto;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.task.DistributedJobMetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only distributed execution metrics for the operator console.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@RestController
@RequestMapping("/api/console/distributed")
@RequiredArgsConstructor
public class ConsoleDistributedController {

    private final DistributedJobMetricsService distributedJobMetricsService;

    /**
     * @return queue depth and active worker snapshot
     */
    @GetMapping("/metrics")
    public R<DistributedQueueMetricsDto> metrics() {
        return R.ok(distributedJobMetricsService.queueMetrics());
    }
}

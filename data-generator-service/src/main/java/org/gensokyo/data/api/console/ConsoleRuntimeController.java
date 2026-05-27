/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.gensokyo.data.api.console.dto.ConsoleRuntimeDto;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.vo.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Console shell runtime metadata (feature flags for the React UI).
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@RestController
@RequestMapping("/api/console")
public class ConsoleRuntimeController {

    private final DataGeneratorProperties properties;
    private final DynamicRoutingDataSource dynamicRoutingDataSource;

    /**
     * @param properties              application flags
     * @param dynamicRoutingDataSource optional JDBC registry for editor dropdowns
     */
    public ConsoleRuntimeController(
            DataGeneratorProperties properties,
            @Autowired(required = false) DynamicRoutingDataSource dynamicRoutingDataSource) {
        this.properties = properties;
        this.dynamicRoutingDataSource = dynamicRoutingDataSource;
    }

    /**
     * @return flags for navbar / home (e.g. V1 execution enabled)
     */
    @GetMapping("/runtime")
    public R<ConsoleRuntimeDto> runtime() {
        return R.ok(new ConsoleRuntimeDto(properties.isV1ExecutionEnabled()));
    }

    /**
     * @return JDBC datasource keys for editor source/sink dropdowns
     */
    @GetMapping("/jdbc-names")
    public R<List<String>> jdbcNames() {
        if (dynamicRoutingDataSource == null || dynamicRoutingDataSource.getDataSources() == null) {
            return R.ok(Collections.emptyList());
        }
        Set<String> keys = dynamicRoutingDataSource.getDataSources().keySet();
        return R.ok(keys.stream().sorted().toList());
    }
}

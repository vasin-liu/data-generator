/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the embedded React operator console at {@code /console/**} with SPA fallback.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@Configuration
public class ConsoleWebConfig implements WebMvcConfigurer {

    private static final String CONSOLE_RESOURCE_LOCATION = "classpath:/static/console/";

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/console/");
        registry.addRedirectViewController("/console", "/console/");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/console/**")
                .addResourceLocations(CONSOLE_RESOURCE_LOCATION)
                .resourceChain(true)
                .addResolver(new SpaFallbackResourceResolver());
    }

    /**
     * Returns {@code index.html} for client-side routes when no static file matches.
     */
    private static final class SpaFallbackResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resolved = super.getResource(resourcePath, location);
            if (resolved != null) {
                return resolved;
            }
            // Deep links such as /console/templates must not 404 before React Router runs.
            return super.getResource("index.html", location);
        }
    }
}

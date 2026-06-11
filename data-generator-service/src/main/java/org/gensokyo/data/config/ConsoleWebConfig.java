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
        // Include "/console/" explicitly: PathPattern "/console/**" alone may not match the root URL.
        registry.addResourceHandler("/console", "/console/", "/console/**")
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
            if (resourcePath != null && !resourcePath.isBlank()) {
                Resource requested = location.createRelative(resourcePath);
                if (isReadableResource(requested)) {
                    return requested;
                }
                // Client routes such as /console/templates have no file extension.
                if (!resourcePath.contains(".")) {
                    return indexHtml(location);
                }
                return null;
            }
            // GET /console/ resolves to an empty path; classpath folder exists but is not a file.
            return indexHtml(location);
        }

        private static boolean isReadableResource(Resource resource) throws IOException {
            // Do not use isFile(): classpath entries inside a JAR are readable but not "files" on disk.
            return resource != null && resource.exists() && resource.isReadable();
        }

        private static Resource indexHtml(Resource location) throws IOException {
            Resource index = location.createRelative("index.html");
            return isReadableResource(index) ? index : null;
        }
    }
}

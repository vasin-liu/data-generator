/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.security;

import jakarta.servlet.FilterChain;
import org.gensokyo.data.config.ConsoleSecurityProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link ConsoleAuthorizationFilter} when RBAC is enabled.
 *
 * @author Gensokyo
 * @since 2026-06-02
 */
class ConsoleAuthorizationFilterTest {

    @Test
    void missingRoleHeader_returnsForbiddenWhenEnabled() throws Exception {
        ConsoleSecurityProperties properties = new ConsoleSecurityProperties();
        properties.setEnabled(true);
        ConsoleAuthorizationFilter filter = new ConsoleAuthorizationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/templates/scenarios");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> Assertions.fail("Filter chain must not run without role header");

        filter.doFilterInternal(request, response, chain);

        Assertions.assertEquals(403, response.getStatus());
    }

    @Test
    void viewerCannotCreateTemplateWhenEnabled() throws Exception {
        ConsoleSecurityProperties properties = new ConsoleSecurityProperties();
        properties.setEnabled(true);
        ConsoleAuthorizationFilter filter = new ConsoleAuthorizationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/templates");
        request.addHeader("X-Console-Role", "VIEWER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> Assertions.fail("Filter chain must not run for VIEWER create");

        filter.doFilterInternal(request, response, chain);

        Assertions.assertEquals(403, response.getStatus());
    }

    @Test
    void viewerCanReadScenariosWhenEnabled() throws Exception {
        ConsoleSecurityProperties properties = new ConsoleSecurityProperties();
        properties.setEnabled(true);
        ConsoleAuthorizationFilter filter = new ConsoleAuthorizationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/templates/scenarios");
        request.addHeader("X-Console-Role", "VIEWER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = { false };
        FilterChain chain = (req, res) -> chainInvoked[0] = true;

        filter.doFilterInternal(request, response, chain);

        Assertions.assertTrue(chainInvoked[0]);
        Assertions.assertEquals(200, response.getStatus());
    }

    @Test
    void editorCannotPublishWhenEnabled() throws Exception {
        ConsoleSecurityProperties properties = new ConsoleSecurityProperties();
        properties.setEnabled(true);
        ConsoleAuthorizationFilter filter = new ConsoleAuthorizationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/templates/42/publish");
        request.addHeader("X-Console-Role", "EDITOR");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> Assertions.fail("Filter chain must not run for EDITOR publish");

        filter.doFilterInternal(request, response, chain);

        Assertions.assertEquals(403, response.getStatus());
    }

    @Test
    void disabledFilterSkipsAuthorization() throws Exception {
        ConsoleSecurityProperties properties = new ConsoleSecurityProperties();
        properties.setEnabled(false);
        ConsoleAuthorizationFilter filter = new ConsoleAuthorizationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/templates/scenarios");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = { false };
        FilterChain chain = (req, res) -> chainInvoked[0] = true;

        Assertions.assertTrue(filter.shouldNotFilter(request));
        filter.doFilter(request, response, chain);

        Assertions.assertTrue(chainInvoked[0]);
    }
}

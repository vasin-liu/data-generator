/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.ConnectionTestRequest;
import org.gensokyo.data.datasource.api.ConnectionTestResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified {@link ConnectionCatalogImpl#test(ConnectionTestRequest)} for JDBC, Kafka, and ES (D-18..D-20).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class ConnectionCatalogTestTests {

    private static EmbeddedKafkaBroker kafkaBroker;

    @Autowired
    private ConnectionCatalogImpl connectionCatalog;

    @BeforeAll
    static void startEmbeddedKafka() {
        kafkaBroker = new EmbeddedKafkaKraftBroker(1, 1);
        kafkaBroker.afterPropertiesSet();
    }

    @AfterAll
    static void stopEmbeddedKafka() {
        if (kafkaBroker != null) {
            kafkaBroker.destroy();
        }
    }

    @Test
    void jdbcDraftTest_succeedsAgainstEmbeddedH2() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("url", "jdbc:h2:mem:catalog-test-jdbc;DB_CLOSE_DELAY=-1");
        payload.put("username", "sa");
        payload.put("password", "");
        payload.put("driverClassName", "org.h2.Driver");

        ConnectionTestResult result = connectionCatalog.test(
                ConnectionTestRequest.forDraft(ConnectionKind.JDBC, payload));

        Assertions.assertTrue(result.success());
        Assertions.assertFalse(result.message().isBlank());
        Assertions.assertFalse(result.message().toLowerCase().contains("password"));
    }

    @Test
    void jdbcDraftTest_failureHasActionableMessageWithoutSecrets() {
        Map<String, Object> payload = Map.of(
                "url", "jdbc:h2:mem:catalog-test-bad;DB_CLOSE_DELAY=-1",
                "username", "sa",
                "password", "wrong-secret",
                "driverClassName", "org.h2.Driver");

        ConnectionTestResult result = connectionCatalog.test(
                ConnectionTestRequest.forDraft(ConnectionKind.JDBC, payload));

        // H2 may accept empty/wrong password — force unreachable host instead.
        payload = Map.of(
                "url", "jdbc:h2:tcp://127.0.0.1:59999/nonexistent;DB_CLOSE_DELAY=-1",
                "username", "sa",
                "password", "s3cr3t",
                "driverClassName", "org.h2.Driver");
        result = connectionCatalog.test(ConnectionTestRequest.forDraft(ConnectionKind.JDBC, payload));

        Assertions.assertFalse(result.success());
        Assertions.assertFalse(result.message().isBlank());
        Assertions.assertFalse(result.message().contains("s3cr3t"));
        Assertions.assertFalse(result.message().contains("wrong-secret"));
    }

    @Test
    void jdbcDraftTest_dmDriverFailureIsActionableWithoutSecrets() {
        assertProprietaryDriverFailureWithoutSecrets(
                "jdbc:dm://127.0.0.1:59999/YOUR_SCHEMA",
                "dm.jdbc.driver.DmDriver");
    }

    @Test
    void jdbcDraftTest_kingbaseDriverFailureIsActionableWithoutSecrets() {
        assertProprietaryDriverFailureWithoutSecrets(
                "jdbc:kingbase8://127.0.0.1:59999/YOUR_DATABASE",
                "com.kingbase8.Driver");
    }

    @Test
    void jdbcDraftTest_highgoDriverFailureIsActionableWithoutSecrets() {
        assertProprietaryDriverFailureWithoutSecrets(
                "jdbc:highgo://127.0.0.1:59999/highgo",
                "com.highgo.jdbc.Driver");
    }

    @Test
    void jdbcDraftTest_failureStripsJdbcUrlUserinfo() {
        Map<String, Object> payload = Map.of(
                "url", "jdbc:dm://operator:s3cr3t@127.0.0.1:59999/YOUR_SCHEMA",
                "username", "operator",
                "password", "s3cr3t",
                "driverClassName", "dm.jdbc.driver.DmDriver");

        ConnectionTestResult result = connectionCatalog.test(
                ConnectionTestRequest.forDraft(ConnectionKind.JDBC, payload));

        Assertions.assertFalse(result.success());
        Assertions.assertFalse(result.message().isBlank());
        Assertions.assertFalse(result.message().contains("s3cr3t"));
        Assertions.assertFalse(result.message().contains("operator:s3cr3t@"));
    }

    private void assertProprietaryDriverFailureWithoutSecrets(String url, String driverClassName) {
        Map<String, Object> payload = Map.of(
                "url", url,
                "username", "operator",
                "password", "s3cr3t",
                "driverClassName", driverClassName);

        ConnectionTestResult result = connectionCatalog.test(
                ConnectionTestRequest.forDraft(ConnectionKind.JDBC, payload));

        Assertions.assertFalse(result.success());
        Assertions.assertFalse(result.message().isBlank());
        Assertions.assertFalse(result.message().contains("s3cr3t"));
        Assertions.assertTrue(
                result.message().contains("unreachable")
                        || result.message().contains("connectivity")
                        || result.message().contains("failed")
                        || result.message().contains(driverClassName));
        Object detailsDriver = result.details().get("driverClassName");
        Assertions.assertEquals(driverClassName, detailsDriver);
    }

    @Test
    void kafkaDraftTest_succeedsAgainstEmbeddedBroker() {
        Map<String, Object> payload = Map.of(
                "bootstrapServers", List.of(kafkaBroker.getBrokersAsString()));

        ConnectionTestResult result = connectionCatalog.test(
                ConnectionTestRequest.forDraft(ConnectionKind.KAFKA, payload));

        Assertions.assertTrue(result.success(), result.message());
    }

    @Test
    void kafkaDraftTest_failureWhenBrokerUnreachable() {
        Map<String, Object> payload = Map.of(
                "bootstrapServers", List.of("127.0.0.1:59998"));

        ConnectionTestResult result = connectionCatalog.test(
                ConnectionTestRequest.forDraft(ConnectionKind.KAFKA, payload));

        Assertions.assertFalse(result.success());
        Assertions.assertFalse(result.message().isBlank());
    }

    @Test
    void elasticsearchDraftTest_succeedsAgainstHttpStub() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", ConnectionCatalogTestTests::handleEsPing);
        server.start();
        int port = server.getAddress().getPort();
        try {
            Map<String, Object> payload = Map.of("uris", List.of("http://127.0.0.1:" + port));

            ConnectionTestResult result = connectionCatalog.test(
                    ConnectionTestRequest.forDraft(ConnectionKind.ELASTICSEARCH, payload));

            Assertions.assertTrue(result.success(), result.message());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void elasticsearchDraftTest_failureWhenHostUnreachable() {
        Map<String, Object> payload = Map.of("uris", List.of("http://127.0.0.1:59997"));

        ConnectionTestResult result = connectionCatalog.test(
                ConnectionTestRequest.forDraft(ConnectionKind.ELASTICSEARCH, payload));

        Assertions.assertFalse(result.success());
        Assertions.assertFalse(result.message().isBlank());
    }

    private static void handleEsPing(HttpExchange exchange) throws IOException {
        byte[] body = "{\"cluster_name\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}

/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClientBuilder;
import org.gensokyo.boot.elasticsearch.config.MultipleElasticsearchProperties;
import org.gensokyo.boot.elasticsearch.config.MultiplePropertiesCredentialsProvider;
import org.gensokyo.boot.elasticsearch.config.MultipleRestClientBuilderCustomizer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * @author Gensokyo V.L.
 * @since 2023/1/14 , Version
 */
@Slf4j
public class SslCertificateMultipleRestClientBuilderCustomizer implements MultipleRestClientBuilderCustomizer {

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private final String cluster;
    private final MultipleElasticsearchProperties props;

    public SslCertificateMultipleRestClientBuilderCustomizer(String cluster, MultipleElasticsearchProperties props) {
        this.cluster = cluster;
        this.props = props;
    }

    @Override
    public void customize(RestClientBuilder builder) {
        try {
            Resource resource = resolver.getResource(String.format("classpath:certs/%s/ca.crt", cluster));
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate trustedCa;
            try (InputStream is = Files.newInputStream(resource.getFile().toPath())) {
                trustedCa = factory.generateCertificate(is);
            }
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", trustedCa);
            SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, null);
            final SSLContext sslContext = sslContextBuilder.build();
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.disableAuthCaching()
                    .setDefaultCredentialsProvider(new MultiplePropertiesCredentialsProvider(this.props))
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Scope scope() {
        return Scope.SINGLE;
    }

    @Override
    public String cluster() {
        return cluster;
    }
}

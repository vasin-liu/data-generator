package org.gensokyo.data.generator;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.boot.elasticsearch.MultipleElasticsearchAutoConfiguration;
import org.gensokyo.boot.elasticsearch.config.MultipleElasticsearchClusterProperties;
import org.gensokyo.boot.elasticsearch.config.MultipleRestClientBuilderCustomizer;
import org.gensokyo.boot.kafka.config.MultipleKafkaClusterAutoConfiguration;
import org.gensokyo.data.generator.elasticsearch.BasicCertificateMultipleRestClientBuilderCustomizer;
import org.gensokyo.data.generator.elasticsearch.SslCertificateMultipleRestClientBuilderCustomizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootApplication
@AutoConfigureAfter(value = {MultipleElasticsearchAutoConfiguration.class, MultipleKafkaClusterAutoConfiguration.class})
public class DataGeneratorApplication {

    @Bean
    public List<MultipleRestClientBuilderCustomizer> customizers(MultipleElasticsearchClusterProperties p) {
        List<MultipleRestClientBuilderCustomizer> list = new ArrayList<>();
        list.add(new SslCertificateMultipleRestClientBuilderCustomizer("es1", p.getClusters().get("es1")));
        list.add(new BasicCertificateMultipleRestClientBuilderCustomizer("es2", p.getClusters().get("es2")));
        return list;
    }

    public static void main(String[] args) {
        SpringApplication.run(DataGeneratorApplication.class, args);
    }
}

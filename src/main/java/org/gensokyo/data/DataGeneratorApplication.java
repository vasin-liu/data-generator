package org.gensokyo.data;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.boot.elasticsearch.MultipleElasticsearchAutoConfiguration;
import org.gensokyo.boot.elasticsearch.config.MultipleElasticsearchClusterProperties;
import org.gensokyo.boot.elasticsearch.config.MultipleRestClientBuilderCustomizer;
import org.gensokyo.boot.kafka.config.MultipleKafkaClusterAutoConfiguration;
import org.gensokyo.data.elasticsearch.BasicCertificateMultipleRestClientBuilderCustomizer;
import org.gensokyo.data.elasticsearch.SslCertificateMultipleRestClientBuilderCustomizer;
import org.gensokyo.kit.collect.MapKit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据生成器入口
 *
 * @author Gensokyo V.L.
 * @since 2023/1/9 , Version 1.0.0
 */
@Slf4j
@SpringBootApplication
@AutoConfigureAfter(value = {MultipleElasticsearchAutoConfiguration.class, MultipleKafkaClusterAutoConfiguration.class})
public class DataGeneratorApplication {

    @Bean
    public List<MultipleRestClientBuilderCustomizer> customizers(MultipleElasticsearchClusterProperties p) {
        List<MultipleRestClientBuilderCustomizer> list = new ArrayList<>();
        if (MapKit.isNotEmpty(p.getClusters())) {
            list.add(new SslCertificateMultipleRestClientBuilderCustomizer("es1", p.getClusters().get("es1")));
            list.add(new BasicCertificateMultipleRestClientBuilderCustomizer("es2", p.getClusters().get("es2")));
        }
        return list;
    }

    public static void main(String[] args) {
        SpringApplication.run(DataGeneratorApplication.class, args);
    }
}

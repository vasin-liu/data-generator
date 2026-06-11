package org.gensokyo.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据生成器入口
 *
 * @author Gensokyo V.L.
 * @since 2023/1/9 , Version 1.0.0
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class DataGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataGeneratorApplication.class, args);
    }
}

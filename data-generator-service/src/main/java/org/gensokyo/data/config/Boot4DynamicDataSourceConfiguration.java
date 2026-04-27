package org.gensokyo.data.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.BasicDataSourceCreator;
import com.baomidou.dynamic.datasource.creator.DataSourceCreator;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.baomidou.dynamic.datasource.creator.DruidDataSourceCreator;
import com.baomidou.dynamic.datasource.creator.JndiDataSourceCreator;
import com.baomidou.dynamic.datasource.event.EncDataSourceInitEvent;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(DynamicDataSourceProperties.class)
public class Boot4DynamicDataSourceConfiguration {

    @Bean
    @Primary
    public DynamicRoutingDataSource dataSource(DynamicDataSourceProperties properties) {
        DynamicRoutingDataSource dataSource = new DynamicRoutingDataSource();
        dataSource.setPrimary(properties.getPrimary());
        dataSource.setStrict(properties.getStrict());
        dataSource.setP6spy(properties.getP6spy());
        dataSource.setSeata(properties.getSeata());
        dataSource.setStrategy(properties.getStrategy());
        return dataSource;
    }

    @Bean
    public DynamicDataSourceProvider dynamicDataSourceProvider(DynamicDataSourceProperties properties) throws Exception {
        DefaultDataSourceCreator creator = defaultDataSourceCreator(properties);
        return () -> {
            Map<String, DataSource> dataSources = new LinkedHashMap<>();
            for (Map.Entry<String, com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty> entry : properties.getDatasource().entrySet()) {
                dataSources.put(entry.getKey(), creator.createDataSource(entry.getValue()));
            }
            return dataSources;
        };
    }

    private DefaultDataSourceCreator defaultDataSourceCreator(DynamicDataSourceProperties properties) throws Exception {
        DefaultDataSourceCreator creator = new DefaultDataSourceCreator();
        List<DataSourceCreator> creators = new ArrayList<>();
        creators.add(applyProperties(new JndiDataSourceCreator(), properties));

        DruidDataSourceCreator druidDataSourceCreator = applyProperties(new DruidDataSourceCreator(), properties);
        druidDataSourceCreator.afterPropertiesSet();
        creators.add(druidDataSourceCreator);

        creators.add(applyProperties(new BasicDataSourceCreator(), properties));
        creator.setCreators(creators);
        return creator;
    }

    private <T extends DataSourceCreator> T applyProperties(T creator, DynamicDataSourceProperties properties) {
        Field propertiesField = ReflectionUtils.findField(creator.getClass().getSuperclass(), "properties");
        if (propertiesField == null) {
            throw new IllegalStateException("Dynamic datasource creator properties field not found");
        }
        ReflectionUtils.makeAccessible(propertiesField);
        ReflectionUtils.setField(propertiesField, creator, properties);

        Field dataSourceInitEventField = ReflectionUtils.findField(creator.getClass().getSuperclass(), "dataSourceInitEvent");
        if (dataSourceInitEventField == null) {
            throw new IllegalStateException("Dynamic datasource creator init event field not found");
        }
        ReflectionUtils.makeAccessible(dataSourceInitEventField);
        ReflectionUtils.setField(dataSourceInitEventField, creator, new EncDataSourceInitEvent());
        return creator;
    }
}

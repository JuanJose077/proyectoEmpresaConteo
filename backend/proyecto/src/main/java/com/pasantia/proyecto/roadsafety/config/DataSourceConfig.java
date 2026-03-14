package com.pasantia.proyecto.roadsafety.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "empresaDataSource")
    @ConfigurationProperties(prefix = "app.datasource.empresa")
    public DataSource empresaDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "empresaJdbcTemplate")
    @Primary
    public JdbcTemplate empresaJdbcTemplate(@Qualifier("empresaDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean(name = "localDataSource")
    @ConfigurationProperties(prefix = "app.datasource.local")
    public DataSource localDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "localJdbcTemplate")
    public JdbcTemplate localJdbcTemplate(@Qualifier("localDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
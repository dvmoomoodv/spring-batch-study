package com.example.batch.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 데이터베이스 설정 클래스
 * - 소스 DB (MSSQL)와 타겟 DB (MariaDB) 설정
 * - 개발/운영 환경별 설정 분리
 */
@Configuration
public class DatabaseConfig {

    /**
     * 소스 데이터베이스 (MSSQL) 설정
     */
    @Bean(name = "sourceDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.source")
    public DataSource sourceDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * 타겟 데이터베이스 (MariaDB) 설정 - 개발환경
     */
    @Bean(name = "targetDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.target")
    public DataSource targetDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * 타겟 슬레이브 데이터베이스 (MariaDB) 설정 - 운영환경만
     */
    @Bean(name = "targetSlaveDataSource")
    @Profile("prod")
    @ConfigurationProperties(prefix = "spring.datasource.target-slave")
    public DataSource targetSlaveDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * 소스 DB용 JdbcTemplate
     */
    @Bean(name = "sourceJdbcTemplate")
    public JdbcTemplate sourceJdbcTemplate(@Qualifier("sourceDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 타겟 DB용 JdbcTemplate
     */
    @Bean(name = "targetJdbcTemplate")
    public JdbcTemplate targetJdbcTemplate(@Qualifier("targetDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 타겟 슬레이브 DB용 JdbcTemplate (운영환경만)
     */
    @Bean(name = "targetSlaveJdbcTemplate")
    @Profile("prod")
    public JdbcTemplate targetSlaveJdbcTemplate(@Qualifier("targetSlaveDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

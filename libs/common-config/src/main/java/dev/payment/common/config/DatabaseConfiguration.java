package dev.payment.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(PayFlowProperties.class)
@ConditionalOnProperty(name = "payflow.database.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseConfiguration {

    @Bean
    @Primary
    public DataSource dataSource(PayFlowProperties properties) {
        PayFlowProperties.Database db = properties.getDatabase();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(db.getUrl());
        config.setUsername(db.getUsername());
        config.setPassword(db.getPassword());
        config.setDriverClassName("org.postgresql.Driver");

        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);
        config.setConnectionTimeout(30000);
        config.setPoolName("PayFlowHikariPool");
        config.setRegisterMbeans(true);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
    }
}
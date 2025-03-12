package ru.panifidkin.examples.config.db;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "db.examples.postgresql")
@ToString(exclude = "password")
public class JdbcProperties {

    private String url;
    private String schema;
    private String username;
    private String password;
    private String driver;
    private PoolConfig pool;

    @Data
    public static class PoolConfig {
        private int minimumIdle = 10;
        private int maximumSize = 100;
        private int connectionTimeout = 30_000;
        private int idleTimeout = 60_000;
        private int leakDetectionThreshold = 0;
    }

    @PostConstruct
    void log() {
        log.info("db.examples.postgresql: {}", this);
    }
}

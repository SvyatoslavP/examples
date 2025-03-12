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
@ConfigurationProperties(prefix = "jpa.examples.hibernate")
@ToString
public class JpaProperties {

    private String ddlAuto;
    private String showSql;
    private String formatSql;
    private String lockTimeout;

    @PostConstruct
    void log() {
        log.info("jpa.examples.hibernate: {}", this);
    }
}

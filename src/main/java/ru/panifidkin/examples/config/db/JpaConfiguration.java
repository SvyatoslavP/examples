package ru.panifidkin.examples.config.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.Nonnull;
import jakarta.persistence.PessimisticLockScope;
import liquibase.integration.spring.SpringLiquibase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.TimeZone;

import static ru.panifidkin.examples.config.db.JpaConfiguration.ENTITY_MANAGER_FACTORY_BEAN;
import static ru.panifidkin.examples.config.db.JpaConfiguration.TRANSACTION_MANAGER_BEAN;

@Slf4j
@Configuration
@AllArgsConstructor
@EnableTransactionManagement
@EntityScan(basePackages = {"ru.panifidkin.examples"})
@EnableJpaRepositories(basePackages = "ru.panifidkin.examples",
        entityManagerFactoryRef = ENTITY_MANAGER_FACTORY_BEAN,
        transactionManagerRef = TRANSACTION_MANAGER_BEAN)
public class JpaConfiguration {
    private static final String DATA_SOURCE_POOL_NAME = "examples";
    protected static final String TRANSACTION_MANAGER_BEAN = "annotationDriverTransactionManager";
    protected static final String ENTITY_MANAGER_FACTORY_BEAN = "entityManagerFactoryBean";

    private final JdbcProperties jdbcProperties;

    @Bean("exampleDataSource")
    @LiquibaseDataSource
    public DataSource exampleDataSource(JpaProperties jpaProperties) {
        var dataSource = DataSourceBuilder.create()
                .driverClassName(jdbcProperties.getDriver())
                .url(jdbcProperties.getUrl())
                .username(jdbcProperties.getUsername())
                .password(jdbcProperties.getPassword())
                .type(HikariDataSource.class)
                .build();
        dataSource.setSchema(jdbcProperties.getSchema());
        dataSource.setPoolName(DATA_SOURCE_POOL_NAME);

        var lockTimeOut = jpaProperties.getLockTimeout();
        if (lockTimeOut != null) {
            dataSource.setConnectionInitSql("SET lock_timeout = '" + lockTimeOut + "'");
            log.info("Default lock timeout set to {} ms", lockTimeOut);
        }

        final var poolConfig = jdbcProperties.getPool();
        if (poolConfig != null) {
            dataSource.setConnectionTimeout(poolConfig.getConnectionTimeout());
            dataSource.setIdleTimeout(poolConfig.getIdleTimeout());
            dataSource.setMinimumIdle(poolConfig.getMinimumIdle());
            dataSource.setMaximumPoolSize(poolConfig.getMaximumSize());
            dataSource.setLeakDetectionThreshold(poolConfig.getLeakDetectionThreshold());
        }
        return dataSource;
    }

    @Bean
    public DataSourceHealthIndicator examplesDataSourceHealth(@Qualifier("exampleDataSource") DataSource dataSource) {
        return new DataSourceHealthIndicator(dataSource);
    }

    @Bean(ENTITY_MANAGER_FACTORY_BEAN)
    public LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean(
            @Qualifier("exampleDataSource") DataSource dataSource,
            JpaProperties jpaProperties) {
        var properties = new Properties();
        properties.put(MappingSettings.IMPLICIT_NAMING_STRATEGY, "component-path");
        properties.put(JdbcSettings.JAKARTA_JDBC_DRIVER, jdbcProperties.getDriver());
        properties.put(SchemaToolingSettings.HBM2DDL_AUTO, jpaProperties.getDdlAuto());
        properties.put(JdbcSettings.SHOW_SQL, jpaProperties.getShowSql());
        properties.put(JdbcSettings.FORMAT_SQL, jpaProperties.getFormatSql());
        properties.put(AvailableSettings.JAKARTA_LOCK_SCOPE, PessimisticLockScope.NORMAL);
        properties.put(MappingSettings.JSON_FORMAT_MAPPER, hibernateJsonFormatMapper());
        properties.put(MappingSettings.TIMEZONE_DEFAULT_STORAGE, TimeZoneStorageType.NORMALIZE);

        final var entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource);
        entityManagerFactoryBean.setPackagesToScan("ru.panifidkin.examples");
        entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        entityManagerFactoryBean.setJpaProperties(properties);
        return entityManagerFactoryBean;
    }

    @Bean(TRANSACTION_MANAGER_BEAN)
    public PlatformTransactionManager annotationDriverTransactionManager() {
        return new JpaTransactionManager();
    }

    @Bean("examplesNamedParameterJdbcOperations")
    public NamedParameterJdbcOperations examplesNamedParameterJdbcOperations(@Qualifier("exampleDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean("examplesLiquibase")
    public SpringLiquibase liquibase(@Qualifier("exampleDataSource") DataSource dataSource,
                                     @Value("${liquibase.shouldRun:true}") boolean shouldRun) {
        final var liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:db/changelog-master.xml");
        liquibase.setDataSource(dataSource);
        liquibase.setShouldRun(shouldRun);
        liquibase.setLiquibaseSchema(jdbcProperties.getSchema());
        liquibase.setDefaultSchema(jdbcProperties.getSchema());
        return liquibase;
    }

    @Nonnull
    private JacksonJsonFormatMapper hibernateJsonFormatMapper() {
        var mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setTimeZone(TimeZone.getDefault())
                .registerModule(new JavaTimeModule());

        return new JacksonJsonFormatMapper(mapper);
    }
}

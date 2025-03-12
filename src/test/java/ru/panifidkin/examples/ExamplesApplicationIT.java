package ru.panifidkin.examples;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration(exclude = {
        MetricsAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@TestConstructor(autowireMode = ALL)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = ExamplesApplication.class)
@RequiredArgsConstructor
class ExamplesApplicationIT {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest;

    @Test
    void ping() {
        //given
        var url = "http://localhost:" + port + "/api/ping";
        //when
        var entity = rest.getForEntity(url, String.class);
        //then
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo("pong");
    }

}

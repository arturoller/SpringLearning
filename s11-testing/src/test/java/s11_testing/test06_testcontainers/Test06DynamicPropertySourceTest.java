package s11_testing.test06_testcontainers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 2 lekcji Test06Testcontainers — STARY sposób (przed Boot 3.1, wciąż spotykany w poradnikach):
 * kontener bez {@code @ServiceConnection}, a adres, login i hasło przepisujemy ręcznie do {@code spring.datasource.*}
 * przez {@code @DynamicPropertySource} (Test05TestConfiguration, sekcja 3). Działa tak samo, ale łatwo pominąć
 * właściwość, a dla każdej usługi (baza, Redis, Kafka…) trzeba znać jej nazwy właściwości.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test06")
@Testcontainers(disabledWithoutDocker = true)
class Test06DynamicPropertySourceTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);      // np. jdbc:postgresql://localhost:32771/test
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    Environment environment;

    @Autowired
    ProductService service;

    @Test
    @DisplayName("Sekcja 2: @DynamicPropertySource — spring.datasource.url wskazuje kontener, a dodawanie produktu (ON CONFLICT) działa")
    void dynamicPropertiesPointToContainer() {
        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo(postgres.getJdbcUrl());
        assertThat(service.addIfAbsent("Ciastka", new BigDecimal("12.50"))).isTrue();
        assertThat(service.addIfAbsent("Ciastka", new BigDecimal("12.50"))).isFalse();
    }
}

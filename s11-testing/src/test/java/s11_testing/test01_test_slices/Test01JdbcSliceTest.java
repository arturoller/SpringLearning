package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 2 lekcji Test01TestSlices: wycinek {@code @JdbcTest} — baza i JDBC ({@code DataSource}, {@code JdbcClient},
 * transakcje), ale bez JPA i bez repozytoriów Spring Data. Dla aplikacji pisanych na samym JDBC (s07_data_jdbc).
 * Tu służy do porównania z {@code @DataJpaTest}: ta sama baza z Flyway, inny zakres beanów.
 */
@JdbcTest
@ActiveProfiles("test01")
class Test01JdbcSliceTest {

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sekcja 2: w wycinku Jdbc jest JdbcClient z danymi z Flyway, a repozytoriów JPA nie ma")
    void sliceContents() {
        assertThat(jdbc.sql("SELECT COUNT(*) FROM product").query(Long.class).single()).isEqualTo(2);
        assertThat(context.getBeanNamesForType(ProductRepository.class)).isEmpty();
        assertThat(context.getBeanNamesForType(jakarta.persistence.EntityManagerFactory.class)).isEmpty();
    }
}

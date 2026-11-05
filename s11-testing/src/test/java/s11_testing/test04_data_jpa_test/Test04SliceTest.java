package s11_testing.test04_data_jpa_test;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 1 lekcji Test04DataJpaTest: co robi {@code @DataJpaTest} — encje, repozytoria, Flyway lekcji, baza
 * wbudowana z LOSOWĄ nazwą i transakcja z wycofaniem (rollback) po każdym teście. Pełny kontekst webowy
 * i serwisy nie są ładowane (Test01TestSlices, sekcja 2).
 */
@DataJpaTest(showSql = false)   // showSql = false: nie zaśmiecamy konsoli SQL-em
@ActiveProfiles("test04")        // profil lekcji: Flyway, ddl-auto=validate
class Test04SliceTest {

    @Autowired
    ProductRepository repository;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    DataSource dataSource;

    @Autowired
    EntityManagerFactory emf;

    @Autowired
    ObjectProvider<CategoryService> service;

    @Test
    @DisplayName("Sekcja 1: wycinek zawiera repozytoria i encje lekcji, ale nie serwisy")
    void sliceContents() {
        assertThat(repository.count()).isEqualTo(5);
        assertThat(emf.getMetamodel().getEntities()).extracting(type -> (Object) type.getJavaType())
                .containsExactlyInAnyOrder(Product.class, Category.class);
        assertThat(service.getIfAvailable()).isNull();
    }

    @Test
    @DisplayName("Sekcja 1: Flyway lekcji wykonał migracje V1 i V2 na bazie wycinka (dane są z migracji)")
    void flywayRan() {
        assertThat(jdbc.sql("SELECT \"version\" FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY \"installed_rank\"")
                .query(String.class).list()).containsExactly("1", "2");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM category").query(Long.class).single()).isEqualTo(2);
    }

    @Test
    @DisplayName("Sekcja 1: baza wycinka to wbudowana H2 z LOSOWĄ nazwą — nie 'jdbc:h2:mem:test04' z profilu")
    void randomDatabaseName() throws Exception {
        String url;
        try (var connection = dataSource.getConnection()) {
            url = connection.getMetaData().getURL();
        }

        assertThat(url).startsWith("jdbc:h2:mem:").doesNotContain("test04");
    }

    @Test
    @DisplayName("Sekcja 1: rollback po teście (1 z 2) — wstawiony produkt jest widoczny tylko w tym teście")
    void rollbackFirst() {
        repository.save(new Product("Zeszyt-A", repository.findById(1L).orElseThrow().getCategory(), new BigDecimal("4.50"), 1));

        assertThat(repository.count()).as("5 z migracji + 1 własny; gdyby poprzedni test nie był wycofany, byłoby więcej").isEqualTo(6);
    }

    @Test
    @DisplayName("Sekcja 1: rollback po teście (2 z 2) — identyczny test; oba przechodzą w DOWOLNEJ kolejności, więc każdy zaczyna od 5")
    void rollbackSecond() {
        repository.save(new Product("Zeszyt-B", repository.findById(1L).orElseThrow().getCategory(), new BigDecimal("4.50"), 1));

        assertThat(repository.count()).isEqualTo(6);
    }
}

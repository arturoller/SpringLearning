package s11_testing.test06_testcontainers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 1 lekcji Test06Testcontainers: te same zapytania natywne na H2 (zwykły {@code @DataJpaTest}, bez Dockera).
 * Wynik jest ODWROTNY niż na PostgreSQL (Test06ServiceConnectionTest): składnia PostgreSQL pada, a składnia H2
 * działa — czyli zielony test na H2 nie dowodzi, że zapytanie zadziała na produkcyjnej bazie.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test06")
class Test06H2DifferencesTest {

    @Autowired
    ProductRepository repository;

    @Test
    @DisplayName("Sekcja 1: na H2 'ON CONFLICT DO NOTHING' (składnia PostgreSQL) to błąd składni")
    void postgresSyntaxFailsOnH2() {
        assertThatThrownBy(() -> repository.insertIfAbsent("Kawa", new BigDecimal("1.00")))
                .isInstanceOf(InvalidDataAccessResourceUsageException.class)
                .hasMessageContaining("ON CONFLICT");
    }

    @Test
    @DisplayName("Sekcja 1: na H2 'SELECT TOP 3' działa — test jest zielony, choć PostgreSQL tej składni nie zna")
    void h2SyntaxWorksOnH2() {
        assertThat(repository.findTop3MostExpensiveH2Style()).extracting(Product::getName)
                .containsExactly("Czajnik", "Kawa", "Kubek");
    }

    @Test
    @DisplayName("Sekcja 1: H2 po błędzie SQL NIE przerywa transakcji — kolejne zapytanie w niej działa (PostgreSQL zachowuje się inaczej)")
    void h2KeepsTransactionAfterError() {
        assertThatThrownBy(() -> repository.insertIfAbsent("Kawa", new BigDecimal("1.00")))
                .isInstanceOf(InvalidDataAccessResourceUsageException.class);

        assertThat(repository.findMostExpensive(1)).extracting(Product::getName).containsExactly("Czajnik");
    }

    @Test
    @DisplayName("Sekcja 1: przenośne 'LIMIT' działa na H2 (i na PostgreSQL)")
    void portableSyntaxWorks() {
        assertThat(repository.findMostExpensive(2)).extracting(Product::getName).containsExactly("Czajnik", "Kawa");
    }
}

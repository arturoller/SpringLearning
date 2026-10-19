package s11_testing.test04_data_jpa_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Test04DataJpaTest. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Baza startuje z 14 produktami z migracji V2. Uzupełnij miejsca z TODO i usuń {@code todo()}; asercje są gotowe.
 */
@Tag("cwiczenie")
@DataJpaTest
@ActiveProfiles("test04")
class Test04DataJpaTestExercisesTest {

    /** Znacznik „tu jeszcze nic nie ma” — rzuca wyjątek, dopóki go nie usuniesz. */
    static void todo() {
        throw new UnsupportedOperationException("TODO");
    }

    @Autowired
    ProductRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    JdbcClient jdbc;

    @Test
    @DisplayName("Ćwiczenie 1: TestEntityManager dodaje czwartą książkę")
    void exercise1() {
        // TODO: zapisz przez entityManager produkt KSI-004 „Spring w akcji”, KSIAZKI, 119.00 zł, stan 6
        todo();

        assertThat(repository.findByCategoryOrderBySku("KSIAZKI"))
                .extracting(Product::getSku)
                .containsExactly("KSI-001", "KSI-002", "KSI-003", "KSI-004");
    }

    @Test
    @DisplayName("Ćwiczenie 2: zmiana stanu lampki dociera do bazy (widzi ją zwykłe SQL)")
    void exercise2() {
        Product lamp = repository.findBySku("DOM-002").orElseThrow();
        // TODO: ustaw stan lampki na 0 i spraw, żeby UPDATE naprawdę poszedł do bazy
        //       (zapytanie poniżej to zwykłe SQL — omija kontekst utrwalania JPA)
        todo();

        assertThat(jdbc.sql("SELECT stock FROM product WHERE sku = 'DOM-002'").query(Integer.class).single()).isZero();
        assertThat(lamp.getStock()).isZero();
    }

    @Test
    @DisplayName("Ćwiczenie 3: @Sql zeruje stan KSI-003 tylko dla tego testu")
    // TODO: dodaj tu adnotację @Sql z instrukcją UPDATE, która ustawia stan KSI-003 na 0
    void exercise3() {
        todo();   // TODO: usuń tę linię po dodaniu adnotacji

        assertThat(repository.findLowStock(1))
                .extracting(Product::getSku)
                .containsExactly("ELE-002", "KSI-003", "SPO-003");
    }

    @Test
    @DisplayName("Ćwiczenie 4: zakończ transakcję testu i pokaż, że zapis zniknął")
    void exercise4() {
        repository.save(new Product("DOM-003", "Odkurzacz", "DOM", new BigDecimal("599.00"), 3));
        assertThat(repository.count()).isEqualTo(15);

        // TODO: zakończ transakcję testu tak, jak robi to Spring po teście (wycofanie)
        todo();

        assertThat(TestTransaction.isActive()).isFalse();
        assertThat(repository.count()).isEqualTo(14);
    }
}

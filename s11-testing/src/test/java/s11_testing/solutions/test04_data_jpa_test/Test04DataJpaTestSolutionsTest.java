package s11_testing.solutions.test04_data_jpa_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.transaction.TestTransaction;
import s11_testing.test04_data_jpa_test.Product;
import s11_testing.test04_data_jpa_test.ProductRepository;
import s11_testing.test04_data_jpa_test.Test04Application;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Test04DataJpaTest (te same testy co w Test04DataJpaTestExercisesTest,
 * już uzupełnione). {@code @Tag("wzorzec")} — biegną zawsze. Zajrzyj dopiero po własnej próbie!
 * Pakiet {@code solutions…} nie leży pod pakietem lekcji, więc konfigurację wskazujemy jawnie.
 */
@Tag("wzorzec")
@DataJpaTest
@ActiveProfiles("test04")
@ContextConfiguration(classes = Test04Application.class)
class Test04DataJpaTestSolutionsTest {

    @Autowired
    ProductRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    JdbcClient jdbc;

    @Test
    @DisplayName("Wzorzec 1: TestEntityManager dodaje czwartą książkę")
    void solution1() {
        entityManager.persist(new Product("KSI-004", "Spring w akcji", "KSIAZKI", new BigDecimal("119.00"), 6));

        assertThat(repository.findByCategoryOrderBySku("KSIAZKI"))
                .extracting(Product::getSku)
                .containsExactly("KSI-001", "KSI-002", "KSI-003", "KSI-004");
    }

    @Test
    @DisplayName("Wzorzec 2: zmiana stanu lampki dociera do bazy (widzi ją zwykłe SQL)")
    void solution2() {
        Product lamp = repository.findBySku("DOM-002").orElseThrow();
        lamp.setStock(0);
        entityManager.flush();   // albo repository.flush() — wysyła zaległy UPDATE

        assertThat(jdbc.sql("SELECT stock FROM product WHERE sku = 'DOM-002'").query(Integer.class).single()).isZero();
        assertThat(lamp.getStock()).isZero();
    }

    @Test
    @DisplayName("Wzorzec 3: @Sql zeruje stan KSI-003 tylko dla tego testu")
    @Sql(statements = "UPDATE product SET stock = 0 WHERE sku = 'KSI-003'")
    void solution3() {
        assertThat(repository.findLowStock(1))
                .extracting(Product::getSku)
                .containsExactly("ELE-002", "KSI-003", "SPO-003");
    }

    @Test
    @DisplayName("Wzorzec 4: zakończ transakcję testu i pokaż, że zapis zniknął")
    void solution4() {
        repository.save(new Product("DOM-003", "Odkurzacz", "DOM", new BigDecimal("599.00"), 3));
        assertThat(repository.count()).isEqualTo(15);

        TestTransaction.end();   // transakcja testu jest oznaczona do wycofania, więc end() = rollback

        assertThat(TestTransaction.isActive()).isFalse();
        assertThat(repository.count()).isEqualTo(14);
    }
}

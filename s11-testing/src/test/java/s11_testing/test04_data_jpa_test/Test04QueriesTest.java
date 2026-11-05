package s11_testing.test04_data_jpa_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Test04DataJpaTest: testujemy to, co NAPISALIŚMY — własne zapytanie {@code @Query} i projekcję — oraz
 * dane testowe z pliku {@code @Sql}. Metod {@code JpaRepository} (save, findById, count…) nie testujemy: to kod
 * Spring Data, sprawdzony przez jego autorów.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test04")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)   // kolejność ustalona celowo — tylko dla pary testów @Sql / rollback
class Test04QueriesTest {

    @Autowired
    ProductRepository repository;

    @Test
    @DisplayName("Sekcja 5: własne @Query — produkty o stanie ≤ 5, od najmniejszego stanu (Lampka 0, Monitor 5)")
    void lowStock() {
        assertThat(repository.findLowStock(5)).extracting(Product::getName).containsExactly("Lampka", "Monitor");
        assertThat(repository.findLowStock(-1)).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 5: @Sql dodaje produkt, a własne @Query widzi go i sortuje po stanie")
    @Sql("/test04/extra-products.sql")   // skrypt wykonany PRZED testem, w transakcji testu (więc wycofany po nim)
    void lowStockSeesSqlData() {
        // dla progu 40 lista rośnie po stanie: 0, 5, 12, 30, 40
        assertThat(repository.findLowStock(40)).extracting(Product::getName)
                .containsExactly("Lampka", "Monitor", "Klawiatura", "Mysz", "Zeszyt");
    }

    @Test
    @Order(1)
    @DisplayName("Sekcja 5 (1/2): dane z @Sql są widoczne w tym teście — 6 produktów")
    @Sql("/test04/extra-products.sql")
    void sqlDataVisible() {
        assertThat(repository.count()).isEqualTo(6);
    }

    @Test
    @Order(2)
    @DisplayName("Sekcja 5 (2/2): następny test (bez @Sql) widzi tylko dane z migracji — 5 produktów; skrypt @Sql został wycofany")
    void sqlDataRolledBack() {
        assertThat(repository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("Sekcja 5: projekcja interfejsowa — tylko nazwa i cena, posortowane po nazwie")
    void projection() {
        List<ProductSummary> summaries = repository.findByCategoryNameOrderByName("Komputery");

        assertThat(summaries).extracting(ProductSummary::getName).containsExactly("Klawiatura", "Monitor", "Mysz");
        assertThat(summaries.get(1).getPrice()).isEqualByComparingTo(new BigDecimal("899.99"));
    }
}

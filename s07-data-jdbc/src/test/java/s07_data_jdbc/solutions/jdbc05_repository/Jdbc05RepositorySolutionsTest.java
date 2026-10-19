package s07_data_jdbc.solutions.jdbc05_repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import s07_data_jdbc.jdbc05_repository.DuplicateSkuException;
import s07_data_jdbc.jdbc05_repository.Jdbc05Repository.CategoryCount;
import s07_data_jdbc.jdbc05_repository.Jdbc05TestDatabase;
import s07_data_jdbc.jdbc05_repository.NewProduct;
import s07_data_jdbc.jdbc05_repository.Product;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Jdbc05RepositoryExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Jdbc05RepositorySolutionsTest {

    private EmbeddedDatabase database;
    private JdbcClient jdbc;

    @BeforeEach
    void setUp() {
        database = Jdbc05TestDatabase.create();
        jdbc = JdbcClient.create(database);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    @DisplayName("Wzorzec 1: produkty nie droższe niż podana cena, od najtańszego")
    void solution1() {
        assertThat(Jdbc05RepositorySolutions.solution1(jdbc, new BigDecimal("100.00"))).extracting(Product::sku)
                .containsExactly("SPO-002", "SPO-003", "ODZ-002", "SPO-001", "KSI-001", "KSI-003");
    }

    @Test
    @DisplayName("Wzorzec 2: usunięcie produktów bez stanu")
    void solution2() {
        assertThat(Jdbc05RepositorySolutions.solution2(jdbc)).isEqualTo(2);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM product").query(Integer.class).single()).isEqualTo(12);
    }

    @Test
    @DisplayName("Wzorzec 3: zapis z id, a duplikat SKU daje wyjątek domenowy")
    void solution3() {
        Product saved = Jdbc05RepositorySolutions.solution3(jdbc,
                new NewProduct("DOM-003", "Odkurzacz", "DOM", new BigDecimal("599.00"), 3));
        assertThat(saved.id()).isEqualTo(15L);
        assertThat(saved.sku()).isEqualTo("DOM-003");

        assertThatThrownBy(() -> Jdbc05RepositorySolutions.solution3(jdbc,
                new NewProduct("ELE-001", "Inny", "ELEKTRONIKA", BigDecimal.ONE, 1)))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessage("Produkt o SKU ELE-001 już istnieje")
                .hasCauseInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("Wzorzec 4: liczba produktów w kategoriach")
    void solution4() {
        assertThat(Jdbc05RepositorySolutions.solution4(jdbc)).containsExactly(
                new CategoryCount("DOM", 2),
                new CategoryCount("ELEKTRONIKA", 4),
                new CategoryCount("KSIAZKI", 3),
                new CategoryCount("ODZIEZ", 2),
                new CategoryCount("SPOZYWCZE", 3));
    }
}

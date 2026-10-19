package s07_data_jdbc.jdbc05_repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Jdbc05Repository. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Jdbc05RepositoryExercisesTest {

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
    @DisplayName("Ćwiczenie 1: produkty nie droższe niż podana cena, od najtańszego")
    void exercise1() {
        assertThat(Jdbc05Repository.exercise1(jdbc, new BigDecimal("100.00"))).extracting(Product::sku)
                .containsExactly("SPO-002", "SPO-003", "ODZ-002", "SPO-001", "KSI-001", "KSI-003");
    }

    @Test
    @DisplayName("Ćwiczenie 2: usunięcie produktów bez stanu")
    void exercise2() {
        assertThat(Jdbc05Repository.exercise2(jdbc)).isEqualTo(2);   // Smartfon X i Oliwa z oliwek
        assertThat(jdbc.sql("SELECT COUNT(*) FROM product").query(Integer.class).single()).isEqualTo(12);
    }

    @Test
    @DisplayName("Ćwiczenie 3: zapis z id, a duplikat SKU daje wyjątek domenowy")
    void exercise3() {
        Product saved = Jdbc05Repository.exercise3(jdbc,
                new NewProduct("DOM-003", "Odkurzacz", "DOM", new BigDecimal("599.00"), 3));
        assertThat(saved.id()).isEqualTo(15L);
        assertThat(saved.sku()).isEqualTo("DOM-003");

        assertThatThrownBy(() -> Jdbc05Repository.exercise3(jdbc,
                new NewProduct("ELE-001", "Inny", "ELEKTRONIKA", BigDecimal.ONE, 1)))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessage("Produkt o SKU ELE-001 już istnieje")
                .hasCauseInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 4: liczba produktów w kategoriach")
    void exercise4() {
        assertThat(Jdbc05Repository.exercise4(jdbc)).containsExactly(
                new Jdbc05Repository.CategoryCount("DOM", 2),
                new Jdbc05Repository.CategoryCount("ELEKTRONIKA", 4),
                new Jdbc05Repository.CategoryCount("KSIAZKI", 3),
                new Jdbc05Repository.CategoryCount("ODZIEZ", 2),
                new Jdbc05Repository.CategoryCount("SPOZYWCZE", 3));
    }
}

package s07_data_jdbc.solutions.jdbc01_jdbc_client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import s07_data_jdbc.jdbc01_jdbc_client.Jdbc01TestDatabase;
import s07_data_jdbc.jdbc01_jdbc_client.Product;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jdbc01JdbcClientExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Jdbc01JdbcClientSolutionsTest {

    private EmbeddedDatabase database;
    private JdbcClient jdbc;

    @BeforeEach
    void setUp() {
        database = Jdbc01TestDatabase.create();
        jdbc = Jdbc01TestDatabase.client(database);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    @DisplayName("Wzorzec 1: nazwy produktów kategorii od najdroższego")
    void solution1() {
        assertThat(Jdbc01JdbcClientSolutions.solution1(jdbc, "KSIAZKI"))
                .containsExactly("Java. Podstawy", "Wzorce projektowe", "Czysty kod");
    }

    @Test
    @DisplayName("Wzorzec 2: najtańszy dostępny produkt kategorii")
    void solution2() {
        assertThat(Jdbc01JdbcClientSolutions.solution2(jdbc, "ELEKTRONIKA")).get()
                .extracting(Product::sku).isEqualTo("ELE-003");
        assertThat(Jdbc01JdbcClientSolutions.solution2(jdbc, "NIE-MA-TAKIEJ")).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 3: wstawienie produktu zwraca nadane id")
    void solution3() {
        long id = Jdbc01JdbcClientSolutions.solution3(jdbc, "DOM-003", "Odkurzacz", new BigDecimal("599.00"));
        assertThat(id).isEqualTo(15L);
        assertThat(jdbc.sql("SELECT name FROM product WHERE id = :id").param("id", id).query(String.class).single())
                .isEqualTo("Odkurzacz");
    }

    @Test
    @DisplayName("Wzorzec 4: zmniejszenie stanu tylko gdy wystarcza sztuk")
    void solution4() {
        assertThat(Jdbc01JdbcClientSolutions.solution4(jdbc, "ELE-004", 3)).isTrue();
        assertThat(Jdbc01JdbcClientSolutions.solution4(jdbc, "ELE-004", 3)).isFalse();
        assertThat(jdbc.sql("SELECT stock FROM product WHERE sku = 'ELE-004'").query(Integer.class).single()).isEqualTo(1);
        assertThat(Jdbc01JdbcClientSolutions.solution4(jdbc, "NIE-MA", 1)).isFalse();
    }
}

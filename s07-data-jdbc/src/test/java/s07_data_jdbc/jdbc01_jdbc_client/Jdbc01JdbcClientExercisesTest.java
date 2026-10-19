package s07_data_jdbc.jdbc01_jdbc_client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jdbc01JdbcClient. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Jdbc01JdbcClientExercisesTest {

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
    @DisplayName("Ćwiczenie 1: nazwy produktów kategorii od najdroższego")
    void exercise1() {
        assertThat(Jdbc01JdbcClient.exercise1(jdbc, "KSIAZKI"))
                .containsExactly("Java. Podstawy", "Wzorce projektowe", "Czysty kod");
    }

    @Test
    @DisplayName("Ćwiczenie 2: najtańszy dostępny produkt kategorii")
    void exercise2() {
        // w ELEKTRONIKA Smartfon X nie ma na stanie, a najtańsze są Słuchawki BT
        assertThat(Jdbc01JdbcClient.exercise2(jdbc, "ELEKTRONIKA")).get()
                .extracting(Product::sku).isEqualTo("ELE-003");
        assertThat(Jdbc01JdbcClient.exercise2(jdbc, "NIE-MA-TAKIEJ")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: wstawienie produktu zwraca nadane id")
    void exercise3() {
        long id = Jdbc01JdbcClient.exercise3(jdbc, "DOM-003", "Odkurzacz", new BigDecimal("599.00"));
        assertThat(id).isEqualTo(15L);
        assertThat(jdbc.sql("SELECT name FROM product WHERE id = :id").param("id", id).query(String.class).single())
                .isEqualTo("Odkurzacz");
    }

    @Test
    @DisplayName("Ćwiczenie 4: zmniejszenie stanu tylko gdy wystarcza sztuk")
    void exercise4() {
        // ELE-004 Monitor ma 4 sztuki
        assertThat(Jdbc01JdbcClient.exercise4(jdbc, "ELE-004", 3)).isTrue();
        assertThat(Jdbc01JdbcClient.exercise4(jdbc, "ELE-004", 3)).isFalse();   // została 1 sztuka
        assertThat(jdbc.sql("SELECT stock FROM product WHERE sku = 'ELE-004'").query(Integer.class).single()).isEqualTo(1);
        assertThat(Jdbc01JdbcClient.exercise4(jdbc, "NIE-MA", 1)).isFalse();
    }
}

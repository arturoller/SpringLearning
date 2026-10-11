package s07_data_jdbc.jdbc04_flyway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jdbc04Flyway. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
@ActiveProfiles("jdbc04")
class Jdbc04FlywayExercisesTest {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("Ćwiczenie 1: opisy migracji wersjonowanych w kolejności wykonania")
    void exercise1() {
        assertThat(Jdbc04Flyway.exercise1(jdbcClient)).containsExactly("create products", "add category", "seed products");
    }

    @Test
    @DisplayName("Ćwiczenie 2: kolumny tabeli PRODUCTS po migracjach")
    void exercise2() {
        assertThat(Jdbc04Flyway.exercise2(jdbcClient)).containsExactly("CATEGORY", "ID", "NAME", "PRICE_CENTS");
    }

    @Test
    @DisplayName("Ćwiczenie 3: Flyway uruchomiony programowo tylko do wersji 2")
    void exercise3() {
        DataSource emptyDatabase = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        assertThat(Jdbc04Flyway.exercise3(emptyDatabase))
                .containsExactly("V1__create_products.sql", "V2__add_category.sql", "R__product_names_view.sql");
    }
}

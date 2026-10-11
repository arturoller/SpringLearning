package s07_data_jdbc.solutions.jdbc04_flyway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ActiveProfiles;
import s07_data_jdbc.jdbc04_flyway.Jdbc04Application;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Jdbc04FlywayExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma własnej klasy startowej, więc wskazujemy ją wprost: {@code classes = Jdbc04Application.class}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Jdbc04Application.class)
@ActiveProfiles("jdbc04")
class Jdbc04FlywaySolutionsTest {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("Wzorzec 1: opisy migracji wersjonowanych w kolejności wykonania")
    void solution1() {
        assertThat(Jdbc04FlywaySolutions.solution1(jdbcClient)).containsExactly("create products", "add category", "seed products");
    }

    @Test
    @DisplayName("Wzorzec 2: kolumny tabeli PRODUCTS po migracjach")
    void solution2() {
        assertThat(Jdbc04FlywaySolutions.solution2(jdbcClient)).containsExactly("CATEGORY", "ID", "NAME", "PRICE_CENTS");
    }

    @Test
    @DisplayName("Wzorzec 3: Flyway uruchomiony programowo tylko do wersji 2")
    void solution3() {
        DataSource emptyDatabase = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        assertThat(Jdbc04FlywaySolutions.solution3(emptyDatabase))
                .containsExactly("V1__create_products.sql", "V2__add_category.sql", "R__product_names_view.sql");
    }
}

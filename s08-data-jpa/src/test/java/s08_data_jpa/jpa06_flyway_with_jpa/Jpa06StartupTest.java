package s08_data_jpa.jpa06_flyway_with_jpa;

import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.simple.JdbcClient;
import s08_data_jpa.jpa06_pitfalls.WrongTypeApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 2, 3 i 5 lekcji Jpa06FlywayWithJpa na PEŁNEJ aplikacji (start ma się nie udać albo potrzebujemy innych
 * ustawień ddl-auto niż w profilu). Każdy start dostaje własną bazę H2.
 */
class Jpa06StartupTest {

    private static ConfigurableApplicationContext start(Class<?> application, String... extraArgs) {
        List<String> args = new ArrayList<>(List.of("--spring.profiles.active=jpa06",
                "--spring.datasource.url=jdbc:h2:mem:jpa06-start-" + UUID.randomUUID()));
        args.addAll(List.of(extraArgs));
        return new SpringApplicationBuilder(application).web(WebApplicationType.NONE).run(args.toArray(String[]::new));
    }

    private static List<String> columns(ConfigurableApplicationContext app) {
        return app.getBean(JdbcClient.class)
                .sql("SELECT LOWER(column_name) || ':' || data_type FROM information_schema.columns "
                        + "WHERE table_name = 'PRODUCT' ORDER BY ordinal_position")
                .query(String.class).list();
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: pole bez kolumny (migracje tylko do V2) → SchemaManagementException „missing column”")
    void missingColumnStopsStartup() {
        assertThatThrownBy(() -> start(Jpa06Application.class, "--spring.flyway.target=2").close())
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("Error creating bean with name 'entityManagerFactory'")
                .rootCause().isInstanceOf(SchemaManagementException.class)
                .hasMessage("Schema validation: missing column [weight_grams] in table [product]");
    }

    @Test
    @DisplayName("Sekcja 2: zły typ pola (String na kolumnie INTEGER) → SchemaManagementException „wrong column type”")
    void wrongTypeStopsStartup() {
        assertThatThrownBy(() -> start(WrongTypeApplication.class).close())
                .isInstanceOf(BeanCreationException.class)
                .rootCause().isInstanceOf(SchemaManagementException.class)
                .hasMessage("Schema validation: wrong column type encountered in column [stock] in table [product]; "
                        + "found [integer (Types#INTEGER)], but expecting [varchar(255) (Types#VARCHAR)]");
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: ddl-auto=update w Hibernate 7 po cichu ZMIENIA typ kolumny stock na VARCHAR i niczego nie usuwa")
    void updateSilentlyChangesColumnType() {
        try (ConfigurableApplicationContext app = start(WrongTypeApplication.class, "--spring.jpa.hibernate.ddl-auto=update")) {
            assertThat(columns(app))
                    .as("stock: INTEGER → CHARACTER VARYING; kolumn spoza encji (name, price…) update nie usunął")
                    .contains("stock:CHARACTER VARYING", "name:CHARACTER VARYING", "price:NUMERIC", "weight_grams:INTEGER");
        }
    }

    @Test
    @DisplayName("Sekcja 3: ddl-auto=update — nieudany ALTER jest tylko ostrzeżeniem: aplikacja startuje bez kolumny")
    void updateSwallowsFailedDdl() {
        try (ConfigurableApplicationContext app = start(Jpa06Application.class,
                "--spring.flyway.target=2", "--spring.jpa.hibernate.ddl-auto=update")) {
            assertThat(columns(app)).as("ALTER … ADD COLUMN weight_grams integer not null padł na istniejących wierszach")
                    .noneMatch(column -> column.startsWith("weight_grams"));

            ProductRepository repository = app.getBean(ProductRepository.class);
            assertThatThrownBy(repository::findAll)
                    .as("błąd wychodzi dopiero przy pierwszym zapytaniu — np. na produkcji")
                    .isInstanceOf(InvalidDataAccessResourceUsageException.class)
                    .hasMessageContaining("Column \"P1_0.WEIGHT_GRAMS\" not found");
        }
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: jakarta.persistence.schema-generation.scripts.* — Hibernate zapisuje DDL do pliku (szkic migracji)")
    void generateDdlScript(@TempDir Path dir) throws IOException {
        Path script = dir.resolve("create.sql");

        start(Jpa06Application.class,
                "--spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
                "--spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=" + script).close();

        assertThat(script).exists();
        assertThat(Files.readString(script))
                .startsWith("create table product (")
                .contains("weight_grams integer not null", "price numeric(10,2) not null", "name varchar(100) not null")
                .as("Hibernate dla H2 wybrał typ enum — my w migracji mamy VARCHAR(20)")
                .contains("category enum ('COMPUTERS','GARDEN','HOME','OFFICE') not null")
                .as("brak naszego ograniczenia UNIQUE z V1 — Hibernate o nim nie wie")
                .doesNotContain("unique");
    }
}

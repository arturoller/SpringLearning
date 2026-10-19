package s08_data_jpa.jpa06_flyway_with_jpa;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Jpa06FlywayWithJpa. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy albo
 * {@code mvnw -pl s08-data-jpa test -Dgroups=cwiczenie -DexcludedGroups=none}).
 */
@DataJpaTest
@ActiveProfiles("jpa06")
@Tag("cwiczenie")
class Jpa06FlywayWithJpaExercisesTest {

    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("Ćwiczenie 1: migracja V4 w katalogu jpa06-exercises dodaje kolumnę weight_grams (integer, może być pusta)")
    void exercise1() {
        // osobna, pusta baza i Flyway uruchomiony „ręcznie” — tylko schemat, bez Hibernate'a
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:h2:mem:jpa06exercise1", true);
        try {
            Flyway flyway = Flyway.configure().dataSource(dataSource)
                    .locations("classpath:db/migration/jpa06", "classpath:db/migration/jpa06-exercises")
                    .load();
            flyway.migrate();

            MigrationInfo current = flyway.info().current();
            assertThat(current.getVersion().getVersion()).as("ostatnia wykonana wersja").isEqualTo("4");
            assertThat(current.getDescription()).isEqualTo("add weight column");
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            assertThat(jdbc.queryForObject("select data_type from information_schema.columns "
                    + "where table_name = 'PRODUCTS' and column_name = 'WEIGHT_GRAMS'", String.class)).isEqualTo("INTEGER");
            assertThat(jdbc.queryForObject("select is_nullable from information_schema.columns "
                    + "where table_name = 'PRODUCTS' and column_name = 'WEIGHT_GRAMS'", String.class)).isEqualTo("YES");
        } finally {
            dataSource.destroy();                       // zamyka jedyne połączenie → baza w pamięci znika
        }
    }

    @Test
    @DisplayName("Ćwiczenie 2: encja Customer pasuje do tabeli customers z migracji (validate ją przepuścił)")
    void exercise2() {
        Customer jan = entityManager.find(Customer.class, 1L);
        assertThat(jan.getName()).isEqualTo("Jan Kowalski");
        assertThat(jan.getCity()).isEqualTo("Warszawa");
        assertThat(jan.isVip()).isTrue();
        assertThat(entityManager.find(Customer.class, 2L).getEmail()).isNull();       // Maria Nowak bez e-maila

        Customer anna = entityManager.persistFlushFind(new Customer("Anna Wójcik", "Łódź", "anna@example.com", false));
        assertThat(anna.getId()).as("id nadane przez kolumnę identity, po 7 klientach z V3").isGreaterThan(7L);
    }

    @Test
    @DisplayName("Ćwiczenie 3: diagnoza startu — null przy udanym starcie, najgłębsza przyczyna przy nieudanym")
    void exercise3() {
        assertThat(Jpa06FlywayWithJpa.exercise3("--spring.profiles.active=jpa06",
                "--spring.datasource.url=jdbc:h2:mem:jpa06exercise3a")).isNull();
        assertThat(Jpa06FlywayWithJpa.exercise3("--spring.profiles.active=jpa06",
                "--spring.datasource.url=jdbc:h2:mem:jpa06exercise3b", "--spring.flyway.target=1"))
                .isEqualTo("Schema validation: missing column [stock] in table [products]");
    }
}

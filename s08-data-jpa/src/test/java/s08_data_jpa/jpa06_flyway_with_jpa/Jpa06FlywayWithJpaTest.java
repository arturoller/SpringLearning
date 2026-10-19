package s08_data_jpa.jpa06_flyway_with_jpa;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jpa06FlywayWithJpa. Sekcje 1–2: {@code @DataJpaTest} z profilem jpa06 — Flyway wykonuje migracje
 * lekcji na bazie testowej, a Hibernate je sprawdza (validate). Sekcje 3–6 uruchamiają aplikację lekcji
 * z różnymi ustawieniami (każda ze swoją bazą {@code jdbc:h2:mem:...}), żeby pokazać błędy STARTU.
 */
@DataJpaTest
@ActiveProfiles("jpa06")
class Jpa06FlywayWithJpaTest {

    @Autowired
    ProductRepository repository;

    @Autowired
    Flyway flyway;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TestEntityManager entityManager;

    /** Startuje Jpa06Application (bez serwera WWW) z argumentami jak z wiersza poleceń. */
    private static ConfigurableApplicationContext start(String... args) {
        return new SpringApplicationBuilder(Jpa06Application.class).web(WebApplicationType.NONE).run(args);
    }

    @Test
    @DisplayName("1. Najpierw Flyway (3 migracje w historii), potem Hibernate z ddl-auto=validate; dane i sekwencja z migracji")
    void flywayThenHibernate() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(Arrays.stream(applied).map(info -> info.getVersion().getVersion()))
                .containsExactly("1", "2", "3");
        assertThat(Arrays.stream(applied).map(MigrationInfo::getDescription))
                .containsExactly("create tables", "add stock", "insert shop data");
        assertThat(applied).allSatisfy(info -> assertThat(info.getState().isApplied()).isTrue());

        assertThat(entityManagerFactory.getProperties().get("hibernate.hbm2ddl.auto")).isEqualTo("validate");
        assertThat(repository.count()).isEqualTo(14);                               // dane z V3
        assertThat(repository.findBySku("ELE-001")).map(Product::getName).contains("Laptop Pro 14");

        // nowy produkt dostaje id z sekwencji utworzonej w V1 (start 1001, krok 50) — daleko od id 1–14 z V3
        Product saved = repository.saveAndFlush(
                new Product("DOM-003", "Czajnik", Category.DOM, new BigDecimal("149.00"), 5));
        assertThat(saved.getId()).isGreaterThan(14L);
    }

    @Test
    @DisplayName("2. validate sprawdza tylko to, czego potrzebują encje: dodatkowa kolumna i tabela bez encji nie przeszkadzają")
    void validateChecksOnlyMappedParts() {
        // kolumna notes nie ma pola w Product, tabela customers nie ma (jeszcze) encji — a kontekst wstał
        assertThat(entityManager.getEntityManager().createNativeQuery(
                "select count(*) from information_schema.columns where table_name = 'PRODUCTS' and column_name = 'NOTES'")
                .getSingleResult()).isEqualTo(1L);
        assertThat(entityManager.getEntityManager().createNativeQuery("select count(*) from customers")
                .getSingleResult()).isEqualTo(7L);
        // enum zapisany tekstem: w migracji varchar(20) — validate to akceptuje
        assertThat(entityManager.getEntityManager().createNativeQuery(
                "select data_type from information_schema.columns where table_name = 'PRODUCTS' and column_name = 'CATEGORY'")
                .getSingleResult()).isEqualTo("CHARACTER VARYING");
    }

    @Test
    @DisplayName("3. Flyway w projekcie i brak ddl-auto w ustawieniach: Boot nie każe Hibernate'owi niczego robić ze schematem")
    void defaultDdlAutoWithFlyway() {
        // bez profilu jpa06 (który ustawia validate), z tymi samymi migracjami
        try (ConfigurableApplicationContext app = start("--spring.datasource.url=jdbc:h2:mem:jpa06default",
                "--spring.flyway.locations=classpath:db/migration/jpa06")) {
            assertThat(app.getBean(EntityManagerFactory.class).getProperties().get("hibernate.hbm2ddl.auto")).isNull();
            assertThat(app.getBean(ProductRepository.class).count()).isEqualTo(14);
        }
        // bez spring.flyway.locations Flyway bierze classpath:db/migration RAZEM z podkatalogami → dwa razy wersja 1
        assertThatThrownBy(() -> start("--spring.datasource.url=jdbc:h2:mem:jpa06nolocation"))
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("Found more than one migration with version 1");
    }

    @Test
    @DisplayName("4. Brak migracji V2 (spring.flyway.target=1): validate zatrzymuje start — missing column [stock]")
    void missingMigrationFailsValidation() {
        assertThatThrownBy(() -> start("--spring.profiles.active=jpa06",
                "--spring.datasource.url=jdbc:h2:mem:jpa06target1", "--spring.flyway.target=1"))
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("Unable to build Hibernate SessionFactory")
                .hasRootCauseInstanceOf(SchemaManagementException.class)
                .hasRootCauseMessage("Schema validation: missing column [stock] in table [products]");
    }

    @Test
    @DisplayName("5. Zły typ kolumny w migracji (price jako varchar): validate zatrzymuje start — wrong column type")
    void wrongColumnTypeFailsValidation() {
        assertThatThrownBy(() -> start("--spring.profiles.active=jpa06",
                "--spring.datasource.url=jdbc:h2:mem:jpa06wrongtype",
                "--spring.flyway.locations=classpath:db/migration/jpa06-wrong-type"))
                .isInstanceOf(BeanCreationException.class)
                .hasRootCauseInstanceOf(SchemaManagementException.class)
                .hasRootCauseMessage("Schema validation: wrong column type encountered in column [price] in table "
                        + "[products]; found [character varying (Types#VARCHAR)], but expecting [numeric(10,2) (Types#NUMERIC)]");
    }

    @Test
    @DisplayName("6. Dlaczego nie update: Hibernate sam dobudowuje kolumnę, a potem prawdziwa migracja V2 pada na duplikacie")
    void updateFightsWithFlyway() {
        String sharedDatabase = "--spring.datasource.url=jdbc:h2:mem:jpa06update";
        // A. baza tylko z V1, ddl-auto=update — Hibernate „naprawia” schemat po swojemu: add column stock
        try (ConfigurableApplicationContext first = start("--spring.profiles.active=jpa06", sharedDatabase,
                "--spring.flyway.target=1", "--spring.jpa.hibernate.ddl-auto=update")) {
            // B. ta sama baza (pierwszy kontekst trzyma ją otwartą), pełne migracje: V2 próbuje dodać stock drugi raz
            assertThatThrownBy(() -> start("--spring.profiles.active=jpa06", sharedDatabase))
                    .isInstanceOf(BeanCreationException.class)
                    .hasMessageContaining("flywayInitializer")
                    .hasStackTraceContaining("Failed to execute script V2__add_stock.sql")
                    .satisfies(error -> {
                        Throwable root = NestedExceptionUtils.getRootCause(error);
                        assertThat(root).isInstanceOf(SQLException.class);
                        assertThat(((SQLException) root).getSQLState()).as("duplikat kolumny").isEqualTo("42S21");
                        assertThat(root.getMessage()).contains("\"STOCK\"");
                    });
            assertThat(first.isActive()).isTrue();
        }
    }
}

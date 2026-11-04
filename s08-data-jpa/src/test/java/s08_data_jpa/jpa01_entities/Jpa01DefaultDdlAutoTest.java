package s08_data_jpa.jpa01_entities;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 2: DOMYŚLNA wartość ddl-auto w Boot 4. Pełna aplikacja lekcji BEZ profilu jpa01 (czyli bez jawnego
 * ddl-auto), każda z własną bazą. Sprawdzamy, co Boot przekazał Hibernate (właściwość hibernate.hbm2ddl.auto
 * EntityManagerFactory) i czy tabela product w ogóle powstała.
 */
class Jpa01DefaultDdlAutoTest {

    /** Wynik startu: przekazana wartość ddl-auto (null = Boot nic nie ustawił) i czy powstała tabela product. */
    private record StartResult(String ddlAuto, boolean productTableExists) {
    }

    private static StartResult start(String... extraArgs) {
        String[] args = new String[extraArgs.length + 1];
        args[0] = "--spring.datasource.url=jdbc:h2:mem:jpa01-default-" + UUID.randomUUID();
        System.arraycopy(extraArgs, 0, args, 1, extraArgs.length);
        try (ConfigurableApplicationContext app = new SpringApplicationBuilder(Jpa01Application.class)
                .web(WebApplicationType.NONE).run(args)) {
            Object ddlAuto = app.getBean(EntityManagerFactory.class).getProperties().get("hibernate.hbm2ddl.auto");
            long tables = app.getBean(JdbcClient.class)
                    .sql("SELECT count(*) FROM information_schema.tables WHERE table_name = 'PRODUCT'")
                    .query(Long.class).single();
            return new StartResult((String) ddlAuto, tables == 1);
        }
    }

    @Test
    @DisplayName("Sekcja 2: baza wbudowana bez Flyway → Boot domyślnie ustawia create-drop (tabele powstają)")
    void embeddedWithoutFlyway() {
        assertThat(start()).isEqualTo(new StartResult("create-drop", true));
    }

    @Test
    @DisplayName("Sekcja 2: baza wbudowana z włączonym Flyway → domyślnie none: Boot nic nie ustawia, Hibernate nie tworzy tabel")
    void embeddedWithFlyway() {
        assertThat(start("--spring.flyway.enabled=true", "--spring.flyway.locations=classpath:db/migration/jpa01-brak"))
                .isEqualTo(new StartResult(null, false));
    }
}

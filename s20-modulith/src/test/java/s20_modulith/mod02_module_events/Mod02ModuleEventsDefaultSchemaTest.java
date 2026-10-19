package s20_modulith.mod02_module_events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test sekcji 5 lekcji Mod02ModuleEvents, przypadek „ustawienia domyślne”: BEZ profilu {@code mod02} (więc bez
 * {@code spring.modulith.events.jdbc.schema-initialization.enabled=false}) i z Flyway zatrzymanym na V1. Tabelę
 * EVENT_PUBLICATION zakłada wtedy sam Spring Modulith (skrypt schema-h2.sql z jar-a). Osobna baza H2.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/mod02",
        "spring.flyway.target=1",
        "spring.datasource.url=jdbc:h2:mem:mod02-default-schema;DB_CLOSE_DELAY=-1"})
class Mod02ModuleEventsDefaultSchemaTest {

    @Autowired
    private JdbcClient jdbc;

    @Test
    @DisplayName("5b. Domyślnie Spring Modulith 2.1 sam tworzy tabelę EVENT_PUBLICATION (schemat v2 ze STATUS)")
    void modulithCreatesTableByDefault() {
        assertThat(jdbc.sql("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'")
                .query(String.class).list())
                .contains("EVENT_PUBLICATION", "ORDERS", "STOCK");

        assertThat(jdbc.sql("""
                        SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_NAME = 'EVENT_PUBLICATION' ORDER BY ORDINAL_POSITION""")
                .query(String.class).list())
                .containsExactly("ID", "COMPLETION_DATE", "EVENT_TYPE", "LISTENER_ID", "PUBLICATION_DATE",
                        "SERIALIZED_EVENT", "STATUS", "COMPLETION_ATTEMPTS", "LAST_RESUBMISSION_DATE");
    }
}

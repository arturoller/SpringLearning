package s20_modulith.mod02_module_events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import s20_modulith.mod02_module_events.orders.OrderManagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test sekcji 5 lekcji Mod02ModuleEvents, przypadek „brak tabeli”: profil {@code mod02} wyłącza zakładanie tabeli
 * przez Spring Modulith, a {@code spring.flyway.target=1} każe Flyway zatrzymać się na migracji V1 — więc tabeli
 * EVENT_PUBLICATION (migracja V2) nie ma. Osobna baza H2 ({@code mod02-no-table}), bo inne testy lekcji migrują
 * swoją bazę do końca.
 */
@SpringBootTest(properties = {
        "spring.flyway.target=1",
        "spring.datasource.url=jdbc:h2:mem:mod02-no-table;DB_CLOSE_DELAY=-1"})
@ActiveProfiles("mod02")
class Mod02ModuleEventsSchemaTest {

    @Autowired
    private OrderManagement orders;

    @Autowired
    private JdbcClient jdbc;

    @Test
    @DisplayName("5a. Bez tabeli EVENT_PUBLICATION publikacja zdarzenia wywraca całą transakcję zamówienia")
    void missingTableBreaksBusinessTransaction() {
        assertThatThrownBy(() -> orders.placeOrder("ZAM-501", "KSI-001", 1))
                .isInstanceOf(BadSqlGrammarException.class)
                .hasMessageContaining("INSERT INTO EVENT_PUBLICATION");

        // zamówienie wycofane razem z nieudanym zapisem publikacji — ta sama transakcja
        assertThat(jdbc.sql("SELECT COUNT(*) FROM orders").query(Integer.class).single()).isZero();
    }
}

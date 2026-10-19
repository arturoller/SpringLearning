package s17_events.evt03_async_and_outbox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Evt03AsyncAndOutbox. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). Baza H2 ze schematem migracji V1. */
@Tag("cwiczenie")
class Evt03AsyncAndOutboxExercisesTest {

    private EmbeddedDatabase database;
    private JdbcClient jdbc;
    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        database = Evt03TestDatabase.create();
        jdbc = JdbcClient.create(database);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(database));
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    private int count(String table) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table).query(Integer.class).single();
    }

    private void insertOutbox(String orderId, int attempts) {
        jdbc.sql("INSERT INTO outbox (order_id, type, payload, attempts) VALUES (:orderId, 'OrderPlaced', 'x', :attempts)")
                .param("orderId", orderId).param("attempts", attempts).update();
    }

    @Test
    @DisplayName("Ćwiczenie 1: zamówienie i wiadomość outbox w jednej transakcji (albo oba, albo żadne)")
    void exercise1() {
        Evt03AsyncAndOutbox.exercise1(transactions, jdbc, "ZAM-001", "Jan Kowalski", false);
        assertThat(count("orders")).isEqualTo(1);
        assertThat(jdbc.sql("SELECT payload FROM outbox WHERE order_id = 'ZAM-001' AND type = 'OrderPlaced'")
                .query(String.class).single()).isEqualTo("orderId=ZAM-001;customer=Jan Kowalski");

        assertThatThrownBy(() -> Evt03AsyncAndOutbox.exercise1(transactions, jdbc, "ZAM-002", "Maria Nowak", true))
                .isInstanceOf(IllegalStateException.class).hasMessage("Płatność odrzucona");
        assertThat(count("orders")).isEqualTo(1);   // ZAM-002 wycofane razem z wiadomością
        assertThat(count("outbox")).isEqualTo(1);
    }

    @Test
    @DisplayName("Ćwiczenie 2: przekaźnik oznacza wysłane po sukcesie, a przy błędzie zwiększa licznik prób i jedzie dalej")
    void exercise2() {
        insertOutbox("ZAM-001", 0);
        insertOutbox("ZAM-002", 0);
        insertOutbox("ZAM-003", 0);
        List<String> delivered = new ArrayList<>();
        Consumer<OutboxMessage> sender = message -> {
            if (message.orderId().equals("ZAM-002")) {
                throw new IllegalStateException("Serwer poczty niedostępny");
            }
            delivered.add(message.orderId());
        };

        assertThat(Evt03AsyncAndOutbox.exercise2(jdbc, sender)).isEqualTo(2);

        assertThat(delivered).containsExactly("ZAM-001", "ZAM-003");
        assertThat(jdbc.sql("SELECT order_id FROM outbox WHERE sent = FALSE").query(String.class).list())
                .containsExactly("ZAM-002");
        assertThat(jdbc.sql("SELECT attempts FROM outbox WHERE order_id = 'ZAM-002'").query(Integer.class).single())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Ćwiczenie 3: odbiorca odporny na duplikaty obsługuje wiadomość tylko raz")
    void exercise3() {
        Set<Long> processedIds = new HashSet<>();
        List<Long> handled = new ArrayList<>();
        OutboxMessage message = new OutboxMessage(7, "ZAM-001", "OrderPlaced", "x", 0);

        assertThat(Evt03AsyncAndOutbox.exercise3(processedIds, message, m -> handled.add(m.id()))).isTrue();
        assertThat(Evt03AsyncAndOutbox.exercise3(processedIds, message, m -> handled.add(m.id()))).isFalse();

        assertThat(handled).containsExactly(7L);
    }

    @Test
    @DisplayName("Ćwiczenie 4: przekaźnik pomija wiadomości, które wyczerpały limit prób")
    void exercise4() {
        insertOutbox("ZAM-001", 0);
        insertOutbox("ZAM-002", 3);   // wyczerpała limit prób
        List<String> delivered = new ArrayList<>();

        int sent = Evt03AsyncAndOutbox.exercise4(jdbc, message -> delivered.add(message.orderId()), 3);

        assertThat(sent).isEqualTo(1);
        assertThat(delivered).containsExactly("ZAM-001");
        assertThat(jdbc.sql("SELECT order_id FROM outbox WHERE sent = FALSE").query(String.class).list())
                .containsExactly("ZAM-002");
    }
}

package s10_transactions.tx02_propagation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.annotation.Propagation;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test PUŁAPKI z sekcji 2 lekcji Tx02Propagation: REQUIRES_NEW potrzebuje DRUGIEGO połączenia z puli.
 *
 * <p>Osobna klasa, bo potrzebuje innych ustawień puli HikariCP: tylko 2 połączenia (maximum-pool-size = 2; jedno
 * nie wystarcza nawet Flywayowi przy starcie aplikacji — sprawdzone, aplikacja nie wstaje) i krótkie czekanie na wolne
 * połączenie (connection-timeout = 250 — w milisekundach; sama liczba, bez „ms”, bo to pole typu long
 * w HikariDataSource; 250 ms to najmniejsza wartość, na jaką pozwala HikariCP). Własna baza (jdbc:h2:mem:tx02pool),
 * żeby nie mieszać danych z Tx02PropagationTest.</p>
 *
 * <p>Jedno z dwóch połączeń zajmuje w teście „inne żądanie” (zwykłe {@code dataSource.getConnection()}). To model
 * w małej skali sytuacji z produkcji: pula 10 połączeń i 10 równoległych żądań, z których każde trzyma jedno
 * połączenie i czeka na drugie.</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tx02pool",
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.connection-timeout=250"
})
@ActiveProfiles("tx02")
class Tx02PropagationPoolTest {

    @Autowired
    OrderService orderService;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.sql("delete from orders").update();
        jdbc.sql("delete from audit_log").update();
    }

    @Test
    @DisplayName("2. PUŁAPKA: brak wolnego DRUGIEGO połączenia — REQUIRES_NEW nie startuje, cała operacja pada")
    void requiresNewStarvesThePool() throws Exception {
        try (Connection otherRequest = dataSource.getConnection()) {                // „inne żądanie” zajmuje 1 z 2
            assertThatThrownBy(() -> orderService.placeOrderThenFail("Kawa ziarnista 1kg", 1, Propagation.REQUIRES_NEW))
                    .isInstanceOf(CannotCreateTransactionException.class)
                    .hasMessageStartingWith("Could not open JPA EntityManager for transaction")
                    .rootCause().hasMessageContaining("Connection is not available, request timed out after");
        }

        assertThat(jdbc.sql("select count(*) from orders").query(Integer.class).single())
                .as("zamówienie wycofane").isZero();
    }
}

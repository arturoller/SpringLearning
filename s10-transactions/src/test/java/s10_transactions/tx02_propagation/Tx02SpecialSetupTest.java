package s10_transactions.tx02_propagation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.NestedTransactionNotSupportedException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx02Propagation, które potrzebują NIETYPOWYCH ustawień (pula z 1 połączeniem, krótki czas oczekiwania na
 * blokadę) — każdy uruchamia własną aplikację z własną bazą (UUID w nazwie), żeby nie psuć innych testów.
 * Czekanie jest ograniczone ustawieniami (250 ms puli, 200 ms blokady H2) — test nie może się zawiesić.
 */
class Tx02SpecialSetupTest {

    private static ConfigurableApplicationContext start(String urlSuffix, String... extraArgs) {
        List<String> args = new ArrayList<>(List.of("--spring.profiles.active=tx02",
                "--spring.datasource.url=jdbc:h2:mem:tx02-" + UUID.randomUUID() + urlSuffix));
        args.addAll(List.of(extraArgs));
        return new SpringApplicationBuilder(Tx02Application.class).web(WebApplicationType.NONE)
                .run(args.toArray(String[]::new));
    }

    @Test
    @DisplayName("Sekcja 3: REQUIRES_NEW przy puli z 1 połączeniem — drugiego połączenia nie ma, transakcja nie startuje")
    void section3RequiresNewExhaustsPool() {
        // Flyway przy starcie potrzebuje więcej niż jednego połączenia, więc tu schemat i dane wczytuje prostsze
        // spring.sql.init (s07_data_jdbc/Jdbc03SqlInit) z tych samych plików migracji.
        try (ConfigurableApplicationContext app = start("", "--spring.datasource.hikari.maximum-pool-size=1",
                "--spring.datasource.hikari.connection-timeout=250", "--spring.flyway.enabled=false",
                "--spring.sql.init.mode=always",
                "--spring.sql.init.schema-locations=classpath:db/migration/tx02/V1__create_orders_stock_audit.sql",
                "--spring.sql.init.data-locations=classpath:db/migration/tx02/V2__insert_stock.sql")) {
            OrderService orders = app.getBean(OrderService.class);

            // placeOrder trzyma JEDYNE połączenie, a audit.record (REQUIRES_NEW) potrzebuje drugiego
            assertThatThrownBy(() -> orders.placeOrder("ZAM-1", "Mysz", 1))
                    .isInstanceOf(CannotCreateTransactionException.class)
                    .hasMessage("Could not open JPA EntityManager for transaction")
                    .rootCause()
                    .hasMessageContaining("Connection is not available, request timed out after");
            assertThat(Tx02TestData.orders(app.getBean(JdbcClient.class))).isEmpty();
        }
    }

    @Test
    @DisplayName("Sekcja 3: REQUIRES_NEW zmienia wiersz zablokowany przez zawieszoną transakcję — zakleszczenie (tu: limit czasu blokady)")
    void section3RequiresNewWaitsForOwnLock() {
        try (ConfigurableApplicationContext app = start(";LOCK_TIMEOUT=200")) {
            OrderService orders = app.getBean(OrderService.class);
            JdbcClient jdbc = app.getBean(JdbcClient.class);

            assertThatThrownBy(() -> orders.reserveAndCorrectInNewTransaction("Mysz"))
                    .isInstanceOf(org.springframework.dao.QueryTimeoutException.class)
                    .hasMessageContaining("Timeout trying to lock table \"STOCK\"");
            assertThat(Tx02TestData.stock(jdbc, "Mysz")).as("obie zmiany wycofane").isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Sekcja 4: JpaTransactionManager z nestedTransactionAllowed = true — Hibernate i tak nie obsługuje savepointów")
    void section4JpaManagerCannotDoSavepoints() {
        try (ConfigurableApplicationContext app = start("")) {
            app.getBean(JpaTransactionManager.class).setNestedTransactionAllowed(true);

            assertThatThrownBy(() -> app.getBean(NestedOrderService.class).placeOrderWithGiftUsingJpaManager("ZAM-1", "Mysz"))
                    .isInstanceOf(NestedTransactionNotSupportedException.class)
                    .hasMessage("JpaDialect does not support savepoints - check your JPA provider's capabilities");
        }
    }
}

package s11_testing.test05_test_configuration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Test05TestConfiguration: {@code @DynamicPropertySource} — właściwość, której wartość znamy dopiero
 * W CZASIE DZIAŁANIA testu. Tu: test uruchamia lokalny serwer-zaślepkę (stub) kursów walut na LOSOWYM wolnym porcie
 * (port 0) i podaje jego adres aplikacji. Żadnej prawdziwej sieci — tylko localhost.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class Test05TestConfigurationDynamicTest {

    /** Serwer HTTP z JDK (com.sun.net.httpserver) — startuje, zanim Spring zbuduje kontekst (inicjalizacja statyczna). */
    static final HttpServer RATES_STUB = startStub();

    static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);   // 0 = wybierz wolny port
            server.createContext("/rates/EUR", exchange -> {
                byte[] body = "4.25".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource   // wywoływane przy budowie kontekstu; dostawca () -> ... liczy wartość dopiero, gdy jest potrzebna
    static void exchangeRatesUrl(DynamicPropertyRegistry registry) {
        registry.add("shop.exchange.base-url", () -> "http://localhost:" + RATES_STUB.getAddress().getPort());
    }

    @AfterAll
    static void stopStub() {
        RATES_STUB.stop(0);
    }

    @Autowired
    ExchangeRateClient client;

    @Autowired
    EuroPriceCalculator calculator;

    @Test
    @DisplayName("4. @DynamicPropertySource: klient dostał adres zaślepki z losowym portem i odczytał kurs 4.25")
    void dynamicPropertyPointsClientAtStub() {
        assertThat(client.eurRate()).isEqualByComparingTo("4.25");
        assertThat(calculator.toEuro(new BigDecimal("42.50"))).isEqualByComparingTo("10.00");   // 42.50 / 4.25
    }
}

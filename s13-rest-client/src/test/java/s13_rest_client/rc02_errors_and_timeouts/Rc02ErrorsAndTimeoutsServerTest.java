package s13_rest_client.rc02_errors_and_timeouts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rc02ErrorsAndTimeouts — sekcja 4: PRAWDZIWE limity czasu na prawdziwym serwerze (RANDOM_PORT).
 *
 * <p>Determinizm bez zegara: udawany dostawca pytany o WOLNY czeka przy szlabanie (SlowSupplierGate) aż do jego
 * podniesienia — test podnosi go dopiero PO asercji (w finally). Serwer nie może więc odpowiedzieć przed upływem
 * limitu klienta, niezależnie od szybkości komputera. Nie mierzymy czasu i nie ma {@code sleep}.</p>
 *
 * <p>W tym kontekście {@code spring.http.clients.read-timeout=300ms} — krótki limit, żeby test trwał chwilę.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.http.clients.read-timeout=300ms")
@ActiveProfiles("rc02")
class Rc02ErrorsAndTimeoutsServerTest {

    @LocalServerPort
    int port;

    @Autowired
    RestClient.Builder builder;   // builder od Boota — niesie limity czasu z właściwości spring.http.clients.*

    @Autowired
    SlowSupplierGate gate;

    @Test
    @DisplayName("4. Limit z właściwości spring.http.clients.read-timeout: brak odpowiedzi w czasie → ResourceAccessException")
    void readTimeoutFromProperties() {
        RestClient client = builder.baseUrl("http://localhost:" + port).build();
        gate.close();
        try {
            assertThatThrownBy(() -> client.get().uri("/supplier/api/stock/WOLNY").retrieve().toBodilessEntity())
                    .isInstanceOf(ResourceAccessException.class)
                    .hasMessage("I/O error on GET request for \"http://localhost:" + port
                            + "/supplier/api/stock/WOLNY\": Request cancelled")
                    .hasRootCauseInstanceOf(HttpTimeoutException.class);
        } finally {
            gate.open();   // serwer kończy czekanie od razu — test nie zostawia wiszącego wątku na 5 s
        }
    }

    @Test
    @DisplayName("4. Po podniesieniu szlabanu to samo zapytanie przechodzi — winny był czas, nie adres")
    void sameRequestSucceedsWhenServerAnswers() {
        RestClient client = builder.baseUrl("http://localhost:" + port).build();
        gate.open();

        assertThat(client.get().uri("/supplier/api/stock/WOLNY").retrieve().requiredBody(StockLevel.class))
                .isEqualTo(new StockLevel("WOLNY", 1));
    }

    @Test
    @DisplayName("4. Limit ustawiony w kodzie: ClientHttpRequestFactoryBuilder + HttpClientSettings dla jednego klienta")
    void readTimeoutInCode() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk()
                        .build(HttpClientSettings.defaults().withTimeouts(Duration.ofSeconds(2), Duration.ofMillis(200))))
                .build();
        gate.close();
        try {
            assertThatThrownBy(() -> client.get().uri("/supplier/api/stock/WOLNY").retrieve().toBodilessEntity())
                    .isInstanceOf(ResourceAccessException.class)
                    .hasRootCauseInstanceOf(HttpTimeoutException.class);
        } finally {
            gate.open();
        }
    }

    @Test
    @DisplayName("4. Nikt nie słucha na porcie → ResourceAccessException z przyczyną ConnectException")
    void connectionRefused() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {   // weź wolny port i od razu go zwolnij
            closedPort = socket.getLocalPort();
        }
        RestClient client = builder.baseUrl("http://localhost:" + closedPort).build();

        assertThatThrownBy(() -> client.get().uri("/supplier/api/stock/ELE-001").retrieve().toBodilessEntity())
                .isInstanceOf(ResourceAccessException.class)
                .cause()
                .isInstanceOf(ConnectException.class);
    }

    @Test
    @DisplayName("4. PUŁAPKA: stara nazwa z Boot 3 (spring.http.client.read-timeout) w Boot 4 nie ustawia limitu (pytanie 4)")
    void oldPropertyNameIsIgnored() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class));

        runner.withPropertyValues("spring.http.client.read-timeout=300ms")      // Boot 3.x — bez „s”
                .run(context -> assertThat(context.getBean(HttpClientSettings.class).readTimeout()).isNull());
        runner.withPropertyValues("spring.http.clients.read-timeout=300ms")     // Boot 4 — „clients”
                .run(context -> assertThat(context.getBean(HttpClientSettings.class).readTimeout())
                        .isEqualTo(Duration.ofMillis(300)));
    }
}

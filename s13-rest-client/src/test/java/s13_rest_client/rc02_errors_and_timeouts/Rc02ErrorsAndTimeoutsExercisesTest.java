package s13_rest_client.rc02_errors_and_timeouts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Testy ĆWICZEŃ lekcji Rc02ErrorsAndTimeouts. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Ćwiczenia 1 i 3 — atrapa dostawcy w pamięci; ćwiczenie 4 — prawdziwy serwer na losowym porcie i szlaban.
 */
@Tag("cwiczenie")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("rc02")
class Rc02ErrorsAndTimeoutsExercisesTest {

    private static final String STOCK_URL = "http://dostawca.test/supplier/api/stock/";

    @LocalServerPort
    int port;

    @Autowired
    SlowSupplierGate gate;

    @Test
    @DisplayName("Ćwiczenie 1: liczba sztuk albo -1, gdy dostawca odpowie 404")
    void exercise1() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://dostawca.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(STOCK_URL + "ELE-001"))
                .andRespond(withSuccess("{\"sku\":\"ELE-001\",\"available\":7}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(STOCK_URL + "XXX-999")).andRespond(withResourceNotFound());
        server.expect(requestTo(STOCK_URL + "AWARIA")).andRespond(withServerError());
        RestClient client = builder.build();

        assertThat(Rc02ErrorsAndTimeouts.exercise1(client, "ELE-001")).isEqualTo(7);
        assertThat(Rc02ErrorsAndTimeouts.exercise1(client, "XXX-999")).isEqualTo(-1);
        assertThatThrownBy(() -> Rc02ErrorsAndTimeouts.exercise1(client, "AWARIA"))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 2: opis wyjątku klienta HTTP po polsku")
    void exercise2() {
        assertThat(Rc02ErrorsAndTimeouts.exercise2(httpError(HttpStatus.NOT_FOUND))).isEqualTo("brak produktu");
        assertThat(Rc02ErrorsAndTimeouts.exercise2(httpError(HttpStatus.CONFLICT))).isEqualTo("błąd zapytania: 409");
        assertThat(Rc02ErrorsAndTimeouts.exercise2(httpError(HttpStatus.SERVICE_UNAVAILABLE)))
                .isEqualTo("awaria dostawcy: 503");
        assertThat(Rc02ErrorsAndTimeouts.exercise2(new ResourceAccessException("Read timed out")))
                .isEqualTo("brak połączenia albo przekroczony czas");
        assertThat(Rc02ErrorsAndTimeouts.exercise2(new RestClientException("coś innego"))).isEqualTo("inny błąd");
    }

    @Test
    @DisplayName("Ćwiczenie 3: 404 i 503 jako wyjątki domenowe, z wartością Retry-After w komunikacie")
    void exercise3() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://dostawca.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(STOCK_URL + "SPO-001"))
                .andRespond(withSuccess("{\"sku\":\"SPO-001\",\"available\":120}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(STOCK_URL + "XXX-999")).andRespond(withResourceNotFound());
        server.expect(requestTo(STOCK_URL + "PRZERWA")).andRespond(withServiceUnavailable().header("Retry-After", "120"));
        RestClient client = builder.build();

        assertThat(Rc02ErrorsAndTimeouts.exercise3(client, "SPO-001")).isEqualTo(new StockLevel("SPO-001", 120));
        assertThatThrownBy(() -> Rc02ErrorsAndTimeouts.exercise3(client, "XXX-999"))
                .isInstanceOf(ProductNotFoundAtSupplierException.class);
        assertThatThrownBy(() -> Rc02ErrorsAndTimeouts.exercise3(client, "PRZERWA"))
                .isInstanceOf(SupplierUnavailableException.class)
                .hasMessage("Przerwa u dostawcy, spróbuj za 120 s");
    }

    @Test
    @DisplayName("Ćwiczenie 4: klient z limitem odczytu — zamknięty szlaban → ResourceAccessException, otwarty → wynik")
    void exercise4() {
        RestClient client = Rc02ErrorsAndTimeouts.exercise4("http://localhost:" + port, Duration.ofMillis(200));
        gate.close();
        try {
            assertThatThrownBy(() -> client.get().uri("/supplier/api/stock/WOLNY").retrieve().toBodilessEntity())
                    .isInstanceOf(ResourceAccessException.class);
        } finally {
            gate.open();
        }
        assertThat(client.get().uri("/supplier/api/stock/WOLNY").retrieve().requiredBody(StockLevel.class))
                .isEqualTo(new StockLevel("WOLNY", 1));
    }

    private static RestClientException httpError(HttpStatus status) {
        if (status.is4xxClientError()) {
            return HttpClientErrorException.create(status, status.getReasonPhrase(), new HttpHeaders(), new byte[0], null);
        }
        return HttpServerErrorException.create(status, status.getReasonPhrase(), new HttpHeaders(), new byte[0], null);
    }
}

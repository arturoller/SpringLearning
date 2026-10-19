package s13_rest_client.solutions.rc02_errors_and_timeouts;

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
import s13_rest_client.rc02_errors_and_timeouts.ProductNotFoundAtSupplierException;
import s13_rest_client.rc02_errors_and_timeouts.Rc02Application;
import s13_rest_client.rc02_errors_and_timeouts.SlowSupplierGate;
import s13_rest_client.rc02_errors_and_timeouts.StockLevel;
import s13_rest_client.rc02_errors_and_timeouts.SupplierUnavailableException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Sprawdzenia z Rc02ErrorsAndTimeoutsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * {@code classes = Rc02Application.class} — w pakiecie rozwiązań nie ma własnej klasy {@code @SpringBootApplication}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Rc02Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("rc02")
class Rc02ErrorsAndTimeoutsSolutionsTest {

    private static final String STOCK_URL = "http://dostawca.test/supplier/api/stock/";

    @LocalServerPort
    int port;

    @Autowired
    SlowSupplierGate gate;

    @Test
    @DisplayName("Wzorzec 1: liczba sztuk albo -1, gdy dostawca odpowie 404")
    void solution1() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://dostawca.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(STOCK_URL + "ELE-001"))
                .andRespond(withSuccess("{\"sku\":\"ELE-001\",\"available\":7}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(STOCK_URL + "XXX-999")).andRespond(withResourceNotFound());
        server.expect(requestTo(STOCK_URL + "AWARIA")).andRespond(withServerError());
        RestClient client = builder.build();

        assertThat(Rc02ErrorsAndTimeoutsSolutions.solution1(client, "ELE-001")).isEqualTo(7);
        assertThat(Rc02ErrorsAndTimeoutsSolutions.solution1(client, "XXX-999")).isEqualTo(-1);
        assertThatThrownBy(() -> Rc02ErrorsAndTimeoutsSolutions.solution1(client, "AWARIA"))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: opis wyjątku klienta HTTP po polsku")
    void solution2() {
        assertThat(Rc02ErrorsAndTimeoutsSolutions.solution2(httpError(HttpStatus.NOT_FOUND))).isEqualTo("brak produktu");
        assertThat(Rc02ErrorsAndTimeoutsSolutions.solution2(httpError(HttpStatus.CONFLICT)))
                .isEqualTo("błąd zapytania: 409");
        assertThat(Rc02ErrorsAndTimeoutsSolutions.solution2(httpError(HttpStatus.SERVICE_UNAVAILABLE)))
                .isEqualTo("awaria dostawcy: 503");
        assertThat(Rc02ErrorsAndTimeoutsSolutions.solution2(new ResourceAccessException("Read timed out")))
                .isEqualTo("brak połączenia albo przekroczony czas");
        assertThat(Rc02ErrorsAndTimeoutsSolutions.solution2(new RestClientException("coś innego")))
                .isEqualTo("inny błąd");
    }

    @Test
    @DisplayName("Wzorzec 3: 404 i 503 jako wyjątki domenowe, z wartością Retry-After w komunikacie")
    void solution3() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://dostawca.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(STOCK_URL + "SPO-001"))
                .andRespond(withSuccess("{\"sku\":\"SPO-001\",\"available\":120}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(STOCK_URL + "XXX-999")).andRespond(withResourceNotFound());
        server.expect(requestTo(STOCK_URL + "PRZERWA")).andRespond(withServiceUnavailable().header("Retry-After", "120"));
        RestClient client = builder.build();

        assertThat(Rc02ErrorsAndTimeoutsSolutions.solution3(client, "SPO-001")).isEqualTo(new StockLevel("SPO-001", 120));
        assertThatThrownBy(() -> Rc02ErrorsAndTimeoutsSolutions.solution3(client, "XXX-999"))
                .isInstanceOf(ProductNotFoundAtSupplierException.class);
        assertThatThrownBy(() -> Rc02ErrorsAndTimeoutsSolutions.solution3(client, "PRZERWA"))
                .isInstanceOf(SupplierUnavailableException.class)
                .hasMessage("Przerwa u dostawcy, spróbuj za 120 s");
    }

    @Test
    @DisplayName("Wzorzec 4: klient z limitem odczytu — zamknięty szlaban → ResourceAccessException, otwarty → wynik")
    void solution4() {
        RestClient client = Rc02ErrorsAndTimeoutsSolutions.solution4("http://localhost:" + port, Duration.ofMillis(200));
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

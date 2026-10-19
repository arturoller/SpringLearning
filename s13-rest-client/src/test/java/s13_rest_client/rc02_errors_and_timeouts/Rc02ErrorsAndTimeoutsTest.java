package s13_rest_client.rc02_errors_and_timeouts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Testy lekcji Rc02ErrorsAndTimeouts — sekcje 1–3 i 6 (bez sieci: MockRestServiceServer udaje dostawcę,
 * także jego awarie). {@code withException(...)} symuluje błąd sieci bez czekania na prawdziwy limit czasu.
 * Sekcja 4 (prawdziwe limity czasu) — Rc02ErrorsAndTimeoutsServerTest, sekcja 5 — Rc02ErrorsAndTimeoutsShopTest.
 */
@RestClientTest(components = SupplierStockClient.class, properties = "rc02.supplier.base-url=http://dostawca.test")
@ExtendWith(OutputCaptureExtension.class)   // przechwytuje System.out — czyli też logi (sekcja 6)
class Rc02ErrorsAndTimeoutsTest {

    private static final String STOCK_URL = "http://dostawca.test/supplier/api/stock/";
    private static final String PROBLEM_404 = """
            {"type":"about:blank","title":"Not Found","status":404,"detail":"Nieznany produkt: XXX-999"}""";

    @Autowired
    SupplierStockClient client;

    @Autowired
    MockRestServiceServer server;

    @Test
    @DisplayName("1. Domyślnie 404 → HttpClientErrorException.NotFound; ciało błędu da się odczytać jako ProblemDetail")
    void section1NotFound() {
        server.expect(requestTo(STOCK_URL + "XXX-999"))
                .andRespond(withResourceNotFound().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(PROBLEM_404));

        HttpClientErrorException.NotFound ex = catchThrowableOfType(HttpClientErrorException.NotFound.class,
                () -> client.stockWithDefaultErrors("XXX-999"));

        assertThat(ex).hasMessage("404 Not Found: \"" + PROBLEM_404 + "\"");
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail problem = ex.getResponseBodyAs(ProblemDetail.class);
        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).isEqualTo("Nieznany produkt: XXX-999");
    }

    @Test
    @DisplayName("1. Domyślnie 5xx → HttpServerErrorException (InternalServerError, ServiceUnavailable)")
    void section1ServerErrors() {
        server.expect(requestTo(STOCK_URL + "AWARIA")).andRespond(withServerError());
        server.expect(requestTo(STOCK_URL + "PRZERWA")).andRespond(withServiceUnavailable());

        assertThatThrownBy(() -> client.stockWithDefaultErrors("AWARIA"))
                .isInstanceOf(HttpServerErrorException.InternalServerError.class)
                .hasMessage("500 Internal Server Error: [no body]");
        assertThatThrownBy(() -> client.stockWithDefaultErrors("PRZERWA"))
                .isInstanceOf(HttpServerErrorException.ServiceUnavailable.class)
                .hasMessage("503 Service Unavailable: [no body]");
    }

    @Test
    @DisplayName("1. Hierarchia: każdy błąd statusu to RestClientResponseException; nietypowy kod 418 → sama klasa bazowa")
    void section1Hierarchy() {
        server.expect(requestTo(STOCK_URL + "CZAJNIK")).andRespond(withRawStatus(418));

        assertThatThrownBy(() -> client.stockWithDefaultErrors("CZAJNIK"))
                .isExactlyInstanceOf(HttpClientErrorException.class)   // nie ma podklasy dla 418
                .isInstanceOf(HttpStatusCodeException.class)
                .isInstanceOf(RestClientResponseException.class)
                .isInstanceOf(RestClientException.class)
                .hasMessage("418 I'm a teapot: [no body]");
    }

    @Test
    @DisplayName("2. onStatus: 404 → ProductNotFoundAtSupplierException, 5xx → SupplierUnavailableException")
    void section2() {
        server.expect(requestTo(STOCK_URL + "XXX-999")).andRespond(withResourceNotFound());
        server.expect(requestTo(STOCK_URL + "PRZERWA")).andRespond(withServiceUnavailable());

        assertThatThrownBy(() -> client.stock("XXX-999"))
                .isInstanceOf(ProductNotFoundAtSupplierException.class)
                .hasMessage("Dostawca nie zna produktu XXX-999");
        assertThatThrownBy(() -> client.stock("PRZERWA"))
                .isInstanceOf(SupplierUnavailableException.class)
                .hasMessage("Dostawca odpowiedział 503 SERVICE_UNAVAILABLE");
    }

    @Test
    @DisplayName("2. Status bez własnego onStatus (400) obsługuje dalej domyślny handler → HttpClientErrorException.BadRequest")
    void section2Unhandled() {
        server.expect(requestTo(STOCK_URL + "ELE-001")).andRespond(withBadRequest());

        assertThatThrownBy(() -> client.stock("ELE-001")).isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    @DisplayName("2. Błąd sieci to nie status: ResourceAccessException → SupplierUnavailableException z przyczyną")
    void section2NetworkError() {
        server.expect(requestTo(STOCK_URL + "ELE-001"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.stock("ELE-001"))
                .isInstanceOf(SupplierUnavailableException.class)
                .hasMessage("Brak odpowiedzi od dostawcy")
                .cause()
                .isInstanceOf(ResourceAccessException.class)
                .hasMessage("I/O error on GET request for \"http://dostawca.test/supplier/api/stock/ELE-001\": "
                        + "Read timed out");
    }

    @Test
    @DisplayName("3. exchange: 200 → Optional z wynikiem, 404 → Optional.empty(), 500 → wyjątek z createException()")
    void section3() {
        server.expect(requestTo(STOCK_URL + "ELE-001"))
                .andRespond(withSuccess("{\"sku\":\"ELE-001\",\"available\":7}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(STOCK_URL + "XXX-999")).andRespond(withResourceNotFound());
        server.expect(requestTo(STOCK_URL + "AWARIA")).andRespond(withServerError());

        assertThat(client.findStock("ELE-001")).contains(new StockLevel("ELE-001", 7));
        assertThat(client.findStock("XXX-999")).isEmpty();
        assertThatThrownBy(() -> client.findStock("AWARIA"))
                .isInstanceOf(HttpServerErrorException.InternalServerError.class);
    }

    @Test
    @DisplayName("6. Interceptor loguje każde wywołanie: metoda, adres, status albo błąd sieci")
    void section6(CapturedOutput output) {
        server.expect(requestTo(STOCK_URL + "ELE-001"))
                .andRespond(withSuccess("{\"sku\":\"ELE-001\",\"available\":7}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(STOCK_URL + "XXX-999")).andRespond(withResourceNotFound());
        server.expect(requestTo(STOCK_URL + "SPO-001"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        client.findStock("ELE-001");
        client.findStock("XXX-999");
        assertThatThrownBy(() -> client.findStock("SPO-001")).isInstanceOf(ResourceAccessException.class);

        assertThat(output.getOut())
                .contains("Dostawca: GET http://dostawca.test/supplier/api/stock/ELE-001 → 200 OK")
                .contains("Dostawca: GET http://dostawca.test/supplier/api/stock/XXX-999 → 404 NOT_FOUND")
                .contains("Dostawca: GET http://dostawca.test/supplier/api/stock/SPO-001 → błąd sieci: "
                        + "java.net.SocketTimeoutException: Read timed out");
    }
}

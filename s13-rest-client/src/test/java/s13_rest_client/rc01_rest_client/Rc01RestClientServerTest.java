package s13_rest_client.rc01_rest_client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rc01RestClient — sekcja 6: ten sam klient przeciwko PRAWDZIWEMU serwerowi HTTP.
 *
 * <p>{@code RANDOM_PORT} startuje Tomcata na losowym wolnym porcie, razem z FakeSupplierController („dostawcą”).
 * Zapytania idą naprawdę przez sieć (localhost), JSON pisze prawdziwy kontroler — test sprawdza całą drogę, której
 * MockRestServiceServer nie widzi (serializacja po stronie serwera, nagłówki wymagane przez serwer, kody błędów).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("rc01")
class Rc01RestClientServerTest {

    @LocalServerPort
    int port;

    @Autowired
    RestClient.Builder builder;   // prototype: dostajemy NOWY builder od Boota (z konwerterami JSON itd.)

    SupplierClient client;

    @BeforeEach
    void createClient() {
        // Adres serwera znamy dopiero PO starcie (losowy port), więc klienta tworzymy sami — tym samym konstruktorem,
        // którego używa Spring. Bean SupplierClient z kontekstu wskazuje na server.port, a w tym teście to 0.
        client = new SupplierClient(builder, "http://localhost:" + port, "sklep-01");
    }

    @Test
    @DisplayName("6. Prawdziwy serwer: GET oferty — JSON z kontrolera dostawcy wraca jako rekord")
    void offer() {
        assertThat(client.offer("ELE-001"))
                .isEqualTo(new SupplierOffer("ELE-001", "Laptop Pro 14", new BigDecimal("5499.99"), 7));
    }

    @Test
    @DisplayName("6. Prawdziwy serwer: lista ofert z kategorii KSIAZKI")
    void offersInCategory() {
        assertThat(client.offersInCategory("KSIAZKI")).extracting(SupplierOffer::sku).containsExactly("KSI-001");
    }

    @Test
    @DisplayName("6. Prawdziwy serwer: POST zamówienia → 201 Created, Location i potwierdzenie PRZYJETE")
    void placeOrder() {
        ResponseEntity<SupplierOrderConfirmation> response = client.placeOrder("KSI-001", 3);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SupplierOrderConfirmation confirmation = response.getBody();
        assertThat(confirmation).isNotNull();
        assertThat(confirmation.status()).isEqualTo("PRZYJETE");
        assertThat(response.getHeaders().getLocation()).hasToString("/supplier/api/orders/" + confirmation.orderId());
    }

    @Test
    @DisplayName("6. Bez nagłówka X-Shop-Id dostawca odpowiada 400 — dlatego klient ustawia go jako domyślny")
    void missingShopIdHeader() {
        RestClient withoutDefaults = RestClient.create("http://localhost:" + port);

        assertThatThrownBy(() -> withoutDefaults.get().uri("/supplier/api/offers/ELE-001").retrieve().toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }
}

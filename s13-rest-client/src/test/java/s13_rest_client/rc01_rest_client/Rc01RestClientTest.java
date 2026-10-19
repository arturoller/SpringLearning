package s13_rest_client.rc01_rest_client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Testy lekcji Rc01RestClient — „wynik” sekcji 1–5 i 7.
 *
 * <p>{@code @RestClientTest} (test klienta REST) to WYCINEK (slice) kontekstu: tylko SupplierClient, builder RestClienta
 * od Boota i {@code MockRestServiceServer} (udawany serwer w pamięci). Kontrolery, w tym FakeSupplierController,
 * NIE są ładowane. Żadnej sieci: zapytanie nie wychodzi z JVM — MockRestServiceServer podstawia się pod bibliotekę HTTP,
 * sprawdza zapytanie z oczekiwaniami ({@code expect}) i odsyła przygotowaną odpowiedź ({@code andRespond}).
 * Adres {@code http://dostawca.test} nie musi istnieć — nikt się z nim nie łączy.</p>
 */
@RestClientTest(components = SupplierClient.class,
        properties = {"rc01.supplier.base-url=http://dostawca.test", "rc01.supplier.shop-id=sklep-01"})
class Rc01RestClientTest {

    private static final String OFFER_JSON = """
            {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99,"available":7}""";

    @Autowired
    SupplierClient client;

    @Autowired
    MockRestServiceServer server;   // Boot podpiął go pod builder, z którego SupplierClient zbudował RestClient

    @Test
    @DisplayName("1. Klient z buildera Boota: adres bazowy + ścieżka i nagłówki domyślne w każdym zapytaniu")
    void section1() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/ELE-001"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Shop-Id", "sklep-01"))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(OFFER_JSON, MediaType.APPLICATION_JSON));

        client.offer("ELE-001");

        server.verify();   // verify = sprawdź, że KAŻDE oczekiwane zapytanie naprawdę zostało wysłane
    }

    @Test
    @DisplayName("2. GET + body(...): JSON dostawcy zamienia się w rekord SupplierOffer")
    void section2() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/ELE-001"))
                .andRespond(withSuccess(OFFER_JSON, MediaType.APPLICATION_JSON));

        SupplierOffer offer = client.offer("ELE-001");

        assertThat(offer).isEqualTo(new SupplierOffer("ELE-001", "Laptop Pro 14", new BigDecimal("5499.99"), 7));
    }

    @Test
    @DisplayName("2. Nowe, nieznane pole w JSON-ie dostawcy jest pomijane — klient działa dalej (pytanie 3)")
    void section2UnknownField() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/ELE-001"))
                .andRespond(withSuccess("""
                        {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99,"available":7,"promocja":"-10%"}""",
                        MediaType.APPLICATION_JSON));

        assertThat(client.offer("ELE-001").available()).isEqualTo(7);
    }

    @Test
    @DisplayName("2. PUŁAPKA: brak pola dla składowej int (available) → RestClientException (pytanie 4)")
    void section2MissingPrimitive() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/ELE-001"))
                .andRespond(withSuccess("""
                        {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99}""", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.offer("ELE-001"))
                .isInstanceOf(RestClientException.class)
                .hasMessage("Error while extracting response for type [s13_rest_client.rc01_rest_client.SupplierOffer]"
                        + " and content type [application/json]")
                .rootCause()
                .hasMessageContaining("Cannot map `null` into type `int`");
    }

    @Test
    @DisplayName("2. retrieve() bez body()/toEntity() NIC nie wysyła (pytanie 2)")
    void section2RetrieveAlone() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://dostawca.test");
        MockRestServiceServer standalone = MockRestServiceServer.bindTo(builder).build();   // osobny, „ręczny” udawany serwer
        standalone.expect(requestTo("http://dostawca.test/supplier/api/offers/ELE-001"))
                .andRespond(withSuccess(OFFER_JSON, MediaType.APPLICATION_JSON));
        RestClient restClient = builder.build();

        restClient.get().uri("/supplier/api/offers/{sku}", "ELE-001").retrieve();   // brak body(...) — brak wysłania

        assertThatThrownBy(standalone::verify)
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("Further request(s) expected leaving 1 unsatisfied expectation(s).")
                .hasMessageContaining("0 request(s) executed.");
    }

    @Test
    @DisplayName("3. toEntity(...): status, nagłówki (Cache-Control od dostawcy) i ciało w jednym obiekcie")
    void section3() {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("max-age=60");
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/ELE-001"))
                .andRespond(withSuccess(OFFER_JSON, MediaType.APPLICATION_JSON).headers(headers));

        ResponseEntity<SupplierOffer> response = client.offerWithHeaders("ELE-001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("max-age=60");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Laptop Pro 14");
    }

    @Test
    @DisplayName("4. Lista przez ParameterizedTypeReference + parametr zapytania ?category=")
    void section4() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers?category=ELEKTRONIKA"))
                .andRespond(withSuccess("""
                        [{"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99,"available":7},
                         {"sku":"ELE-002","name":"Smartfon X","price":2999.00,"available":0}]""",
                        MediaType.APPLICATION_JSON));

        List<SupplierOffer> offers = client.offersInCategory("ELEKTRONIKA");

        assertThat(offers).extracting(SupplierOffer::sku).containsExactly("ELE-001", "ELE-002");
        assertThat(offers.get(1).price()).isEqualTo(new BigDecimal("2999.00"));
    }

    @Test
    @DisplayName("4. PUŁAPKA: body(List.class) daje listę MAP, a nie rekordów — ClassCastException przy użyciu (pytanie 5)")
    @SuppressWarnings("unchecked")
    void section4RawList() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://dostawca.test");
        MockRestServiceServer standalone = MockRestServiceServer.bindTo(builder).build();
        standalone.expect(requestTo("http://dostawca.test/supplier/api/offers"))
                .andRespond(withSuccess("[" + OFFER_JSON + "]", MediaType.APPLICATION_JSON));

        List<SupplierOffer> offers = builder.build().get().uri("/supplier/api/offers").retrieve().body(List.class);

        assertThat(offers).isNotNull();
        Object first = offers.getFirst();
        assertThat(first).isInstanceOf(Map.class);   // Jackson nie wiedział „lista czego”, więc zrobił mapy
        assertThatThrownBy(() -> offers.getFirst().sku())
                .isInstanceOf(ClassCastException.class)
                .hasMessageContaining("java.util.LinkedHashMap cannot be cast to class "
                        + "s13_rest_client.rc01_rest_client.SupplierOffer");
    }

    @Test
    @DisplayName("5. POST: rekord jako JSON w ciele, odpowiedź 201 Created z nagłówkiem Location")
    void section5() {
        server.expect(requestTo("http://dostawca.test/supplier/api/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Shop-Id", "sklep-01"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"sku":"KSI-001","quantity":3}"""))
                .andRespond(withCreatedEntity(URI.create("/supplier/api/orders/DOST-1001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"orderId":"DOST-1001","sku":"KSI-001","quantity":3,"status":"PRZYJETE"}"""));

        ResponseEntity<SupplierOrderConfirmation> response = client.placeOrder("KSI-001", 3);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasToString("/supplier/api/orders/DOST-1001");
        assertThat(response.getBody()).isEqualTo(new SupplierOrderConfirmation("DOST-1001", "KSI-001", 3, "PRZYJETE"));
        server.verify();
    }

    @Test
    @DisplayName("7. RestTemplate (starszy styl) też działa; WebClient wymaga spring-webflux, którego w tym module nie ma")
    void section7() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer oldStyleServer = MockRestServiceServer.bindTo(restTemplate).build();
        oldStyleServer.expect(requestTo("http://dostawca.test/supplier/api/offers/ELE-001"))
                .andRespond(withSuccess(OFFER_JSON, MediaType.APPLICATION_JSON));

        SupplierOffer offer = restTemplate.getForObject(
                "http://dostawca.test/supplier/api/offers/{sku}", SupplierOffer.class, "ELE-001");

        assertThat(offer).isNotNull();
        assertThat(offer.name()).isEqualTo("Laptop Pro 14");
        assertThat(ClassUtils.isPresent("org.springframework.web.client.RestTemplate", null)).isTrue();
        assertThat(ClassUtils.isPresent("org.springframework.web.reactive.function.client.WebClient", null)).isFalse();
    }
}

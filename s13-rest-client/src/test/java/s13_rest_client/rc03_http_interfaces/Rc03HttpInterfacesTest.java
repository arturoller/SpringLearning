package s13_rest_client.rc03_http_interfaces;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.NotFoundRestClientAdapterDecorator;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Testy lekcji Rc03HttpInterfaces — sekcje 1, 2 i 4 BEZ Springa: proxy tworzymy ręcznie (SupplierApiFactory),
 * a dostawcę udaje MockRestServiceServer podpięty pod zwykły RestClient.Builder. Sekcja 3 (grupy w Boot 4) —
 * Rc03HttpInterfacesGroupTest.
 */
class Rc03HttpInterfacesTest {

    private static final String OFFER_JSON = """
            {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99,"available":7}""";

    MockRestServiceServer server;
    RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://dostawca.test")
                .defaultHeader("X-Shop-Id", "sklep-01");
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    @Test
    @DisplayName("1. Interfejs + adnotacje = gotowy klient: GET z @PathVariable składa adres i nagłówki")
    void section1() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/ELE-001"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))   // z @HttpExchange(accept)
                .andExpect(header("X-Shop-Id", "sklep-01"))                                // z RestClienta
                .andRespond(withSuccess(OFFER_JSON, MediaType.APPLICATION_JSON));
        SupplierApi api = SupplierApiFactory.create(restClient);

        SupplierOffer offer = api.offer("ELE-001");

        assertThat(offer).isEqualTo(new SupplierOffer("ELE-001", "Laptop Pro 14", new BigDecimal("5499.99"), 7));
        server.verify();
    }

    @Test
    @DisplayName("2. Spring tworzy obiekt pośredniczący (proxy JDK) — implementacji interfejsu nikt nie pisał")
    void section2Proxy() {
        SupplierApi api = SupplierApiFactory.create(restClient);

        assertThat(Proxy.isProxyClass(api.getClass())).isTrue();
        assertThat(api).isInstanceOf(SupplierApi.class);
    }

    @Test
    @DisplayName("2. @RequestParam → ?category=..., lista z typem odczytanym z sygnatury metody")
    void section2List() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers?category=ELEKTRONIKA"))
                .andRespond(withSuccess("[" + OFFER_JSON + "]", MediaType.APPLICATION_JSON));

        assertThat(SupplierApiFactory.create(restClient).offers("ELEKTRONIKA"))
                .extracting(SupplierOffer::sku).containsExactly("ELE-001");
    }

    @Test
    @DisplayName("2. @PostExchange + @RequestBody: rekord jako JSON, odpowiedź jako rekord")
    void section2Post() {
        server.expect(requestTo("http://dostawca.test/supplier/api/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"sku":"KSI-001","quantity":3}"""))
                .andRespond(withCreatedEntity(URI.create("/supplier/api/orders/DOST-1001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"orderId":"DOST-1001","sku":"KSI-001","quantity":3,"status":"PRZYJETE"}"""));

        SupplierOrderConfirmation confirmation = SupplierApiFactory.create(restClient).order(new SupplierOrder("KSI-001", 3));

        assertThat(confirmation.orderId()).isEqualTo("DOST-1001");
    }

    @Test
    @DisplayName("4. Domyślnie 404 to wyjątek — dla zwykłego typu, ResponseEntity i Optional (pytanie 3)")
    void section4Default() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/XXX-999")).andRespond(withResourceNotFound());
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/XXX-999")).andRespond(withResourceNotFound());
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/XXX-999")).andRespond(withResourceNotFound());
        SupplierApi api = SupplierApiFactory.create(restClient);

        assertThatThrownBy(() -> api.offer("XXX-999")).isInstanceOf(HttpClientErrorException.NotFound.class);
        assertThatThrownBy(() -> api.offerEntity("XXX-999")).isInstanceOf(HttpClientErrorException.NotFound.class);
        assertThatThrownBy(() -> api.findOffer("XXX-999")).isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    @Test
    @DisplayName("4. NotFoundRestClientAdapterDecorator (Framework 7): 404 → null, ResponseEntity 404, Optional.empty()")
    void section4Decorator() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/XXX-999")).andRespond(withResourceNotFound());
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/XXX-999")).andRespond(withResourceNotFound());
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/XXX-999")).andRespond(withResourceNotFound());
        SupplierApi api = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .exchangeAdapterDecorator(NotFoundRestClientAdapterDecorator::new)   // owija adapter: 404 ≠ wyjątek
                .build()
                .createClient(SupplierApi.class);

        assertThat(api.offer("XXX-999")).isNull();
        ResponseEntity<SupplierOffer> entity = api.offerEntity("XXX-999");
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.findOffer("XXX-999")).isEmpty();
    }
}

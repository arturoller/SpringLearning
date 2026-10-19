package s13_rest_client.solutions.rc03_http_interfaces;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import s13_rest_client.rc03_http_interfaces.SupplierApi;
import s13_rest_client.rc03_http_interfaces.SupplierOffer;
import s13_rest_client.rc03_http_interfaces.SupplierOrder;
import s13_rest_client.rc03_http_interfaces.SupplierOrderConfirmation;
import s13_rest_client.solutions.rc03_http_interfaces.Rc03HttpInterfacesSolutions.SolutionSupplierApi;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Sprawdzenia z Rc03HttpInterfacesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Rc03HttpInterfacesSolutionsTest {

    private static final String OFFER_JSON = """
            {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99,"available":7}""";

    MockRestServiceServer server;
    RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://dostawca.test");
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    private SolutionSupplierApi solutionApi() {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build()
                .createClient(SolutionSupplierApi.class);
    }

    @Test
    @DisplayName("Wzorzec 1: GET oferty przez metodę interfejsu z @GetExchange i @PathVariable")
    void solution1() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/ELE-001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(OFFER_JSON, MediaType.APPLICATION_JSON));

        assertThat(solutionApi().offer("ELE-001").name()).isEqualTo("Laptop Pro 14");
        server.verify();
    }

    @Test
    @DisplayName("Wzorzec 2: lista ofert z parametrem ?category=")
    void solution2() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers?category=ELEKTRONIKA"))
                .andRespond(withSuccess("[" + OFFER_JSON + "]", MediaType.APPLICATION_JSON));

        assertThat(solutionApi().offers("ELEKTRONIKA")).extracting(SupplierOffer::sku).containsExactly("ELE-001");
        server.verify();
    }

    @Test
    @DisplayName("Wzorzec 3: POST zamówienia jako JSON z nagłówkiem Idempotency-Key")
    void solution3() {
        server.expect(requestTo("http://dostawca.test/supplier/api/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "zam-2026-0001"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"sku":"KSI-001","quantity":3}"""))
                .andRespond(withSuccess("""
                        {"orderId":"DOST-1001","sku":"KSI-001","quantity":3,"status":"PRZYJETE"}""",
                        MediaType.APPLICATION_JSON));

        SupplierOrderConfirmation confirmation =
                solutionApi().order(new SupplierOrder("KSI-001", 3), "zam-2026-0001");

        assertThat(confirmation.status()).isEqualTo("PRZYJETE");
        server.verify();
    }

    @Test
    @DisplayName("Wzorzec 4: proxy dowolnego interfejsu HTTP (sprawdzane na SupplierApi z lekcji)")
    void solution4() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/ELE-001"))
                .andRespond(withSuccess(OFFER_JSON, MediaType.APPLICATION_JSON));

        SupplierApi api = Rc03HttpInterfacesSolutions.solution4(restClient, SupplierApi.class);

        assertThat(Proxy.isProxyClass(api.getClass())).isTrue();
        assertThat(api.offer("ELE-001").sku()).isEqualTo("ELE-001");
        server.verify();
    }
}

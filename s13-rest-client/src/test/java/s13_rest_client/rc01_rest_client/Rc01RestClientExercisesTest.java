package s13_rest_client.rc01_rest_client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Testy ĆWICZEŃ lekcji Rc01RestClient. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Bez Springa: atrapę dostawcy (MockRestServiceServer) podpinamy ręcznie pod zwykły RestClient.Builder.
 */
@Tag("cwiczenie")
class Rc01RestClientExercisesTest {

    RestClient.Builder builder;
    MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();   // podpięcie PRZED build()
    }

    @Test
    @DisplayName("Ćwiczenie 1: nazwa produktu z oferty dostawcy")
    void exercise1() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/ELE-001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99,"available":7}""",
                        MediaType.APPLICATION_JSON));

        assertThat(Rc01RestClient.exercise1(builder.baseUrl("http://dostawca.test").build(), "ELE-001"))
                .isEqualTo("Laptop Pro 14");
        server.verify();
    }

    @Test
    @DisplayName("Ćwiczenie 2: dostępne oferty z kategorii, posortowane po sku")
    void exercise2() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers?category=ELEKTRONIKA"))
                .andRespond(withSuccess("""
                        [{"sku":"ELE-003","name":"Słuchawki BT","price":349.90,"available":25},
                         {"sku":"ELE-002","name":"Smartfon X","price":2999.00,"available":0},
                         {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99,"available":7}]""",
                        MediaType.APPLICATION_JSON));

        assertThat(Rc01RestClient.exercise2(builder.baseUrl("http://dostawca.test").build(), "ELEKTRONIKA"))
                .containsExactly("ELE-001", "ELE-003");
        server.verify();
    }

    @Test
    @DisplayName("Ćwiczenie 3: POST zamówienia jako JSON i adres z nagłówka Location")
    void exercise3() {
        server.expect(requestTo("http://dostawca.test/supplier/api/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"sku":"KSI-001","quantity":3}"""))
                .andRespond(withCreatedEntity(URI.create("/supplier/api/orders/DOST-1001")));

        assertThat(Rc01RestClient.exercise3(builder.baseUrl("http://dostawca.test").build(), "KSI-001", 3))
                .isEqualTo(URI.create("/supplier/api/orders/DOST-1001"));
        server.verify();
    }

    @Test
    @DisplayName("Ćwiczenie 4: klient z adresem bazowym i nagłówkami domyślnymi")
    void exercise4() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers/SPO-001"))
                .andExpect(header("X-Shop-Id", "sklep-07"))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        RestClient client = Rc01RestClient.exercise4(builder, "http://dostawca.test", "sklep-07");
        client.get().uri("/supplier/api/offers/SPO-001").retrieve().toBodilessEntity();

        server.verify();
    }
}

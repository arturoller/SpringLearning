package s13_rest_client.rc03_http_interfaces;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rc03HttpInterfaces — sekcja 3: grupa klientów z {@code @ImportHttpServices} i właściwości
 * {@code spring.http.serviceclient.dostawca.*} (profil rc03). Adres grupy nadpisujemy na http://dostawca.test,
 * a {@code @AutoConfigureMockRestServiceServer} podpina atrapę pod RestClient.Builder GRUPY — Boot stosuje
 * do grup te same dostosowania (customizers) co do zwykłego buildera.
 */
@SpringBootTest(properties = "spring.http.serviceclient.dostawca.base-url=http://dostawca.test")
@AutoConfigureMockRestServiceServer
@AutoConfigureMockMvc
@ActiveProfiles("rc03")
class Rc03HttpInterfacesGroupTest {

    private static final String OFFERS_JSON = """
            [{"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99,"available":7},
             {"sku":"ELE-002","name":"Smartfon X","price":2999.00,"available":0},
             {"sku":"ELE-003","name":"Słuchawki BT","price":349.90,"available":25}]""";

    @Autowired
    SupplierApi supplierApi;

    @Autowired
    ApplicationContext context;

    @Autowired
    MockRestServiceServer server;

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("3. @ImportHttpServices rejestruje bean-proxy „dostawca#...SupplierApi”")
    void section3Bean() {
        assertThat(context.getBeanNamesForType(SupplierApi.class))
                .containsExactly("dostawca#s13_rest_client.rc03_http_interfaces.SupplierApi");
        assertThat(Proxy.isProxyClass(supplierApi.getClass())).isTrue();
    }

    @Test
    @DisplayName("3. Adres i nagłówek X-Shop-Id grupy pochodzą z właściwości spring.http.serviceclient.dostawca.*")
    void section3Properties() {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers?category=ELEKTRONIKA"))
                .andExpect(header("X-Shop-Id", "sklep-01"))
                .andRespond(withSuccess(OFFERS_JSON, MediaType.APPLICATION_JSON));

        assertThat(supplierApi.offers("ELEKTRONIKA")).hasSize(3);
        server.verify();
    }

    @Test
    @DisplayName("3. Serwis i kontroler używają interfejsu jak zwykłego beana: najtańsza DOSTĘPNA oferta to ELE-003")
    void section3UsedLikeAnyBean() throws Exception {
        server.expect(requestTo("http://dostawca.test/supplier/api/offers?category=ELEKTRONIKA"))
                .andRespond(withSuccess(OFFERS_JSON, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/shop/cheapest").param("category", "ELEKTRONIKA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("ELE-003"));
    }

    @Test
    @DisplayName("3. Bez @ImportHttpServices nie ma beana SupplierApi — kontekst nie wstaje (pytanie 2)")
    void section3WithoutImport() {
        new ApplicationContextRunner()
                .withUserConfiguration(CheapestOfferService.class)   // serwis potrzebuje SupplierApi
                .run(started -> assertThat(started).hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(NoSuchBeanDefinitionException.class)
                        .hasMessageContaining("No qualifying bean of type "
                                + "'s13_rest_client.rc03_http_interfaces.SupplierApi' available"));
    }
}

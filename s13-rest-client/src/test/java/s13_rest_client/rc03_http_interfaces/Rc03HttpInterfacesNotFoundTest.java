package s13_rest_client.rc03_http_interfaces;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.support.NotFoundRestClientAdapterDecorator;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;

/**
 * Testy lekcji Rc03HttpInterfaces — sekcja 4 w Boot: dostosowanie GRUPY w kodzie przez bean
 * {@code RestClientHttpServiceGroupConfigurer} (konfigurator grup klientów RestClient, Spring Framework 7).
 * Zagnieżdżona {@code @TestConfiguration} dokłada go do konfiguracji aplikacji tylko w tym teście.
 */
@SpringBootTest(properties = "spring.http.serviceclient.dostawca.base-url=http://dostawca.test")
@AutoConfigureMockRestServiceServer
@ActiveProfiles("rc03")
class Rc03HttpInterfacesNotFoundTest {

    @TestConfiguration
    static class NotFoundAsEmpty {

        @Bean
        RestClientHttpServiceGroupConfigurer notFoundAsEmpty() {
            return groups -> groups.filterByName("dostawca")
                    .forEachProxyFactory((group, factory) ->
                            factory.exchangeAdapterDecorator(NotFoundRestClientAdapterDecorator::new));
        }
    }

    @Autowired
    SupplierApi supplierApi;

    @Autowired
    MockRestServiceServer server;

    @Test
    @DisplayName("4. Konfigurator grupy z NotFoundRestClientAdapterDecorator: 404 → null / 404 / Optional.empty()")
    void notFoundIsNotAnException() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo("http://dostawca.test/supplier/api/offers/XXX-999")).andRespond(withResourceNotFound());
        }

        assertThat(supplierApi.offer("XXX-999")).isNull();
        assertThat(supplierApi.offerEntity("XXX-999").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(supplierApi.findOffer("XXX-999")).isEmpty();
        server.verify();
    }
}

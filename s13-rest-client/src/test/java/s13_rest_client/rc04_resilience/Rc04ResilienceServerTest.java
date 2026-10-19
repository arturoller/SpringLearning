package s13_rest_client.rc04_resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Rc04Resilience — sekcja 5 na PRAWDZIWYM, „kapryśnym” serwerze (FlakySupplierController, RANDOM_PORT):
 * tu widać nie tylko liczbę zapytań, ale i skutek po stronie dostawcy — zapisane zamówienia.
 *
 * <p>Adres serwera znamy dopiero po starcie, więc SupplierClient tworzymy sami ({@code new}), a potem przepuszczamy go
 * przez post-procesory Springa: {@code initializeBean(...)} zwraca obiekt OWINIĘTY w proxy z @Retryable — dokładnie
 * tak, jak Spring robi to z każdym beanem. Bez tego kroku (sam {@code new}) ponawiania by nie było.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "rc04.retry.delay=0ms")
@ActiveProfiles("rc04")
class Rc04ResilienceServerTest {

    @LocalServerPort
    int port;

    @Autowired
    RestClient.Builder builder;

    @Autowired
    ConfigurableApplicationContext context;

    SupplierClient client;
    RestClient supplierAdmin;   // „okno” na stan dostawcy: lista zapisanych zamówień

    @BeforeEach
    void createClient() {
        SupplierClient plain = new SupplierClient(builder, "http://localhost:" + port);
        client = (SupplierClient) context.getAutowireCapableBeanFactory().initializeBean(plain, "supplierClientForTest");
        supplierAdmin = RestClient.create("http://localhost:" + port);
    }

    private List<SupplierOrderConfirmation> ordersAtSupplier() {
        return supplierAdmin.get().uri("/supplier/api/orders").retrieve()
                .requiredBody(new ParameterizedTypeReference<List<SupplierOrderConfirmation>>() {
                });
    }

    @Test
    @DisplayName("5. Prawdziwy kapryśny serwer: dwa 503, trzecia próba 200 — cena dociera do sklepu")
    void priceAfterRetries() {
        assertThat(AopUtils.isAopProxy(client)).isTrue();

        assertThat(client.price("KSI-001")).isEqualTo(new SupplierPrice("KSI-001", new BigDecimal("79.00")));
    }

    @Test
    @DisplayName("5. PUŁAPKA: ponowiony POST bez klucza → u dostawcy DWA zamówienia zamiast jednego (pytanie 5)")
    void unsafeOrderCreatesDuplicate() {
        int before = ordersAtSupplier().size();

        SupplierOrderConfirmation confirmation = client.placeOrderUnsafe(new SupplierOrder("SPO-001", 10));

        List<SupplierOrderConfirmation> orders = ordersAtSupplier();
        assertThat(orders).hasSize(before + 2);                               // pierwsza próba też zapisała!
        assertThat(orders.subList(before, before + 2))
                .extracting(SupplierOrderConfirmation::sku).containsExactly("SPO-001", "SPO-001");
        assertThat(confirmation.orderId()).isEqualTo(orders.getLast().orderId());
    }

    @Test
    @DisplayName("5. DOBRZE: ten sam Idempotency-Key w każdej próbie → u dostawcy JEDNO zamówienie")
    void idempotentOrderIsStoredOnce() {
        int before = ordersAtSupplier().size();

        SupplierOrderConfirmation confirmation = client.placeOrder(new SupplierOrder("KSI-001", 3), "zam-test-0001");

        List<SupplierOrderConfirmation> orders = ordersAtSupplier();
        assertThat(orders).hasSize(before + 1);
        assertThat(orders.getLast()).isEqualTo(confirmation);
    }
}

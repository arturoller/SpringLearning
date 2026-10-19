package s13_rest_client.rc04_resilience;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rc04Resilience — sekcje 2, 3, 4 i 6 na prawdziwych beanach (proxy z @Retryable/@ConcurrencyLimit),
 * a dostawcę udaje MockRestServiceServer. Liczymy ZAPYTANIA, nie czas: oczekiwania atrapy są uporządkowane
 * (najpierw 503, potem 200), a {@code server.verify()} sprawdza, że padło dokładnie tyle zapytań, ile trzeba.
 * {@code rc04.retry.delay=0ms} — ponowienia bez czekania.
 */
@SpringBootTest(properties = {"rc04.supplier.base-url=http://dostawca.test", "rc04.retry.delay=0ms"})
@AutoConfigureMockRestServiceServer
@AutoConfigureMockMvc
@ActiveProfiles("rc04")
@ExtendWith(OutputCaptureExtension.class)
class Rc04ResilienceTest {

    private static final String PRICE_URL = "http://dostawca.test/supplier/api/prices/ELE-001";
    private static final String PRICE_JSON = "{\"sku\":\"ELE-001\",\"price\":5499.99}";

    @Autowired
    SupplierClient client;

    @Autowired
    MockRestServiceServer server;

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("2. @Retryable: 503, 503, a potem 200 → wynik po 3 zapytaniach, wołający nie widzi awarii")
    void section2RetriesUntilSuccess(CapturedOutput output) {
        server.expect(times(2), requestTo(PRICE_URL)).andRespond(withServiceUnavailable());
        server.expect(requestTo(PRICE_URL)).andRespond(withSuccess(PRICE_JSON, MediaType.APPLICATION_JSON));

        SupplierPrice price = client.price("ELE-001");

        assertThat(price.price()).isEqualTo(new BigDecimal("5499.99"));
        server.verify();   // dokładnie 3 zapytania
        assertThat(output.getOut()).contains("Nieudana próba price: "
                + "org.springframework.web.client.HttpServerErrorException$ServiceUnavailable: 503 Service Unavailable: [no body]");
    }

    @Test
    @DisplayName("2. Próby się kończą: 4 × 503 (1 + maxRetries 3) → OSTATNI wyjątek trafia do wołającego")
    void section2GivesUpAfterMaxRetries() {
        server.expect(times(4), requestTo(PRICE_URL)).andRespond(withServiceUnavailable());

        assertThatThrownBy(() -> client.price("ELE-001"))
                .isInstanceOf(HttpServerErrorException.ServiceUnavailable.class)
                .hasMessage("503 Service Unavailable: [no body]");
        server.verify();   // 4 zapytania — ani jednego więcej
    }

    @Test
    @DisplayName("2. 404 nie jest na liście includes → żadnego ponowienia, jedno zapytanie")
    void section2NoRetryFor404() {
        server.expect(times(1), requestTo("http://dostawca.test/supplier/api/prices/XXX-999"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.price("XXX-999")).isInstanceOf(HttpClientErrorException.NotFound.class);
        server.verify();
    }

    @Test
    @DisplayName("2. Błąd sieci (ResourceAccessException) też jest ponawiany")
    void section2RetriesNetworkError() {
        server.expect(requestTo(PRICE_URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));
        server.expect(requestTo(PRICE_URL)).andRespond(withSuccess(PRICE_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.price("ELE-001").sku()).isEqualTo("ELE-001");
        server.verify();
    }

    @Test
    @DisplayName("3. PUŁAPKA: wywołanie z tej samej klasy (basketPrices → price) omija proxy — 0 ponowień (pytanie 3)")
    void section3SelfInvocation() {
        server.expect(times(1), requestTo(PRICE_URL)).andRespond(withServiceUnavailable());

        assertThat(AopUtils.isAopProxy(client)).isTrue();   // bean JEST proxy...
        assertThatThrownBy(() -> client.basketPrices(List.of("ELE-001")))
                .isInstanceOf(HttpServerErrorException.ServiceUnavailable.class);   // ...a mimo to bez ponowień
        server.verify();   // dokładnie 1 zapytanie
    }

    @Test
    @DisplayName("5. Ponawiany POST z kluczem: KAŻDA próba niesie ten sam nagłówek Idempotency-Key")
    void section5SameKeyInEveryAttempt() {
        server.expect(requestTo("http://dostawca.test/supplier/api/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "zam-0001"))
                .andRespond(withServiceUnavailable());
        server.expect(requestTo("http://dostawca.test/supplier/api/orders"))
                .andExpect(header("Idempotency-Key", "zam-0001"))
                .andRespond(withSuccess("""
                        {"orderId":"DOST-1001","sku":"KSI-001","quantity":3,"status":"PRZYJETE"}""",
                        MediaType.APPLICATION_JSON));

        SupplierOrderConfirmation confirmation = client.placeOrder(new SupplierOrder("KSI-001", 3), "zam-0001");

        assertThat(confirmation.orderId()).isEqualTo("DOST-1001");
        server.verify();
    }

    @Test
    @DisplayName("4. @ConcurrencyLimit(2): trzecie równoczesne wywołanie czeka; dostawca nie widzi więcej niż 2 naraz")
    void section4ConcurrencyLimit() throws Exception {
        AtomicInteger inside = new AtomicInteger();      // ile zapytań „u dostawcy” w tej chwili
        AtomicInteger maxInside = new AtomicInteger();   // największa zaobserwowana liczba równoczesnych
        CountDownLatch twoEntered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        server.expect(times(3), requestTo("http://dostawca.test/supplier/api/report")).andRespond(request -> {
            maxInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
            twoEntered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);   // „dostawca liczy raport”, dopóki test nie pozwoli skończyć
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            inside.decrementAndGet();
            return withSuccess("{\"products\":3,\"totalAvailable\":142}", MediaType.APPLICATION_JSON).createResponse(request);
        });

        Thread first = Thread.ofPlatform().start(client::stockReport);
        Thread second = Thread.ofPlatform().start(client::stockReport);
        Thread third = null;
        try {
            assertThat(twoEntered.await(5, TimeUnit.SECONDS)).isTrue();   // dwa wywołania są „u dostawcy”
            third = Thread.ofPlatform().start(client::stockReport);
            Thread waiting = third;
            await().atMost(5, TimeUnit.SECONDS).until(() -> waiting.getState() == Thread.State.WAITING);
            assertThat(inside.get()).as("trzecie wywołanie czeka przed dostawcą, nie u niego").isEqualTo(2);
        } finally {
            release.countDown();
            first.join();
            second.join();
            if (third != null) {
                third.join();
            }
        }
        assertThat(maxInside.get()).isEqualTo(2);
        server.verify();   // wszystkie 3 wywołania w końcu przeszły
    }

    @Test
    @DisplayName("6. Nasze API: GET /api/shop/prices → 200 mimo dwóch 503 u dostawcy; po wyczerpaniu prób → 503")
    void section6ShopEndpoint() throws Exception {
        server.expect(times(2), requestTo(PRICE_URL)).andRespond(withServiceUnavailable());
        server.expect(requestTo(PRICE_URL)).andRespond(withSuccess(PRICE_JSON, MediaType.APPLICATION_JSON));
        server.expect(times(4), requestTo("http://dostawca.test/supplier/api/prices/SPO-001"))
                .andRespond(withServiceUnavailable());

        mockMvc.perform(get("/api/shop/prices/ELE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(5499.99));
        mockMvc.perform(get("/api/shop/prices/SPO-001"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Dostawca nie odpowiada mimo ponowień — spróbuj później"));
        server.verify();
    }
}

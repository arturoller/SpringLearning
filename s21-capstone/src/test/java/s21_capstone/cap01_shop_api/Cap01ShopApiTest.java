package s21_capstone.cap01_shop_api;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import s21_capstone.cap01_shop_api.order.OrderRepository;
import s21_capstone.cap01_shop_api.product.ProductRepository;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy PEŁNEGO KONTEKSTU projektu Cap01ShopApi: {@code @SpringBootTest} + MockMvc — prawdziwe kontrolery, serwisy,
 * transakcje, Hibernate i baza H2 z migracjami. To one sprawdzają, że warstwy WSPÓŁPRACUJĄ (wycinki tego nie widzą).
 *
 * <p>Testy NIE są {@code @Transactional}: każde zapytanie MockMvc ma własną transakcję, jak w prawdziwej aplikacji
 * (z transakcją testu leniwe relacje „działałyby” mimo open-in-view=false i test ukryłby błąd). Dlatego każdy test
 * tworzy własne dane (nowy sku, nowe zamówienie) i nie zakłada, ile wierszy jest w tabelach.</p>
 *
 * <p>Zegar: {@code @TestConfiguration} z zatrzymanym {@code Clock} ({@code @Primary} — wygrywa z zegarem systemowym
 * z Cap01Config), więc {@code createdAt} nowych zamówień jest przewidywalne.</p>
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("cap01")
class Cap01ShopApiTest {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 10, 0);

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW.atZone(Cap01Config.SHOP_ZONE).toInstant(), Cap01Config.SHOP_ZONE);
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    OrderRepository orders;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    /** Szpieg (spy): prawdziwe repozytorium, w którym test może podmienić odpowiedź jednej metody. */
    @MockitoSpyBean
    ProductRepository products;

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    String body(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("1. Scenariusz: nowy klient → zamówienie (201, cena i suma) → OPLACONE → WYSLANE → DOSTARCZONE → ANULOWANE (409)")
    void fullOrderLifecycle() throws Exception {
        String customer = body(json(post("/api/customers"), """
                {"name":"Piotr Zieliński","city":"Łódź","email":"piotr.cap01@example.com"}"""));
        Integer customerId = JsonPath.read(customer, "$.id");

        String created = mockMvc.perform(json(post("/api/orders"), """
                        {"customerId":%d,"lines":[{"sku":"KSI-003","quantity":2},{"sku":"SPO-002","quantity":3}]}"""
                        .formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NOWE"))
                .andExpect(jsonPath("$.total").value(220.47))                 // 2 × 99.00 + 3 × 7.49
                .andExpect(jsonPath("$.createdAt").value("2026-10-19T10:00:00"))
                .andExpect(jsonPath("$.allowedStatusChanges[0]").value("OPLACONE"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Integer orderId = JsonPath.read(created, "$.id");
        assertThat((String) JsonPath.read(created, "$.number")).isEqualTo("ZAM-%03d".formatted(orderId));

        for (String next : new String[]{"OPLACONE", "WYSLANE", "DOSTARCZONE"}) {
            mockMvc.perform(json(patch("/api/orders/" + orderId + "/status"), "{\"status\":\"" + next + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(next));
        }
        mockMvc.perform(json(patch("/api/orders/" + orderId + "/status"), """
                        {"status":"ANULOWANE"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStatus").value("DOSTARCZONE"))
                .andExpect(jsonPath("$.allowed").isEmpty());

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(jsonPath("$.status").value("DOSTARCZONE"))
                .andExpect(jsonPath("$.customer.name").value("Piotr Zieliński"))
                .andExpect(jsonPath("$.lines[1].lineTotal").value(22.47));
    }

    @Test
    @DisplayName("2. Jedna transakcja: nieznany sku w drugiej pozycji → 404 i ŻADNE zamówienie nie powstaje")
    void orderIsAllOrNothing() throws Exception {
        long before = orders.count();

        mockMvc.perform(json(post("/api/orders"), """
                        {"customerId":1,"lines":[{"sku":"ELE-001","quantity":1},{"sku":"XXX-000","quantity":1}]}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Nie znaleziono: produkt XXX-000"));

        assertThat(orders.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("2. Reguła w serwisie + ograniczenie w bazie: gdy sprawdzenie „przegra wyścig”, UNIQUE w bazie daje ten sam 409")
    void databaseConstraintIsLastLineOfDefence() throws Exception {
        // Udajemy wyścig: existsBySku odpowiada „nie ma”, choć KSI-002 jest w bazie (tak jakby drugi klient zapisał go
        // między naszym sprawdzeniem a INSERT-em). Ratuje nas uk_products_sku + saveAndFlush w serwisie.
        doReturn(false).when(products).existsBySku("KSI-002");

        mockMvc.perform(json(post("/api/products"), """
                        {"sku":"KSI-002","name":"Java. Podstawy (kopia)","category":"KSIAZKI","price":1.00,"stock":1}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKU_TAKEN"));
        mockMvc.perform(get("/api/products/KSI-002"))
                .andExpect(jsonPath("$.name").value("Java. Podstawy"));   // stary produkt nietknięty
    }

    @Test
    @DisplayName("3. Klucz obcy chroni historię: produkt z zamówień → 409; nowy, nieużyty → 204, potem 404")
    void deleteRespectsOrders() throws Exception {
        mockMvc.perform(delete("/api/products/ELE-003"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_IN_USE"));

        mockMvc.perform(json(post("/api/products"), """
                        {"sku":"DOM-077","name":"Czajnik","category":"DOM","price":149.00,"stock":5}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/products/DOM-077"));
        mockMvc.perform(delete("/api/products/DOM-077")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products/DOM-077")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("6. Lista zamówień przez HTTP: pełna strona = 2 zapytania SQL (dane + count), ile by zamówień nie było")
    void orderListQueryCount() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/orders").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].customerName").exists());

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("5. Ten sam kształt błędu w całym API: 405 z klasy bazowej porady to też application/problem+json")
    void everyErrorIsProblemDetail() throws Exception {
        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Method 'DELETE' is not supported."));
    }
}

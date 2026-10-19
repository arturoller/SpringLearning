package s09_jpa_relations.rel06_entity_to_dto;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rel06EntityToDto — „WYNIK” każdej sekcji.
 *
 * <p>{@code @SpringBootTest} + {@code @AutoConfigureMockMvc}: cały kontekst (kontrolery, serwis, JPA, Jackson)
 * i zapytania HTTP przez MockMvc, bez prawdziwego serwera. Profil rel06 ma spring.jpa.open-in-view=false —
 * dokładnie jak zalecana konfiguracja produkcyjna. Testy NIE są transakcyjne (jak prawdziwe żądania HTTP).
 * Id klientów bierzemy z bazy po nazwie, a nie „na sztywno”.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("rel06")
class Rel06EntityToDtoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CustomerRepository customers;

    @Autowired
    ShopService shopService;

    @Autowired
    EntityManagerFactory emf;

    @Test
    @DisplayName("1. Encja w odpowiedzi: wyciekają techniczne id i cena zakupu (purchasePrice)")
    void entityLeaksInternalFields() throws Exception {
        mvc.perform(get("/api/rel06/entities/products/ELE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.purchasePrice").value(4100.00));
    }

    @Test
    @DisplayName("1. DTO produktu: tylko sku, name, price — bez id i ceny zakupu")
    void productDtoShowsOnlyChosenFields() throws Exception {
        mvc.perform(get("/api/rel06/products/ELE-001"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99}""", true))
                .andExpect(jsonPath("$.purchasePrice").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    @DisplayName("2. Encja z leniwą kolekcją (open-in-view=false): 500 — LazyInitializationException w czasie pisania JSON")
    void lazyRelationFailsDuringSerialization() throws Exception {
        Long janId = idOf("Jan Kowalski");

        MvcResult result = mvc.perform(get("/api/rel06/entities/customers/" + janId))
                .andExpect(status().isInternalServerError())
                .andReturn();

        assertThat(result.getResolvedException())
                .isInstanceOf(HttpMessageNotWritableException.class)
                .hasMessageStartingWith("Could not write JSON: Cannot lazily initialize collection of role "
                        + "'s09_jpa_relations.rel06_entity_to_dto.Customer.orders'")
                .hasRootCauseInstanceOf(LazyInitializationException.class);
    }

    @Test
    @DisplayName("4. DTO klienta: dane i zamówienia (posortowane), bez notatki wewnętrznej")
    void customerDtoHasOnlyChosenFields() throws Exception {
        Long janId = idOf("Jan Kowalski");

        mvc.perform(get("/api/rel06/customers/" + janId))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"name":"Jan Kowalski","city":"Warszawa","email":"jan@example.com",
                         "orders":[{"number":"ZAM-001","status":"NOWE"},{"number":"ZAM-003","status":"WYSLANE"}]}""",
                        true))
                .andExpect(jsonPath("$.internalNote").doesNotExist());
    }

    @Test
    @DisplayName("5. Mapowanie w serwisie (w transakcji, left join fetch): 1 zapytanie, DTO kompletne poza transakcją")
    void mappingInServiceCostsOneQuery() {
        Long mariaId = idOf("Maria Nowak");
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        CustomerDto dto = shopService.customer(mariaId);                 // transakcja już zamknięta

        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(dto).isEqualTo(new CustomerDto("Maria Nowak", "Kraków", null,
                java.util.List.of(new CustomerDto.OrderDto("ZAM-002", "OPLACONE"))));
    }

    private Long idOf(String name) {
        return customers.findByName(name).orElseThrow().getId();
    }
}

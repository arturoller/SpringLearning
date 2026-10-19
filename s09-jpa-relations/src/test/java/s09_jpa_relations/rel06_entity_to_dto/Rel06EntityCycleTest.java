package s09_jpa_relations.rel06_entity_to_dto;

import jakarta.persistence.EntityManagerFactory;
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

/**
 * Test sekcji 3 lekcji Rel06EntityToDto z WŁĄCZONYM open-in-view ({@code spring.jpa.open-in-view=true}) — osobna
 * klasa, bo inna wartość właściwości to inny kontekst Springa. Ten sam EntityController co w Rel06EntityToDtoTest.
 */
@SpringBootTest(properties = "spring.jpa.open-in-view=true")
@AutoConfigureMockMvc
@ActiveProfiles("rel06")
class Rel06EntityCycleTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CustomerRepository customers;

    @Autowired
    EntityManagerFactory emf;

    @Test
    @DisplayName("3. open-in-view=true + relacja dwukierunkowa: cykl w JSON, wyjątek w logu, a klient dostaje 200")
    void bidirectionalEntityCreatesCycle() throws Exception {
        Long janId = customers.findByName("Jan Kowalski").orElseThrow().getId();
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        MvcResult result = mvc.perform(get("/api/rel06/entities/customers/" + janId)).andReturn();

        assertThat(result.getResponse().getStatus()).as("odpowiedź już wysłana — status 200").isEqualTo(200);
        assertThat(result.getResolvedException())
                .isInstanceOf(HttpMessageNotWritableException.class)
                .hasMessageContaining("Document nesting depth (501) exceeds the maximum allowed (500");
        String body = result.getResponse().getContentAsString();
        assertThat(body).startsWith("{\"city\":\"Warszawa\"")
                .contains("\"orders\":[{\"customer\":{\"city\":\"Warszawa\"")
                .contains("\"internalNote\"");
        assertThat(body.length()).as("dziesiątki KB powtórzeń").isGreaterThan(20_000);
        assertThat(stats.getPrepareStatementCount()).as("klient w serwisie + zamówienia W CZASIE pisania JSON")
                .isEqualTo(2);
    }
}

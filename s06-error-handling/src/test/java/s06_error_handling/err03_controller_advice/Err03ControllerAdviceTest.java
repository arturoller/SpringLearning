package s06_error_handling.err03_controller_advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.ControllerAdviceBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Err03ControllerAdvice. {@code @SpringBootTest} + MockMvc: potrzebujemy całego kontekstu, bo sprawdzamy, jak
 * współpracuje kilka porad (beanów) i kontrolery z różnych pakietów. Porady budują odpowiedź same, więc MockMvc je widzi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)   // przechwytuje to, co aplikacja wypisała w logu
class Err03ControllerAdviceTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("1. Jedna porada globalna obsługuje wyjątek z kontrolera, który sam niczego nie obsługuje (404 + nasze ciało)")
    void globalAdviceHandlesControllerException() throws Exception {
        mockMvc.perform(get("/api/products/XXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUKT_NIE_ISTNIEJE"))
                .andExpect(jsonPath("$.message").value("Nie ma produktu o SKU XXX"));
    }

    @Test
    @DisplayName("2. Walidacja: błędy pól zamieniają się w mapę pole → komunikat (także pola zagnieżdżone)")
    void validationErrorsBecomeFieldMap() throws Exception {
        String json = """
                {"customer":" ","lines":[{"sku":"","quantity":0},{"sku":"ELE-001","quantity":2}]}""";

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BLAD_WALIDACJI"))
                .andExpect(jsonPath("$.fields.customer").value("Podaj imię i nazwisko klienta"))
                .andExpect(jsonPath("$.fields['lines[0].quantity']").value("Ilość musi być dodatnia"))
                .andExpect(jsonPath("$.fields['lines[0].sku']").value("SKU nie może być puste"))
                .andExpect(jsonPath("$.fields.length()").value(3));   // pozycja nr 1 (indeks 1) jest poprawna — brak błędu
    }

    @Test
    @DisplayName("3. Niepoprawny JSON w ciele → 400 z kodem NIEPOPRAWNY_JSON, bez szczegółów parsera")
    void unreadableBody() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{nie json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NIEPOPRAWNY_JSON"));
    }

    @Test
    @DisplayName("4. Błąd wewnętrzny: klient dostaje ogólny komunikat, a szczegóły trafiają do logu dokładnie RAZ")
    void internalErrorLoggedOnceAndHiddenFromClient(CapturedOutput output) throws Exception {
        String body = mockMvc.perform(get("/api/products/ELE-001/stock"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("BLAD_WEWNETRZNY"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("jdbc:h2").doesNotContain("Baza nie odpowiada");   // nic z wnętrza aplikacji
        assertThat(output.getAll()).contains("jdbc:h2:mem:sklep");           // ... ale w logu jest cały komunikat i stos
        assertThat(output.getAll().split("\\[BLAD-WEWNETRZNY\\]", -1)).hasSize(2);   // dokładnie jedno wystąpienie
        assertThat(output.getAll()).doesNotContain("Servlet.service()");     // Tomcat nie loguje drugi raz, bo błąd jest obsłużony
    }

    @Test
    @DisplayName("5. Porada zawężona do klasy (assignableTypes): ten sam wyjątek w BillingController → 502, nie 500")
    void narrowAdviceByType() throws Exception {
        mockMvc.perform(post("/api/billing/charge"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("BRAMKA_PLATNOSCI"));
    }

    @Test
    @DisplayName("6. Porada zawężona do pakietu (basePackages): AdminController → 503")
    void narrowAdviceByPackage() throws Exception {
        mockMvc.perform(get("/api/admin/report"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("RAPORT_NIEDOSTEPNY"));
    }

    @Test
    @DisplayName("7. Kolejność porad wynika z @Order: Billing (najwyższy priorytet), Admin (1), Global (najniższy)")
    void adviceOrder() {
        List<String> order = ControllerAdviceBean.findAnnotatedBeans(context).stream()
                .map(bean -> bean.getBeanType().getSimpleName())
                .filter(name -> name.endsWith("Advice") && !name.equals("ResponseBodyAdvice"))
                .toList();

        assertThat(order).containsSubsequence("BillingAdvice", "AdminAdvice", "GlobalErrorAdvice");
    }

    @Test
    @DisplayName("8. Wyjątki Springa bez handlera w poradzie (404, 405) nadal idą ścieżką domyślną z lekcji Err01")
    void springExceptionsStillDefault() throws Exception {
        mockMvc.perform(get("/api/nie-ma-takiego")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/products/ELE-001")).andExpect(status().isMethodNotAllowed());
    }
}

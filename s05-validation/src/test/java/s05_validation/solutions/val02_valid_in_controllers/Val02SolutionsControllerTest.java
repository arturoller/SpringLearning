package s05_validation.solutions.val02_valid_in_controllers;

import s05_validation.val02_valid_in_controllers.Customers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy rozwiązań wzorcowych lekcji Val02ValidInControllers. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
@WebMvcTest(Val02SolutionsController.class)
@Import(Customers.class)
class Val02SolutionsControllerTest {

    static final String GOOD = "{\"name\":\"Ewa\",\"email\":\"ewa@example.com\"}";
    static final String BAD = "{\"name\":\"\",\"email\":\"nie-email\"}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    Customers customers;

    @BeforeEach
    void resetCustomers() {
        customers.reset();   // każdy test startuje od Anny (1) i Jana (2) — niezależnie od kolejności testów
    }

    @Test
    @DisplayName("Wzorzec 1: POST → 201 dla poprawnych danych, 400 (i brak nowego klienta) dla błędnych")
    void exercise1() throws Exception {
        mockMvc.perform(post("/val02/cw/customers").contentType(MediaType.APPLICATION_JSON).content(GOOD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.email").value("ewa@example.com"));
        mockMvc.perform(post("/val02/cw/customers").contentType(MediaType.APPLICATION_JSON).content(BAD))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/val02/cw/customers").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        assertThat(customers.size()).isEqualTo(3);   // dodany tylko Ewa
    }

    @Test
    @DisplayName("Wzorzec 2: GET → 200, 404, a id mniejsze niż 1 → 400")
    void exercise2() throws Exception {
        mockMvc.perform(get("/val02/cw/customers/1")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Anna"));
        mockMvc.perform(get("/val02/cw/customers/99")).andExpect(status().isNotFound());
        mockMvc.perform(get("/val02/cw/customers/0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/val02/cw/customers/-5")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Wzorzec 3: /manual → 400 z posortowaną listą \"pole:Kod\" albo 201")
    void exercise3() throws Exception {
        mockMvc.perform(post("/val02/cw/customers/manual").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \",\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("email:NotBlank"))
                .andExpect(jsonPath("$[1]").value("name:NotBlank"));
        mockMvc.perform(post("/val02/cw/customers/manual").contentType(MediaType.APPLICATION_JSON).content(GOOD))
                .andExpect(status().isCreated());
        assertThat(customers.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Wzorzec 4: messagesByField — mapa pole → komunikat, posortowana, komunikaty jednego pola połączone")
    void exercise4() {
        BindingResult errors = new BeanPropertyBindingResult(new Val02SolutionsController.CustomerRequest("", ""), "customerRequest");
        errors.rejectValue("name", "NotBlank", "Nazwa jest wymagana");
        errors.rejectValue("email", "Email", "Zły e-mail");
        errors.rejectValue("email", "NotBlank", "E-mail jest wymagany");

        Map<String, String> result = Val02SolutionsController.messagesByField(errors);

        assertThat(List.copyOf(result.keySet())).containsExactly("email", "name");
        assertThat(result.get("email")).isEqualTo("E-mail jest wymagany; Zły e-mail");
        assertThat(result.get("name")).isEqualTo("Nazwa jest wymagana");
    }

    @Test
    @DisplayName("Wzorzec 5: PUT → 200, 404; zły id albo złe ciało → 400 i klient nie zmieniony")
    void exercise5() throws Exception {
        mockMvc.perform(put("/val02/cw/customers/1").contentType(MediaType.APPLICATION_JSON).content(GOOD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ewa"));
        mockMvc.perform(put("/val02/cw/customers/99").contentType(MediaType.APPLICATION_JSON).content(GOOD))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/val02/cw/customers/0").contentType(MediaType.APPLICATION_JSON).content(GOOD))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/val02/cw/customers/2").contentType(MediaType.APPLICATION_JSON).content(BAD))
                .andExpect(status().isBadRequest());
        assertThat(customers.find(2).orElseThrow().name()).isEqualTo("Jan");
    }
}

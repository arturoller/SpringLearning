package s04_web_rest.rest05_crud_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import s04_web_rest.rest05_crud_api.CustomerBook;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy ĆWICZEŃ lekcji Rest05CrudApi. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@WebMvcTest(ExerciseController.class)
@Import(CustomerBook.class)
class Rest05CrudApiExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CustomerBook book;

    @BeforeEach
    void resetBook() {
        book.reset();   // każdy test startuje od Anny (1) i Jana (2) — niezależnie od kolejności testów
    }

    @Test
    @DisplayName("Ćwiczenie 1: GET → 200 z klientem albo 404 bez ciała")
    void exercise1() throws Exception {
        mockMvc.perform(get("/rest05/cw/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Anna"));
        mockMvc.perform(get("/rest05/cw/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Ćwiczenie 2: DELETE → 204, a drugie usunięcie tego samego → 404")
    void exercise2() throws Exception {
        mockMvc.perform(delete("/rest05/cw/customers/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/rest05/cw/customers/1")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Ćwiczenie 3: POST → 201, Location z nowym id i klient w ciele")
    void exercise3() throws Exception {
        mockMvc.perform(post("/rest05/cw/customers").contentType("application/json")
                        .content("{\"name\":\"Ola\",\"email\":\"ola@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/rest05/cw/customers/3"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Ola"));
    }

    @Test
    @DisplayName("Ćwiczenie 4: PUT zmienia nazwę (e-mail zostaje) → 200; brak klienta → 404")
    void exercise4() throws Exception {
        mockMvc.perform(put("/rest05/cw/customers/1").contentType("application/json")
                        .content("{\"name\":\"Anna Maria\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Anna Maria"))
                .andExpect(jsonPath("$.email").value("anna@example.com"));
        mockMvc.perform(put("/rest05/cw/customers/99").contentType("application/json")
                        .content("{\"name\":\"Nikt\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Ćwiczenie 5: PATCH zmienia e-mail → 200; zły e-mail → 400 i bez zmiany; brak klienta → 404")
    void exercise5() throws Exception {
        mockMvc.perform(patch("/rest05/cw/customers/1").contentType("application/json")
                        .content("{\"email\":\"nowy@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nowy@example.com"))
                .andExpect(jsonPath("$.name").value("Anna"));

        mockMvc.perform(patch("/rest05/cw/customers/2").contentType("application/json")
                        .content("{\"email\":\"bez-malpy\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/rest05/cw/customers/2"))
                .andExpect(jsonPath("$.email").value("jan@example.com"));

        mockMvc.perform(patch("/rest05/cw/customers/99").contentType("application/json")
                        .content("{\"email\":\"a@b.pl\"}"))
                .andExpect(status().isNotFound());
    }
}

package s04_web_rest.rest01_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy ĆWICZEŃ lekcji Rest01Controllers. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@WebMvcTest(ExerciseController.class)
class Rest01ControllersExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: GET /rest01/cw/greeting zwraca tekst")
    void exercise1() throws Exception {
        mockMvc.perform(get("/rest01/cw/greeting"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andExpect(content().string("Witaj w sklepie!"));
    }

    @Test
    @DisplayName("Ćwiczenie 2: GET /rest01/cw/shop zwraca rekord jako JSON")
    void exercise2() throws Exception {
        mockMvc.perform(get("/rest01/cw/shop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sklep DP21"))
                .andExpect(jsonPath("$.openDays").value(6));
    }

    @Test
    @DisplayName("Ćwiczenie 3: ta sama ścieżka /cart — GET i DELETE to dwie metody")
    void exercise3() throws Exception {
        mockMvc.perform(get("/rest01/cw/cart")).andExpect(content().string("koszyk"));
        mockMvc.perform(delete("/rest01/cw/cart"))
                .andExpect(status().isOk())
                .andExpect(content().string("koszyk opróżniony"));
    }

    @Test
    @DisplayName("Ćwiczenie 4: raport tylko dla Accept: text/csv, inaczej 406")
    void exercise4() throws Exception {
        mockMvc.perform(get("/rest01/cw/report").accept("text/csv"))
                .andExpect(status().isOk())
                .andExpect(content().string("id;name"));
        mockMvc.perform(get("/rest01/cw/report").accept("application/json"))
                .andExpect(status().isNotAcceptable());
    }
}

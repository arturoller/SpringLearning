package s05_validation.val02_valid_in_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy ĆWICZEŃ lekcji Val02ValidInControllers. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Kontekst Springa nie jest potrzebny: {@code standaloneSetup} buduje MockMvc wokół jednego kontrolera (szybko).
 */
@Tag("cwiczenie")
class Val02ValidInControllersExercisesTest {

    private final MockMvc customers = MockMvcBuilders.standaloneSetup(new ExerciseCustomerController()).build();

    @Test
    @DisplayName("Ćwiczenie 1: zły formularz klienta → 400, dobry → 201")
    void exercise1() throws Exception {
        customers.perform(post("/api/exercise/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"email\":\"abc\"}"))
                .andExpect(status().isBadRequest());
        customers.perform(post("/api/exercise/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jan Kowalski\",\"email\":\"jan@example.com\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Ćwiczenie 2: id mniejsze od 1 → 400, poprawne → 200")
    void exercise2() throws Exception {
        customers.perform(get("/api/exercise/customers/0")).andExpect(status().isBadRequest());
        customers.perform(get("/api/exercise/customers/7")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ćwiczenie 3: wyjątek walidacji ciała → mapa pole → komunikat, posortowana")
    void exercise3() throws Exception {
        MockMvc products = MockMvcBuilders.standaloneSetup(new ProductController()).build();
        var result = products.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"sku\":\"x\",\"price\":-1}"))
                .andReturn();

        MethodArgumentNotValidException ex = (MethodArgumentNotValidException) result.getResolvedException();
        Map<String, String> errors = Val02ValidInControllers.fieldErrors(ex);

        assertThat(errors).containsExactly(
                Map.entry("name", "Nazwa jest wymagana"),
                Map.entry("price", "Cena musi być dodatnia"),
                Map.entry("sku", "SKU ma format np. ELE-001"));
    }
}

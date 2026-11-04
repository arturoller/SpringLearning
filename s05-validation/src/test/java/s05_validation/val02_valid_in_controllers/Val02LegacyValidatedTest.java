package s05_validation.val02_valid_in_controllers;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sekcja 4 lekcji Val02ValidInControllers: kontroler ze starym {@code @Validated} na klasie. */
@WebMvcTest(LegacyValidatedController.class)
class Val02LegacyValidatedTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Sekcja 4: z @Validated na klasie błąd parametru to ConstraintViolationException (nieobsłużony → 500), nie 400")
    void classLevelValidatedThrowsConstraintViolation() throws Exception {
        assertThatThrownBy(() -> mockMvc.perform(get("/val02/legacy/products/0")))
                .hasRootCauseInstanceOf(ConstraintViolationException.class);
        mockMvc.perform(get("/val02/legacy/products/1")).andExpect(status().isOk());
    }
}

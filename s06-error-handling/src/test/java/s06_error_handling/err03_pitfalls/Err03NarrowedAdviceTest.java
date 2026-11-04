package s06_error_handling.err03_pitfalls;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import s06_error_handling.err03_controller_advice.AdminAdvice;
import s06_error_handling.err03_controller_advice.AdminController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 3 lekcji Err03ControllerAdvice: advice zawężony selektorem ({@code assignableTypes}) działa dopiero, gdy Spring
 * zna kontroler, który rzucił wyjątek. Przy błędach "przed kontrolerem" (zła metoda HTTP → 405) nie obowiązuje.
 */
@WebMvcTest
@ContextConfiguration(classes = {AdminController.class, AdminAdvice.class})
class Err03NarrowedAdviceTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Sekcja 3: zawężony advice obsługuje błąd z kontrolera, ale nie dotyka 405, które powstaje przed wyborem kontrolera")
    void narrowedAdviceSkipsErrorsBeforeTheController() throws Exception {
        mockMvc.perform(get("/err03/admin/discount")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ADMIN_BAD_INPUT"));
        mockMvc.perform(delete("/err03/admin/discount")).andExpect(status().isMethodNotAllowed()).andExpect(content().string(""));
    }
}

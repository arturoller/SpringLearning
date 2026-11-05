package s06_error_handling.err02_exception_handler;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Sekcja 5 lekcji Err02ExceptionHandler: handler z {@link ShopController} nie obsługuje innego kontrolera. */
@WebMvcTest(OtherController.class)
class Err02OtherControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Sekcja 5: ten sam wyjątek w kontrolerze bez handlera NIE jest obsłużony (w MockMvc wylatuje jako ServletException)")
    void handlerDoesNotReachOtherControllers() {
        assertThatThrownBy(() -> mockMvc.perform(get("/err02/other/products/9")))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(ProductNotFoundException.class)
                .hasRootCauseMessage("Nie ma produktu o id 9");
    }
}

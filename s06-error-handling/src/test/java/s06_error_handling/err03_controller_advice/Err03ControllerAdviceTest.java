package s06_error_handling.err03_controller_advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import s06_error_handling.err03_controller_advice.legacy.LegacyController;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcje 1-5 lekcji Err03ControllerAdvice. {@code @WebMvcTest(controllers = …)} ładuje wskazane kontrolery ORAZ wszystkie
 * beany {@code @ControllerAdvice} — dlatego własne advice działają w teście bez dodatkowej konfiguracji.
 */
@WebMvcTest(controllers = {ProductController.class, OrderController.class, LocalOverrideController.class, AdminController.class,
        ReportsController.class, LegacyController.class})
class Err03ControllerAdviceTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Sekcja 1: jeden advice obsługuje błędy z DWÓCH kontrolerów, które same nie mają żadnego handlera")
    void oneAdviceForSeveralControllers() throws Exception {
        mockMvc.perform(get("/err03/products/9"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))   // @RestControllerAdvice = @ResponseBody
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Nie ma produktu o id 9"));
        mockMvc.perform(get("/err03/orders/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        mockMvc.perform(post("/err03/orders/1/pay")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
        mockMvc.perform(get("/err03/products/1/stock")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
    }

    @Test
    @DisplayName("Sekcja 1: błąd walidacji w advice — jeden format ApiError z posortowaną mapą fields")
    void validationInAdvice() throws Exception {
        mockMvc.perform(post("/err03/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.name").value("Nazwa jest wymagana"))
                .andExpect(jsonPath("$.fields.quantity").value("Ilość musi wynosić co najmniej 1"));
    }

    @Test
    @DisplayName("Sekcja 2: handler LOKALNY w kontrolerze wygrywa z globalnym dla tego samego wyjątku")
    void localHandlerBeatsAdvice() throws Exception {
        mockMvc.perform(get("/err03/local/products/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOCAL_NOT_FOUND"));
        mockMvc.perform(get("/err03/products/9")).andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));   // inny kontroler: globalny
    }

    @Test
    @DisplayName("Sekcja 2: dwa advice dla tego samego wyjątku — wygrywa @Order(1), mimo że drugi jest pierwszy alfabetycznie")
    void orderDecidesBetweenAdvices() throws Exception {
        mockMvc.perform(get("/err03/products/priority"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PRIMARY"));
    }

    @Test
    @DisplayName("Sekcja 3: ten sam wyjątek IllegalArgumentException, trzy zawężone advice — trzy różne odpowiedzi")
    void narrowedAdvices() throws Exception {
        mockMvc.perform(get("/err03/admin/discount")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_BAD_INPUT")).andExpect(jsonPath("$.message").value("Rabat musi być w przedziale 0-100"));
        mockMvc.perform(get("/err03/reports/sales")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REPORT_BAD_INPUT"));
        mockMvc.perform(get("/err03/legacy/export")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LEGACY_BAD_INPUT"));
    }

    @Test
    @DisplayName("Sekcja 4: handler Exception.class dostaje resztę — 500, ogólny komunikat, bez tajnych szczegółów")
    void fallbackHidesDetails() throws Exception {
        mockMvc.perform(get("/err03/products/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("UNEXPECTED"))
                .andExpect(jsonPath("$.message").value("Wystąpił nieoczekiwany błąd"))
                .andExpect(content().string(not(containsString("hasło"))));
    }

    @Test
    @DisplayName("Sekcja 4: poprawiony fallback przepuszcza wyjątki Springa z ich własnym statusem (405, 415, 404, 400)")
    void fallbackKeepsSpringsOwnStatuses() throws Exception {
        mockMvc.perform(delete("/err03/products/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.code").value("HTTP_405"));
        mockMvc.perform(post("/err03/products").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("HTTP_415"));
        mockMvc.perform(get("/err03/nie-ma-takiej-sciezki"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("HTTP_404"));
        mockMvc.perform(post("/err03/products").contentType(MediaType.APPLICATION_JSON).content("{zepsuty"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mockMvc.perform(get("/err03/products/abc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("Sekcja 5: każdy błąd ma ten sam szkielet — status, code, message, path (klient pisze jedną obsługę)")
    void oneFormatEverywhere() throws Exception {
        List<String> uris = List.of("/err03/products/9", "/err03/orders/7", "/err03/products/1/stock", "/err03/products/crash",
                "/err03/admin/discount", "/err03/reports/sales", "/err03/nie-ma-takiej-sciezki", "/err03/products/abc");
        for (String uri : uris) {
            ResultActions result = mockMvc.perform(get(uri));
            result.andExpect(jsonPath("$.status").isNumber())
                    .andExpect(jsonPath("$.code").isString())
                    .andExpect(jsonPath("$.message").isString())
                    .andExpect(jsonPath("$.path").value(uri));
        }
    }
}

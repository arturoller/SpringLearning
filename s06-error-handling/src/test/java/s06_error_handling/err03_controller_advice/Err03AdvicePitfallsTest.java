package s06_error_handling.err03_controller_advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PUŁAPKI porad, pokazane na małych, własnych kontrolerach i poradach (zwykły test jednostkowy z
 * {@code standaloneSetup} — MockMvc zbudowany ręcznie, bez Springa Boota). Klasy zagnieżdżone nie są beanami aplikacji.
 */
class Err03AdvicePitfallsTest {

    @RestController
    static class GetOnlyController {
        @GetMapping("/ping")
        String ping() {
            return "pong";
        }

        @GetMapping("/bad")
        String bad() {
            throw new IllegalArgumentException("zły argument");
        }
    }

    /** Naiwna porada „łapiąca wszystko”. */
    @RestControllerAdvice
    static class NaiveCatchAllAdvice {
        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        ApiError all(Exception e) {
            return new ApiError("WSZYSTKO", "cokolwiek");
        }
    }

    @RestControllerAdvice
    @Order(1)
    static class BroadAdvice {
        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        ApiError broad(Exception e) {
            return new ApiError("SZEROKA", "łapie wszystko");
        }
    }

    @RestControllerAdvice
    @Order(2)
    static class SpecificAdvice {
        @ExceptionHandler(IllegalArgumentException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        ApiError specific(IllegalArgumentException e) {
            return new ApiError("SZCZEGOLOWA", "tylko IllegalArgumentException");
        }
    }

    @Test
    @DisplayName("1. PUŁAPKA: porada z handlerem na Exception zamienia 405 Method Not Allowed w 500")
    void catchAllSwallowsSpringExceptions() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GetOnlyController())
                .setControllerAdvice(new NaiveCatchAllAdvice()).build();

        mockMvc.perform(get("/ping")).andExpect(status().isOk());
        mockMvc.perform(post("/ping"))   // zła metoda HTTP: normalnie 405 (Allow: GET)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("WSZYSTKO"));
    }

    @Test
    @DisplayName("2. Bez porady z Exception ten sam błąd to poprawne 405")
    void withoutCatchAllItIs405() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GetOnlyController()).build();

        mockMvc.perform(post("/ping")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("3. PUŁAPKA: porada o wyższym priorytecie z szerokim handlerem wygrywa z bardziej szczegółowym handlerem z dalszej porady")
    void precedenceBeatsSpecificity() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GetOnlyController())
                .setControllerAdvice(new SpecificAdvice(), new BroadAdvice()).build();   // kolejność w wywołaniu nie ma znaczenia

        mockMvc.perform(get("/bad"))
                .andExpect(status().isInternalServerError())   // a nie 400 z SpecificAdvice
                .andExpect(jsonPath("$.code").value("SZEROKA"));
    }

    /** Zwykłe @ControllerAdvice — bez @ResponseBody: wynik handlera nie trafia do ciała jako JSON. */
    @org.springframework.web.bind.annotation.ControllerAdvice
    static class PlainAdvice {
        @ExceptionHandler(IllegalArgumentException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        ApiError plain(IllegalArgumentException e) {
            return new ApiError("ZWYKLA", "bez @ResponseBody");
        }
    }

    @Test
    @DisplayName("5. PUŁAPKA: zwykłe @ControllerAdvice (bez Rest) nie zapisuje rekordu jako JSON — Spring szuka widoku")
    void plainControllerAdviceDoesNotWriteJson() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GetOnlyController())
                .setControllerAdvice(new PlainAdvice()).build();

        assertThatThrownBy(() -> mockMvc.perform(get("/bad")))
                .hasStackTraceContaining("Circular view path [bad]");   // rekord potraktowany jak atrybut modelu, a widok wzięty z adresu
    }

    @Test
    @DisplayName("4. Gdy szeroki handler stoi w porze o NIŻSZYM priorytecie, szczegółowy wygrywa")
    void specificFirstWhenOrdered() throws Exception {
        @RestControllerAdvice
        @Order(Ordered.LOWEST_PRECEDENCE)
        class LateBroadAdvice {
            @ExceptionHandler(Exception.class)
            @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
            ApiError broad(Exception e) {
                return new ApiError("SZEROKA", "łapie wszystko");
            }
        }
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GetOnlyController())
                .setControllerAdvice(new LateBroadAdvice(), new SpecificAdvice()).build();

        mockMvc.perform(get("/bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SZCZEGOLOWA"));
    }
}

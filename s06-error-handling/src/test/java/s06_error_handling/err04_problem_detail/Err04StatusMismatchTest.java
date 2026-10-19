package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PUŁAPKA: rozjazd między statusem HTTP a polem status w ciele ProblemDetail (ZNAJDŹ BŁĄD z pytań). Mały własny
 * kontroler i porada, zwykły test jednostkowy ze standaloneSetup.
 */
class Err04StatusMismatchTest {

    @RestController
    static class BoomController {
        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("bum");
        }
    }

    @RestControllerAdvice
    static class MismatchAdvice {
        @ExceptionHandler(IllegalStateException.class)
        ResponseEntity<ProblemDetail> handle(IllegalStateException e) {
            ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);   // 500 w ciele...
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);                      // ...a 409 w odpowiedzi
        }
    }

    @Test
    @DisplayName("1. ResponseEntity ze statusem 409 i ProblemDetail ze statusem 500: do klienta idzie 409, a w ciele zostaje 500")
    void statusLineAndBodyCanDisagree() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BoomController())
                .setControllerAdvice(new MismatchAdvice()).build();

        mockMvc.perform(get("/boom"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(500));
    }
}

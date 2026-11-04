package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcje 1, 2 i 4 lekcji Err04ProblemDetail. {@code @WebMvcTest(ShopController.class)} ładuje kontroler ORAZ wszystkie
 * {@code @ControllerAdvice} lekcji, więc działa {@code Err04Advice} (dziedziczący po ResponseEntityExceptionHandler).
 * Slice webowy wystarcza: sprawdzamy tylko status, nagłówki i JSON odpowiedzi.
 */
@WebMvcTest(ShopController.class)
class Err04ProblemDetailTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Sekcja 1: handler zwracający ProblemDetail → application/problem+json z polami type, title, status, detail, instance")
    void problemDetailFromHandler() throws Exception {
        mockMvc.perform(get("/err04/products/9"))
                .andExpect(status().isNotFound())                                        // status z ProblemDetail, bez @ResponseStatus
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://sklep.example/errors/product-not-found"))
                .andExpect(jsonPath("$.title").value("Produkt nie istnieje"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Nie ma produktu o id 9"))
                .andExpect(jsonPath("$.instance").value("/err04/products/9"))           // instance dopisał Spring
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))               // własna właściwość na tym samym poziomie
                .andExpect(jsonPath("$.*", hasSize(6)));                                 // dokładnie 6 pól, nic więcej
    }

    @Test
    @DisplayName("Sekcja 2: drugi wyjątek domenowy — 409 i kod biznesowy we właściwości code")
    void outOfStock() throws Exception {
        mockMvc.perform(get("/err04/products/1/stock"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Brak towaru"))
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
    }

    @Test
    @DisplayName("Sekcja 2: ResponseStatusException i ErrorResponseException same stają się ProblemDetail — bez naszego handlera")
    void errorResponsesBecomeProblemDetail() throws Exception {
        mockMvc.perform(get("/err04/products/1/reserve"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))                       // brak własnego tytułu = opis statusu
                .andExpect(jsonPath("$.detail").value("Produkt 1 jest już zarezerwowany"));
        mockMvc.perform(get("/err04/products/1/archive"))
                .andExpect(status().isGone())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://sklep.example/errors/product-archived"))
                .andExpect(jsonPath("$.code").value("PRODUCT_ARCHIVED"));
    }

    @Test
    @DisplayName("Sekcja 4: wyjątki Springa (405, 415, 400, 404) dostają ProblemDetail od ResponseEntityExceptionHandler")
    void springsOwnExceptionsAsProblemDetail() throws Exception {
        mockMvc.perform(delete("/err04/products/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))                              // nagłówek Allow zachowany
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Method Not Allowed"));
        mockMvc.perform(post("/err04/products").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(post("/err04/products").contentType(MediaType.APPLICATION_JSON).content("{zepsuty"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/err04/products/abc"))                                      // zły typ id: wyjątek, który NIE jest ErrorResponse
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/err04/nie-ma-takiej-sciezki"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("Sekcja 4: nadpisane handleMethodArgumentNotValid dodaje listę błędów pól do ProblemDetail")
    void validationErrorsList() throws Exception {
        mockMvc.perform(post("/err04/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Nazwa jest wymagana"))
                .andExpect(jsonPath("$.errors[1].field").value("quantity"))
                .andExpect(jsonPath("$.errors[1].message").value("Ilość musi wynosić co najmniej 1"));
    }

    @Test
    @DisplayName("Sekcja 4: handler Exception.class w klasie dziedziczącej NIE połyka wyjątków Springa i nie zdradza szczegółów")
    void catchAllKeepsSpringsStatuses() throws Exception {
        mockMvc.perform(get("/err04/products/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("UNEXPECTED"))
                .andExpect(jsonPath("$.detail").value("Wystąpił nieoczekiwany błąd"))
                .andExpect(content().string(not(containsString("hasło"))));
        mockMvc.perform(delete("/err04/products/1")).andExpect(status().isMethodNotAllowed());   // nadal 405, nie 500
    }
}

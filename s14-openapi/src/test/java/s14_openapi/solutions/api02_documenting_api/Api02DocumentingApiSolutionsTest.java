package s14_openapi.solutions.api02_documenting_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Api02DocumentingApiExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Api02DocumentingApiSolutionsTest {

    static final String DOC = """
            {"openapi":"3.1.0",
             "paths":{
               "/api/orders":{"post":{"responses":{"201":{"description":"Utworzono"}}}},
               "/api/orders/{id}":{"summary":"Zamówienie",
                  "get":{"parameters":[{"name":"id","in":"path"}],
                         "responses":{"200":{"description":"OK"},"404":{"description":"Nie ma"}}},
                  "delete":{"parameters":[{"name":"id","in":"path"}],
                            "responses":{"204":{"description":"Usunięto"}}},
                  "put":{"parameters":[{"name":"dry","in":"query"},{"name":"id","in":"path"}],
                         "responses":{"200":{"description":"OK"}}}},
               "/api/orders/search":{"get":{"parameters":[{"name":"q","in":"query"}],
                         "responses":{"200":{"description":"OK"}}}}},
             "components":{"schemas":{
               "Order":{"type":"object","required":["number","customer"],
                        "properties":{"number":{"type":"string","description":"Numer zamówienia"},
                                      "customer":{"type":"string"},
                                      "total":{"type":"number"}}},
               "Empty":{"type":"object","properties":{"a":{"type":"string","description":"x"}}}}}}
            """;

    @Test
    @DisplayName("Wzorzec 1: wymagane pola schematu (posortowane)")
    void solution1() {
        assertThat(Api02DocumentingApiSolutions.solution1(DOC, "Order")).containsExactly("customer", "number");
        assertThat(Api02DocumentingApiSolutions.solution1(DOC, "Empty")).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 2: pola schematu bez opisu")
    void solution2() {
        assertThat(Api02DocumentingApiSolutions.solution2(DOC, "Order")).containsExactly("customer", "total");
        assertThat(Api02DocumentingApiSolutions.solution2(DOC, "Empty")).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 3: operacje z parametrem ścieżki, ale bez odpowiedzi 404")
    void solution3() {
        assertThat(Api02DocumentingApiSolutions.solution3(DOC))
                .containsExactly("DELETE /api/orders/{id}", "PUT /api/orders/{id}");
    }
}

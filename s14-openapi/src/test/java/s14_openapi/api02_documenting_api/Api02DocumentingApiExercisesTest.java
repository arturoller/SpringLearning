package s14_openapi.api02_documenting_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Api02DocumentingApi. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Api02DocumentingApiExercisesTest {

    /** Mały dokument OpenAPI wklejony na sztywno (test nie potrzebuje aplikacji). */
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
    @DisplayName("Ćwiczenie 1: wymagane pola schematu (posortowane)")
    void exercise1() {
        assertThat(Api02DocumentingApi.exercise1(DOC, "Order")).containsExactly("customer", "number");
        assertThat(Api02DocumentingApi.exercise1(DOC, "Empty")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 2: pola schematu bez opisu")
    void exercise2() {
        assertThat(Api02DocumentingApi.exercise2(DOC, "Order")).containsExactly("customer", "total");
        assertThat(Api02DocumentingApi.exercise2(DOC, "Empty")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: operacje z parametrem ścieżki, ale bez odpowiedzi 404")
    void exercise3() {
        assertThat(Api02DocumentingApi.exercise3(DOC)).containsExactly("DELETE /api/orders/{id}", "PUT /api/orders/{id}");
    }
}

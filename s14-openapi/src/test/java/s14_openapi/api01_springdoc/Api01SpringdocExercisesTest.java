package s14_openapi.api01_springdoc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Api01Springdoc. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Api01SpringdocExercisesTest {

    /** Mały dokument OpenAPI w stylu tego z lekcji (wklejony na sztywno — test nie potrzebuje aplikacji). */
    static final String DOC = """
            {"openapi":"3.1.0","info":{"title":"Sklep","version":"v1"},
             "paths":{
               "/api/products":{"get":{"responses":{"200":{"description":"OK"}}},
                                "post":{"responses":{"200":{"description":"OK"}}}},
               "/api/orders/{id}":{"summary":"Zamówienie","parameters":[],
                                   "delete":{"responses":{"204":{"description":"No Content"}}},
                                   "get":{"responses":{"200":{"description":"OK"},"404":{"description":"Nie ma"}}}}}}
            """;

    @Test
    @DisplayName("Ćwiczenie 1: posortowana lista ścieżek")
    void exercise1() {
        assertThat(Api01Springdoc.exercise1(DOC)).containsExactly("/api/orders/{id}", "/api/products");
    }

    @Test
    @DisplayName("Ćwiczenie 2: operacje w postaci METODA ścieżka (bez pól niebędących metodami)")
    void exercise2() {
        assertThat(Api01Springdoc.exercise2(DOC)).containsExactly(
                "DELETE /api/orders/{id}", "GET /api/orders/{id}", "GET /api/products", "POST /api/products");
    }

    @Test
    @DisplayName("Ćwiczenie 3: brakujące kody statusu w opisie operacji")
    void exercise3() {
        assertThat(Api01Springdoc.exercise3(DOC, "/api/products", "post", Set.of("201", "400", "200")))
                .containsExactly("201", "400");
        assertThat(Api01Springdoc.exercise3(DOC, "/api/orders/{id}", "get", Set.of("200", "404"))).isEmpty();
        assertThatThrownBy(() -> Api01Springdoc.exercise3(DOC, "/api/products", "delete", Set.of("204")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("/api/products");
        assertThatThrownBy(() -> Api01Springdoc.exercise3(DOC, "/nie/ma", "get", Set.of("200")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("/nie/ma");
    }
}

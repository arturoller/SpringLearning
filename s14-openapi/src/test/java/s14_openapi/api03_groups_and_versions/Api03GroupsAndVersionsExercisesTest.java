package s14_openapi.api03_groups_and_versions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Api03GroupsAndVersions. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Api03GroupsAndVersionsExercisesTest {

    static final String CONFIG = """
            {"configUrl":"/v3/api-docs/swagger-config",
             "urls":[{"url":"/v3/api-docs/public","name":"Publiczne"},{"url":"/v3/api-docs/admin","name":"Administracja"}]}
            """;

    static final String DOC = """
            {"paths":{
              "/admin/stats":{"get":{"security":[{"bearerAuth":[]}],"responses":{"200":{"description":"OK"}}}},
              "/api/orders":{"summary":"Zamówienia",
                  "get":{"responses":{"200":{"description":"OK"}}},
                  "post":{"security":[{"bearerAuth":[]}],"responses":{"201":{"description":"OK"}}}},
              "/api/products/{sku}":{"get":{"parameters":[
                  {"name":"sku","in":"path","schema":{"type":"string"}},
                  {"name":"API-Version","in":"header","schema":{"type":"string","enum":["2.0","1.0"]}}]}},
              "/api/stock":{"get":{"parameters":[{"name":"API-Version","in":"header","schema":{"type":"string"}}]}}}}
            """;

    @Test
    @DisplayName("Ćwiczenie 1: nazwy grup z swagger-config (posortowane)")
    void exercise1() {
        assertThat(Api03GroupsAndVersions.exercise1(CONFIG)).containsExactly("Administracja", "Publiczne");
        assertThat(Api03GroupsAndVersions.exercise1("{\"configUrl\":\"x\"}")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 2: operacje z wymaganiem zabezpieczenia")
    void exercise2() {
        assertThat(Api03GroupsAndVersions.exercise2(DOC)).containsExactly("GET /admin/stats", "POST /api/orders");
    }

    @Test
    @DisplayName("Ćwiczenie 3: wartości enum parametru operacji")
    void exercise3() {
        assertThat(Api03GroupsAndVersions.exercise3(DOC, "/api/products/{sku}", "get", "API-Version"))
                .containsExactly("1.0", "2.0");
        assertThat(Api03GroupsAndVersions.exercise3(DOC, "/api/products/{sku}", "get", "nie-ma")).isEmpty();
        assertThat(Api03GroupsAndVersions.exercise3(DOC, "/api/stock", "get", "API-Version")).isEmpty();
        assertThatThrownBy(() -> Api03GroupsAndVersions.exercise3(DOC, "/api/stock", "post", "API-Version"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

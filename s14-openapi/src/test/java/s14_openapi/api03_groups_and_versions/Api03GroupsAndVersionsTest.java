package s14_openapi.api03_groups_and_versions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Api03GroupsAndVersions. Wycinek: {@code @SpringBootTest} + MockMvc z profilem "api03" (ustawienia
 * wersjonowania API z application-api03.properties) — springdoc potrzebuje pełnego kontekstu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("api03")
class Api03GroupsAndVersionsTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("Sekcja 1: bean OpenAPI ustawia tytuł, wersję, opis, kontakt i licencję całego dokumentu")
    void openApiBeanSetsGeneralInfo() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.info.title").value("Sklep — API"))
                .andExpect(jsonPath("$.info.version").value("2.0.0"))
                .andExpect(jsonPath("$.info.contact.email").value("api@sklep.example"))
                .andExpect(jsonPath("$.info.license.name").value("Apache 2.0"));
    }

    @Test
    @DisplayName("Sekcja 2: grupy dają osobne dokumenty — public ma tylko /api/**, admin tylko /admin/**")
    void groupsSplitTheDocument() throws Exception {
        mvc.perform(get("/v3/api-docs/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths", hasKey("/api/products")))
                .andExpect(jsonPath("$.paths", not(hasKey("/admin/stats"))));
        mvc.perform(get("/v3/api-docs/admin"))
                .andExpect(jsonPath("$.paths", aMapWithSize(2)))
                .andExpect(jsonPath("$.paths", hasKey("/admin/stats")))
                .andExpect(jsonPath("$.paths", hasKey("/admin/products/{sku}/restock")))
                .andExpect(jsonPath("$.paths", not(hasKey("/api/products"))));
        // informacje ogólne z beana OpenAPI są wspólne dla wszystkich grup
        mvc.perform(get("/v3/api-docs/admin")).andExpect(jsonPath("$.info.title").value("Sklep — API"));
        // dokument bez nazwy grupy nadal zawiera wszystko
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths", hasKey("/admin/stats")))
                .andExpect(jsonPath("$.paths", hasKey("/api/products")));
    }

    @Test
    @DisplayName("Sekcja 3: Swagger UI dostaje listę grup z nazwami wyświetlanymi (swagger-config)")
    void swaggerConfigListsGroups() throws Exception {
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(jsonPath("$.urls", hasSize(4)))
                .andExpect(jsonPath("$.urls[?(@.url == '/v3/api-docs/admin')].name").value("Administracja"))
                .andExpect(jsonPath("$.urls[?(@.url == '/v3/api-docs/public')].name").value("Publiczne"));
    }

    @Test
    @DisplayName("Sekcja 4: schemat zabezpieczeń to tylko opis — wymaganie dopisane operacjom grupy admin, nie publicznym")
    void securitySchemeIsOnlyDescription() throws Exception {
        mvc.perform(get("/v3/api-docs/admin"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/admin/stats'].get.security[0]", hasKey("bearerAuth")))
                .andExpect(jsonPath("$.paths['/admin/products/{sku}/restock'].post.security[0]", hasKey("bearerAuth")));
        mvc.perform(get("/v3/api-docs/public"))
                .andExpect(jsonPath("$.paths['/api/products'].get", not(hasKey("security"))));
        // i najważniejsze: dokument niczego nie chroni — punkt admin działa bez żadnego tokenu (w module nie ma Spring Security)
        mvc.perform(get("/admin/stats")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sekcja 5: springdoc rozumie wersje API — dokłada parametr nagłówka API-Version, a metody jednej ścieżki scala w jedną operację")
    void springdocUnderstandsApiVersions() throws Exception {
        // prawdziwe zachowanie aplikacji: ta sama ścieżka, różny kształt odpowiedzi
        mvc.perform(get("/api/products/ELE-001").header("API-Version", "2.0"))
                .andExpect(jsonPath("$.price.gross").value(5499.99));
        mvc.perform(get("/api/products/ELE-001"))   // bez nagłówka: wersja domyślna 1.0
                .andExpect(jsonPath("$.price").value(5499.99));

        // dokument: jedna operacja GET /api/products/{sku} (operationId z jednej z metod), odpowiedź oneOf V1/V2
        mvc.perform(get("/v3/api-docs/public"))
                .andExpect(jsonPath("$.paths['/api/products/{sku}']", aMapWithSize(1)))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.parameters[?(@.name == 'API-Version')].in")
                        .value("header"))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.responses['200'].content['*/*'].schema.oneOf",
                        hasSize(2)));
    }

    @Test
    @DisplayName("Sekcja 6: obejście — grupa na każdą wersję z własnym filtrem metod daje czyste dokumenty v1 i v2")
    void groupPerVersionGivesCleanDocuments() throws Exception {
        mvc.perform(get("/v3/api-docs/v1"))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.operationId").value("oneV1"))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ProductV1"))
                .andExpect(jsonPath("$.components.schemas", not(hasKey("ProductV2"))));
        mvc.perform(get("/v3/api-docs/v2"))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.operationId").value("oneV2"))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ProductV2"))
                .andExpect(jsonPath("$.components.schemas", not(hasKey("ProductV1"))))
                // metoda skus() ("1.0+") działa też w wersji 2, więc jest w obu dokumentach
                .andExpect(jsonPath("$.paths", hasKey("/api/products")));
    }
}

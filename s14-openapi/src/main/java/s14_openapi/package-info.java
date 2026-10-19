/**
 * Dział s14_openapi — DOKUMENTACJA API (OpenAPI = otwarty standard opisu interfejsów HTTP; springdoc-openapi = biblioteka,
 * która generuje taki opis z kontrolerów Spring MVC i wystawia Swagger UI).
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code api01_springdoc} — Api01Springdoc: dokument {@code /v3/api-docs} i Swagger UI bez żadnych adnotacji,
 *       ścieżki, operacje, schematy z rekordów, właściwości {@code springdoc.*}, wyłączanie na produkcji.</li>
 *   <li>{@code api02_documenting_api} — Api02DocumentingApi: {@code @Operation}, {@code @ApiResponse}, {@code @Parameter},
 *       {@code @Schema}, {@code @Tag}; ograniczenia Bean Validation w schemacie; ProblemDetail; test pilnujący dokumentu.</li>
 *   <li>{@code api03_groups_and_versions} — Api03GroupsAndVersions: {@code GroupedOpenApi}, bean {@code OpenAPI} (tytuł,
 *       kontakt), schemat zabezpieczeń, dokumentowanie wersji API z Spring Framework 7.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie (Api01Application…), więc kontrolery jednej lekcji nie trafiają do dokumentu innej —
 * dlatego kilka lekcji może używać tego samego adresu /api/products. Rozwiązania ćwiczeń leżą w pakiecie
 * {@code s14_openapi.solutions.<lekcja>}. Moduł nie ma wspólnego application.properties; ustawienia lekcji są w testach,
 * w klasie startowej albo w pliku profilu lekcji.</p>
 *
 * <p>Springdoc nie wchodzi do BOM-a Spring Boota — jego wersja ({@code springdoc.version}) jest w parent pom i musi
 * pasować do wersji Boota (linia 3.x springdoc dla Boota 4, 2.x dla Boota 3).</p>
 */
package s14_openapi;

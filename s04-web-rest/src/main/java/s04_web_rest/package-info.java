/**
 * Dział s04_web_rest — REST W SPRINGU (REST — Representational State Transfer, styl budowania API przez HTTP):
 * jak przyjąć zapytanie HTTP, odczytać z niego dane i odesłać odpowiedź w formacie JSON.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code rest01_controllers} — Rest01Controllers: {@code @RestController}, mapowania ({@code @GetMapping}…),
 *       jak zapytanie trafia do metody (DispatcherServlet).</li>
 *   <li>{@code rest02_request_data} — Rest02RequestData: {@code @PathVariable}, {@code @RequestParam},
 *       {@code @RequestHeader}, {@code @RequestBody}.</li>
 *   <li>{@code rest03_responses} — Rest03Responses: {@code ResponseEntity}, kody statusu HTTP, nagłówki
 *       ({@code Location}), {@code @ResponseStatus}.</li>
 *   <li>{@code rest04_json} — Rest04Json: Jackson 3 — rekordy jako JSON, nazwy pól, daty, {@code null}, nieznane pola.</li>
 *   <li>{@code rest05_crud_api} — Rest05CrudApi: kompletne API produktów (lista, szczegóły, dodanie, zmiana, usunięcie).</li>
 *   <li>{@code rest06_cors} — Rest06Cors: CORS — kiedy przeglądarka blokuje zapytania z innej domeny i jak to ustawić.</li>
 *   <li>{@code rest07_api_versioning} — Rest07ApiVersioning: wbudowane wersjonowanie API w Spring Framework 7.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie, więc kontrolery jednej lekcji nie są widoczne w drugiej.</p>
 */
package s04_web_rest;

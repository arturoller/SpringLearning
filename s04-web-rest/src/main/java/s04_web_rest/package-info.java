/**
 * Dział s04_web_rest — REST W SPRING MVC: jak aplikacja przyjmuje zapytania HTTP i odpowiada JSON-em
 * (REST = Representational State Transfer, styl budowania API opartego na adresach zasobów i metodach HTTP).
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code rest01_controllers} — Rest01Controllers: {@code @RestController}, {@code @RequestMapping},
 *       {@code @GetMapping}…{@code @DeleteMapping}, droga zapytania przez DispatcherServlet, 404/405, duplikaty mapowań.</li>
 *   <li>{@code rest02_request_data} — Rest02RequestData: {@code @PathVariable}, {@code @RequestParam},
 *       {@code @RequestBody}, {@code @RequestHeader}, konwersja typów i błąd 400, obiekt z parametrów zapytania.</li>
 *   <li>{@code rest03_responses} — Rest03Responses: {@code ResponseEntity}, 200/201 + Location/204/404,
 *       {@code @ResponseStatus}, {@code ResponseStatusException}, produces/consumes (406, 415), zwracanie null.</li>
 *   <li>{@code rest04_json} — Rest04Json: Jackson 3 (pakiet tools.jackson), adnotacje JSON, daty, BigDecimal,
 *       pola null, nieznane pola, {@code JsonMapperBuilderCustomizer}, {@code @JsonTest}; zmiany Jackson 2 → 3.</li>
 *   <li>{@code rest05_crud_api} — Rest05CrudApi: pełne CRUD produktów, DTO oddzielone od modelu, kody statusu,
 *       idempotentność PUT/DELETE, PATCH.</li>
 *   <li>{@code rest06_cors} — Rest06Cors: zasada tego samego pochodzenia, zapytanie wstępne (preflight),
 *       {@code @CrossOrigin}, globalna konfiguracja CORS, „*” a dane uwierzytelniające.</li>
 *   <li>{@code rest07_api_versioning} — Rest07ApiVersioning: wersjonowanie API wbudowane w Spring Framework 7
 *       (atrybut version, nagłówek/ścieżka/parametr/typ mediów, spring.mvc.apiversion.*, wersje przestarzałe).</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie (Rest01Application, Rest02Application…), więc kontrolery jednej lekcji nie są widoczne
 * w drugiej — dlatego kilka lekcji może używać tego samego adresu /api/products. Rozwiązania ćwiczeń leżą w pakiecie
 * {@code s04_web_rest.solutions.<lekcja>} z własną klasą startową (te same adresy co szkielety ćwiczeń, osobny kontekst).
 * Wspólny application.properties modułu nie jest potrzebny — ustawienia lekcji są w jej klasie startowej albo w teście.</p>
 */
package s04_web_rest;

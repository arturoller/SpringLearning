/**
 * Dział s04_web_rest — REST w Spring MVC: kontrolery, dane z zapytania, odpowiedzi i kody HTTP, JSON (Jackson 3),
 * pełne API CRUD, CORS i wersjonowanie API (nowość Spring Framework 7).
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet z własną klasą startową {@code RestNNApplication}):</p>
 * <ol>
 *   <li>{@code rest01_controllers} — Rest01Controllers: {@code @RestController} kontra {@code @Controller},
 *       {@code @RequestMapping}, {@code @GetMapping}/{@code @PostMapping}/..., jak DispatcherServlet wybiera metodę,
 *       404 i 405.</li>
 *   <li>{@code rest02_request_data} — Rest02RequestData: {@code @PathVariable}, {@code @RequestParam},
 *       {@code @RequestHeader}, {@code @RequestBody} z rekordami; 400 i 415.</li>
 *   <li>{@code rest03_responses} — Rest03Responses: {@code ResponseEntity}, kody 200/201/204/404, nagłówek Location,
 *       {@code @ResponseStatus}, własne nagłówki.</li>
 *   <li>{@code rest04_json} — Rest04Json: Jackson 3 w Boot 4, adnotacje {@code @JsonProperty}/{@code @JsonIgnore}/
 *       {@code @JsonInclude}, daty, nieznane pola, ustawienia {@code spring.jackson.*}.</li>
 *   <li>{@code rest05_crud_api} — Rest05CrudApi: kompletne API CRUD produktów w pamięci (GET, POST, PUT, PATCH,
 *       DELETE), idempotentność.</li>
 *   <li>{@code rest06_cors} — Rest06Cors: CORS, zapytanie wstępne (preflight), {@code @CrossOrigin} i konfiguracja
 *       globalna.</li>
 *   <li>{@code rest07_api_versioning} — Rest07ApiVersioning: wbudowane wersjonowanie API w Spring Framework 7.</li>
 * </ol>
 *
 * <p>Rozwiązania wzorcowe ćwiczeń: pakiet {@code s04_web_rest.solutions} (rodzeństwo lekcji, z własnymi klasami
 * startowymi). Gotowe zapytania do uruchomionych aplikacji: katalog {@code s04-web-rest/http}.</p>
 *
 * <p>Uwaga: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Gdyby była, skanowałaby wszystkie lekcje
 * naraz — a kilka lekcji ma kontrolery pod tym samym adresem (np. /api/products), więc aplikacja nie wystartowałaby
 * („Ambiguous mapping” — niejednoznaczne mapowanie).</p>
 */
package s04_web_rest;

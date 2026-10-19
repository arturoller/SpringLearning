/**
 * Dział s06_error_handling — OBSŁUGA BŁĘDÓW (error handling = obsługa błędów): co widzi klient, gdy w aplikacji
 * webowej coś pójdzie nie tak, i jak zamienić wyjątek na czytelną, bezpieczną odpowiedź.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code err01_default_errors} — Err01DefaultErrors: domyślna obsługa Boota (/error, BasicErrorController),
 *       JSON z czterema polami, strona Whitelabel, ustawienia {@code spring.web.error.*} (w Boot 4 zamiast
 *       {@code server.error.*}) i pułapka MockMvc, który nie przechodzi przez /error.</li>
 *   <li>{@code err02_exception_handler} — Err02ExceptionHandler: {@code @ResponseStatus} na wyjątku,
 *       {@code ResponseStatusException}, {@code @ExceptionHandler} w kontrolerze i to, który handler wygrywa.</li>
 *   <li>{@code err03_controller_advice} — Err03ControllerAdvice: {@code @RestControllerAdvice} dla całej aplikacji,
 *       zawężanie (pakiet, klasa), kolejność ({@code @Order}), mapa pole → komunikat, logowanie w jednym miejscu.</li>
 *   <li>{@code err04_problem_detail} — Err04ProblemDetail: standard RFC 9457 ({@code ProblemDetail},
 *       {@code application/problem+json}), {@code ResponseEntityExceptionHandler}, {@code spring.mvc.problemdetails.enabled},
 *       komunikaty z {@code MessageSource}.</li>
 *   <li>{@code err05_custom_exceptions} — Err05CustomExceptions: hierarchia wyjątków domeny sklepu, dobór kodów
 *       400/404/409/422/500/503, brak wycieku szczegółów i identyfikator błędu w odpowiedzi i w logu.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową ({@code Err01Application}…) w swoim podpakiecie, więc jej kontrolery i porady (np. {@code @RestControllerAdvice})
 * nie widzą klas z innych lekcji — inaczej porady z jednej lekcji zmieniałyby odpowiedzi w innej. Testy webowe
 * używają losowego portu albo MockMvc, nigdy portu 8080.</p>
 */
package s06_error_handling;

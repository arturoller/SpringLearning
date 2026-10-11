/**
 * Dział s06_error_handling — OBSŁUGA BŁĘDÓW w aplikacji webowej: co robi Spring Boot domyślnie, jak przejąć kontrolę
 * nad odpowiedzią z błędem i jak zaprojektować własne wyjątki.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code err01_default_errors} — Err01DefaultErrors: domyślna obsługa błędów Boota ({@code /error},
 *       {@code BasicErrorController}), co widać w MockMvc, a co przy prawdziwym serwerze.</li>
 *   <li>{@code err02_exception_handler} — Err02ExceptionHandler: {@code @ExceptionHandler} w kontrolerze,
 *       {@code @ResponseStatus}, {@code ResponseStatusException}.</li>
 *   <li>{@code err03_controller_advice} — Err03ControllerAdvice: globalna obsługa błędów
 *       ({@code @RestControllerAdvice}), zakres i kolejność.</li>
 *   <li>{@code err04_problem_detail} — Err04ProblemDetail: standard RFC 9457 ({@code ProblemDetail},
 *       {@code application/problem+json}).</li>
 *   <li>{@code err05_custom_exceptions} — Err05CustomExceptions: hierarchia własnych wyjątków domenowych, walidacja,
 *       logowanie i nieujawnianie szczegółów wewnętrznych.</li>
 * </ol>
 *
 * <p>Uwaga: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę startową
 * w swoim podpakiecie, a jej {@code @RestControllerAdvice} działa tylko w aplikacji tej lekcji.</p>
 */
package s06_error_handling;

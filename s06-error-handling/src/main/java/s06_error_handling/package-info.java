/**
 * Dział s06_error_handling — OBSŁUGA BŁĘDÓW (error handling): jak zamienić wyjątek w Javie na czytelną, spójną
 * odpowiedź HTTP z właściwym kodem statusu — w jednym miejscu, dla wszystkich kontrolerów.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code err01_default_errors} — Err01DefaultErrors: co Boot robi z wyjątkiem bez żadnej obsługi
 *       ({@code /error}, {@code BasicErrorController}, {@code server.error.*}).</li>
 *   <li>{@code err02_exception_handler} — Err02ExceptionHandler: {@code @ExceptionHandler} w jednym kontrolerze.</li>
 *   <li>{@code err03_controller_advice} — Err03ControllerAdvice: {@code @RestControllerAdvice} — obsługa globalna.</li>
 *   <li>{@code err04_problem_detail} — Err04ProblemDetail: {@code ProblemDetail} (RFC 9457),
 *       {@code ErrorResponseException}, {@code ResponseEntityExceptionHandler}.</li>
 *   <li>{@code err05_custom_exceptions} — Err05CustomExceptions: hierarchia własnych wyjątków domenowych sklepu
 *       i mapowanie ich na kody HTTP.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie — dlatego {@code @RestControllerAdvice} jednej lekcji nie obsługuje kontrolerów innej.</p>
 */
package s06_error_handling;

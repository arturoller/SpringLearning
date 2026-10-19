/**
 * Dział s17_events — ZDARZENIA APLIKACJI w Springu: publikowanie i obsługa zdarzeń, zdarzenia powiązane z transakcją,
 * zdarzenia w osobnym wątku i wzorzec outbox. Na bazie H2 w pamięci, bez weba.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet z własną klasą startową):</p>
 * <ol>
 *   <li>{@code evt01_application_events} — Evt01ApplicationEvents: {@code ApplicationEventPublisher},
 *       {@code @EventListener}, synchroniczność, wyjątki słuchaczy, {@code @Order}, {@code condition}, zdarzenia zwracane
 *       ze słuchaczy; po co zdarzenia i kiedy ich nie używać.</li>
 *   <li>{@code evt02_transactional_events} — Evt02TransactionalEvents: {@code @TransactionalEventListener} i jego
 *       fazy, publikacja poza transakcją ({@code fallbackExecution}), zapis do bazy w słuchaczu po zatwierdzeniu.</li>
 *   <li>{@code evt03_async_and_outbox} — Evt03AsyncAndOutbox: słuchacze {@code @Async}, zdarzenie tracone przy awarii
 *       i wzorzec outbox (zapis zdarzenia w tej samej transakcji, osobne wysyłanie).</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie, własny profil ({@code application-evtNN.properties}), własną bazę H2
 * ({@code jdbc:h2:mem:evtNN}) i własne migracje Flyway ({@code db/migration/evtNN}). Starter Flyway jest na ścieżce klas
 * całego modułu, więc lekcje, które go nie uczą, wyłączają go ustawieniem {@code spring.flyway.enabled=false}.
 * Rozwiązania ćwiczeń leżą w pakiecie {@code solutions}, obok lekcji.</p>
 */
package s17_events;

/**
 * Dział s15_actuator_logging — Actuator, metryki, logowanie i obserwowalność.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code obs01_actuator} — Obs01Actuator: punkty końcowe /actuator, wystawianie, dostęp, info, env, port zarządzania.</li>
 *   <li>{@code obs02_health_indicators} — Obs02HealthIndicators: własny HealthIndicator, szczegóły, grupy, sondy.</li>
 *   <li>{@code obs03_metrics} — Obs03Metrics: Micrometer — licznik, czasomierz, wskaźnik, tagi, metryki HTTP.</li>
 *   <li>{@code obs04_logging} — Obs04Logging: SLF4J i Logback, poziomy, MDC, logi strukturalne, zmiana poziomu w locie.</li>
 *   <li>{@code obs05_observations} — Obs05Observations: Observation API — jedna obserwacja, wiele sygnałów.</li>
 * </ol>
 *
 * <p>Uwaga: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja, która potrzebuje Springa, ma
 * własną klasę startową w swoim podpakiecie — dzięki temu lekcje nie widzą nawzajem swoich beanów.</p>
 */
package s15_actuator_logging;

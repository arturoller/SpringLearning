/**
 * Dział s13_rest_client — KLIENT HTTP (REST client): jak Twoja aplikacja woła CUDZE API — np. kurier, bramka
 * płatności, kursy walut — i jak przetrwać, gdy tamta usługa jest wolna, zwraca błędy albo nie odpowiada.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code client01_rest_client} — Client01RestClient: {@code RestClient} — GET/POST, JSON ↔ rekordy,
 *       {@code RestClient.Builder} z Boota, adres bazowy z konfiguracji.</li>
 *   <li>{@code client02_errors_timeouts} — Client02ErrorsTimeouts: kody 4xx/5xx ({@code onStatus}), wyjątki,
 *       timeouty połączenia i odczytu.</li>
 *   <li>{@code client03_http_interfaces} — Client03HttpInterfaces: {@code @HttpExchange} — klient opisany samym
 *       interfejsem Javy.</li>
 *   <li>{@code client04_resilience} — Client04Resilience: {@code @Retryable} i {@code @ConcurrencyLimit}
 *       wbudowane w Spring Framework 7.</li>
 *   <li>{@code client05_testing} — Client05Testing: {@code @RestClientTest}, {@code MockRestServiceServer}
 *       i lokalny serwer testowy.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}; każda lekcja ma własną klasę
 * startową. Testy nigdy nie łączą się z internetem — „zewnętrzne” API udaje atrapa albo serwer uruchomiony w teście.</p>
 */
package s13_rest_client;

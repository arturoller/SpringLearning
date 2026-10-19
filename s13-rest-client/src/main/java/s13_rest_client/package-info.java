/**
 * Dział s13_rest_client — KLIENT HTTP (HTTP client = klient HTTP): jak NASZA aplikacja wysyła zapytania do CUDZEGO API.
 * Scenariusz wszystkich lekcji: sklep pyta zewnętrzne API dostawcy (supplier = dostawca) o oferty, ceny i stan magazynu
 * oraz składa u niego zamówienia.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code rc01_rest_client} — Rc01RestClient: {@code RestClient} — budowanie (builder od Boota, adres bazowy,
 *       nagłówki domyślne), GET, {@code toEntity}, listy z {@code ParameterizedTypeReference}, POST, testy
 *       z {@code MockRestServiceServer} i z prawdziwym serwerem; porównanie z RestTemplate i WebClient.</li>
 *   <li>{@code rc02_errors_and_timeouts} — Rc02ErrorsAndTimeouts: wyjątki dla 4xx/5xx, {@code onStatus},
 *       {@code exchange}, limity czasu (właściwości {@code spring.http.clients.*}), błędy sieci, tłumaczenie
 *       błędów dostawcy na odpowiedzi naszego API, logowanie wywołań.</li>
 *   <li>{@code rc03_http_interfaces} — Rc03HttpInterfaces: klient opisany interfejsem ({@code @HttpExchange},
 *       {@code @GetExchange}, {@code @PostExchange}), {@code HttpServiceProxyFactory}, a w Spring Framework 7 / Boot 4
 *       grupy klientów: {@code @ImportHttpServices} i właściwości {@code spring.http.serviceclient.*}.</li>
 *   <li>{@code rc04_resilience} — Rc04Resilience: odporność wbudowana w Spring Framework 7 — {@code @Retryable},
 *       {@code @ConcurrencyLimit}, {@code @EnableResilientMethods}, {@code RetryTemplate}; pułapki: ponawianie
 *       operacji nieidempotentnej i wywołanie z tej samej klasy.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie i własny profil ustawień ({@code application-rcNN.properties}). „Zewnętrzny” dostawca
 * to udawany serwer (kontroler w tej samej aplikacji) — w testach nie łączymy się z internetem: zamiast dostawcy jest
 * {@code MockRestServiceServer} albo lokalny serwer na losowym porcie.</p>
 */
package s13_rest_client;

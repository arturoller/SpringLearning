package s13_rest_client.rc02_errors_and_timeouts;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * <pre>
 * TEMAT: Błędy i limity czasu klienta HTTP — gdy dostawca odpowiada 404/500, odpowiada za wolno albo wcale
 *        (error = błąd; timeout = limit czasu; status code = kod statusu; network = sieć)
 *
 * W SKRÓCIE:
 *   Cudze API zawodzi: produktu nie ma (404), serwer dostawcy ma awarię (500), jest przerwa techniczna (503), a czasem
 *   odpowiedź nie przychodzi wcale. RestClient zamienia statusy 4xx i 5xx na wyjątki (HttpClientErrorException,
 *   HttpServerErrorException), a kłopoty z siecią i limity czasu — na ResourceAccessException. Lekcja pokazuje:
 *   domyślne wyjątki, własną reakcję przez onStatus, pełną kontrolę przez exchange, limity czasu w Boot 4
 *   (spring.http.clients.*), tłumaczenie błędów dostawcy na odpowiedzi NASZEGO API i logowanie wywołań.
 *
 * ANALOGIA: telefon do hurtowni, który nie zawsze kończy się zamówieniem.
 *   „Nie mamy takiego towaru” (404) to normalna odpowiedź — zapisujesz i idziesz dalej. „System nam padł” (500,
 *   503) — oddzwonisz później. Najgorzej, gdy nikt nie odbiera: bez limitu czasu stoisz ze słuchawką przy uchu w
 *   nieskończoność, a kolejka klientów w sklepie rośnie. Limit czasu to postanowienie „czekam najwyżej 3 sygnały”.
 *   A klientowi przy kasie mówisz „hurtownia chwilowo nie odpowiada”, a nie czytasz mu na głos ich komunikatu błędu.
 *
 * JAK TO DZIAŁA:
 *   odpowiedź dostawcy                    retrieve() domyślnie rzuca
 *   2xx                                   — (body/toEntity zwraca wynik)
 *   404 Not Found                         HttpClientErrorException.NotFound   ┐
 *   400, 401, 403, 409, 429...            HttpClientErrorException.BadRequest… ├ HttpStatusCodeException
 *   418 i inne bez własnej podklasy       HttpClientErrorException            │   → RestClientResponseException
 *   500, 502, 503, 504                    HttpServerErrorException.…          ┘     → RestClientException
 *   brak odpowiedzi (sieć, limit czasu)   ResourceAccessException ─────────────────→ RestClientException
 *
 *   SupplierStockClient.stock("XXX-999") → 404 → onStatus → ProductNotFoundAtSupplierException
 *   ShopAvailabilityController            → @ExceptionHandler → 404 / 503 z ProblemDetail dla NASZEGO klienta
 *
 * SŁÓWKA:
 *   client error = błąd klienta (4xx — „źle zapytałeś”); server error = błąd serwera (5xx — „zepsuło się u nas”);
 *   resource access = dostęp do zasobu (tu: sieć); on status = przy statusie; exchange = wymiana (pełna obsługa
 *   odpowiedzi); connect timeout = limit czasu połączenia; read timeout = limit czasu odczytu (czekania na odpowiedź);
 *   interceptor = przechwytywacz; Retry-After = „spróbuj ponownie po” (nagłówek); gate = szlaban; latch = zatrzask.
 *
 * ZOBACZ TEŻ: s13_rest_client/Rc01RestClient (podstawy RestClienta), s06_error_handling/Err04ProblemDetail
 *             (ProblemDetail), s06_error_handling/Err02ExceptionHandler, t10_exceptions/Exceptions06ChainingWrapping
 *             (opakowywanie wyjątków), t28_networking_http/Http05AsyncTimeouts (limity czasu w kliencie JDK),
 *             t21_concurrency/Concurrency07Synchronizers (CountDownLatch), s13_rest_client/Rc04Resilience (ponawianie).
 * </pre>
 */
public final class Rc02ErrorsAndTimeouts {

    private Rc02ErrorsAndTimeouts() {
    }

    // =================================================================================================
    // 1. DOMYŚLNE WYJĄTKI — 4xx, 5xx (SupplierStockClient.stockWithDefaultErrors)
    // =================================================================================================
    //
    // retrieve() + body/toEntity przy statusie 4xx rzuca HttpClientErrorException, przy 5xx — HttpServerErrorException.
    // Popularne kody mają podklasy (NotFound, BadRequest, Conflict, TooManyRequests, InternalServerError,
    // ServiceUnavailable...), więc możesz łapać dokładnie ten przypadek: catch (HttpClientErrorException.NotFound e).
    // Kod bez podklasy (np. 418) daje samą klasę bazową. Wspólny przodek: RestClientResponseException — ma
    // getStatusCode(), getResponseHeaders(), getResponseBodyAsString() i getResponseBodyAs(ProblemDetail.class).
    // Komunikat ma postać „404 Not Found: "{treść}"” albo „503 Service Unavailable: [no body]” (sprawdzone testem).
    //
    // PUŁAPKA: catch (RestClientException e) { return null; } „na wszelki wypadek”. Dlaczego to błąd? Wrzucasz do
    //   jednego worka „nie ma produktu” (normalna sytuacja), „zepsuty serwer” (spróbuj później) i „błąd w naszym
    //   zapytaniu” (400 — trzeba poprawić kod). A null wybuchnie dalej, bez śladu przyczyny.
    // DOBRA PRAKTYKA: z treści błędu dostawcy (getResponseBodyAs(ProblemDetail.class)) korzystaj w logach, a nie
    //   w odpowiedzi dla swojego klienta. Dlaczego? To szczegóły cudzego systemu — mogą zawierać dane wewnętrzne.

    // =================================================================================================
    // 2. onStatus — własne wyjątki zamiast klas HTTP (SupplierStockClient.stock)
    // =================================================================================================
    //
    // .retrieve()
    // .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (req, res) -> { throw new ProductNotFound...; })
    // .onStatus(HttpStatusCode::is5xxServerError,                    (req, res) -> { throw new SupplierUnavailable...; })
    // .requiredBody(StockLevel.class)
    // Handlery sprawdzane są w kolejności dodania, przed domyślnym. Status bez własnego handlera (np. 400) obsługuje
    // dalej domyślny → HttpClientErrorException.BadRequest. Wyjątek RuntimeException rzucony w handlerze leci do
    // wołającego bez opakowania (IOException byłby opakowany w RestClientException).
    // Wyjątki DOMENOWE (ProductNotFoundAtSupplierException, SupplierUnavailableException) to język sklepu: serwis
    // i kontroler nie muszą wiedzieć, że pod spodem jest HTTP, a gdy zmienisz dostawcę — zmieniasz tylko klienta.
    //
    // PUŁAPKA: liczenie na onStatus przy limicie czasu albo braku połączenia. Dlaczego to błąd? Wtedy NIE MA
    //   odpowiedzi, więc nie ma statusu — onStatus się nie wykona, a poleci ResourceAccessException. Łap go osobno
    //   (try/catch w kliencie), jak w SupplierStockClient.stock.

    // =================================================================================================
    // 3. exchange — pełna kontrola nad odpowiedzią (SupplierStockClient.findStock)
    // =================================================================================================
    //
    // exchange((request, response) -> ...) / exchangeForRequiredValue(...): Spring wysyła zapytanie i oddaje Ci
    // odpowiedź. Żadnych wyjątków za status — Ty decydujesz: response.getStatusCode(), response.bodyTo(Typ.class),
    // a dla nieoczekiwanych statusów throw response.createException() (Spring Framework 7.0) — ten sam wyjątek, który
    // rzuciłby retrieve(). Odpowiedź jest zamykana po wyjściu z lambdy — nie wynoś z niej strumienia ciała.
    // Kiedy? Gdy „brak” to zwykła odpowiedź, nie wyjątek: 404 → Optional.empty(). Wyjątek byłby tu sterowaniem
    // przepływem przez try/catch — wolniej i mniej czytelnie.

    // =================================================================================================
    // 4. LIMITY CZASU — Boot 4: spring.http.clients.* (Rc02ErrorsAndTimeoutsServerTest)
    // =================================================================================================
    //
    //   connect timeout — ile czekać na NAWIĄZANIE połączenia (serwer wyłączony, zły adres, zapora);
    //   read timeout    — ile czekać na ODPOWIEDŹ, gdy połączenie już jest (serwer myśli, zawiesił się).
    // W application-rc02.properties: spring.http.clients.connect-timeout=2s i spring.http.clients.read-timeout=3s.
    // Boot zamienia je na HttpClientSettings i stosuje w fabryce zapytań KAŻDEGO RestClient.Builder, który daje.
    // Bez tych ustawień Boot nie ustawia limitów — zostają domyślne biblioteki; dla klienta z JDK (używanego tu)
    // oznacza to czekanie na odpowiedź bez ograniczenia.
    // Jeden klient z innym limitem — w kodzie:
    //   builder.requestFactory(ClientHttpRequestFactoryBuilder.jdk()
    //           .build(HttpClientSettings.defaults().withTimeouts(Duration.ofSeconds(2), Duration.ofMillis(200))))
    // Przekroczenie limitu → ResourceAccessException „I/O error on GET request for "http://...": Request cancelled”,
    // przyczyna java.net.http.HttpTimeoutException (dokładny tekst zależy od biblioteki HTTP — klient
    // SimpleClientHttpRequestFactory zgłosiłby SocketTimeoutException „Read timed out”). Brak serwera na porcie →
    // ResourceAccessException z przyczyną java.net.ConnectException.
    // Test jest deterministyczny: udawany serwer czeka przy szlabanie (SlowSupplierGate, CountDownLatch), który test
    // podnosi dopiero po asercji — serwer nie zdąży odpowiedzieć „przypadkiem szybciej”, a test niczego nie mierzy.
    //
    // PUŁAPKA: nazwy z tutoriali do Boota 3: spring.http.client.read-timeout (bez „s”). Dlaczego to błąd? W Boot 4
    //   nazwy zmieniono na spring.http.clients.*; stara nazwa jest tylko w metadanych jako przestarzała (IntelliJ ją
    //   podkreśli), ale NIE ustawia limitu — test oldPropertyNameIsIgnored pokazuje, że readTimeout zostaje null.
    // DOBRA PRAKTYKA: zawsze ustaw oba limity dla każdego zewnętrznego API. Dlaczego? Bez limitu jeden zawieszony
    //   dostawca blokuje wątki serwera Tomcat, które obsługują NASZYCH klientów — awaria dostawcy staje się naszą.
    // DOBRA PRAKTYKA: limit odczytu dobierz do tego, ile Twój klient może czekać (np. 2–5 s dla zapytania z przeglądarki),
    //   a nie do „najgorszego przypadku dostawcy”. Dlaczego? Lepiej szybko odpowiedzieć „spróbuj za chwilę” (503) niż
    //   po 60 sekundach, gdy użytkownik dawno odświeżył stronę.

    // =================================================================================================
    // 5. BŁĘDY DOSTAWCY ≠ BŁĘDY NASZEGO API (ShopAvailabilityController, Rc02ErrorsAndTimeoutsShopTest)
    // =================================================================================================
    //
    // Nieobsłużony HttpServerErrorException w kontrolerze to dla NASZEGO klienta 500 Internal Server Error — „to nasz
    // błąd”, a nie jest. Kontroler łapie wyjątki domenowe w @ExceptionHandler:
    //   ProductNotFoundAtSupplierException → 404 + ProblemDetail „Dostawca nie zna produktu XXX-999”,
    //   SupplierUnavailableException (5xx dostawcy, limit czasu, brak połączenia) → 503 Service Unavailable.
    // 503 mówi klientowi: „spróbuj później” — i tak jest. Test łączy dwie atrapy: MockMvc (nasze API) i
    // MockRestServiceServer (dostawca), a błąd sieci udaje withException(new SocketTimeoutException(...)).
    //
    // DOBRA PRAKTYKA: w odpowiedzi 503 nie pokazuj adresu dostawcy ani treści jego błędu. Dlaczego? To informacja
    //   o Twojej infrastrukturze (ułatwia atak) i myli klienta — szczegóły zapisz w logu.

    // =================================================================================================
    // 6. LOGOWANIE WYWOŁAŃ — ClientHttpRequestInterceptor (SupplierCallLoggingInterceptor)
    // =================================================================================================
    //
    // builder.requestInterceptor(new SupplierCallLoggingInterceptor()) — przechwytywacz owija każde wywołanie:
    //   „Dostawca: GET http://.../stock/ELE-001 → 200 OK”, „... → 404 NOT_FOUND”, „... → błąd sieci: java.net....”.
    // Test czyta logi przez OutputCaptureExtension (Boot) — bez zależności od czasu. Więcej szczegółów z samego Springa:
    // logging.level.org.springframework.web.client=DEBUG (m.in. do jakiego typu czytana jest odpowiedź).
    // Pomiary czasu i liczby wywołań (metryki) dokłada Boot do buildera, gdy jest Micrometer/Actuator —
    // s15_actuator_logging/Obs03Metrics.
    //
    // PUŁAPKA: logowanie całych zapytań z nagłówkami. Dlaczego to błąd? Nagłówek Authorization czy klucz API trafi do
    //   pliku logów, który czyta wiele osób i systemów. Loguj metodę, adres, status, a ciało najwyżej skrócone.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • 4xx → HttpClientErrorException(.NotFound, .BadRequest...), 5xx → HttpServerErrorException(.InternalServerError,
     *     .ServiceUnavailable...), brak odpowiedzi → ResourceAccessException; wspólny przodek: RestClientException.
     *   • ex.getStatusCode(), ex.getResponseBodyAsString(), ex.getResponseBodyAs(ProblemDetail.class).
     *   • onStatus(predykat, (req, res) -> { throw new MójWyjątek(); }) — przed domyślną obsługą; nie widzi błędów sieci.
     *   • exchange((req, res) -> ...) — pełna kontrola; res.bodyTo(Typ.class); res.createException() (Framework 7).
     *   • Limity (Boot 4): spring.http.clients.connect-timeout, spring.http.clients.read-timeout — NIE spring.http.client.*.
     *   • W kodzie: ClientHttpRequestFactoryBuilder.jdk().build(HttpClientSettings.defaults().withTimeouts(c, r)).
     *   • Przekroczony limit / brak połączenia → ResourceAccessException (przyczyna: HttpTimeoutException / ConnectException).
     *   • Błędy dostawcy tłumacz na swoje: 404 → 404, 5xx/limit/sieć → 503 (nie 500), bez szczegółów dostawcy.
     *   • Logowanie: ClientHttpRequestInterceptor w builderze; bez nagłówków z sekretami.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaki wyjątek rzuci retrieve().body(...) przy 404, przy 503 i gdy serwer nie odpowie w limicie czasu?
     *      Jaki mają wspólny typ?
     *   2. Co się stanie? Dostawca odpowie statusem 418 I'm a teapot. Jaka DOKŁADNIE klasa wyjątku i jaki komunikat?
     *   3. ZNAJDŹ BŁĄD: .onStatus(HttpStatusCode::isError, (req, res) -> { throw new SupplierUnavailableException("x"); })
     *      ma „złapać wszystkie kłopoty z dostawcą, także limity czasu”.
     *   4. Co się stanie? Kolega skopiował z tutoriala do Boota 3 linię spring.http.client.read-timeout=3s, a dostawca
     *      się zawiesił i nie odpowiada.
     *   5. Kiedy wybrać exchange(...) zamiast retrieve() z onStatus(...)?
     *   6. Dlaczego ShopAvailabilityController zamienia awarię dostawcy na 503, a nie przepuszcza 500?
     *   7. Dlaczego test limitu czasu używa szlabanu (CountDownLatch), a nie serwera, który „śpi 2 sekundy”?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Rc02ErrorsAndTimeoutsExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): pobierz stan magazynu GET /supplier/api/stock/{sku} (rekord StockLevel) i zwróć liczbę
     * dostępnych sztuk. Gdy dostawca odpowie 404 — zwróć -1. Inne błędy niech lecą dalej.
     * Podpowiedź: try { ... } catch (HttpClientErrorException.NotFound e) { return -1; }.
     */
    static int exercise1(RestClient client, String sku) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): opisz wyjątek klienta HTTP po polsku:
     * 404 → "brak produktu"; inny 4xx → "błąd zapytania: KOD"; 5xx → "awaria dostawcy: KOD";
     * ResourceAccessException → "brak połączenia albo przekroczony czas"; cokolwiek innego → "inny błąd".
     * KOD to liczba, np. "awaria dostawcy: 503".
     * Podpowiedź: switch z wzorcami typów (t23_modern_java/Modern05RecordsSealedPatterns) — kolejność ma znaczenie:
     * HttpClientErrorException.NotFound przed HttpClientErrorException; kod: e.getStatusCode().value().
     */
    static String exercise2(RestClientException exception) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): pobierz StockLevel z GET /supplier/api/stock/{sku}, ale:
     * 404 → ProductNotFoundAtSupplierException(sku); 503 → SupplierUnavailableException z komunikatem
     * "Przerwa u dostawcy, spróbuj za X s", gdzie X to wartość nagłówka Retry-After z odpowiedzi.
     * Podpowiedź: dwa onStatus(...); w handlerze response.getHeaders().getFirst("Retry-After").
     */
    static StockLevel exercise3(RestClient client, String sku) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zbuduj (bez buildera Boota — RestClient.builder()) klienta z adresem bazowym baseUrl
     * i limitem czasu ODCZYTU readTimeout. Test zapyta przez niego o WOLNY: przy zamkniętym szlabanie oczekuje
     * ResourceAccessException, przy otwartym — poprawnej odpowiedzi.
     * Podpowiedź: requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(HttpClientSettings.defaults()
     * .withReadTimeout(readTimeout))).
     */
    static RestClient exercise4(String baseUrl, Duration readTimeout) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 404 → HttpClientErrorException.NotFound, 503 → HttpServerErrorException.ServiceUnavailable, brak odpowiedzi
     *      w limicie → ResourceAccessException (przyczyna: java.net.http.HttpTimeoutException dla klienta JDK).
     *      Wspólny typ: RestClientException (dwa pierwsze są też RestClientResponseException).
     *   2. Dokładnie HttpClientErrorException (klasa bazowa — 418 nie ma własnej podklasy), komunikat
     *      „418 I'm a teapot: [no body]” (sprawdzone testem section1Hierarchy).
     *   3. onStatus działa tylko, gdy przyszła ODPOWIEDŹ ze statusem. Przy limicie czasu i braku połączenia odpowiedzi
     *      nie ma — poleci ResourceAccessException, którego ten handler nie zobaczy. Trzeba dodatkowo
     *      catch (ResourceAccessException e) wokół wywołania (jak w SupplierStockClient.stock).
     *   4. Limit NIE zostanie ustawiony: Boot 4 czyta spring.http.clients.*, a stara nazwa jest tylko oznaczona
     *      w metadanych jako przestarzała. HttpClientSettings.readTimeout() zostaje null (test oldPropertyNameIsIgnored),
     *      więc klient JDK czeka na odpowiedź bez ograniczenia, a wątek obsługujący zapytanie stoi.
     *   5. Gdy jakiś status „błędu” jest normalną odpowiedzią (404 = „nie ma” → Optional.empty()) albo gdy decyzja zależy
     *      od statusu i nagłówków naraz. retrieve() + onStatus wystarczy, gdy błąd ma się skończyć wyjątkiem.
     *   6. 500 znaczy „błąd po stronie naszego serwera”, a nasz kod działa — to dostawca zawiódł. 503 mówi „usługa
     *      chwilowo niedostępna, spróbuj później”, co jest prawdą i co klient może sensownie obsłużyć.
     *   7. „Śpiący” serwer zależy od zegara: na wolnym komputerze (albo w CI) limit i sen mogą się minąć i test raz
     *      przechodzi, raz nie. Szlaban gwarantuje, że serwer nie odpowie, dopóki test go nie otworzy — wynik zawsze
     *      ten sam, a test nie czeka dłużej niż sam limit klienta.
     */
    // </editor-fold>
}

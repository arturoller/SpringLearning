package s04_web_rest.rest06_cors;

/**
 * <pre>
 * TEMAT: CORS — dlaczego przeglądarka blokuje odpowiedzi z „obcych” serwerów (same-origin policy), zapytanie wstępne
 *        (preflight) OPTIONS, @CrossOrigin na kontrolerze i konfiguracja globalna WebMvcConfigurer#addCorsMappings
 *        (CORS = Cross-Origin Resource Sharing, współdzielenie zasobów między różnymi pochodzeniami; origin =
 *        pochodzenie: schemat + host + port, np. https://sklep.example.com; same-origin policy = zasada tego samego
 *        pochodzenia; preflight = zapytanie wstępne, „rozpoznanie”)
 *
 * W SKRÓCIE:
 *   JavaScript na stronie https://sklep.example.com chce pobrać dane z API https://api.example.com. To INNE pochodzenie,
 *   więc przeglądarka wpuści odpowiedź do skryptu tylko wtedy, gdy serwer API jawnie na to pozwoli nagłówkiem
 *   Access-Control-Allow-Origin. Przed „ryzykownymi” zapytaniami (PUT, DELETE, JSON w ciele) przeglądarka najpierw
 *   pyta zapytaniem OPTIONS (preflight): „czy https://sklep.example.com może wysłać PUT z Content-Type?”.
 *   W Springu pozwolenia dajesz adnotacją @CrossOrigin (jeden kontroler) albo globalnie w WebMvcConfigurer.
 *   Spring odrzuca zapytanie CORS spoza listy kodem 403 („Invalid CORS request”).
 *
 * ANALOGIA: ochroniarz w biurowcu klienta.
 *   CORS nie chroni Twojego API — chroni UŻYTKOWNIKA przeglądarki. Przeglądarka to ochroniarz, który nie wpuszcza do
 *   biura (skryptu na stronie) przesyłek z obcych firm, chyba że firma-nadawca (Twoje API) dołączy pisemną zgodę
 *   („ta przesyłka może trafić do sklep.example.com” — Access-Control-Allow-Origin). Kurier spoza budynku (curl,
 *   Postman, inny serwer) ochroniarza w ogóle nie spotyka — CORS go nie dotyczy.
 *
 * JAK TO DZIAŁA:
 *   Przeglądarka (strona https://panel.example.com)          Serwer Spring
 *   OPTIONS /api/orders                                      AbstractHandlerMapping znajduje handler i jego CorsConfiguration
 *     Origin: https://panel.example.com                      (z @CrossOrigin albo z addCorsMappings)
 *     Access-Control-Request-Method: POST                    DefaultCorsProcessor sprawdza pochodzenie, metodę, nagłówki
 *     Access-Control-Request-Headers: Content-Type           ✔ → 200 + Access-Control-Allow-Origin/-Methods/-Headers/-Max-Age
 *                                                            ✘ → 403 „Invalid CORS request”
 *   POST /api/orders  (dopiero gdy preflight się udał)        handler wykonuje się, odpowiedź + Access-Control-Allow-Origin
 *     Origin: https://panel.example.com
 *   Zapytanie BEZ nagłówka Origin (curl, ta sama strona) to nie jest zapytanie CORS — przechodzi normalnie.
 *
 * SŁÓWKA:
 *   origin = pochodzenie; cross-origin = między różnymi pochodzeniami; same-origin = to samo pochodzenie;
 *   preflight = zapytanie wstępne (OPTIONS); allow = zezwól; expose = ujawnij (nagłówki odpowiedzi dla JavaScriptu);
 *   max age = maksymalny wiek (ile sekund pamiętać wynik preflight); credentials = poświadczenia (ciasteczka,
 *   nagłówek Authorization); registry = rejestr; invalid = nieprawidłowy.
 *
 * ZOBACZ TEŻ: t28_networking_http/Http01UriUrl (schemat, host, port w adresie), t28_networking_http/Http04JsonApi,
 *             s04_web_rest/Rest01Controllers (OPTIONS i nagłówek Allow), s12_security (CORS razem ze Spring Security).
 * </pre>
 */
public final class Rest06Cors {

    private Rest06Cors() {
    }

    // =================================================================================================
    // 1. POCHODZENIE (ORIGIN) i ZASADA TEGO SAMEGO POCHODZENIA (Rest06CorsTest, sekcja 1)
    // =================================================================================================
    //
    // Pochodzenie = schemat + host + port. Wszystkie trzy muszą się zgadzać:
    //   https://sklep.example.com/a  i  https://sklep.example.com/b        → to samo (ścieżka się nie liczy)
    //   https://sklep.example.com    i  http://sklep.example.com           → różne (schemat)
    //   https://sklep.example.com    i  https://api.sklep.example.com      → różne (host — subdomena to inny host!)
    //   http://localhost:3000        i  http://localhost:8080              → różne (port) — typowe przy nauce:
    //                                                                        frontend na 3000, Spring na 8080
    // Przeglądarka WYSYŁA zapytanie do innego pochodzenia, ale bez zgody serwera NIE POKAZUJE odpowiedzi skryptowi.
    // Dlaczego tak? Gdyby każda strona mogła czytać odpowiedzi z dowolnego serwera, złośliwa strona odczytałaby
    // np. Twoją skrzynkę pocztową — przeglądarka dołącza do zapytań Twoje ciasteczka z zalogowania.
    //
    // PUŁAPKA: zapytanie bez nagłówka Origin NIE jest zapytaniem CORS. Dlaczego ważne? curl, Postman, plik .http
    //   i testy MockMvc bez .header("Origin", ...) zawsze „działają” — błąd CORS zobaczysz dopiero w przeglądarce.
    //   GET /api/catalog bez Origin → 200 i brak nagłówków Access-Control-*. Sprawdzone testem.

    // =================================================================================================
    // 2. BRAK KONFIGURACJI CORS — co się dzieje (sekcja 2)
    // =================================================================================================
    //
    // InternalStatsController nie ma żadnych reguł CORS. Zapytanie z Origin: https://zlodziej.example.com:
    //   GET /api/internal/stats → 200 i dane, ale BEZ Access-Control-Allow-Origin → przeglądarka nie odda ich skryptowi.
    //   preflight OPTIONS → 200, ale bez nagłówków Access-Control-* → przeglądarka nie wyśle właściwego zapytania.
    //
    // PUŁAPKA: CORS to NIE zabezpieczenie API. Dlaczego? Serwer i tak WYKONAŁ zapytanie (status 200 w teście) —
    //   blokuje tylko przeglądarka i tylko odczyt odpowiedzi. Kto chce Twoje API wywołać z serwera lub z curl, zrobi to.
    //   Do ochrony danych służy uwierzytelnianie i autoryzacja (s12_security). PYTANIE REKRUTACYJNE: „Czy CORS
    //   chroni API przed nieuprawnionym dostępem?” — nie.

    // =================================================================================================
    // 3. @CrossOrigin NA KONTROLERZE (sekcja 3)
    // =================================================================================================
    //
    //   @CrossOrigin(origins = "https://sklep.example.com")   ← na klasie (wszystkie metody) albo na jednej metodzie
    //   GET /api/catalog, Origin: https://sklep.example.com   → 200 + Access-Control-Allow-Origin: https://sklep.example.com
    //   GET /api/catalog, Origin: https://zlodziej.example.com → 403 „Invalid CORS request” (handler się NIE wykonuje)
    //   preflight OPTIONS z Origin sklepu i Access-Control-Request-Method: GET
    //                                                         → 200 + Access-Control-Allow-Methods: GET
    //                                                           + Access-Control-Max-Age: 1800 (domyślne 30 minut)
    // Gdy kontroler ma konfigurację CORS, Spring sam odrzuca obce pochodzenia (403), zanim wywoła Twoją metodę.
    // Domyślnie @CrossOrigin pozwala na metody z mapowania (tu GET) i wszystkie nagłówki zapytania.
    //
    // PUŁAPKA: @CrossOrigin bez origins (albo origins = "*") wpuszcza KAŻDĄ stronę. Dlaczego niebezpieczne przy
    //   API z logowaniem? Każda strona w internecie może z przeglądarki zalogowanego użytkownika czytać odpowiedzi.
    //   Wymieniaj pochodzenia jawnie. (Spring i tak nie pozwoli połączyć "*" z allowCredentials(true).)

    // =================================================================================================
    // 4. KONFIGURACJA GLOBALNA — WebMvcConfigurer#addCorsMappings (sekcja 4)
    // =================================================================================================
    //
    //   @Configuration class CorsConfig implements WebMvcConfigurer {
    //       public void addCorsMappings(CorsRegistry registry) {
    //           registry.addMapping("/api/orders/**").allowedOrigins("https://panel.example.com")
    //                   .allowedMethods("GET", "POST").allowedHeaders("Content-Type")
    //                   .exposedHeaders("X-Total-Count").maxAge(3600);
    //   }}
    //   preflight POST z Content-Type z panelu      → 200 + Allow-Methods: GET,POST + Allow-Headers: Content-Type + Max-Age: 3600
    //   preflight DELETE z panelu                   → 403 (DELETE nie jest na liście, choć kontroler go obsługuje!)
    //   preflight z nagłówkiem X-Secret             → 403 (nagłówek spoza listy)
    //   GET z Origin sklepu (nie panelu)            → 403
    // CorsConfig leży w pakiecie lekcji, więc działa TYLKO w Rest06Application. Aplikacja lekcji Rest01 (inny pakiet)
    // jej nie widzi — sprawdzone testem Rest06CorsIsolationTest.
    //
    // DOBRA PRAKTYKA: reguły CORS trzymaj w JEDNYM miejscu (konfiguracja globalna), a listę pochodzeń — w ustawieniach
    //   (application.properties), nie w kodzie. Dlaczego? Pochodzenia różnią się między środowiskami (localhost:3000
    //   na komputerze, https://sklep.example.com na produkcji), a rozrzucone @CrossOrigin łatwo przeoczyć.

    // =================================================================================================
    // 5. CORS a SPRING SECURITY (tylko zapowiedź)
    // =================================================================================================
    //
    // PUŁAPKA: po dodaniu Spring Security konfiguracja CORS z WebMvcConfigurer może „przestać działać” — preflight
    //   dostaje 401 albo 403. Dlaczego? Filtry bezpieczeństwa działają PRZED DispatcherServlet, a preflight nigdy nie
    //   niesie poświadczeń (loginu, tokenu), więc może zostać odrzucony, zanim dotrze do mechanizmu CORS Spring MVC.
    //   CORS trzeba włączyć także w łańcuchu filtrów Security (http.cors(...)) — szczegóły w dziale s12_security.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Origin = schemat + host + port. Inny którykolwiek → inne pochodzenie → potrzebna zgoda CORS.
     *   • CORS egzekwuje PRZEGLĄDARKA; curl/Postman/serwery go nie znają. CORS ≠ zabezpieczenie API.
     *   • Zapytanie CORS ma nagłówek Origin; preflight = OPTIONS + Origin + Access-Control-Request-Method (+ -Headers).
     *   • Odpowiedź-zgoda: Access-Control-Allow-Origin, -Allow-Methods, -Allow-Headers, -Expose-Headers, -Max-Age.
     *   • @CrossOrigin(origins = "https://...") na klasie/metodzie; domyślne max-age 1800 s.
     *   • Globalnie: WebMvcConfigurer#addCorsMappings → registry.addMapping("/api/x/**").allowedOrigins(...)...
     *   • Spring: obce pochodzenie / metoda / nagłówek przy skonfigurowanym CORS → 403 „Invalid CORS request”.
     *   • Brak konfiguracji CORS → zapytanie się wykonuje, ale bez nagłówków zgody (przeglądarka blokuje odczyt).
     *   • Ze Spring Security: CORS trzeba włączyć także w Security (s12_security).
     *
     * PYTANIA KONTROLNE:
     *   1. Z czego składa się pochodzenie (origin)? Czy https://sklep.example.com i https://api.sklep.example.com to
     *      to samo pochodzenie?
     *   2. Kto egzekwuje CORS — serwer czy przeglądarka? Czy CORS chroni API przed curl? (PYTANIE REKRUTACYJNE)
     *   3. Co się stanie? GET /api/catalog z nagłówkiem Origin: https://zlodziej.example.com.
     *   4. Co się stanie? Panel (https://panel.example.com) wysyła preflight dla DELETE /api/orders/5.
     *   5. Kiedy przeglądarka wysyła preflight i co w nim jest?
     *   6. ZNAJDŹ BŁĄD: „Mój test MockMvc GET /api/orders przechodzi, a frontend na localhost:3000 dostaje błąd CORS.”
     *      Czego brakuje w teście, a czego w konfiguracji?
     *   7. Dlaczego CorsConfig z tej lekcji nie zmienia zachowania aplikacji z lekcji Rest01?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — ćwiczenia 1–2 poniżej, ćwiczenie 3 w ExerciseCorsConfig. Testy: Rest06CorsExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): wytnij pochodzenie (origin) z pełnego adresu: schemat + "://" + host, a port tylko wtedy,
     * gdy jest podany jawnie. Przykład: "https://sklep.example.com:8443/api/x?y=1" → "https://sklep.example.com:8443";
     * "http://localhost/api" → "http://localhost". Podpowiedź: URI.create(url) i getScheme(), getHost(), getPort()
     * (-1 = brak portu) — t28_networking_http/Http01UriUrl.
     */
    static String exercise1(String url) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): czy dwa adresy mają to samo pochodzenie? Porównaj schemat (bez względu na wielkość liter),
     * host (też) i port — brak portu znaczy port domyślny: 80 dla http, 443 dla https.
     * Przykłady: ("https://sklep.example.com/a", "https://sklep.example.com:443/b") → true;
     * ("http://localhost:3000", "http://localhost:8080") → false; ("https://a.example.com", "https://b.example.com") → false.
     */
    static boolean exercise2(String url1, String url2) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Schemat + host + port. Nie — api.sklep.example.com to inny host (subdomena), więc inne pochodzenie.
     *   2. Przeglądarka. CORS nie chroni API przed curl, Postmanem ani innym serwerem — oni nie stosują tej zasady.
     *      Do ochrony API służą uwierzytelnianie i autoryzacja (s12_security).
     *   3. 403 Forbidden z treścią „Invalid CORS request” — kontroler ma @CrossOrigin tylko dla sklepu, więc Spring
     *      odrzuca obce pochodzenie, zanim wywoła metodę. Sprawdzone testem (sekcja 3).
     *   4. 403 — globalna reguła dla /api/orders/** pozwala tylko na GET i POST, choć kontroler obsługuje DELETE.
     *      Sprawdzone testem (sekcja 4).
     *   5. Przed zapytaniami „nieprostymi”: metody inne niż GET/HEAD/POST, POST z Content-Type: application/json,
     *      własne nagłówki (np. Authorization). Preflight to OPTIONS z nagłówkami Origin,
     *      Access-Control-Request-Method i (opcjonalnie) Access-Control-Request-Headers.
     *   6. Test nie wysyła nagłówka Origin, więc to nie jest zapytanie CORS — zawsze przejdzie. W konfiguracji brakuje
     *      pochodzenia http://localhost:3000 na liście allowedOrigins. Dodaj je i dopisz w teście .header("Origin", ...).
     *   7. CorsConfig to bean w pakiecie s04_web_rest.rest06_cors, a każda lekcja ma własną klasę
     *      @SpringBootApplication skanującą tylko swój pakiet. Sprawdzone testem (Rest06CorsIsolationTest).
     */
    // </editor-fold>
}

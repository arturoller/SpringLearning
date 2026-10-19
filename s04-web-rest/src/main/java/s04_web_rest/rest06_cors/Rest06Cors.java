package s04_web_rest.rest06_cors;

/**
 * <pre>
 * TEMAT: CORS — wywołania API ze strony z innego pochodzenia: zasada tego samego pochodzenia, zapytanie wstępne
 *        (preflight, OPTIONS), {@code @CrossOrigin}, globalna konfiguracja {@code WebMvcConfigurer.addCorsMappings},
 *        „*” a ciasteczka (allowCredentials) i wzorce pochodzeń
 *        (CORS = Cross-Origin Resource Sharing — współdzielenie zasobów między pochodzeniami; origin = pochodzenie;
 *        same-origin policy = zasada tego samego pochodzenia; credentials = dane uwierzytelniające, np. ciasteczka)
 *
 * W SKRÓCIE:
 *   Pochodzenie strony = schemat + host + port (http://localhost:3000). Przeglądarka pozwala skryptowi strony
 *   CZYTAĆ odpowiedzi tylko z tego samego pochodzenia. Frontend z http://localhost:3000, który woła API na
 *   http://localhost:8080, potrzebuje więc ZGODY serwera — nagłówków Access-Control-Allow-*. Spring dodaje je według
 *   {@code @CrossOrigin} albo globalnej konfiguracji. Przed „nieprostymi” zapytaniami (PUT, DELETE, JSON, własne
 *   nagłówki) przeglądarka pyta najpierw zapytaniem wstępnym OPTIONS. CORS egzekwuje PRZEGLĄDARKA, a nie serwer —
 *   curl i plik .http nic o nim nie wiedzą.
 *
 * ANALOGIA: ochroniarz w biurowcu sklepu.
 *   Pracownik z innej firmy (strona z innego pochodzenia) przychodzi po dokumenty. Ochroniarz (przeglądarka) pyta
 *   recepcję API: „czy ta firma może je odebrać?”. Lista firm na recepcji to konfiguracja CORS. Przy grubszych
 *   sprawach (przeprowadzka mebli = PUT/DELETE) ochroniarz najpierw dzwoni z pytaniem (preflight), a dopiero potem
 *   wpuszcza ekipę. Kurier, który wchodzi tylnym wejściem bez ochroniarza (curl), w ogóle nie jest sprawdzany —
 *   dlatego CORS to NIE jest zabezpieczenie serwera przed złodziejem, tylko ochrona użytkownika przeglądarki.
 *
 * JAK TO DZIAŁA:
 *   1. Przeglądarka dokleja do zapytania z innego pochodzenia nagłówek Origin: http://localhost:3000.
 *   2. Spring (AbstractHandlerMapping) znajduje metodę kontrolera i jej CorsConfiguration (z {@code @CrossOrigin}
 *      i/lub z addCorsMappings — łączy je). DefaultCorsProcessor porównuje Origin z konfiguracją:
 *        zgoda    → dodaje Access-Control-Allow-Origin (i inne), metoda kontrolera działa normalnie,
 *        odmowa   → 403 i ciało „Invalid CORS request”, metoda kontrolera NIE rusza,
 *        brak konfiguracji → żadnych nagłówków CORS; odpowiedź idzie, ale przeglądarka jej skryptowi nie pokaże.
 *   3. Preflight: OPTIONS + Origin + Access-Control-Request-Method (+ Access-Control-Request-Headers). Spring odpowiada
 *      sam (bez wołania kontrolera) nagłówkami Allow-Methods, Allow-Headers, Max-Age — albo 403.
 *
 * SŁÓWKA:
 *   preflight = zapytanie wstępne (OPTIONS przed właściwym); simple request = zapytanie proste (bez preflight);
 *   allowed origins = dozwolone pochodzenia; origin pattern = wzorzec pochodzenia (https://*.sklep.example);
 *   exposed headers = nagłówki udostępnione skryptowi; max age = maksymalny wiek (jak długo pamiętać zgodę);
 *   frontend = część aplikacji działająca w przeglądarce; backend = serwer z API; wildcard = symbol wieloznaczny („*”).
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest05CrudApi (API, które woła frontend), t28_networking_http/Http01UriUrl (budowa adresu —
 *             schemat, host, port), s12_security/Sec01Basics (CORS a Spring Security — filtry przed DispatcherServlet),
 *             s12_security/Sec04StatelessApi (API z tokenami zamiast ciasteczek).
 * </pre>
 */
public final class Rest06Cors {

    private Rest06Cors() {
    }

    // =================================================================================================
    // 1. ZASADA TEGO SAMEGO POCHODZENIA (same-origin policy)
    // =================================================================================================
    //
    //   pochodzenie = schemat://host:port
    //   http://localhost:3000  vs  http://localhost:8080   → RÓŻNE (inny port)
    //   http://sklep.example   vs  https://sklep.example   → RÓŻNE (inny schemat)
    //   https://sklep.example  vs  https://api.sklep.example → RÓŻNE (inny host — subdomena to inny host)
    //   https://sklep.example/a vs https://sklep.example/b  → TO SAMO (ścieżka się nie liczy)
    //
    // Przeglądarka nie pozwala skryptowi strony A czytać odpowiedzi z pochodzenia B. Dlaczego? Gdyby pozwalała,
    // dowolna strona otwarta w Twojej przeglądarce mogłaby — z Twoimi ciasteczkami — czytać Twoją pocztę czy konto
    // w banku. CORS to kontrolowany wyjątek od tej zasady: serwer B mówi „stronie A wolno”.
    //
    // PUŁAPKA: „w pliku .http działa, w przeglądarce nie” (albo odwrotnie: „dodałem CORS i nic się nie zmieniło
    //   w curl”). Dlaczego? CORS egzekwuje PRZEGLĄDARKA. curl, plik .http, Postman i inny serwer nie wysyłają Origin
    //   (albo ignorują odpowiedź na niego) — test noOriginNoCors. Błąd CORS diagnozujesz w narzędziach deweloperskich
    //   przeglądarki (F12 → Console/Network), a w .http udajesz przeglądarkę, dopisując nagłówek Origin.

    // =================================================================================================
    // 2. GLOBALNIE: WebMvcConfigurer.addCorsMappings (CorsConfig; testy globalConfig*)
    // =================================================================================================
    //
    //   registry.addMapping("/api/shop/**")
    //           .allowedOrigins("http://localhost:3000")         → Access-Control-Allow-Origin: http://localhost:3000
    //           .allowedMethods("GET", "POST", "PUT", "DELETE")  → Access-Control-Allow-Methods (w preflight)
    //           .allowedHeaders("Content-Type", "X-Shop-Id")     → Access-Control-Allow-Headers (w preflight)
    //           .exposedHeaders("Location", "X-Total-Count")     → Access-Control-Expose-Headers
    //           .maxAge(3600);                                   → Access-Control-Max-Age (w preflight)
    //
    // exposedHeaders: domyślnie skrypt widzi tylko kilka „bezpiecznych” nagłówków odpowiedzi (np. Content-Type).
    // Location po 201 albo X-Total-Count trzeba udostępnić jawnie — inaczej frontend dostanie null.
    // Boot nie ma właściwości spring.mvc.cors.* — globalny CORS dla Spring MVC konfigurujesz kodem (WebMvcConfigurer).
    //
    // PUŁAPKA: samo registry.addMapping("/api/**") bez dalszych wywołań. Dlaczego groźne? Domyślne wartości to
    //   KAŻDE pochodzenie ("*"), metody GET, HEAD, POST, wszystkie nagłówki i max-age 1800 — czyli zgoda dla całego
    //   internetu. Zawsze podawaj allowedOrigins albo allowedOriginPatterns wprost.

    // =================================================================================================
    // 3. ZAPYTANIE WSTĘPNE — PREFLIGHT (testy preflightAllowed, preflightRejected)
    // =================================================================================================
    //
    // Zapytanie PROSTE (GET/HEAD/POST, tylko podstawowe nagłówki, Content-Type tylko text/plain, form-urlencoded albo
    // multipart/form-data) przeglądarka wysyła od razu i po prostu sprawdza nagłówki odpowiedzi. Każde inne — PUT,
    // DELETE, PATCH, POST z application/json, własny nagłówek X-Shop-Id albo Authorization — poprzedza zapytaniem:
    //   OPTIONS /api/shop/products
    //   Origin: http://localhost:3000
    //   Access-Control-Request-Method: PUT
    //   Access-Control-Request-Headers: content-type
    // Serwer odpowiada 200 + Access-Control-Allow-Methods: GET,POST,PUT,DELETE + Allow-Headers + Max-Age: 3600 —
    // dopiero wtedy przeglądarka wysyła właściwy PUT. Metoda spoza listy (PATCH) albo nagłówek spoza listy
    // (authorization) → 403 i właściwe zapytanie w ogóle nie wychodzi.
    //
    // PUŁAPKA: zapytanie proste (np. zwykły POST formularza) przeglądarka WYKONUJE, nawet gdy CORS się nie zgadza —
    //   blokuje tylko odczyt odpowiedzi. Dlaczego to ważne? Serwer mógł już coś zmienić (efekt uboczny). Ochrona przed
    //   takimi zapytaniami z obcych stron (CSRF) to zadanie Spring Security — s12_security.

    // =================================================================================================
    // 4. @CrossOrigin NA METODZIE ALBO KLASIE (PublicController, PartnerController)
    // =================================================================================================
    //
    //   @CrossOrigin(origins = "*")                       — każde pochodzenie (dane publiczne: godziny otwarcia)
    //   @CrossOrigin(origins = "http://localhost:5173")   — jedno pochodzenie
    //   @CrossOrigin                                       — BEZ atrybutów: też każde pochodzenie!
    //
    // Domyślnie @CrossOrigin zezwala na metody z mapowania (tu GET), wszystkie nagłówki i max-age 1800 sekund.
    // Na klasie — dotyczy wszystkich metod kontrolera. Gdy pasuje też konfiguracja globalna, Spring łączy obie.
    //
    // DOBRA PRAKTYKA: jedna, globalna konfiguracja CORS (CorsConfig) zamiast @CrossOrigin rozsianych po kontrolerach.
    //   Dlaczego? Lista dozwolonych stron to decyzja o bezpieczeństwie — powinna być w jednym miejscu, łatwa do
    //   przeglądu i do zmiany profilem (inne adresy na produkcji). @CrossOrigin zostaw na wyjątki.

    // =================================================================================================
    // 5. „*” + allowCredentials(true) I WZORCE POCHODZEŃ (Rest06CorsCredentialsTest, originPatternsWithCredentials)
    // =================================================================================================
    //
    // allowCredentials(true) = przeglądarka może wysłać ciasteczka/nagłówek Authorization i pokazać odpowiedź.
    // Specyfikacja CORS zabrania wtedy odpowiedzi Access-Control-Allow-Origin: * — musi być KONKRETNE pochodzenie.
    // Spring pilnuje tego i rzuca IllegalArgumentException:
    //   „When allowCredentials is true, allowedOrigins cannot contain the special value "*" since that cannot be set
    //    on the "Access-Control-Allow-Origin" response header. To allow credentials to a set of origins, list them
    //    explicitly or consider using "allowedOriginPatterns" instead.”
    //   • w @CrossOrigin(origins = "*", allowCredentials = "true") — przy STARCIE aplikacji (rejestracja mapowań),
    //   • w addCorsMappings(...).allowedOrigins("*").allowCredentials(true) — aplikacja startuje, ale KAŻDE zapytanie
    //     pod /api/** kończy się 500, nawet bez nagłówka Origin (globalna konfiguracja jest sprawdzana przy każdym
    //     zapytaniu do pasującego adresu).
    //
    // Rozwiązanie: allowedOriginPatterns("https://*.partner.example") (w adnotacji: originPatterns). Wzorzec może mieć
    // „*”, a Spring odsyła w odpowiedzi KONKRETNE pochodzenie klienta (https://hurt.partner.example) — zgodnie ze
    // specyfikacją.
    //
    // PUŁAPKA: allowedOriginPatterns("*") + allowCredentials(true) „działa” — i właśnie dlatego jest groźne. Dlaczego?
    //   Każda strona w internecie może wtedy wołać Twoje API z ciasteczkami zalogowanego użytkownika. Wzorzec ma
    //   zawężać (twoje subdomeny), a nie omijać zabezpieczenie.

    // =================================================================================================
    // 6. BRAK KONFIGURACJI = SERWER ODPOWIADA, PRZEGLĄDARKA BLOKUJE (InternalController; test noCorsConfig...)
    // =================================================================================================
    //
    // Bez CORS zapytanie z Origin dostaje 200 i dane — tylko bez Access-Control-Allow-Origin. Przeglądarka pokaże
    // w konsoli błąd „...has been blocked by CORS policy...”, a skrypt dostanie błąd sieci. Preflight dostaje 200 bez
    // nagłówków zgody, więc właściwy PUT/DELETE w ogóle nie wychodzi.
    // Zapowiedź: ze Spring Security (s12_security) o CORS trzeba zadbać także w łańcuchu filtrów (http.cors(...)).
    // Dlaczego? Filtry bezpieczeństwa działają PRZED DispatcherServlet — preflight bez logowania musi przez nie przejść.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Pochodzenie = schemat + host + port. Inny port = inne pochodzenie.
     *   • CORS egzekwuje przeglądarka; curl/.http go nie widzą. Testuj z nagłówkiem Origin.
     *   • Globalnie: WebMvcConfigurer.addCorsMappings → addMapping, allowedOrigins/allowedOriginPatterns, allowedMethods,
     *     allowedHeaders, exposedHeaders, allowCredentials, maxAge. Lokalnie: @CrossOrigin na metodzie/klasie.
     *   • Preflight = OPTIONS + Origin + Access-Control-Request-Method — przed PUT/DELETE/PATCH, JSON, własnymi nagłówkami.
     *   • Odmowa: 403 „Invalid CORS request”. Brak konfiguracji: 200 bez nagłówków (blokuje przeglądarka).
     *   • "*" + allowCredentials(true) → IllegalArgumentException (adnotacja: start; globalnie: 500 przy zapytaniu).
     *     Użyj allowedOriginPatterns z konkretną domeną.
     *   • exposedHeaders dla Location, X-Total-Count — inaczej skrypt ich nie przeczyta.
     *
     * PYTANIA KONTROLNE:
     *   1. Z czego składa się pochodzenie? Czy http://localhost:3000 i http://localhost:8080 to to samo pochodzenie?
     *   2. Kiedy przeglądarka wysyła zapytanie wstępne (preflight) i jakie nagłówki w nim są?
     *   3. Co się stanie? Frontend z http://localhost:3000 wywoła GET /api/internal/report (bez konfiguracji CORS) —
     *      co odpowie serwer i co zobaczy skrypt?
     *   4. ZNAJDŹ BŁĄD: @CrossOrigin(origins = "*", allowCredentials = "true") nad metodą kontrolera — co się stanie
     *      przy uruchomieniu aplikacji?
     *   5. Co się stanie? W CorsConfig zmienisz allowedOrigins na "*" i dodasz .allowCredentials(true), a potem
     *      wyślesz z pliku .http zwykłe GET /api/shop/products (bez Origin).
     *   6. Frontend po POST dostaje 201, ale response.headers.get("Location") zwraca null. Co trzeba dodać w CORS?
     *   7. Dlaczego CORS nie chroni serwera przed atakiem z curl?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — Rest06ExerciseController (ćwiczenie 1) i Rest06Exercises (ćwiczenia 2–4),
    // testy: Rest06CorsExercisesTest. Rozwiązania wzorcowe: s04_web_rest.solutions.rest06_cors.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Schemat + host + port. Nie — różnią się portem, więc to dwa różne pochodzenia.
     *   2. Przed zapytaniem, które nie jest „proste”: metoda inna niż GET/HEAD/POST, Content-Type inny niż text/plain,
     *      application/x-www-form-urlencoded, multipart/form-data (np. application/json), albo własne nagłówki
     *      (X-Shop-Id, Authorization). Preflight to OPTIONS z nagłówkami Origin, Access-Control-Request-Method
     *      i (opcjonalnie) Access-Control-Request-Headers.
     *   3. Serwer odpowie 200 z danymi, ale BEZ Access-Control-Allow-Origin. Przeglądarka zablokuje odczyt — skrypt
     *      dostanie błąd sieci, a w konsoli będzie komunikat o polityce CORS. Sprawdzone testem
     *      noCorsConfigServerStillResponds (odpowiedź serwera; blokadę robi przeglądarka).
     *   4. Aplikacja nie wystartuje: BeanCreationException z przyczyną IllegalArgumentException „When allowCredentials
     *      is true, allowedOrigins cannot contain the special value "*" ...”. Sprawdzone testem
     *      Rest06CorsCredentialsTest.annotationFailsAtStartup. Poprawka: originPatterns z konkretną domeną albo
     *      konkretne origins.
     *   5. Aplikacja wystartuje, ale to GET (i każde inne zapytanie pod /api/shop/**) skończy się 500 — globalna
     *      konfiguracja jest sprawdzana przy każdym zapytaniu do pasującego adresu, także bez Origin. Sprawdzone testem
     *      Rest06CorsCredentialsTest.globalConfigFailsAtRequestTime.
     *   6. exposedHeaders("Location") (w @CrossOrigin: exposedHeaders = "Location"). Bez tego przeglądarka ukrywa przed
     *      skryptem większość nagłówków odpowiedzi.
     *   7. Bo CORS egzekwuje przeglądarka, żeby chronić UŻYTKOWNIKA (jego ciasteczka) przed obcymi stronami. curl nie
     *      jest przeglądarką — wysyła, co chce, i czyta wszystko. Serwer chronią uwierzytelnianie i autoryzacja
     *      (s12_security).
     */
    // </editor-fold>
}

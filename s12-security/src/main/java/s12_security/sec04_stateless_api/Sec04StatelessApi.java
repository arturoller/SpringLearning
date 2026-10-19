package s12_security.sec04_stateless_api;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * <pre>
 * TEMAT: API bez sesji — STATELESS, CSRF (kiedy wolno wyłączyć), CORS w Security, błędy 401/403 jako JSON
 *        (stateless = bezstanowy; session = sesja; CSRF = cross-site request forgery, fałszowanie zapytań
 *        między witrynami; CORS = cross-origin resource sharing, współdzielenie zasobów między pochodzeniami)
 *
 * W SKRÓCIE:
 *   • Przeglądarka z formularzem logowania pamięta zalogowanie w SESJI (ciasteczko JSESSIONID). API dla programów
 *     zwykle jest BEZSTANOWE: każde zapytanie niesie dane logowania (Basic) albo token (Sec05Jwt).
 *   • Jedna aplikacja może mieć kilka łańcuchów filtrów: securityMatcher("/api/**") + @Order — pierwszy pasujący
 *     łańcuch obsługuje zapytanie.
 *   • CSRF chroni użytkownika, którego przeglądarka SAMA dokleja dane logowania (ciasteczko sesji). W Security 7 jest
 *     domyślnie włączone. Wyłączyć wolno tylko tam, gdzie logowanie NIE opiera się na tym, co przeglądarka dokleja
 *     sama — np. API bez sesji z tokenem w nagłówku.
 *   • CORS: przeglądarka pyta serwer (preflight OPTIONS), czy strona z innego pochodzenia może wołać API.
 *     Ze Security trzeba go włączyć w łańcuchu: http.cors(withDefaults()) + bean corsConfigurationSource.
 *   • Odmowy 401/403 powstają w FILTRACH, przed Spring MVC — JSON piszesz w AuthenticationEntryPoint
 *     i AccessDeniedHandler, nie w @RestControllerAdvice.
 *
 * ANALOGIA: hotel.
 *   Gość hotelowy dostaje kartę-klucz (sesja): raz się melduje i potem tylko przykłada kartę. Kurier (API) nie
 *   dostaje karty — przy każdej wizycie pokazuje dowód i list przewozowy (dane logowania w każdym zapytaniu).
 *   CSRF to oszust, który wsuwa gościowi do ręki fałszywe zlecenie „przelej pieniądze” — drzwi otworzą się, bo
 *   gość ma kartę. Dlatego zlecenia od gości muszą mieć tajny podpis (token CSRF). Kuriera to nie dotyczy: nikt
 *   nie przyłoży za niego jego dowodu. CORS to lista firm, których kurierzy mogą wejść tylnym wejściem.
 *
 * JAK TO DZIAŁA:
 *   zapytanie → FilterChainProxy → pierwszy pasujący łańcuch:
 *     /api/**  → apiChain (Order 1): CorsFilter → BasicAuthenticationFilter → ... → AuthorizationFilter
 *                (bez CsrfFilter, bez sesji; odmowy → ProblemDetailSecurityHandler → JSON)
 *     reszta   → webChain (Order 2): CorsFilter → CsrfFilter → formularz /login → ... → AuthorizationFilter
 *                (sesja JSESSIONID, token CSRF w sesji)
 *
 * SŁÓWKA:
 *   security matcher = dopasowanie łańcucha (którego łańcucha dotyczy zapytanie); session creation policy = zasada
 *   tworzenia sesji; stateless = bezstanowy (serwer nic nie pamięta między zapytaniami); cookie = ciasteczko;
 *   token = żeton (losowy, tajny napis); origin = pochodzenie (schemat + host + port, np. http://localhost:5173);
 *   preflight = zapytanie wstępne (OPTIONS przed właściwym zapytaniem); entry point = punkt wejścia;
 *   access denied handler = obsługa odmowy dostępu; problem detail = szczegóły problemu (format błędu, RFC 9457).
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest06Cors (CORS w samym Spring MVC), s06_error_handling/Err04ProblemDetail (ProblemDetail
 *             w kontrolerach), s12_security/Sec01Basics (strona /error i 401 a 403), s12_security/Sec05Jwt (token
 *             w nagłówku zamiast hasła), t28_networking_http/Http02HttpClient (klient HTTP i ciasteczka w Javie).
 * </pre>
 */
public final class Sec04StatelessApi {

    private Sec04StatelessApi() {
    }

    // =================================================================================================
    // 1. DWA ŁAŃCUCHY FILTRÓW — securityMatcher i @Order
    // =================================================================================================
    //
    // Sec04SecurityConfig ma dwa beany SecurityFilterChain:
    //   @Order(1) apiChain: http.securityMatcher("/api/**") ... — tylko dla /api/**
    //   @Order(2) webChain: bez securityMatcher                 — dla całej reszty
    // FilterChainProxy sprawdza łańcuchy w kolejności @Order i bierze PIERWSZY pasujący. Każdy łańcuch ma własne
    // filtry: w apiChain nie ma CsrfFilter, w webChain jest formularz logowania (Sec04StatelessApiTest).
    //
    // securityMatcher ≠ requestMatchers: securityMatcher mówi, KTÓREGO ŁAŃCUCHA dotyczy zapytanie; requestMatchers
    // w authorizeHttpRequests — jakie REGUŁY obowiązują wewnątrz łańcucha.
    //
    // PUŁAPKA: łańcuch „na wszystko” (bez securityMatcher) przed łańcuchem /api/** — aplikacja nie startuje:
    //   UnreachableFilterChainException „A filter chain that matches any request [...] has already been configured,
    //   which means that this filter chain [...] will never get invoked. Please use `HttpSecurity#securityMatcher` to
    //   ensure that there is only one filter chain configured for 'any request' and that the 'any request' filter
    //   chain is published last.” Dlaczego? Drugi łańcuch byłby martwy (Sec04StatelessApiPitfallsTest).

    // =================================================================================================
    // 2. BEZ SESJI — SessionCreationPolicy.STATELESS
    // =================================================================================================
    //
    // Sesja HTTP to pamięć serwera o kliencie, powiązana ciasteczkiem JSESSIONID. W Security 7 samo logowanie Basic
    // NIE zapisuje się w sesji, ale sesję tworzą inne mechanizmy, np.:
    //   • RequestCache (pamięć zapytania) — przy 401/przekierowaniu na /login zapamiętuje adres, „żeby wrócić tam po
    //     zalogowaniu” (dlatego w Sec01 nawet odpowiedź 401 miała Set-Cookie: JSESSIONID),
    //   • formularz logowania — zalogowanie trzyma się w sesji,
    //   • token CSRF — domyślnie leży w sesji (HttpSessionCsrfTokenRepository).
    //
    // http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) w apiChain:
    //   • Security nie tworzy sesji (401 i 200 bez Set-Cookie),
    //   • i NIE czyta zalogowania z istniejącej sesji: ciasteczko z logowania formularzem nie wpuści do /api/orders
    //     (401) — każde zapytanie musi samo przynieść dane logowania (Sec04StatelessApiTest).
    //
    // DOBRA PRAKTYKA: API dla programów — STATELESS. Dlaczego? Brak sesji = brak stanu na serwerze: dowolna kopia
    //   aplikacji obsłuży dowolne zapytanie (łatwe skalowanie), nie ma ciasteczka do ukradzenia ani do nadużycia
    //   przez CSRF.

    // =================================================================================================
    // 3. CSRF — czym jest, kiedy WOLNO wyłączyć, a kiedy NIE
    // =================================================================================================
    //
    // Atak: jesteś zalogowany w sklepie (ciasteczko JSESSIONID w przeglądarce). Wchodzisz na złośliwą stronę, która
    // ma ukryty formularz POST https://sklep.example/web/newsletter i wysyła go skryptem. Przeglądarka SAMA dołącza
    // ciasteczko sklepu — serwer widzi „zalogowanego piotra” i wykonuje zlecenie. Złośliwa strona nie musi znać
    // hasła ani czytać odpowiedzi; wystarczy, że przeglądarka doklei ciasteczko.
    //
    // Obrona Security (domyślnie WŁĄCZONA w Security 7): zapytania zmieniające stan (wszystko poza GET, HEAD, TRACE,
    // OPTIONS) muszą nieść tajny token CSRF — w polu _csrf formularza albo w nagłówku X-CSRF-TOKEN. Token leży
    // w sesji; obca strona go nie zna (nie może czytać stron sklepu). Szczegóły Security 7: token jest maskowany
    // (XorCsrfTokenRequestAttributeHandler — przy każdym wyświetleniu inny zapis tego samego tokenu) i ZMIENIA SIĘ
    // po zalogowaniu (token z formularza /login jest potem nieważny — Sec04StatelessApiTest).
    //   POST /login bez _csrf → 403; POST /web/newsletter bez tokenu → 403; z X-CSRF-TOKEN → 200.
    //
    // KIEDY WOLNO wyłączyć (http.csrf(AbstractHttpConfigurer::disable)): gdy logowanie NIE opiera się na niczym, co
    //   przeglądarka dokleja SAMA — czyli API bez sesji, w którym dane logowania/token w nagłówku Authorization dodaje
    //   klient programowo (aplikacja mobilna, inny serwer, frontend wysyłający token JWT — Sec05Jwt). Obca strona nie
    //   potrafi doczepić takiego nagłówka do Twojego zapytania. Tak jest w apiChain.
    //
    // PUŁAPKA: NIE wyłączaj CSRF w aplikacji z ciasteczkiem sesji (formLogin, logowanie przez przeglądarkę). Dlaczego?
    //   To dokładnie sytuacja, przed którą CSRF chroni. Uwaga też na HTTP Basic używane Z PRZEGLĄDARKI: przeglądarka
    //   zapamiętuje hasło Basic i dokleja je sama do kolejnych zapytań — to też „dane doklejane automatycznie”.
    //   Dlatego apiChain zakłada klientów-programy, nie przeglądarkę.
    //
    // DOBRA PRAKTYKA: frontend SPA (np. React) z logowaniem przez sesję: zamiast wyłączać CSRF — http.csrf(c -> c.spa())
    //   (nowość Security 7.0): token w ciasteczku XSRF-TOKEN, które skrypt strony czyta i odsyła w nagłówku.
    //
    // PUŁAPKA: GET, który ZMIENIA dane (np. GET /web/unsubscribe), nie jest chroniony — CSRF celowo pomija GET.
    //   Dlatego zmiany stanu tylko przez POST/PUT/PATCH/DELETE (s04_web_rest/Rest01Controllers).

    // =================================================================================================
    // 4. CORS w Security — http.cors(...) i bean corsConfigurationSource
    // =================================================================================================
    //
    // Przeglądarka blokuje skryptowi ze strony http://localhost:5173 czytanie odpowiedzi z http://localhost:8080
    // (inne pochodzenie = inny port). Zanim wyśle POST z nagłówkiem Authorization, wysyła PREFLIGHT:
    //   OPTIONS /api/orders, Origin: http://localhost:5173, Access-Control-Request-Method: POST — BEZ danych logowania.
    // Serwer odpowiada nagłówkami Access-Control-Allow-*, a przeglądarka decyduje, czy wysłać właściwe zapytanie.
    //
    //   http.cors(Customizer.withDefaults());   // użyj beana o nazwie corsConfigurationSource (a gdy go nie ma —
    //                                           // konfiguracji CORS ze Spring MVC)
    // Efekt (Sec04StatelessApiTest): preflight z localhost:5173 → 200 + Access-Control-Allow-Origin, -Methods,
    // -Headers, -Max-Age; z http://zly.example → 403 i treść „Invalid CORS request”.
    // Ciekawostka sprawdzona testem: w Security 7.1, jeśli w kontekście jest bean UrlBasedCorsConfigurationSource,
    // Security SAM dokłada http.cors(withDefaults()) do każdego łańcucha — webChain też ma CorsFilter.
    //
    // PUŁAPKA: CORS skonfigurowany tylko w Spring MVC (WebMvcConfigurer.addCorsMappings, @CrossOrigin —
    //   s04_web_rest/Rest06Cors), a łańcuch bez http.cors(...) → preflight dostaje 401. Dlaczego? Preflight nie ma
    //   danych logowania, a filtr autoryzacji zatrzymuje go, zanim dotrze do Spring MVC, który znałby reguły CORS
    //   (Sec04StatelessApiPitfallsTest). Rozwiązanie: http.cors(withDefaults()) — wtedy Security użyje reguł z MVC.
    //
    // PUŁAPKA: CORS nie jest zabezpieczeniem serwera. To przeglądarka pilnuje reguł; curl, Postman czy inny serwer
    //   je ignorują. CORS chroni UŻYTKOWNIKÓW przeglądarki, a dostęp do API i tak muszą pilnować reguły autoryzacji.

    // =================================================================================================
    // 5. 401 i 403 jako JSON — AuthenticationEntryPoint i AccessDeniedHandler
    // =================================================================================================
    //
    // Odmowa powstaje w filtrach (ExceptionTranslationFilter, BasicAuthenticationFilter) — Spring MVC jeszcze nie
    // działa, więc @RestControllerAdvice/@ExceptionHandler (s06_error_handling/Err03ControllerAdvice) jej NIE widzi.
    // Dlatego ProblemDetailSecurityHandler implementuje dwa interfejsy Security:
    //   AuthenticationEntryPoint.commence(...) — niezalogowany → 401
    //   AccessDeniedHandler.handle(...)        — zalogowany bez uprawnień → 403
    // i pisze ProblemDetail tym samym konwerterem, co Spring MVC. Odpowiedź (Sec04StatelessApiTest):
    //   401, Content-Type: application/problem+json, WWW-Authenticate: Basic realm="sklep-api",
    //   {"detail":"Brak lub złe dane logowania.","instance":"/api/orders","status":401,"title":"Unauthorized"}
    // (brak pola type oznacza wg RFC 9457 domyślne „about:blank”).
    //
    // PUŁAPKA: samo http.exceptionHandling(ex -> ex.authenticationEntryPoint(...)) nie obejmuje ZŁEGO HASŁA.
    //   Dlaczego? Złe hasło odrzuca BasicAuthenticationFilter, który ma WŁASNY punkt wejścia (domyślnie
    //   BasicAuthenticationEntryPoint): odpowiedź ma WWW-Authenticate: Basic realm="Realm", charset="UTF-8" i ciało ze
    //   strony /error Boota — zwykły JSON, nie ProblemDetail (Sec04StatelessApiPitfallsTest, z publicznym /error).
    //   Trzeba go podać też w http.httpBasic(basic -> basic.authenticationEntryPoint(...)). Gdy /error jest chronione,
    //   zapytanie o stronę błędu przechodzi przez łańcuch drugi raz i trafia do Twojego punktu wejścia — działa to
    //   „przez przypadek” i łatwo to zepsuć.
    //
    // DOBRA PRAKTYKA: w treści błędu nie zdradzaj szczegółów („nie ma takiego loginu”, „konto zablokowane”).
    //   Dlaczego? Atakujący dowiedziałby się, które loginy istnieją. Wystarczy status i ogólny opis.
    //   Gdy JSON nie jest potrzebny, prosty HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED) zwraca samo 401.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Kilka łańcuchów: http.securityMatcher("/api/**") + @Order(1); łańcuch „na wszystko” — ostatni.
     *   • API bez sesji: http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) —
     *     brak Set-Cookie, sesja ignorowana; dane logowania/token w KAŻDYM zapytaniu.
     *   • CSRF domyślnie włączone; chroni POST/PUT/PATCH/DELETE; token w _csrf albo X-CSRF-TOKEN; zmienia się po
     *     zalogowaniu. Wyłącz (csrf(AbstractHttpConfigurer::disable)) TYLKO gdy brak ciasteczek/danych doklejanych
     *     przez przeglądarkę. SPA z sesją: csrf(c -> c.spa()).
     *   • CORS: http.cors(withDefaults()) + bean corsConfigurationSource (albo reguły z MVC); bez tego preflight → 401.
     *   • 401/403 jako JSON: AuthenticationEntryPoint + AccessDeniedHandler (exceptionHandling) + entry point
     *     w httpBasic(...) dla złego hasła; @RestControllerAdvice tu nie działa.
     *
     * PYTANIA KONTROLNE:
     *   1. Po co dwa łańcuchy w jednej aplikacji i skąd FilterChainProxy wie, którego użyć?
     *   2. Co się stanie? Zalogujesz się formularzem (/login), a potem z tym samym ciasteczkiem JSESSIONID wywołasz
     *      GET /api/orders bez nagłówka Authorization.
     *   3. Dlaczego atak CSRF działa na aplikację z ciasteczkiem sesji, a nie działa na API z tokenem w nagłówku
     *      Authorization?
     *   4. ZNAJDŹ BŁĄD: aplikacja dla przeglądarki z formLogin, programista dodał http.csrf(AbstractHttpConfigurer::disable),
     *      „bo POST-y dawały 403”.
     *   5. Co się stanie? Konfiguracja CORS jest tylko w WebMvcConfigurer (jak w Rest06Cors), a łańcuch filtrów nie ma
     *      http.cors(...). Frontend z http://localhost:5173 wysyła preflight do /api/orders.
     *   6. Dlaczego @RestControllerAdvice nie zamieni odpowiedzi 401 z filtra na JSON?
     *   7. ZNAJDŹ BŁĄD: w łańcuchu (z publicznym /error) jest exceptionHandling(ex -> ex.authenticationEntryPoint(json)),
     *      a mimo to ZŁE hasło zwraca 401 z nagłówkiem Basic realm="Realm" i zwykłym JSON-em Boota zamiast ProblemDetail.
     *   8. Co się stanie? Łańcuch webChain (bez securityMatcher) ma @Order(1), a apiChain z securityMatcher("/api/**") —
     *      @Order(2).
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Sec04StatelessApiExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): czy zapytanie tą metodą HTTP wymaga tokenu CSRF w Security 7? Bez tokenu przechodzą tylko
     * GET, HEAD, TRACE i OPTIONS (wielkość liter nie ma znaczenia: "get" = "GET"). Pozostałe → true.
     * Podpowiedź: Set.of(...) i toUpperCase(Locale.ROOT).
     */
    static boolean exercise1(String httpMethod) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj CorsConfigurationSource: dla /api/** dozwolone pochodzenie
     * "https://sklep.example", metody GET i POST, nagłówki Authorization i Content-Type, max age 1800 s.
     * Inne ścieżki — bez konfiguracji CORS.
     * Podpowiedź: Sec04SecurityConfig.corsConfigurationSource().
     */
    static CorsConfigurationSource exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): bezstanowy łańcuch dla kontrolera ApiController: GET /api/products — wszyscy, reszta —
     * zalogowani; HTTP Basic; brak sesji; CSRF wyłączone. Oczekiwane: POST /api/orders z Basic bez tokenu CSRF → 201;
     * niezalogowany GET /api/orders → 401 BEZ nagłówka Set-Cookie.
     * Podpowiedź: Sec04SecurityConfig.apiChain (bez securityMatcher — w teście to jedyny łańcuch).
     */
    static SecurityFilterChain exercise3(HttpSecurity http) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // ĆWICZENIE 4 (trudniejsze): klasa ExerciseJsonEntryPoint w tym pakiecie — punkt wejścia z odpowiedzią JSON.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo API i strony dla przeglądarki potrzebują innych zasad: API — bez sesji, bez CSRF, błędy w JSON; strony —
     *      formularz, sesja, CSRF. FilterChainProxy sprawdza łańcuchy w kolejności @Order i bierze pierwszy, którego
     *      securityMatcher pasuje do zapytania (łańcuch bez securityMatcher pasuje do wszystkiego).
     *   2. 401 (ProblemDetail w JSON). apiChain jest STATELESS: nie czyta zalogowania z sesji, więc ciasteczko nic nie
     *      daje. Sprawdzone testem Sec04StatelessApiTest.statelessApiIgnoresSession.
     *   3. Przeglądarka sama dołącza ciasteczko sesji do każdego zapytania do sklepu — także wysłanego ze złośliwej
     *      strony. Nagłówka Authorization z tokenem przeglądarka sama nie dokleja; dodaje go kod klienta, którego obca
     *      strona nie kontroluje. Brak automatycznie doklejanych danych = brak ataku CSRF.
     *   4. Wyłączenie CSRF w aplikacji z sesją w ciasteczku otwiera ją na atak CSRF. Poprawnie: zostawić CSRF
     *      i wysyłać token (ukryte pole _csrf w formularzu, nagłówek X-CSRF-TOKEN, dla SPA csrf(c -> c.spa())).
     *      403 przy POST bez tokenu to ochrona, nie błąd (Sec04StatelessApiTest.webPostNeedsCsrfToken).
     *   5. Preflight dostanie 401: nie ma danych logowania, a łańcuch bez http.cors(...) nie przepuszcza go do Spring MVC.
     *      Przeglądarka zablokuje właściwe zapytanie. Sprawdzone testem
     *      Sec04StatelessApiPitfallsTest.corsMustBeEnabledInSecurity (bez cors() 401, z cors() 200).
     *   6. Odmowę zgłasza filtr Security, zanim zapytanie dotrze do DispatcherServlet — @RestControllerAdvice obsługuje
     *      tylko wyjątki z kontrolerów. JSON trzeba napisać w AuthenticationEntryPoint/AccessDeniedHandler.
     *   7. Złe hasło obsługuje BasicAuthenticationFilter i jego własny punkt wejścia (BasicAuthenticationEntryPoint:
     *      WWW-Authenticate: Basic realm="Realm", charset="UTF-8" i zwykły JSON strony /error). Trzeba go podmienić także
     *      w http.httpBasic(basic -> basic.authenticationEntryPoint(json)). Sprawdzone testem
     *      Sec04StatelessApiPitfallsTest.wrongPasswordUsesBasicEntryPoint; poprawną wersję (Sec04SecurityConfig.apiChain)
     *      sprawdza Sec04StatelessApiTest.problemDetailErrors.
     *   8. Aplikacja nie wystartuje — UnreachableFilterChainException („A filter chain that matches any request [...]
     *      will never get invoked...”), bo łańcuch dla /api/** nigdy by nie zadziałał. Sprawdzone testem
     *      Sec04StatelessApiPitfallsTest.catchAllChainMustBeLast.
     */
    // </editor-fold>
}

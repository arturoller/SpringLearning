package s12_security.sec04_stateless_api;

/**
 * <pre>
 * TEMAT: API bez stanu (stateless) — sesja kontra nagłówek Authorization, CSRF (kiedy włączony, kiedy wolno wyłączyć),
 *        CORS w Spring Security, błędy 401/403 jako ProblemDetail, nagłówki bezpieczeństwa
 *        (stateless = bezstanowy: serwer nic nie pamięta między żądaniami; session = sesja; CSRF — Cross-Site Request
 *         Forgery = fałszowanie żądań między stronami; CORS — Cross-Origin Resource Sharing = współdzielenie zasobów między
 *         pochodzeniami; entry point = punkt wejścia; access denied handler = obsługa odmowy dostępu)
 *
 * W SKRÓCIE:
 *   Aplikacja dla przeglądarki loguje Cię RAZ i zapamiętuje w SESJI — przeglądarka odsyła potem ciasteczko JSESSIONID sama,
 *   przy każdym żądaniu. Ta wygoda ma cenę: przeglądarka odeśle ciasteczko także wtedy, gdy żądanie wysłała OBCA strona.
 *   To jest atak CSRF, a ochrona CSRF (token, którego obca strona nie zna) jest w Spring Security domyślnie WŁĄCZONA.
 *   API dla programów (aplikacja mobilna, inny serwer, SPA z tokenem) zwykle działa inaczej: każde żądanie niesie dane
 *   logowania w nagłówku Authorization, a serwer nie trzyma sesji ({@code SessionCreationPolicy.STATELESS}). Nagłówka
 *   przeglądarka sama nie dokłada — dlatego w takim API CSRF wolno wyłączyć. Do tego: CORS musi być ustawiony W SPRING
 *   SECURITY (preflight przychodzi bez logowania), a błędy 401/403 zamieniamy w ProblemDetail w filtrach, bo
 *   {@code @RestControllerAdvice} ich nie widzi. Domyślne nagłówki bezpieczeństwa Security dodaje sam.
 *
 * ANALOGIA: klub z opaską i sklep z biletem jednorazowym.
 *   Klub (sesja): przy wejściu dostajesz opaskę na rękę (ciasteczko). Barman podaje drinka każdemu z opaską — nawet gdy
 *   zamówienie złożył za Ciebie obcy, który pchnął Twoją rękę w stronę baru (CSRF). Dlatego barman prosi jeszcze o hasło dnia
 *   wypisane na Twoim stoliku (token CSRF) — obcy go nie zna. Sklep (API bezstanowe): przy każdym zakupie pokazujesz dowód
 *   (nagłówek Authorization). Nikt nie "pamięta" Cię między zakupami, więc nie ma czego podrobić "przy okazji".
 *
 * JAK TO DZIAŁA:
 *   1. {@code FilterChainProxy} wybiera PIERWSZY łańcuch (według {@code @Order}), którego {@code securityMatcher} pasuje:
 *      {@code /sec04/api/**} → apiChain (bez sesji), wszystko inne → browserChain (z sesją).
 *   2. STATELESS: {@code SecurityContextHolderFilter} nie czyta kontekstu z sesji, nikt nie zapisuje go do sesji, a pamięć
 *      żądań ({@code RequestCache}) jest wyłączona — więc nawet 401 nie zakłada sesji.
 *   3. {@code CsrfFilter} (przed logowaniem!) przepuszcza GET/HEAD/TRACE/OPTIONS, a dla POST/PUT/PATCH/DELETE porównuje token
 *      z żądania (pole {@code _csrf} albo nagłówek {@code X-CSRF-TOKEN}) z tokenem zapisanym w sesji. Brak/zły → 403.
 *   4. {@code CorsFilter} (też przed logowaniem) odpowiada na preflight OPTIONS sam i nie puszcza go dalej.
 *   5. Odmowy z filtrów obsługuje {@code ExceptionTranslationFilter}: anonim → {@code AuthenticationEntryPoint} (401),
 *      zalogowany → {@code AccessDeniedHandler} (403). Złe hasło Basic obsługuje sam {@code BasicAuthenticationFilter}
 *      swoim punktem wejścia.
 *   6. {@code HeaderWriterFilter} dopisuje do KAŻDEJ odpowiedzi nagłówki bezpieczeństwa.
 *
 * SŁÓWKA:
 *   stateless / stateful = bez stanu / ze stanem; session = sesja; cookie = ciasteczko; session creation policy = zasada
 *   tworzenia sesji; request cache = pamięć żądań (zapamiętany adres "na po zalogowaniu"); forgery = fałszerstwo; cross-site =
 *   między stronami; token = żeton, losowy napis; SPA — Single Page Application = aplikacja jednej strony (JavaScript
 *   w przeglądarce); origin = pochodzenie (schemat + host + port); preflight = zapytanie wstępne; allowed = dozwolony;
 *   entry point = punkt wejścia; handler = obsługa; header = nagłówek; nosniff = "nie zgaduj typu"; frame = ramka;
 *   HSTS — HTTP Strict Transport Security = ścisłe wymuszanie HTTPS; security matcher = dopasowanie łańcucha.
 *
 * ZOBACZ TEŻ: s12_security/Sec01Basics (łańcuch filtrów, 401 kontra 403, WWW-Authenticate),
 *             s12_security/Sec02Authorization (reguły adresów, odmowa z @PreAuthorize i advice "łap wszystko"),
 *             s12_security/Sec05Jwt (API bezstanowe z tokenem JWT zamiast hasła w każdym żądaniu),
 *             s04_web_rest/Rest06Cors (CORS bez Spring Security, preflight),
 *             s06_error_handling/Err04ProblemDetail (format ProblemDetail), s06_error_handling/Err03ControllerAdvice.
 * </pre>
 */
public final class Sec04StatelessApi {

    private Sec04StatelessApi() {
    }

    // =================================================================================================
    // 1. SESJA KONTRA API BEZ STANU: sessionCreationPolicy(STATELESS)
    //    (Sec04SecurityConfig, Sec04SessionTest, Sec04StatelessApiTest.statelessCreatesNoSession)
    // =================================================================================================
    //
    // Ta lekcja ma DWA łańcuchy w jednej aplikacji (Sec04SecurityConfig):
    //
    //     @Bean @Order(1)
    //     SecurityFilterChain apiChain(HttpSecurity http, …) {
    //         http.securityMatcher("/sec04/api/**")                  // TEN łańcuch tylko dla API
    //             …
    //             .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    //     }
    //
    //     @Bean @Order(2)
    //     SecurityFilterChain browserChain(HttpSecurity http) {      // bez securityMatcher = wszystko inne
    //         … .formLogin(Customizer.withDefaults()).httpBasic(Customizer.withDefaults());
    //     }
    //
    // Co widać na prawdziwym serwerze (Sec04SessionTest, RANDOM_PORT):
    //
    //     | żądanie                                          | łańcuch | status | Set-Cookie: JSESSIONID |
    //     |--------------------------------------------------|---------|--------|------------------------|
    //     | GET /sec04/api/orders + Basic anna               | API     | 200    | NIE                    |
    //     | GET /sec04/api/orders bez logowania              | API     | 401    | NIE                    |
    //     | GET /sec04/web/profile bez logowania             | web     | 401    | TAK (pamięć żądań)     |
    //     | POST /login (formularz) anna                     | web     | 302    | TAK (nowa sesja)       |
    //     | GET /sec04/web/profile z samym ciasteczkiem      | web     | 200    | —                      |
    //     | GET /sec04/api/orders z tym samym ciasteczkiem   | API     | 401    | NIE (API sesji nie czyta) |
    //
    //   • Już 401 w łańcuchu z sesją zakłada sesję: Spring zapamiętuje adres ({@code RequestCache}), żeby po zalogowaniu
    //     odesłać Cię tam z powrotem. W STATELESS pamięć żądań jest wyłączona — 401 bez ciasteczka.
    //   • Po zalogowaniu formularzem Spring zmienia identyfikator sesji (ochrona przed "session fixation" — przejęciem sesji,
    //     której identyfikator podsunął napastnik).
    //   • Ciekawostka sprawdzona testem basicDoesNotCreateSession: w Security 7 samo HTTP Basic NIE zapisuje logowania w sesji,
    //     nawet w łańcuchu z sesją (200 bez Set-Cookie). Sesję zakłada logowanie formularzem.
    //
    // Wartości {@code SessionCreationPolicy}: {@code ALWAYS} (zawsze), {@code IF_REQUIRED} (domyślna — gdy potrzebna),
    // {@code NEVER} (sam nie tworzy, ale użyje istniejącej), {@code STATELESS} (nie tworzy i nie używa).
    //
    // DOBRA PRAKTYKA: API dla programów — STATELESS. Dlaczego? Każda instancja serwera obsłuży każde żądanie (nie trzeba
    //   współdzielić sesji między serwerami — skalowanie, s19), nie ma ciasteczka do podrobienia (CSRF, sekcja 2), a pamięć
    //   serwera nie rośnie z liczbą klientów.
    //
    // PUŁAPKA: "STATELESS" nie znaczy "bezpieczne". Dlaczego? Hasło w nagłówku Basic jedzie przy KAŻDYM żądaniu (Base64 to tylko
    //   inny zapis tekstu, nie szyfr). Bez HTTPS każdy po drodze je przeczyta. Dlatego API wymaga HTTPS, a zamiast hasła w każdym
    //   żądaniu woli krótko żyjący token (Sec05Jwt).

    // =================================================================================================
    // 2. CSRF: czym jest atak, dlaczego ochrona jest domyślnie włączona, kiedy wolno ją wyłączyć
    //    (Sec04SessionTest.csrfAttackIsBlocked, Sec04StatelessApiTest.csrfProtectsStateChangingRequests,
    //     csrfCheckedBeforeAuthentication, apiWithoutCsrf, Sec04SpaCsrfTest)
    // =================================================================================================
    //
    // Atak krok po kroku:
    //   1. Anna loguje się w sklepie (http://sklep.example). Przeglądarka dostaje ciasteczko JSESSIONID.
    //   2. W innej karcie anna otwiera stronę napastnika (http://zly.example). Strona ma ukryty formularz:
    //
    //          <form action="http://sklep.example/sec04/web/email" method="post">
    //            <input name="email" value="napastnik@zly.example">
    //          </form>
    //          <script>document.forms[0].submit()</script>
    //
    //   3. Przeglądarka wysyła POST do sklepu i SAMA dokłada ciasteczko anny (ciasteczka należą do adresu docelowego,
    //      a nie do strony, która wysyła formularz). Tak samo sama dokłada zapamiętane dane logowania Basic, gdy anna wpisała
    //      je wcześniej w okienku przeglądarki.
    //   4. Bez ochrony serwer widzi zalogowaną annę i zmienia jej e-mail. Napastnik "resetuje" hasło na swój adres.
    //
    // Obrona: token CSRF — losowy napis, który serwer zapisuje w sesji i wkłada do SWOICH formularzy (ukryte pole {@code _csrf}).
    // Obca strona nie może przeczytać naszej strony (same-origin policy, s04_web_rest/Rest06Cors), więc nie zna tokenu.
    //
    //     | żądanie do łańcucha z sesją                         | wynik |
    //     |-----------------------------------------------------|-------|
    //     | POST z ciasteczkiem, bez tokenu (atak)              | 403   |
    //     | POST z poprawnym tokenem ({@code with(csrf())})     | 200   |
    //     | POST z fałszywym tokenem ({@code csrf().useInvalidToken()}) | 403 |
    //     | GET (tylko odczyt) bez tokenu                       | 200   |
    //     | POST anonima bez tokenu                             | 403 — nie 401! CsrfFilter stoi PRZED logowaniem |
    //
    // Dlaczego GET nie potrzebuje tokenu? Bo GET z definicji niczego nie zmienia (HTTP: metody "bezpieczne" — GET, HEAD,
    // TRACE, OPTIONS). Wniosek: NIGDY nie zmieniaj danych w GET (np. {@code GET /delete?id=5}) — ominiesz ochronę CSRF.
    //
    // Kiedy wolno wyłączyć CSRF ({@code .csrf(AbstractHttpConfigurer::disable)}, jak w apiChain)?
    //   • API NIE używa ciasteczek ani logowania, które przeglądarka dołącza sama (sesja, Basic z okienka przeglądarki),
    //   • a dane logowania przychodzą w nagłówku, który program dokłada świadomie ({@code Authorization: Bearer …}).
    //   Obca strona nie ustawi nagłówka Authorization z tokenem anny — nie zna go. Nie ma czego "podrobić przy okazji".
    //
    // DOBRA PRAKTYKA: zostaw CSRF włączony wszędzie tam, gdzie przeglądarka może sama dołączyć poświadczenia (sesja, ciasteczko
    //   z tokenem, Basic). Dlaczego? Wyłączenie "bo przeszkadza w Postmanie" otwiera atak na prawdziwych użytkowników. W testach
    //   dodaj token ({@code with(csrf())}, Sec06SecurityTesting), zamiast wyłączać ochronę.
    //
    // PUŁAPKA: token w ciasteczku zamiast w nagłówku Authorization (np. JWT w ciasteczku, Sec05Jwt). Dlaczego? Ciasteczko
    //   przeglądarka dołącza sama — wracasz do sytuacji z sesją i CSRF MUSI zostać włączony.
    //
    // SPA (aplikacja w JavaScripcie) z sesją: {@code .csrf(csrf -> csrf.spa())} (gotowe w Security 7.1; test Sec04SpaCsrfTest).
    //   Serwer wysyła token w ciasteczku {@code XSRF-TOKEN} BEZ HttpOnly (JavaScript musi je przeczytać), a skrypt odsyła go
    //   w nagłówku {@code X-XSRF-TOKEN}. POST z samym ciasteczkiem (jak z obcej strony) → 403. W starszych poradnikach zobaczysz
    //   ręczne {@code CookieCsrfTokenRepository.withHttpOnlyFalse()} + własny filtr — {@code spa()} robi to za Ciebie.
    //
    // PYTANIE REKRUTACYJNE: "Czym jest CSRF i kiedy można wyłączyć ochronę?" — atak, w którym obca strona wysyła żądanie
    //   w imieniu zalogowanego użytkownika, bo przeglądarka sama dołącza ciasteczka. Wyłączyć można, gdy uwierzytelnianie
    //   nie opiera się na niczym, co przeglądarka dołącza automatycznie (bezstanowe API z nagłówkiem Authorization).

    // =================================================================================================
    // 3. CORS W SPRING SECURITY: http.cors(...) + bean CorsConfigurationSource
    //    (Sec04SecurityConfig.corsConfigurationSource, Sec04StatelessApiTest.preflightAllowed, preflightRejected,
    //     actualCorsRequest, sec04_pitfalls: Sec04NoCorsPitfallTest)
    // =================================================================================================
    //
    // Przypomnienie (s04_web_rest/Rest06Cors): przed "nie-prostym" żądaniem z innego pochodzenia przeglądarka wysyła PREFLIGHT —
    // {@code OPTIONS} z nagłówkami {@code Origin} i {@code Access-Control-Request-Method}. Preflight NIGDY nie niesie
    // nagłówka Authorization ani ciasteczek. Dla Spring Security to więc żądanie ANONIMA.
    //
    //     @Bean
    //     UrlBasedCorsConfigurationSource corsConfigurationSource() {     // nazwa beana ma znaczenie!
    //         CorsConfiguration api = new CorsConfiguration();
    //         api.setAllowedOrigins(List.of("http://localhost:3000"));
    //         api.setAllowedMethods(List.of("GET", "POST"));
    //         api.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    //         api.setMaxAge(Duration.ofHours(1));
    //         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    //         source.registerCorsConfiguration("/sec04/api/**", api);
    //         return source;
    //     }
    //     …
    //     http.cors(Customizer.withDefaults())       // CorsFilter w łańcuchu, PRZED filtrami logowania
    //
    //   • Preflight z http://localhost:3000 bez logowania → 200 + {@code Access-Control-Allow-Origin}, {@code -Methods: GET,POST},
    //     {@code -Max-Age: 3600} (test preflightAllowed). Obce pochodzenie albo metoda DELETE → 403 "Invalid CORS request".
    //   • Właściwe żądanie GET z Origin: CORS dopisuje nagłówek, ale logowanie dalej obowiązuje (bez Authorization → 401).
    //     CORS to NIE uwierzytelnianie — mówi tylko przeglądarce, czy skrypt może przeczytać odpowiedź.
    //   • Skąd {@code cors(withDefaults())} bierze reguły (sprawdzone w {@code CorsConfigurer} 7.1)? Najpierw bean o nazwie
    //     {@code corsConfigurationSource}; gdy go nie ma — reguły ze Spring MVC ({@code @CrossOrigin},
    //     {@code addCorsMappings}). Do tego Security 7 sam dopisuje {@code cors()} do łańcuchów, gdy w kontekście jest bean typu
    //     {@code UrlBasedCorsConfigurationSource} — nie licz na to, pisz {@code cors(...)} jawnie (czytelniej).
    //
    // PUŁAPKA: CORS ustawiony tylko w Spring MVC (jak w s04_web_rest/Rest06Cors), a łańcuch bez {@code http.cors(...)}.
    //   Dlaczego? Filtry bezpieczeństwa działają PRZED DispatcherServlet: preflight bez logowania dostaje 401 i reguła z MVC nie
    //   zostanie w ogóle przeczytana (test preflightRejectedBySecurity). Przeglądarka pokaże "CORS error", a curl i Postman
    //   (które nie robią preflight) działają — więc błąd "nie istnieje" (test actualRequestLooksFine). Sprawdziliśmy też
    //   naprawę: sama linijka {@code .cors(Customizer.withDefaults())} w tej aplikacji daje 200, bo bierze reguły z MVC.
    //
    // DOBRA PRAKTYKA: wypisz dozwolone pochodzenia dokładnie ({@code setAllowedOrigins(List.of(…))}), nie {@code "*"}.
    //   Dlaczego? {@code "*"} pozwala czytać odpowiedzi skryptom z KAŻDEJ strony, a z {@code allowCredentials(true)} Spring
    //   w ogóle odrzuci taką konfigurację (Rest06Cors).

    // =================================================================================================
    // 4. BŁĘDY 401/403 JAKO ProblemDetail: authenticationEntryPoint i accessDeniedHandler
    //    (ProblemDetailSecurityHandlers, Sec04StatelessApiTest.unauthorizedAsProblemDetail, forbiddenAsProblemDetail,
    //     sec04_pitfalls: Sec04AdvicePitfallTest, Sec04BasicEntryPointPitfallTest)
    // =================================================================================================
    //
    // Domyślnie 401/403 z filtrów mają w MockMvc PUSTE ciało (na prawdziwym serwerze Tomcat przekazuje je dalej na {@code /error}
    // i Boot dokleja swój JSON — Sec01Basics). Klient API wolałby ProblemDetail (s06_error_handling/Err04ProblemDetail):
    //
    //     public class ProblemDetailSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    //         public void commence(request, response, AuthenticationException e) {   // "nie wiem, kim jesteś" → 401
    //             response.setHeader("WWW-Authenticate", "Basic realm=\"sklep\"");
    //             write(response, …, 401, "Wymagane logowanie", …);                    // status + problem+json
    //         }
    //         public void handle(request, response, AccessDeniedException e) {       // "nie wolno ci" → 403
    //             write(response, …, 403, "Brak uprawnień", …);
    //         }
    //     }
    //     …
    //     .httpBasic(basic -> basic.authenticationEntryPoint(problemHandlers))        // złe hasło
    //     .exceptionHandling(errors -> errors
    //             .authenticationEntryPoint(problemHandlers)                          // brak logowania
    //             .accessDeniedHandler(problemHandlers))                              // brak uprawnień
    //
    //   Wynik: {@code {"detail":"…","instance":"/sec04/api/orders","status":401,"title":"Wymagane logowanie"}}, typ
    //   {@code application/problem+json}, nagłówek {@code WWW-Authenticate: Basic realm="sklep"}. Łańcuch przeglądarki nie ma
    //   tych ustawień — tam dalej domyślne 401 z {@code Basic realm="Realm", charset="UTF-8"} (test forbiddenAsProblemDetail).
    //
    // PUŁAPKA: {@code @RestControllerAdvice} z {@code @ExceptionHandler(AuthenticationException.class)} albo
    //   {@code AccessDeniedException.class} "dla ładnych błędów". Dlaczego nie działa? Odmowa z reguł ADRESÓW zapada w filtrze
    //   ({@code AuthorizationFilter}), a advice działa wewnątrz DispatcherServlet — żądanie tam nigdy nie dociera. Test
    //   adviceDoesNotSeeFilterErrors: 401 i 403 z pustym ciałem, advice wywołany 0 razy. (Odmowa z {@code @PreAuthorize} w
    //   kontrolerze DOCIERA do advice — i "łap wszystko" robi z niej 500, Sec02Authorization, sekcja 3.)
    //
    // PUŁAPKA: punkt wejścia tylko w {@code exceptionHandling}. Dlaczego za mało? Brak logowania obsługuje
    //   {@code ExceptionTranslationFilter} (nasz JSON), ale ZŁE HASŁO odrzuca sam {@code BasicAuthenticationFilter} swoim
    //   domyślnym punktem wejścia: 401 z pustym ciałem i {@code realm="Realm"} (test wrongPasswordSkipsCustomEntryPoint).
    //   Ustaw go także w {@code httpBasic(basic -> basic.authenticationEntryPoint(…))}.
    //
    // DOBRA PRAKTYKA: w odpowiedzi błędu nie zwracaj {@code exception.getMessage()}. Dlaczego? Komunikaty techniczne ("Bad
    //   credentials", "User is disabled", nazwy klas) pomagają napastnikowi (wyliczanie kont — Sec03UsersPasswords). Klient
    //   dostaje ogólny tytuł, szczegóły idą do logów. I zawsze ustaw {@code WWW-Authenticate} przy 401 — wymaga tego HTTP.

    // =================================================================================================
    // 5. NAGŁÓWKI BEZPIECZEŃSTWA (Sec04StatelessApiTest.securityHeaders)
    // =================================================================================================
    //
    // {@code http.headers(...)} jest włączone domyślnie. Każda odpowiedź (także 200 z publicznego katalogu) dostaje:
    //
    //     | nagłówek                                    | wartość                                       | po co                          |
    //     |---------------------------------------------|-----------------------------------------------|--------------------------------|
    //     | X-Content-Type-Options                      | nosniff                                       | przeglądarka nie "zgaduje" typu pliku (np. JSON jako skrypt) |
    //     | X-Frame-Options                             | DENY                                          | strony nie da się osadzić w ramce na obcej stronie (clickjacking — "porywanie kliknięć") |
    //     | Cache-Control / Pragma / Expires            | no-cache, no-store, max-age=0, must-revalidate / no-cache / 0 | dane po zalogowaniu nie zostają w pamięci przeglądarki ani pośredników |
    //     | X-XSS-Protection                            | 0                                             | wyłącza stary, dziurawy filtr XSS przeglądarek (zalecenie OWASP) |
    //     | Strict-Transport-Security (HSTS)            | max-age=31536000 ; includeSubDomains          | TYLKO przy HTTPS: "przez rok łącz się ze mną wyłącznie po HTTPS" |
    //
    //   HSTS przez zwykłe HTTP nie ma sensu (napastnik w środku mógłby go usunąć), więc Spring wysyła go tylko na żądania
    //   bezpieczne — w teście {@code .secure(true)}. Własna polityka CSP (Content Security Policy — skąd wolno ładować skrypty):
    //   {@code http.headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'")))} — dla stron HTML.
    //
    // PUŁAPKA: {@code headers(h -> h.disable())}, "bo przeszkadza w osadzeniu strony". Dlaczego? Wyłączasz wszystkie ochrony
    //   naraz. Zmień jeden nagłówek: {@code h.frameOptions(f -> f.sameOrigin())} (ramki tylko z tej samej strony, np. konsola H2).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • API: .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) — brak sesji i JSESSIONID,
     *     także przy 401. Domyślnie (IF_REQUIRED) nawet 401 zakłada sesję (pamięć żądań).
     *   • Kilka łańcuchów: securityMatcher("/api/**") + @Order — wygrywa pierwszy pasujący; ostatni bez securityMatcher.
     *   • CSRF domyślnie włączony: POST/PUT/PATCH/DELETE bez tokenu → 403 (nawet anonim — CsrfFilter jest przed logowaniem).
     *     GET/HEAD/TRACE/OPTIONS bez tokenu. Nie zmieniaj danych w GET.
     *   • Wyłącz CSRF tylko, gdy przeglądarka nie dołącza poświadczeń sama (nagłówek Authorization, bez ciasteczek).
     *     SPA z sesją: .csrf(c -> c.spa()) — ciasteczko XSRF-TOKEN + nagłówek X-XSRF-TOKEN.
     *   • CORS: bean corsConfigurationSource + http.cors(Customizer.withDefaults()); preflight przychodzi bez logowania.
     *     Sam CORS w MVC + Security bez cors() → preflight 401.
     *   • 401/403 jako JSON: exceptionHandling(authenticationEntryPoint, accessDeniedHandler) ORAZ
     *     httpBasic(b -> b.authenticationEntryPoint(…)) dla złego hasła. @RestControllerAdvice błędów z filtrów nie widzi.
     *   • Nagłówki domyślnie: nosniff, X-Frame-Options DENY, Cache-Control no-store, X-XSS-Protection 0; HSTS tylko HTTPS.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? Anna zalogowała się formularzem w łańcuchu z sesją i ma ciasteczko JSESSIONID. Wysyła je (bez nagłówka
     *      Authorization) do {@code /sec04/api/orders}, który ma łańcuch STATELESS. Jaki status dostanie?
     *   2. Co się stanie? Anonim wysyła {@code POST /sec04/web/email} bez tokenu CSRF do łańcucha, który wymaga logowania.
     *      401 czy 403 — i dlaczego?
     *   3. PYTANIE REKRUTACYJNE: czym jest atak CSRF? Dlaczego bezstanowe API z nagłówkiem {@code Authorization: Bearer …}
     *      może wyłączyć ochronę CSRF, a aplikacja trzymająca JWT w ciasteczku — nie?
     *   4. ZNAJDŹ BŁĄD: frontend z http://localhost:3000 dostaje w przeglądarce "CORS error", a w Postmanie wszystko działa.
     *      CORS jest ustawiony w {@code WebMvcConfigurer.addCorsMappings}, łańcuch ma {@code anyRequest().authenticated()}.
     *   5. ZNAJDŹ BŁĄD: zespół napisał {@code @RestControllerAdvice} z {@code @ExceptionHandler(AuthenticationException.class)},
     *      który zwraca ProblemDetail, a klient dalej dostaje 401 z pustym ciałem. Dlaczego i jak to naprawić?
     *   6. Co się stanie? Punkt wejścia z JSON-em jest ustawiony tylko w {@code exceptionHandling}. Klient wysyła złe hasło Basic.
     *   7. Dlaczego Spring nie wysyła nagłówka {@code Strict-Transport-Security} w odpowiedzi na żądanie przez zwykłe HTTP?
     *   8. Dlaczego GET nie wymaga tokenu CSRF i co z tego wynika dla projektowania API?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: CsrfRules, ExerciseCors, ExerciseJsonEntryPoint, ExerciseSecurityConfig (ExerciseController
    // i ExerciseSecurityBeans są gotowe); testy: Sec04StatelessApiExercisesTest i Sec04ExerciseSecurityTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Rozwiązania wzorcowe: pakiet s12_security.solutions.sec04_stateless_api. Adresy ćwiczeń mają prefiks /sec04/cw.
    //
    // ĆWICZENIE 1 (łatwe): CsrfRules.requiresCsrf — {@code true} dla metod zmieniających dane, {@code false} dla GET, HEAD, TRACE,
    //   OPTIONS (wielkość liter bez znaczenia). Podpowiedź: {@code Set.of(...)} i {@code toUpperCase(Locale.ROOT)}; sekcja 2.
    //
    // ĆWICZENIE 2 (łatwe): ExerciseCors.source — reguła dla {@code /sec04/cw/**}: pochodzenie http://localhost:3000, metody
    //   GET i POST, nagłówki Authorization i Content-Type, maxAge 1800 s. Podpowiedź: wzór w Sec04SecurityConfig;
    //   {@code setMaxAge(1800L)}.
    //
    // ĆWICZENIE 3 (średnie): ExerciseJsonEntryPoint.commence — 401, {@code WWW-Authenticate: Basic realm="cw"},
    //   {@code application/problem+json}, UTF-8, JSON z {@code status} i {@code title} "Zaloguj się". Podpowiedź:
    //   {@code response.setStatus}, {@code setHeader}, {@code setContentType}, {@code setCharacterEncoding}, potem
    //   {@code response.getWriter().write(...)} (stały tekst JSON wystarczy). Kolejność: kodowanie PRZED getWriter().
    //
    // ĆWICZENIE 4 (trudniejsze): ExerciseSecurityConfig.filterChain — łańcuch dla {@code /sec04/cw/**}: GET catalog dla każdego,
    //   reszta dla zalogowanych, STATELESS, CSRF wyłączony, CORS z ćwiczenia 2 ({@code cors(c -> c.configurationSource(…))}),
    //   httpBasic i exceptionHandling z punktem wejścia z ćwiczenia 3. Podpowiedź: wzór apiChain; pamiętaj o punkcie wejścia
    //   w OBU miejscach (sekcja 4). Dopóki jest TODO, test zgłosi "Failed to load ApplicationContext" z przyczyną
    //   UnsupportedOperationException: TODO.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 401 — łańcuch STATELESS w ogóle nie czyta sesji, więc ciasteczko nic nie znaczy; liczy się tylko nagłówek
     *      Authorization (test sessionCookieIsEnoughForBrowserChain).
     *   2. 403 — CsrfFilter stoi w łańcuchu PRZED filtrami logowania i autoryzacji, więc POST bez tokenu zostaje odrzucony,
     *      zanim Spring w ogóle zapyta, kim jest klient (test csrfCheckedBeforeAuthentication).
     *   3. CSRF: obca strona wysyła żądanie do naszego serwera, a przeglądarka sama dołącza ciasteczka (i zapamiętane Basic)
     *      zalogowanego użytkownika. Nagłówka Authorization z tokenem przeglądarka sama nie dołączy, a obca strona tokenu nie zna,
     *      więc bezstanowe API z Bearer jest odporne. JWT w ciasteczku jest dołączany automatycznie — wraca ryzyko CSRF.
     *   4. Brak http.cors(...) w łańcuchu: preflight OPTIONS przychodzi bez logowania i dostaje 401 z filtrów, zanim reguła z MVC
     *      zostanie przeczytana (Sec04NoCorsPitfallTest). Postman nie robi preflight. Naprawa: .cors(Customizer.withDefaults())
     *      (weźmie reguły z MVC) albo bean corsConfigurationSource.
     *   5. Advice działa w DispatcherServlet, a odmowa z reguł adresów zapada w filtrach, zanim żądanie tam dotrze
     *      (Sec04AdvicePitfallTest). Naprawa: własny AuthenticationEntryPoint i AccessDeniedHandler w łańcuchu
     *      (ProblemDetailSecurityHandlers).
     *   6. Złe hasło obsługuje BasicAuthenticationFilter swoim domyślnym punktem wejścia: 401, WWW-Authenticate
     *      Basic realm="Realm", charset="UTF-8" i puste ciało (Sec04BasicEntryPointPitfallTest). Trzeba ustawić punkt wejścia
     *      także w httpBasic(...).
     *   7. HSTS mówi "łącz się ze mną tylko po HTTPS"; wysłany przez HTTP mógłby zostać usunięty albo podrobiony po drodze, więc
     *      Spring dodaje go tylko do żądań bezpiecznych (test securityHeaders: bez .secure(true) nagłówka nie ma).
     *   8. GET to metoda "bezpieczna" — z definicji tylko czyta, więc podrobiony GET nie zmieni danych. Wniosek: operacje
     *      zmieniające stan zawsze przez POST/PUT/PATCH/DELETE; GET, który usuwa lub zmienia dane, omija ochronę CSRF.
     */
    // </editor-fold>
}

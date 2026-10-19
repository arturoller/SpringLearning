package s12_security.sec01_basics;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * <pre>
 * TEMAT: Spring Security — pierwsze kroki: co robi sam starter, łańcuch filtrów, własny SecurityFilterChain, 401 a 403
 *        (security = bezpieczeństwo; filter chain = łańcuch filtrów; authentication = uwierzytelnianie;
 *        authorization = autoryzacja)
 *
 * W SKRÓCIE:
 *   • Dodajesz spring-boot-starter-security i CAŁA aplikacja jest zamknięta: każdy adres wymaga logowania. Boot tworzy
 *     użytkownika „user” z losowym hasłem (wypisanym w logu), formularz /login i logowanie HTTP Basic.
 *   • Security to łańcuch FILTRÓW, które oglądają zapytanie, ZANIM dotrze do kontrolera. Kontroler nic nie wie
 *     o zabezpieczeniach — i dobrze.
 *   • Własny bean SecurityFilterChain zastępuje łańcuch domyślny Boota. Reguły piszesz DSL-em z lambdami:
 *     authorizeHttpRequests → requestMatchers(...) → permitAll() / authenticated() / hasRole(...).
 *   • 401 Unauthorized = „nie wiem, kim jesteś” (zaloguj się). 403 Forbidden = „wiem, kim jesteś, ale nie wolno ci”.
 *   • Stare poradniki (Security 5: WebSecurityConfigurerAdapter, antMatchers, .and()) w Security 7.1 się NIE KOMPILUJĄ.
 *
 * ANALOGIA: biurowiec z ochroną.
 *   Przy wejściu stoi ochrona (filtry). Najpierw: „kim pan jest?” — pokazujesz identyfikator (uwierzytelnianie).
 *   Potem: „na które piętro?” — ochroniarz sprawdza listę (autoryzacja). Bez identyfikatora słyszysz „proszę się
 *   wylegitymować” (401). Z identyfikatorem pracownika, ale na piętro zarządu: „tam pana nie wpuszczę” (403).
 *   Recepcja (kontroler) w ogóle nie sprawdza identyfikatorów — ufa ochronie przy drzwiach.
 *
 * JAK TO DZIAŁA:
 *   zapytanie HTTP → Tomcat → filtry serwletów → DelegatingFilterProxy („springSecurityFilterChain”, kolejność -100)
 *        → FilterChainProxy (bean Springa) → wybiera PIERWSZY pasujący SecurityFilterChain → jego filtry po kolei:
 *          ... CsrfFilter → BasicAuthenticationFilter (kim jesteś?) → AnonymousAuthenticationFilter (nikt? to „anonim”)
 *          → ExceptionTranslationFilter (zamienia odmowy na 401/403) → AuthorizationFilter (czy wolno?)
 *        → DispatcherServlet → kontroler
 *   Gdy AuthorizationFilter odmówi: anonim → „punkt wejścia” (entry point) → 401 albo przekierowanie na /login;
 *   zalogowany → 403.
 *
 * SŁÓWKA:
 *   authentication = uwierzytelnianie (sprawdzenie, KIM jesteś); authorization = autoryzacja (sprawdzenie, CO ci wolno);
 *   principal = podmiot (zalogowany „ktoś”); credentials = dane uwierzytelniające (np. hasło);
 *   unauthorized = nieuwierzytelniony (kod 401 — mimo nazwy chodzi o brak logowania, nie o brak uprawnień);
 *   forbidden = zabroniony (403); permit all = wpuść wszystkich; authenticated = zalogowany;
 *   realm = obszar (nazwa chronionej strefy w nagłówku Basic); entry point = punkt wejścia (co zrobić z niezalogowanym);
 *   filter = filtr (kod wykonywany przed serwletem); proxy = pośrednik; customizer = „dostrajacz” (lambda, która
 *   konfiguruje jedną część łańcucha); DSL = domain-specific language (mały język do jednego zadania).
 *
 * ZOBACZ TEŻ: s03_boot_basics/Boot01AutoConfiguration (warunki autokonfiguracji — dlaczego domyślny łańcuch znika),
 *             s06_error_handling/Err01DefaultErrors (strona błędu /error), s04_web_rest/Rest06Cors (CORS — w Security:
 *             s12_security/Sec04StatelessApi), s12_security/Sec02Authorization (role i kolejność reguł),
 *             t31_jdk_toolbox/Toolbox01UuidBase64 (Base64 w nagłówku Basic),
 *             t13_lambdas/Lambda02FunctionalInterfaces (Customizer to interfejs funkcyjny).
 * </pre>
 */
public final class Sec01Basics {

    private Sec01Basics() {
    }

    // =================================================================================================
    // 1. SAM STARTER — co się dzieje zaraz po dodaniu spring-boot-starter-security
    // =================================================================================================
    //
    // Uruchom Sec01StarterOnlyApplication (▶ przy main). To aplikacja z jednym kontrolerem i BEZ własnej konfiguracji
    // bezpieczeństwa. W logu zobaczysz (skopiowane z prawdziwego uruchomienia, hasło za każdym razem inne):
    //
    //   2026-10-19T10:34:27.088+02:00  WARN 16352 --- [           main] .s.a.UserDetailsServiceAutoConfiguration :
    //
    //   Using generated security password: c28d13e7-fa07-4a24-917f-81ca35f7def3
    //
    //   This generated password is for development use only. Your security configuration must be updated before
    //   running your application in production.
    //
    // („Używam wygenerowanego hasła… To hasło jest tylko do developmentu. Przed produkcją zmień konfigurację”).
    // Poziom WARN (ostrzeżenie), bo to stan przejściowy. Użytkownik nazywa się „user”, hasło to losowy UUID
    // wylosowany przy KAŻDYM starcie. Co robi starter (autokonfiguracja Boota):
    //   • UserDetailsServiceAutoConfiguration — bean InMemoryUserDetailsManager z jednym użytkownikiem „user”,
    //   • ServletWebSecurityAutoConfiguration — domyślny łańcuch: anyRequest().authenticated() + formLogin + httpBasic,
    //   • SecurityFilterAutoConfiguration — rejestruje filtr Security w Tomcacie.
    //
    // Efekt (Sec01BasicsStarterOnlyTest):
    //   GET /api/public/products              → 401, nagłówek WWW-Authenticate: Basic realm="Realm", charset="UTF-8"
    //   to samo z Accept: text/html (przeglądarka) → 302 Found, Location: http://localhost:8080/login
    //   GET /login                            → 200 i gotowy formularz HTML „Please sign in”
    //   z nagłówkiem Authorization: Basic (user + hasło z logu) → 200
    // Skąd różnica 401/302? Boot włącza OBA sposoby logowania, a Security patrzy na nagłówek Accept: przeglądarkę
    // odsyła na formularz, a programowi (curl, aplikacja mobilna, plik .http) odpowiada 401 z prośbą o Basic.
    // Nagłówek WWW-Authenticate („uwierzytelnij się tak”) mówi klientowi, JAK ma się zalogować. Dopisek
    // charset="UTF-8" pojawił się w Security 7.1 — starsze poradniki pokazują samo Basic realm="Realm".
    //
    // PUŁAPKA: w TYM module wygenerowanego hasła NIE MA, dopóki nie ustawisz spring.security.user.name (dlatego
    //   Sec01StarterOnlyApplication je ustawia). Dlaczego? Moduł ma też starter oauth2-resource-server (do lekcji o JWT),
    //   a Boot tworzy użytkownika „user” tylko wtedy, gdy na classpath NIE ma alternatywnego sposobu logowania
    //   (klasy OpaqueTokenIntrospector, ClientRegistrationRepository albo RelyingPartyRegistrationRepository) — chyba
    //   że sam podasz spring.security.user.name lub spring.security.user.password. Bez tego: nie ma żadnego
    //   użytkownika, a każda próba logowania kończy się 401 (Sec01BasicsResourceServerPitfallTest). Gdy „nagle
    //   zniknęło hasło z logu” — sprawdź, jaki nowy starter doszedł.
    //
    // DOBRA PRAKTYKA: wygenerowanego hasła używaj tylko w pierwszych minutach nauki. Dlaczego? Zmienia się przy każdym
    //   starcie, jest jedno dla wszystkich i ląduje w logach. Prawdziwa aplikacja definiuje własnych użytkowników
    //   (sekcja 3 i Sec03UsersPasswords) albo przyjmuje tokeny (Sec05Jwt).

    // =================================================================================================
    // 2. ŁAŃCUCH FILTRÓW — DelegatingFilterProxy, FilterChainProxy, SecurityFilterChain
    // =================================================================================================
    //
    // Filtr serwletu (jakarta.servlet.Filter) to kod, który Tomcat wywołuje PRZED serwletem — może zapytanie
    // przepuścić dalej albo od razu odpowiedzieć (np. 401). Security składa się z kilkunastu takich filtrów.
    //
    // Trzy piętra, każde z innego powodu:
    //   • DelegatingFilterProxy („pośrednik delegujący”) — JEDYNY filtr Security, który zna Tomcat. Serwer rejestruje
    //     filtry wcześnie: w Boocie jeszcze w trakcie budowania kontekstu, zanim powstanie większość beanów (w klasycznym
    //     WAR-ze nawet przed kontekstem Springa), i sam nie umie wstrzykiwać beanów. Pośrednik jest lekki: dopiero przy
    //     pierwszym zapytaniu odszukuje bean „springSecurityFilterChain” i oddaje mu pracę — most między światem
    //     serwletów a światem beanów. Boot rejestruje go sam (DelegatingFilterProxyRegistrationBean, kolejność -100,
    //     ustawienie spring.security.filter.order — wcześnie, przed filtrami aplikacji o domyślnej kolejności).
    //   • FilterChainProxy („pośrednik łańcuchów”) — bean Springa o nazwie springSecurityFilterChain. Trzyma listę
    //     łańcuchów i dla każdego zapytania wybiera PIERWSZY pasujący (np. inny dla /api/**, inny dla reszty —
    //     Sec04StatelessApi). Robi też rzeczy wspólne dla wszystkich łańcuchów: sprawdza adres zaporą
    //     StrictHttpFirewall (firewall = zapora; odrzuca podejrzane ścieżki) i po zapytaniu czyści kontekst
    //     bezpieczeństwa wątku.
    //   • SecurityFilterChain („łańcuch filtrów bezpieczeństwa”) — konkretna lista filtrów dla pasujących zapytań.
    //
    // Domyślny łańcuch Boota ma 16 filtrów (Sec01BasicsStarterOnlyTest wypisuje je po kolei), m.in.:
    //   CsrfFilter (ochrona przed CSRF — Sec04StatelessApi), LogoutFilter (/logout),
    //   UsernamePasswordAuthenticationFilter (POST /login z formularza), DefaultLoginPageGeneratingFilter (strona /login),
    //   BasicAuthenticationFilter (nagłówek Authorization: Basic), AnonymousAuthenticationFilter (niezalogowany = „anonim”),
    //   ExceptionTranslationFilter (odmowa → 401/403), AuthorizationFilter (OSTATNI: „czy wolno?”).
    //
    // PUŁAPKA: kolejność filtrów jest stała i ma znaczenie — uwierzytelnianie jest PRZED autoryzacją. Dlatego złe
    //   hasło wysłane na publiczny adres daje 401: BasicAuthenticationFilter odrzuca zapytanie, zanim
    //   AuthorizationFilter zdąży sprawdzić, że adres jest publiczny (sekcja 4, Sec01BasicsTest).

    // =================================================================================================
    // 3. WŁASNY SecurityFilterChain — DSL z lambdami (Sec01SecurityConfig)
    // =================================================================================================
    //
    //   @Bean
    //   SecurityFilterChain securityFilterChain(HttpSecurity http) {
    //       http.authorizeHttpRequests(auth -> auth
    //               .requestMatchers("/api/public/**").permitAll()
    //               .requestMatchers("/error").permitAll()
    //               .requestMatchers("/api/admin/**").hasRole("ADMIN")
    //               .anyRequest().authenticated());
    //       http.httpBasic(Customizer.withDefaults());
    //       return http.build();
    //   }
    //
    // • Każda część łańcucha ma metodę przyjmującą lambdę (Customizer) — „skonfiguruj tę część tak”.
    //   Customizer.withDefaults() = „włącz z ustawieniami domyślnymi”.
    // • requestMatchers("/api/public/**") — wzorzec ścieżki: ** = dowolnie wiele segmentów (/api/public/a/b/c).
    // • Reguły czyta się od góry; pierwsza pasująca wygrywa (szczegóły i pułapki kolejności: Sec02Authorization).
    // • Gdy w kontekście jest choć jeden bean SecurityFilterChain, Boot WYCOFUJE swój domyślny łańcuch
    //   (@ConditionalOnMissingBean — s03_boot_basics/Boot01AutoConfiguration). Obowiązuje tylko to, co napisałeś:
    //   nie dodałeś formLogin — nie ma strony /login, a przeglądarka dostaje 401 zamiast przekierowania.
    //   Nasz łańcuch ma 12 filtrów zamiast 16 (Sec01BasicsTest).
    //
    // PUŁAPKA: kod z poradników dla Security 5 w Security 7.1 się NIE KOMPILUJE. Sprawdzone kompilacją:
    //   „extends WebSecurityConfigurerAdapter” → error: cannot find symbol (klasa usunięta w Security 6.0),
    //   http.authorizeRequests() → cannot find symbol (usunięte w 7.0; jest authorizeHttpRequests(...)),
    //   .antMatchers(...) → cannot find symbol (usunięte w 6.0; jest requestMatchers(...)),
    //   http.httpBasic().and() → „method httpBasic in class HttpSecurity cannot be applied to given types”
    //   (wersje bez lambdy i .and() usunięte w 7.0). Dlaczego to zmieniono? Styl lambd jest czytelniejszy (widać,
    //   co do czego należy), a jedna klasa bazowa do dziedziczenia nie pozwalała mieć kilku łańcuchów jako beanów.
    //
    // DOBRA PRAKTYKA: konfigurację bezpieczeństwa trzymaj w osobnej klasie @Configuration (tu Sec01SecurityConfig),
    //   a nie w klasie z main. Dlaczego? Łatwo ją znaleźć, a w testach @WebMvcTest dołączasz ją jednym @Import
    //   (Sec06SecurityTesting).

    // =================================================================================================
    // 4. 401 a 403 — i strona błędu /error
    // =================================================================================================
    //
    //   anonim → /api/admin/report                     → 401 Unauthorized + WWW-Authenticate: Basic ... (zaloguj się)
    //   piotr (rola USER) → /api/admin/report          → 403 Forbidden, BEZ WWW-Authenticate (logowanie nic nie da)
    //   anna (rola ADMIN) → /api/admin/report          → 200 OK
    //   anonim → /api/nie-ma-takiego                   → 401, nie 404! Security działa PRZED DispatcherServletem
    //                                                    i nie wie, czy taki adres w ogóle istnieje
    //   anna → /api/nie-ma-takiego                     → 404
    //
    // PYTANIE REKRUTACYJNE: czym różni się 401 od 403? 401 — brak (albo złe) uwierzytelnienie: klient może spróbować
    //   ponownie z danymi logowania. 403 — uwierzytelnienie jest, brakuje uprawnień: ponowne logowanie nic nie zmieni.
    //
    // Ciało odpowiedzi: Security wywołuje response.sendError(401/403), a Tomcat przekazuje wtedy zapytanie na stronę
    // błędu Boota /error (s06_error_handling/Err01DefaultErrors), która zwraca JSON
    //   {"timestamp":"...","status":403,"error":"Forbidden","path":"/api/admin/report"}.
    // Ale /error to też adres — i łańcuch filtrów sprawdza go tak samo jak każdy inny.
    //
    // PUŁAPKA: bez .requestMatchers("/error").permitAll() niezalogowany dostaje 401 z PUSTYM ciałem (tak jest
    //   w domyślnym łańcuchu Boota — Sec01BasicsStarterOnlyTest). Gorzej: każda odmowa dla anonima, która przechodzi
    //   przez /error, zamienia się w 401 — np. POST bez tokenu CSRF zwraca wtedy 401 zamiast 403 (test
    //   Sec04StatelessApiPitfallsTest; o CSRF — Sec04StatelessApi). Dlaczego? Przekierowanie na /error to dla Security nowe
    //   zapytanie od anonima, a reguła anyRequest().authenticated() go nie wpuszcza.
    //
    // DOBRA PRAKTYKA: w łańcuchu z anyRequest().authenticated() dodaj .requestMatchers("/error").permitAll().
    //   Dlaczego? Strona błędu nie zdradza danych (tylko status i ścieżkę), a klient dostaje prawdziwy kod błędu
    //   i czytelne ciało JSON zamiast pustej odpowiedzi.
    //
    // PUŁAPKA: złe hasło wysłane na PUBLICZNY adres → 401, choć bez nagłówka Authorization byłoby 200.
    //   Dlaczego? BasicAuthenticationFilter widzi nagłówek, próbuje zalogować, nie udaje się — i od razu odpowiada 401
    //   (sekcja 2: uwierzytelnianie jest przed autoryzacją). Klient, który „na wszelki wypadek” dokleja stare hasło
    //   do każdego zapytania, nagle traci dostęp nawet do katalogu produktów.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Starter security = wszystko zamknięte; użytkownik „user”, hasło w logu (WARN, nowe przy każdym starcie);
     *     przeglądarka → 302 na /login, program → 401 + WWW-Authenticate: Basic realm="Realm", charset="UTF-8".
     *   • Brak hasła w logu? Na classpath jest inny sposób logowania (np. oauth2-resource-server) — ustaw
     *     spring.security.user.name albo zdefiniuj własny UserDetailsService.
     *   • Tomcat → DelegatingFilterProxy → FilterChainProxy (bean springSecurityFilterChain) → pierwszy pasujący
     *     SecurityFilterChain → filtry → DispatcherServlet.
     *   • Własny łańcuch: @Bean SecurityFilterChain(HttpSecurity http) { http.authorizeHttpRequests(auth -> auth
     *     .requestMatchers(...).permitAll() ... .anyRequest().authenticated()); http.httpBasic(withDefaults());
     *     return http.build(); } — domyślny łańcuch Boota znika (także /login).
     *   • 401 = kim jesteś? (+ WWW-Authenticate); 403 = wiem, ale nie wolno. Anonim na nieistniejący adres → 401.
     *   • .requestMatchers("/error").permitAll() — inaczej puste ciała i 403 zamienione na 401.
     *   • Security 5 (WebSecurityConfigurerAdapter, authorizeRequests, antMatchers, .and()) — w 7.1 nie istnieje.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego po dodaniu samego startera nawet lista produktów zwraca 401? Jaki nagłówek mówi klientowi, jak się
     *      zalogować?
     *   2. Co się stanie? Uruchomisz Sec01StarterOnlyApplication BEZ ustawienia spring.security.user.name — czy w logu
     *      pojawi się wygenerowane hasło? Dlaczego?
     *   3. Po co jest DelegatingFilterProxy, skoro i tak cała praca dzieje się w FilterChainProxy?
     *   4. Dlaczego po dodaniu własnego beana SecurityFilterChain zniknął formularz /login?
     *   5. Co się stanie? W Sec01Application niezalogowany wyśle GET /api/nie-ma-takiego. 404 czy 401? A anna?
     *   6. Co się stanie? Klient wyśle do PUBLICZNEGO /api/public/products nagłówek Basic ze złym hasłem.
     *   7. ZNAJDŹ BŁĄD: kod z poradnika:
     *        public class SecurityConfig extends WebSecurityConfigurerAdapter {
     *            protected void configure(HttpSecurity http) throws Exception {
     *                http.authorizeRequests().antMatchers("/api/public/**").permitAll()
     *                    .anyRequest().authenticated().and().httpBasic();
     *            }
     *        }
     *   8. Czym różni się 401 od 403 (PYTANIE REKRUTACYJNE)? Kiedy odpowiedź ma nagłówek WWW-Authenticate?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Sec01BasicsExercisesTest (każde ćwiczenie 2–3 startuje małą aplikację z Twoim łańcuchem
    // i kontrolerem ShopController; użytkownicy w teście: anna/anna-test-123 (ADMIN), piotr/piotr-test-123 (USER))
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj wartość nagłówka Authorization dla HTTP Basic: "Basic " + Base64(login + ":" + hasło),
     * bajty tekstu w UTF-8. Przykład: ("anna", "anna-test-123") → "Basic YW5uYTphbm5hLXRlc3QtMTIz".
     * Podpowiedź: java.util.Base64.getEncoder() i getBytes(StandardCharsets.UTF_8) (t31_jdk_toolbox/Toolbox01UuidBase64).
     * Zauważ: Base64 to NIE szyfrowanie — każdy odczyta hasło. Dlatego Basic tylko przez HTTPS.
     */
    static String exercise1(String username, String password) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj łańcuch: /api/public/** i /error — dla wszystkich; każdy inny adres — tylko dla
     * zalogowanych; logowanie przez HTTP Basic. Oczekiwane: anonim → /api/public/products 200, /api/orders 401;
     * piotr → /api/orders 200.
     * Podpowiedź: wzór w Sec01SecurityConfig (bez reguły dla /api/admin/**).
     */
    static SecurityFilterChain exercise2(HttpSecurity http) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): odtwórz zachowanie domyślnego łańcucha Boota (formularz logowania I HTTP Basic),
     * ale z wyjątkami: /api/public/** i /error — dla wszystkich, /api/admin/** — tylko rola ADMIN, reszta — zalogowani.
     * Oczekiwane: przeglądarka (Accept: text/html) → /api/orders daje 302 na /login; program bez Accept → 401
     * z WWW-Authenticate; GET /login → 200 (formularz); piotr → /api/admin/report 403; anna → 200.
     * Podpowiedź: http.formLogin(Customizer.withDefaults()) i http.httpBasic(Customizer.withDefaults()) — oba naraz.
     */
    static SecurityFilterChain exercise3(HttpSecurity http) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Starter włącza domyślny łańcuch anyRequest().authenticated() — nie wie, które adresy uważasz za publiczne.
     *      Odpowiedź 401 ma nagłówek WWW-Authenticate: Basic realm="Realm", charset="UTF-8" — „zaloguj się przez Basic”.
     *   2. Nie pojawi się. Moduł ma na classpath starter oauth2-resource-server (klasa OpaqueTokenIntrospector), więc
     *      Boot uznaje, że logowanie będzie przez tokeny, i nie tworzy użytkownika „user”. Hasło wraca po ustawieniu
     *      spring.security.user.name albo spring.security.user.password. Sprawdzone testem
     *      Sec01BasicsResourceServerPitfallTest (brak beana UserDetailsService, brak linii w logu, logowanie → 401).
     *   3. Serwer rejestruje filtry wcześnie (w Boocie — zanim powstanie większość beanów) i nie umie wstrzykiwać
     *      beanów. DelegatingFilterProxy to zwykły, lekki filtr serwletu, który dopiero przy pierwszym zapytaniu
     *      znajduje bean springSecurityFilterChain (FilterChainProxy) i przekazuje mu pracę — most między światem
     *      serwletów a światem Springa. Dzięki temu cała konfiguracja bezpieczeństwa nie musi powstać przy starcie serwera.
     *   4. Domyślny łańcuch Boota ma warunek @ConditionalOnMissingBean(SecurityFilterChain.class). Twój bean go wyłącza
     *      w całości, a w Twoim łańcuchu nie ma formLogin — więc nie ma też filtrów strony /login (12 filtrów zamiast 16).
     *   5. Anonim dostaje 401: Security działa przed DispatcherServletem i nie wie, że adresu nie ma; reguła
     *      anyRequest().authenticated() wymaga logowania. Anna (zalogowana) przechodzi przez filtry i dostaje 404 od
     *      Spring MVC. Sprawdzone testem Sec01BasicsTest.unknownUrl.
     *   6. 401. BasicAuthenticationFilter próbuje zalogować na podstawie nagłówka, logowanie się nie udaje i filtr od razu
     *      odpowiada 401 — reguła permitAll() nawet nie jest sprawdzana. Sprawdzone testem
     *      Sec01BasicsTest.wrongPasswordOnPublicEndpoint.
     *   7. To kod dla Security 5. W Security 7.1 pierwszym błędem jest błąd KOMPILACJI: „cannot find symbol: class
     *      WebSecurityConfigurerAdapter” (usunięta w 6.0); dalej authorizeRequests() (usunięte w 7.0) i antMatchers
     *      (usunięte w 6.0). Poprawnie: @Bean SecurityFilterChain z http.authorizeHttpRequests(auth -> auth
     *      .requestMatchers("/api/public/**").permitAll().anyRequest().authenticated()) i
     *      http.httpBasic(Customizer.withDefaults()). Sprawdzone kompilacją na zależnościach modułu.
     *   8. 401 — brak lub złe uwierzytelnienie; odpowiedź ma WWW-Authenticate (jak się zalogować). 403 — użytkownik
     *      znany, ale bez uprawnień; nagłówka WWW-Authenticate nie ma, bo ponowne logowanie nic nie da
     *      (Sec01BasicsTest.unauthorizedVersusForbidden).
     */
    // </editor-fold>
}

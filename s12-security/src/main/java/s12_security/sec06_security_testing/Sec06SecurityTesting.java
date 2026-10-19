package s12_security.sec06_security_testing;

/**
 * <pre>
 * TEMAT: Testowanie zabezpieczeń — @WebMvcTest z Security, @WithMockUser, @WithUserDetails, httpBasic(), user(),
 *        jwt(), csrf(), testy 401/403/200 dla każdej reguły
 *        (mock user = udawany użytkownik; request post-processor = „obróbka” zapytania przed wysłaniem)
 *
 * W SKRÓCIE:
 *   • Reguły bezpieczeństwa to kod jak każdy inny — i psują się po cichu (zła kolejność, literówka w roli). Każda reguła
 *     zasługuje na test: anonim → 401, zły użytkownik → 403, właściwy → 200.
 *   • @WebMvcTest ładuje kontrolery i Security, ale NIE Twoją klasę @Configuration z łańcuchem — trzeba ją dołączyć
 *     przez @Import. Bez tego testujesz domyślny łańcuch Boota, nie swój.
 *   • Użytkownika „podkładasz”: @WithMockUser (dowolny, bez logowania), @WithUserDetails (z Twojego
 *     UserDetailsService), albo na jedno zapytanie: .with(user(...)), .with(httpBasic(...)), .with(jwt()).
 *   • CSRF działa też w testach — POST/PUT/DELETE w łańcuchu z CSRF potrzebują .with(csrf()).
 *
 * ANALOGIA: próbny alarm w biurowcu.
 *   Nie czekasz na prawdziwego włamywacza — sprawdzasz każde drzwi: „czy obcy wejdzie?” (401), „czy pracownik wejdzie
 *   do serwerowni?” (403), „czy administrator wejdzie?” (200). @WithMockUser to przepustka wydrukowana na potrzeby
 *   próby — ochrona jej nie weryfikuje w kadrach. httpBasic() to prawdziwa przepustka sprawdzana w czytniku.
 *
 * JAK TO DZIAŁA:
 *   @WithMockUser(roles = "ADMIN") ── przed testem ──► TestSecurityContextHolder: Authentication „user”, ROLE_ADMIN
 *   mockMvc.perform(post(...).with(csrf()))
 *     → MockMvc z filtrami Security (Boot dokłada je sam — SecurityMockMvcAutoConfiguration)
 *     → FilterChainProxy → CsrfFilter (token z csrf() pasuje) → AuthorizationFilter (hasRole ADMIN) → kontroler
 *   .with(httpBasic("anna", "hasło")) → nagłówek Authorization → BasicAuthenticationFilter → PRAWDZIWE logowanie
 *   .with(jwt()) → gotowy JwtAuthenticationToken w kontekście — bez podpisu i bez JwtDecoder
 *
 * SŁÓWKA:
 *   slice = wycinek (test ładuje tylko część aplikacji); import = dołącz; mock user = udawany użytkownik;
 *   anonymous = anonimowy (niezalogowany); user details = dane użytkownika (z UserDetailsService);
 *   request post-processor = obróbka zapytania (dokłada nagłówek, token CSRF albo użytkownika);
 *   meta-annotation = meta-adnotacja (adnotacja umieszczona na innej adnotacji); full context = pełny kontekst.
 *
 * ZOBACZ TEŻ: s11_testing/Test01TestSlices (wycinki testowe), s11_testing/Test02MockitoBeans ({@code @MockitoBean}),
 *             s00_start/Start04TestingIntro (MockMvc od podstaw), s12_security/Sec02Authorization (testowane reguły),
 *             s12_security/Sec04StatelessApi (CSRF), s12_security/Sec05Jwt (prawdziwe tokeny),
 *             t19_annotations_reflection/Annotations02Custom (własne adnotacje).
 * </pre>
 */
public final class Sec06SecurityTesting {

    private Sec06SecurityTesting() {
    }

    // =================================================================================================
    // 1. @WebMvcTest A SECURITY — co jest ładowane i dlaczego trzeba @Import
    // =================================================================================================
    //
    // @WebMvcTest(ShopController.class) to wycinek: kontroler, filtry, konwertery, MockMvc — bez serwisów i bez Twoich
    // klas @Configuration. W Boot 4 Security trafia do tego wycinka dzięki spring-boot-starter-security-test: jego jar
    // dopisuje autokonfiguracje Security do MockMvc (SecurityAutoConfiguration, ServletWebSecurityAutoConfiguration,
    // SecurityFilterAutoConfiguration, UserDetailsServiceAutoConfiguration, SecurityMockMvcAutoConfiguration) —
    // sprawdzone w zawartości jarów Boota 4.1.1. Dołącza też skanowane komponenty TYPU SecurityFilterChain, ale nasz
    // łańcuch to metoda @Bean w klasie @Configuration — a takich klas wycinek nie skanuje.
    //
    // PUŁAPKA: test bez @Import(Sec06SecurityConfig.class) działa, tylko testuje NIE TO. W kontekście jest wyłącznie
    //   defaultSecurityFilterChain Boota (wszystko wymaga logowania), więc publiczne GET /api/products dostaje 401
    //   (Sec06SecurityTestingNoImportTest). Ktoś „naprawi” test, wpisując 401 — i test będzie zielony dla złej reguły.
    //
    //   @WebMvcTest(ShopController.class)
    //   @Import(Sec06SecurityConfig.class)   // nasz łańcuch, użytkownicy, PasswordEncoder, JwtDecoder
    //   class Sec06SecurityTestingTest { @MockitoBean OrderService orderService; ... }
    // W kontekście są wtedy dokładnie nasze łańcuchy: partnerChain i shopChain.

    // =================================================================================================
    // 2. @WithMockUser i @WithAnonymousUser — użytkownik „z powietrza”
    // =================================================================================================
    //
    //   @WithMockUser                                      → „user”, rola USER (hasło „password” — nikt go nie sprawdza)
    //   @WithMockUser(username = "anna", roles = "ADMIN")  → anna, ROLE_ADMIN
    //   @WithMockUser(authorities = "report:read")         → dokładnie te uprawnienia (bez prefiksu)
    //   @WithAnonymousUser                                 → jawnie niezalogowany
    // Adnotacja wkłada gotowe Authentication do kontekstu PRZED testem — bez filtra logowania i bez UserDetailsService.
    //
    // PUŁAPKA: @WithMockUser nie sprawdza, czy użytkownik istnieje, ani hasła. @WithMockUser(username = "nikt-taki")
    //   dostaje 200 na /api/me, z uprawnieniami ["ROLE_USER"] — bez FACTOR_PASSWORD, bo logowania nie było. Dlatego
    //   test z @WithMockUser sprawdza REGUŁY, a nie to, czy logowanie działa (do tego httpBasic() — sekcja 4).
    //
    // PUŁAPKA: CSRF działa także w testach. Admin wysyłający POST bez .with(csrf()) dostaje 403 — i łatwo uznać, że to
    //   „rola nie działa”. W łańcuchu z CSRF (shopChain ma formularz logowania, więc CSRF jest włączone) każdy
    //   POST/PUT/DELETE w teście potrzebuje .with(csrf()).
    //
    // PUŁAPKA: MockMvc nie przekierowuje na stronę /error, więc ciało odpowiedzi 401/403 jest w teście PUSTE, choć
    //   prawdziwy serwer zwróciłby JSON {"status":403,...}. Statusy sprawdzisz MockMvc-iem; treść błędów — testem na
    //   prawdziwym porcie (RANDOM_PORT, jak w Sec01Basics i Sec04StatelessApi).

    // =================================================================================================
    // 3. @WithUserDetails — prawdziwy użytkownik z Twojego UserDetailsService
    // =================================================================================================
    //
    //   @WithUserDetails("anna")   → wczytuje annę z beana UserDetailsService (z @Import) — prawdziwe role (ROLE_ADMIN)
    // Hasła dalej nikt nie sprawdza, ale użytkownik musi ISTNIEĆ w Twoim źródle użytkowników, a jego uprawnienia są
    // takie, jak w „bazie”. Dobre, gdy kod używa Twojego typu principala (np. własnej klasy UserDetails).
    // Wymaga beana UserDetailsService w kontekście — w @WebMvcTest daje go @Import(Sec06SecurityConfig.class).

    // =================================================================================================
    // 4. Obróbka zapytań: httpBasic(), user(), jwt(), csrf()
    // =================================================================================================
    //
    // SecurityMockMvcRequestPostProcessors — dla JEDNEGO zapytania (zamiast adnotacji na całym teście):
    //   .with(httpBasic("anna", "anna-test-123"))   nagłówek Authorization → PRAWDZIWE logowanie przez filtr,
    //                                              UserDetailsService i PasswordEncoder; złe hasło → 401;
    //                                              uprawnienia [FACTOR_PASSWORD, ROLE_ADMIN] (Security 7)
    //   .with(user("piotr").roles("USER"))          użytkownik jak @WithMockUser, ale dla jednego zapytania
    //   .with(jwt())                                JwtAuthenticationToken bez podpisu i bez JwtDecoder; domyślnie
    //                                              sub = „user”, scope = „read” → SCOPE_read
    //   .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_stock:read")))   — uprawnienia wprost
    //   .with(jwt().jwt(token -> token.subject("hurtownia-abc").claim("scope", "stock:read")))   — claimy;
    //                                              uprawnienia wyliczone ze scope (SCOPE_stock:read)
    //   .with(csrf())                               poprawny token CSRF w zapytaniu
    //
    // DOBRA PRAKTYKA: przynajmniej jeden test na httpBasic() (albo prawdziwy token z Sec05Jwt), reszta — szybkie
    //   @WithMockUser/user()/jwt(). Dlaczego? Udawani użytkownicy testują reguły, ale nie wykryją zepsutego
    //   PasswordEncodera czy UserDetailsService.

    // =================================================================================================
    // 5. Test każdej reguły i test, że hasło nie wycieka
    // =================================================================================================
    //
    // Reguła z Sec06SecurityConfig                   → testy (Sec06SecurityTestingTest)
    //   GET /api/products — permitAll                → anonim 200
    //   POST /api/products — hasRole ADMIN (+ CSRF)  → anonim 401, USER 403, ADMIN + csrf() 201, ADMIN bez csrf() 403
    //   /api/me — authenticated                      → httpBasic 200, złe hasło 401
    //   /api/partner/stock — SCOPE_stock:read        → jwt z zakresem 200, zwykły jwt() 403, brak 401
    //   anyRequest().denyAll()                       → nawet ADMIN 403 (/api/stats, /api/partner/inne)
    // Test „hasło nie wycieka”: odpowiedź /api/me (po httpBasic) nie zawiera słowa password, hasła ani „$2a$”.
    //
    // DOBRA PRAKTYKA: dla KAŻDEJ reguły trzy pytania: kto NIE wejdzie (401), kto wejdzie, ale nie powinien mieć
    //   dostępu (403), kto wejdzie (200/201). Dlaczego? Pułapki z Sec02Authorization (kolejność, hasAuthority zamiast
    //   hasRole) nie wywracają startu aplikacji — wyłapie je tylko test.

    // =================================================================================================
    // 6. Pełny kontekst i zabezpieczenia metod
    // =================================================================================================
    //
    // @SpringBootTest + @AutoConfigureMockMvc ładuje CAŁĄ aplikację — konfiguracja bezpieczeństwa jest bez @Import,
    // a OrderService jest prawdziwym beanem z proxy @PreAuthorize (Sec06SecurityTestingFullContextTest). Wolniej niż
    // @WebMvcTest, ale sprawdza współpracę wszystkiego.
    // @WithMockUser działa też BEZ MockMvc — przy zwykłym wywołaniu metody serwisu:
    //   @WithMockUser(username = "piotr") → orderService.ordersOf("piotr") = [ZAM-002, ZAM-003],
    //                                       ordersOf("anna") → AuthorizationDeniedException
    //
    // PUŁAPKA: wywołanie zabezpieczonej metody, gdy w kontekście nie ma ŻADNEGO Authentication (test bez adnotacji,
    //   zadanie w tle, inny wątek — Sec03UsersPasswords). Wyrażenie SpEL nie da się policzyć: IllegalArgumentException
    //   „Failed to evaluate expression '#owner == authentication.name or hasRole('ADMIN')'”, a w środku
    //   AuthenticationCredentialsNotFoundException „An Authentication object was not found in the SecurityContext”.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @WebMvcTest(Kontroler.class) + @Import(TwojaSecurityConfig.class) + @MockitoBean dla serwisów.
     *   • Bez @Import → domyślny łańcuch Boota (wszystko 401). Pełny kontekst: @SpringBootTest + @AutoConfigureMockMvc.
     *   • @WithMockUser(username, roles, authorities) — bez logowania i bez sprawdzania istnienia; @WithAnonymousUser.
     *   • @WithUserDetails("login") — użytkownik z Twojego UserDetailsService.
     *   • .with(httpBasic(login, hasło)) — prawdziwe logowanie; .with(user(...)) — jak @WithMockUser na jedno zapytanie.
     *   • .with(jwt()) / jwt().authorities(...) / jwt().jwt(t -> t.claim("scope", ...)) — token bez podpisu.
     *   • POST/PUT/DELETE w łańcuchu z CSRF → .with(csrf()), inaczej 403.
     *   • MockMvc: puste ciało 401/403 (brak /error) — treść błędów testuj na RANDOM_PORT.
     *   • Własna adnotacja: @Retention(RUNTIME) + @WithMockUser(...) na adnotacji (meta-adnotacja).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego @WebMvcTest nie ładuje Twojej klasy z łańcuchem filtrów i jak to naprawić?
     *   2. Co się stanie? Test @WebMvcTest(ShopController.class) BEZ @Import wysyła GET /api/products (w aplikacji
     *      permitAll).
     *   3. ZNAJDŹ BŁĄD: @WithMockUser(roles = "ADMIN") i mockMvc.perform(post("/api/products").content(json)) zwraca 403.
     *      Programista zmienia regułę na permitAll, „bo rola nie działa”.
     *   4. Czym różni się @WithMockUser od .with(httpBasic(...))? Który wykryje zepsuty PasswordEncoder?
     *   5. Co się stanie? @WithMockUser(username = "nikt-taki") i GET /api/me — status i uprawnienia?
     *   6. Dlaczego test z MockMvc widzi puste ciało odpowiedzi 403, a curl na działającej aplikacji — JSON?
     *   7. Jak przetestować /api/partner/stock (Resource Server) bez wystawiania prawdziwego tokenu?
     *   8. Co się stanie? Test bez żadnej adnotacji użytkownika wywoła orderService.ordersOf("piotr") (pełny kontekst).
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w tej lekcji piszesz TESTY: Sec06SecurityTestingExercisesTest (src/test, ten sam pakiet)
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): test „niezalogowany GET /api/orders/piotr → 401”.
    //   Podpowiedź: Sec06SecurityTestingTest — wystarczy mockMvc.perform(get(...)).andExpect(status().isUnauthorized()).
    // ĆWICZENIE 2 (średnie): test „user("anna").roles("ADMIN") + csrf() → POST /api/products 201; bez csrf() → 403”.
    //   Podpowiedź: sekcje 2 i 4; ciało JSON produktu jak w stałej PRODUCT w Sec06SecurityTestingTest.
    // ĆWICZENIE 3 (średnie): test „jwt() z sub hurtownia-xyz i scope stock:read → 200 i $.partner = hurtownia-xyz;
    //   scope orders:read → 403”.
    //   Podpowiedź: jwt().jwt(token -> token.subject(...).claim("scope", ...)), jsonPath("$.partner").
    // ĆWICZENIE 4 (trudniejsze): uzupełnij adnotację WithMockAdmin (src/test) tak, żeby @WithMockAdmin oznaczało
    //   „anna z rolą ADMIN”; gotowy test exercise4 ma przejść.
    //   Podpowiedź: meta-adnotacja — @WithMockUser(...) napisane NAD @interface WithMockAdmin.
    // Rozwiązania: s12_security.solutions.sec06_security_testing (Sec06SecurityTestingSolutionsTest, SolutionWithMockAdmin).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @WebMvcTest skanuje tylko komponenty webowe (kontrolery, filtry, konwertery...). Klasa @Configuration
     *      z metodą @Bean SecurityFilterChain do nich nie należy, więc nie trafia do wycinka. Naprawa:
     *      @Import(Sec06SecurityConfig.class) albo pełny kontekst (@SpringBootTest + @AutoConfigureMockMvc).
     *   2. 401 — w kontekście jest tylko defaultSecurityFilterChain Boota (anyRequest().authenticated()), a nie nasza
     *      reguła permitAll. Sprawdzone testem Sec06SecurityTestingNoImportTest.
     *   3. Brakuje .with(csrf()) — 403 pochodzi z CsrfFilter, nie z reguły ról. Zmiana reguły na permitAll otwiera
     *      dziurę i nic nie naprawia (CSRF dalej zablokuje POST). Poprawka: dodać .with(csrf()) w teście. Sprawdzone
     *      testem Sec06SecurityTestingTest.adminWithoutCsrfToken.
     *   4. @WithMockUser wkłada gotowe Authentication — nie ma logowania, hasła ani UserDetailsService. httpBasic() wysyła
     *      nagłówek Authorization i przechodzi prawdziwe logowanie (filtr, UserDetailsService, PasswordEncoder) — tylko
     *      on wykryje zepsuty PasswordEncoder; widać też FACTOR_PASSWORD w uprawnieniach.
     *   5. 200 i {"username":"nikt-taki","authorities":["ROLE_USER"]} — @WithMockUser nie sprawdza istnienia
     *      użytkownika. Sprawdzone testem Sec06SecurityTestingTest.mockUserDoesNotNeedToExist.
     *   6. Security odpowiada przez response.sendError(...); prawdziwy Tomcat przekazuje wtedy zapytanie na stronę /error
     *      Boota (JSON), a MockMvc tego przekierowania nie wykonuje. Status jest ten sam, ciało — nie.
     *   7. .with(jwt()) ze spring-security-test: jwt().authorities(...) albo jwt().jwt(t -> t.claim("scope", ...)).
     *      Uwierzytelnienie trafia do kontekstu bez podpisu i bez JwtDecoder. Sprawdzone testem
     *      Sec06SecurityTestingTest.jwtPostProcessor.
     *   8. IllegalArgumentException „Failed to evaluate expression '#owner == authentication.name or hasRole('ADMIN')'”,
     *      a jej przyczyną jest AuthenticationCredentialsNotFoundException „An Authentication object was not found in the
     *      SecurityContext”. Sprawdzone testem Sec06SecurityTestingFullContextTest.noUserAtAll.
     */
    // </editor-fold>
}

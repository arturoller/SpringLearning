package s12_security.sec06_security_testing;

/**
 * <pre>
 * TEMAT: Testowanie zabezpieczeń — @WithMockUser, @WithAnonymousUser, @WithUserDetails, post-procesory MockMvc (user, httpBasic,
 *        csrf, jwt), testy bezpieczeństwa metod, macierz dostępu, test z prawdziwym tokenem i pułapka addFilters = false
 *        (mock user = udawany użytkownik; anonymous = anonim; post-processor = "obróbka po": dopisuje coś do żądania
 *         testowego; access matrix = macierz/tabela dostępu; end-to-end = od końca do końca)
 *
 * W SKRÓCIE:
 *   Zabezpieczenia to kod jak każdy inny — i psują się po cichu (zła kolejność reguł, zapomniany adres, wyłączony CSRF).
 *   Spring Security Test (moduł {@code spring-security-test}, w Boot 4 starter {@code spring-boot-starter-security-test})
 *   daje dwa narzędzia: ADNOTACJE na teście ({@code @WithMockUser} — "ten test wykonuje anna z rolą CUSTOMER") i
 *   POST-PROCESORY na pojedynczym żądaniu MockMvc ({@code .with(user("anna"))}, {@code .with(httpBasic(…))},
 *   {@code .with(csrf())}, {@code .with(jwt())}). Testuj każdą regułę z OBU stron: kto wchodzi i kto NIE wchodzi — najlepiej
 *   jako tabelę (macierz). Przynajmniej jeden test niech przejdzie całą drogę: hasło → token → API na prawdziwym serwerze.
 *   I nigdy nie "naprawiaj" czerwonych testów przez {@code addFilters = false} — to wyłącza całe bezpieczeństwo.
 *
 * ANALOGIA: próbny alarm w biurowcu.
 *   Nie czekasz na prawdziwego włamywacza. Strażnik dostaje kartę "na niby" z wybranym poziomem dostępu (@WithMockUser) i
 *   próbuje KAŻDYCH drzwi (macierz): te, które mają się otworzyć, i te, które mają zostać zamknięte. Raz w roku robisz
 *   pełną próbę z prawdziwą kartą z recepcji (test z prawdziwym tokenem). Kto na czas próby wyłącza wszystkie zamki
 *   (addFilters = false), ten "zalicza" próbę, ale niczego nie sprawdził.
 *
 * JAK TO DZIAŁA:
 *   1. {@code @WithMockUser} / {@code @WithUserDetails} / {@code @WithAnonymousUser}: słuchacz testów
 *      ({@code WithSecurityContextTestExecutionListener}) przed testem wkłada gotowe {@code Authentication} do
 *      {@code TestSecurityContextHolder} (i {@code SecurityContextHolder}) — bez logowania, bez hasła.
 *   2. MockMvc z Spring Security ({@code @WebMvcTest}/{@code @AutoConfigureMockMvc} same to włączają) czyta ten kontekst:
 *      filtry widzą użytkownika, jakby się zalogował. Post-procesor robi to samo dla jednego żądania; {@code httpBasic(…)} i
 *      {@code csrf()} dopisują prawdziwy nagłówek / token.
 *   3. Bezpieczeństwo metod działa w proxy beana — każdy test, który woła bean z kontekstu, je sprawdza; {@code new Serwis()}
 *      — nie.
 *   4. Po żądaniu MockMvc filtry czyszczą {@code SecurityContextHolder} (ten sam wątek!) — czytaj kontekst przed żądaniem.
 *
 * SŁÓWKA:
 *   mock = atrapa; anonymous = anonimowy; user details = szczegóły użytkownika; post-processor = post-procesor (dopisuje dane
 *   do żądania); request = żądanie; parameterized test = test sparametryzowany (jeden test, wiele zestawów danych);
 *   csv = wartości rozdzielone przecinkami; matrix = macierz, tabela; filter = filtr; slice = wycinek (test części aplikacji);
 *   end-to-end = od początku do końca; false positive = fałszywy alarm "wszystko OK".
 *
 * ZOBACZ TEŻ: s12_security/Sec02Authorization (reguły adresów i metod, które tu testujemy),
 *             s12_security/Sec04StatelessApi (CSRF, sesja), s12_security/Sec05Jwt (tokeny),
 *             s11_testing/Test01TestSlices (wycinki testów), s11_testing/Test03MockMvcTester (MockMvcTester),
 *             s11_testing/Test02MockitoBeans (@MockitoBean — atrapy beanów).
 * </pre>
 */
public final class Sec06SecurityTesting {

    private Sec06SecurityTesting() {
    }

    // =================================================================================================
    // 1. ADNOTACJE: @WithMockUser, @WithAnonymousUser, @WithUserDetails (Sec06AnnotationsTest)
    // =================================================================================================
    //
    // Testowana aplikacja (Sec06SecurityConfig) to powtórka: {@code /sec06/token} z Basic, panel {@code /sec06/panel/**}
    // z sesją i CSRF, reszta — API z JWT; OrderService z {@code @PreAuthorize}; hierarchia ADMIN > EMPLOYEE > CUSTOMER.
    //
    //     @WebMvcTest
    //     @Import({Sec06SecurityConfig.class, OrderService.class})   // łańcuchy + prawdziwy serwis (z proxy)
    //     @WithMockUser(username = "anna", roles = "CUSTOMER")      // NA KLASIE: domyślny użytkownik każdego testu
    //     class Sec06AnnotationsTest {
    //         @Test void classLevelUser() { … }                       // anna
    //         @Test @WithMockUser(username = "boss", roles = "ADMIN") void methodLevelOverrides() { … }   // boss
    //         @Test @WithAnonymousUser void anonymous() { … }        // anonim mimo adnotacji na klasie
    //         @Test @WithUserDetails("ewa") void realUserDetails() { … }  // ewa z UserDetailsService
    //     }
    //
    //     | adnotacja                       | skąd użytkownik                          | kiedy                                       |
    //     |---------------------------------|------------------------------------------|---------------------------------------------|
    //     | @WithMockUser(username, roles)  | zmyślony (hasło "password"); nie musi istnieć | reguły adresów i metod — najczęściej        |
    //     | @WithAnonymousUser              | anonim (AnonymousAuthenticationToken)    | "co widzi niezalogowany", nadpisanie klasy  |
    //     | @WithUserDetails("ewa")         | PRAWDZIWY UserDetailsService z kontekstu | role/pola z bazy, własny UserDetails        |
    //
    //   • {@code @WithMockUser} bez parametrów = użytkownik "user" z rolą USER. {@code roles} dopisuje {@code ROLE_},
    //     {@code authorities} — nie (tak samo jak w Sec02Authorization; nie podawaj obu naraz).
    //   • {@code @WithMockUser(username = "nikt")} przechodzi, choć takiego konta nie ma (test mockUserNeedNotExist) — to zaleta
    //     (szybko) i wada (nie sprawdzisz, czy użytkownik naprawdę ma te role). Do tego służy {@code @WithUserDetails}.
    //   • Uprawnienia z adnotacji to DOKŁADNIE to, co podasz — bez {@code FACTOR_PASSWORD}, które Security 7 dodaje po prawdziwym
    //     logowaniu hasłem (test realUserDetails: tylko ROLE_EMPLOYEE). Reguły oparte na czynnikach logowania testuj prawdziwym
    //     logowaniem ({@code httpBasic}).
    //
    // PUŁAPKA: odczyt {@code SecurityContextHolder} PO żądaniu MockMvc. Dlaczego? MockMvc wykonuje filtry w tym samym wątku co
    //   test, a łańcuch na końcu żądania czyści kontekst — {@code getAuthentication()} zwraca wtedy {@code null}. Czytaj przed
    //   żądaniem (komentarz w mockUserNeedNotExist).
    //
    // PUŁAPKA: testy zależne od kolejności przez stan współdzielonego kontekstu. Dlaczego? Testy z tą samą konfiguracją dzielą
    //   jeden kontekst Springa (cache). Po prawdziwym logowaniu ewy hasłem DaoAuthenticationProvider sam zamienił jej
    //   {@code {noop}} na {@code {bcrypt}} (upgrade hasła, Sec03UsersPasswords) — test sprawdzający dokładny hash raz
    //   przechodził, raz nie (zależnie od kolejności klas). Asercje pisz na tym, co stałe.

    // =================================================================================================
    // 2. POST-PROCESORY: user(), httpBasic(), csrf(), jwt(), anonymous() (Sec06PostProcessorsTest,
    //    sec06_pitfalls: Sec06JwtClaimsPitfallTest)
    // =================================================================================================
    //
    // Klasa {@code SecurityMockMvcRequestPostProcessors} (pakiet {@code org.springframework.security.test.web.servlet.request}):
    //
    //     mvc.get().uri("/sec06/orders/of/piotr").with(user("anna").roles("CUSTOMER"))         // gotowy użytkownik → 403
    //     mvc.get().uri("/sec06/orders/of/piotr").with(anonymous())                             // anonim → 401
    //     mvc.post().uri("/sec06/token").with(httpBasic("ewa", "haslo-testowe"))                 // prawdziwy nagłówek Basic
    //     mvc.post().uri("/sec06/panel/price")…with(user("ewa").roles("EMPLOYEE")).with(csrf()) // + poprawny token CSRF
    //     mvc.get().uri("/sec06/admin/stats").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
    //     mvc.get().uri("/sec06/orders").with(jwt().jwt(token -> token.subject("piotr")))       // twierdzenia tokenu
    //
    //   • Ten sam {@code .with(…)} działa w {@code MockMvcTester} i w klasycznym
    //     {@code MockMvc.perform(get(…).with(…)).andExpect(status().isOk())} (test classicMockMvc — tak wygląda większość
    //     poradników).
    //   • Różnica: {@code user(…)} i {@code jwt()} wkładają GOTOWE uwierzytelnienie (hasło i podpis nie są sprawdzane), a
    //     {@code httpBasic(…)} wysyła nagłówek, który sprawdza prawdziwy UserDetailsService (złe hasło → 401).
    //   • {@code jwt()} nie potrzebuje klucza ani dekodera — test nie zależy od tego, jak tokeny są wydawane.
    //   • {@code csrf()} jest potrzebne TYLKO tam, gdzie CSRF jest włączony (panel). {@code csrf().useInvalidToken()} — zły token.
    //
    // PUŁAPKA: {@code with(jwt().jwt(j -> j.claim("roles", List.of("ADMIN"))))} i zdziwienie, że admin dostaje 403. Dlaczego?
    //   Post-procesor NIE używa konwertera aplikacji — sam liczy uprawnienia domyślnie, z {@code scope} (test
    //   jwtPostProcessorIgnoresAppConverter). Podaj uprawnienia wprost ({@code .authorities(new SimpleGrantedAuthority(…))})
    //   albo ten sam konwerter co aplikacja ({@code .authorities(Sec06SecurityConfig.rolesAuthorities())}).
    //
    // DOBRA PRAKTYKA: adnotacja, gdy cały test to jeden użytkownik; post-procesor, gdy w jednym teście porównujesz kilku
    //   (anna 403, piotr 200). Dlaczego? Test czyta się jak zdanie: "piotr pyta o zamówienia piotra → 200".

    // =================================================================================================
    // 3. TESTY BEZPIECZEŃSTWA METOD BEZ WEBA (Sec06MethodSecurityTest)
    // =================================================================================================
    //
    //     @SpringJUnitConfig(classes = {OrderService.class, MethodSecurityTestConfig.class})   // malutki kontekst
    //     class Sec06MethodSecurityTest {
    //         @Autowired OrderService orders;                                                  // bean = proxy
    //
    //         @Test @WithMockUser(username = "anna", roles = "CUSTOMER")
    //         void deniedForOtherCustomer() {
    //             assertThatThrownBy(() -> orders.ordersOf("piotr")).isInstanceOf(AuthorizationDeniedException.class);
    //         }
    //     }
    //
    //   • {@code MethodSecurityTestConfig} to klasa z {@code @EnableMethodSecurity} (i hierarchią ról) BEZ {@code @Configuration}
    //     — podana jawnie, więc nie zeskanuje jej żaden {@code @SpringBootTest} lekcji. Kontekst startuje w ułamku sekundy.
    //   • Odmowa: {@code AuthorizationDeniedException} (Security 6.3+, podklasa {@code AccessDeniedException}) z komunikatem
    //     "Access Denied". Bez użytkownika: {@code AuthenticationCredentialsNotFoundException} (test noUser).
    //   • Przez HTTP te same odmowy stają się 403/401 (macierz w sekcji 4: DELETE zamówienia jako ewa → 403).
    //
    // PUŁAPKA: test jednostkowy serwisu z {@code new OrderService()}. Dlaczego? Reguły sprawdza proxy tworzone przez Springa,
    //   a {@code new} daje goły obiekt — anonim "anuluje" zamówienie i test jest zielony (test newBypassesSecurity). Logikę
    //   serwisu możesz testować przez {@code new}, ale reguły bezpieczeństwa — tylko na beanie z kontekstu.

    // =================================================================================================
    // 4. MACIERZ DOSTĘPU: @ParameterizedTest + @CsvSource (Sec06AccessMatrixTest)
    // =================================================================================================
    //
    //     @ParameterizedTest(name = "{0} {1} jako {2} → {3}")
    //     @CsvSource(textBlock = """
    //             # metoda, adres,                kto,    status
    //             POST,   /sec06/products,        anonim, 401
    //             POST,   /sec06/products,        anna,   403
    //             POST,   /sec06/products,        ewa,    201
    //             DELETE, /sec06/orders/1,        ewa,    403
    //             GET,    /sec06/internal/debug,  admin,  403
    //             …
    //             """)
    //     void accessMatrix(String method, String path, String who, int expectedStatus) {
    //         assertThat(mvc.method(HttpMethod.valueOf(method)).uri(path).with(who(who))).hasStatus(expectedStatus);
    //     }
    //
    //   • Każdy wiersz to osobny test z czytelną nazwą w raporcie: "POST /sec06/products jako anna → 403".
    //   • Tabela pokazuje też odmowy: anonim 401, zalogowany bez roli 403, adres bez reguły 403 nawet dla admina.
    //
    // DOBRA PRAKTYKA: testuj reguły jako tabelę "każdy adres × każda rola", także wiersze z ODMOWĄ. Dlaczego? Większość błędów
    //   bezpieczeństwa to "za dużo wolno" (zła kolejność reguł, Sec02Authorization), a testy pisane "pod szczęśliwą ścieżkę"
    //   sprawdzają tylko, że uprawniony wchodzi. Tabelę łatwo przejrzeć na code review i porównać z wymaganiami.
    //
    // PYTANIE REKRUTACYJNE: "Jak testujesz zabezpieczenia w Spring Boot?" — wycinek {@code @WebMvcTest} z zaimportowaną
    //   konfiguracją bezpieczeństwa, {@code @WithMockUser}/post-procesory dla ról, macierz statusów 200/401/403 (także odmowy),
    //   testy metod na beanie z proxy i co najmniej jeden test end-to-end z prawdziwym logowaniem/tokenem.

    // =================================================================================================
    // 5. TEST "PRAWDZIWY" (RANDOM_PORT) I PUŁAPKA addFilters = false (Sec06ServerTest,
    //    sec06_pitfalls: Sec06NoFiltersPitfallTest)
    // =================================================================================================
    //
    // Atrapy ({@code @WithMockUser}, {@code jwt()}) pomijają logowanie i podpis. Jeden test powinien przejść całą drogę:
    //
    //     String ewa = tokenFor("ewa");                               // POST /sec06/token z Basic → access_token
    //     client().get().uri("/sec06/orders/of/piotr").header("Authorization", "Bearer " + ewa)…   // 200
    //
    //   Prawdziwy Tomcat na losowym porcie ({@code RANDOM_PORT}, {@code @LocalServerPort}), {@code RestClient}, prawdziwy
    //   klucz, dekoder i filtry. Tylko taki test wykryje np. zły klucz w dekoderze albo zły konwerter ról.
    //
    // PUŁAPKA: {@code @AutoConfigureMockMvc(addFilters = false)} "bo testy dostają 401". Dlaczego to zło? MockMvc pomija WSZYSTKIE
    //   filtry, czyli cały Spring Security z sieci: anonim wchodzi do panelu admina (200), POST bez CSRF przechodzi (200) — testy
    //   są zielone, choć reguły mogłyby nie istnieć (test everythingIsOpen). Bezpieczeństwo metod zostaje (to proxy), ale bez
    //   filtrów nie ma nawet anonima: zamiast 401 lecą wyjątki ({@code AuthenticationCredentialsNotFoundException},
    //   "Failed to evaluate expression" dla {@code authentication.name} — test methodSecurityStillOnButNo401). Naprawa: dodaj
    //   użytkownika ({@code @WithMockUser}, {@code with(user(…))}), a nie wyłączaj zabezpieczeń.
    //
    // DOBRA PRAKTYKA: test, który MA zobaczyć 401/403, jest tak samo ważny jak test na 200. Dlaczego? Gdyby ktoś wyłączył filtry
    //   albo pomylił reguły, testy "na 200" dalej by przechodziły — padnie dopiero test odmowy.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @WithMockUser(username = "anna", roles = "CUSTOMER") — na klasie (wszystkie testy) albo metodzie (nadpisuje);
     *     użytkownik zmyślony, hasło "password", bez FACTOR_PASSWORD.
     *   • @WithAnonymousUser — anonim; @WithUserDetails("ewa") — prawdziwy UserDetailsService z kontekstu.
     *   • .with(user("anna").roles("CUSTOMER")), .with(anonymous()), .with(httpBasic("ewa", "haslo")) (prawdziwe hasło!),
     *     .with(csrf()) / csrf().useInvalidToken(), .with(jwt().authorities(...)) / jwt().jwt(j -> j.subject(...)).
     *   • jwt() NIE używa konwertera aplikacji — .authorities(konwerter albo gotowe uprawnienia).
     *   • Metody: @SpringJUnitConfig(serwis + klasa z @EnableMethodSecurity) → AuthorizationDeniedException /
     *     AuthenticationCredentialsNotFoundException; new Serwis() omija reguły.
     *   • Macierz: @ParameterizedTest + @CsvSource(textBlock) — adres × użytkownik → status, z odmowami.
     *   • Jeden test RANDOM_PORT: hasło → token → API. Nigdy addFilters = false w testach bezpieczeństwa.
     *   • SecurityContextHolder czytaj PRZED żądaniem MockMvc — po żądaniu jest wyczyszczony.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? Klasa testowa ma {@code @WithMockUser(roles = "CUSTOMER")}, a metoda {@code @WithAnonymousUser} i woła
     *      {@code GET /sec06/orders}. Jaki status?
     *   2. Czym różni się {@code @WithMockUser(username = "ewa")} od {@code @WithUserDetails("ewa")}? Kiedy test z pierwszą
     *      adnotacją przejdzie, a z drugą — nie?
     *   3. ZNAJDŹ BŁĄD: test {@code with(jwt().jwt(j -> j.claim("roles", List.of("ADMIN"))))} na {@code /sec06/admin/stats}
     *      dostaje 403, a aplikacja z prawdziwym tokenem admina działa. Co poprawić w teście?
     *   4. Co się stanie? Test tworzy {@code new OrderService()} i bez żadnego użytkownika woła {@code cancel(1)}
     *      (metoda ma {@code @PreAuthorize("hasRole('ADMIN')")}).
     *   5. ZNAJDŹ BŁĄD: "testy kontrolerów dawały 401, więc dodałem {@code @AutoConfigureMockMvc(addFilters = false)} i jest
     *      zielono". Co teraz sprawdzają te testy?
     *   6. Co się stanie? Anonim wysyła {@code POST /sec06/panel/price} z poprawnym tokenem CSRF ({@code with(csrf())}).
     *      A bez tokenu?
     *   7. PYTANIE REKRUTACYJNE: jak przetestować reguły dostępu do wszystkich punktów końcowych, żeby test był czytelny
     *      i łatwy do rozbudowy?
     *   8. Dlaczego warto mieć choć jeden test na RANDOM_PORT z prawdziwym tokenem, skoro {@code with(jwt())} jest szybsze?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielety są w src/TEST (to narzędzia testów, korzystają ze spring-security-test): TestUsers, PanelRules,
    // ExerciseTokens, ExerciseSecurityContext (pakiet s12_security.sec06_security_testing); test:
    // Sec06SecurityTestingExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Rozwiązania wzorcowe: src/test, pakiet s12_security.solutions.sec06_security_testing (test Sec06SolutionsTest).
    //
    // ĆWICZENIE 1 (łatwe): TestUsers.customer(username) i TestUsers.adminToken() — wspólne post-procesory użytkowników testowych.
    //   Podpowiedź: {@code user(username).roles("CUSTOMER")}; {@code jwt().jwt(j -> j.subject("admin")).authorities(…)}.
    //
    // ĆWICZENIE 2 (średnie): PanelRules.expectedStatus(who, withCsrf) — PRZEWIDŹ status {@code POST /sec06/panel/price} dla
    //   anonima, anny, ewy i admina, z tokenem CSRF i bez. Test porówna przewidywania z aplikacją (8 przypadków).
    //   Podpowiedź: który filtr stoi wcześniej — CsrfFilter czy AuthorizationFilter (Sec04StatelessApi, sekcja 2)? Pamiętaj
    //   o hierarchii ról.
    //
    // ĆWICZENIE 3 (średnie): ExerciseTokens.bearerFor(mvc, username) — POST /sec06/token z {@code httpBasic}, odczyt
    //   {@code access_token} z JSON i wynik {@code "Bearer " + token}. Podpowiedź: {@code mvc.post()….exchange()} zwraca
    //   {@code MvcTestResult}; {@code getResponse().getContentAsByteArray()}; {@code JsonMapper.builder().build().readTree(…)}.
    //
    // ĆWICZENIE 4 (trudniejsze): ExerciseSecurityContext.runAs(username, role, action) — to, co w środku robi
    //   {@code @WithMockUser}. Podpowiedź: zapamiętaj {@code SecurityContextHolder.getContext()}, utwórz NOWY
    //   ({@code createEmptyContext()}) z {@code UsernamePasswordAuthenticationToken.authenticated(…)}, ustaw, wykonaj akcję
    //   i przywróć poprzedni w {@code finally} (test sprawdza sprzątanie także po odmowie dostępu).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 401 — adnotacja na metodzie zastępuje tę z klasy, więc test wykonuje anonim, a /sec06/orders wymaga logowania
     *      (test anonymous).
     *   2. @WithMockUser tworzy zmyślonego użytkownika z podanymi rolami (hasło "password") — konto nie musi istnieć.
     *      @WithUserDetails ładuje użytkownika z prawdziwego UserDetailsService (role i hasło z serwisu). Test z
     *      @WithMockUser(username = "nikt") przejdzie, @WithUserDetails("nikt") — nie: test kończy się, zanim się zacznie
     *      (IllegalStateException "Unable to create SecurityContext…", przyczyna UsernameNotFoundException — sprawdzone).
     *   3. jwt() liczy uprawnienia domyślnym konwerterem (scope → SCOPE_…), a nie konwerterem aplikacji, więc roles nic nie daje
     *      (Sec06JwtClaimsPitfallTest). Dodaj .authorities(Sec06SecurityConfig.rolesAuthorities()) albo
     *      .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).
     *   4. Metoda się wykona i zwróci "Anulowano zamówienie 1" — obiekt z new nie ma proxy, więc @PreAuthorize nikt nie sprawdza
     *      (test newBypassesSecurity). Reguły testuj na beanie z kontekstu.
     *   5. Prawie nic z bezpieczeństwa: MockMvc pomija wszystkie filtry, więc reguły adresów, logowanie i CSRF nie działają —
     *      anonim wchodzi do panelu admina (Sec06NoFiltersPitfallTest). Zostają tylko reguły metod, które bez filtrów kończą się
     *      wyjątkami zamiast 401. Zamiast tego: @WithMockUser albo with(user(...)).
     *   6. Z tokenem: 401 (CSRF przeszedł, ale panel wymaga zalogowanego EMPLOYEE). Bez tokenu: 403 — CsrfFilter odrzuca
     *      żądanie, zanim dojdzie do logowania (test wzorcowy ćwiczenia 2).
     *   7. Macierz: @ParameterizedTest z @CsvSource (metoda, adres, użytkownik, oczekiwany status) — każdy wiersz to test
     *      z czytelną nazwą; nowa reguła lub rola to nowe wiersze, a tabela pokazuje także odmowy (Sec06AccessMatrixTest).
     *   8. jwt() i @WithMockUser pomijają logowanie, wydawanie tokenu, podpis, dekoder i konwerter ról. Tylko test z prawdziwym
     *      tokenem wykryje np. inny klucz w dekoderze, zły konwerter albo brakujący łańcuch dla /token (Sec06ServerTest).
     */
    // </editor-fold>
}

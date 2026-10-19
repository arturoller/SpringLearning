package s21_capstone.cap03_library_with_auth;

/**
 * <pre>
 * TEMAT: Projekt końcowy 3 — biblioteka z logowaniem: książki, wypożyczenia i kary liczone z Clock, role
 *        BIBLIOTEKARZ i CZYTELNIK, logowanie zwracające JWT, hasła BCrypt, @PreAuthorize i testy 401/403/200
 *        (JWT = JSON Web Token, podpisany żeton; BCrypt = algorytm skrótu haseł; authorization = autoryzacja,
 *         czyli „co wolno”; authentication = uwierzytelnienie, czyli „kim jesteś”)
 *
 * W SKRÓCIE:
 *   Czytelnik i bibliotekarz logują się loginem i hasłem (POST /api/auth/login) i dostają token JWT, który potem
 *   wysyłają w nagłówku Authorization: Bearer. Hasła w bazie to wyłącznie skróty BCrypt. Uprawnienia sprawdzamy na
 *   DWÓCH poziomach: reguły URL w łańcuchu filtrów („wypożycza tylko bibliotekarz”) i adnotacje na metodach serwisów,
 *   gdy decyzja zależy od DANYCH („czytelnik widzi tylko SWOJE wypożyczenia”). Kary za spóźnienie i ważność tokenów
 *   liczymy jednym wstrzykniętym zegarem. Każdy błąd — także 401 i 403 z wnętrza kontrolera — to ProblemDetail.
 *
 * ANALOGIA: biblioteka z czytnikiem kart.
 *   Przy wejściu pokazujesz dowód (login i hasło) i dostajesz kartę wstępu ważną godzinę (token). Bramka (łańcuch
 *   filtrów) sprawdza tylko kartę: czy prawdziwa, czy ważna, czy masz dostęp do działu (rola). Przy ladzie
 *   bibliotekarka (adnotacje na serwisie) sprawdza więcej: „to TWOJE konto wypożyczeń?”. Kartoteka haseł nie zawiera
 *   haseł, tylko ich odciski (BCrypt) — kradzież kartoteki nie daje haseł.
 *
 * JAK TO DZIAŁA:
 *   1. POST /api/auth/login {"username":"jan","password":"jan-test-123"} → AuthController → AuthenticationManager
 *      (DaoAuthenticationProvider: LibraryUserDetailsService czyta library_users, DelegatingPasswordEncoder porównuje
 *      hasło ze skrótem {bcrypt}...) → TokenService (JwtEncoder, klucz prywatny RSA, Clock) → {"access_token":"eyJ..."}.
 *   2. GET /api/readers/maria/loans + Authorization: Bearer eyJ... → BearerTokenAuthenticationFilter → JwtDecoder
 *      (podpis, exp liczony TYM SAMYM zegarem, iss) → JwtAuthenticationConverter (claim roles → ROLE_CZYTELNIK)
 *      → AuthorizationFilter (reguła URL: authenticated — OK) → LoanController → proxy LoanService:
 *      @PreAuthorize("hasRole('BIBLIOTEKARZ') or #username == authentication.name") → „maria” ≠ „jan”
 *      → AuthorizationDeniedException → LibraryProblemAdvice → 403 {"code":"ACCESS_DENIED",...}.
 *
 * SŁÓWKA:
 *   authentication = uwierzytelnienie (kim jesteś); authorization = autoryzacja (co ci wolno); credentials = dane
 *   logowania; password hash = skrót hasła; salt = sól (losowy dodatek do hasła przed skrótem); bearer token = token
 *   okaziciela; claim = pole tokenu; resource server = serwer zasobów (API przyjmujące tokeny); user enumeration =
 *   wyliczanie kont (zgadywanie, które loginy istnieją); defense in depth = obrona w głąb (kilka warstw zabezpieczeń);
 *   call number = sygnatura (numer egzemplarza); fine = kara; overdue = po terminie; loan = wypożyczenie.
 *
 * ZOBACZ TEŻ: s12_security/Sec02Authorization (@PreAuthorize, proxy), s12_security/Sec03UsersPasswords (BCrypt,
 *             DelegatingPasswordEncoder), s12_security/Sec04StatelessApi (bez sesji, CSRF), s12_security/Sec05Jwt (JWT),
 *             s12_security/Sec06SecurityTesting (jwt(), @WithMockUser), s02_configuration/Cfg02ConfigurationProperties,
 *             s21_capstone/Cap01ShopApi i s21_capstone/Cap02Reservations (architektura, Clock, @Version),
 *             t31_jdk_toolbox/Toolbox02HashingSecurity (skróty i sól).
 * </pre>
 */
public final class Cap03LibraryWithAuth {

    private Cap03LibraryWithAuth() {
    }

    // =================================================================================================
    // 1. SPECYFIKACJA — role, wymagania, punkty końcowe (testy: Cap03LibraryWithAuthTest, Cap03LibraryWithAuthWebSliceTest)
    // =================================================================================================
    //
    // Role: BIBLIOTEKARZ (anna) — prowadzi katalog i wypożyczenia; CZYTELNIK (jan, maria, adam) — przegląda katalog
    // i SWOJE wypożyczenia. Hasła testowe: login-test-123 (np. jan-test-123) — w bazie tylko ich skróty.
    //
    // Wymagania:
    //   W1. Katalog publiczny (bez logowania); dodawanie egzemplarzy — bibliotekarz.
    //   W2. Logowanie loginem i hasłem → token JWT ważny 1 godzinę; zły login i złe hasło → ten sam 401.
    //   W3. Wypożyczenie (bibliotekarz): termin = dziś + 14 dni; czytelnik bez zaległości i z mniej niż 3 książkami;
    //       egzemplarz na półce. Zwrot: kara 0.50 zł za każdy dzień po terminie.
    //   W4. Czytelnik widzi tylko SWOJE wypożyczenia (lista i pojedyncze); bibliotekarz — wszystkie.
    //   W5. Liczby (14 dni, 0.50 zł, 3 książki) w konfiguracji, nie w kodzie (LibraryRules).
    //
    // Punkty końcowe (kto → wynik):
    //   POST /api/auth/login              wszyscy        200 token | 400 | 401 BAD_CREDENTIALS
    //   GET  /api/me                      zalogowany     200 {username, authorities}
    //   GET  /api/books, /api/books/{id}  wszyscy        200 (z polem available, bez danych wypożyczającego)
    //   POST /api/books                   BIBLIOTEKARZ   201 | 400 | 401 | 403 | 409 CALL_NUMBER_TAKEN
    //   POST /api/loans                   BIBLIOTEKARZ   201 | 400 | 404 | 409 BOOK_ON_LOAN, READER_HAS_OVERDUE, LOAN_LIMIT
    //   POST /api/loans/{id}/return       BIBLIOTEKARZ   200 z karą | 404 | 409 ALREADY_RETURNED
    //   GET  /api/loans/{id}              bibliotekarz lub właściciel   200 | 403 | 404
    //   GET  /api/readers/{login}/loans   bibliotekarz lub ten czytelnik 200 | 403
    //   GET  /api/me/loans                zalogowany     200 swoje
    //
    // Model danych (V1–V3): library_users (username, password_hash, role); books (call_number, on_loan, version);
    //   loans (book, reader, loaned_on, due_on, returned_on, fine, extended); book_reservations (ćwiczenie 3).

    // =================================================================================================
    // 2. LOGOWANIE, HASŁA I TOKENY (testy: Cap03LibraryWithAuthTest #2)
    // =================================================================================================
    //
    // Hasła: DelegatingPasswordEncoder (PasswordEncoderFactories.createDelegatingPasswordEncoder()). Skrót w bazie:
    //     {bcrypt}$2a$10$kzAtenLZ9YyysA9oUs0MmuQ6q7BIh9Dyf8tBbhCu.dzwCk1NvTFK2      (hasło jan-test-123)
    //     {bcrypt} — który algorytm; $2a — wersja BCrypt; $10 — koszt (2^10 rund); dalej sól i skrót.
    // Ten sam login i hasło dają za każdym razem INNY skrót (losowa sól), więc w migracji jest gotowy skrót, wygenerowany
    // raz — a test passwordsAreBcryptHashes sprawdza, że encoder.matches("jan-test-123", skrót) == true.
    //
    // Logowanie: AuthenticationManager = ProviderManager(DaoAuthenticationProvider(LibraryUserDetailsService) + encoder).
    // AuthController wywołuje authenticate(UsernamePasswordAuthenticationToken.unauthenticated(login, hasło)) i przy
    // sukcesie wystawia token. Dla złego hasła ORAZ nieistniejącego loginu leci BadCredentialsException („Bad
    // credentials”) — DaoAuthenticationProvider domyślnie ukrywa UsernameNotFoundException — a porada daje w obu
    // przypadkach 401 {"detail":"Nieprawidłowy login lub hasło","code":"BAD_CREDENTIALS"} (test loginIssuesJwt).
    //
    // Token: claimy iss=https://biblioteka.example, sub=login, roles=["BIBLIOTEKARZ"], iat, exp (= iat + 1 h). Klucze RSA
    // powstają przy starcie (klucz testowy, nic w repozytorium); dekoder sprawdza podpis, wystawcę i ważność TYM SAMYM
    // zegarem, którym TokenService wystawia token. JwtAuthenticationConverter zamienia roles na ROLE_..., a Security 7
    // dokłada FACTOR_BEARER: GET /api/me dla jana → ["FACTOR_BEARER","ROLE_CZYTELNIK"].
    // Token przeterminowany → 401, puste ciało i nagłówek (z testu expiredToken):
    //     WWW-Authenticate: Bearer error="invalid_token", error_description="An error occurred while attempting to decode
    //     the Jwt: Jwt expired at 2026-10-19T07:00:00Z", ...
    //
    // PUŁAPKA: zegar demonstracyjny a tokeny. Profil cap03 zatrzymuje zegar na 2026-10-19 10:00 — token wystawiony
    //   w aplikacji demo NIGDY się nie przeterminuje (czas stoi). Dlaczego to ważne? To ustawienie tylko do nauki i testów;
    //   w produkcji zegar musi iść, inaczej exp traci sens.
    //
    // DOBRA PRAKTYKA: w tokenie tylko login i role — bez hasła, emaila, pełnego nazwiska. Dlaczego? Ładunek JWT czyta
    //   każdy (to tylko Base64URL — s12_security/Sec05Jwt); test loginIssuesJwt dekoduje go bez żadnego klucza.

    // =================================================================================================
    // 3. DOMENA — wypożyczenia, kary z Clock, zasady w konfiguracji, @Version (testy: Cap03LibraryWithAuthUnitTest,
    //    Cap03LibraryWithAuthRepositoryTest, Cap03LibraryWithAuthTest.lendAndReturn)
    // =================================================================================================
    //
    // LibraryRules = rekord @ConfigurationProperties(prefix = "cap03.library"): loan-days=14, fine-per-day=0.50,
    // max-active-loans=3. FineCalculator to czysta funkcja (termin, dzień): dni po terminie × stawka. Na „dziś”
    // 2026-10-19: Jan (termin 15.10) — 4 dni, 2.00 zł; Maria (termin 15.09) — 34 dni, 17.00 zł; w dniu terminu — 0.
    //
    // Wypożyczenie egzemplarza ustawia books.on_loan = true. Dwóch bibliotekarzy wypożyczających TEN SAM egzemplarz
    // zmienia TEN SAM wiersz, więc tu @Version działa (w przeciwieństwie do dwóch nowych rezerwacji w Cap02Reservations):
    // drugi zapis → ObjectOptimisticLockingFailureException → porada → 409 CONCURRENT_MODIFICATION. Test repozytorium
    // pokazuje to na dwóch kopiach tego samego wiersza (version 0) — druga dostaje wyjątek, w bazie version = 1.
    //
    // DOBRA PRAKTYKA: liczby biznesowe (termin, kara, limit) w konfiguracji, a nie „magiczne stałe”. Dlaczego? Zmiana
    //   regulaminu biblioteki to zmiana pliku .properties, a test jednostkowy podaje własne LibraryRules bez Springa.

    // =================================================================================================
    // 4. AUTORYZACJA NA DWÓCH POZIOMACH (testy: Cap03LibraryWithAuthWebSliceTest, Cap03LibraryWithAuthTest #4)
    // =================================================================================================
    //
    // Poziom 1 — reguły URL (Cap03SecurityConfig): od szczegółowych do ogólnych; publiczne logowanie i GET katalogu;
    // POST /api/books, /api/loans, /api/loans/{id}/return — hasRole("BIBLIOTEKARZ"); reszta — authenticated().
    // Odpowiedzi z FILTRÓW (puste ciało, opis w nagłówku — z testów):
    //     bez tokenu   → 401  WWW-Authenticate: Bearer resource_metadata="http://localhost/.well-known/oauth-protected-resource"
    //     czytelnik    → 403  WWW-Authenticate: Bearer error="insufficient_scope", error_description="The request requires
    //                         higher privileges than provided by the access token.", error_uri="..."
    // Reguła URL zatrzymuje czytelnika ZANIM ruszy kontroler (w teście wycinka serwis-atrapa nie dostał ani jednego wywołania).
    //
    // Poziom 2 — adnotacje na metodach (@EnableMethodSecurity), gdy decyzja zależy od danych:
    //   @PreAuthorize("hasRole('BIBLIOTEKARZ') or #username == authentication.name")    — przed wykonaniem, z parametru;
    //   @PostAuthorize("hasRole('BIBLIOTEKARZ') or returnObject.reader() == authentication.name") — po wykonaniu,
    //     z WYNIKU (właściciela wypożyczenia znamy dopiero po odczycie). authentication.name = claim sub tokenu.
    //   Jan: /api/readers/jan/loans → 200; /api/readers/maria/loans → 403; /api/loans/1 → 200; /api/loans/3 → 403.
    // BookService.add i LoanService.lend mają @PreAuthorize MIMO reguły URL — obrona w głąb: metodę może kiedyś zawołać
    // inny kontroler albo zadanie w tle, a reguła URL tego nie zobaczy.
    //
    // PUŁAPKA: @PostAuthorize na metodzie, która coś ZMIENIA. Dlaczego groźne? Metoda wykonuje się w całości, a dopiero
    //   potem sprawdzany jest wynik — dla odczytu to w porządku, ale zmiana mogła już zostać zrobiona, zanim padnie
    //   „odmowa”. Do zmian używaj @PreAuthorize (sprawdzenie PRZED wykonaniem).
    //
    // DOBRA PRAKTYKA: 403 vs 404 dla cudzych zasobów. U nas cudze wypożyczenie → 403, nieistniejące → 404 — więc
    //   można sprawdzić, które numery istnieją. Dla danych wrażliwych lepiej zwracać 404 w obu przypadkach.

    // =================================================================================================
    // 5. BŁĘDY A SPRING SECURITY — 401/403 z wnętrza kontrolera (test: Cap03LibraryWithAuthTest #2, #4)
    // =================================================================================================
    //
    // Wyjątki Security rzucone z WNĘTRZA wywołania kontrolera (BadCredentialsException przy logowaniu,
    // AuthorizationDeniedException z @PreAuthorize) najpierw trafiają do porad Spring MVC, a dopiero gdy żadna ich nie
    // obsłuży — do filtra ExceptionTranslationFilter Spring Security.
    //
    // PUŁAPKA: (PYTANIE REKRUTACYJNE) siatka @ExceptionHandler(Exception.class) w poradzie z projektu bez logowania,
    //   przeniesiona do projektu z logowaniem. Co się stanie? Łapie także wyjątki Security: sprawdzone uruchomieniem —
    //   bez metod onAccessDenied i onAuthenticationFailed złe hasło daje 500 (w logu BadCredentialsException: Bad
    //   credentials), a odmowa z @PreAuthorize — 500 (AuthorizationDeniedException: Access Denied). Klient widzi
    //   „błąd serwera” zamiast 401/403. Rozwiązanie w LibraryProblemAdvice: osobne handlery AccessDeniedException → 403
    //   i AuthenticationException → 401 (bardziej szczegółowy handler wygrywa z Exception). Inna droga: w handlerze
    //   AccessDeniedException rzucić wyjątek dalej (throw e) — wtedy obsłuży go ExceptionTranslationFilter (sprawdzone:
    //   403, puste ciało, WWW-Authenticate: Bearer error="insufficient_scope" — jak przy regule URL).
    //
    // Dwa kształty błędów: 401/403 z FILTRÓW (brak tokenu, reguła URL) mają puste ciało i opis w WWW-Authenticate
    // (standard RFC 6750 dla tokenów Bearer); 401/403 z KONTROLERA (logowanie, @PreAuthorize) — ProblemDetail
    // z polem code. Chcesz jednego kształtu? Własny AuthenticationEntryPoint i AccessDeniedHandler w łańcuchu filtrów.

    // =================================================================================================
    // 6. TESTY NA KAŻDYM POZIOMIE
    // =================================================================================================
    //
    //   Cap03LibraryWithAuthUnitTest        JUnit                kara (granice dni), reguły egzemplarza i zwrotu
    //   Cap03LibraryWithAuthWebSliceTest    @WebMvcTest + @Import(Cap03SecurityConfig, Cap03Config) + jwt()
    //                                                            reguły URL 401/403/201, walidacja, 409 przy @Version
    //   Cap03LibraryWithAuthRepositoryTest  @DataJpaTest         zaległości, limit, join fetch, @Version (NOT_SUPPORTED)
    //   Cap03LibraryWithAuthTest            @SpringBootTest + MockMvc + PRAWDZIWE logowanie
    //                                                            BCrypt, JWT, @PreAuthorize/@PostAuthorize, kary, wygasły token
    //   ćwiczenia                           @SpringBootTest + @WithMockUser (zalogowany na czas testu, bez tokenu)
    // jwt() ze spring-security-test wkłada gotowe uwierzytelnienie z wybranymi uprawnieniami — szybko i bez kluczy;
    // pełny test robi to „naprawdę” (login → token → dekoder), bo tylko tak sprawdzisz konfigurację tokenów.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Hasła: DelegatingPasswordEncoder, w bazie {bcrypt}$2a$10$...; nigdy jawnie, nigdy w odpowiedzi ani w tokenie.
     *   • Logowanie: AuthenticationManager(DaoAuthenticationProvider(UserDetailsService) + encoder) → TokenService → JWT.
     *   • Zły login = złe hasło = ten sam 401 (nie zdradzaj, które konta istnieją).
     *   • JWT: sub, roles, iss, iat, exp; klucz RSA (testowy, z generatora); dekoder z walidatorami i TYM SAMYM Clock.
     *   • claim roles → JwtGrantedAuthoritiesConverter(prefiks ROLE_) → hasRole('BIBLIOTEKARZ').
     *   • Dwa poziomy: reguły URL (grubo) + @PreAuthorize/@PostAuthorize (z danymi: #param, returnObject).
     *   • Filtry: 401/403 z pustym ciałem i WWW-Authenticate; kontroler: wyjątki Security idą NAJPIERW do porady —
     *     obsłuż AccessDeniedException (403) i AuthenticationException (401), zanim złapie je siatka Exception (500).
     *   • @Version działa, gdy dwie operacje zmieniają TEN SAM wiersz (books.on_loan).
     *   • Testy: jwt() w wycinku, prawdziwe logowanie w pełnym kontekście, @WithMockUser przy wywołaniu serwisu wprost.
     *
     * PYTANIA KONTROLNE:
     *   1. Co jest w tokenie JWT tej aplikacji, a czego nie powinno w nim nigdy być? Kto może go odczytać?
     *   2. Co się stanie? Z LibraryProblemAdvice znikną metody onAccessDenied i onAuthenticationFailed (siatka
     *      Exception zostaje), a jan wyśle GET /api/readers/maria/loans ze swoim tokenem.
     *   3. Co się stanie? Jan (CZYTELNIK) wyśle POST /api/loans {"bookId":6,"reader":"adam"} z poprawnym tokenem.
     *      Jaki status, jakie ciało i czy LoanService zostanie wywołany?
     *   4. ZNAJDŹ BŁĄD: logowanie zwraca 404 „Nie ma użytkownika nikt”, gdy login nie istnieje, i 401 „Złe hasło”,
     *      gdy hasło się nie zgadza.
     *   5. Co się stanie? Wyślesz GET /api/me z tokenem wystawionym 2 godziny temu, ważnym 1 godzinę.
     *   6. Dlaczego GET /api/loans/{id} używa @PostAuthorize, a GET /api/readers/{username}/loans — @PreAuthorize?
     *   7. ZNAJDŹ BŁĄD: „Mamy regułę URL hasRole("BIBLIOTEKARZ") dla POST /api/loans, więc @PreAuthorize na
     *      LoanService.lend jest zbędne — usuwamy je”.
     *   8. Po co @Version w Book, skoro LoanService i tak sprawdza onLoan przed wypożyczeniem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA (rozszerzenia biblioteki) — w klasie Cap03Exercises, testy: Cap03LibraryWithAuthExercisesTest (▶ sam),
    // rozwiązania: s21_capstone.solutions.cap03_library_with_auth.Cap03LibraryWithAuthSolutions.
    //   ĆWICZENIE 1 (łatwe): przedłużenie terminu — tylko własne, tylko aktywne, nie po terminie, najwyżej raz.
    //   ĆWICZENIE 2 (średnie): raport zaległości tylko dla bibliotekarza (@PreAuthorize) — jedno zapytanie z join fetch.
    //   ĆWICZENIE 3 (trudniejsze): rezerwacja wypożyczonej książki i wypożyczenie z poszanowaniem rezerwacji
    //   (atomowo: nieudane wypożyczenie zostawia rezerwację aktywną).
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. iss (wystawca), sub (login), roles, iat, exp — i nic więcej. Nigdy hasła, skrótu hasła, danych osobowych.
     *      Ładunek to tylko Base64URL: odczyta go każdy, kto ma token (test loginIssuesJwt robi to bez klucza).
     *      Podpis chroni przed ZMIANĄ, nie przed odczytem.
     *   2. 500 „Wystąpił błąd wewnętrzny...” zamiast 403 — AuthorizationDeniedException z @PreAuthorize trafia najpierw do
     *      porady i łapie go siatka Exception (sprawdzone uruchomieniem; w logu „AuthorizationDeniedException: Access
     *      Denied”). Tak samo złe hasło przy logowaniu da 500 zamiast 401.
     *   3. 403 z reguły URL w łańcuchu filtrów: puste ciało, nagłówek WWW-Authenticate: Bearer error="insufficient_scope"...
     *      LoanService NIE zostanie wywołany — filtr zatrzymał żądanie przed kontrolerem (test lendingIsForLibrarians:
     *      verifyNoInteractions(loanService)).
     *   4. Różne odpowiedzi zdradzają, które loginy istnieją (user enumeration) — atakujący najpierw zbiera konta, potem
     *      zgaduje hasła. Poprawnie: zawsze ten sam 401 „Nieprawidłowy login lub hasło” (u nas — test loginIssuesJwt).
     *   5. 401, puste ciało, WWW-Authenticate: Bearer error="invalid_token", error_description="An error occurred while
     *      attempting to decode the Jwt: Jwt expired at 2026-10-19T07:00:00Z" (zegar aplikacji: 10:00, token do 9:00 +
     *      60 s tolerancji; test expiredToken).
     *   6. W liście login czytelnika jest PARAMETREM — można go porównać przed wykonaniem (@PreAuthorize, #username).
     *      Przy jednym wypożyczeniu znamy tylko id — właściciela poznajemy dopiero z wyniku, więc sprawdzamy
     *      returnObject.reader() po wykonaniu (@PostAuthorize). To tylko odczyt, więc wykonanie metody niczego nie psuje.
     *   7. Reguła URL chroni tylko ten jeden adres. Jeśli kiedyś inny kontroler, zadanie w tle albo ćwiczenie
     *      (lendRespectingReservations) zawoła lend, reguła URL nic nie sprawdzi. @PreAuthorize na serwisie to obrona
     *      w głąb — działa przy KAŻDYM wywołaniu przez proxy.
     *   8. Sprawdzenie onLoan i zapis to dwa kroki (check-then-act): dwóch bibliotekarzy może jednocześnie zobaczyć
     *      „na półce”. Oba zapisy zmieniają TEN SAM wiersz books, więc @Version wykryje, że drugi pracował na starej
     *      wersji → ObjectOptimisticLockingFailureException → 409 CONCURRENT_MODIFICATION.
     */
    // </editor-fold>
}

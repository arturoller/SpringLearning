package s12_security.sec02_authorization;

/**
 * <pre>
 * TEMAT: Autoryzacja w Spring Security 7 — reguły adresów, role i uprawnienia, bezpieczeństwo metod (@PreAuthorize),
 *        hierarchia ról
 *        (authorization = autoryzacja, czyli "co ci wolno"; role = rola; authority = uprawnienie; method security =
 *         bezpieczeństwo metod; role hierarchy = hierarchia ról; pre/post authorize = sprawdź przed / po wykonaniu metody)
 *
 * W SKRÓCIE:
 *   Sec01Basics odpowiedziała na pytanie "kim jesteś?". Tu odpowiadamy na "co ci wolno?". Pierwsza linia obrony to reguły
 *   ADRESÓW w {@code authorizeHttpRequests}: dla ścieżki (i metody HTTP) mówimy, czy wolno każdemu ({@code permitAll}), tylko
 *   zalogowanym ({@code authenticated}), tylko roli ({@code hasRole}) albo nikomu ({@code denyAll}). Reguły czyta się OD GÓRY
 *   i wygrywa PIERWSZA pasująca. Rola to uprawnienie z prefiksem {@code ROLE_}: {@code hasRole("ADMIN")} sprawdza
 *   {@code ROLE_ADMIN}. Druga linia obrony to reguły na METODACH serwisów ({@code @PreAuthorize}, {@code @PostAuthorize},
 *   {@code @PostFilter}) — tam, gdzie decyzja zależy od danych ("czy to TWOJE zamówienie?"). Hierarchia ról
 *   ({@code RoleHierarchy}: ADMIN > EMPLOYEE > CUSTOMER) oszczędza wypisywania wszystkich ról w każdej regule.
 *
 * ANALOGIA: hotel.
 *   Recepcja (reguły adresów) patrzy na kartę: gość wejdzie do lobby i restauracji, personel także na zaplecze, kierownik
 *   wszędzie. Ale to, czy wejdziesz do POKOJU 214, rozstrzyga zamek w drzwiach tego pokoju (reguła na metodzie): karta musi
 *   pasować do TEGO pokoju. Karta kierownika otwiera też drzwi personelu (hierarchia ról). Pokój, którego nie ma w systemie
 *   (adres bez reguły), jest zamknięty dla wszystkich (deny by default).
 *
 * JAK TO DZIAŁA:
 *   1. {@code authorizeHttpRequests} buduje listę par (dopasowanie adresu → {@code AuthorizationManager}). Filtr
 *      {@code AuthorizationFilter} bierze PIERWSZĄ parę, której dopasowanie pasuje, i pyta jej menedżera o decyzję.
 *   2. {@code hasRole("X")} to skrót {@code hasAuthority("ROLE_X")}. Uprawnienia użytkownika daje {@code UserDetailsService}
 *      ({@code roles("X")} zapisuje {@code ROLE_X}, {@code authorities("Y")} — dokładnie {@code Y}).
 *   3. {@code @EnableMethodSecurity} rejestruje "doradców" AOP: każdy bean z {@code @PreAuthorize} dostaje proxy CGLIB.
 *      Wywołanie przez proxy → sprawdzenie wyrażenia SpEL → metoda (albo {@code AuthorizationDeniedException}).
 *   4. Bean {@code RoleHierarchy} Spring Security 7 znajduje sam i używa go i w regułach adresów, i w wyrażeniach metod.
 *   5. Odmowa (z adresu albo z metody) → {@code ExceptionTranslationFilter} → 401 dla anonima, 403 dla zalogowanego.
 *
 * SŁÓWKA:
 *   authorize = autoryzować, zezwolić; request matcher = dopasowanie żądania; permit all = wpuść wszystkich; deny all = odmów
 *   wszystkim; authenticated = zalogowany; has role / has any role / has authority = ma rolę / którąkolwiek z ról /
 *   uprawnienie; prefix = prefiks, przedrostek; expression = wyrażenie; SpEL = Spring Expression Language (język wyrażeń
 *   Springa); return object = zwracany obiekt; filter object = element filtrowanej listy; hierarchy = hierarchia;
 *   implies = zawiera w sobie; owner = właściciel; deny by default = domyślnie odmawiaj; defense in depth = obrona
 *   w głąb (kilka warstw zabezpieczeń); self-invocation = wywołanie metody z tej samej klasy (przez this).
 *
 * ZOBACZ TEŻ: s12_security/Sec01Basics (filtry, 401 kontra 403, /error),
 *             s12_security/Sec03UsersPasswords (skąd biorą się role użytkownika: baza),
 *             s12_security/Sec06SecurityTesting (@WithMockUser, testy macierzy ról),
 *             s10_transactions/Tx03ProxyPitfalls (proxy i self-invocation — ta sama pułapka co @Transactional),
 *             s05_validation/Val05ServiceValidation (inna funkcja działająca przez proxy).
 * </pre>
 */
public final class Sec02Authorization {

    private Sec02Authorization() {
    }

    // =================================================================================================
    // 1. REGUŁY ADRESÓW: authorizeHttpRequests (Sec02SecurityConfig, Sec02UrlRulesTest.methodSpecificRules, deleteOnlyAdmin,
    //    sec02_pitfalls: Sec02RuleOrderPitfallTest, Sec02AnyRequestFirstPitfallTest)
    // =================================================================================================
    //
    // Reguły sklepu (Sec02SecurityConfig):
    //
    //     .requestMatchers(HttpMethod.GET, "/sec02/products/**").permitAll()         // katalog: każdy, także anonim
    //     .requestMatchers(HttpMethod.POST, "/sec02/products/**").hasRole("EMPLOYEE") // dodawanie: pracownik
    //     .requestMatchers(HttpMethod.DELETE, "/sec02/products/**").hasRole("ADMIN")  // usuwanie: administrator
    //     .requestMatchers("/sec02/orders/**").hasAnyRole("CUSTOMER", "EMPLOYEE")     // zamówienia: klient lub pracownik
    //     .requestMatchers("/sec02/reports/**").hasAuthority("REPORTS_READ")          // raporty: konkretne uprawnienie
    //     .requestMatchers("/sec02/admin/**").hasRole("ADMIN")
    //     .requestMatchers("/error").permitAll()
    //     .anyRequest().denyAll()                                                     // reszta: nikt (sekcja 5)
    //
    // Ten sam adres może mieć różne reguły dla różnych metod HTTP — {@code requestMatchers(HttpMethod.POST, …)}. Test
    // methodSpecificRules: GET katalogu bez logowania 200; POST — anonim 401, klient 403, pracownik 201.
    // Dostępne decyzje (sprawdzone w {@code AuthorizeHttpRequestsConfigurer.AuthorizedUrl} 7.1): {@code permitAll},
    // {@code denyAll}, {@code authenticated}, {@code fullyAuthenticated}, {@code anonymous}, {@code rememberMe},
    // {@code hasRole}, {@code hasAnyRole}, {@code hasAllRoles}, {@code hasAuthority}, {@code hasAnyAuthority},
    // {@code hasAllAuthorities}, {@code access(własny AuthorizationManager)} i {@code not()} (zaprzeczenie następnej decyzji).
    //
    // KOLEJNOŚĆ: reguły sprawdzane są OD GÓRY, a decyduje PIERWSZA pasująca. Kolejne nie są już w ogóle czytane.
    //
    // PUŁAPKA: ogólna reguła przed szczegółową. Dlaczego? W aplikacji {@code RuleOrderApplication} stoi:
    //
    //     .requestMatchers("/sec02/pitfall/admin/**").authenticated()               // pasuje też do …/admin/reports/…
    //     .requestMatchers("/sec02/pitfall/admin/reports/**").hasRole("ADMIN")      // nigdy nie zostanie sprawdzona
    //
    //   Klient (CUSTOMER) czyta raport z pensjami: 200 zamiast 403 (test generalRuleFirstWins). Spring nie ostrzega — obie reguły
    //   są poprawne, tylko w złej kolejności. Zasada: od najbardziej szczegółowej do najogólniejszej, {@code anyRequest()} na końcu.
    //
    // {@code anyRequest()} przed innymi regułami Spring Security wykrywa sam: aplikacja NIE STARTUJE, błąd
    // {@code IllegalStateException: Can't configure requestMatchers after anyRequest} (test Sec02AnyRequestFirstPitfallTest).
    //
    // UWAGA (Security 7 vs stare poradniki): {@code antMatchers}, {@code mvcMatchers}, {@code authorizeRequests} i
    //   {@code access("hasRole('ADMIN')")} z napisem SpEL w regułach adresów nie istnieją. Jest {@code requestMatchers} (wzorce
    //   {@code PathPatternRequestMatcher}, jak w {@code @GetMapping}) i {@code access(AuthorizationManager)}.

    // =================================================================================================
    // 2. ROLE KONTRA UPRAWNIENIA (Sec02UrlRulesTest.authorityIsNotARole, Sec02RoleHierarchyTest.hasRoleWithPrefixFails,
    //    Sec02RolePrefixPitfallTest, Sec02ProxyPitfallTest.prefixInExpressionWorks)
    // =================================================================================================
    //
    // Dla Spring Security użytkownik ma po prostu listę napisów — uprawnień ({@code GrantedAuthority}). Rola to umowa: uprawnienie
    // z prefiksem {@code ROLE_}.
    //
    //     | zapis w UserDetails             | uprawnienie na liście | przejdzie regułę                                    |
    //     |---------------------------------|-----------------------|-----------------------------------------------------|
    //     | .roles("ADMIN")                 | ROLE_ADMIN            | hasRole("ADMIN"), hasAuthority("ROLE_ADMIN")        |
    //     | .authorities("REPORTS_READ")    | REPORTS_READ          | hasAuthority("REPORTS_READ") — nie ma tu żadnej roli |
    //     | .authorities("ROLE_EMPLOYEE")   | ROLE_EMPLOYEE         | hasRole("EMPLOYEE")                                 |
    //
    // Po co dwa rodzaje? Rola opisuje, KIM ktoś jest w firmie (pracownik, administrator) — grubo. Uprawnienie opisuje JEDNĄ
    // czynność (czytać raporty, zwracać pieniądze) — drobno. Duże systemy przypisują roli zestaw uprawnień, a reguły sprawdzają
    // uprawnienia. Test authorityIsNotARole: ewa (EMPLOYEE + REPORTS_READ) czyta raport, a admin — NIE (403), bo hierarchia
    // z sekcji 4 dotyczy ról, a REPORTS_READ to osobne uprawnienie, którego admin nie dostał.
    //
    // PUŁAPKA: {@code hasRole("ROLE_ADMIN")} w regułach adresów. Dlaczego? {@code hasRole} sam dopisuje {@code ROLE_}, więc
    //   wyszłoby {@code ROLE_ROLE_ADMIN}. Spring Security 7 zatrzymuje to przy starcie: {@code IllegalArgumentException:
    //   ROLE_ADMIN should not start with ROLE_ since ROLE_ is automatically prepended…} (testy hasRoleWithPrefixFails
    //   i Sec02RolePrefixPitfallTest). Uwaga na niekonsekwencję: w wyrażeniu {@code @PreAuthorize("hasRole('ROLE_ADMIN')")}
    //   prefiks jest akceptowany i działa (test prefixInExpressionWorks) — nie ucz się na tym przykładzie, pisz wszędzie
    //   {@code hasRole("ADMIN")}.
    //
    // PUŁAPKA: {@code User.withUsername("ewa").roles("EMPLOYEE").authorities("REPORTS_READ")}. Dlaczego? Obie metody USTAWIAJĄ
    //   całą listę (nie dopisują), więc druga wymazuje pierwszą i ewa traci rolę. Dlatego w Sec02SecurityConfig jest jedno
    //   {@code authorities("ROLE_EMPLOYEE", "REPORTS_READ")}.

    // =================================================================================================
    // 3. BEZPIECZEŃSTWO METOD: @EnableMethodSecurity, @PreAuthorize, @PostAuthorize, @PostFilter
    //    (OrderService, Sec02MethodSecurityTest, Sec02UrlRulesTest.methodSecurityOverHttp)
    // =================================================================================================
    //
    // Reguła adresu nie wie, CZYJE są dane. {@code /sec02/orders/of/piotr} wygląda tak samo dla anny i dla piotra. Taką regułę
    // piszemy na metodzie serwisu, w języku SpEL:
    //
    //     @PreAuthorize("#username == authentication.name or hasRole('EMPLOYEE')")   // PRZED wykonaniem: parametr metody
    //     public List<ShopOrder> ordersOf(String username) { … }
    //
    //     @PostAuthorize("returnObject.owner() == authentication.name or hasRole('EMPLOYEE')")  // PO wykonaniu: wynik
    //     public ShopOrder byId(long id) { … }
    //
    //     @PostFilter("filterObject.owner() == authentication.name or hasRole('EMPLOYEE')")  // PO: usuń cudze elementy listy
    //     public List<ShopOrder> visibleOrders() { … }
    //
    //   • {@code @EnableMethodSecurity} (na klasie konfiguracji) włącza te adnotacje. W Security 7 domyślnie działają
    //     {@code @PreAuthorize}/{@code @PostAuthorize}/{@code @PreFilter}/{@code @PostFilter}; dawne {@code @Secured} i
    //     {@code @RolesAllowed} trzeba włączyć atrybutami ({@code securedEnabled}, {@code jsr250Enabled}).
    //     Starsze {@code @EnableGlobalMethodSecurity} z poradników jest przestarzałe (deprecated) — nie używaj go.
    //   • {@code #username} — parametr metody po nazwie (kompilator Boota zachowuje nazwy parametrów, {@code -parameters}).
    //   • {@code @PostAuthorize} — metoda WYKONUJE się, a sprawdzamy wynik. Dobre do odczytu; NIE do zmian (zmiana już by się
    //     stała, zanim padnie odmowa).
    //   • {@code @PostFilter} — nie daje 403, tylko wycina elementy (test postFilter: piotr widzi wyłącznie zamówienie 3).
    //     W Security 7 działa także na liście niezmiennej ({@code List.of}, tak jest w OrderService) — wynik to nowa lista.
    //     Starsze wersje czyściły listę "w miejscu", więc lista niezmienna kończyła się wyjątkiem.
    //
    // Co się dzieje przy odmowie? Proxy rzuca {@code AuthorizationDeniedException} (od Security 6.3; podklasa
    // {@code AccessDeniedException}, więc stary kod łapiący AccessDeniedException działa dalej). Wyjątek leci przez kontroler
    // i DispatcherServlet do {@code ExceptionTranslationFilter}, który robi z niego 403 dla zalogowanego (test
    // methodSecurityOverHttp) i 401 dla anonima. Bez żadnego użytkownika w kontekście (wywołanie spoza żądania HTTP, np. z testu
    // bez {@code @WithMockUser}) leci {@code AuthenticationCredentialsNotFoundException} (test noAuthentication).
    //
    // PUŁAPKA: {@code @RestControllerAdvice} z handlerem {@code Exception.class} (s06_error_handling/Err03ControllerAdvice)
    //   połyka też {@code AuthorizationDeniedException} rzucony z metody i robi z niego 500 (test Sec02CatchAllPitfallTest).
    //   Dlaczego? Advice działa w DispatcherServlet, zanim wyjątek dotrze do filtra bezpieczeństwa. Jeśli masz "łap wszystko", przepuść wyjątki dostępu
    //   dalej (rzuć je ponownie) albo obsłuż je jawnie jako 403. Więcej o błędach bezpieczeństwa: Sec04StatelessApi.
    //
    // PYTANIE REKRUTACYJNE: "Czym różni się @PreAuthorize od @PostAuthorize?" — Pre sprawdza przed wywołaniem (parametry,
    //   role), metoda może się w ogóle nie wykonać. Post sprawdza po wywołaniu (wynik) — metoda już się wykonała, więc nie
    //   nadaje się do operacji zmieniających dane.

    // =================================================================================================
    // 4. HIERARCHIA RÓL: RoleHierarchy (Sec02SecurityConfig.roleHierarchy, Sec02RoleHierarchyTest.reachableAuthorities,
    //    Sec02UrlRulesTest.roleHierarchyForUrls, Sec02MethodSecurityTest.roleHierarchyForMethods)
    // =================================================================================================
    //
    // Bez hierarchii każda reguła musiałaby wymieniać wszystkich: {@code hasAnyRole("EMPLOYEE", "ADMIN")}. Z hierarchią:
    //
    //     @Bean
    //     static RoleHierarchy roleHierarchy() {
    //         return RoleHierarchyImpl.withDefaultRolePrefix()
    //                 .role("ADMIN").implies("EMPLOYEE")
    //                 .role("EMPLOYEE").implies("CUSTOMER")
    //                 .build();
    //     }
    //
    //   • {@code getReachableGrantedAuthorities([ROLE_ADMIN])} = ROLE_ADMIN, ROLE_EMPLOYEE, ROLE_CUSTOMER (test
    //     reachableAuthorities). Hierarchia działa "w dół": EMPLOYEE nie dostaje ADMIN (test hierarchyGoesDownOnly).
    //   • W Security 7 WYSTARCZY bean — sprawdzone testami: reguły adresów (admin robi POST jak EMPLOYEE, test
    //     roleHierarchyForUrls) i {@code @PreAuthorize} (test roleHierarchyForMethods) same go używają. W starych poradnikach
    //     zobaczysz ręczne podpinanie przez {@code DefaultMethodSecurityExpressionHandler.setRoleHierarchy(…)} i zapis tekstowy
    //     {@code "ROLE_ADMIN > ROLE_EMPLOYEE"} ({@code RoleHierarchyImpl.fromHierarchy(…)} — nadal działa, ale builder jest czytelniejszy).
    //   • Metoda beana jest {@code static} — infrastruktura bezpieczeństwa metod powstaje bardzo wcześnie, a metoda statyczna
    //     nie wymaga tworzenia całej klasy konfiguracji (Spring zaleca to dla beanów infrastruktury).
    //
    // DOBRA PRAKTYKA: hierarchia ról zamiast długich list {@code hasAnyRole(…)}. Dlaczego? Nowa rola (np. MANAGER między
    //   EMPLOYEE a ADMIN) to jedna linijka w hierarchii, a nie poprawianie każdej reguły w projekcie.

    // =================================================================================================
    // 5. PUŁAPKI I DOBRE PRAKTYKI: deny by default, nie tylko UI, self-invocation
    //    (Sec02UrlRulesTest.denyByDefault, Sec02RuleOrderPitfallTest.hiddenInUiOnly, Sec02ProxyPitfallTest)
    // =================================================================================================
    //
    // DOBRA PRAKTYKA: "deny by default" — na końcu {@code anyRequest().denyAll()} (albo przynajmniej {@code authenticated()}).
    //   Dlaczego? Programista doda jutro kontroler {@code /sec02/internal/debug} i zapomni o regule. Z {@code denyAll()} adres jest
    //   zamknięty dla wszystkich — nawet admin dostaje 403, anonim 401 (test denyByDefault) — i błąd wyjdzie od razu, przy
    //   pierwszym teście. Z {@code permitAll()} na końcu dane wyciekłyby po cichu. {@code authenticated()} na końcu to kompromis:
    //   obcy nie wejdzie, ale każdy zalogowany już tak.
    //
    // PUŁAPKA: zabezpieczanie tylko interfejsu (UI), a nie API. Dlaczego? Ukrycie przycisku "Usuń" przed klientem nic nie daje —
    //   każdy może wysłać {@code DELETE} curlem albo z pliku .http. W {@code RuleOrderApplication} usuwanie nie ma własnej
    //   reguły, a na końcu jest {@code anyRequest().authenticated()}: klient usuwa produkt (204, test hiddenInUiOnly). Decyzję
    //   zawsze podejmuje SERWER — reguła adresu i/lub metody.
    //
    // PUŁAPKA: wywołanie metody z {@code @PreAuthorize} z TEJ SAMEJ klasy. Dlaczego? Reguły sprawdza proxy, a {@code this.metoda()}
    //   idzie prosto do obiektu, z pominięciem proxy — dokładnie jak przy {@code @Transactional}
    //   (s10_transactions/Tx03ProxyPitfalls). {@code PayrollService.report()} (bez adnotacji) woła {@code salaries()} (tylko
    //   ADMIN): klient dostaje pensje (test selfInvocationBypassesSecurity), choć bezpośrednie {@code salaries()} daje odmowę.
    //   Naprawa: adnotacja na metodzie publicznej, którą woła kontroler, albo przeniesienie zabezpieczonej metody do innego beana.
    //
    // DOBRA PRAKTYKA: obrona w głąb (defense in depth) — reguła adresu ORAZ reguła metody dla ważnych operacji. Dlaczego? Jeśli
    //   ktoś pomyli kolejność reguł adresów albo doda nowy kontroler do tej samej metody serwisu, druga warstwa nadal chroni dane.
    //   Tak jest z {@code /sec02/admin/stats}: reguła adresu {@code hasRole("ADMIN")} i {@code @PreAuthorize} na
    //   {@code OrderService.countAll()}.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.GET, "/x/**").permitAll() … .anyRequest().denyAll()).
     *   • Kolejność: pierwsza pasująca wygrywa → od szczegółu do ogółu; anyRequest() na końcu (inaczej błąd przy starcie).
     *   • hasRole("X") == hasAuthority("ROLE_X"); hasRole("ROLE_X") w regułach adresów → IllegalArgumentException przy starcie.
     *   • roles(...) i authorities(...) w User.withUsername USTAWIAJĄ całą listę — nie łącz ich.
     *   • @EnableMethodSecurity → @PreAuthorize (przed, parametry #nazwa), @PostAuthorize (po, returnObject), @PostFilter
     *     (filterObject, nowa lista bez cudzych elementów). Advice "łap wszystko" zamienia odmowę metody w 500. Odmowa: AuthorizationDeniedException (extends AccessDeniedException) → 403/401.
     *   • Bez użytkownika w kontekście: AuthenticationCredentialsNotFoundException.
     *   • @Bean static RoleHierarchy (RoleHierarchyImpl.withDefaultRolePrefix().role("ADMIN").implies("EMPLOYEE")…) — Security 7
     *     używa go sam w URL i w metodach. Hierarchia dotyczy ról, działa w dół.
     *   • Deny by default; serwer decyduje, nie UI; self-invocation omija @PreAuthorize (proxy).
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? Reguły to {@code requestMatchers("/shop/**").authenticated()}, a POD nią
     *      {@code requestMatchers("/shop/admin/**").hasRole("ADMIN")}. Zalogowany klient (CUSTOMER) wchodzi na /shop/admin/users.
     *      Jaki status dostanie?
     *   2. ZNAJDŹ BŁĄD: {@code .requestMatchers("/admin/**").hasRole("ROLE_ADMIN")} — aplikacja nie startuje. Dlaczego i jak
     *      to poprawić na dwa sposoby?
     *   3. Czym różni się rola od uprawnienia? Admin z hierarchią ADMIN > EMPLOYEE nie może czytać raportów z regułą
     *      hasAuthority("REPORTS_READ"). Dlaczego?
     *   4. PYTANIE REKRUTACYJNE: czym różni się {@code @PreAuthorize} od {@code @PostAuthorize}? Której nie użyjesz do metody
     *      {@code deleteOrder} i dlaczego?
     *   5. Co się stanie? Klient (CUSTOMER) woła publiczną metodę serwisu bez adnotacji, która w środku woła
     *      {@code this.salaries()} z {@code @PreAuthorize("hasRole('ADMIN')")}.
     *   6. Jaki wyjątek rzuca odmowa z {@code @PreAuthorize} w Security 7, a jaki — gdy w kontekście nie ma żadnego użytkownika?
     *      Jaki status HTTP zobaczy zalogowany klient?
     *   7. Co się stanie? Konfiguracja kończy się {@code anyRequest().denyAll()}, a ktoś dodał kontroler /sec02/internal/debug
     *      bez reguły. Co dostanie admin, a co anonim?
     *   8. ZNAJDŹ BŁĄD: hierarchia ról jest beanem, ale ktoś "na wszelki wypadek" pisze wszędzie
     *      {@code hasAnyRole("CUSTOMER", "EMPLOYEE", "ADMIN")}. Co jest nie tak (nie błąd działania, tylko utrzymania)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: AuthorityNames, ExerciseRoleHierarchy, ExerciseOrderService, ExerciseSecurityConfig
    // (kontroler ExerciseController jest gotowy); testy: Sec02AuthorizationExercisesTest i Sec02ExerciseSecurityTest
    // (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Rozwiązania wzorcowe: pakiet s12_security.solutions.sec02_authorization. Adresy ćwiczeń mają prefiks /sec02/cw.
    //
    // ĆWICZENIE 1 (łatwe): AuthorityNames.roleToAuthority — "ADMIN" → "ROLE_ADMIN"; nazwa zaczynająca się od "ROLE_" →
    //   IllegalArgumentException. Podpowiedź: {@code startsWith}; tak samo pilnuje tego hasRole (sekcja 2).
    //
    // ĆWICZENIE 2 (łatwe): ExerciseRoleHierarchy.create — ADMIN zawiera EMPLOYEE i AUDITOR, EMPLOYEE zawiera CUSTOMER.
    //   Podpowiedź: {@code implies(...)} przyjmuje kilka ról naraz; wzór: Sec02SecurityConfig.roleHierarchy.
    //
    // ĆWICZENIE 3 (średnie): ExerciseOrderService — dopisz treść metod i adnotacje: {@code cancel(owner, id)} wolno właścicielowi
    //   albo ADMIN; {@code refund(amount)} tylko EMPLOYEE. W teście NIE ma hierarchii ról, więc ADMIN nie zrobi zwrotu.
    //   Podpowiedź: {@code @PreAuthorize("#owner == authentication.name or hasRole('ADMIN')")}.
    //
    // ĆWICZENIE 4 (trudniejsze): ExerciseSecurityConfig.filterChain — reguły /sec02/cw/**: GET catalog/hidden/** tylko EMPLOYEE,
    //   pozostałe GET catalog/** dla każdego, POST catalog tylko EMPLOYEE, /error dla każdego, reszta denyAll; httpBasic, CSRF
    //   wyłączony. Podpowiedź: KOLEJNOŚĆ — która reguła musi być pierwsza (sekcja 1)? Dopóki jest TODO, test zgłosi
    //   "Failed to load ApplicationContext" z przyczyną UnsupportedOperationException: TODO.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 200 — pierwsza pasująca reguła to "/shop/**" authenticated(), a klient jest zalogowany; reguła z hasRole("ADMIN")
     *      nie jest w ogóle czytana (test generalRuleFirstWins). Szczegółowa reguła musi stać wyżej.
     *   2. hasRole sam dopisuje prefiks ROLE_ i odrzuca nazwę, która już go ma: IllegalArgumentException "ROLE_ADMIN should not
     *      start with ROLE_…" przy budowie łańcucha (test Sec02RolePrefixPitfallTest). Poprawki: hasRole("ADMIN") albo
     *      hasAuthority("ROLE_ADMIN").
     *   3. Rola to uprawnienie z prefiksem ROLE_ (grube "kim jesteś"), uprawnienie to dowolny napis (drobne "co możesz zrobić").
     *      Hierarchia ADMIN > EMPLOYEE rozszerza ROLE_ADMIN tylko o inne ROLE_…; REPORTS_READ to osobne uprawnienie, którego admin
     *      nie ma na liście (test authorityIsNotARole). Można je dopisać adminowi albo do hierarchii.
     *   4. @PreAuthorize sprawdza przed wykonaniem — metoda może się nie wykonać. @PostAuthorize sprawdza wynik po wykonaniu.
     *      Do deleteOrder nie: zamówienie byłoby już usunięte, zanim padnie odmowa.
     *   5. Klient dostanie wynik — this.salaries() omija proxy, więc @PreAuthorize nie jest sprawdzane (test
     *      selfInvocationBypassesSecurity). Ta sama pułapka co przy @Transactional (Tx03ProxyPitfalls).
     *   6. AuthorizationDeniedException (podklasa AccessDeniedException); bez użytkownika — AuthenticationCredentialsNotFoundException
     *      (test noAuthentication). Zalogowany klient przez HTTP dostaje 403 (test methodSecurityOverHttp).
     *   7. Admin 403, anonim 401 (test denyByDefault) — adres jest zamknięty, dopóki ktoś świadomie nie dopisze reguły.
     *   8. Działa, ale dubluje hierarchię: każda zmiana ról wymaga poprawiania wielu reguł, a o którejś się zapomni. Z hierarchią
     *      wystarczy hasRole("CUSTOMER") — EMPLOYEE i ADMIN przejdą same.
     */
    // </editor-fold>
}

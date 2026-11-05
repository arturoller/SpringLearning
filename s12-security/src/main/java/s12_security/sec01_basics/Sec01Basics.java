package s12_security.sec01_basics;

/**
 * <pre>
 * TEMAT: Podstawy Spring Security 7 — co robi sam starter, łańcuch filtrów, własny SecurityFilterChain, 401 kontra 403
 *        (security = bezpieczeństwo; authentication = uwierzytelnienie, czyli "kim jesteś?"; authorization = autoryzacja,
 *         czyli "co ci wolno?"; filter chain = łańcuch filtrów; principal = podmiot, zalogowany "ktoś")
 *
 * W SKRÓCIE:
 *   Dodanie startera {@code spring-boot-starter-security} zamyka CAŁĄ aplikację: każdy adres wymaga logowania. Robią to filtry
 *   serwletów (filter — obiekt, który ogląda żądanie, ZANIM dotrze do kontrolera). Tomcat zna jeden filtr
 *   ({@code DelegatingFilterProxy}), który oddaje pracę beanowi {@code FilterChainProxy}, a ten — łańcuchowi
 *   {@code SecurityFilterChain} z kilkunastoma filtrami w ustalonej kolejności. Jeden filtr odczytuje login z nagłówka, inny
 *   z formularza, inny zamienia wyjątki na 401/403, ostatni ({@code AuthorizationFilter}) decyduje "wolno / nie wolno".
 *   Kto jest zalogowany, wiadomo z {@code SecurityContextHolder} — schowka przypisanego do wątku. Własny bean
 *   {@code SecurityFilterChain} zastępuje reguły Boota: sami mówimy, które adresy są publiczne, a które wymagają logowania.
 *   401 znaczy "nie wiem, kim jesteś", 403 — "wiem, ale nie wolno ci".
 *
 * ANALOGIA: ochrona w biurowcu sklepu.
 *   Przy wejściu (Tomcat) stoi jeden portier (DelegatingFilterProxy), który niczego sam nie sprawdza — dzwoni do firmy
 *   ochroniarskiej (FilterChainProxy). Firma ma ustawionych w szeregu strażników (filtry): jeden sprawdza przepustkę w ręce
 *   (Basic), drugi prowadzi do okienka z formularzem (form login), trzeci daje plakietkę "GOŚĆ" tym, którzy nic nie pokazali
 *   (anonim), ostatni stoi przy drzwiach do pokoi i patrzy na listę (AuthorizationFilter). Zatrzymany bez przepustki słyszy
 *   "pokaż przepustkę" (401), zatrzymany z przepustką kasjera przed gabinetem prezesa — "tu ci nie wolno" (403).
 *
 * JAK TO DZIAŁA:
 *   1. Starter włącza autokonfigurację: {@code ServletWebSecurityAutoConfiguration} (domyślny łańcuch
 *      {@code defaultSecurityFilterChain}: wszystko wymaga logowania, formularz + Basic), {@code SecurityFilterAutoConfiguration}
 *      (rejestracja filtra w Tomcacie: bean {@code securityFilterChainRegistration}) i {@code UserDetailsServiceAutoConfiguration}
 *      (użytkownik {@code user} w pamięci — w TYM module tylko po ustawieniu {@code spring.security.user.*}, sekcja 1).
 *   2. Każde żądanie przechodzi przez łańcuch filtrów: wczytanie kontekstu → nagłówki bezpieczeństwa → CSRF → logowanie
 *      (formularz, Basic) → anonim → tłumaczenie wyjątków → autoryzacja. Dopiero potem {@code DispatcherServlet} i kontroler.
 *   3. Filtr logowania tworzy obiekt {@code Authentication} (kto + uprawnienia) i wkłada go do {@code SecurityContextHolder}.
 *      Kontroler może go dostać jako parametr ({@code Authentication}, {@code Principal}, {@code @AuthenticationPrincipal}).
 *   4. {@code AuthorizationFilter} porównuje regułę adresu z uprawnieniami. Brak zgody → wyjątek →
 *      {@code ExceptionTranslationFilter}: anonim dostaje 401 (albo przekierowanie na /login), zalogowany — 403.
 *
 * SŁÓWKA:
 *   starter = zestaw startowy zależności; filter = filtr; chain = łańcuch; delegating = delegujący (przekazujący dalej);
 *   proxy = pośrednik; authentication = uwierzytelnienie; authorization = autoryzacja; authority = uprawnienie;
 *   role = rola; principal = podmiot; credentials = dane uwierzytelniające (np. hasło); anonymous = anonimowy;
 *   entry point = punkt wejścia (co odpowiedzieć niezalogowanemu); realm = obszar (nazwa chronionej "strefy");
 *   unauthorized = nieuwierzytelniony (401); forbidden = zabroniony (403); permit = zezwolić; matcher = dopasowywacz (reguła
 *   "czy ten adres pasuje"); dispatch = przekazanie żądania wewnątrz serwera; factor = czynnik (sposób logowania).
 *
 * ZOBACZ TEŻ: s03_boot_basics/Boot01AutoConfiguration (jak działa autokonfiguracja),
 *             s06_error_handling/Err01DefaultErrors (strona /error i jej JSON),
 *             s10_transactions/Tx01Transactional (inny "schowek na wątek": transakcja),
 *             s11_testing/Test01TestSlices (co ładuje @WebMvcTest), s11_testing/Test03MockMvcTester (MockMvcTester),
 *             s12_security/Sec02Authorization (reguły adresów i role), s12_security/Sec03UsersPasswords (hasła, BCrypt),
 *             s12_security/Sec04StatelessApi (CSRF, sesje), s12_security/Sec06SecurityTesting (testowanie zabezpieczeń).
 * </pre>
 */
public final class Sec01Basics {

    private Sec01Basics() {
    }

    // =================================================================================================
    // 1. SAM STARTER BEZ KONFIGURACJI (sec01_pitfalls/defaults: Sec01NoUserTest, Sec01GeneratedPasswordTest,
    //    Sec01UserPropertiesTest)
    // =================================================================================================
    //
    // Weź aplikację z jednym kontrolerem ({@code DefaultsApplication} w testach) i dopisz do pom.xml:
    //
    //     <dependency>
    //         <groupId>org.springframework.boot</groupId>
    //         <artifactId>spring-boot-starter-security</artifactId>
    //     </dependency>
    //
    // Bez jednej linijki kodu dzieje się bardzo dużo (autokonfiguracja — s03_boot_basics/Boot01AutoConfiguration):
    //   • KAŻDY adres wymaga logowania — także ten, który chciałeś mieć publiczny. To zasada "bezpieczne domyślnie"
    //     (secure by default): lepiej zamknąć za dużo niż za mało.
    //   • Klient API (bez nagłówka {@code Accept: text/html}) dostaje {@code 401} z nagłówkiem
    //     {@code WWW-Authenticate: Basic realm="Realm", charset="UTF-8"} — to prośba "zaloguj się metodą Basic".
    //   • Przeglądarka (Accept: text/html) dostaje {@code 302} na {@code /login} — formularz, który Spring Security sam generuje.
    //   • Jest {@code POST /logout}, ochrona CSRF i nagłówki bezpieczeństwa (Sec04StatelessApi).
    //
    // Skąd wziąć login? W typowej aplikacji Boot tworzy użytkownika {@code user} z LOSOWYM hasłem i wypisuje je w logu:
    //
    //     Using generated security password: 6fab8f9e-faa7-4a7f-8cc1-cbe210526844
    //     This generated password is for development use only. …
    //
    // Właściwości {@code spring.security.user.name}, {@code spring.security.user.password} i {@code spring.security.user.roles}
    // zmieniają nazwę, hasło i role tego użytkownika (sprawdzone w metadanych spring-boot-security 4.1.1; domyślna nazwa: user).
    //
    // PUŁAPKA: (w TYM module i w każdym projekcie z resource-server albo oauth2-client) moduł s12-security ma na classpath
    //   starter {@code spring-boot-starter-security-oauth2-resource-server} (tokeny JWT, Sec05Jwt). Boot uznaje wtedy, że
    //   logowanie załatwia "ktoś inny" (np. serwer tokenów), i NIE tworzy użytkownika {@code user} ani losowego hasła — chyba że
    //   sam ustawisz {@code spring.security.user.name} lub {@code .password} (warunek
    //   {@code MissingAlternativeUserDetailsManagerOrUserPropertiesConfigured} w Boocie). Efekt: wszystko jest zamknięte,
    //   a w logu nie ma hasła — nikt nie może się zalogować (test Sec01NoUserTest). Dlaczego warto to wiedzieć? Bo poradniki
    //   obiecują "hasło w logu", a Ty go nie zobaczysz i będziesz szukać błędu w złym miejscu.
    //
    // PUŁAPKA: hasło z logu albo {@code spring.security.user.password} na produkcji. Dlaczego? Hasło w logu czyta każdy, kto ma
    //   dostęp do logów, a hasło w {@code application.properties} trafia do repozytorium. Domyślny użytkownik służy tylko do
    //   pierwszego uruchomienia — prawdziwych użytkowników trzyma baza (Sec03UsersPasswords), a sekrety zmienne środowiska albo
    //   menedżer sekretów (dział s19_deploy).

    // =================================================================================================
    // 2. ŁAŃCUCH FILTRÓW I SecurityContextHolder (Sec01FilterChainTest, Sec01SecurityContextTest)
    // =================================================================================================
    //
    // Jak żądanie trafia do Spring Security? Przez trzy warstwy pośredników:
    //
    //     Tomcat ──► DelegatingFilterProxy ──► FilterChainProxy ──► SecurityFilterChain (lista filtrów) ──► DispatcherServlet
    //                (zwykły filtr serwletu,    (bean "springSecurity-    (wybrany łańcuch: pierwszy,           ──► kontroler
    //                 zarejestrowany przez       FilterChain")              którego securityMatcher pasuje)
    //                 Boota)
    //
    //   • {@code DelegatingFilterProxy} — Tomcat nic nie wie o Springu, umie tylko wołać filtry. Boot rejestruje w nim jeden filtr
    //     (bean {@code securityFilterChainRegistration} typu {@code DelegatingFilterProxyRegistrationBean}), który przy każdym
    //     żądaniu szuka beana {@code springSecurityFilterChain} i oddaje mu pracę. Rejestracja obejmuje typy przekazań REQUEST,
    //     ERROR, FORWARD, INCLUDE i ASYNC (właściwość {@code spring.security.filter.dispatcher-types}) — ważne w sekcji 5.
    //   • {@code FilterChainProxy} — bean Springa, który trzyma listę łańcuchów ({@code SecurityFilterChain}) i wybiera pierwszy
    //     pasujący do adresu. Zwykle jest jeden łańcuch na wszystko (test filterOrder: {@code hasSize(1)}).
    //   • {@code SecurityFilterChain} — uporządkowana lista filtrów. Wydruk z naszej lekcji (test filterOrder):
    //
    //     DisableEncodeUrlFilter, WebAsyncManagerIntegrationFilter, SecurityContextHolderFilter, HeaderWriterFilter, CsrfFilter,
    //     LogoutFilter, UsernamePasswordAuthenticationFilter, DefaultResourcesFilter, DefaultLoginPageGeneratingFilter,
    //     DefaultLogoutPageGeneratingFilter, BasicAuthenticationFilter, RequestCacheAwareFilter,
    //     SecurityContextHolderAwareRequestFilter, AnonymousAuthenticationFilter, ExceptionTranslationFilter, AuthorizationFilter
    //
    //   Najważniejsze: {@code SecurityContextHolderFilter} (przygotowuje schowek), {@code UsernamePasswordAuthenticationFilter}
    //   (obsługuje POST /login z formularza), {@code BasicAuthenticationFilter} (nagłówek {@code Authorization: Basic …}),
    //   {@code AnonymousAuthenticationFilter} (kto się nie zalogował, dostaje token "anonymousUser"),
    //   {@code ExceptionTranslationFilter} (wyjątek "brak dostępu" → 401/403 albo przekierowanie), {@code AuthorizationFilter}
    //   (ZAWSZE na końcu: decyzja według reguł z {@code authorizeHttpRequests}). Kolejność ma sens: najpierw ustalamy KIM jesteś,
    //   dopiero potem pytamy, CO ci wolno. Każda metoda {@code http.xxx(...)} w konfiguracji dokłada lub ustawia swój filtr —
    //   bez {@code formLogin} nie byłoby {@code UsernamePasswordAuthenticationFilter}.
    //
    // {@code SecurityContextHolder} — "kto jest zalogowany W TYM żądaniu". Domyślnie to {@code ThreadLocal} (zmienna osobna dla
    // każdego wątku): Tomcat obsługuje jedno żądanie w jednym wątku, więc każde żądanie ma własny schowek. To ten sam pomysł,
    // co transakcja przypięta do wątku w s10_transactions/Tx01Transactional. Test contextIsPerThread: kontekst ustawiony w wątku
    // testu jest NIEWIDOCZNY w innym wątku. Po żądaniu filtr czyści schowek ({@code clearContext()}), żeby następne żądanie
    // obsłużone przez ten sam wątek z puli nie "odziedziczyło" poprzedniego użytkownika.
    //
    // PUŁAPKA: odczyt {@code SecurityContextHolder} w kodzie uruchomionym w innym wątku ({@code new Thread}, własna pula,
    //   {@code CompletableFuture.supplyAsync}). Dlaczego? Schowek jest przypięty do wątku żądania — nowy wątek widzi {@code null}
    //   (test contextIsPerThread). Rozwiązania (wrappery {@code DelegatingSecurityContextRunnable}/{@code …Executor}) poznasz przy
    //   pracy asynchronicznej (dział s16_cache_scheduling_async).
    //
    // PYTANIE REKRUTACYJNE: "Czym jest DelegatingFilterProxy i po co FilterChainProxy?" — Tomcat tworzy filtry sam, zanim Spring
    //   zbuduje beany; DelegatingFilterProxy jest "mostem": filtrem serwletu, który przy żądaniu sięga po bean. FilterChainProxy
    //   to jeden punkt wejścia do wszystkich łańcuchów (wybór łańcucha, czyszczenie kontekstu, zapora {@code HttpFirewall}).

    // =================================================================================================
    // 3. WŁASNY @Bean SecurityFilterChain (Sec01SecurityConfig, Sec01BasicsTest, Sec01SliceWithoutImportTest)
    // =================================================================================================
    //
    // Prawie każda aplikacja chce mieć część adresów publiczną. Piszemy więc własny bean (Sec01SecurityConfig):
    //
    //     @Bean
    //     SecurityFilterChain sec01FilterChain(HttpSecurity http) {
    //         http
    //                 .authorizeHttpRequests(auth -> auth
    //                         .requestMatchers("/sec01/public/**").permitAll()
    //                         .requestMatchers("/error").permitAll()
    //                         .requestMatchers("/sec01/admin/**").hasRole("ADMIN")
    //                         .anyRequest().authenticated())
    //                 .httpBasic(Customizer.withDefaults())
    //                 .formLogin(Customizer.withDefaults());
    //         return http.build();
    //     }
    //
    // Co tu się dzieje:
    //   • Bean {@code HttpSecurity} (budowniczy łańcucha) daje nam Spring Security; każda metoda włącza jedną funkcję.
    //   • {@code authorizeHttpRequests} — reguły adresów, sprawdzane OD GÓRY, pierwsza pasująca wygrywa (szczegóły i pułapka
    //     kolejności: Sec02Authorization). {@code requestMatchers("/sec01/public/**")} w Security 7 używa
    //     {@code PathPatternRequestMatcher} (te same wzorce co {@code @GetMapping}: {@code **} = dowolnie wiele segmentów).
    //   • {@code Customizer.withDefaults()} — "włącz z ustawieniami domyślnymi".
    //   • Sam fakt istnienia NASZEGO beana {@code SecurityFilterChain} wyłącza domyślny łańcuch Boota (warunek
    //     {@code @ConditionalOnDefaultWebSecurity}) — test ownChainReplacesDefault: jest tylko {@code sec01FilterChain}.
    //   • Użytkownicy: bean {@code UserDetailsService} ({@code InMemoryUserDetailsManager} z anną i adminem). Szczegóły haseł
    //     i bazy: Sec03UsersPasswords.
    //
    // UWAGA (Security 7 vs stare poradniki): w internecie zobaczysz kod, który w Spring Security 7 SIĘ NIE SKOMPILUJE:
    //
    //     public class Config extends WebSecurityConfigurerAdapter {             // klasa USUNIĘTA (od Security 6)
    //         protected void configure(HttpSecurity http) throws Exception {
    //             http.authorizeRequests()                                        // USUNIĘTE — jest authorizeHttpRequests
    //                 .antMatchers("/public/**").permitAll()                      // USUNIĘTE — jest requestMatchers
    //                 .anyRequest().authenticated()
    //                 .and().httpBasic();                                         // .and() USUNIĘTE — tylko lambdy
    //         }
    //     }
    //
    //   W Security 7 jest wyłącznie styl lambda (lambda DSL) i bean {@code SecurityFilterChain}. Klas {@code AntPathRequestMatcher}
    //   i {@code MvcRequestMatcher} też już nie ma (zostały {@code PathPatternRequestMatcher} i {@code RegexRequestMatcher}).
    //   A {@code http.build()} nie deklaruje już {@code throws Exception}, więc metoda beana nie musi go mieć.
    //
    // PUŁAPKA: (w testach) {@code @WebMvcTest} NIE ładuje skanowanych klas {@code @Configuration}. Nasz łańcuch jest metodą
    //   {@code @Bean} w takiej klasie, więc bez {@code @Import(Sec01SecurityConfig.class)} wycinek używa domyślnego łańcucha Boota
    //   i publiczny adres daje 401 (test Sec01SliceWithoutImportTest). Dlaczego to groźne? Test sprawdza wtedy reguły Boota, a nie
    //   Twoje — możesz zepsuć konfigurację, a test tego nie zauważy (albo odwrotnie: "naprawiasz" działający kod).
    //
    // DOBRA PRAKTYKA: konfigurację bezpieczeństwa trzymaj w osobnej klasie {@code @Configuration} (tu Sec01SecurityConfig),
    //   a w testach wycinka dołączaj ją jawnie przez {@code @Import}. Dlaczego? Jedna klasa = jedno miejsce, gdzie widać WSZYSTKIE
    //   reguły, a {@code @Import} sprawia, że test na pewno sprawdza właśnie je.

    // =================================================================================================
    // 4. 401 KONTRA 403 (Sec01BasicsTest.unauthorizedVersusForbidden, browserGoesToLoginForm)
    // =================================================================================================
    //
    // PYTANIE REKRUTACYJNE: "Czym różni się 401 od 403?"
    //
    //     | sytuacja                                  | status | nagłówek / zachowanie                         | co pomoże?         |
    //     |-------------------------------------------|--------|-----------------------------------------------|--------------------|
    //     | brak danych logowania (anonim)            | 401    | WWW-Authenticate: Basic realm="Realm", …      | zaloguj się        |
    //     | złe hasło albo nieznany login             | 401    | WWW-Authenticate: Basic …                     | popraw dane        |
    //     | anonim z przeglądarki (Accept: text/html) | 302    | Location: /login (formularz)                  | zaloguj się        |
    //     | zalogowany, ale bez roli (anna → admin)   | 403    | BEZ WWW-Authenticate                          | nic — nie wolno    |
    //     | zalogowany z rolą (admin → admin)         | 200    | —                                             | —                  |
    //
    //   • 401 Unauthorized — nazwa jest myląca: naprawdę znaczy "unauthenticated" (nieuwierzytelniony). Serwer nie wie, kim
    //     jesteś, i nagłówkiem {@code WWW-Authenticate} mówi, JAK się przedstawić. Wysyła go {@code BasicAuthenticationEntryPoint}
    //     (entry point = punkt wejścia — "co powiedzieć niezalogowanemu"). W Security 7 nagłówek ma też {@code charset="UTF-8"}.
    //   • 403 Forbidden — serwer WIE, kim jesteś, i odmawia. Ponowne logowanie tym samym kontem nic nie zmieni, więc nie ma
    //     nagłówka {@code WWW-Authenticate}.
    //   • Ponieważ włączyliśmy i Basic, i formularz, Spring wybiera punkt wejścia po nagłówkach żądania: przeglądarka
    //     (Accept: text/html) dostaje przekierowanie na formularz, klient API — 401 z prośbą o Basic. W MockMvc nagłówek
    //     {@code Location} jest względny ({@code /login}); prawdziwy Tomcat odsyła pełny adres ({@code http://localhost:8080/login}).
    //   • Decyzję podejmuje {@code ExceptionTranslationFilter}: łapie {@code AccessDeniedException} rzucony przez
    //     {@code AuthorizationFilter} i patrzy, kto pyta — anonim → punkt wejścia (401/302), zalogowany → 403.
    //
    // DOBRA PRAKTYKA: w API nie zamieniaj 403 na 404 "żeby ukryć istnienie zasobu", jeśli nie masz takiego wymogu, i nie zwracaj
    //   200 z treścią "brak dostępu". Dlaczego? Klienci (aplikacja mobilna, front) reagują na STATUS: 401 → pokaż ekran logowania,
    //   403 → pokaż "brak uprawnień". Zły status = zła reakcja klienta.

    // =================================================================================================
    // 5. KTO JEST ZALOGOWANY: Authentication, Principal, @AuthenticationPrincipal; PUŁAPKA /error
    //    (Sec01Controller, Sec01BasicsTest.whoAmI, withMockUser, Sec01ServerTest, Sec01ErrorPagePitfallTest)
    // =================================================================================================
    //
    // Kontroler nie sprawdza haseł — dostaje gotowy wynik. Spring MVC umie wstrzyknąć do metody (Sec01Controller.me):
    //   • {@code Authentication} — pełny obiekt: {@code getName()}, {@code getAuthorities()} (uprawnienia),
    //     {@code getPrincipal()} (obiekt użytkownika), {@code isAuthenticated()};
    //   • {@code java.security.Principal} — interfejs z Javy, tylko {@code getName()} (Authentication go rozszerza);
    //   • {@code @AuthenticationPrincipal UserDetails user} — od razu obiekt użytkownika z {@code UserDetailsService}
    //     (Sec03UsersPasswords pokaże własną klasę użytkownika).
    // Wszystkie trzy to ten sam użytkownik, ten sam obiekt co w {@code SecurityContextHolder} (test whoAmI: sameAsSecurityContext).
    //
    // NOWOŚĆ Security 7: po zalogowaniu hasłem lista uprawnień ma DODATKOWY wpis {@code FACTOR_PASSWORD} (test whoAmI:
    //   {@code ["FACTOR_PASSWORD", "ROLE_CUSTOMER"]}). To {@code FactorGrantedAuthority} — "czynnik", którym się zalogowałeś
    //   (hasło, token, kod jednorazowy…). Służy do logowania wieloskładnikowego (MFA): reguła może żądać np. hasła I kodu.
    //   Dla Ciebie znaczy to tyle: nie zakładaj, że {@code getAuthorities()} zawiera same role. {@code @WithMockUser} tego wpisu nie
    //   dodaje (test withMockUser) — on wkłada gotowy obiekt do kontekstu, bez logowania.
    //
    // DOBRA PRAKTYKA: identyfikuj użytkownika po {@code authentication.getName()} z kontekstu, a nie po {@code ?user=anna}
    //   w adresie czy polu w JSON-ie. Dlaczego? Parametr może podać każdy, a kontekst ustawił filtr po sprawdzeniu hasła.
    //
    // PUŁAPKA: strona /error (znany problem przy pierwszych krokach) — publiczny adres rzuca wyjątek, a klient zamiast 500 dostaje 401.
    //   Dlaczego? Kontroler rzuca wyjątek → Tomcat PRZEKAZUJE żądanie na stronę błędów {@code /error} (dispatch typu ERROR,
    //   s06_error_handling/Err01DefaultErrors) → filtry Spring Security działają też dla przekazań ERROR (sekcja 2) →
    //   {@code /error} nie jest na liście {@code permitAll()}, a pyta anonim → 401. Tak samo nieznany publiczny adres: zamiast 404
    //   jest 401 (test Sec01ErrorPagePitfallTest). Prawdziwa przyczyna (wyjątek, brak strony) ginie, a klient myśli, że to problem
    //   z logowaniem. Naprawa: {@code .requestMatchers("/error").permitAll()} (Sec01SecurityConfig; test Sec01ServerTest:
    //   500 i 404, a komunikat techniczny wyjątku nie wycieka — {@code /error} Boota go domyślnie ukrywa). Zalogowany użytkownik
    //   problemu nie ma (dla niego {@code anyRequest().authenticated()} przepuszcza {@code /error}), dlatego błąd łatwo przeoczyć.
    //   Pokazać to można tylko na prawdziwym serwerze — MockMvc nie robi przekazania na {@code /error}.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Sam starter: wszystko chronione; API → 401 + WWW-Authenticate: Basic, przeglądarka → 302 /login; użytkownik "user"
     *     z hasłem w logu — ale z resource-server/oauth2-client na classpath dopiero po ustawieniu spring.security.user.*.
     *   • Tomcat → DelegatingFilterProxy → FilterChainProxy (bean springSecurityFilterChain) → SecurityFilterChain (filtry)
     *     → DispatcherServlet. AuthorizationFilter zawsze na końcu.
     *   • SecurityContextHolder = ThreadLocal; inny wątek widzi null; filtr czyści go po żądaniu.
     *   • Własny @Bean SecurityFilterChain(HttpSecurity http) wyłącza defaultSecurityFilterChain Boota.
     *   • Security 7: tylko lambdy (authorizeHttpRequests, requestMatchers); brak WebSecurityConfigurerAdapter, antMatchers,
     *     authorizeRequests, and(); build() bez throws Exception.
     *   • 401 = nie wiem, kim jesteś (WWW-Authenticate); 403 = wiem, ale nie wolno (bez WWW-Authenticate).
     *   • Authentication / Principal / @AuthenticationPrincipal = ten sam użytkownik; po haśle Security 7 dodaje FACTOR_PASSWORD.
     *   • .requestMatchers("/error").permitAll() — inaczej błędy publicznych adresów wyglądają jak 401.
     *   • @WebMvcTest + @Import(SecurityConfig.class) — inaczej testujesz domyślne reguły Boota.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? W module s12-security uruchamiasz aplikację bez żadnej konfiguracji bezpieczeństwa i bez właściwości
     *      spring.security.user.*. Czy w logu zobaczysz wygenerowane hasło? Czy ktokolwiek się zaloguje?
     *   2. Wymień po kolei drogę żądania od Tomcata do kontrolera (DelegatingFilterProxy, FilterChainProxy, SecurityFilterChain,
     *      DispatcherServlet). Dlaczego AuthorizationFilter jest na końcu łańcucha?
     *   3. PYTANIE REKRUTACYJNE: czym różni się 401 od 403? Który z nich ma nagłówek WWW-Authenticate i dlaczego?
     *   4. Co się stanie? Publiczny adres (permitAll) rzuca IllegalStateException, a w regułach nie ma /error. Jaki status dostanie
     *      anonim na prawdziwym serwerze, a jaki zalogowany użytkownik?
     *   5. ZNAJDŹ BŁĄD: test {@code @WebMvcTest(Sec01Controller.class)} bez żadnego {@code @Import} sprawdza, że
     *      /sec01/public/hello daje 200 — i nie przechodzi (401). Konfiguracja aplikacji jest poprawna. Co jest nie tak z testem?
     *   6. Co się stanie? W metodzie kontrolera uruchamiasz {@code Thread.ofVirtual().start(...)} i w nowym wątku czytasz
     *      {@code SecurityContextHolder.getContext().getAuthentication()}. Co dostaniesz?
     *   7. ZNAJDŹ BŁĄD: kod z poradnika {@code extends WebSecurityConfigurerAdapter} z {@code http.authorizeRequests().antMatchers(...)
     *      .permitAll().and().httpBasic()} nie kompiluje się w projekcie na Boot 4. Dlaczego i jak go przepisać?
     *   8. Użytkownik zalogowany hasłem ma role CUSTOMER. Test sprawdza, że getAuthorities() to dokładnie [ROLE_CUSTOMER], i nie
     *      przechodzi. Co jeszcze jest na liście i po co?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, CurrentUser, StatusRules, ExerciseSecurityConfig; testy:
    // Sec01BasicsExercisesTest i Sec01ExerciseSecurityTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Rozwiązania wzorcowe: pakiet s12_security.solutions.sec01_basics. Adresy ćwiczeń mają prefiks /sec01/cw.
    //
    // ĆWICZENIE 1 (łatwe): powitanie — w ExerciseController.greeting zwróć "Dzień dobry, " + nazwa zalogowanego użytkownika.
    //   Podpowiedź: parametr Authentication już jest; patrz Sec01Controller.me (getName()).
    //
    // ĆWICZENIE 2 (łatwe): CurrentUser.name() — przeczytaj Authentication z SecurityContextHolder. Brak (null) albo token anonima
    //   ({@code AnonymousAuthenticationToken}) → "anonim", w przeciwnym razie getName().
    //   Podpowiedź: {@code SecurityContextHolder.getContext().getAuthentication()} i {@code instanceof}.
    //
    // ĆWICZENIE 3 (średnie): CurrentUser.hasRole("ADMIN") — czy wśród uprawnień jest "ROLE_ADMIN". Pamiętaj, że lista może mieć
    //   też FACTOR_PASSWORD. Podpowiedź: {@code getAuthorities().stream().map(GrantedAuthority::getAuthority)}.
    //
    // ĆWICZENIE 4 (średnie): StatusRules.statusFor(authentication, rola) — tabela z sekcji 4 w kodzie: 401 (null, anonim albo
    //   {@code isAuthenticated() == false}), 403 (brak "ROLE_" + rola), 200 (jest).
    //   Podpowiedź: {@code UsernamePasswordAuthenticationToken.unauthenticated(...)} ma {@code isAuthenticated() == false}.
    //
    // ĆWICZENIE 5 (trudniejsze): ExerciseSecurityConfig.filterChain — reguły dla /sec01/cw/**: public/** i /error dla każdego,
    //   admin/** tylko ADMIN, reszta zalogowani; httpBasic. Podpowiedź: wzoruj się na Sec01SecurityConfig; dopóki jest TODO,
    //   test zgłosi "Failed to load ApplicationContext" z przyczyną UnsupportedOperationException: TODO.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nie i nie. Moduł ma na classpath resource-server, więc Boot nie tworzy użytkownika "user" (warunek
     *      MissingAlternativeUserDetailsManagerOrUserPropertiesConfigured) i nie wypisuje hasła. Domyślny łańcuch i tak działa,
     *      więc wszystko daje 401 (test Sec01NoUserTest). Wystarczy ustawić spring.security.user.name (hasło w logu — test
     *      Sec01GeneratedPasswordTest) albo także .password (test Sec01UserPropertiesTest).
     *   2. Tomcat woła DelegatingFilterProxy (zwykły filtr serwletu zarejestrowany przez Boota) → ten oddaje pracę beanowi
     *      FilterChainProxy (springSecurityFilterChain) → FilterChainProxy wybiera pasujący SecurityFilterChain i przepuszcza
     *      żądanie przez jego filtry → DispatcherServlet → kontroler. AuthorizationFilter jest na końcu, bo decyzja "wolno?"
     *      wymaga wiedzy "kim jesteś", a tę dają wcześniejsze filtry (Basic, formularz, anonim).
     *   3. 401: serwer nie wie, kim jesteś (brak lub złe dane logowania) — ma WWW-Authenticate, bo mówi, jak się przedstawić.
     *      403: wie, kim jesteś, i odmawia — bez WWW-Authenticate, bo ponowne logowanie nic nie zmieni (test
     *      unauthorizedVersusForbidden).
     *   4. Anonim: 401 (przekazanie na /error też przechodzi przez filtry, a /error nie jest dozwolone). Zalogowany: 500, bo jego
     *      anyRequest().authenticated() przepuszcza na /error (test Sec01ErrorPagePitfallTest; poprawna wersja: Sec01ServerTest).
     *   5. @WebMvcTest nie ładuje skanowanych klas @Configuration, więc Sec01SecurityConfig nie działa i zostaje domyślny łańcuch
     *      Boota (wszystko wymaga logowania). Trzeba dopisać @Import(Sec01SecurityConfig.class) (test Sec01SliceWithoutImportTest).
     *   6. null — SecurityContextHolder trzyma kontekst w ThreadLocal, a nowy wątek ma własny, pusty schowek (test
     *      contextIsPerThread).
     *   7. Spring Security 7 usunął WebSecurityConfigurerAdapter, authorizeRequests, antMatchers i and(). Zamiast tego bean:
     *      {@code @Bean SecurityFilterChain chain(HttpSecurity http)} z
     *      {@code http.authorizeHttpRequests(a -> a.requestMatchers("/public/**").permitAll().anyRequest().authenticated())
     *      .httpBasic(Customizer.withDefaults()); return http.build();}
     *   8. FACTOR_PASSWORD — FactorGrantedAuthority z Security 7: informacja, że użytkownik zalogował się hasłem (podstawa
     *      logowania wieloskładnikowego). Test powinien sprawdzać, czy lista ZAWIERA ROLE_CUSTOMER, a nie, że jest jedyna
     *      (test whoAmI pokazuje pełną listę).
     */
    // </editor-fold>
}

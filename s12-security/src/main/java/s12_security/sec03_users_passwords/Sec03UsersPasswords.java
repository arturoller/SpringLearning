package s12_security.sec03_users_passwords;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Map;

/**
 * <pre>
 * TEMAT: Użytkownicy i hasła — UserDetailsService, PasswordEncoder (BCrypt, {bcrypt}, {noop}) i „kto jest zalogowany?”
 *        (user details = dane użytkownika; password encoder = koder haseł; hash = skrót)
 *
 * W SKRÓCIE:
 *   • Spring Security pyta bean UserDetailsService: „podaj dane użytkownika X” (login, SKRÓT hasła, uprawnienia,
 *     blokada). Skąd je weźmiesz — mapa, baza, LDAP — to Twoja sprawa.
 *   • Hasła NIGDY nie przechowujemy jawnie. Przechowujemy skrót z solą: BCrypt. Przy logowaniu PasswordEncoder
 *     liczy skrót wpisanego hasła tą samą solą i porównuje.
 *   • DelegatingPasswordEncoder zapisuje algorytm w prefiksie: {bcrypt}$2a$10$... — dzięki temu można kiedyś
 *     zmienić algorytm bez unieważniania starych haseł. {noop} = jawny tekst — tylko w przykładach.
 *   • Zalogowany użytkownik: @AuthenticationPrincipal w kontrolerze, Authentication jako parametr albo
 *     SecurityContextHolder (zmienna WĄTKU) w dowolnym miejscu.
 *
 * ANALOGIA: szatnia w teatrze.
 *   Szatniarz nie zapamiętuje Twojej twarzy — daje numerek (skrót). Gdy wracasz, porównuje numerek z wieszakiem.
 *   BCrypt idzie dalej: z samego numerka nie da się odtworzyć płaszcza (skrót jest jednokierunkowy), a dwie osoby
 *   z identycznym płaszczem dostają różne numerki (sól). Książka szatni (UserDetailsService) mówi tylko, który
 *   numerek pasuje do którego wieszaka — porównuje szatniarz (PasswordEncoder w DaoAuthenticationProvider).
 *
 * JAK TO DZIAŁA:
 *   logowanie przez HTTP Basic, krok po kroku:
 *   BasicAuthenticationFilter: „anna:anna-test-123” z nagłówka
 *     → AuthenticationManager (ProviderManager) → DaoAuthenticationProvider
 *         → UserDetailsService.loadUserByUsername("anna")          → User(anna, {bcrypt}$2a$10$..., ROLE_ADMIN)
 *         → sprawdzenie blokady (isEnabled, isAccountNonLocked...)
 *         → PasswordEncoder.matches("anna-test-123", "{bcrypt}$2a$10$...") → true
 *     → Authentication (bilet: login, uprawnienia; hasło USUNIĘTE) → SecurityContextHolder (wątek zapytania)
 *     → kontroler: @AuthenticationPrincipal / Authentication / SecurityContextHolder
 *
 * SŁÓWKA:
 *   hash = skrót (wynik funkcji jednokierunkowej); salt = sól (losowy dodatek do hasła przed liczeniem skrótu);
 *   cost / strength = koszt / siła (ile pracy kosztuje jeden skrót); encode = zakoduj (policz skrót);
 *   matches = pasuje; delegating = delegujący (przekazujący pracę innemu koderowi); noop = no operation (bez zmian);
 *   principal = podmiot (zalogowany); credentials = dane uwierzytelniające (hasło); disabled = wyłączone konto;
 *   bad credentials = złe dane logowania; thread local = zmienna lokalna wątku; upgrade = podnieść (do lepszego formatu).
 *
 * ZOBACZ TEŻ: t31_jdk_toolbox/Toolbox02HashingSecurity (skróty i sól w czystej Javie), s12_security/Sec01Basics
 *             (łańcuch filtrów), s12_security/Sec02Authorization (role i uprawnienia), s08_data_jpa/Jpa02Repositories
 *             (użytkownicy w prawdziwej bazie), t21_concurrency/Concurrency01Threads (wątki),
 *             t13_lambdas/Lambda02FunctionalInterfaces (UserDetailsService to interfejs funkcyjny).
 * </pre>
 */
public final class Sec03UsersPasswords {

    private Sec03UsersPasswords() {
    }

    // =================================================================================================
    // 1. UserDetailsService — skąd Spring Security zna użytkowników
    // =================================================================================================
    //
    // Interfejs ma JEDNĄ metodę: UserDetails loadUserByUsername(String username). ShopUserDetailsService szuka konta
    // w „bazie” (EmployeeAccountRepository) i buduje User.withUsername(...).password(SKRÓT).roles(...).disabled(...).
    // Nie ma konta → UsernameNotFoundException. Spring Security sam znajduje jedyny bean UserDetailsService i składa
    // z nim DaoAuthenticationProvider (dostawcę uwierzytelniania z „bazy”).
    //
    // Efekt (Sec03UsersPasswordsTest): anna z dobrym hasłem → 200; złe hasło, nieznany login, zablokowany marek → 401.
    //
    // DOBRA PRAKTYKA: (wbudowana w Spring Security) nieznany login i złe hasło dają TEN SAM wyjątek
    //   BadCredentialsException („Bad credentials”). Dlaczego? Inaczej atakujący sprawdzałby, które loginy istnieją
    //   (enumeracja użytkowników).
    //   DaoAuthenticationProvider liczy nawet skrót „na niby” dla nieznanego loginu, żeby czas odpowiedzi nie zdradził
    //   różnicy. Zablokowane konto → DisabledException („User is disabled”) — ale klient HTTP i tak widzi tylko 401.
    //
    // PUŁAPKA: komunikaty wyjątków Security są TŁUMACZONE na język systemu (SpringSecurityMessageSource). Na polskim
    //   Windowsie zobaczysz „Niepoprawne dane uwierzytelniające” i „Konto użytkownika jest wyłączone”, na angielskim —
    //   „Bad credentials” i „User is disabled”. Test, który porównuje tekst komunikatu, musi ustalić język
    //   (LocaleContextHolder.setLocale) — inaczej przejdzie u Ciebie, a padnie na serwerze CI z innym językiem.

    // =================================================================================================
    // 2. InMemoryUserDetailsManager — użytkownicy w pamięci
    // =================================================================================================
    //
    // Gotowa implementacja z Sec01 i Sec02. To nie tylko „usługa”, ale i MENEDŻER (UserDetailsManager): createUser,
    // updateUser, deleteUser, userExists, changePassword. Dobra do przykładów i testów; dane znikają po restarcie.
    //
    // Ciekawostka sprawdzona testem: InMemoryUserDetailsManager implementuje też UserDetailsPasswordService. Gdy
    // ktoś zaloguje się hasłem zapisanym w starym formacie ({noop}...), DaoAuthenticationProvider po udanym logowaniu
    // sam przekoduje je na {bcrypt} i zapisze (upgradeEncoding → updatePassword). Twoja usługa może zrobić to samo
    // dla bazy — wystarczy, że zaimplementuje UserDetailsPasswordService. Tak migruje się hasła bez resetu.

    // =================================================================================================
    // 3. BCrypt — skrót z solą i kosztem
    // =================================================================================================
    //
    // Skrót ≠ szyfrowanie. Szyfrowanie da się odwrócić kluczem; skrótu — nie. Przy logowaniu nie „odszyfrowujemy”
    // hasła z bazy, tylko liczymy skrót wpisanego hasła i porównujemy.
    //
    //   $2a$10$Aydxj54Ew6VDNf0dAofOt.EOnHbAGMd.wz6e9YfI3HXtrzBLLG1Nm   (prawdziwy skrót „anna-test-123”, 60 znaków)
    //   $2a$ = wersja, 10$ = koszt, potem 22 znaki soli (Aydxj54Ew6VDNf0dAofOt.) i 31 znaków skrótu
    //
    // • Sól — losowa przy KAŻDYM encode(): ten sam tekst daje różne skróty, a oba pasują (matches → true).
    //   Dlaczego to ważne? Dwie osoby z hasłem „lato2026” mają różne skróty, a gotowe tablice skrótów
    //   (rainbow tables — tęczowe tablice) są bezużyteczne.
    // • Koszt (domyślnie 10) — liczba rund to 2^koszt. Koszt 12 = 4 razy wolniej niż 10. Celowo WOLNO: logowanie
    //   jednej osoby tego nie odczuje, a łamanie miliardów prób — tak.
    // • Sól i koszt są ZAPISANE w skrócie, więc new BCryptPasswordEncoder() sprawdzi też skrót z kosztem 12.
    //
    // PUŁAPKA: nie używaj szybkich skrótów (MD5, SHA-256) do haseł — nawet z solą. Dlaczego? Karta graficzna liczy
    //   miliardy SHA-256 na sekundę; BCrypt jest projektowo wolny. (Do sum kontrolnych plików SHA-256 jest w porządku —
    //   t31_jdk_toolbox/Toolbox02HashingSecurity.)

    // =================================================================================================
    // 4. DelegatingPasswordEncoder i prefiksy {bcrypt}, {noop}
    // =================================================================================================
    //
    //   PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    //   encoder.encode("anna-test-123")  → "{bcrypt}$2a$10$..."        (domyślny algorytm: bcrypt)
    //   encoder.matches(..., "{noop}anna-test-123") → true             ({noop} = porównaj jawny tekst)
    //   encoder.upgradeEncoding("{noop}...") → true                    (warto przekodować)
    //
    // Prefiks w nawiasach klamrowych mówi, KTÓRYM koderem sprawdzić skrót. Fabryka zna m.in. bcrypt (domyślny),
    // argon2, scrypt, pbkdf2, noop, SHA-256. Bean PasswordEncoder z Sec03SecurityConfig Spring Security bierze do
    // logowania; bez takiego beana i tak używa kodera delegującego — dlatego {noop} działało w Sec01 i Sec02.
    //
    // PUŁAPKA: skrót BEZ prefiksu (np. z importu ze starej bazy: „$2a$10$...”) → przy logowaniu
    //   IllegalArgumentException „Given that there is no default password encoder configured, each password must have
    //   a password encoding prefix. Please either prefix this password with '{noop}' or set a default password encoder
    //   in `DelegatingPasswordEncoder`.” — a klient dostaje 500, nie 401 (sprawdzone uruchomieniem). Nieznany prefiks,
    //   np. {sha512} → „There is no password encoder mapped for the id 'sha512'. ...”. Starsze poradniki pokazują
    //   inny komunikat („There is no PasswordEncoder mapped for the id "null"”) — w Security 7.1 brzmi jak wyżej.
    //
    // PUŁAPKA: {noop} i hasła wpisane w kod. W Sec01/Sec02 to celowe uproszczenie z hasłami TESTOWYMI. W prawdziwej
    //   aplikacji hasło w kodzie = hasło w repozytorium Git, w historii na zawsze, u każdego, kto sklonuje projekt.
    //   Hasła ustawia użytkownik (formularz), a w bazie jest tylko skrót {bcrypt}.
    //
    // DOBRA PRAKTYKA: definiuj bean PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder(),
    //   a nie gołe new BCryptPasswordEncoder(). Dlaczego? Gdy za kilka lat zmienisz algorytm (np. na argon2), stare
    //   skróty {bcrypt} nadal zadziałają, a nowe zapiszą się w nowym formacie.
    //   Stary poradnik: User.withDefaultPasswordEncoder() — w Security 7.1 przestarzałe (deprecated), tylko do demo.

    // =================================================================================================
    // 5. KTO JEST ZALOGOWANY? — @AuthenticationPrincipal, Authentication, SecurityContextHolder
    // =================================================================================================
    //
    //   @GetMapping public Me me(@AuthenticationPrincipal UserDetails user)          // principal z UserDetailsService
    //   @GetMapping public ... authentication(Authentication authentication)         // cały bilet
    //   SecurityContextHolder.getContext().getAuthentication()                       // w serwisie, bez parametrów
    //
    // Po zalogowaniu Authentication to UsernamePasswordAuthenticationToken, a hasło jest z niego USUNIĘTE
    // (ProviderManager woła eraseCredentials — credentials = null, także getPassword() principala = null).
    // Dlaczego? Hasło nie jest już potrzebne, a im krócej leży w pamięci, tym mniejsze ryzyko wycieku.
    //
    // Nowość Security 7 (sprawdzona testem): uprawnienia BILETU (authentication.getAuthorities()) to nie tylko role
    // z UserDetails. DaoAuthenticationProvider dopisuje „FACTOR_PASSWORD” (FactorGrantedAuthority — czynnik: „zalogował
    // się hasłem”). Anna ma więc w bilecie [FACTOR_PASSWORD, ROLE_ADMIN], a w UserDetails tylko [ROLE_ADMIN]. To podstawa
    // logowania wieloskładnikowego (MFA, @EnableMultiFactorAuthentication): reguła może wymagać np. hasła I kodu
    // jednorazowego. Starsze poradniki tego nie znają — nie zdziw się, gdy zobaczysz FACTOR_... w logach albo testach.
    //
    // PUŁAPKA: SecurityContextHolder to zmienna WĄTKU (ThreadLocal). Nowy wątek nie widzi zalogowanego — „nikt”
    //   (Sec03UsersPasswordsTest: sameThread = anna, newThread = nikt). Dotyczy też własnych pul wątków i zadań
    //   asynchronicznych. Rozwiązanie: opakuj zadanie w DelegatingSecurityContextRunnable (albo pulę
    //   w DelegatingSecurityContextExecutor) — kontekst zostanie przeniesiony (newThreadWithDelegatingRunnable = anna).
    //
    // Niezalogowany na adresie permitAll() też ma Authentication: AnonymousAuthenticationToken o nazwie
    // „anonymousUser” (filtr AnonymousAuthenticationFilter) — więc „authentication != null” NIE znaczy „zalogowany”.

    // =================================================================================================
    // 6. HASŁO NIE MOŻE WYCIEC — odpowiedź to DTO, nie UserDetails
    // =================================================================================================
    //
    // MeController.leaky() zwraca UserDetails świeżo wczytany z „bazy”. Jackson zamienia na JSON wszystkie gettery,
    // więc klient dostaje "password":"{bcrypt}$2a$10$..." (Sec03UsersPasswordsTest.leakyEndpointExposesHash).
    // Skrót to nie hasło, ale z nim można łamać hasło offline, bez limitu prób.
    //
    // DOBRA PRAKTYKA: odpowiadaj rekordem DTO (MeController.Me: username + authorities) — wypisujesz pola, które
    //   klient MA zobaczyć. Dlaczego? Nowe pole w encji (np. pesel) nie trafi do API przypadkiem. Test, że hasło
    //   nie wycieka: Sec06SecurityTesting.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • UserDetailsService.loadUserByUsername(login) → User.withUsername(..).password(SKRÓT).roles(..).disabled(..);
     *     brak → UsernameNotFoundException (klient i tak widzi 401, jak przy złym haśle).
     *   • Hasło porównuje DaoAuthenticationProvider + PasswordEncoder.matches(wpisane, skrót).
     *   • BCrypt: $2a$10$ + sól + skrót (60 znaków); sól losowa przy każdym encode; koszt 10 = 2^10 rund.
     *   • Bean PasswordEncoder: PasswordEncoderFactories.createDelegatingPasswordEncoder() → {bcrypt}...
     *   • {noop} = jawny tekst, tylko w przykładach; skrót bez prefiksu → IllegalArgumentException → 500.
     *   • InMemoryUserDetailsManager po zalogowaniu sam podnosi {noop} do {bcrypt} (UserDetailsPasswordService).
     *   • Zalogowany: @AuthenticationPrincipal, parametr Authentication, SecurityContextHolder (zmienna wątku!).
     *   • Security 7: uprawnienia biletu zawierają też FACTOR_PASSWORD (czym się zalogował — podstawa MFA).
     *   • Nowy wątek → DelegatingSecurityContextRunnable. Odpowiedź z danymi użytkownika → DTO, nigdy UserDetails.
     *   • Komunikaty Security są tłumaczone (po polsku: „Niepoprawne dane uwierzytelniające”).
     *
     * PYTANIA KONTROLNE:
     *   1. Kto w Spring Security porównuje hasło: UserDetailsService czy ktoś inny? Co dokładnie zwraca UserDetailsService?
     *   2. Dlaczego dwa wywołania encode("anna-test-123") dają różne wyniki, a oba przechodzą matches?
     *   3. Co się stanie? Zalogujesz się w Sec03Application jako „nieznany” — a potem jako anna ze złym hasłem.
     *      Czy odpowiedzi będą się różnić? Dlaczego tak ma być?
     *   4. ZNAJDŹ BŁĄD: bean PasswordEncoder = createDelegatingPasswordEncoder(), a w bazie skróty wyglądają tak:
     *      „$2a$10$Aydxj54Ew6VDNf0dAofOt...”. Co dostanie klient przy logowaniu?
     *   5. Co się stanie? W serwisie uruchomisz new Thread(() -> System.out.println(
     *      SecurityContextHolder.getContext().getAuthentication())) w trakcie zapytania anny. Co zostanie wypisane?
     *   6. ZNAJDŹ BŁĄD: @GetMapping("/api/me") public UserDetails me(Authentication a) {
     *      return users.loadUserByUsername(a.getName()); }
     *   7. Po co prefiks {bcrypt}, skoro BCrypt i tak ma własny znacznik $2a$?
     *   8. Dlaczego do haseł nie używamy SHA-256, skoro to „bezpieczny” skrót?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Sec03UsersPasswordsExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): czy tekst wygląda jak skrót BCrypt? Format: "$2a$", "$2b$" albo "$2y$", dwie cyfry kosztu,
     * "$", potem dokładnie 53 znaki z zestawu: . / A-Z a-z 0-9. Razem 60 znaków. null → false.
     * Podpowiedź: wyrażenie regularne i matches() (t04_strings/Strings05Regex); pamiętaj, że $ w regex trzeba poprzedzić \\.
     */
    static boolean exercise1(String hash) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zamień konto z „bazy” na UserDetails: login, skrót hasła, role (bez prefiksu w koncie),
     * konto nieaktywne → disabled. Przykład: ("ewa", "{noop}x", ["USER", "HR"], false) → uprawnienia ROLE_USER, ROLE_HR,
     * isEnabled() = false.
     * Podpowiedź: ShopUserDetailsService.
     */
    static UserDetails exercise2(EmployeeAccount account) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć UserDetailsService (wystarczy lambda — to interfejs z jedną metodą) oparty na mapie
     * login → konto. Login ma być niewrażliwy na wielkość liter: "ANNA" znajduje konto "anna" (klucze w mapie są małymi
     * literami), a getUsername() zwraca "anna". Brak konta → UsernameNotFoundException.
     * Podpowiedź: toLowerCase(Locale.ROOT); do budowy UserDetails użyj swojego exercise2.
     */
    static UserDetailsService exercise3(Map<String, EmployeeAccount> accounts) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć login zalogowanego użytkownika z SecurityContextHolder albo "anonim", gdy:
     * w kontekście nie ma Authentication, Authentication nie jest uwierzytelnione (isAuthenticated() = false) albo jest
     * to AnonymousAuthenticationToken (niezalogowany na publicznym adresie).
     * Podpowiedź: sekcja 5; instanceof AnonymousAuthenticationToken.
     */
    static String exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Porównuje DaoAuthenticationProvider za pomocą PasswordEncoder.matches. UserDetailsService zwraca tylko dane:
     *      login, SKRÓT hasła, uprawnienia i stan konta (aktywne, zablokowane, wygasłe).
     *   2. BCrypt losuje nową sól przy każdym encode i zapisuje ją w wyniku. matches odczytuje sól (i koszt) ze skrótu,
     *      liczy skrót wpisanego hasła tą samą solą i porównuje. Sprawdzone testem
     *      Sec03UsersPasswordsEncodersTest.bcryptSaltsEveryHash.
     *   3. Obie odpowiedzi to 401 z tym samym nagłówkiem WWW-Authenticate — klient nie wie, czy login istnieje.
     *      W środku oba przypadki kończą się BadCredentialsException („Bad credentials”). Tak ma być, bo inaczej
     *      atakujący mógłby zgadywać istniejące loginy. Sprawdzone testami Sec03UsersPasswordsTest.loginOverHttp
     *      i daoAuthenticationProviderHidesUnknownUsers.
     *   4. Skróty nie mają prefiksu {bcrypt}, więc DelegatingPasswordEncoder rzuca IllegalArgumentException („Given that
     *      there is no default password encoder configured, each password must have a password encoding prefix...”).
     *      Klient dostaje 500 Internal Server Error, a w logu jest ERROR. Sprawdzone uruchomieniem (aplikacja ze skrótem
     *      bez prefiksu → 500). Naprawa: dopisz {bcrypt} do skrótów w bazie (np. jednorazową migracją) albo ustaw
     *      koder domyślny (setDefaultPasswordEncoderForMatches).
     *   5. null — nowy wątek ma pusty kontekst (ThreadLocal), więc getAuthentication() zwraca null. Sprawdzone testem
     *      Sec03UsersPasswordsTest.securityContextIsThreadLocal (nowy wątek widzi „nikt”). Rozwiązanie:
     *      DelegatingSecurityContextRunnable.
     *   6. Metoda zwraca cały UserDetails z „bazy” — Jackson wypisze skrót hasła ("password":"{bcrypt}$2a$10$...").
     *      Zwracaj DTO (np. rekord z loginem i uprawnieniami). Sprawdzone testem leakyEndpointExposesHash.
     *   7. Prefiks wybiera KODER w DelegatingPasswordEncoder, niezależnie od formatu algorytmu. Dzięki temu w jednej bazie
     *      mogą być skróty z różnych algorytmów ({bcrypt}, {argon2}, stare {noop}), a zmiana domyślnego algorytmu nie
     *      psuje starych haseł; upgradeEncoding podpowiada, które przekodować.
     *   8. SHA-256 jest projektowo SZYBKI — dobry do sum kontrolnych, zły do haseł: atakujący z kartą graficzną sprawdzi
     *      miliardy haseł na sekundę. BCrypt (albo argon2, scrypt) jest celowo wolny i ma regulowany koszt.
     */
    // </editor-fold>
}

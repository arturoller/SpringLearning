package s12_security.sec03_users_passwords;

/**
 * <pre>
 * TEMAT: Użytkownicy i hasła — UserDetailsService na JPA, PasswordEncoder (BCrypt, DelegatingPasswordEncoder), rejestracja,
 *        konta wyłączone, zmiana i "podnoszenie" hasła
 *        (user details = dane użytkownika; password encoder = koder haseł, w praktyce: funkcja haszująca hasła;
 *         hash = skrót, wynik funkcji jednokierunkowej; salt = sól, losowy dodatek do hasła; delegating = delegujący)
 *
 * W SKRÓCIE:
 *   Spring Security nie wie, gdzie trzymasz użytkowników. Pyta o nich jeden interfejs: {@code UserDetailsService} —
 *   "daj mi użytkownika o tej nazwie". W Sec01 i Sec02 odpowiadał {@code InMemoryUserDetailsManager} (lista w pamięci);
 *   tu odpowiada nasz {@code JpaUserDetailsService}, który czyta tabelę {@code app_users} (Flyway + JPA). Haseł NIGDY nie
 *   zapisujemy jawnie ani zwykłym SHA-256: tylko wolną funkcją z solą, czyli BCryptem, przez {@code PasswordEncoder}.
 *   {@code DelegatingPasswordEncoder} dopisuje do hasha prefiks algorytmu ({@code {bcrypt}…}), dzięki czemu stare i nowe
 *   hashe żyją obok siebie, a {@code UserDetailsPasswordService} pozwala Springowi samemu przepisać stary hash przy logowaniu.
 *   Rejestracja haszuje hasło w serwisie, a do klienta wraca DTO — nigdy encja z hashem.
 *
 * ANALOGIA: szatnia w teatrze.
 *   Nie dajesz szatniarzowi zapamiętać swojego płaszcza "na słowo" (hasło jawne), tylko dostajesz numerek. Szatniarz
 *   (Spring Security) nie ma pojęcia, gdzie wiszą płaszcze — pyta kierownika szatni (UserDetailsService): "daj płaszcz numer
 *   214". Hash hasła jest jak odcisk palca: z odcisku nie odtworzysz palca, ale łatwo sprawdzisz, czy ten palec pasuje.
 *   Sól to rękawiczka z losowym wzorem: dwie osoby o "takim samym palcu" zostawiają różne odciski, więc złodziej nie
 *   rozpozna od razu wszystkich, którzy mają hasło "123456".
 *
 * JAK TO DZIAŁA:
 *   1. Logowanie Basic: {@code BasicAuthenticationFilter} → {@code AuthenticationManager} → {@code DaoAuthenticationProvider}.
 *   2. Provider woła {@code userDetailsService.loadUserByUsername(nazwa)} → dostaje {@code UserDetails} (nazwa, hash, role,
 *      flagi: enabled, accountNonLocked…). Brak użytkownika → {@code UsernameNotFoundException}.
 *   3. Sprawdza flagi konta (wyłączone → {@code DisabledException}), potem {@code passwordEncoder.matches(hasło, hash)}
 *      (złe → {@code BadCredentialsException}). Każdy z tych wyjątków to dla klienta 401.
 *   4. Po sukcesie: jeśli {@code passwordEncoder.upgradeEncoding(hash)} mówi "przestarzały", a w kontekście jest bean
 *      {@code UserDetailsPasswordService} — provider haszuje hasło od nowa i woła {@code updatePassword}.
 *   5. Spring Security sam składa provider z NASZYCH beanów: jedynego {@code UserDetailsService}, jedynego
 *      {@code PasswordEncoder} i jedynego {@code UserDetailsPasswordService} — nie trzeba niczego "podpinać".
 *
 * SŁÓWKA:
 *   user details service = usługa danych użytkownika; password encoder = koder haseł; encode = zakoduj (tu: zahaszuj);
 *   matches = pasuje; upgrade encoding = podnieś kodowanie (przepisz na lepszy algorytm); strength / cost = siła / koszt
 *   (liczba rund BCrypt); salt = sól; rainbow table = tęczowa tablica (gotowe skróty popularnych haseł); brute force = atak
 *   siłowy (zgadywanie po kolei); enabled / disabled = włączone / wyłączone; user enumeration = wyliczanie kont (sprawdzanie,
 *   które loginy istnieją); timing attack = atak czasowy (wnioskowanie z czasu odpowiedzi); legacy = stary, odziedziczony.
 *
 * ZOBACZ TEŻ: s12_security/Sec01Basics (filtry, Basic), s12_security/Sec02Authorization (role w regułach),
 *             s08_data_jpa/Jpa06FlywayWithJpa (Flyway + ddl-auto=validate), s05_validation/Val02ValidInControllers (@Valid),
 *             s06_error_handling/Err04ProblemDetail (409 jako ProblemDetail), s09_jpa_relations/Rel06EntityToDto (DTO zamiast encji),
 *             s10_transactions/Tx01Transactional (transakcja w serwisie), t31_jdk_toolbox/Toolbox02HashingSecurity (skróty w JDK).
 * </pre>
 */
public final class Sec03UsersPasswords {

    private Sec03UsersPasswords() {
    }

    // =================================================================================================
    // 1. UserDetailsService I UserDetails: w pamięci kontra baza (JpaUserDetailsService, AppUser, V1__create_app_users.sql,
    //    Sec03InMemoryUsersTest, Sec03UserDetailsServiceTest)
    // =================================================================================================
    //
    // Interfejs ma JEDNĄ metodę:
    //
    //     public interface UserDetailsService {
    //         UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
    //     }
    //
    // {@code UserDetails} to "karta użytkownika" dla Spring Security: {@code getUsername()}, {@code getPassword()} (HASH, nie
    // hasło), {@code getAuthorities()} (role i uprawnienia), {@code isEnabled()}, {@code isAccountNonLocked()},
    // {@code isAccountNonExpired()}, {@code isCredentialsNonExpired()}. Gotowa implementacja: {@code User} z builderem
    // {@code User.withUsername(…).password(…).roles(…).disabled(…).build()}.
    //
    //     | sposób                        | gdzie są dane     | po restarcie | do czego                                   |
    //     |-------------------------------|-------------------|--------------|--------------------------------------------|
    //     | InMemoryUserDetailsManager    | mapa w pamięci    | znikają      | nauka, testy, prototyp (Sec01, Sec02)      |
    //     | własny serwis na JPA (tu)     | tabela app_users  | zostają      | prawdziwa aplikacja z rejestracją          |
    //     | JdbcUserDetailsManager        | gotowe tabele SQL | zostają      | gdy pasuje schemat users/authorities       |
    //
    // Nasza tabela (Flyway V1): {@code username} z UNIQUE, {@code password_hash}, {@code roles} (napis "CUSTOMER,EMPLOYEE"),
    // {@code enabled}. Encja {@link AppUser} to obiekt BAZY; {@link JpaUserDetailsService} tłumaczy ją na {@code UserDetails}
    // (test loadsUserFromDatabase: hash {@code {bcrypt}…}, rola {@code ROLE_CUSTOMER}, a konto "jan" ma {@code enabled = false}).
    // Dlaczego nie zrobić, żeby encja implementowała {@code UserDetails}? Można, ale wtedy encja JPA zależy od Spring Security
    // i miesza dwie odpowiedzialności; osobny "tłumacz" jest prostszy do zmiany i testowania.
    //
    // PUŁAPKA: {@code loadUserByUsername} zwracające {@code null} dla nieznanej nazwy. Dlaczego? Umowa interfejsu mówi:
    //   rzuć {@code UsernameNotFoundException} (test unknownUser). {@code null} skończy się wyjątkiem
    //   {@code InternalAuthenticationServiceException} zamiast zwykłego 401 z "Bad credentials".
    //
    // DOBRA PRAKTYKA: w aplikacji ma być JEDEN bean {@code UserDetailsService}. Dlaczego? Spring Security buduje logowanie
    //   automatycznie tylko wtedy, gdy jest dokładnie jeden; przy dwóch nie wie, którego użyć, i logowanie hasłem nie zostanie
    //   skonfigurowane (zostaje tylko ostrzeżenie w logu).

    // =================================================================================================
    // 2. PasswordEncoder: BCrypt, DelegatingPasswordEncoder, dlaczego nie SHA-256 (Sec03SecurityConfig.passwordEncoder,
    //    Sec03PasswordEncoderTest)
    // =================================================================================================
    //
    // Zasada: serwer NIE MOŻE znać haseł. Trzyma tylko wynik funkcji jednokierunkowej (hash). Przy logowaniu liczy hash
    // z podanego hasła i porównuje. Gdy baza wycieknie, złodziej ma hashe, nie hasła — i musi je ZGADYWAĆ.
    //
    // Dlaczego nie SHA-256 / MD5 / SHA-1 (t31_jdk_toolbox/Toolbox02HashingSecurity)?
    //   • są SZYBKIE: zwykła karta graficzna liczy miliardy SHA-256 na sekundę — zgadywanie wszystkich krótkich haseł trwa chwilę;
    //   • bez soli to samo hasło daje zawsze ten sam skrót (test sha256IsDeterministic): wszyscy z hasłem "123456" mają ten
    //     sam wpis, a gotowe "tęczowe tablice" mają skróty milionów popularnych haseł.
    // A hasło jawne ({@code {noop}}) albo zaszyfrowane kluczem z serwera? Wyciek bazy = wyciek haseł (a ludzie używają tych samych
    // haseł w wielu miejscach). Szyfrowanie da się odwrócić kluczem — haszowanie nie.
    //
    // BCrypt ({@code BCryptPasswordEncoder}) został zaprojektowany do haseł:
    //   • SÓL: losowa przy każdym {@code encode} — to samo hasło daje dwa różne hashe, a {@code matches} i tak rozpoznaje oba,
    //     bo sól jest zapisana w hashu (test bcryptSaltsEveryHash);
    //   • KOSZT (strength, domyślnie 10, zakres 4–31): 2^koszt rund, celowo wolno (dziesiątki milisekund na hash). Logowanie
    //     użytkownika tego nie odczuje, zgadywanie miliardów haseł — bardzo. Koszt +1 = dwa razy wolniej;
    //   • budowa: {@code $2a$10$} + 22 znaki soli + 31 znaków wyniku = 60 znaków (dlatego kolumna ma VARCHAR(100) z zapasem
    //     na prefiks {@code {bcrypt}}). Koszt jest w hashu, więc można go później podnieść (test bcryptStrength).
    //
    // PUŁAPKA: BCrypt bierze pod uwagę tylko pierwsze 72 BAJTY hasła. Dlaczego to groźne? {@code encode} dłuższego hasła rzuca
    //   {@code IllegalArgumentException: password cannot be more than 72 bytes}, a {@code matches} po cichu UCINA resztę —
    //   hasło "72 znaki + cokolwiek" pasuje do hasha z samych 72 znaków (test bcrypt72Bytes). Polska litera w UTF-8 to 2 bajty,
    //   więc 37 znaków "ą" już przekracza limit. Dlatego {@link RegistrationRequest} ma {@code @Size(max = 64)}.
    //
    // {@code DelegatingPasswordEncoder} — koder, który sam niczego nie liczy, tylko wybiera algorytm po prefiksie:
    //
    //     PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    //     encoder.encode("haslo-testowe");                      // {bcrypt}$2a$10$… — NOWE hasła zawsze BCryptem
    //     encoder.matches("haslo-testowe", "{noop}haslo-testowe"); // true — stary hash jawny dalej działa
    //     encoder.upgradeEncoding("{noop}haslo-testowe");          // true — "przepisz mnie przy okazji" (sekcja 4)
    //
    //   Fabryka zna m.in. identyfikatory {@code bcrypt} (domyślny), {@code noop}, {@code pbkdf2}, {@code scrypt},
    //   {@code argon2}, {@code SHA-256}, {@code MD5} (sprawdzone w {@code PasswordEncoderFactories} 7.1.1). Po co prefiks? Gdy
    //   za kilka lat zmienisz algorytm, nie musisz znać haseł, żeby przepisać bazę: nowe hasła dostaną nowy prefiks, stare
    //   zostaną rozpoznane po swoim (test delegatingEncoder). Bean {@code PasswordEncoder} wystarczy zadeklarować —
    //   Spring Security użyje go sam przy logowaniu, a my wstrzykujemy go do serwisu przy rejestracji.
    //
    // PUŁAPKA: hash BCrypt bez prefiksu ({@code $2a$10$…}) w bazie, a w aplikacji {@code DelegatingPasswordEncoder}. Dlaczego?
    //   Koder nie wie, którego algorytmu użyć, i rzuca {@code IllegalArgumentException: … each password must have a password
    //   encoding prefix …} (test hashWithoutPrefix). Przy migracji starych danych dopisz prefiks SQL-em
    //   ({@code '{bcrypt}' || password_hash}) albo ustaw {@code setDefaultPasswordEncoderForMatches(...)}.
    //
    // Argon2 ({@code Argon2PasswordEncoder}) — nowocześniejszy (zwycięzca konkursu Password Hashing Competition, celowo
    // "pamięciożerny", co utrudnia ataki kartami graficznymi). Klasa jest w spring-security-crypto, ale liczy hash biblioteką
    // Bouncy Castle (zależność {@code org.bouncycastle:bcprov-jdk18on}); bez niej leci {@code NoClassDefFoundError} (test
    // argon2NeedsBouncyCastle). Security 7 ma też kodery oparte na bibliotece Password4j (pakiet
    // {@code org.springframework.security.crypto.password4j}) — również z dodatkową zależnością. BCrypt jest wbudowany i wciąż
    // jest rozsądnym, domyślnym wyborem.
    //
    // PYTANIE REKRUTACYJNE: "Jak przechowywać hasła?" — nigdy jawnie ani zwykłym szybkim skrótem; wolną funkcją z solą
    //   (BCrypt, Argon2, scrypt, PBKDF2) przez {@code PasswordEncoder}, najlepiej {@code DelegatingPasswordEncoder}, żeby dało
    //   się później zmienić algorytm.

    // =================================================================================================
    // 3. REJESTRACJA: POST /sec03/register (UserAccountController, UserAccountService, Sec03ErrorHandler,
    //    Sec03AccountsTest.registerAndLogin, duplicateAndInvalid, seededUser)
    // =================================================================================================
    //
    // Droga hasła przy rejestracji:
    //
    //     JSON {"username":"ola","password":"…"} → @Valid RegistrationRequest (s05_validation) → UserAccountService.register:
    //        existsByUsername? → 409 │ passwordEncoder.encode(hasło) → new AppUser(…, hash, "CUSTOMER", true) → saveAndFlush
    //     → UserView {"username":"ola","roles":["CUSTOMER"]} (201)
    //
    //   • Adres {@code POST /sec03/register} ma {@code permitAll()} — inaczej nikt nowy by się nie zarejestrował.
    //   • Haszowanie jest w SERWISIE: kontroler nie zna {@code PasswordEncoder}, encja nigdy nie widzi hasła. Rola nadawana
    //     przez serwer ({@code CUSTOMER}), NIE z żądania — inaczej każdy zarejestrowałby się jako ADMIN.
    //   • Zaraz po rejestracji nowe konto loguje się Basic (test registerAndLogin): {@code JpaUserDetailsService} czyta
    //     świeży wiersz, a {@code matches} rozpoznaje hash. W bazie leży {@code {bcrypt}$2a$10$…}, a nie hasło.
    //   • Zajęta nazwa → {@code UsernameTakenException} → 409 ProblemDetail z {@code code = USERNAME_TAKEN}; za krótkie hasło
    //     → 400 z walidacji, a konto nie powstaje (test duplicateAndInvalid).
    //
    // PUŁAPKA: samo {@code existsByUsername} przed zapisem. Dlaczego? Dwie rejestracje "ola" w tej samej milisekundzie obie
    //   usłyszą "nie istnieje" i obie spróbują zapisać. Ostatnią linią obrony jest UNIQUE w bazie: drugi zapis rzuca
    //   {@code DataIntegrityViolationException}, który serwis zamienia na ten sam 409. Sprawdzenie w kodzie daje ładny
    //   komunikat, ograniczenie w bazie daje PEWNOŚĆ.
    //
    // DOBRA PRAKTYKA: wymagaj DŁUGICH haseł (tu co najmniej 12 znaków), a nie skomplikowanych reguł "duża litera + cyfra +
    //   znak specjalny". Dlaczego? Długość najbardziej utrudnia zgadywanie, a wymuszone "Haslo1!" ludzie zapisują na
    //   karteczkach. Zalecenia NIST mówią to samo; dodatkowo warto sprawdzać hasła z wycieków
    //   ({@code CompromisedPasswordChecker} w Spring Security — wzmianka).

    // =================================================================================================
    // 4. KONTO WYŁĄCZONE, ZMIANA HASŁA, PRZEPISANIE STAREGO HASHA (JpaUserDetailsService.updatePassword,
    //    Sec03AccountsTest.disabledAccount, changePassword, passwordUpgradeOnLogin, Sec03AuthenticationProviderTest)
    // =================================================================================================
    //
    // Konto wyłączone: {@code .disabled(!user.isEnabled())} w {@code UserDetails}. Provider rzuca {@code DisabledException},
    // klient dostaje 401 — nawet z poprawnym hasłem (test disabledAccount). Ciekawostka sprawdzona testem disabledAccount
    // (Sec03AuthenticationProviderTest): stan konta jest sprawdzany PRZED hasłem, więc {@code DisabledException} leci także
    // przy ZŁYM haśle. Klient HTTP tego nie widzi (zawsze to samo 401), ale Twoje logi i zdarzenia logowania — tak.
    // Po co wyłączać zamiast usuwać? Zostaje historia zamówień, a konto da się przywrócić.
    //
    // Zmiana hasła ({@code PUT /sec03/me/password}): serwis wymaga STAREGO hasła ({@code matches}), dopiero potem haszuje
    // nowe. Dlaczego? Ktoś przy cudzym, niezablokowanym komputerze nie przejmie konta jednym żądaniem. Test changePassword:
    // złe stare hasło → 400 {@code WRONG_PASSWORD}; poprawne → 204; potem stare hasło daje 401, nowe 200. Przy Basic
    // klient po prostu wysyła nowe hasło od następnego żądania; przy sesjach trzeba by jeszcze wylogować inne sesje.
    //
    // Przepisanie starego hasha ("password upgrade"): w bazie jest konto "stary" z {@code {noop}haslo-testowe} (np. z dawnego
    // systemu). Nie znamy jego hasła — ale poznajemy je na chwilę przy KAŻDYM udanym logowaniu. Wtedy:
    //
    //     DaoAuthenticationProvider: matches(hasło, "{noop}…") == true
    //        → passwordEncoder.upgradeEncoding("{noop}…") == true (inny algorytm niż domyślny bcrypt)
    //        → nowy hash = passwordEncoder.encode(hasło)                         // {bcrypt}$2a$10$…
    //        → userDetailsPasswordService.updatePassword(user, nowy hash)        // nasz JpaUserDetailsService zapisuje go
    //
    //   Test passwordUpgradeOnLogin: przed logowaniem {@code {noop}haslo-testowe}, po jednym logowaniu {@code {bcrypt}$2a$10$…},
    //   hasło dla użytkownika to samo. Wystarczyło, że {@code JpaUserDetailsService} implementuje także
    //   {@code UserDetailsPasswordService} — Spring Security podłączył go sam. Tak samo zadziała podniesienie kosztu BCrypt
    //   (hash z {@code $2a$04$} przy koderze z kosztem 10 też ma {@code upgradeEncoding == true}).
    //   UWAGA: {@code updatePassword} dostaje JUŻ HASH, nie hasło — nie haszuj go drugi raz.
    //
    // PUŁAPKA: {@code UserDetailsService}, który przy każdym logowaniu rzuca wyjątek techniczny (np. baza nie działa). Dlaczego?
    //   Provider opakowuje go w {@code InternalAuthenticationServiceException} — klient dostaje 401, jakby hasło było złe,
    //   a prawdziwa przyczyna jest tylko w logu. Przy dziwnych 401 zajrzyj więc do logów serwera.

    // =================================================================================================
    // 5. PUŁAPKI: hash w JSON, wyliczanie kont, hasła w logach (Sec03PasswordLeakTest, Sec03AuthenticationProviderTest,
    //    Sec03AccountsTest.noUserEnumeration)
    // =================================================================================================
    //
    // PUŁAPKA: zwracanie encji {@code AppUser} z kontrolera. Dlaczego? Jackson zamienia na JSON wszystkie gettery — także
    //   {@code getPasswordHash()} (test entityLeaksHash: w JSON jest {@code "passwordHash":"{bcrypt}…"}). Hash to nie hasło,
    //   ale z hashem złodziej może zgadywać hasło u siebie, bez żadnych limitów. Rozwiązanie: DTO {@link UserView}
    //   z samymi bezpiecznymi polami (s09_jpa_relations/Rel06EntityToDto).
    //
    // PUŁAPKA: różne odpowiedzi dla "nie ma takiego użytkownika" i "złe hasło" (wyliczanie kont — user enumeration). Dlaczego?
    //   Atakujący sprawdza listę e-maili i dowiaduje się, które konta istnieją, a potem zgaduje hasła tylko dla nich (albo
    //   wysyła im fałszywe maile). Spring Security domyślnie ukrywa różnicę: {@code hideUserNotFoundExceptions = true}, więc
    //   nieznany login daje ten sam {@code BadCredentialsException("Bad credentials")} co złe hasło (test
    //   sameExceptionForUnknownUserAndWrongPassword); po wyłączeniu ukrywania leci {@code UsernameNotFoundException} (test
    //   revealingUserNotFound). Przez HTTP złe hasło, nieznany login i konto wyłączone dają IDENTYCZNE 401 z pustym ciałem
    //   i tym samym nagłówkiem (test noUserEnumeration). Dodatkowo provider dla nieznanego loginu i tak liczy BCrypt na
    //   sztucznym haśle ({@code mitigateAgainstTimingAttack}), żeby czas odpowiedzi nie zdradzał, czy konto istnieje.
    //   Uczciwie: nasza rejestracja (409 "nazwa zajęta") też zdradza, że nazwa istnieje — to typowy kompromis wygody.
    //   W poważnych systemach ogranicza się wtedy liczbę prób (rate limiting) albo potwierdza konto mailem.
    //
    // PUŁAPKA: hasło w logach. Dlaczego? Logi czyta wiele osób i narzędzi, trzymane są latami. Rekord Javy w {@code toString()}
    //   wypisuje WSZYSTKIE pola — {@code log.info("Rejestracja: {}", request)} zapisałby hasło (test recordToStringShowsEverything).
    //   Dlatego {@link RegistrationRequest} i {@link PasswordChangeRequest} nadpisują {@code toString()}, a {@code User} ze Spring
    //   Security sam pokazuje {@code Password=[PROTECTED]} (test toStringHidesPasswords). Nie loguj też całych żądań HTTP z
    //   nagłówkiem {@code Authorization} — w Basic to login i hasło w Base64, czyli praktycznie jawnie.
    //
    // DOBRA PRAKTYKA: Basic tylko przez HTTPS. Dlaczego? Hasło leci w KAŻDYM żądaniu, a Base64 to nie szyfrowanie. Lepsze
    //   podejście dla API (jedno logowanie, potem token): Sec05Jwt.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • UserDetailsService.loadUserByUsername(nazwa) → UserDetails (nazwa, HASH, role, enabled…); brak → UsernameNotFoundException.
     *   • InMemoryUserDetailsManager — nauka/testy; własny serwis na JPA — prawdziwe konta. Jeden bean UserDetailsService.
     *   • @Bean PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder() → {bcrypt}$2a$10$… (60 znaków + prefiks).
     *   • BCrypt: sól w hashu (ten sam tekst → różne hashe), koszt 10 (2^10 rund), limit 72 BAJTÓW (encode rzuca, matches ucina).
     *   • Nie: jawnie, {noop}, MD5, SHA-1, SHA-256 (szybkie, bez soli). Argon2 wymaga Bouncy Castle.
     *   • Hash bez {id} w DelegatingPasswordEncoder → IllegalArgumentException.
     *   • Rejestracja: permitAll, @Valid DTO, encode w serwisie, rola z serwera, UNIQUE w bazie + 409, odpowiedź = DTO.
     *   • disabled → DisabledException → 401 (sprawdzane przed hasłem); zmiana hasła wymaga starego.
     *   • UserDetailsPasswordService + upgradeEncoding → stary hash przepisany przy logowaniu (updatePassword dostaje hash).
     *   • hideUserNotFoundExceptions = true (domyślnie): nieznany login = złe hasło = 401 "Bad credentials".
     *   • Nigdy encja z hashem w JSON; nigdy hasło w logu (toString rekordów!).
     *
     * PYTANIA KONTROLNE:
     *   1. PYTANIE REKRUTACYJNE: dlaczego hasła haszuje się BCryptem, a nie SHA-256? Podaj dwa powody.
     *   2. Co się stanie? Wywołujesz dwa razy {@code new BCryptPasswordEncoder().encode("haslo-testowe")} i porównujesz wyniki
     *      {@code equals}. Co dostaniesz i jak w takim razie działa {@code matches}?
     *   3. ZNAJDŹ BŁĄD: po migracji danych z dawnego systemu w kolumnie {@code password_hash} są hashe {@code $2a$10$…},
     *      a aplikacja ma {@code PasswordEncoderFactories.createDelegatingPasswordEncoder()}. Nikt nie może się zalogować. Dlaczego?
     *   4. Co się stanie? Użytkownik ma hasło złożone z 80 liter "a". Rejestracja z {@code BCryptPasswordEncoder.encode}? A gdyby
     *      hash już istniał — czy {@code matches} dla hasła "72 × a + xyz" zwróci true?
     *   5. Co się stanie? Konto "jan" ma {@code enabled = false}. Loguje się poprawnym, a potem złym hasłem. Jakie wyjątki rzuci
     *      {@code DaoAuthenticationProvider} i jaki status zobaczy klient?
     *   6. Jak działa "podnoszenie" hasła {@code {noop}} na {@code {bcrypt}}? Kiedy się dzieje i jakie dwa warunki muszą być
     *      spełnione? Co dostaje metoda {@code updatePassword}?
     *   7. ZNAJDŹ BŁĄD: {@code @GetMapping("/me") AppUser me(...) { return repository.findByUsername(name).orElseThrow(); }}.
     *      Co zobaczy klient w JSON i jak to naprawić?
     *   8. Co to jest wyliczanie kont (user enumeration) i jak Spring Security się przed nim broni przy logowaniu? Czy nasza
     *      rejestracja jest całkiem odporna?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: PasswordPolicy, ExerciseEncoders, ExerciseUserDetailsService; testy:
    // Sec03UsersPasswordsExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Rozwiązania wzorcowe: pakiet s12_security.solutions.sec03_users_passwords. Wszystkie ćwiczenia to zwykłe klasy —
    // testy działają bez Springa i bez bazy.
    //
    // ĆWICZENIE 1 (łatwe): PasswordPolicy.problems — "ZA_KROTKIE" (mniej niż 12 znaków) i "ZAWIERA_LOGIN" (hasło zawiera nazwę
    //   użytkownika bez względu na wielkość liter), w tej kolejności. Podpowiedź: {@code toLowerCase(Locale.ROOT)}.
    //
    // ĆWICZENIE 2 (średnie): w tej samej metodzie dodaj "ZA_DLUGIE" — więcej niż 72 BAJTY w UTF-8 (limit BCrypt, sekcja 2).
    //   Podpowiedź: {@code password.getBytes(StandardCharsets.UTF_8).length}; "ą" to 2 bajty.
    //
    // ĆWICZENIE 3 (średnie): ExerciseEncoders.create — {@code new DelegatingPasswordEncoder("bcrypt", mapa)}: "bcrypt" →
    //   BCrypt o koszcie 12, "noop" → {@code NoOpPasswordEncoder.getInstance()} (tylko do odczytu starych haseł).
    //   Podpowiedź: pierwszy argument konstruktora to identyfikator algorytmu dla NOWYCH haseł.
    //
    // ĆWICZENIE 4 (trudniejsze): ExerciseUserDetailsService.loadUserByUsername — zamień AppUser z mapy na UserDetails (role
    //   z napisu "CUSTOMER,EMPLOYEE", disabled), brak konta → UsernameNotFoundException. Drugi test wkłada Twój serwis do
    //   prawdziwego DaoAuthenticationProvider. Podpowiedź: wzór w JpaUserDetailsService; dopóki jest TODO, provider opakuje
    //   wyjątek w InternalAuthenticationServiceException z komunikatem TODO (sekcja 4, ostatnia pułapka).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. (a) SHA-256 jest szybki — miliardy prób na sekundę na karcie graficznej; BCrypt jest celowo wolny (koszt 2^10 rund,
     *      regulowany). (b) Zwykły SHA-256 nie ma soli — to samo hasło daje ten sam skrót (test sha256IsDeterministic), więc
     *      działają tęczowe tablice i widać, kto ma to samo hasło; BCrypt losuje sól przy każdym hashu.
     *   2. false — dwa różne hashe, bo za każdym razem jest nowa losowa sól (test bcryptSaltsEveryHash). matches wyciąga sól
     *      i koszt z zapisanego hasha, liczy hash podanego hasła z TĄ solą i porównuje wyniki.
     *   3. DelegatingPasswordEncoder wymaga prefiksu {id}; hash bez niego daje IllegalArgumentException "each password must
     *      have a password encoding prefix" (test hashWithoutPrefix). Naprawa: dopisać {bcrypt} do hashy w bazie albo ustawić
     *      setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder()).
     *   4. encode rzuci IllegalArgumentException "password cannot be more than 72 bytes". matches nie rzuca, tylko bierze pierwsze
     *      72 bajty — więc "72 × a + xyz" pasuje do hasha z "72 × a" i zwróci true (test bcrypt72Bytes). Dlatego limit
     *      długości w DTO (@Size(max = 64)).
     *   5. W obu przypadkach DisabledException — stan konta jest sprawdzany przed hasłem (test disabledAccount
     *      w Sec03AuthenticationProviderTest). Klient zawsze dostaje 401 (test disabledAccount w Sec03AccountsTest).
     *   6. Przy udanym logowaniu DaoAuthenticationProvider pyta passwordEncoder.upgradeEncoding(hash). Warunki: upgradeEncoding
     *      zwraca true (np. {noop} przy domyślnym bcrypt albo niższy koszt BCrypt) i w kontekście jest bean
     *      UserDetailsPasswordService. Wtedy haszuje podane hasło od nowa i woła updatePassword(user, NOWY HASH) — metoda
     *      dostaje gotowy hash, nie hasło (test passwordUpgradeOnLogin).
     *   7. Klient zobaczy pole passwordHash z hashem (i id z bazy) — test entityLeaksHash. Naprawa: zwracać DTO (UserView)
     *      z samymi bezpiecznymi polami.
     *   8. To sprawdzanie, które loginy istnieją, po różnych odpowiedziach serwera. Spring Security domyślnie
     *      (hideUserNotFoundExceptions = true) zamienia "nie ma użytkownika" na ten sam BadCredentialsException co złe hasło,
     *      a dla nieznanego loginu i tak liczy BCrypt (taki sam czas odpowiedzi); przez HTTP wszystko to jedno 401 (test
     *      noUserEnumeration). Rejestracja nie jest odporna: 409 "nazwa zajęta" zdradza istnienie konta — kompromis wygody,
     *      który ogranicza się limitami prób albo potwierdzaniem konta mailem.
     */
    // </editor-fold>
}

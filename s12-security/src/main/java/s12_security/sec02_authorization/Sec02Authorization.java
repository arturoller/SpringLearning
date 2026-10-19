package s12_security.sec02_authorization;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;

/**
 * <pre>
 * TEMAT: Autoryzacja — role i uprawnienia, kolejność reguł, metody HTTP, @PreAuthorize i domyślne „zabronione”
 *        (authorization = autoryzacja, czyli „co ci wolno”; role = rola; authority = uprawnienie)
 *
 * W SKRÓCIE:
 *   • Każdy zalogowany ma listę uprawnień (GrantedAuthority — przyznane uprawnienie). Rola to uprawnienie
 *     z prefiksem ROLE_: roles("ADMIN") zapisuje „ROLE_ADMIN”, a hasRole("ADMIN") szuka „ROLE_ADMIN”.
 *   • Reguły w authorizeHttpRequests czyta się od góry; PIERWSZA pasująca wygrywa. Od szczegółu do ogółu,
 *     anyRequest() zawsze na końcu.
 *   • Reguła może dotyczyć tylko jednej metody HTTP: requestMatchers(HttpMethod.POST, "/api/products").
 *   • Gdy decyzja zależy od DANYCH (czyje to zamówienie?), zabezpiecz metodę serwisu: @EnableMethodSecurity +
 *     @PreAuthorize("#owner == authentication.name or hasRole('ADMIN')").
 *   • Bezpieczny domyślny wybór: anyRequest().denyAll() — czego nie wymieniono, tego nie wolno nikomu.
 *
 * ANALOGIA: hotel i karty-klucze.
 *   Karta gościa (rola USER) otwiera pokój i siłownię, karta pokojówki (rola HR) — wszystkie pokoje na piętrze,
 *   karta kierownika (ADMIN) — prawie wszystko. Recepcja programuje zamki: „drzwi 1–20: pokojówka; drzwi 7: gość
 *   z pokoju 7”. Zamek sprawdza listę od góry — jeśli pierwsza linijka mówi „każda karta otwiera wszystko”, dalsze
 *   linijki są martwe. A sejf w pokoju (metoda serwisu) ma WŁASNY zamek: otworzy go tylko właściciel pokoju.
 *
 * JAK TO DZIAŁA:
 *   zapytanie → AuthorizationFilter → przegląda reguły od góry:
 *       /error               → permitAll
 *       GET /api/products/** → permitAll
 *       POST /api/products   → hasRole("ADMIN")      = czy uprawnienia zawierają „ROLE_ADMIN”?
 *       ...
 *       anyRequest()         → denyAll
 *   → pierwsza pasująca decyduje: wpuść / odmów (anonim → 401, zalogowany → 403)
 *   → kontroler → OrderService (proxy!) → @PreAuthorize: wyrażenie SpEL → true: wywołaj metodę, false: wyjątek
 *     AuthorizationDeniedException → ExceptionTranslationFilter → 403
 *
 * SŁÓWKA:
 *   granted authority = przyznane uprawnienie; role = rola (uprawnienie z prefiksem ROLE_); has role = ma rolę;
 *   has any role = ma którąkolwiek z ról; has authority = ma uprawnienie; deny all = zabroń wszystkim;
 *   method security = zabezpieczenia metod; pre/post authorize = autoryzuj przed/po (wywołaniu metody);
 *   SpEL = Spring Expression Language (język wyrażeń Springa); return object = zwrócony obiekt;
 *   self-invocation = wywołanie samego siebie (metoda klasy woła inną metodę tej samej klasy);
 *   access denied = odmowa dostępu; fail closed = „w razie wątpliwości zamknięte” (bezpieczny domyślny stan).
 *
 * ZOBACZ TEŻ: s12_security/Sec01Basics (łańcuch filtrów, 401 a 403), s12_security/Sec03UsersPasswords (skąd się biorą
 *             użytkownicy i ich uprawnienia), s10_transactions/Tx03ProxyPitfalls (ta sama pułapka proxy przy
 *             {@code @Transactional}), s18_aop (jak działają proxy), t19_annotations_reflection/Annotations06DynamicProxy
 *             (proxy w czystej Javie), s04_web_rest/Rest01Controllers (metody HTTP w kontrolerach).
 * </pre>
 */
public final class Sec02Authorization {

    private Sec02Authorization() {
    }

    // =================================================================================================
    // 1. ROLA A UPRAWNIENIE — prefiks ROLE_
    // =================================================================================================
    //
    // Spring Security nie ma osobnego pojęcia „rola”. Zalogowany użytkownik ma listę napisów-uprawnień
    // (GrantedAuthority). Rola to umowa: uprawnienie, które zaczyna się od ROLE_.
    //   User...roles("ADMIN")                → uprawnienie „ROLE_ADMIN”   (prefiks dokleja się sam)
    //   User...authorities("report:read")    → uprawnienie „report:read”  (dokładnie ten napis)
    //   hasRole("ADMIN")                     → sprawdza „ROLE_ADMIN”      (prefiks dokleja się sam)
    //   hasAuthority("report:read")          → sprawdza „report:read”     (dokładnie ten napis)
    //   hasAnyRole("HR", "ADMIN")            → „ROLE_HR” albo „ROLE_ADMIN”
    // Role są „grube” (kim jesteś w firmie), uprawnienia — „drobne” (co konkretnie możesz zrobić). Tokeny JWT
    // zwykle niosą drobne uprawnienia (scope — zakres, Sec05Jwt).
    //
    // Pracownicy lekcji (Sec02Users): anna — ADMIN, piotr — USER, kasia — HR, tomasz — SALES + report:read.
    //   kasia, anna → /api/hr/employees = 200; piotr → 403 (hasAnyRole("HR", "ADMIN")).
    //   tomasz → /api/reports/sales = 200; anna (ADMIN!) → 403. Role nie tworzą hierarchii: ADMIN nie dostaje
    //   automatycznie cudzych uprawnień. Hierarchię da się włączyć beanem RoleHierarchy — tu tego nie robimy.
    //
    // PUŁAPKA: hasRole("ROLE_HR") — aplikacja NIE WYSTARTUJE: IllegalArgumentException „ROLE_HR should not start with
    //   ROLE_ since ROLE_ is automatically prepended when using hasAnyRole. Consider using hasAnyAuthority instead.”
    //   (tak, komunikat mówi „hasAnyRole”, choć wywołałeś hasRole — skopiowane z uruchomienia). Podobnie
    //   roles("ROLE_ADMIN") → „ROLE_ADMIN cannot start with ROLE_ (it is automatically added)”. Dlaczego to dobrze?
    //   Lepiej, żeby literówka wywróciła start, niż żeby po cichu nikt nie miał dostępu.
    //
    // PUŁAPKA: hasAuthority("HR") zamiast hasRole("HR") — kasia (rola HR = uprawnienie „ROLE_HR”) dostaje 403, bo
    //   „HR” to inny napis niż „ROLE_HR”. Tu nic nie wybucha przy starcie — dlatego takie reguły trzeba testować
    //   (Sec02AuthorizationRuleOrderTest).
    //
    // PUŁAPKA: w budowniczym User metody roles(...) i authorities(...) NADPISUJĄ się — zostaje to, co wywołano
    //   ostatnie. User.withUsername("tomasz").roles("SALES").authorities("report:read") ma TYLKO „report:read”.
    //   Chcesz obu? Jedno wywołanie: authorities("ROLE_SALES", "report:read").

    // =================================================================================================
    // 2. KOLEJNOŚĆ REGUŁ — pierwsza pasująca wygrywa
    // =================================================================================================
    //
    // AuthorizationFilter przegląda reguły z góry na dół i ZATRZYMUJE SIĘ na pierwszej pasującej. Dalsze reguły
    // dla tego zapytania nie istnieją. Stąd zasada: od SZCZEGÓŁU do OGÓŁU.
    //
    // PRZED (źle):                                          PO (dobrze):
    //   .requestMatchers("/api/**").authenticated()           .requestMatchers("/api/hr/**").hasAnyRole("HR", "ADMIN")
    //   .requestMatchers("/api/hr/**").hasAnyRole(...)        .requestMatchers("/api/**").authenticated()
    //
    // PUŁAPKA: w wersji PRZED piotr (USER) czyta pensje z /api/hr/employees — 200 zamiast 403. Pierwsza reguła
    //   pasuje (to też /api/**) i mówi „wystarczy być zalogowanym”. Aplikacja startuje bez żadnego ostrzeżenia
    //   (Sec02AuthorizationRuleOrderTest.generalRuleFirstOpensTheDoor).
    //
    // PUŁAPKA: anyRequest() przed innymi regułami — tu Security pomaga: aplikacja nie startuje, wyjątek
    //   IllegalStateException „Can't configure requestMatchers after anyRequest”. Dlaczego blokada? Reguły po
    //   anyRequest() byłyby martwe, bo anyRequest() pasuje do wszystkiego.

    // =================================================================================================
    // 3. METODY HTTP W REGUŁACH
    // =================================================================================================
    //
    //   .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()          // czytać — każdy
    //   .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")       // dodawać — ADMIN
    //   .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")  // usuwać — ADMIN
    // Bez HttpMethod reguła dotyczy WSZYSTKICH metod. Efekt (Sec02AuthorizationTest):
    //   anonim GET → 200; piotr POST → 403; anna POST → 201; anna DELETE → 204; anonim DELETE → 401;
    //   anna PUT → 403 — dla PUT nie ma reguły, więc łapie go anyRequest().denyAll() (sekcja 5).
    //
    // Uwaga o CSRF: Sec02SecurityConfig wyłącza ochronę CSRF, bo to API dla programów wysyłających hasło w każdym
    // zapytaniu. Z włączonym CSRF każdy POST/PUT/DELETE bez tokenu dostałby 403 — nawet od admina — i łatwo
    // pomylić to z odmową z reguły ról. Kiedy wolno wyłączyć CSRF: Sec04StatelessApi.

    // =================================================================================================
    // 4. ZABEZPIECZENIA METOD — @EnableMethodSecurity, @PreAuthorize, @PostAuthorize
    // =================================================================================================
    //
    // Reguła „/api/orders/** — zalogowani” nie odróżni zamówień piotra od zamówień anny. To wie dopiero kod, który
    // zna parametr (właściciela) albo wynik (zamówienie). Dlatego OrderService:
    //   @PreAuthorize("#owner == authentication.name or hasRole('ADMIN')")
    //   public List<Order> ordersOf(String owner)
    //       #owner              — parametr metody o nazwie owner
    //       authentication.name — login zalogowanego (obiekt Authentication z kontekstu bezpieczeństwa)
    //       hasRole('ADMIN')    — te same funkcje co w łańcuchu filtrów
    //   @PostAuthorize("returnObject.owner() == authentication.name or hasRole('ADMIN')")
    //   public Order findById(String id)  — metoda SIĘ WYKONA, a sprawdzany jest wynik (returnObject)
    // Efekt: piotr → /api/orders/piotr 200, /api/orders/anna 403; anna → /api/orders/piotr 200;
    //        piotr → /api/orders/by-id/ZAM-002 200, ZAM-001 (anny) 403.
    //
    // Jak to działa: @EnableMethodSecurity rejestruje mechanizm, który podmienia bean OrderService na proxy
    // (pośrednika). Kontroler woła proxy, proxy liczy wyrażenie, a przy „false” rzuca AuthorizationDeniedException
    // („Access Denied”); ExceptionTranslationFilter zamienia go na 403 (anonim dostałby 401).
    // @EnableMethodSecurity domyślnie włącza @PreAuthorize/@PostAuthorize; @Secured i @RolesAllowed są wyłączone
    // (securedEnabled = false, jsr250Enabled = false).
    //
    // PUŁAPKA: bez @EnableMethodSecurity adnotacja @PreAuthorize NIC nie robi — bez błędu i bez ostrzeżenia
    //   (Sec02AuthorizationMethodSecurityTest). Stary kod używa @EnableGlobalMethodSecurity(prePostEnabled = true) —
    //   w Security 7.1 ta adnotacja jeszcze istnieje, ale jest przestarzała (deprecated); używaj @EnableMethodSecurity.
    //
    // PUŁAPKA: wywołanie metody z WNĘTRZA tej samej klasy omija proxy. OrderService.summaryOf (bez adnotacji) woła
    //   this.ordersOf(owner) — i @PreAuthorize nie jest sprawdzane: piotr dostaje „anna: 1 zamówień na 5499.99 zł”.
    //   Dlaczego? Proxy stoi PRZED obiektem; „this” to sam obiekt, nie proxy. Ta sama pułapka dotyczy
    //   @Transactional (s10_transactions/Tx03ProxyPitfalls). Rozwiązanie: adnotacja na metodzie wołanej z zewnątrz
    //   albo wydzielenie metody do osobnego beana.
    //
    // PUŁAPKA: #owner działa tylko wtedy, gdy kompilator zapisał nazwy parametrów (opcja -parameters). Boot
    //   (spring-boot-starter-parent) ją włącza, ale w projekcie bez tego rodzica #owner byłoby null i reguła
    //   odmawiałaby wszystkim poza adminem. Zabezpieczenie: @P("owner") przy parametrze
    //   (org.springframework.security.core.parameters.P) — nazwa podana jawnie.
    //
    // DOBRA PRAKTYKA: łańcuch filtrów = gruba krata („do /api/orders tylko zalogowani”), @PreAuthorize = drobne sito
    //   („tylko swoje zamówienia”). Dlaczego oba? Łańcuch zatrzymuje obcych najtaniej (zanim powstanie kontroler),
    //   a reguły zależne od danych i tak muszą być tam, gdzie są dane.

    // =================================================================================================
    // 5. DOMYŚLNIE ZABRONIONE — anyRequest().denyAll() zamiast anyRequest().authenticated()
    // =================================================================================================
    //
    // Programista dodał StaffController.stats() (/api/stats) i zapomniał o regule. Co się stanie?
    //   z anyRequest().authenticated() → każdy zalogowany (także piotr) czyta statystyki — dziura;
    //   z anyRequest().denyAll()       → anna 403, anonim 401 — nikt, dopóki ktoś świadomie nie dopisze reguły.
    //
    // DOBRA PRAKTYKA: kończ reguły na anyRequest().denyAll() i wymieniaj każdy dozwolony adres (fail closed).
    //   Dlaczego? Zapomniana reguła daje wtedy 403 widoczne od razu w testach, a nie cichą dziurę na produkcji.
    //   Koszt: każdy nowy punkt końcowy trzeba dopisać — to celowa chwila namysłu „kto może to wywołać?”.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • roles("X") = uprawnienie „ROLE_X”; hasRole("X") sprawdza „ROLE_X”; hasAuthority("x") — dokładny napis.
     *   • hasRole("ROLE_X") i roles("ROLE_X") → wyjątek przy starcie; hasAuthority("X") dla roli → cichy 403.
     *   • roles(...) i authorities(...) nadpisują się — oba uprawnienia w jednym authorities("ROLE_X", "y").
     *   • Reguły od szczegółu do ogółu; pierwsza pasująca wygrywa; anyRequest() na końcu (inaczej wyjątek przy starcie).
     *   • requestMatchers(HttpMethod.POST, "/api/products") — reguła dla jednej metody HTTP.
     *   • @EnableMethodSecurity + @PreAuthorize("#owner == authentication.name or hasRole('ADMIN')");
     *     @PostAuthorize("returnObject...") — sprawdza wynik. Odmowa: AuthorizationDeniedException → 403.
     *   • Bez @EnableMethodSecurity — adnotacje martwe. Wywołanie this.metoda() — omija proxy.
     *   • Na końcu anyRequest().denyAll() — bezpieczny domyślny stan.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie uprawnienie ma użytkownik zbudowany przez roles("ADMIN") i co dokładnie sprawdza hasRole("ADMIN")?
     *   2. Co się stanie? W regułach napiszesz .requestMatchers("/api/hr/**").hasRole("ROLE_HR").
     *   3. ZNAJDŹ BŁĄD: kasia ma roles("HR"), reguła to .requestMatchers("/api/hr/**").hasAuthority("HR"). Kasia
     *      dostaje 403. Dlaczego?
     *   4. Co się stanie? Reguły: .requestMatchers("/api/**").authenticated(), a POD nią
     *      .requestMatchers("/api/hr/**").hasAnyRole("HR", "ADMIN"). Piotr (USER) wysyła GET /api/hr/employees.
     *   5. Dlaczego zamówienia „tylko swoje” sprawdzamy w @PreAuthorize, a nie w łańcuchu filtrów?
     *   6. ZNAJDŹ BŁĄD: w serwisie jest @PreAuthorize("hasRole('ADMIN')") na metodzie cancel(), a metoda close()
     *      (bez adnotacji) woła this.cancel(). Piotr wywołuje close() i... anuluje zamówienie. Dlaczego?
     *   7. Co się stanie? Usuniesz @EnableMethodSecurity z Sec02SecurityConfig, a piotr poprosi o /api/orders/anna.
     *   8. Czym różni się anyRequest().denyAll() od anyRequest().authenticated(), gdy ktoś doda nowy kontroler?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Sec02AuthorizationExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zamień nazwę roli na uprawnienie tak jak Spring: "ADMIN" → "ROLE_ADMIN".
     * Nazwa pusta, null albo już zaczynająca się od "ROLE_" → IllegalArgumentException.
     * Podpowiedź: isBlank(), startsWith().
     */
    static String exercise1(String role) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj użytkownika tomasz (hasło "{noop}tomasz-test-123") z rolą SALES ORAZ uprawnieniem
     * report:read — tak, żeby miał OBA (test sprawdza dokładnie: "ROLE_SALES", "report:read").
     * Podpowiedź: sekcja 1, ostatnia PUŁAPKA. User.withUsername(...)...build().
     */
    static UserDetails exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zbuduj łańcuch dla kontrolerów ProductController i StaffController:
     * /error — wszyscy; GET /api/products/** — wszyscy; POST /api/products i DELETE /api/products/** — tylko ADMIN;
     * /api/hr/** — HR albo ADMIN; wszystko inne — NIKT; HTTP Basic; CSRF wyłączone (jak w Sec02SecurityConfig).
     * Test loguje się jako pracownicy z Sec02Users.
     * Podpowiedź: sekcje 2, 3 i 5 — pamiętaj o kolejności.
     */
    static SecurityFilterChain exercise3(HttpSecurity http) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // ĆWICZENIE 4 (trudniejsze): klasa ExerciseSalaryService w tym pakiecie — dopisz @PreAuthorize i treść metody
    // salaryOf (opis w tej klasie).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Uprawnienie „ROLE_ADMIN” (prefiks dokleja roles). hasRole("ADMIN") też dokleja prefiks i sprawdza, czy
     *      wśród uprawnień jest „ROLE_ADMIN”.
     *   2. Aplikacja nie wystartuje: IllegalArgumentException „ROLE_HR should not start with ROLE_ since ROLE_ is
     *      automatically prepended when using hasAnyRole. Consider using hasAnyAuthority instead.” Sprawdzone testem
     *      Sec02AuthorizationRuleOrderTest.rolePrefixFailsAtStartup.
     *   3. roles("HR") zapisuje uprawnienie „ROLE_HR”, a hasAuthority("HR") szuka dokładnie napisu „HR”. Poprawka:
     *      hasRole("HR") albo hasAuthority("ROLE_HR"). Sprawdzone testem
     *      Sec02AuthorizationRuleOrderTest.authorityInsteadOfRole (403 dla kasi).
     *   4. 200 — piotr widzi pensje. Pierwsza pasująca reguła to /api/** (authenticated), a piotr jest zalogowany;
     *      reguła dla /api/hr/** nigdy nie jest sprawdzana. Aplikacja startuje bez ostrzeżeń. Sprawdzone testem
     *      Sec02AuthorizationRuleOrderTest.generalRuleFirstOpensTheDoor.
     *   5. Adres /api/orders/{owner} da się dopasować wzorcem, ale porównanie „owner == zalogowany” i „właściciel
     *      zwróconego zamówienia” to logika na danych — najpewniej sprawdzić ją tam, gdzie dane są znane, i tak, żeby
     *      działała niezależnie od tego, który kontroler woła serwis. @PostAuthorize widzi nawet wynik metody.
     *   6. Wywołanie this.cancel() wewnątrz klasy omija proxy, więc @PreAuthorize nie jest sprawdzane. Adnotacja musi być
     *      na metodzie wołanej z zewnątrz (close) albo cancel musi trafić do innego beana. Ten sam mechanizm sprawdza
     *      test Sec02AuthorizationTest.selfInvocationBypassesSecurity (summaryOf → ordersOf).
     *   7. Piotr dostanie 200 i zamówienia anny — @PreAuthorize bez @EnableMethodSecurity jest martwą adnotacją
     *      (reguła w łańcuchu wymaga tylko zalogowania). Sprawdzone uruchomieniem Sec02Application z zakomentowaną
     *      adnotacją (200, [{"id":"ZAM-001","owner":"anna","total":5499.99}]) i testem
     *      Sec02AuthorizationMethodSecurityTest.withoutEnableMethodSecurity (brak proxy, metoda zwraca zamówienia anny).
     *   8. authenticated() wpuści każdego zalogowanego na nowy, zapomniany adres (cicha dziura). denyAll() da 403
     *      (anonimowi 401), dopóki ktoś nie dopisze świadomej reguły. Sprawdzone testem
     *      Sec02AuthorizationTest.denyAllByDefault.
     */
    // </editor-fold>
}

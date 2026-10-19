package s02_configuration.cfg03_profiles;

import org.springframework.core.env.Environment;

/**
 * <pre>
 * TEMAT: Profile — inna konfiguracja i inne beany na komputerze programisty, w testach i na produkcji
 *        (profile = profil, nazwany zestaw ustawień i beanów; active profile = aktywny profil;
 *         environment = środowisko; production = produkcja; development = rozwój)
 *
 * W SKRÓCIE:
 *   Ta sama aplikacja działa w kilku miejscach i w każdym potrzebuje czegoś innego: u programisty bramka płatności
 *   „na niby”, na produkcji prawdziwa; inne adresy, inne limity. Profil to etykieta (np. "cfg03-prod"), którą
 *   włączasz przy uruchomieniu. Etykieta robi dwie rzeczy: (1) włącza beany oznaczone @Profile("etykieta"),
 *   (2) wczytuje plik application-etykieta.properties, który nadpisuje ustawienia z pliku bazowego.
 *
 * ANALOGIA: strój do okazji.
 *   Ta sama osoba (aplikacja) idzie do pracy, na siłownię albo na wesele. Profil to dress code: „formalny” wybiera
 *   garnitur (beany i ustawienia produkcyjne), „sportowy” — dres (atrapy i lokalne adresy). Nie szyjesz nowej osoby
 *   na każdą okazję, tylko zmieniasz ubranie.
 *
 * JAK TO DZIAŁA:
 *   --spring.profiles.active=cfg03,cfg03-prod      (albo @ActiveProfiles w teście, albo .profiles(...) w kodzie)
 *     1. Environment zapamiętuje aktywne profile (kolejność ma znaczenie!).
 *     2. Boot wczytuje application.properties, potem application-cfg03.properties, potem application-cfg03-prod.properties
 *        — każdy kolejny plik nadpisuje poprzedni, więc WYGRYWA profil wymieniony PÓŹNIEJ.
 *     3. Podczas skanowania Spring sprawdza @Profile przy każdej klasie: bean powstaje tylko wtedy, gdy wyrażenie
 *        profilowe pasuje do aktywnych profili.
 *     4. Brak aktywnych profili → aktywny jest profil "default" (domyślny).
 *
 * SŁÓWKA:
 *   profile = profil; active = aktywny; default = domyślny; group = grupa; expression = wyrażenie; fake = atrapa
 *   (sztuczna implementacja); gateway = bramka; override = nadpisać; negation = negacja (!); audit = audyt.
 *
 * ZOBACZ TEŻ: s01_core_ioc/Ioc05MultipleCandidates (kilka implementacji tego samego interfejsu),
 *             s02_configuration/Cfg01Properties (priorytety źródeł ustawień),
 *             s02_configuration/Cfg04Yaml (profile w jednym pliku YAML),
 *             s02_configuration/Cfg05Environment (acceptsProfiles i @ConditionalOnProperty).
 * </pre>
 */
public final class Cfg03Profiles {

    private Cfg03Profiles() {
    }

    // =================================================================================================
    // 1. @Profile — który bean istnieje w którym środowisku
    // =================================================================================================
    //
    // FakePaymentGateway  @Profile("!cfg03-prod")  — wszędzie POZA produkcją (atrapa: nie łączy się z siecią)
    // RealPaymentGateway  @Profile("cfg03-prod")   — tylko na produkcji
    // Obie implementują PaymentGateway, ale w każdym środowisku istnieje dokładnie JEDNA, więc wstrzyknięcie
    // PaymentGateway zawsze jest jednoznaczne (inaczej byłby błąd NoUniqueBeanDefinitionException — lekcja Ioc05).
    // @Profile można dać na klasie z @Component albo na metodzie @Bean.
    //
    // DOBRA PRAKTYKA: zabezpieczenie „domyślnie atrapa”: @Profile("!prod") dla wersji nieprodukcyjnej.
    //   Dlaczego? Gdy ktoś zapomni włączyć jakikolwiek profil, dostanie bezpieczną atrapę, a nie prawdziwą kartę
    //   kredytową. Odwrotny układ (prawdziwa bramka jako domyślna) kończy się obciążeniem prawdziwych pieniędzy w teście.

    // =================================================================================================
    // 2. application-{profil}.properties — ustawienia profilu nadpisują bazę
    // =================================================================================================
    //
    //   application-cfg03.properties       url=http://localhost:8080/pay,  timeout=10s     (baza lekcji)
    //   application-cfg03-dev.properties   url=http://localhost:9090/fake-pay              (timeout zostaje 10s)
    //   application-cfg03-prod.properties  url=https://pay.example.com/api, timeout=3s
    // W pliku profilu wpisuj TYLKO to, co się różni od bazy. Reszta jest dziedziczona.
    //
    // PUŁAPKA: kolejność aktywnych profili. Przy --spring.profiles.active=cfg03-prod,cfg03 (baza na końcu) wygrywa
    //   adres z pliku cfg03, a nie z prod. Dlaczego? Pliki wczytywane są w kolejności wymienienia profili, a późniejszy
    //   nadpisuje wcześniejszy. Sprawdzone testem (sekcja 2). Wniosek: ogólne profile wymieniaj pierwsze,
    //   szczegółowe (prod) na końcu.

    // =================================================================================================
    // 3. JAK WŁĄCZYĆ PROFIL
    // =================================================================================================
    //
    //   a) argument wiersza poleceń:  --spring.profiles.active=cfg03,cfg03-prod
    //   b) właściwość JVM / zmienna środowiska:  -Dspring.profiles.active=...  /  SPRING_PROFILES_ACTIVE=...
    //   c) w pliku application.properties:  spring.profiles.active=...  (wygodne dla domyślnego profilu rozwojowego)
    //   d) w kodzie startowym: new SpringApplicationBuilder(...).profiles("cfg03", "cfg03-dev")  (jak w Cfg03Application)
    //   e) w teście: @ActiveProfiles({"cfg03", "cfg03-dev"})
    // @ActiveProfiles działa tylko w testach opartych na Spring Test (np. @SpringBootTest).
    // Test sekcji 3 włącza profil produkcyjny sposobami a), b) i d) i za każdym razem dostaje prawdziwą bramkę;
    // sposób e) pokazuje test sekcji 7.
    //
    // DOBRA PRAKTYKA: profil produkcyjny podawaj z ZEWNĄTRZ (zmienna środowiska SPRING_PROFILES_ACTIVE w kontenerze),
    //   a nie wpisuj na stałe do pliku w repozytorium. Dlaczego? Ten sam plik JAR ma działać wszędzie, a pomyłka
    //   „wdrożyłem z profilem dev na produkcji” jest zbyt kosztowna.

    // =================================================================================================
    // 4. PROFIL „default” i brak profilu
    // =================================================================================================
    //
    // Gdy nie włączysz żadnego profilu, aktywny jest profil o nazwie "default" (environment.getDefaultProfiles()).
    // @Profile("default") oznacza więc „bean tylko wtedy, gdy nie ma żadnych innych profili”. Istnieje też plik
    // application-default.properties — wczytywany w takiej sytuacji. W tym module go nie tworzymy: dotknąłby
    // wszystkich lekcji bez profilu. Test sekcji 4: DefaultProfileMarker istnieje bez profili i znika po włączeniu cfg03.
    //
    // PUŁAPKA: włączasz profil "dev" i dziwisz się, że przestał działać bean z @Profile("default").
    //   Dlaczego? "default" jest aktywny TYLKO gdy nie ma żadnego innego profilu. Dołożenie dowolnego profilu go wyłącza.
    //   Sprawdzone testem (sekcja 4).

    // =================================================================================================
    // 5. WYRAŻENIA PROFILOWE: ! & | oraz nawiasy
    // =================================================================================================
    //
    //   @Profile("!cfg03-prod")                 nie-produkcja
    //   @Profile("cfg03-prod | cfg03-audit")    produkcja LUB audyt (AuditLog)
    //   @Profile("cfg03-dev & cfg03-audit")     oba naraz
    // PUŁAPKA: mieszanie & i | bez nawiasów, np. "a & b | c", jest niedozwolone — Spring wymaga nawiasów:
    //   "(a & b) | c". Dlaczego? Wyrażenie bez nawiasów jest niejednoznaczne i Spring woli odmówić niż zgadywać.
    //   Kontekst nie wystartuje: IllegalArgumentException: Malformed profile expression [a & b | c]
    //   (malformed = źle zbudowane). Sprawdzone testem (sekcja 5).
    // Wiele nazw w @Profile({"a", "b"}) znaczy LUB.

    // =================================================================================================
    // 6. GRUPY PROFILI — jedna nazwa włącza kilka profili
    // =================================================================================================
    //
    //   spring.profiles.group.cfg03-full=cfg03-dev,cfg03-audit
    //   --spring.profiles.active=cfg03,cfg03-full      → aktywne: cfg03, cfg03-full, cfg03-dev, cfg03-audit
    // Grupa upraszcza uruchamianie: zamiast pięciu nazw podajesz jedną. Test sekcji 6 sprawdza, że grupa włączyła
    // profile (a więc i plik cfg03-dev oraz bean AuditLog).

    // =================================================================================================
    // 7. PROFILE W TESTACH
    // =================================================================================================
    //
    // @ActiveProfiles({"cfg03", "cfg03-dev"}) nad klasą testu; Spring Test uruchamia kontekst z tymi profilami.
    // Test sekcji 7 sprawdza, że kontekst klasy testowej ma dokładnie te profile (i plik cfg03-dev).
    // Różne zestawy profili = różne konteksty = wolniejsze testy (Spring trzyma gotowy kontekst w pamięci podręcznej
    // — cache — osobno dla każdego zestawu). Dlatego w tej lekcji scenariusze „prod”, „audit” i „grupa” uruchamiamy
    // małymi aplikacjami w try-with-resources.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Profile("nazwa") na klasie lub @Bean: bean tylko w tym profilu; "!nazwa" = poza nim; "a | b", "a & b" (z nawiasami).
     *   • Plik application-{profil}.properties nadpisuje application.properties; późniejszy profil wygrywa z wcześniejszym.
     *   • Włączanie: --spring.profiles.active=a,b | SPRING_PROFILES_ACTIVE | spring.profiles.active w pliku
     *     | SpringApplicationBuilder.profiles(...) | @ActiveProfiles w teście.
     *   • Bez aktywnych profili działa profil "default"; dołożenie jakiegokolwiek profilu go wyłącza.
     *   • Grupa: spring.profiles.group.nazwa=profil1,profil2 — jedna nazwa włącza kilka profili.
     *   • Atrapa jako domyślna, prawdziwa usługa tylko na produkcji. Profil produkcyjny podawaj z zewnątrz.
     *   • Environment.acceptsProfiles / matchesProfiles sprawdza profile w kodzie (lekcja Cfg05Environment).
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie dwie rzeczy robi włączenie profilu "cfg03-prod"?
     *   2. Co się stanie? Uruchamiasz aplikację z --spring.profiles.active=cfg03-prod,cfg03 — z którego pliku
     *      pochodzi adres bramki?
     *   3. Co się stanie? Masz bean z @Profile("default") i włączasz profil cfg03.
     *   4. ZNAJDŹ BŁĄD: @Profile("a & b | c") — dlaczego Spring odmówi takiego wyrażenia i jak je naprawić?
     *   5. ZNAJDŹ BŁĄD: prawdziwa bramka płatności ma @Profile("!prod"), a atrapa @Profile("prod") —
     *      co jest nie tak?
     *   6. Dlaczego dobrą praktyką jest atrapa jako bean „domyślny”, a prawdziwa usługa tylko w profilu produkcyjnym?
     *   7. Który profil jest aktywny, gdy nie włączysz żadnego, i co się z nim dzieje po włączeniu innego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Cfg03ProfilesExercisesTest
    // =================================================================================================

    /** Interfejs do ćwiczenia 1. */
    interface MailSender {
        String send(String to);
    }

    /**
     * ĆWICZENIE 1 (łatwe): wysyłka poczty. {@code ConsoleMailSender} ma działać wszędzie POZA profilem
     * {@code cfg03-prod}, a {@code SmtpMailSender} tylko w profilu {@code cfg03-prod}. Dopisz adnotacje nad klasami.
     * Podpowiedź: {@code @Profile("!...")} i {@code @Profile("...")}. Test rejestruje obie klasy naraz.
     */
    static class ConsoleMailSender implements MailSender {
        // TODO: twoje rozwiązanie (adnotacja nad klasą)
        @Override
        public String send(String to) {
            return "KONSOLA: " + to;
        }
    }

    /** Druga klasa do ćwiczenia 1 — patrz {@link ConsoleMailSender}. */
    static class SmtpMailSender implements MailSender {
        // TODO: twoje rozwiązanie (adnotacja nad klasą)
        @Override
        public String send(String to) {
            return "SMTP: " + to;
        }
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć nazwę trybu: "PRODUKCJA", jeśli aktywny jest profil {@code cfg03-prod};
     * w przeciwnym razie "TEST", jeśli aktywny jest {@code cfg03-test}; w pozostałych przypadkach "ROZWÓJ".
     * Podpowiedź: {@code environment.matchesProfiles(...)} (zwięzły odpowiednik acceptsProfiles(Profiles.of(...))).
     */
    static String exercise2(Environment environment) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć ustawienia (tekst {@code "klucz=wartość"}), które zdefiniują grupę
     * {@code cfg03-full} składającą się z profili {@code cfg03-dev} i {@code cfg03-audit} oraz włączą
     * profile {@code cfg03} i {@code cfg03-full}. Test uruchomi Cfg03Application z tymi ustawieniami
     * (jako argumenty z dwoma myślnikami) i sprawdzi aktywne profile oraz bean AuditLog.
     */
    static String[] exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. (1) Włącza beany z @Profile("cfg03-prod") i wyłącza te z "!cfg03-prod"; (2) wczytuje plik
     *      application-cfg03-prod.properties, który nadpisuje ustawienia z pliku bazowego.
     *   2. Z pliku bazowego cfg03 (http://localhost:8080/pay): profile wczytują się w kolejności wymienienia, a
     *      późniejszy nadpisuje wcześniejszy — baza na końcu wygrywa. Sprawdzone testem (sekcja 2).
     *   3. Bean z @Profile("default") nie powstanie: "default" jest aktywny tylko wtedy, gdy nie ma żadnego innego
     *      profilu. Sprawdzone testem (sekcja 4).
     *   4. Mieszanie & i | bez nawiasów jest niejednoznaczne, więc kontekst nie wystartuje
     *      (IllegalArgumentException: Malformed profile expression [a & b | c]). Napraw: @Profile("(a & b) | c") albo
     *      "a & (b | c)", zależnie od zamiaru. Sprawdzone testem (sekcja 5).
     *   5. Odwrócone: na produkcji (profil prod) powstanie atrapa, a prawdziwa bramka we wszystkich pozostałych
     *      środowiskach. Przy braku profilu prawdziwa bramka byłaby domyślna — ryzyko prawdziwych obciążeń w testach.
     *   6. Bo pomyłka (brak profilu, literówka w nazwie) kończy się wtedy bezpieczną atrapą, a nie prawdziwymi
     *      pieniędzmi czy prawdziwą wysyłką poczty.
     *   7. Profil "default"; po włączeniu jakiegokolwiek innego przestaje być aktywny. Sprawdzone testem (sekcja 4).
     */
    // </editor-fold>
}

package s15_actuator_logging.obs01_actuator;

import java.util.List;
import java.util.Set;

/**
 * <pre>
 * TEMAT: Actuator — punkty końcowe do podglądu i kontroli działającej aplikacji
 *        (actuator = siłownik/urządzenie wykonawcze; endpoint = punkt końcowy; expose = wystawić; health = zdrowie;
 *        metrics = metryki; loggers = rejestratory logów; env = środowisko; beans = ziarna; access = dostęp)
 *
 * W SKRÓCIE:
 *   Aplikacja, która działa na serwerze, jest „czarną skrzynką”. Actuator (starter spring-boot-starter-actuator) dokłada
 *   do niej gotowe adresy pod /actuator: czy żyje (health), co to za aplikacja (info), jakie ma liczniki (metrics),
 *   jakie poziomy logów (loggers), jakie ma ustawienia (env), jakie beany (beans), które autokonfiguracje zadziałały
 *   (conditions). Nie piszesz do tego żadnego kontrolera. Najważniejsza zasada: domyślnie przez HTTP widać TYLKO health —
 *   resztę wystawiasz świadomie, bo to bardzo wrażliwe dane.
 *
 * ANALOGIA: pulpit w kabinie samolotu.
 *   Samolot (aplikacja) leci sam, ale pilot ma zegary: wysokość, paliwo, alarmy. Actuator to takie zegary dla Twojej
 *   aplikacji. Pasażerowie (internet) nie mają prawa do kabiny pilotów — dlatego zegary są domyślnie schowane, a
 *   „kontrolka zielona/czerwona” (health) jest widoczna dla wszystkich, np. dla systemu, który pilnuje, czy samolot
 *   w ogóle jeszcze leci.
 *
 * JAK TO DZIAŁA:
 *   1. Starter actuator → autokonfiguracja rejestruje punkty końcowe (endpoint = adres + dane) pod /actuator.
 *   2. Każdy punkt końcowy ma DWA niezależne ustawienia:
 *        a) wystawienie (exposure) przez sieć: management.endpoints.web.exposure.include / exclude,
 *        b) dostęp (access): {@code management.endpoint.<id>.access} = none | read-only | unrestricted.
 *      Punkt jest osiągalny przez HTTP dopiero, gdy jest wystawiony ORAZ dostęp nie jest „none”.
 *   3. Strona /actuator (discovery = odkrywanie) wymienia to, co faktycznie jest dostępne.
 *   4. Tych samych punktów można używać przez JMX (osobne ustawienie: management.endpoints.jmx.exposure.*).
 *
 *   PRZED (Boot 2.x i wiele starych tutoriali):  management.endpoint.env.enabled=false,
 *                                                management.endpoints.enabled-by-default=false
 *   PO (od Boota 3.4, aktualne w 4.1.1):         management.endpoint.env.access=none,
 *                                                management.endpoints.access.default=none
 *   W metadanych Boota 4.1.1 stara właściwość management.endpoints.enabled-by-default jest oznaczona jako przestarzała
 *   (zastąpiona przez management.endpoints.access.default). Boot 4 ma też Actuator podzielony na kilka modułów
 *   (spring-boot-health, spring-boot-micrometer-metrics...) — pakiety klas zdrowia zmieniły się na
 *   org.springframework.boot.health.contributor (patrz Obs02HealthIndicators).
 *
 * SŁÓWKA:
 *   exposure = wystawienie (czy adres jest widoczny z sieci); include/exclude = uwzględnij/wyłącz;
 *   unrestricted = bez ograniczeń; read-only = tylko odczyt; discovery = odkrywanie (lista dostępnych adresów);
 *   management port = port zarządzania; mask = zamaskować (ukryć wartość gwiazdkami); sanitize = oczyścić z wrażliwych danych;
 *   heap dump = zrzut pamięci (całej sterty JVM); thread dump = zrzut wątków; liveness = żywotność; readiness = gotowość.
 *
 * ZOBACZ TEŻ: s03_boot_basics/Boot01AutoConfiguration (skąd Actuator wie, co dołożyć — i raport „conditions”),
 *             s02_configuration/Cfg01Properties (właściwości management.*), s04_web_rest/Rest01Controllers (zwykły kontroler),
 *             s15_actuator_logging/Obs02HealthIndicators (dalej: zdrowie), s12_security/Sec01Basics (jak zabezpieczać adresy).
 * </pre>
 */
public final class Obs01Actuator {

    private Obs01Actuator() {
    }

    // =================================================================================================
    // 1. DOMYŚLNIE: TYLKO /actuator/health
    // =================================================================================================
    //
    // Po dodaniu startera actuator zapytania GET /actuator/health → 200 {"groups":["liveness","readiness"],"status":"UP"}.
    // Wszystko inne (info, env, metrics, loggers, beans, conditions...) → 404 Not Found. Strona /actuator wymienia
    // wtedy tylko self i health. W Boot 4.1.1 sprawdzone uruchomieniem (Obs01ActuatorTest.Defaults).
    // Dlaczego tak? Bezpieczeństwo „domyślnie zamknięte”: nikt nie wystawia przypadkiem listy ustawień aplikacji.
    // Zwróć uwagę: health bez szczegółów to sam status UP/DOWN — to wystarcza systemowi, który tylko pyta „żyjesz?”.
    //
    // Test: Defaults (MockMvc, bez prawdziwego serwera — punkty Actuatora to zwykłe adresy w DispatcherServlet).

    // =================================================================================================
    // 2. WYSTAWIANIE: management.endpoints.web.exposure.include
    // =================================================================================================
    //
    //   management.endpoints.web.exposure.include=health,info,metrics,loggers,env,beans,conditions
    //
    // Lista identyfikatorów (id) rozdzielona przecinkami. Plik application-obs01.properties w tej lekcji robi dokładnie
    // to i pozwala uruchomić aplikację (Obs01Application, plik http/obs01_actuator.http). Co pokazują:
    //   /actuator/health      — status; ze szczegółami po management.endpoint.health.show-details=always (Obs02)
    //   /actuator/info        — dane o aplikacji (sekcja 3)
    //   /actuator/metrics     — nazwy liczników; /actuator/metrics/{nazwa} — wartość jednego (Obs03)
    //   /actuator/loggers     — poziomy logów, ZMIENIANE w czasie działania (Obs04)
    //   /actuator/env         — właściwości ze wszystkich źródeł (sekcja 4)
    //   /actuator/beans       — wszystkie beany kontekstu (nazwa → typ, zależności)
    //   /actuator/conditions  — raport autokonfiguracji: positiveMatches (zadziałały), negativeMatches (nie zadziałały —
    //                           z powodem). To ten sam raport, który widzisz w logu z debug=true (Boot01AutoConfiguration).
    //
    // Są też inne: configprops (obiekty @ConfigurationProperties), mappings (wszystkie adresy aplikacji), threaddump,
    // scheduledtasks, sbom i dalsze zależne od dodanych modułów. Pełną listę zobaczysz na stronie /actuator po include=*.
    //
    // DOBRA PRAKTYKA: wymieniaj punkty końcowe po nazwach (include=health,info,metrics), nie gwiazdką. Dlaczego? Lista
    //   jest dokumentacją tego, co świadomie udostępniasz. Gwiazdka wystawi też każdy punkt, który dołoży przyszła
    //   zależność — i nikt tego nie zauważy w przeglądzie kodu.
    //
    // Test: Exposed (200 dla wymienionych, 404 dla niewymienionego configprops; beans zawiera productController).

    // =================================================================================================
    // 3. /actuator/info — dane aplikacji
    // =================================================================================================
    //
    // info składa się z „współtwórców” (InfoContributor — contributor = współtwórca). Boot ma kilka: build (wersja z
    // build-info), git (informacje o commicie), java, os, process — i env, który wypisuje właściwości zaczynające się od
    // info.*. W Boot 4.1.1 współtwórca env jest domyślnie WYŁĄCZONY (management.info.env.enabled=false), więc:
    //   info.app.name=Sklep SpringLearning     ← samo to nie wystarcza
    //   management.info.env.enabled=true       ← dopiero z tym /actuator/info pokaże {"app":{"name":"Sklep SpringLearning",...}}
    //
    // PUŁAPKA: wystawiasz info, ustawiasz info.app.name — a odpowiedź to puste {}. Dlaczego? Współtwórca env jest
    //   domyślnie wyłączony (od Boota 3.0), więc właściwości info.* nikt nie czyta. Odpowiedź 200 z pustym JSON-em
    //   wygląda jak „działa”, a nie pokazuje nic — łatwo to przeoczyć. Włącz management.info.env.enabled=true.
    //
    // Test: InfoWithData i InfoWithoutEnvContributor (oba warianty na prawdziwym kontekście).

    // =================================================================================================
    // 4. /actuator/env — ustawienia aplikacji, ale zamaskowane
    // =================================================================================================
    //
    // env pokazuje wszystkie źródła właściwości (pliki, zmienne środowiskowe, argumenty) i skąd pochodzi dana wartość.
    // W Boot 4.1.1 domyślnie wartości są MASKOWANE (management.endpoint.env.show-values=never):
    //   GET /actuator/env/sklep.klucz-testowy → {"property":{"source":"...","value":"******"}, ...}
    // Widzisz nazwę i źródło, ale nie wartość. Zmieniasz to przez show-values=always (każdy widzi wszystko) albo
    // when-authorized (tylko autoryzowani, wymaga zabezpieczeń — s12_security/Sec01Basics).
    //
    // PUŁAPKA: show-values=always przy wystawionym env na publicznym adresie. Dlaczego to groźne? env zawiera hasła do
    //   bazy, klucze API i adresy wewnętrznych usług. Nawet zamaskowany env zdradza strukturę ustawień i nazwy
    //   usług — to prezent dla atakującego. Wystawiaj env tylko w sieci zarządzania albo za uwierzytelnieniem.
    //
    // Test: EnvMasked (******) i EnvRevealed (wartość wprost — test pokazuje, jak łatwo to ujawnić).

    // =================================================================================================
    // 5. include=* i dostęp: czego gwiazdka NIE wystawia
    // =================================================================================================
    //
    // include=* wystawia wszystkie punkty końcowe — z jednym zastrzeżeniem: shutdown i heapdump mają domyślnie
    // management.endpoint.<id>.access=none (sprawdzone w metadanych Boota 4.1.1). Zatem nawet przy gwiazdce
    // GET /actuator/heapdump → 404, a na stronie /actuator nie ma linku. Zrzut sterty (heap dump) to cały plik z
    // zawartością pamięci aplikacji — hasła, tokeny sesji, dane klientów. Żeby go dostać, trzeba to ZROBIĆ jawnie:
    //   management.endpoint.heapdump.access=unrestricted
    // Odwrotnie: wyłączasz jeden punkt mimo gwiazdki przez exclude (exclude ma pierwszeństwo przed include):
    //   management.endpoints.web.exposure.include=*
    //   management.endpoints.web.exposure.exclude=env,threaddump
    //
    // PUŁAPKA: include=* „na chwilę, żeby sprawdzić” i zapomniane w profilu produkcyjnym. Dlaczego? Wystawia env
    //   (nazwy ustawień), beans, mappings (mapa całego API), threaddump, configprops. Takie ustawienie lubi „zostać”.
    //
    // Test: Wildcard (są env/threaddump/mappings/configprops, nie ma shutdown/heapdump) i ExcludeAndAccess.

    // =================================================================================================
    // 6. OSOBNY PORT ZARZĄDZANIA
    // =================================================================================================
    //
    //   management.server.port=9090   ← Actuator słucha na 9090, a API sklepu nadal na server.port (8080)
    //
    // Na porcie aplikacji /actuator/health daje wtedy 404, a na porcie zarządzania nie ma /api/products. Sprawdzone
    // testem SeparateManagementPort (RANDOM_PORT; wartość 0 = losowy wolny port, @LocalManagementPort wstrzykuje go).
    // Po co? Port zarządzania możesz zamknąć firewallem (zapora sieciowa), a port aplikacji zostawić publicznym — bez
    // pisania ani jednej reguły bezpieczeństwa w kodzie. Dodatkowo management.endpoints.web.base-path zmienia /actuator
    // na inny adres, a management.server.address=127.0.0.1 ogranicza nasłuch do lokalnej maszyny.
    //
    // DOBRA PRAKTYKA: na produkcji trzymaj Actuator na osobnym porcie dostępnym tylko z sieci wewnętrznej
    //   (monitoring, Kubernetes). Dlaczego? Wtedy pomyłka w konfiguracji bezpieczeństwa API nie ujawni Actuatora, a
    //   błędne ustawienie Actuatora nie ujawni API. Dwa osobne zamki zamiast jednego.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Starter spring-boot-starter-actuator → adresy pod /actuator. Domyślnie przez HTTP tylko /actuator/health.
     *   • Wystawienie: management.endpoints.web.exposure.include=a,b,c; wyjątki: exposure.exclude (ma pierwszeństwo).
     *   • Dostęp: management.endpoint.<id>.access = none | read-only | unrestricted (Boot 3.4+; dawne ".enabled" jest stare).
     *   • shutdown i heapdump mają access=none — nie wystawi ich nawet include=*.
     *   • info: potrzebne management.info.env.enabled=true, żeby pokazać właściwości info.*.
     *   • env: wartości zamaskowane (******), dopóki nie ustawisz management.endpoint.env.show-values.
     *   • Osobny port: management.server.port; adres bazowy: management.endpoints.web.base-path.
     *   • Testy: MockMvc (get("/actuator/...")) albo RANDOM_PORT + @LocalManagementPort dla osobnego portu.
     *
     * PYTANIA KONTROLNE:
     *   1. Które punkty końcowe Actuatora są dostępne przez HTTP, gdy dodasz starter i nic nie ustawisz?
     *   2. Co się stanie? Ustawiasz management.endpoints.web.exposure.include=* i wysyłasz GET /actuator/heapdump.
     *   3. Co się stanie? Ustawiasz include=env oraz exclude=env i wysyłasz GET /actuator/env.
     *   4. ZNAJDŹ BŁĄD: include=info, info.app.name=Sklep — a GET /actuator/info zwraca {}. Czego brakuje?
     *   5. Co się stanie? Ustawiasz management.server.port=9090 (aplikacja na 8080) i wysyłasz GET http://localhost:8080/actuator/health.
     *   6. Co pokazuje /actuator/env/sklep.klucz-testowy przy ustawieniach domyślnych i dlaczego?
     *   7. Czym różni się „wystawienie” (exposure) od „dostępu” (access)?
     *   8. Dlaczego include=* w profilu produkcyjnym to zły pomysł?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Obs01ActuatorExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj linię właściwości, która wystawia podane punkty końcowe, np. dla ["health", "info"]
     * → "management.endpoints.web.exposure.include=health,info". Dla pustej listy zgłoś IllegalArgumentException
     * (wystawienie „niczego” to błąd w konfiguracji, a nie ustawienie).
     * Podpowiedź: String.join(",", lista).
     */
    static String exercise1(List<String> endpointIds) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): rozstrzygnij, czy punkt końcowy jest wystawiony przez HTTP według reguł Actuatora.
     * include i exclude to listy id rozdzielone przecinkami albo "*" (wszystko); pusty tekst = brak wpisów.
     * Reguły: exclude ma pierwszeństwo; punkt musi być na liście include (albo include to "*"); porównuj bez
     * rozróżniania wielkości liter i pomijaj spacje wokół nazw.
     * Przykłady: ("health,info", "", "info") → true; ("*", "env", "env") → false; ("health", "", "env") → false.
     * Podpowiedź: podziel tekst po przecinkach, strip(), toLowerCase().
     */
    static boolean exercise2(String include, String exclude, String endpointId) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): z punktów końcowych wystawionych przez HTTP wybierz te RYZYKOWNE i zwróć je posortowane
     * alfabetycznie. Ryzykowne to: beans, configprops, env, heapdump, loggers, mappings, shutdown, threaddump.
     * Przykład: ["health", "env", "metrics", "beans"] → ["beans", "env"].
     * Podpowiedź: zbiór RISKY poniżej, strumień, filter, sorted (t13_lambdas/Lambda03JavaUtilFunction).
     */
    static List<String> exercise3(Set<String> exposedEndpointIds) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Punkty końcowe uznawane za ryzykowne w ćwiczeniu 3 (RISKY = ryzykowne). */
    static final Set<String> RISKY = Set.of(
            "beans", "configprops", "env", "heapdump", "loggers", "mappings", "shutdown", "threaddump");

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Tylko health (strona /actuator wymienia self i health). Info, env, metrics, loggers, beans i conditions
     *      dają 404. Sprawdzone: Obs01ActuatorTest.Defaults.
     *   2. 404 Not Found. Punkt heapdump ma domyślnie access=none, więc include=* go nie wystawia i nie ma go na
     *      stronie /actuator. Sprawdzone uruchomieniem aplikacji z include=* (404 z ciałem JSON „Not Found”) oraz
     *      testem Wildcard (brak linków heapdump i shutdown).
     *   3. 404 Not Found — exclude ma pierwszeństwo przed include. (Test ExcludeAndAccess sprawdza to dla include=*
     *      z exclude=env,threaddump: ich linków nie ma na stronie /actuator.)
     *   4. Brakuje management.info.env.enabled=true. Współtwórca env jest domyślnie wyłączony, więc info.* nie jest
     *      czytane. Sprawdzone: InfoWithoutEnvContributor (200 i ciało {}) kontra InfoWithData.
     *   5. 404 Not Found — z włączonym management.server.port Actuator słucha TYLKO na porcie 9090, a na porcie
     *      aplikacji takiego adresu nie ma. Sprawdzone: SeparateManagementPort (na osobnym porcie 200 dla health,
     *      na porcie aplikacji 404).
     *   6. {"property":{"source":...,"value":"******"}} — domyślnie show-values=never, więc wartość jest zamaskowana;
     *      widać tylko nazwę i źródło. Sprawdzone: EnvMasked; z show-values=always (EnvRevealed) wartość jest jawna.
     *   7. Wystawienie (exposure.include/exclude) decyduje, czy punkt końcowy jest dostępny przez dany kanał (HTTP, JMX);
     *      dostęp (access) decyduje o poziomie uprawnień do punktu w ogóle (none, read-only, unrestricted). Punkt działa
     *      przez HTTP, gdy jest wystawiony ORAZ jego access nie jest none (dlatego heapdump nie działa przy include=*).
     *   8. Wystawia env, beans, mappings, configprops, threaddump i każdy punkt dodany później przez nowe zależności —
     *      to mapa aplikacji i ustawień dla atakującego. Lepiej wymieniać potrzebne punkty z nazwy.
     */
    // </editor-fold>
}

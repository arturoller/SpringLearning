package s15_actuator_logging.obs02_health_indicators;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Zdrowie aplikacji — HealthIndicator, szczegóły, grupy i sondy liveness/readiness
 *        (health = zdrowie; indicator = wskaźnik; details = szczegóły; group = grupa; probe = sonda;
 *        liveness = żywotność („czy proces żyje”); readiness = gotowość („czy przyjmuje ruch”))
 *
 * W SKRÓCIE:
 *   /actuator/health składa się z wielu małych wskaźników (HealthIndicator): dysk, ping, baza (gdy jest starter JDBC),
 *   a także TWOJE — np. „czy magazyn odpowiada”. Każdy zwraca UP albo DOWN, a Spring zbiera je w jedną odpowiedź:
 *   jeden DOWN = cała aplikacja DOWN i kod HTTP 503. Dla orkiestratora (Kubernetes) istnieją osobne sondy:
 *   liveness (restartuj, gdy martwa) i readiness (nie kieruj ruchu, gdy nie jest gotowa).
 *
 * ANALOGIA: badania okresowe w przychodni.
 *   Lekarz robi kilka niezależnych badań (ciśnienie, krew, wzrok). Gdy JEDNO wypadnie źle, w karcie stoi „do
 *   leczenia”. Liveness to pytanie „czy pacjent w ogóle żyje?” (jeśli nie — reanimacja = restart), readiness to
 *   „czy może teraz przyjąć gości?” (nie — poczekajcie na korytarzu, ale go nie reanimujemy).
 *
 * JAK TO DZIAŁA:
 *   1. Każdy bean typu HealthIndicator jest automatycznie zbierany do /actuator/health. Nazwa składnika to nazwa
 *      beana bez końcówki "HealthIndicator" (warehouseHealthIndicator → "warehouse").
 *   2. Odpowiedź = status zbiorczy + (opcjonalnie) składniki. Statusy: UP, DOWN, OUT_OF_SERVICE, UNKNOWN.
 *      Wygrywa najgorszy w kolejności DOWN, OUT_OF_SERVICE, UP, UNKNOWN (management.endpoint.health.status.order).
 *   3. Kod HTTP: DOWN i OUT_OF_SERVICE → 503, pozostałe → 200 (management.endpoint.health.status.http-mapping).
 *   4. Szczegóły: management.endpoint.health.show-details = never (domyślnie) | when-authorized | always.
 *   5. Grupa: management.endpoint.health.group.{@code <nazwa>}.include=a,b → osobny adres {@code /actuator/health/<nazwa>}.
 *   6. Sondy: grupy "liveness" i "readiness" są w Boot 4.1.1 włączone domyślnie
 *      (management.endpoint.health.probes.enabled=true), a ich stan pochodzi z ApplicationAvailability.
 *
 *   PRZED/PO w Boot 4: klasy zdrowia przeniesiono do osobnego modułu — HealthIndicator, Health, Status są teraz w
 *   org.springframework.boot.health.contributor (w Boot 3: org.springframework.boot.actuate.health). Stary import
 *   w tutorialu z internetu się nie skompiluje; popraw pakiet. Stany dostępności: org.springframework.boot.availability.
 *
 * SŁÓWKA:
 *   health indicator = wskaźnik zdrowia; contributor = współtwórca (składnik odpowiedzi); status = stan;
 *   up/down = działa/nie działa; out of service = wyłączony z użytku; unknown = nieznany; detail = szczegół;
 *   when authorized = gdy uprawniony; refusing traffic = odmawianie ruchu; accepting traffic = przyjmowanie ruchu;
 *   availability = dostępność; orchestrator = orkiestrator (np. Kubernetes — zarządza uruchomionymi kontenerami).
 *
 * ZOBACZ TEŻ: s15_actuator_logging/Obs01Actuator (wystawianie punktów końcowych i show-details w osobnym porcie),
 *             s03_boot_basics/Boot03SpringApplication (życie aplikacji: start, zdarzenia),
 *             s19_deploy/Dep04StartupAndShutdown (eleganckie zamykanie a readiness),
 *             s12_security/Sec02Authorization (when-authorized i role).
 * </pre>
 */
public final class Obs02HealthIndicators {

    private Obs02HealthIndicators() {
    }

    // =================================================================================================
    // 1. WŁASNY WSKAŹNIK: WarehouseHealthIndicator
    // =================================================================================================
    //
    // Wystarczy klasa z @Component implementująca HealthIndicator i metoda health(), która zwraca Health.up() albo
    // Health.down() z dowolnymi szczegółami (withDetail). To zwykła klasa Javy — test 1 tworzy ją przez new, bez Springa:
    //   Health.up().withDetail("items", 14).build()  →  status UP, details {items=14}
    //
    // Dlaczego warto? Gdy magazyn przestaje odpowiadać, to NIE kod aplikacji pada — pada zależność. Bez wskaźnika
    // dowiesz się od klientów; ze wskaźnikiem monitoring pokaże DOWN, zanim ktokolwiek złoży zamówienie.
    //
    // PUŁAPKA: wskaźnik, który robi wolne zapytanie (np. pełne skanowanie bazy albo wywołanie API bez limitu czasu).
    //   Dlaczego to groźne? Orkiestrator woła /actuator/health co kilka sekund — wolny wskaźnik zablokuje odpowiedź,
    //   a przeterminowana sonda może wywołać restart zdrowej aplikacji. Sprawdzaj tanio i z krótkim limitem czasu.
    //   (Boot 4.1.1 loguje ostrzeżenie dla wskaźnika wolniejszego niż management.endpoint.health.logging.slow-indicator-threshold,
    //   domyślnie 10 s.)

    // =================================================================================================
    // 2. AbstractHealthIndicator — wyjątek zamienia się w DOWN
    // =================================================================================================
    //
    // Zamiast pisać try/catch, dziedzicz po AbstractHealthIndicator i implementuj doHealthCheck(Health.Builder).
    // Wyjątek z tej metody Boot łapie, loguje (poziom WARN: "Health check failed" ze śladem stosu) i zwraca
    // Health.down() ze szczegółem error = "java.lang.IllegalStateException: brak połączenia z bazą" (test 2).
    //
    // DOBRA PRAKTYKA: do prostych kontroli używaj AbstractHealthIndicator. Dlaczego? Nie zapomnisz złapać wyjątku —
    //   a niezłapany wyjątek w gołym HealthIndicator także nie kładzie endpointu (Boot go obsłuży), ale tracisz
    //   jednolity szczegół error i jednolite logowanie.

    // =================================================================================================
    // 3. SZCZEGÓŁY: show-details
    // =================================================================================================
    //
    // Domyślnie (never) odpowiedź to {"groups":["liveness","readiness"],"status":"UP"} — bez sekcji components (test 3).
    // Z show-details=always: components → warehouse {status, details{items}}, ping, diskSpace, ssl... (test 4).
    //
    // PUŁAPKA: show-details=always na publicznym porcie. Dlaczego? Szczegóły zdradzają ścieżki dysków, nazwy baz,
    //   wersje i adresy zależności. Użyj when-authorized (po dodaniu Spring Security — s12_security) albo wystaw
    //   Actuator na osobnym, wewnętrznym porcie (Obs01Actuator, sekcja 6).

    // =================================================================================================
    // 4. GRUPY, SONDY I ZDARZENIA DOSTĘPNOŚCI
    // =================================================================================================
    //
    // GRUPA: management.endpoint.health.group.sklep.include=warehouse,ping → GET /actuator/health/sklep zwraca status
    // tylko tych dwóch składników (diskSpace nie ma). Grupa ma własne show-details.
    //
    // SONDY (Kubernetes liveness/readiness):
    //   GET /actuator/health/liveness  → {"status":"UP"}  — „aplikacja nie jest zawieszona/zepsuta wewnętrznie”
    //   GET /actuator/health/readiness → {"status":"UP"}  — „może przyjmować zapytania”
    // Stany pochodzą z ApplicationAvailability (LivenessState: CORRECT/BROKEN, ReadinessState: ACCEPTING_TRAFFIC/
    // REFUSING_TRAFFIC), a zmieniasz je zdarzeniem:
    //   AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
    // Wtedy GET /actuator/health/readiness → 503 {"status":"OUT_OF_SERVICE"}, a liveness nadal 200 (test 4).
    // Wniosek: aplikacja może być „żywa, ale niegotowa” — i dokładnie to chcesz, np. podczas rozgrzewania cache albo
    // zamykania (przy eleganckim zamykaniu — graceful shutdown — Boot sam zmienia gotowość na odmowę ruchu; Dep04StartupAndShutdown).
    //
    // Ważne: wskaźnik warehouse jest w głównym /actuator/health, ale NIE w sondach (test: „zepsuty magazyn nie wpływa
    // na sondy”). To świadoma zasada Boota: sondy mają odpowiadać na pytania o samą aplikację, nie o jej zależności.
    //
    // PUŁAPKA: dodanie bazy lub zewnętrznego API do grupy liveness (management.endpoint.health.group.liveness.include=db).
    //   Dlaczego to błąd? Gdy baza padnie, Kubernetes „wyleczy” aplikację restartem — a to nic nie pomoże bazie.
    //   Dostaniesz kaskadę restartów wszystkich instancji. Zależności sprawdzaj w readiness albo w zwykłym health.

    // =================================================================================================
    // 5. WYŁĄCZENIE SOND
    // =================================================================================================
    //
    // management.endpoint.health.probes.enabled=false → /actuator/health/liveness i /readiness dają 404 (test ProbesDisabled).
    // Uwaga na stare tutoriale: w Boot 2.x/3.x sondy włączały się tylko w Kubernetes i trzeba je było włączać
    // ustawieniem management.health.probes.enabled — w Boot 4.1.1 ta stara właściwość jest oznaczona jako błąd
    // (level error), a nowa, management.endpoint.health.probes.enabled, ma wartość domyślną true.

    // =================================================================================================
    // 6. JAK BOOT LICZY STATUS ZBIORCZY I KOD HTTP
    // =================================================================================================
    //
    // Test sprawdza prawdziwe klasy Boota (SimpleStatusAggregator, SimpleHttpCodeStatusMapper):
    //   (UP, UNKNOWN) → UP · (UP, OUT_OF_SERVICE) → OUT_OF_SERVICE · (OUT_OF_SERVICE, DOWN, UP) → DOWN · () → UNKNOWN
    //   kod HTTP: UP 200, UNKNOWN 200, DOWN 503, OUT_OF_SERVICE 503.
    // Ćwiczenia 1 i 2 odtwarzają tę logikę samodzielnie.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Własny wskaźnik: @Component + implements HealthIndicator (pakiet org.springframework.boot.health.contributor).
     *   • Health.up()/down().withDetail(klucz, wartość).build(); AbstractHealthIndicator łapie wyjątki → DOWN + error.
     *   • Nazwa składnika = nazwa beana bez "HealthIndicator".
     *   • Jeden DOWN → status zbiorczy DOWN → HTTP 503. DOWN i OUT_OF_SERVICE → 503, UP i UNKNOWN → 200.
     *   • show-details: never (domyślnie) | when-authorized | always.
     *   • Grupa: management.endpoint.health.group.<nazwa>.include=... → /actuator/health/<nazwa>.
     *   • Sondy /actuator/health/liveness i /readiness są domyślnie włączone; zmiana stanu: AvailabilityChangeEvent.publish.
     *   • liveness = „czy restartować”, readiness = „czy kierować ruch”. Zależności zewnętrzne NIE do liveness.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaką nazwę składnika dostanie bean WarehouseHealthIndicator w odpowiedzi /actuator/health?
     *   2. Co się stanie? Magazyn jest niedostępny (wskaźnik zwraca DOWN), a Ty wywołujesz GET /actuator/health/readiness.
     *   3. Co się stanie? Publikujesz AvailabilityChangeEvent z ReadinessState.REFUSING_TRAFFIC i wołasz readiness oraz liveness.
     *   4. ZNAJDŹ BŁĄD: management.endpoint.health.group.liveness.include=db,ping — co jest złego w takiej konfiguracji?
     *   5. ZNAJDŹ BŁĄD: import org.springframework.boot.actuate.health.HealthIndicator w projekcie na Boot 4.1.1.
     *   6. Co się stanie? Zwykły, domyślnie skonfigurowany projekt — GET /actuator/health po dodaniu własnego wskaźnika:
     *      czy zobaczysz w odpowiedzi składnik "warehouse"?
     *   7. Dlaczego orkiestrator potrzebuje DWÓCH sond, a nie jednej?
     *   8. Jaki kod HTTP zwróci /actuator/health, gdy jeden wskaźnik jest UP, a drugi OUT_OF_SERVICE?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Obs02HealthIndicatorsExercisesTest
    // =================================================================================================

    /** HealthResult = wynik własnego sprawdzenia: status ("UP"/"DOWN") i szczegóły (używany w ćwiczeniu 3). */
    public record HealthResult(String status, Map<String, Object> details) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): zamień status zdrowia na kod HTTP jak robi to Boot domyślnie:
     * "DOWN" i "OUT_OF_SERVICE" → 503, "UP" i "UNKNOWN" → 200. Dla innego tekstu — IllegalArgumentException.
     * Podpowiedź: switch z wieloma etykietami (t02_controlflow/Control02Switch).
     */
    static int exercise1(String status) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): policz status zbiorczy z listy statusów. Wygrywa najgorszy w kolejności
     * DOWN, OUT_OF_SERVICE, UP, UNKNOWN (DOWN jest „najgorszy”). Pusta lista → "UNKNOWN".
     * Przykład: ["UP", "OUT_OF_SERVICE"] → "OUT_OF_SERVICE"; ["UP", "UNKNOWN"] → "UP".
     * Podpowiedź: lista ORDER poniżej i indexOf (im mniejszy indeks, tym gorszy status).
     */
    static String exercise2(List<String> statuses) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Kolejność od najgorszego do najlepszego statusu (do ćwiczenia 2). */
    static final List<String> ORDER = List.of("DOWN", "OUT_OF_SERVICE", "UP", "UNKNOWN");

    /**
     * ĆWICZENIE 3 (trudniejsze): napisz kontrolę wolnego miejsca na dysku jak DiskSpaceHealthIndicator.
     * Gdy freeBytes >= thresholdBytes → HealthResult("UP", {"free": freeBytes, "threshold": thresholdBytes}).
     * W przeciwnym razie → HealthResult("DOWN", {"free": ..., "threshold": ..., "error": "Za mało miejsca na dysku"}).
     * Ujemna wartość któregokolwiek argumentu → IllegalArgumentException.
     * Podpowiedź: Map.of(...) i osobna Map dla wariantu DOWN (t12_collections/Collections08ImmutableUnmodifiable).
     */
    static HealthResult exercise3(long freeBytes, long thresholdBytes) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. "warehouse" — Boot ucina końcówkę "HealthIndicator" z nazwy beana (warehouseHealthIndicator).
     *      Sprawdzone: Obs02HealthIndicatorsTest, WithDetails.customIndicatorAppears.
     *   2. 200 {"status":"UP"} — wskaźnik magazynu jest w głównym /actuator/health, a sonda readiness ma własną grupę
     *      stanu aplikacji (ApplicationAvailability); zależności nie wpływają na nią automatycznie.
     *      Sprawdzone: brokenWarehouseDoesNotTouchProbes (główne /actuator/health jednocześnie daje 503 i DOWN).
     *   3. readiness → 503 {"status":"OUT_OF_SERVICE"}, liveness → 200 {"status":"UP"}. Sprawdzone testem
     *      refusingTrafficChangesReadinessOnly.
     *   4. Liveness odpowiada na „czy restartować aplikację”. Gdy baza padnie, liveness zrobi się DOWN i orkiestrator
     *      zacznie restartować zdrowe aplikacje, co bazie nie pomoże (kaskada restartów). Zależności → readiness.
     *      W tym projekcie (bez bazy) aplikacja w ogóle nie wystartuje: sprawdzone uruchomieniem — błąd
     *      „Health contributor 'db' defined in 'management.endpoint.health.group.liveness.include' does not exist”
     *      (walidacja management.endpoint.health.validate-group-membership=true).
     *   5. Pakiet nie istnieje w Boot 4 — klasy zdrowia są w org.springframework.boot.health.contributor, więc
     *      import nie skompiluje się („package ... does not exist”). Nowy import to jedyna zmiana w kodzie.
     *   6. Tak, ale tylko z show-details=always albo when-authorized (po zalogowaniu). Domyślnie (never) widać sam status
     *      — brak sekcji components. Sprawdzone: DefaultsNoDetails i WithDetails.
     *   7. Bo to dwa różne pytania z dwiema różnymi reakcjami: „martwa” → zabij i uruchom od nowa; „niegotowa” → nie
     *      kieruj ruchu, ale nie restartuj (np. rozgrzewa się albo właśnie się zamyka). Jedna sonda zmuszałaby do wyboru.
     *   8. 503 — statusem zbiorczym jest OUT_OF_SERVICE (gorszy niż UP), a OUT_OF_SERVICE mapuje się na 503.
     *      Sprawdzone: aggregateStatus i defaultHttpCodes.
     */
    // </editor-fold>
}

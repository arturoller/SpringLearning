package s19_deploy.dep04_startup_and_shutdown;

import java.time.Duration;
import java.util.List;

/**
 * <pre>
 * TEMAT: Start i zamykanie aplikacji — łagodne zamykanie, sondy liveness/readiness, czas startu, leniwa inicjalizacja
 *        i pamięć podręczna AOT
 *        (startup = start; shutdown = zamykanie; graceful = łagodne, „z klasą”; probe = sonda; liveness = „żyję”;
 *        readiness = gotowość; lazy initialization = leniwa inicjalizacja)
 *
 * W SKRÓCIE:
 *   Na serwerze aplikacja jest uruchamiana i zatrzymywana automatycznie — przy każdym wdrożeniu nowej wersji,
 *   skalowaniu, awarii. Dwie sprawy decydują, czy użytkownik to zauważy: (1) czy zamykana aplikacja DOKOŃCZY żądania
 *   w toku, zanim zgaśnie (łagodne zamykanie), oraz (2) czy platforma (Kubernetes, balansier obciążenia) wie, kiedy
 *   aplikacja jest gotowa na ruch, a kiedy trzeba ją zrestartować (sondy). Lekcja pokazuje też, co przyspiesza start.
 *
 * ANALOGIA: zamykanie restauracji.
 *   Zamknięcie natychmiastowe (immediate): kelner zabiera talerze z rąk gości i wyprasza ich z lokalu.
 *   Zamknięcie łagodne (graceful): kelner wiesza na drzwiach „nieczynne”, nikogo nowego nie wpuszcza, ale
 *   goście przy stolikach spokojnie kończą posiłek; dopiero potem gasną światła. Sondy to tabliczka „otwarte/zamknięte”
 *   (readiness: czy przyjmujemy gości) i dzwonek do kierownika, który sprawdza, czy ktoś w ogóle jest w kuchni (liveness).
 *
 * JAK TO DZIAŁA:
 *   Zamykanie (np. docker stop → SIGTERM, Ctrl+C):
 *     1. JVM uruchamia hak zamykania Springa (shutdown hook).
 *     2. Spring publikuje zmianę gotowości na REFUSING_TRAFFIC (sonda readiness zaczyna odpowiadać 503) — platforma
 *        przestaje kierować nowy ruch.
 *     3. Serwer (Tomcat) przestaje przyjmować NOWE połączenia, ale czeka na żądania w toku
 *        (log: „Commencing graceful shutdown. Waiting for active requests to complete”).
 *     4. Gdy żądania się skończą albo minie limit (spring.lifecycle.timeout-per-shutdown-phase, domyślnie 30 s),
 *        serwer się zatrzymuje („Graceful shutdown complete”), potem Spring niszczy beany i kończy proces.
 *
 * SŁÓWKA:
 *   graceful shutdown = łagodne zamykanie; immediate = natychmiastowe; in-flight request = żądanie w toku;
 *   timeout = limit czasu; liveness probe = sonda „żyję” (restartować?); readiness probe = sonda gotowości
 *   (kierować ruch?); load balancer = balanser obciążenia (rozdziela ruch); AOT cache = pamięć podręczna
 *   „z wyprzedzeniem”; lazy = leniwy; pod = najmniejsza jednostka w Kubernetesie (kontener z aplikacją);
 *   grace period = okres łaski (czas na zamknięcie).
 *
 * ZOBACZ TEŻ: s15_actuator_logging/Obs02HealthIndicators (własne wskaźniki zdrowia i sondy),
 *             s19_deploy/Dep02Docker (SIGTERM przy docker stop), s19_deploy/Dep03ProductionConfig (fail fast),
 *             s22_extras/Ext06NativeImage (natywny plik — jeszcze szybszy start).
 * </pre>
 */
public final class Dep04StartupAndShutdown {

    private Dep04StartupAndShutdown() {
    }

    // =================================================================================================
    // 1. ŁAGODNE ZAMYKANIE — server.shutdown i spring.lifecycle.timeout-per-shutdown-phase
    // =================================================================================================
    //
    // Wartości domyślne w Boot 4.1.1 (sprawdzone w metadanych i w teście sekcji 1):
    //     server.shutdown                              = graceful   (łagodne — wartość domyślna, nic nie trzeba włączać)
    //     spring.lifecycle.timeout-per-shutdown-phase  = 30s
    // Czyli nie musisz niczego włączać — ale musisz to rozumieć i dopasować limit do swojego najdłuższego żądania.
    // (W starszych wersjach Boota domyślne było zamykanie natychmiastowe, więc tutoriale każą ustawiać
    // server.shutdown=graceful — w 4.1.1 jest to wartość domyślna; sprawdzone tylko dla tej wersji.)
    //
    // Prawdziwy wynik testu sekcji 2 (aplikacja na prawdziwym porcie, żądanie /api/slow-order „utknęło” w OrderGate;
    // tryb graceful, potem zamykanie kontekstu w osobnym wątku):
    //     GracefulShutdown : Commencing graceful shutdown. Waiting for active requests to complete
    //     nowe połączenie TCP w czasie zamykania:   java.net.ConnectException: Connection refused
    //     żądanie w toku po zwolnieniu bramki:      200 "ZAM-001 gotowe"      ← DOKOŃCZONE
    //     GracefulShutdown : Graceful shutdown complete
    //     stany gotowości:                          [ACCEPTING_TRAFFIC, REFUSING_TRAFFIC]
    // To samo w trybie immediate (--server.shutdown=immediate), prawdziwy wynik:
    //     w logu: „Request processing failed: java.lang.InterruptedException” (wątek żądania przerwany)
    //     klient:  java.io.IOException: HTTP/1.1 header parser received no bytes   ← żądanie PRZERWANE, użytkownik widzi błąd
    //
    // PUŁAPKA: zbyt krótki limit (spring.lifecycle.timeout-per-shutdown-phase) albo zbyt krótki czas, jaki platforma
    //   daje na zamknięcie. Dlaczego? Limit Springa to maksimum oczekiwania — po jego upływie żądania w toku są
    //   przerywane. A Kubernetes po czasie terminationGracePeriodSeconds (domyślnie 30 s) zabija proces siłą (SIGKILL)
    //   niezależnie od Springa. Reguła: terminationGracePeriodSeconds MUSI być większy niż limit Springa plus czas
    //   preStop (ćwiczenie 3 sprawdza taką konfigurację).
    //
    // PUŁAPKA: łagodne zamykanie obejmuje żądania HTTP w toku, a NIE Twoje wątki w tle (zadania @Async, własne pule,
    //   przetwarzanie kolejek). Dlaczego? Tomcat nie wie o wątkach, które sam nie utworzył. Takie zadania zatrzymuj
    //   w metodach @PreDestroy / SmartLifecycle albo przez konfigurację pul (np. await-termination-period).
    //
    // Prawdziwe zachowanie przy docker stop (Dep02Docker): ten sam komunikat „Commencing graceful shutdown”.

    // =================================================================================================
    // 2. SONDY liveness i readiness — /actuator/health/liveness i /actuator/health/readiness
    // =================================================================================================
    //
    // Dwa pytania, które zadaje platforma:
    //   • LIVENESS („czy proces żyje?”): odpowiedź DOWN/503 = aplikacja jest zepsuta bez nadziei, trzeba ją RESTARTOWAĆ.
    //   • READINESS („czy mogę wysłać ruch?”): odpowiedź 503 = nie kieruj teraz żądań, ale NIE restartuj (np. trwa
    //     rozruch, zamykanie albo chwilowo brakuje zależności).
    //
    // Zmiana względem starszych tutoriali: w Boot 4.1 sondy są WŁĄCZONE domyślnie
    // (management.endpoint.health.probes.enabled = true — sprawdzone w metadanych, i uruchomieniem na zwykłym
    // komputerze, bez Kubernetesa). Dawniej trzeba było je włączać ręcznie poza Kubernetesem. Prawdziwy wynik:
    //     GET /actuator/health            → {"groups":["liveness","readiness"],"status":"UP"}
    //     GET /actuator/health/liveness   → {"status":"UP"}
    //     GET /actuator/health/readiness  → {"status":"UP"}
    // Przełączenie stanu (test sekcji 3): AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC)
    // → readiness odpowiada 503 i {"status":"OUT_OF_SERVICE"}, liveness nadal 200; LivenessState.BROKEN → liveness
    // odpowiada 503 i {"status":"DOWN"}. Własny kod może tak zgłosić „jestem zepsuty” (np. nieodwracalny błąd).
    //
    // Fragment manifestu Kubernetesa (ILUSTRACJA — w tej lekcji nie uruchamialiśmy klastra):
    //     livenessProbe:   { httpGet: { path: /actuator/health/liveness,  port: 8080 } }
    //     readinessProbe:  { httpGet: { path: /actuator/health/readiness, port: 8080 } }
    //     lifecycle: { preStop: { exec: { command: ["sleep", "5"] } } }     ← chwila na „wypięcie” z balansera
    //     terminationGracePeriodSeconds: 60
    // Gdy sondy mają być dostępne na głównym porcie pod krótkimi adresami /livez i /readyz, ustawia się
    // management.endpoint.health.probes.add-additional-paths=true (domyślnie false — z metadanych Boota).
    //
    // PUŁAPKA: wpięcie do sondy liveness sprawdzania zewnętrznych zależności (baza, inna usługa). Dlaczego? Awaria
    //   bazy wywołałaby restart WSZYSTKICH instancji aplikacji naraz (kaskada), choć restart bazie nie pomoże.
    //   Liveness ma odpowiadać tylko na pytanie „czy ten proces działa”. Zależności zewnętrzne to sprawa readiness —
    //   i to ostrożnie (zob. s15_actuator_logging/Obs02HealthIndicators).

    // =================================================================================================
    // 3. CZAS STARTU — jak go zmierzyć
    // =================================================================================================
    //
    // Pierwsza informacja jest w logu (prawdziwy wynik z uruchomienia JAR-a na komputerze autora):
    //     Started Dep01Application in 3.536 seconds (process running for 4.354)
    // (3,5 s „Started” to czas Springa; „process running” liczy od startu JVM.) Szczegóły daje
    // BufferingApplicationStartup: kroki startu aplikacji zapisane jako lista zdarzeń. Włączasz ją przed uruchomieniem:
    //     SpringApplication app = new SpringApplication(Dep04Application.class);
    //     app.setApplicationStartup(new BufferingApplicationStartup(4096));
    // Test sekcji 4: w czasie startu zapisano kilkaset zdarzeń (u autora 286), pierwsze to „spring.boot.application.starting”
    // (liczba zdarzeń zależy od zawartości aplikacji — nie opieraj się na dokładnej wartości). Z Actuatorem punkt
    // /actuator/startup pokazuje to samo przez HTTP — ale wymaga właśnie BufferingApplicationStartup: bez niego
    // autokonfiguracja go nie włącza (prawdziwy wpis raportu warunków: „configured applicationStartup is of type
    // DefaultApplicationStartup, expected BufferingApplicationStartup”).
    //
    // DOBRA PRAKTYKA: mierz, zanim zaczniesz optymalizować. Dlaczego? Start zwykle zajmuje najwięcej na rzeczach,
    //   o których nie myślisz (skanowanie klas, migracje bazy, połączenia z usługami zewnętrznymi). Optymalizacja
    //   bez pomiaru poprawia to, co i tak było szybkie.

    // =================================================================================================
    // 4. LENIWA INICJALIZACJA — spring.main.lazy-initialization=true
    // =================================================================================================
    //
    // Domyślnie Spring tworzy WSZYSTKIE beany singletony przy starcie. Z leniwą inicjalizacją tworzy bean dopiero przy
    // pierwszym użyciu. Test sekcji 5 (licznik instancji ExpensiveBean):
    //     domyślnie:                       po starcie 1 instancja
    //     lazy-initialization=true:        po starcie 0 instancji, po pierwszym getBean — 1
    // Zaleta: szybszy start i mniejsze zużycie pamięci na starcie.
    // WADY (dlatego domyślnie jest wyłączona):
    //   • błędy konfiguracji (brak beana, zły wiązany klucz) NIE wychodzą przy starcie, tylko przy pierwszym
    //     żądaniu — użytkownik jest Twoim testerem; traci się „fail fast” z lekcji Dep03ProductionConfig;
    //   • pierwsze żądanie jest wolniejsze (bean powstaje wtedy);
    //   • wolniejsza reakcja pierwszych użytkowników po wdrożeniu — dokładnie wtedy, gdy ruch dopiero wraca.
    // Jeśli włączasz: tylko dla wybranych beanów (@Lazy na klasie) i najlepiej z rozgrzewką (żądanie testowe po starcie).
    //
    // PUŁAPKA: lazy-initialization=true „żeby przyspieszyć start” na produkcji. Dlaczego? Przerzuca koszt na pierwsze
    //   żądania i ukrywa błędy konfiguracji do czasu użycia. Gdy start jest wolny, najpierw zmierz (sekcja 3).

    // =================================================================================================
    // 5. PAMIĘĆ PODRĘCZNA AOT (JDK 25) — szybszy start bez zmiany kodu
    // =================================================================================================
    //
    // JDK 24+ umie zapisać w pliku „pamięć podręczną AOT” (AOT cache — Ahead-Of-Time, „z wyprzedzeniem”): załadowane
    // i połączone klasy aplikacji, dzięki czemu kolejne starty pomijają tę pracę. Boot 4.1 ma do tego tryb „kontekst
    // wychodzi po odświeżeniu” (spring.context.exit=onRefresh): aplikacja startuje „na sucho” i się kończy.
    // Sprawdzone uruchomieniem (JDK 25.0.4, Boot 4.1.1, JAR rozpakowany trybem tools — Dep01ExecutableJar, sekcja 4):
    //
    //     # 1. przebieg treningowy: powstaje plik app.aot (u nas 59 113 472 bajtów ≈ 56 MB)
    //     java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar s19-deploy-1.0-SNAPSHOT.jar
    //     # 2. produkcja: ten sam JAR (ta sama ścieżka klas!) z plikiem pamięci podręcznej
    //     java -XX:AOTCache=app.aot -jar s19-deploy-1.0-SNAPSHOT.jar
    //
    //     bez AOT:   Started Dep01Application in 3.536 seconds, 3.236 seconds
    //     z AOT:     Started Dep01Application in 1.93 seconds,  1.497 seconds        (dwa pomiary na komputerze autora)
    //
    // Czyli start o około połowę krótszy, bez zmiany kodu. W logu przebiegu treningowego JVM wypisuje ostrzeżenia
    // „Skipping … Unlinked class not supported by AOTClassLinking” — to informacje o klasach pominiętych w pamięci
    // podręcznej, nieszkodliwe. LICZBY zależą od komputera — traktuj je jako rząd wielkości, nie obietnicę.
    //
    // PUŁAPKA: plik AOT jest związany z konkretną wersją JVM i ścieżką klas. Dlaczego? Po zmianie JARa, wersji Javy
    //   albo biblioteki plik trzeba wygenerować od nowa — stary zostanie zignorowany (JVM go odrzuca). Generuj go
    //   w kroku budowania obrazu (po pierwszym uruchomieniu na docelowym JDK), nie trzymaj w repozytorium.
    //
    // NIE SPRAWDZONE W TEJ LEKCJI: uruchomienie z grubym JAR-em (a nie rozpakowanym), wariant dla JDK 21 (tam
    // zamiast AOT jest starszy mechanizm CDS — -XX:ArchiveClassesAtExit) i integracja z Buildpacks. Działają
    // inaczej; sprawdź w dokumentacji Boota (rozdział o efektywnym wdrażaniu: CDS), zanim ich użyjesz.
    //
    // GraalVM Native Image (kompilacja do natywnego pliku, start w ułamku sekundy, ale długie budowanie i ograniczenia
    // refleksji) — zapowiedź: s22_extras/Ext06NativeImage.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • server.shutdown=graceful jest DOMYŚLNE; limit: spring.lifecycle.timeout-per-shutdown-phase (domyślnie 30s).
     *   • Kolejność: readiness → REFUSING_TRAFFIC, serwer nie przyjmuje nowych połączeń, żądania w toku się kończą,
     *     potem zamknięcie beanów. W trybie immediate żądania w toku są przerywane.
     *   • Łagodne zamykanie dotyczy żądań HTTP, nie Twoich wątków w tle.
     *   • Sondy (domyślnie włączone w Boot 4.1): /actuator/health/liveness (restartować?) i /readiness (kierować ruch?).
     *   • Liveness bez zależności zewnętrznych; Kubernetes: terminationGracePeriodSeconds > limit Springa + preStop.
     *   • Czas startu: log „Started … in N seconds”, BufferingApplicationStartup, /actuator/startup.
     *   • Leniwa inicjalizacja: szybszy start, ale błędy dopiero przy użyciu — zwykle nie na produkcji.
     *   • JDK 25: -XX:AOTCacheOutput / -XX:AOTCache + -Dspring.context.exit=onRefresh — start około 2× szybszy.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaka jest domyślna wartość server.shutdown w Boot 4.1 i ile czasu aplikacja czeka na żądania w toku?
     *   2. Co się stanie? Żądanie trwa 40 sekund, a aplikacja dostaje SIGTERM przy domyślnych ustawieniach Springa.
     *   3. Co się stanie? Ustawisz {@code --server.shutdown=immediate} i zamkniesz aplikację podczas żądania w toku.
     *   4. ZNAJDŹ BŁĄD: sonda liveness sprawdza połączenie z bazą danych. Co się stanie, gdy baza na chwilę padnie?
     *   5. ZNAJDŹ BŁĄD: manifest Kubernetesa ma terminationGracePeriodSeconds: 30, a w aplikacji
     *      spring.lifecycle.timeout-per-shutdown-phase=45s. Dlaczego limit Springa nie ma tu znaczenia?
     *   6. Czym różnią się odpowiedzi sond liveness i readiness podczas zamykania aplikacji?
     *   7. Co się stanie? Włączysz spring.main.lazy-initialization=true, a w jednym z beanów jest literówka
     *      w kluczu konfiguracji wymaganym przez bean — kiedy zobaczysz błąd?
     *   8. Dlaczego plik pamięci podręcznej AOT należy wygenerować ponownie po zmianie JAR-a?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Dep04StartupAndShutdownExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): czy platforma może kierować ruch do instancji? Tak tylko wtedy, gdy status sondy readiness
     * to "UP" (liveness nie ma tu znaczenia dla kierowania ruchu).
     * Przykłady: ("UP", "UP") → true; ("UP", "OUT_OF_SERVICE") → false; ("DOWN", "UP") → true (ruch kieruje się
     * wg readiness; restartem zajmie się sonda liveness).
     * Podpowiedź: porównaj drugi argument z "UP".
     */
    static boolean exercise1(String livenessStatus, String readinessStatus) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): decyzja platformy na podstawie statusów obu sond. Zwróć jeden napis:
     *   "RESTART"                  — liveness inny niż "UP" (aplikacja zepsuta; to rozstrzyga niezależnie od readiness);
     *   "WYLACZ_Z_BALANSERA"       — liveness "UP", ale readiness inny niż "UP" (nie kieruj ruchu, nie restartuj);
     *   "ZOSTAW"                   — obie sondy "UP".
     * Podpowiedź: najpierw sprawdź liveness, potem readiness.
     */
    static String exercise2(String livenessStatus, String readinessStatus) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): sprawdź ustawienia zamykania. Dane: limit Springa na fazę zamykania
     * ({@code spring.lifecycle.timeout-per-shutdown-phase}), czas oczekiwania w preStop (w sekundach) i
     * {@code terminationGracePeriodSeconds} Kubernetesa. Zwróć listę problemów (w tej kolejności; pusta = dobrze):
     *   "ZA_KROTKI_CZAS_ZAMKNIECIA" — preStop + limit Springa jest WIĘKSZY lub równy terminationGracePeriodSeconds
     *                                 (Kubernetes zabije proces, zanim Spring skończy);
     *   "ZEROWY_LIMIT"              — limit Springa jest zerowy lub ujemny (żądania w toku od razu przerwane).
     * Przykład: (30 s, 5, 60) → [] ({@code 35 < 60} — dobrze); (30 s, 5, 30) → ["ZA_KROTKI_CZAS_ZAMKNIECIA"].
     * Podpowiedź: Duration.getSeconds(), isZero(), isNegative().
     */
    static List<String> exercise3(Duration springTimeoutPerPhase, int preStopSeconds, int terminationGracePeriodSeconds) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. graceful (łagodne), a limit oczekiwania to 30 s (spring.lifecycle.timeout-per-shutdown-phase). Sprawdzone
     *      w metadanych Boota 4.1.1 i testem sekcji 1.
     *   2. Spring odmówi nowych połączeń i będzie czekał na to żądanie do 30 s; po tym czasie zamknie serwer
     *      i żądanie zostanie przerwane (po 30 s z 40). Trzeba zwiększyć limit (i terminationGracePeriodSeconds).
     *   3. Żądanie w toku zostanie przerwane od razu: w logu InterruptedException, klient dostanie błąd
     *      („HTTP/1.1 header parser received no bytes”). Sprawdzone testem sekcji 2.
     *   4. Awaria bazy sprawi, że liveness odpowie DOWN i platforma zrestartuje WSZYSTKIE instancje, choć restart nie
     *      naprawi bazy — powstaje kaskada restartów. Liveness ma sprawdzać tylko sam proces.
     *   5. Kubernetes po 30 s zabija proces siłą (SIGKILL), więc Spring nie dożyje swoich 45 s. Okres terminacji
     *      musi być dłuższy niż limit Springa plus czas preStop.
     *   6. Podczas zamykania readiness przechodzi na REFUSING_TRAFFIC (503, OUT_OF_SERVICE) — „nie kieruj ruchu”,
     *      a liveness zostaje UP (proces nadal żyje i kończy żądania). Sprawdzone testem sekcji 3.
     *   7. Dopiero przy pierwszym użyciu beana (np. pierwszym żądaniu), a nie przy starcie. To główna wada leniwej
     *      inicjalizacji — traci się fail fast. Sprawdzone testem sekcji 5 na liczniku instancji.
     *   8. Plik jest związany z konkretną wersją JVM i konkretnymi plikami na ścieżce klas; po zmianie JVM zignoruje
     *      go jako niepasujący, więc zyskasz tylko na nowo wygenerowanym.
     */
    // </editor-fold>
}

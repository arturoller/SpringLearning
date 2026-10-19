package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.List;

/**
 * <pre>
 * TEMAT: SpringApplication — co się dzieje od main() do „aplikacja gotowa”
 *        (application = aplikacja; runner = uruchamiacz zadania przy starcie; event = zdarzenie;
 *         listener = słuchacz; exit code = kod wyjścia; banner = baner, napis przy starcie)
 *
 * W SKRÓCIE:
 *   SpringApplication.run(...) to nie „magiczna linijka”, tylko przepis w kilku krokach: przygotuj środowisko
 *   (ustawienia, profile), pokaż baner, utwórz kontekst odpowiedniego typu, odśwież go (utwórz beany), uruchom
 *   runnery, ogłoś gotowość. Każdy krok możesz dostroić (obiekt SpringApplication albo SpringApplicationBuilder)
 *   i podejrzeć (zdarzenia cyklu życia). Runnery wykonują zadania przy starcie, a kod wyjścia mówi skryptom,
 *   czy zadanie się udało.
 *
 * ANALOGIA: otwarcie sklepu rano.
 *   Kierownik (SpringApplication) sprawdza grafik i ustawienia dnia (środowisko), zapala neon nad wejściem (baner),
 *   otwiera zaplecze i ustawia pracowników na stanowiskach (kontekst i beany), wykłada towar z dostawy (runnery)
 *   i dopiero wtedy odwraca tabliczkę na „OTWARTE” (ApplicationReadyEvent). Kto czeka na klientów przy drzwiach
 *   (słuchacz z zewnątrz), widzi każdy krok; pracownik (bean) widzi tylko to, co działo się, gdy już był w pracy.
 *
 * JAK TO DZIAŁA:
 *   SpringApplication.run krok po kroku (kolejność sprawdzona testem sekcji 6):
 *   1. ApplicationStartingEvent            — start; nie ma jeszcze ustawień ani kontekstu
 *   2. ApplicationEnvironmentPreparedEvent  — środowisko gotowe: argumenty, pliki application*.properties, profile
 *   3. baner                                — napis „Spring” (o ile nie wyłączono)
 *   4. ApplicationContextInitializedEvent   — kontekst utworzony (typ zależy od WebApplicationType)
 *   5. ApplicationPreparedEvent             — definicje beanów wczytane, beanów jeszcze nie ma
 *   6. odświeżenie (refresh) → ContextRefreshedEvent — beany utworzone i powiązane
 *   7. log „Started … in … seconds” → ApplicationStartedEvent (+ AvailabilityChangeEvent: LivenessState.CORRECT)
 *   8. runnery (ApplicationRunner, CommandLineRunner) w kolejności @Order
 *   9. ApplicationReadyEvent (+ AvailabilityChangeEvent: ReadinessState.ACCEPTING_TRAFFIC) — aplikacja GOTOWA
 *   Błąd po drodze → ApplicationFailedEvent, kontekst jest zamykany, a run() rzuca wyjątek dalej.
 *
 * SŁÓWKA:
 *   starting = startujący; environment prepared = środowisko przygotowane; context initialized = kontekst
 *   zainicjowany; prepared = przygotowany; refreshed = odświeżony; started = wystartowany; ready = gotowy;
 *   failed = nieudany; availability = dostępność; liveness = „żywotność” (czy proces działa); readiness = gotowość
 *   (czy przyjmuje ruch); option = opcja (--klucz=wartość); non-option args = argumenty bez opcji;
 *   lazy initialization = leniwa inicjalizacja; builder = budowniczy; banner mode = tryb banera.
 *
 * ZOBACZ TEŻ: s00_start/Start03FirstApplication (pierwsza aplikacja i main),
 *             s01_core_ioc/Ioc07Lifecycle (cykl życia pojedynczego beana),
 *             s02_configuration/Cfg01Properties (priorytet źródeł ustawień — argumenty, pliki),
 *             s03_boot_basics/Boot01AutoConfiguration (co Boot konfiguruje w kroku 6),
 *             s17_events (własne zdarzenia aplikacji),
 *             t30_build_modules/Build04CommandLineApps (argumenty i kody wyjścia w czystej Javie),
 *             t22_design_patterns/Patterns02Builder (wzorzec budowniczego).
 * </pre>
 */
public final class Boot03SpringApplication {

    private Boot03SpringApplication() {
    }

    // =================================================================================================
    // 1. SpringApplication.run — TYP APLIKACJI I KOLEJNOŚĆ KROKÓW
    // =================================================================================================
    //
    //   SpringApplication.run(Boot03Application.class, args)        ← skrót
    //   new SpringApplication(Boot03Application.class).run(args)    ← to samo, ale możesz coś ustawić przed run
    //
    // Typ aplikacji (WebApplicationType) Boot wybiera sam przy tworzeniu SpringApplication, na podstawie ścieżki klas:
    //   SERVLET   — są klasy serwletów (np. starter spring-boot-starter-webmvc z Tomcatem) → serwer WWW;
    //   REACTIVE  — jest WebFlux (spring-boot-starter-webflux) → serwer reaktywny;
    //   NONE      — ani jednego, ani drugiego → kontekst bez serwera: aplikacja robi swoje i się kończy.
    // W tym module jest NONE, a kontekst to AnnotationConfigApplicationContext (test sekcji 1). Wymusisz typ przez
    // setWebApplicationType(WebApplicationType.NONE) albo spring.main.web-application-type=none (np. żeby uruchomić
    // aplikację webową jako narzędzie wsadowe).
    // W Boot 4 typ wybiera publiczna metoda WebApplicationType.deduce(): najpierw pyta moduły (np. spring-boot-webmvc
    // dostarcza przez spring.factories własny „zgadywacz” typu), a dopiero potem sprawdza klasy serwletów.
    //
    // Test sekcji 1 pokazuje też, że log „Started Boot03Application in … seconds” pojawia się PRZED pracą runnerów
    // („Wczytano produktów: 3”). „Started” nie znaczy „gotowa” — gotowość ogłasza dopiero ApplicationReadyEvent
    // (sekcja 6). Nazwę w tym logu Boot bierze z klasy, której metoda main uruchomiła JVM; w testach trzeba ją
    // ustawić jawnie (setMainApplicationClass), bo main należy do narzędzia testowego.
    //
    // PYTANIE REKRUTACYJNE: co robi SpringApplication.run? (odpowiedź: kroki 1–9 z nagłówka lekcji).

    // =================================================================================================
    // 2. DOSTOSOWANIE STARTU — SpringApplication i SpringApplicationBuilder
    // =================================================================================================
    //
    //   SpringApplication app = new SpringApplication(Boot03Application.class);
    //   app.setBannerMode(Banner.Mode.OFF);               // baner: CONSOLE (konsola), LOG (do logu), OFF (wyłączony)
    //   app.setWebApplicationType(WebApplicationType.NONE);
    //   app.setAdditionalProfiles("boot03-extra");        // profile DOKŁADANE do spring.profiles.active
    //   app.setDefaultProperties(Map.of("klucz", "wartość"));   // ustawienia o NAJNIŻSZYM priorytecie
    //   app.setLazyInitialization(true);                  // beany powstają przy pierwszym użyciu, nie przy starcie
    //   app.addListeners(słuchacz);                       // słuchacz zdarzeń istniejący PRZED kontekstem (sekcja 6)
    //   app.run(args);
    //
    // To samo płynnym API (fluent API — wywołania łańcuchem):
    //   new SpringApplicationBuilder(Boot03Application.class)
    //           .web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF)
    //           .profiles("boot03-extra").properties("klucz=wartość").lazyInitialization(true)
    //           .run(args);
    // Uwaga: properties(...) w builderze to te same ustawienia DOMYŚLNE co setDefaultProperties — przegrywają z plikiem
    // application.properties i z argumentami (test sekcji 2: argument wygrywa; z plikiem — sprawdzone uruchomieniem).
    // Dodatkowe profile są aktywne RAZEM z tymi z spring.profiles.active (najpierw dodatkowe, potem z ustawień).
    //
    // Test sekcji 2: baner zawiera „:: Spring Boot ::” i wersję „(v4.1.1)”; z leniwą inicjalizacją
    // MonthlyReportGenerator powstaje dopiero przy getBean, a bez niej — przy starcie.
    //
    // PUŁAPKA: leniwa inicjalizacja „przyspiesza start”, ale przesuwa błędy w czasie.
    //   Dlaczego to groźne? Bean z błędem konfiguracji wysypie się dopiero przy pierwszym użyciu — np. przy pierwszym
    //   zamówieniu klienta w nocy — zamiast od razu przy starcie, gdy wdrożenie można jeszcze cofnąć.

    // =================================================================================================
    // 3. USTAWIENIA spring.main.* — i PUŁAPKA: plik wygrywa z kodem
    // =================================================================================================
    //
    // Większość setterów ma odpowiednik w ustawieniach (nazwy z metadanych spring-boot-4.1.1.jar):
    //   spring.main.banner-mode=off              spring.main.web-application-type=none
    //   spring.main.lazy-initialization=true     spring.main.log-startup-info=false
    //   spring.main.allow-bean-definition-overriding=false   spring.main.allow-circular-references=false
    //
    // PUŁAPKA: w kodzie setBannerMode(Banner.Mode.CONSOLE), a w pliku albo argumencie spring.main.banner-mode=off —
    //   baneru NIE ma (test sekcji 3).
    //   Dlaczego? Po przygotowaniu środowiska Boot „wiąże” ustawienia spring.main.* z obiektem SpringApplication
    //   i nadpisuje nimi to, co ustawiłeś w kodzie. Ustawienie zewnętrzne ma ostatnie słowo — to celowe: administrator
    //   może zmienić zachowanie bez kompilacji. Gdy „setter nie działa”, poszukaj spring.main.* w plikach i argumentach.

    // =================================================================================================
    // 4. RUNNERY — ApplicationRunner i CommandLineRunner, kolejność @Order
    // =================================================================================================
    //
    //   ApplicationRunner:  void run(ApplicationArguments args)   — argumenty rozłożone na części
    //   CommandLineRunner:  void run(String... args)              — surowa tablica, jak w main
    //
    // Dla argumentów --limit=2 plik.csv (test sekcji 4):
    //   args.getOptionNames()        → [limit]          (opcje: to, co zaczyna się od --)
    //   args.getOptionValues("limit")→ [2]              (lista! --limit=1 --limit=2 da [1, 2]; --flaga bez
    //                                                    wartości da []; brak opcji → null)
    //   args.getNonOptionArgs()      → [plik.csv]       (reszta)
    //   args.getSourceArgs()         → [--limit=2, plik.csv]   (to samo, co dostaje CommandLineRunner)
    // Dodatkowo każda opcja --klucz=wartość trafia do Environment jako ustawienie (s02_configuration/Cfg01Properties).
    //
    // Oba rodzaje runnerów Boot zbiera do JEDNEJ listy i sortuje według @Order (mniejsza liczba = wcześniej):
    // CatalogLoaderRunner (@Order(1)) wczytuje katalog, StartupReportRunner (@Order(2)) widzi już wynik.
    // Zastosowania: wczytanie danych startowych, rozgrzanie pamięci podręcznej, sprawdzenie połączeń, zadanie wsadowe.
    //
    // DOBRA PRAKTYKA: wybieraj ApplicationRunner, gdy potrzebujesz opcji (--klucz=wartość). Dlaczego? Nie piszesz
    //   własnego rozbioru tekstu (split na „=”), który łatwo zepsuć.
    //
    // PUŁAPKA: długie zadanie w runnerze (np. 5 minut importu) opóźnia gotowość aplikacji.
    //   Dlaczego? Runnery działają w wątku startu, PRZED ApplicationReadyEvent (sekcja 6). W aplikacji webowej
    //   serwer już nasłuchuje, ale stan gotowości (readiness — sprawdzany np. przez Kubernetes, dział
    //   s15_actuator_logging) zmieni się dopiero po runnerach. Długie zadania uruchamiaj w tle (dział
    //   s16_cache_scheduling_async).

    // =================================================================================================
    // 5. WYJĄTEK W RUNNERZE ZATRZYMUJE START
    // =================================================================================================
    //
    // Argument --limit=abc → Integer.parseInt rzuca NumberFormatException w CatalogLoaderRunner. Co się dzieje
    // (test sekcji 5):
    //   • Boot loguje „Application run failed” (poziom ERROR) ze stosem wywołań, publikuje ApplicationFailedEvent
    //     i zamyka kontekst (ContextClosedEvent); ApplicationReadyEvent NIE nadchodzi;
    //   • run() rzuca dalej TEN SAM wyjątek: NumberFormatException: For input string: "abc".
    //   • wyjątek KONTROLOWANY (np. IOException) Boot opakowuje:
    //       java.lang.IllegalStateException: Failed to execute ApplicationRunner
    //       Caused by: java.io.IOException: Brak pliku z cennikiem
    //   Bloku „APPLICATION FAILED TO START” tu nie ma — ten blok wypisują analizatory błędów (FailureAnalyzer) tylko
    //   dla znanych im wyjątków (np. brak beana); NumberFormatException do nich nie należy.
    //
    // DOBRA PRAKTYKA: sprawdzaj argumenty i dane wejściowe na początku runnera i rzucaj wyjątek z czytelnym
    //   komunikatem („--limit musi być liczbą, podano: abc”). Dlaczego? Zatrzymanie startu jest dobre (lepiej nie
    //   wystartować niż działać na złych danych), ale komunikat ma od razu mówić, co poprawić.

    // =================================================================================================
    // 6. ZDARZENIA CYKLU ŻYCIA APLIKACJI — i kto je widzi
    // =================================================================================================
    //
    // Test sekcji 6 zapisuje nazwy zdarzeń przez słuchacza dodanego do SpringApplication (addListeners):
    //   ApplicationStartingEvent → ApplicationEnvironmentPreparedEvent → ApplicationContextInitializedEvent →
    //   ApplicationPreparedEvent → ContextRefreshedEvent → ApplicationStartedEvent → AvailabilityChangeEvent →
    //   [runnery] → ApplicationReadyEvent → AvailabilityChangeEvent;  przy zamknięciu: ContextClosedEvent.
    // AvailabilityChangeEvent (zmiana dostępności) Boot wysyła dwa razy: po starcie (proces żyje — liveness) i po
    // runnerach (przyjmuje ruch — readiness). Korzysta z tego Actuator (dział s15_actuator_logging).
    //
    // PUŁAPKA: bean z @EventListener (StartupJournal) NIE zobaczy czterech pierwszych zdarzeń.
    //   Dlaczego? Słuchaczem-beanem staje się dopiero, gdy kontekst go utworzy (krok 6). Zdarzenia sprzed odświeżenia
    //   kontekstu odbiorą tylko słuchacze zarejestrowane w SpringApplication: addListeners(...) albo wpis
    //   org.springframework.context.ApplicationListener=… w META-INF/spring.factories. Dziennik StartupJournal
    //   z testu sekcji 6 zaczyna się więc od „utworzono MonthlyReportGenerator” i „zdarzenie: ContextRefreshedEvent”,
    //   a wpisy runnerów leżą między ApplicationStartedEvent a ApplicationReadyEvent.
    //
    // DOBRA PRAKTYKA: kod „po starcie” (np. powiadomienie, że sklep działa) wieszaj na ApplicationReadyEvent,
    //   a nie na ContextRefreshedEvent. Dlaczego? ContextRefreshedEvent przychodzi przed runnerami i może pojawić się
    //   więcej niż raz (np. w kontekstach potomnych); ApplicationReadyEvent oznacza „wszystko gotowe”.

    // =================================================================================================
    // 7. KOD WYJŚCIA — SpringApplication.exit i ExitCodeGenerator
    // =================================================================================================
    //
    //   ConfigurableApplicationContext context = application.run(args);
    //   System.exit(SpringApplication.exit(context));      // wzorzec aplikacji wsadowej (Boot03Application.main)
    //
    // SpringApplication.exit(context) zamyka kontekst i zwraca kod: pyta wszystkie beany ExitCodeGenerator (oraz
    // generatory podane w wywołaniu) i bierze PIERWSZY niezerowy kod w kolejności @Order (sprawdzone uruchomieniem:
    // beany z kodami 0, 3, 5 i @Order 1, 2, 3 dają 3); same zera → 0.
    // EmptyCatalogExitCode zwraca 3, gdy katalog jest pusty (test sekcji 7: --limit=0 → 3, bez argumentów → 0).
    //
    // PUŁAPKA: System.exit(...) w teście albo w aplikacji webowej.
    //   Dlaczego? System.exit kończy CAŁĄ maszynę wirtualną — w teście zabija proces Mavena/IntelliJ, w aplikacji
    //   webowej wyłącza serwer bez łagodnego zamykania. Testy wywołują tylko SpringApplication.exit(context)
    //   i sprawdzają zwrócony kod.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • run(): StartingEvent → EnvironmentPrepared → baner → ContextInitialized → Prepared → refresh
     *     (ContextRefreshed) → „Started … in …” → StartedEvent → runnery → ReadyEvent. Błąd → FailedEvent.
     *   • WebApplicationType z ścieżki klas: SERVLET / REACTIVE / NONE (WebApplicationType.deduce() w Boot 4).
     *   • SpringApplication: setBannerMode, setWebApplicationType, setAdditionalProfiles, setDefaultProperties,
     *     setLazyInitialization, addListeners; to samo w SpringApplicationBuilder (.properties = domyślne).
     *   • spring.main.* (plik/argument) nadpisuje settery z kodu.
     *   • ApplicationRunner(ApplicationArguments: getOptionNames, getOptionValues → lista albo null,
     *     getNonOptionArgs, getSourceArgs) vs CommandLineRunner(String...). Jedna lista, kolejność @Order.
     *   • Wyjątek w runnerze → start nieudany; niekontrolowany leci bez zmian, kontrolowany w
     *     IllegalStateException("Failed to execute ApplicationRunner").
     *   • Zdarzenia sprzed odświeżenia kontekstu widzi tylko słuchacz z addListeners / spring.factories.
     *   • System.exit(SpringApplication.exit(context)) — pierwszy niezerowy kod z ExitCodeGenerator (wg @Order).
     *
     * PYTANIA KONTROLNE:
     *   1. Jak Boot decyduje, czy uruchomić serwer WWW? Jaki typ aplikacji jest w module s03-boot-basics i dlaczego?
     *   2. Czym różni się ApplicationRunner od CommandLineRunner? Co zwróci args.getOptionValues("limit"),
     *      gdy uruchomisz aplikację bez --limit?
     *   3. Co się stanie? Uruchamiasz Boot03Application z argumentem --limit=abc.
     *   4. ZNAJDŹ BŁĄD: w kodzie jest app.setBannerMode(Banner.Mode.CONSOLE), a baneru nie widać. W application.properties
     *      jest linia spring.main.banner-mode=off. Co wygrywa i dlaczego?
     *   5. Co się stanie? Bean z @EventListener(ApplicationStartingEvent.class) ma zapisać czas startu aplikacji.
     *   6. Log mówi „Started Boot03Application in 1.2 seconds”. Czy runnery już skończyły pracę? Które zdarzenie
     *      mówi „aplikacja gotowa”?
     *   7. Jaki kod wyjścia zwróci SpringApplication.exit(context) dla Boot03Application uruchomionej z --limit=0?
     *   8. Dlaczego leniwa inicjalizacja może przenieść błąd ze startu aplikacji na „środek nocy”?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Boot03SpringApplicationExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): runner liczący produkty. Po {@code run(args)} pole {@code count} ma zawierać:
     * wartość opcji {@code --limit}, ale nie więcej niż liczba produktów w katalogu
     * ({@code ProductCatalog.ALL_PRODUCTS.size()}, czyli 14); bez opcji {@code --limit} — 14.
     * Argumenty bez opcji (np. {@code plik.csv}) nie mają znaczenia.
     * Podpowiedź: {@code args.containsOption("limit")}, {@code args.getOptionValues("limit").getFirst()},
     * {@code Integer.parseInt}, {@code Math.min}.
     */
    static class ProductCountRunner implements ApplicationRunner {

        private int count;

        @Override
        public void run(ApplicationArguments args) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        int count() {
            return count;
        }
    }

    /**
     * ĆWICZENIE 2 (średnie): słuchacz, który zapisuje proste nazwy klas WSZYSTKICH zdarzeń
     * (np. {@code "ApplicationStartingEvent"}) w kolejności nadejścia. Test doda go przez
     * {@code SpringApplication.addListeners(...)} i sprawdzi, że widzi też zdarzenia sprzed utworzenia kontekstu.
     * Podpowiedź: {@code event.getClass().getSimpleName()}; lista bezpieczna dla wątków:
     * {@code Collections.synchronizedList(new ArrayList<>())}; {@code names()} ma zwracać kopię ({@code List.copyOf}).
     */
    static class EventNamesRecorder implements ApplicationListener<ApplicationEvent> {

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        List<String> names() {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): raport importu cennika jako {@link ExitCodeGenerator}. Kod wyjścia:
     * <ul>
     *   <li>0 — wszystko zaimportowane ({@code imported > 0} i {@code rejected == 0});</li>
     *   <li>1 — częściowy sukces ({@code imported > 0} i {@code rejected > 0});</li>
     *   <li>2 — nic nie zaimportowano ({@code imported == 0}), bez względu na {@code rejected}.</li>
     * </ul>
     * Test sprawdzi metodę bezpośrednio i przez {@code SpringApplication.exit(context)} z tym beanem w kontekście.
     * Podpowiedź: zacznij od najbardziej „złego” przypadku ({@code imported == 0}).
     *
     * @param imported ile wierszy zaimportowano
     * @param rejected ile wierszy odrzucono
     */
    record ImportReport(int imported, int rejected) implements ExitCodeGenerator {

        @Override
        public int getExitCode() {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Po klasach na ścieżce klas (WebApplicationType.deduce()): serwlety → SERVLET, WebFlux → REACTIVE, inaczej
     *      NONE. W s03-boot-basics jest NONE — moduł nie ma startera webowego. Sprawdzone testem (sekcja 1).
     *   2. ApplicationRunner dostaje ApplicationArguments (opcje i argumenty bez opcji osobno), CommandLineRunner —
     *      surową tablicę String[]. Bez --limit getOptionValues("limit") zwraca null (nie pustą listę!).
     *      Sprawdzone testem (sekcja 4) i w CatalogLoaderRunner.
     *   3. Aplikacja nie wystartuje: CatalogLoaderRunner rzuca NumberFormatException („For input string: "abc"”),
     *      Boot loguje „Application run failed”, publikuje ApplicationFailedEvent, zamyka kontekst, a run() rzuca ten
     *      sam wyjątek dalej. Sprawdzone testem (sekcja 5).
     *   4. Wygrywa plik: Boot wiąże spring.main.* z obiektem SpringApplication po przygotowaniu środowiska i nadpisuje
     *      wartości z setterów. Sprawdzone testem (sekcja 3, z argumentem; z plikiem — sprawdzone uruchomieniem).
     *   5. Metoda nigdy się nie wywoła: ApplicationStartingEvent jest publikowany, zanim powstanie kontekst i bean.
     *      Słuchacza trzeba dodać przez SpringApplication.addListeners(...) (albo spring.factories). Sprawdzone testem
     *      (sekcja 6: słuchacz z addListeners widzi to zdarzenie, StartupJournal — nie).
     *   6. Nie — „Started …” jest logowane PRZED runnerami (test sekcji 1). Gotowość ogłasza ApplicationReadyEvent,
     *      publikowany po runnerach (test sekcji 6).
     *   7. 3 — EmptyCatalogExitCode zwraca 3 dla pustego katalogu, a to jedyny niezerowy kod. Sprawdzone testem
     *      (sekcja 7).
     *   8. Przy leniwej inicjalizacji bean powstaje przy pierwszym użyciu. Jeśli ma błąd (np. złe ustawienie),
     *      wyjątek pojawi się dopiero wtedy — np. przy pierwszym nocnym zamówieniu — a nie przy starcie, gdy wdrożenie
     *      łatwo cofnąć.
     */
    // </editor-fold>
}

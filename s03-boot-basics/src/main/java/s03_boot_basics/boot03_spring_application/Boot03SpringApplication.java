package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.ApplicationArguments;

/**
 * <pre>
 * TEMAT: SpringApplication — co dzieje się przy starcie i jak tym sterować
 *        (SpringApplication = klasa uruchamiająca aplikację Boota; runner = uruchamiacz, kod wykonywany po starcie;
 *         exit code = kod wyjścia procesu; event = zdarzenie; DevTools = narzędzia programisty)
 *
 * W SKRÓCIE:
 *   {@code SpringApplication.run(Klasa.class, args)} to skrót. Pod spodem jest obiekt SpringApplication, który można
 *   najpierw skonfigurować: typ aplikacji (z serwerem WWW czy bez), banner, dodatkowe profile, właściwości domyślne.
 *   Po zbudowaniu kontekstu Boot wywołuje RUNNERY — beany CommandLineRunner i ApplicationRunner — czyli miejsce na
 *   „zrób coś raz po starcie”. Aplikacja może też zwrócić systemowi KOD WYJŚCIA (ExitCodeGenerator), a o każdym etapie
 *   startu informuje ZDARZENIAMI (ApplicationStartedEvent, ApplicationReadyEvent...).
 *
 * ANALOGIA: otwarcie sklepu rano.
 *   Kierownik (SpringApplication) ma listę ustawień: czy otwieramy kasę internetową (typ aplikacji), czy wieszamy
 *   baner (banner), jaki dziś grafik (profil). Potem ekipa rozkłada towar (tworzenie beanów), a na koniec kilku
 *   pracowników ma zadania „raz po otwarciu” w ustalonej kolejności (runnery z {@code @Order}). Dopiero potem
 *   kierownik ogłasza „otwarte!” (ApplicationReadyEvent). Wieczorem raport: wszystko OK (0) czy problem (np. 3).
 *
 * JAK TO DZIAŁA:
 *   SpringApplication.run(...) w skrócie:
 *   1. ApplicationStartingEvent                    — nic jeszcze nie ma
 *   2. przygotowanie Environment (środowiska): argumenty, application.properties, profile → ApplicationEnvironmentPreparedEvent
 *   3. banner, utworzenie ApplicationContext (typ zależy od WebApplicationType) → ApplicationContextInitializedEvent,
 *      wczytanie źródeł (klasy startowej) → ApplicationPreparedEvent
 *   4. refresh (odświeżenie): skanowanie, autokonfiguracja, tworzenie beanów → ContextRefreshedEvent
 *   5. ApplicationStartedEvent                     — kontekst gotowy
 *   6. RUNNERY: wszystkie ApplicationRunner i CommandLineRunner, posortowane razem według {@code @Order}
 *   7. ApplicationReadyEvent                       — aplikacja gotowa (albo ApplicationFailedEvent przy błędzie)
 *
 * SŁÓWKA:
 *   web application type = typ aplikacji webowej (NONE — bez weba, SERVLET — Spring MVC + Tomcat, REACTIVE — WebFlux);
 *   banner mode = tryb bannera (OFF — wyłączony, CONSOLE — konsola, LOG — log); additional profiles = dodatkowe profile;
 *   default properties = właściwości domyślne; option argument = argument-opcja ({@code --klucz=wartość});
 *   non-option argument = zwykły argument (słowo bez {@code --}); order = kolejność; exit = wyjście;
 *   builder = budowniczy (SpringApplicationBuilder); restart = ponowne uruchomienie; live reload = przeładowanie na żywo.
 *
 * ZOBACZ TEŻ: s00_start/Start03FirstApplication (pierwsze uruchomienie), s02_configuration/Cfg03Profiles (profile),
 *             s02_configuration/Cfg05Environment (Environment i kolejność źródeł właściwości),
 *             s01_core_ioc/Ioc07Lifecycle (cykl życia beana), t30_build_modules/Build04CommandLineApps (argumenty main),
 *             t30_build_modules/Build05ProcessesEnv (kod wyjścia procesu).
 * </pre>
 */
public final class Boot03SpringApplication {

    private Boot03SpringApplication() {
    }

    // =================================================================================================
    // 1. KONFIGURACJA SpringApplication — Boot03Application.createApplication()
    // =================================================================================================
    //
    //   SpringApplication app = new SpringApplication(Boot03Application.class);
    //   app.setWebApplicationType(WebApplicationType.NONE);     ← bez serwera WWW
    //   app.setBannerMode(Banner.Mode.OFF);                     ← bez bannera
    //   app.setAdditionalProfiles("boot03");                    ← profil zawsze aktywny (+ te z spring.profiles.active)
    //   app.setDefaultProperties(Map.of("boot03.shop-name", "Sklep Domyślny"));   ← wartości „na wszelki wypadek”
    //   ConfigurableApplicationContext context = app.run(args);
    //
    // To samo płynnym stylem: new SpringApplicationBuilder(Boot03Application.class).web(WebApplicationType.NONE)
    //   .bannerMode(Banner.Mode.OFF).profiles("boot03").properties("klucz=wartość").run(args).
    //
    // PUŁAPKA: drugie run(...) na TYM SAMYM SpringApplicationBuilder nie uruchamia nowej aplikacji — zwraca kontekst
    //   z pierwszego uruchomienia (nawet już zamknięty). Dlaczego to groźne? Test „uruchom dwa razy z różnymi
    //   argumentami” po cichu sprawdza starą aplikację. Każde uruchomienie = nowy builder (tak robią testy ćwiczenia 3).
    //
    // WebApplicationType: Boot sam go WYKRYWA z classpath (jest Spring MVC → SERVLET; nic webowego → NONE).
    //   W tym module wykryje NONE (test sprawdza getWebApplicationType()). Ustawienie NONE ręcznie przydaje się w module
    //   webowym, gdy chcesz uruchomić narzędzie wiersza poleceń albo zadanie wsadowe BEZ startowania Tomcata.
    // Default properties: NAJNIŻSZY priorytet ze wszystkich źródeł — przegrywają z application.properties, zmiennymi
    //   środowiskowymi i argumentami {@code --klucz=wartość} (test: argument wygrywa).
    //
    // DOBRA PRAKTYKA: właściwości domyślne trzymaj w application.properties, a setDefaultProperties zostaw na
    //   wartości, które MUSZĄ istnieć zawsze (np. w narzędziu rozprowadzanym jako jeden jar). Dlaczego? Ustawienia
    //   w pliku widać od razu; ukryte w kodzie Java łatwo przeoczyć przy szukaniu „skąd ta wartość?”.

    // =================================================================================================
    // 2. RUNNERY — CommandLineRunner kontra ApplicationRunner, kolejność {@code @Order}
    // =================================================================================================
    //
    // Runner to bean z jedną metodą run, wywołaną RAZ po zbudowaniu kontekstu, przed ApplicationReadyEvent.
    //   CommandLineRunner.run(String... args)            ← surowe argumenty, jak w main
    //   ApplicationRunner.run(ApplicationArguments args) ← argumenty przetworzone:
    //       args.getOptionNames()          {"import.file"}          z --import.file=ceny.csv
    //       args.getOptionValues("import.file")  ["ceny.csv"]       (lista! opcję można podać kilka razy; brak → null)
    //       args.getNonOptionArgs()        ["pilne"]                słowa bez --
    //       args.getSourceArgs()           surowe argumenty
    // Oba typy są sortowane RAZEM według {@code @Order} (mniejsza liczba = wcześniej). W lekcji: ImportOptionsRunner
    // ({@code @Order(1)}, ApplicationRunner) → StockReportRunner ({@code @Order(2)}, CommandLineRunner).
    //
    // Opcje {@code --klucz=wartość} trafiają też do Environment jako właściwości o bardzo wysokim priorytecie (wyższym niż
    // application.properties i zmienne środowiskowe) — dlatego
    // {@code --boot03.shop-name=...} nadpisuje właściwość, a env.getProperty("import.file") zwraca „ceny.csv”.
    //
    // PUŁAPKA: getOptionValues zwraca null, gdy opcji nie ma — a nie pustą listę. Dlaczego to groźne?
    //   {@code args.getOptionValues("import.file").getFirst()} bez sprawdzenia containsOption kończy się
    //   NullPointerException, gdy ktoś uruchomi aplikację bez tej opcji (sprawdzone testem sekcji 2).
    // PUŁAPKA: runner wykonuje się w głównym wątku startu. Długa praca w runnerze (np. import godzinę) opóźnia
    //   ApplicationReadyEvent, a wyjątek w runnerze PRZERYWA start: wyjątek kontrolowany (np. IOException) wylatuje
    //   z run(...) opakowany w IllegalStateException „Failed to execute CommandLineRunner”, a RuntimeException —
    //   bez opakowania (oba przypadki sprawdzone testem sekcji 4). Dlaczego to ważne? W aplikacji webowej sonda
    //   „gotowości” (readiness) czeka na ApplicationReadyEvent — długi runner = aplikacja długo „niegotowa”.
    //
    // DOBRA PRAKTYKA: wolisz ApplicationRunner, gdy czytasz opcje. Dlaczego? Nie parsujesz ręcznie stringów
    //   „--klucz=wartość” i nie mylisz opcji ze zwykłymi słowami.
    //
    // PYTANIE REKRUTACYJNE: czym różni się CommandLineRunner od ApplicationRunner? (odpowiedź: typ argumentów — surowe
    //   String... kontra przetworzone ApplicationArguments; poza tym działają tak samo i są sortowane razem).

    // =================================================================================================
    // 3. KOD WYJŚCIA — ExitCodeGenerator i SpringApplication.exit
    // =================================================================================================
    //
    //   int code = SpringApplication.exit(context);   ← pyta beany ExitCodeGenerator, ZAMYKA kontekst, zwraca kod
    //   System.exit(code);                            ← przekazuje kod systemowi operacyjnemu (tylko w main!)
    // Kod 0 = sukces, inny = błąd. Przy kilku generatorach wygrywa pierwszy niezerowy (w kolejności {@code @Order}).
    // ShopExitCode zwraca wartość właściwości boot03.exit-code (domyślnie 0). Skrypt albo harmonogram zadań (cron,
    // Harmonogram zadań Windows) sprawdza ten kod i np. wysyła alarm.
    //
    // PUŁAPKA: System.exit w teście albo w kodzie biblioteki. Dlaczego źle? Zabija CAŁĄ maszynę wirtualną Javy —
    //   także proces testów Mavena. W testach wołaj tylko SpringApplication.exit(context) i sprawdzaj wynik.

    // =================================================================================================
    // 4. ZDARZENIA STARTU — gdzie są runnery
    // =================================================================================================
    //
    // StartupEventsListener zapisuje do RunLog trzy zdarzenia. Test sekcji 4 sprawdza pełną kolejność:
    //   ContextRefreshedEvent → ApplicationStartedEvent → ApplicationRunner → CommandLineRunner → ApplicationReadyEvent
    // ApplicationReadyEvent to dobre miejsce na „aplikacja gotowa — zacznij coś robić w tle”. Zdarzeń sprzed powstania
    // kontekstu (ApplicationStartingEvent, ApplicationEnvironmentPreparedEvent) bean z {@code @EventListener} nie
    // zobaczy — wtedy beanów jeszcze nie ma; nasłuchiwacz dodaje się przez app.addListeners(...).
    //
    // DOBRA PRAKTYKA: kod „po starcie” umieszczaj w runnerze albo w {@code @EventListener(ApplicationReadyEvent.class)},
    //   a nie w konstruktorze czy {@code @PostConstruct}. Dlaczego? W konstruktorze inne beany mogą nie być jeszcze
    //   gotowe, a wyjątek tam wygląda jak błąd tworzenia beana, nie jak błąd zadania.

    // =================================================================================================
    // 5. DEVTOOLS — szybsza praca podczas programowania (tylko teoria, w kursie go nie dodajemy)
    // =================================================================================================
    //
    // spring-boot-devtools to zależność dla programisty:
    //   • automatyczny restart: po zmianie skompilowanej klasy aplikacja sama się restartuje. Szybciej niż ręcznie,
    //     bo DevTools używa DWÓCH ładowaczy klas: „bazowy” (biblioteki z jarów — zostaje) i „restartowy” (Twoje klasy
    //     — wyrzucany i tworzony na nowo). W IntelliJ zmiana zapisuje się po Build → Build Project (Ctrl+F9).
    //   • LiveReload (przeładowanie na żywo): serwer na porcie 35729 każe przeglądarce odświeżyć stronę po zmianie.
    //   • ustawienia „deweloperskie”, np. wyłączona pamięć podręczna szablonów stron.
    // Jak się go dodaje (Maven): {@code <artifactId>spring-boot-devtools</artifactId> <optional>true</optional>}
    //   (w Gradle: konfiguracja developmentOnly). optional = „nie przekazuj dalej” — moduł, który zależy od Twojego
    //   projektu, nie dostanie DevTools „w spadku”.
    // Kiedy DevTools się WYŁĄCZA sam:
    //   • w testach — sprawdza stos wywołań: jeśli start idzie z JUnit (org.junit.platform...) albo z narzędzi testowych
    //     Boota, restart jest wyłączony (sprawdzone w klasie DevToolsEnablementDeducer jara 4.1.1);
    //   • w spakowanym jarze — spring-boot-maven-plugin domyślnie POMIJA DevTools przy pakowaniu (parametr
    //     excludeDevtools, domyślnie true), a uruchomienie przez java -jar nie jest traktowane jako tryb programisty.
    // Test sekcji 5 sprawdza, że w tym module klasy Restarter (serca restartu) po prostu nie ma.
    //
    // PUŁAPKA: DevTools na produkcji (np. przez wyłączenie excludeDevtools). Dlaczego groźne? Wyłącza pamięci
    //   podręczne, zmienia ustawienia logowania i — w trybie zdalnym — otwiera dodatkowy punkt dostępu do aplikacji.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • new SpringApplication(Klasa.class) + setWebApplicationType / setBannerMode / setAdditionalProfiles /
     *     setDefaultProperties, potem run(args). Styl płynny: SpringApplicationBuilder.
     *   • Default properties = najniższy priorytet; argument --klucz=wartość = najwyższy.
     *   • CommandLineRunner (String...) i ApplicationRunner (ApplicationArguments) — raz po starcie, razem według {@code @Order}.
     *   • getOptionValues → lista albo null; getNonOptionArgs → słowa bez --.
     *   • Wyjątek w runnerze = start przerwany (checked → IllegalStateException „Failed to execute ...Runner”,
     *     RuntimeException → ten sam wyjątek); ApplicationFailedEvent zamiast ApplicationReadyEvent.
     *   • SpringApplication.exit(context) → kod z ExitCodeGenerator + zamknięcie kontekstu; System.exit tylko w main.
     *   • Kolejność: ContextRefreshed → ApplicationStarted → runnery → ApplicationReady.
     *   • DevTools: optional/developmentOnly, restart + LiveReload, sam się wyłącza w testach i w spakowanym jarze.
     *
     * PYTANIA KONTROLNE:
     *   1. PYTANIE REKRUTACYJNE: czym różni się CommandLineRunner od ApplicationRunner i w jakiej kolejności się wykonują?
     *   2. Co się stanie? Boot03Application ma właściwość domyślną boot03.shop-name=Sklep Domyślny, a uruchomisz ją
     *      z argumentem --boot03.shop-name=Sklep z Linii Poleceń. Jaką wartość zobaczy aplikacja?
     *   3. Co się stanie? Wywołasz SpringApplication.exit(context) dla aplikacji uruchomionej z --boot03.exit-code=3.
     *      Jaki kod dostaniesz i w jakim stanie będzie kontekst?
     *   4. Co się stanie? Jeden z CommandLineRunnerów rzuci wyjątek. Czy aplikacja wystartuje? Czy będzie ApplicationReadyEvent?
     *   5. ZNAJDŹ BŁĄD: {@code String file = args.getOptionValues("import.file").getFirst();} w ApplicationRunnerze —
     *      aplikacja uruchomiona bez opcji --import.file pada. Dlaczego i jak to naprawić?
     *   6. Dlaczego DevTools dodaje się jako optional (Maven) / developmentOnly (Gradle) i dlaczego nie działa w testach?
     *   7. Gdzie umieścisz kod, który ma się wykonać raz, zaraz po starcie aplikacji? Podaj dwa sposoby.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Boot03SpringApplicationExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć wartość opcji {@code --import.file}, a gdy jej nie ma — tekst „brak”.
     * Przykład: argumenty {@code --import.file=ceny.csv pilne} → „ceny.csv”; argumenty {@code pilne} → „brak”.
     * Podpowiedź: args.containsOption(...), args.getOptionValues(...).getFirst() (patrz ImportOptionsRunner).
     */
    static String exercise1(ApplicationArguments args) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj SpringApplication dla Boot03Application (bez weba, bez bannera) z właściwością
     * domyślną {@code boot03.shop-name=Sklep Ćwiczeniowy}, uruchom ją z podanymi argumentami, odczytaj z Environment
     * wartość boot03.shop-name, ZAMKNIJ kontekst i zwróć odczytaną wartość.
     * Bez argumentów wynik to „Sklep Ćwiczeniowy”, z {@code --boot03.shop-name=Inny} — „Inny”.
     * Podpowiedź: new SpringApplication(...), setDefaultProperties(Map.of(...)), try-with-resources na wyniku run(args).
     */
    static String exercise2(String... args) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // ĆWICZENIE 3 (trudniejsze): uzupełnij klasę Boot03ExerciseExitConfig (opis w jej Javadoc).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Typem argumentu metody run: CommandLineRunner dostaje surowe String... (jak main), ApplicationRunner —
     *      ApplicationArguments z opcjami (--klucz=wartość) i zwykłymi słowami osobno. Oba wykonują się raz, po
     *      ApplicationStartedEvent i przed ApplicationReadyEvent, posortowane RAZEM według {@code @Order}
     *      (mniejsza liczba wcześniej). Sprawdzone testem w Boot03SpringApplicationTest (sekcje 2 i 4).
     *   2. „Sklep z Linii Poleceń” — właściwości domyślne mają najniższy priorytet, argumenty wiersza poleceń jeden
     *      z najwyższych. Sprawdzone testem (sekcja 1).
     *   3. Kod 3 (z beana ShopExitCode), a kontekst będzie zamknięty — exit zawsze go zamyka (isActive() == false).
     *      Sprawdzone testem (sekcja 3).
     *   4. Nie wystartuje: run(...) rzuci wyjątek (kontrolowany, np. IOException, opakowany w IllegalStateException
     *      „Failed to execute CommandLineRunner”; RuntimeException — bez opakowania), kontekst zostanie zamknięty,
     *      a zamiast ApplicationReadyEvent poleci ApplicationFailedEvent. Sprawdzone testem (sekcja 4,
     *      Boot03FailingRunnerConfig).
     *   5. getOptionValues zwraca null, gdy opcji nie ma, więc .getFirst() rzuca NullPointerException. Naprawa: najpierw
     *      args.containsOption("import.file") (jak w ImportOptionsRunner) albo wartość domyślna. Sprawdzone testem (sekcja 2).
     *   6. optional/developmentOnly — żeby DevTools nie przechodził na projekty zależne i nie trafiał do wersji
     *      produkcyjnej (spring-boot-maven-plugin i tak pomija go przy pakowaniu, excludeDevtools = true). W testach
     *      DevTools wykrywa, że start idzie z JUnit, i wyłącza restart — restart w środku testu zepsułby jego przebieg.
     *   7. W beanie CommandLineRunner/ApplicationRunner albo w metodzie {@code @EventListener} dla ApplicationReadyEvent
     *      (ta wykona się po wszystkich runnerach).
     */
    // </editor-fold>
}

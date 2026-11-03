package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

/**
 * <pre>
 * TEMAT: SpringApplication — co naprawdę dzieje się w {@code SpringApplication.run(...)}
 *        (application = aplikacja; runner = „biegacz”, kod uruchamiany po starcie; event = zdarzenie;
 *         lazy initialization = leniwa inicjalizacja; exit code = kod wyjścia programu)
 *
 * W SKRÓCIE:
 *   Jedna linijka w main — {@code SpringApplication.run(Boot03Application.class, args)} — robi całą robotę startu:
 *   ustala typ aplikacji (z serwerem WWW czy bez), przygotowuje środowisko (ustawienia, profile, argumenty),
 *   tworzy kontekst (kontener beanów), wypełnia go (refresh), a na końcu wywołuje Twoje runnery.
 *   Na każdym etapie Boot rozsyła zdarzenia, więc możesz się „podpiąć” pod start. Tę samą pracę możesz wykonać
 *   ręcznie: {@code new SpringApplication(...)} albo wygodniej — {@code SpringApplicationBuilder}.
 *
 * ANALOGIA: otwieranie sklepu rano.
 *   Kierownik (SpringApplication) sprawdza, jaki to dzień i jaki rodzaj sklepu (typ aplikacji, profile), czyta
 *   kartkę z poleceniami od właściciela (argumenty i ustawienia), włącza światła i kasy (tworzy beany), a dopiero
 *   potem woła pracowników z listą zadań na otwarcie (runnery). Przez megafon ogłasza kolejne etapy
 *   („zaczynamy”, „światła włączone”, „otwarte”) — to zdarzenia, które może usłyszeć każdy, kto stoi w pobliżu.
 *
 * JAK TO DZIAŁA:
 *   (uproszczona kolejność)
 *   1. {@code new SpringApplication(źródła...)} — zapamiętuje klasy główne i ustala typ aplikacji z classpath.
 *   2. {@code run(args)}: zdarzenie ApplicationStartingEvent (jeszcze nic nie istnieje).
 *   3. Przygotowanie środowiska (Environment): argumenty, ustawienia, profile → ApplicationEnvironmentPreparedEvent.
 *   4. Utworzenie kontekstu (ApplicationContext) → ApplicationContextInitializedEvent.
 *   5. Wczytanie definicji beanów → ApplicationPreparedEvent (beany jeszcze NIE powstały).
 *   6. refresh — powstają beany singletony (chyba że leniwe) → ContextRefreshedEvent.
 *   7. ApplicationStartedEvent → wywołanie runnerów (ApplicationRunner i CommandLineRunner).
 *   8. ApplicationReadyEvent — aplikacja gotowa do pracy.
 *   Gdy coś pójdzie źle na dowolnym etapie: ApplicationFailedEvent i czytelny komunikat „APPLICATION FAILED TO START”.
 *
 * SŁÓWKA:
 *   SpringApplication = klasa uruchamiająca aplikację; builder = konstruktor (obiekt składany krok po kroku);
 *   fluent API = płynne API (łańcuch wywołań); web application type = typ aplikacji webowej (NONE, SERVLET, REACTIVE);
 *   default properties = ustawienia domyślne (o najniższym priorytecie); profile = profil (nazwany zestaw ustawień);
 *   arguments = argumenty programu; option = opcja ({@code --nazwa=wartość}); non-option argument = zwykły argument;
 *   listener = odbiornik zdarzeń; failure = awaria; analyzer = analizator; exit = wyjście, zakończenie programu.
 *
 * ZOBACZ TEŻ: s03_boot_basics/Boot01AutoConfiguration (ApplicationContextRunner i raport warunków),
 *             s02_configuration/Cfg05Environment (Environment, profile i źródła ustawień),
 *             s03_boot_basics/Boot04Starters (skąd Boot wie, że aplikacja nie ma serwera WWW),
 *             s01_core_ioc/Ioc04BeanMethods (beany singleton i ich tworzenie).
 * </pre>
 */
public final class Boot03SpringApplication {

    private Boot03SpringApplication() {
    }

    // =================================================================================================
    // 1. SpringApplication.run — KROK PO KROKU
    // =================================================================================================
    //
    //   public static void main(String[] args) {
    //       SpringApplication.run(Boot03Application.class, args);     // skrót do trzech linii poniżej
    //   }
    //
    //   SpringApplication application = new SpringApplication(Boot03Application.class);   // 1. konfiguracja
    //   application.setWebApplicationType(WebApplicationType.NONE);                       //    typ aplikacji
    //   application.setDefaultProperties(Map.of("shop.mode", "default"));                 //    ustawienia domyślne
    //   application.setAdditionalProfiles("demo");                                        //    dodatkowy profil
    //   try (ConfigurableApplicationContext context = application.run(args)) {            // 2. start
    //       ...                                                                           //    używasz kontekstu
    //   }                                                                                 // 3. close() = zamknięcie
    //
    // Co daje taki zapis w porównaniu z gołym {@code run}? Możesz ustawić rzeczy ZANIM aplikacja ruszy.
    // Test sekcji 1 sprawdza (uruchomieniem):
    //   • w tym module (bez weba) Boot sam wybiera WebApplicationType.NONE; ustawiamy go jawnie, żeby było widać, gdzie
    //     się to ustawia (inne wartości: SERVLET — klasyczny serwer, REACTIVE — serwer reaktywny);
    //   • profil z setAdditionalProfiles jest aktywny (context.getEnvironment().getActiveProfiles());
    //   • setDefaultProperties ma NAJNIŻSZY priorytet: gdy ten sam klucz przyjdzie z argumentu programu
    //     ({@code --shop.mode=demo}), wygrywa argument; gdy argumentu nie ma — działa wartość domyślna;
    //   • argumenty programu trafiają do Environment jako zwykłe ustawienia (dlatego {@code --server.port=9090} działa
    //     bez żadnego kodu).
    //   • try-with-resources zamyka kontekst: po wyjściu z bloku {@code isActive()} zwraca false.
    //
    // PUŁAPKA: w testach nie wołaj {@code Boot03Application.main(...)} i nie zostawiaj niezamkniętych kontekstów.
    //   Dlaczego? Każde run() startuje CAŁY kontekst (wątki, pule połączeń, nazwy baz). Niezamknięty kontekst żyje do
    //   końca testów, trzyma zasoby i potrafi zepsuć kolejne testy (np. konflikt portu albo nazwy bazy).
    //   Zawsze: try-with-resources albo {@code @SpringBootTest}, który zamyka kontekst za Ciebie.
    //
    // DOBRA PRAKTYKA: ustawienia „na stałe” trzymaj w application.properties, a w kodzie startowym (setXxx) tylko to,
    //   co naprawdę dotyczy sposobu uruchomienia. Dlaczego? Ustawienia z pliku można zmienić bez rekompilacji
    //   (argument, zmienna środowiskowa), a zaszyte w main — nie.
    //
    // PYTANIE REKRUTACYJNE: co robi SpringApplication.run() i jaki obiekt zwraca?
    //   (zwraca ConfigurableApplicationContext — uruchomiony kontekst)

    // =================================================================================================
    // 2. SpringApplicationBuilder — TO SAMO, ALE PŁYNNIE
    // =================================================================================================
    //
    //   new SpringApplicationBuilder(Boot03Application.class)      // builder = konstruktor (składa aplikację)
    //           .web(WebApplicationType.NONE)                      // typ aplikacji
    //           .properties("shop.mode=builder")                   // ustawienia domyślne (jak setDefaultProperties)
    //           .profiles("demo")                                  // dodatkowe profile (jak setAdditionalProfiles)
    //           .bannerMode(Banner.Mode.OFF)                       // bez ASCII-artu „Spring” w logu
    //           .lazyInitialization(true)                          // leniwe beany (sekcja 5)
    //           .run(args);                                        // = application.run(args)
    //
    // Builder (pakiet org.springframework.boot.builder) to „fluent API” — każda metoda zwraca builder, więc piszesz
    // jeden łańcuch zamiast wielu setterów. Pod spodem tworzy zwykły SpringApplication.
    // Test sekcji 2 sprawdza, że efekt jest ten sam co w sekcji 1 (ustawienie domyślne i profil), a leniwość
    // (lazyInitialization) rzeczywiście działa: bean z licznikiem nie powstaje przy starcie.
    //
    // Przy okazji: spring.main.banner-mode=off (jak w application.properties tego modułu) działa tak samo jak
    // {@code bannerMode(OFF)} — to dwa sposoby na to samo. Wszystkie settery SpringApplication mają odpowiedniki
    // w ustawieniach {@code spring.main.*}.
    //
    // DOBRA PRAKTYKA: w testach i narzędziach używaj buildera, w zwykłej aplikacji wystarcza SpringApplication.run.
    //   Dlaczego? Builder jest czytelniejszy przy wielu ustawieniach, a zwykły main powinien być krótki i oczywisty.

    // =================================================================================================
    // 3. ApplicationRunner I CommandLineRunner — KOD URUCHAMIANY PO STARCIE
    // =================================================================================================
    //
    // Czasem chcesz coś zrobić zaraz po starcie (wczytać dane, wypisać raport, sprawdzić konfigurację). Dwa interfejsy:
    //   CommandLineRunner      → run(String... args)             — argumenty „na surowo”, tak jak w main
    //   ApplicationRunner      → run(ApplicationArguments args)  — argumenty już rozdzielone:
    //        args.getOptionNames()      → nazwy opcji ({@code --shop.mode=demo} daje "shop.mode")
    //        args.getOptionValues(nazwa)→ lista wartości opcji (opcja może się powtarzać: --x=1 --x=2)
    //        args.getNonOptionArgs()    → argumenty bez {@code --} ("zeszyt", "dlugopis")
    // Oba są beanami (np. {@code @Component}). Kolejność ustala {@code @Order} (mniejsza liczba = wcześniej).
    // Zobacz klasy PlainArgumentsRunner (@Order(1)) i OptionArgumentsRunner (@Order(2)) — zapisują wpisy
    // w beanie ShopStartupLog. Test sekcji 3 uruchamia aplikację z argumentami {@code --shop.mode=demo zeszyt --x=1 --x=2}
    // i odczytuje dziennik:
    //   CommandLineRunner:  [--shop.mode=demo, zeszyt, --x=1, --x=2]      (surowa tablica)
    //   ApplicationRunner:  opcje=[shop.mode, x], zwykłe=[zeszyt]         (rozdzielone)
    // Z @Order oba rodzaje runnerów są układane razem według liczby (w teście CommandLineRunner z @Order(1) biegnie
    // przed ApplicationRunner z @Order(2)). BEZ @Order kolejność nie jest gwarantowana — w naszych próbach dwóch
    // runnerów bez @Order wynik zmieniał się między uruchomieniami, więc zawsze dawaj jawny @Order.
    //
    // Runnery działają PO utworzeniu wszystkich beanów, więc mogą wstrzykiwać dowolne beany (jak ShopStartupLog).
    //
    // PUŁAPKA: ciężka praca albo wyjątek w runnerze. Dlaczego? Runner wykonuje się w wątku startu: dopóki trwa,
    //   aplikacja nie ogłosi ApplicationReadyEvent (nie jest „gotowa”), a wyjątek z runnera przerywa start całej
    //   aplikacji (ApplicationFailedEvent, kod wyjścia 1). Do długich zadań w tle użyj osobnego wątku/schedulera.
    //
    // DOBRA PRAKTYKA: argumenty czytaj przez ApplicationArguments (albo wprost jako ustawienia przez Environment),
    //   a nie przez parsowanie String[] ręcznie. Dlaczego? Opcje {@code --nazwa=wartość} są wtedy rozpoznawane
    //   zgodnie ze Springiem (powtórzenia, brak wartości) i nie piszesz własnego parsera, który zawsze ma luki.

    // =================================================================================================
    // 4. ZDARZENIA STARTU — KTO CO SŁYSZY I KIEDY
    // =================================================================================================
    //
    // Boot rozsyła zdarzenia (pakiet org.springframework.boot.context.event), a odbiornik to ApplicationListener:
    //   ApplicationStartingEvent             — sam początek run(); nic jeszcze nie istnieje (ani środowisko, ani kontekst)
    //   ApplicationEnvironmentPreparedEvent  — środowisko gotowe (ustawienia, profile), kontekstu jeszcze nie ma
    //   ApplicationContextInitializedEvent   — kontekst utworzony, definicje beanów jeszcze niewczytane
    //   ApplicationPreparedEvent             — definicje beanów wczytane, ale beany jeszcze NIE powstały
    //   (ContextRefreshedEvent — zdarzenie samego Springa: wszystkie beany singletony gotowe)
    //   ApplicationStartedEvent              — kontekst gotowy, runnery JESZCZE nie ruszyły
    //   (… tu wykonują się ApplicationRunner i CommandLineRunner …)
    //   ApplicationReadyEvent                — runnery skończone, aplikacja gotowa
    //   ApplicationFailedEvent               — start się nie udał (niesie wyjątek: getException())
    //
    // Test sekcji 4 podpina odbiornik przez {@code application.addListeners(...)}, zbiera zdarzenia i sprawdza
    // kolejność (zweryfikowaną uruchomieniem):
    //   ApplicationStartingEvent → ApplicationEnvironmentPreparedEvent → ApplicationContextInitializedEvent →
    //   ApplicationPreparedEvent → ContextRefreshedEvent → ApplicationStartedEvent → [wpisy runnerów] →
    //   ApplicationReadyEvent.
    // Wpisy runnerów widać w tej samej kolejności, bo ShopStartupLog publikuje je jako zdarzenia — to dowód, że runnery
    // biegną MIĘDZY Started a Ready. Przy nieudanym starcie kolejność kończy się na ApplicationFailedEvent
    // (ani Started, ani Ready nie dostaniesz).
    //
    // PUŁAPKA: {@code @EventListener} na metodzie beana NIE usłyszy wczesnych zdarzeń. Dlaczego? Zdarzenia Starting,
    //   EnvironmentPrepared, ContextInitialized i Prepared lecą, zanim kontekst zdążył utworzyć beany, więc nie ma
    //   jeszcze beana z metodą @EventListener. Test sekcji 4 to pokazuje: odbiornik-bean słyszy Started i Ready, ale
    //   nie Starting ani Prepared. Wczesne zdarzenia łapiesz tylko przez {@code application.addListeners(...)}
    //   (albo wpis ApplicationListener w META-INF/spring.factories — tak robią biblioteki).
    //
    // DOBRA PRAKTYKA: do „zrób coś, gdy aplikacja jest gotowa” używaj {@code @EventListener(ApplicationReadyEvent.class)}
    //   (albo runnera), a nie ApplicationStartedEvent. Dlaczego? Po Started runnery mogą jeszcze nie skończyć pracy;
    //   po Ready masz pewność, że start jest zakończony. (Przy zadaniach, które MUSZĄ się wykonać przed runnerami,
    //   wybierz Started.)

    // =================================================================================================
    // 5. LENIWA INICJALIZACJA (lazy initialization)
    // =================================================================================================
    //
    // Domyślnie Spring tworzy WSZYSTKIE beany singletony od razu przy starcie (tryb „gorliwy”, eager). Włączasz leniwy:
    //   spring.main.lazy-initialization=true      albo   application.setLazyInitialization(true)
    //   (pojedynczy bean: adnotacja @Lazy na klasie albo metodzie @Bean)
    // Wtedy bean powstaje dopiero przy pierwszym użyciu (getBean albo wstrzyknięcie do innego beana).
    // Test sekcji 5: LazyReportGenerator ma licznik konstrukcji. Gorliwy start → licznik rośnie o 1 jeszcze przy run();
    // leniwy start → licznik się nie zmienia, aż pierwszy raz pobierzesz beana (wtedy rośnie o 1; drugie pobranie nie
    // tworzy nowego obiektu, bo to wciąż singleton).
    //
    // Zysk: szybszy start (np. w testach i przy programowaniu z DevTools). Cena: BŁĘDY KONFIGURACJI WYCHODZĄ PÓŹNIEJ.
    // Test sekcji 5 ma też bean, któremu brakuje zależności: gorliwy start rzuca wyjątek od razu, leniwy startuje bez
    // błędu, a wyjątek dostajesz dopiero przy pierwszym pobraniu tego beana.
    //
    // PUŁAPKA: leniwa inicjalizacja na produkcji bez namysłu. Dlaczego? Błąd w konfiguracji (brak beana, zła wartość)
    //   ujawni się dopiero, gdy pierwszy użytkownik trafi na dany kod — zamiast przy starcie, gdy wdrożenie można
    //   jeszcze wycofać. Pierwsze żądanie jest też wolniejsze, bo to ono płaci za tworzenie beanów.
    //
    // DOBRA PRAKTYKA: lenistwo włączaj świadomie (dev, testy, duże aplikacje z wolnym startem), a na produkcji
    //   zostaw tryb gorliwy. Dlaczego? „Padnij szybko” (fail fast): lepiej, żeby aplikacja nie wystartowała wcale, niż
    //   żeby działała z ukrytym błędem.

    // =================================================================================================
    // 6. KODY WYJŚCIA I NIEUDANY START
    // =================================================================================================
    //
    // Program kończy się kodem wyjścia (exit code): 0 = wszystko dobrze, inna liczba = błąd. Skrypty, harmonogramy
    // i Kubernetes patrzą właśnie na tę liczbę. Spring Boot pozwala ją ustalić:
    //   ExitCodeGenerator (bean albo argument) — metoda getExitCode() zwraca liczbę;
    //   int kod = SpringApplication.exit(context, generatory...);   // zamyka kontekst i zwraca kod wyjścia
    //   System.exit(kod);                                           // dopiero to kończy proces
    // Gdy generatorów jest kilka (beany ExitCodeGenerator + przekazane jako argumenty), liczy się PIERWSZY, który
    // zwróci wartość różną od 0 (zera są pomijane); generatory podane jako argumenty wyprzedzają beany.
    // Test sekcji 6 (sprawdzone uruchomieniem): bean zwraca 7 → exit daje 7 i kontekst jest zamknięty; dodatkowy
    // generator {@code () -> 0} nic nie zmienia (7), a {@code () -> 9} z argumentu wygrywa z beanem (9);
    // bez generatorów → 0. SpringApplication.exit sama NIE kończy procesu — zwraca liczbę.
    //
    // NIEUDANY START. Gdy kontekst nie może powstać (np. brakuje beana), run() rzuca wyjątek, a Boot:
    //   1. publikuje ApplicationFailedEvent (niesie wyjątek),
    //   2. przepuszcza wyjątek przez FailureAnalyzer-y (analizatory awarii) — klasy, które rozpoznają typowe błędy
    //      i zamieniają stos wyjątków w krótki komunikat „APPLICATION FAILED TO START” z sekcjami
    //      Description (co się stało) i Action (co zrobić),
    //   3. zamyka kontekst i rzuca wyjątek dalej (program kończy się kodem 1).
    // Test sekcji 6: aplikacja z beanem, któremu brakuje zależności, rzuca UnsatisfiedDependencyException z opisem
    // brakującego typu, a odbiornik dostaje ApplicationFailedEvent z tym samym wyjątkiem i NIE dostaje Started ani Ready.
    // Własne analizatory pisze się implementując FailureAnalyzer i rejestrując w META-INF/spring.factories (tak jak
    // biblioteki) — poza zakresem tej lekcji, ale warto wiedzieć, skąd bierze się czytelny komunikat.
    //
    // PUŁAPKA: łapanie wyjątku z run() i „połknięcie” go. Dlaczego? Aplikacja, która nie wystartowała, kończy się
    //   wtedy kodem 0 i wygląda na zdrową — harmonogram czy kontener nie zauważą awarii. Zostaw wyjątek w spokoju albo
    //   zakończ proces niezerowym kodem.
    //
    // DOBRA PRAKTYKA: czytaj komunikat „APPLICATION FAILED TO START” od góry — sekcja Action zwykle mówi, co zrobić.
    //   Dlaczego? To najkrótsza droga do przyczyny; stos wyjątków ma dziesiątki linii, a prawdziwy powód bywa na samym
    //   dole („Caused by”).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • run(args) = ustal typ aplikacji → Environment → kontekst → refresh (beany) → runnery → gotowe.
     *   • new SpringApplication(...) + settery albo SpringApplicationBuilder (web, properties, profiles, bannerMode,
     *     lazyInitialization) — ten sam efekt; ustawienia domyślne mają najniższy priorytet, argumenty wygrywają.
     *   • CommandLineRunner = argumenty surowe (String...), ApplicationRunner = ApplicationArguments (opcje i zwykłe);
     *     kolejność tylko przez @Order; wyjątek w runnerze przerywa start.
     *   • Kolejność zdarzeń: Starting → EnvironmentPrepared → ContextInitialized → Prepared → ContextRefreshed →
     *     Started → runnery → Ready; porażka → Failed. Wczesne zdarzenia tylko przez application.addListeners(...).
     *   • Leniwie: spring.main.lazy-initialization=true — szybszy start, błędy dopiero przy użyciu.
     *   • Kod wyjścia: SpringApplication.exit(context, generatory) zwraca liczbę (pierwszy niezerowy generator);
     *     System.exit ją wykonuje.
     *   • Nieudany start: ApplicationFailedEvent + FailureAnalyzer („APPLICATION FAILED TO START”) + wyjątek z run().
     *
     * PYTANIA KONTROLNE:
     *   1. Wymień etapy startu aplikacji w kolejności, w jakiej Boot je wykonuje.
     *   2. Co się stanie? Ustawiasz {@code setDefaultProperties(Map.of("shop.mode", "default"))}, a program uruchamiasz
     *      z argumentem {@code --shop.mode=demo}. Jaką wartość dostaniesz?
     *   3. Czym różni się CommandLineRunner od ApplicationRunner? Który wybierzesz do odczytu {@code --nazwa=wartość}?
     *   4. ZNAJDŹ BŁĄD: {@code @EventListener public void onStart(ApplicationStartingEvent e)} w beanie nigdy się nie
     *      wywołuje. Dlaczego i jak to naprawić?
     *   5. Co się stanie? Włączasz {@code spring.main.lazy-initialization=true}, a bean ma błąd konfiguracji
     *      (brak zależności). Kiedy zobaczysz wyjątek?
     *   6. Co się stanie? W kontekście jest bean ExitCodeGenerator zwracający 7, a do {@code SpringApplication.exit}
     *      przekazujesz dodatkowo generator {@code () -> 0}. Jaki kod dostaniesz?
     *   7. Dlaczego runnery powinny mieć jawny @Order?
     *   8. Po co istnieją FailureAnalyzer-y i skąd wiesz, co zrobić po „APPLICATION FAILED TO START”?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Boot03SpringApplicationExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj (NIE uruchamiaj) {@code SpringApplication} dla {@link Boot03Application}:
     * bez serwera WWW (NONE), z ustawieniem domyślnym {@code shop.mode} równym podanej wartości i z dodatkowym
     * profilem {@code demo}. Test uruchamia ją i sprawdza typ, profil oraz wartość ustawienia.
     * Podpowiedź: sekcja 1 — {@code setWebApplicationType}, {@code setDefaultProperties(Map.of(...))},
     * {@code setAdditionalProfiles}.
     */
    static SpringApplication exercise1Application(String shopMode) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): opisz argumenty programu jako tekst: najpierw opcje posortowane po nazwie, każda jako
     * {@code nazwa=wartość1,wartość2} (opcja bez wartości daje {@code nazwa=}), opcje oddzielone spacją, potem
     * {@code " | "}, potem zwykłe argumenty oddzielone spacją. Przykład: {@code --x=1 --x=2 --a=z foo bar}
     * daje {@code "a=z x=1,2 | foo bar"}.
     * Podpowiedź: sekcja 3 — {@code getOptionNames()}, {@code getOptionValues(nazwa)}, {@code getNonOptionArgs()},
     * {@code String.join}.
     */
    static String exercise2DescribeArguments(ApplicationArguments arguments) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć odbiornik zdarzeń, który zapisuje do listy {@code sink} PROSTE nazwy klas
     * ({@code getClass().getSimpleName()}) tylko tych zdarzeń, które pochodzą z pakietu
     * {@code org.springframework.boot.context.event} (zdarzenia startu Boota). Inne zdarzenia (np. ContextRefreshedEvent,
     * zdarzenia ze Springa) ignoruje. Test dodaje go przez {@code addListeners} i sprawdza kolejność.
     * Podpowiedź: sekcja 4 — odbiornik to lambda {@code event -> ...}; pakiet klasy zdarzenia:
     * {@code event.getClass().getPackageName()}.
     */
    static ApplicationListener<ApplicationEvent> exercise3BootEventRecorder(List<String> sink) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zamknij kontekst i zwróć kod wyjścia programu: 0, gdy {@code failedOrders} jest 0,
     * a w przeciwnym razie {@code 10 + failedOrders} (np. 3 nieudane zamówienia → 13). Po wywołaniu kontekst ma być
     * zamknięty. NIE wołaj {@code System.exit} — to zabiłoby proces testów.
     * Podpowiedź: sekcja 6 — {@code SpringApplication.exit(context, ExitCodeGenerator...)}; {@code ExitCodeGenerator}
     * ma jedną metodę, więc możesz podać lambdę.
     */
    static int exercise4ExitCode(ConfigurableApplicationContext context, int failedOrders) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Typ aplikacji z classpath → zdarzenie Starting → przygotowanie Environment (argumenty, ustawienia, profile)
     *      → kontekst → wczytanie definicji beanów (Prepared) → refresh, czyli tworzenie beanów (ContextRefreshed) →
     *      Started → runnery → Ready. Sprawdzone testem (sekcja 4).
     *   2. "demo". Ustawienia domyślne mają najniższy priorytet, argument programu je przesłania. Sprawdzone testem
     *      (sekcja 1).
     *   3. CommandLineRunner dostaje surową tablicę String[] (opcje i zwykłe argumenty pomieszane), ApplicationRunner —
     *      ApplicationArguments z rozdzielonymi opcjami i zwykłymi argumentami. Do {@code --nazwa=wartość} wybierz
     *      ApplicationRunner. Sprawdzone testem (sekcja 3).
     *   4. Zdarzenie Starting leci, zanim powstanie jakikolwiek bean, więc nie ma jeszcze beana z tą metodą.
     *      Naprawa: {@code application.addListeners(odbiornik)} (albo rejestracja w META-INF/spring.factories).
     *      Sprawdzone testem (sekcja 4).
     *   5. Dopiero przy pierwszym pobraniu (użyciu) wadliwego beana — start przejdzie bez błędu. Przy trybie gorliwym
     *      wyjątek poleciałby od razu z run(). Sprawdzone testem (sekcja 5).
     *   6. 7 — generator zwracający 0 jest pomijany, liczy się pierwszy niezerowy. Sprawdzone testem (sekcja 6).
     *   7. Bez @Order kolejność runnerów nie jest gwarantowana (w próbach zmieniała się między uruchomieniami), a od
     *      kolejności bywa zależny wynik (np. jeden runner przygotowuje dane dla drugiego).
     *   8. FailureAnalyzer-y rozpoznają typowe błędy (brak beana, zajęty port, zła wartość ustawienia) i zamieniają
     *      stos wyjątków w komunikat z sekcjami Description i Action — to druga z nich mówi, co zrobić.
     */
    // </editor-fold>
}

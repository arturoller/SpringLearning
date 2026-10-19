package s00_start.start08_debugging;

/**
 * <pre>
 * TEMAT: Debugowanie aplikacji Springa — jak czytać błędy startu, błędy kontekstu w testach, używać debuggera i logów
 *        (debugging = szukanie i usuwanie błędów; stack trace = ślad stosu, lista wywołań prowadzących do błędu;
 *        breakpoint = punkt przerwania; log = dziennik zdarzeń programu)
 *
 * W SKRÓCIE:
 *   Pierwszy błąd Springa wygląda strasznie: ściana tekstu, kilkadziesiąt linii „Caused by”. Tymczasem prawie zawsze
 *   odpowiedź jest w DWÓCH miejscach: w bloku „APPLICATION FAILED TO START” (Description i Action) i w OSTATNIM
 *   „Caused by” (pierwotna przyczyna). Lekcja uczy: (1) czytać błąd startu, (2) czytać błędy kontekstu w testach i nie
 *   dać się zmylić komunikatowi „failure threshold”, (3) używać debuggera IntelliJ w aplikacji pełnej klas Springa,
 *   (4) włączać logi i raport autokonfiguracji, (5) odróżniać błąd ŚRODOWISKA od błędu KODU.
 *
 * ANALOGIA: lekarz i objawy.
 *   Komunikat „APPLICATION FAILED TO START” to rozmowa z pacjentem (co boli i co robić), łańcuch „Caused by” to wyniki
 *   badań — diagnoza jest na samym końcu. Debugger to prześwietlenie (patrzysz do środka w wybranej chwili), logi to
 *   dzienniczek pacjenta. A błąd środowiska to... pacjent, który przyszedł nie do tego gabinetu.
 *
 * JAK TO DZIAŁA:
 *   błąd przy starcie → Spring Boot (SpringApplication) łapie wyjątek → FailureAnalyzer (analizator błędu) rozpoznaje
 *   typ wyjątku w łańcuchu przyczyn → loguje blok Description + Action; gdy żaden analizator nie pasuje, loguje
 *   „Application run failed” i pełny stos.
 *   test Springa → ładuje kontekst → błąd → „Failed to load ApplicationContext” (z pierwotną przyczyną) → kolejne
 *   testy z tą samą konfiguracją dostają tylko „failure threshold (1) exceeded” (bez przyczyny).
 *   Pomocnicze klasy lekcji: BrokenConfigs (celowo zepsute konfiguracje), DebugShop (typy sklepu), ErrorCatalog
 *   (rozpoznawanie typowych komunikatów: kod czy środowisko), ExerciseConfig i StockFailureAnalyzer (ćwiczenia).
 *
 * SŁÓWKA:
 *   debug = odpluskwiać (szukać błędów); breakpoint = punkt przerwania; step over / step into / step out = krok
 *   obok / do środka / na zewnątrz; frame = ramka stosu; evaluate expression = oblicz wyrażenie; root cause =
 *   pierwotna przyczyna; failure analyzer = analizator błędu; threshold = próg; log level = poziom logowania;
 *   condition = warunek; report = raport; caused by = spowodowany przez.
 *
 * ZOBACZ TEŻ: t00_start/Start05Debugging (debugger w czystej Javie — te same skróty klawiszowe),
 *             s00_start/Start04TestingIntro (co robi @SpringBootTest), s01_core_ioc/Ioc05MultipleCandidates (dwóch
 *             kandydatów: @Primary i @Qualifier), s03_boot_basics/Boot01AutoConfiguration (raport warunków od środka),
 *             s15_actuator_logging (logowanie w całości), t10_exceptions/Exceptions06ChainingWrapping (łańcuch przyczyn).
 * </pre>
 */
public final class Start08Debugging {

    private Start08Debugging() {
    }

    // =================================================================================================
    // 1. CZYTANIE BŁĘDU STARTU
    // =================================================================================================
    //
    // Gdy aplikacja nie startuje, Boot wypisuje (prawdziwy wynik uruchomienia, test applicationFailedToStart):
    //
    //   ***************************
    //   APPLICATION FAILED TO START
    //   ***************************
    //
    //   Description:
    //
    //   Parameter 0 of method priceReport in s00_start.start08_debugging.BrokenConfigs$MissingBean required a bean of
    //   type 's00_start.start08_debugging.DebugShop$PriceSource' that could not be found.
    //
    //   Action:
    //
    //   Consider defining a bean of type 's00_start.start08_debugging.DebugShop$PriceSource' in your configuration.
    //
    // Czytaj to tak: Description (opis) mówi CO się stało, Action (działanie) — CO zrobić. To nie jest zwykły wyjątek,
    // tylko wynik pracy analizatora błędu (FailureAnalyzer): Boot ma ich wiele (same podstawy Boota — 19, kolejne dochodzą z innych modułów), a każdy zna jeden typowy błąd.
    // Analizator patrzy w CAŁY łańcuch przyczyn (nie tylko na wierzch) i zamienia go na ludzkie zdanie.
    //
    // Gdy analizator nie rozpozna błędu, dostajesz zamiast tego linię „Application run failed” i długi stos. Tak jest np.
    // przy brakującej właściwości (test noAnalyzerMatches). Wtedy sam czytasz łańcuch „Caused by”:
    //
    //   BeanCreationException: Error creating bean with name 'shopInfo' ...: Unexpected exception during bean creation
    //   Caused by: PlaceholderResolutionException: Could not resolve placeholder 'shop.currency' in value "${shop.currency}"
    //
    // ZASADA: łańcuch „Caused by” czytaj OD DOŁU. Wyjątek z wierzchu to tylko opakowanie, które dorzuciły kolejne warstwy
    // Springa („Error creating bean...”, „Unsatisfied dependency...”). Pierwotna przyczyna — to, co NAPRAWDĘ poszło źle —
    // jest w ostatnim „Caused by”. Testy missingBean, twoCandidates, missingProperty sprawdzają to dla trzech typowych
    // błędów.
    //
    // Cztery najczęstsze błędy startu i ich pierwotne przyczyny:
    //   brak beana          UnsatisfiedDependencyException → NoSuchBeanDefinitionException
    //                       („required a bean of type ... that could not be found”)
    //   dwóch kandydatów    UnsatisfiedDependencyException → NoUniqueBeanDefinitionException
    //                       („expected single matching bean but found 2: emailNotifier,smsNotifier”;
    //                        analizator: „required a single bean, but 2 were found” + rada @Primary / @Qualifier)
    //   brak właściwości    BeanCreationException → PlaceholderResolutionException
    //                       („Could not resolve placeholder 'shop.currency'”)
    //   zajęty port         ApplicationContextException → PortInUseException → ... → BindException
    //                       („Web server failed to start. Port 8080 was already in use.” — to błąd środowiska, sekcja 5)
    //
    // PUŁAPKA: czytanie błędu OD GÓRY i szukanie przyczyny w pierwszej linii stosu. Dlaczego? Pierwsza linia to opakowanie
    //   („Error creating bean with name...”) — mówi, KTÓRY bean się nie udał, a nie DLACZEGO. Przyczyna jest na dnie.
    //
    // PUŁAPKA: pomijanie bloku Description/Action, bo „to tylko wstęp do stosu”. Dlaczego? To najkrótsza droga do rozwiązania:
    //   Boot sam podaje radę (np. „Consider marking one of the beans as @Primary...”), którą dałoby się wykonać od razu.
    //
    // DOBRA PRAKTYKA: przeczytaj błąd w tej kolejności: (1) Description i Action, (2) ostatni „Caused by”, (3) dopiero
    //   potem wyszukiwarka z dokładnym tekstem pierwotnej przyczyny. Dlaczego? Wyszukanie całego stosu daje szum; krótki
    //   komunikat pierwotnej przyczyny daje trafne odpowiedzi.
    //
    // WAŻNE: Spring Boot 4.1 nadal pisze błędy po angielsku (te same komunikaty co w starszych wersjach, jakie znajdziesz
    // w tutorialach). Przepisuj je do wyszukiwarki dosłownie, bez tłumaczenia.

    // =================================================================================================
    // 2. BŁĘDY KONTEKSTU W TESTACH
    // =================================================================================================
    //
    // W teście @SpringBootTest (albo @WebMvcTest, @DataJpaTest) Spring uruchamia kontekst PRZED pierwszą metodą testową.
    // Gdy konfiguracja jest zepsuta, test kończy się błędem:
    //
    //   java.lang.IllegalStateException: Failed to load ApplicationContext for [WebMergedContextConfiguration@... classes = ...]
    //   Caused by: ... UnsatisfiedDependencyException ...
    //   Caused by: ... NoSuchBeanDefinitionException: No qualifying bean of type '...DebugShop$PriceSource' available: ...
    //
    // „Failed to load ApplicationContext” to opakowanie — pierwotna przyczyna jest, jak zawsze, w ostatnim „Caused by”.
    //
    // Potem robi się ciekawiej. Spring zapamiętuje, że konfiguracja się nie udała, i przy KOLEJNYM teście z tą samą
    // konfiguracją nie próbuje ładować kontekstu od nowa (to byłoby wolne i bez sensu). Zamiast tego rzuca:
    //
    //   java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to
    //   load context for [WebMergedContextConfiguration@... classes = ...]
    //
    // Ten komunikat NIE ma przyczyny i NIE jest nowym błędem — oznacza tylko „już raz się nie udało, więc pomijam”
    // (threshold = próg, domyślnie 1 nieudana próba). Test contextFailureThreshold uruchamia to ręcznie przez
    // TestContextManager (ten sam mechanizm, którego używa @SpringBootTest) i pokazuje oba komunikaty.
    //
    // Co widzisz po uruchomieniu klasy z kilkunastoma testami: pierwszy czerwony test ma „Failed to load ApplicationContext”
    // z pierwotną przyczyną, a pozostałe — „failure threshold (1) exceeded”.
    //
    // PUŁAPKA: szukanie przyczyny w teście, który rzuca „failure threshold exceeded”. Dlaczego? Ten komunikat nie ma w sobie
    //   NICZEGO o prawdziwym błędzie — jest tylko skutkiem pierwszego. Szukasz w niewłaściwym miejscu i tracisz czas.
    //
    // DOBRA PRAKTYKA: w oknie wyników testów sortuj/przewiń do PIERWSZEGO nieudanego testu i dopiero tam czytaj stos.
    //   Dlaczego? Tylko pierwszy test z daną konfiguracją niesie pierwotną przyczynę; reszta to echo.
    //
    // DOBRA PRAKTYKA: po naprawie uruchom test ponownie przez ▶ zamiast „dopisywać obejścia” (np. wyższy próg
    //   spring.test.context.failure.threshold). Dlaczego? Próg zmienia tylko liczbę powtórzeń błędu, a nie jego przyczynę.

    // =================================================================================================
    // 3. DEBUGGER INTELLIJ
    // =================================================================================================
    //
    // Debugger zatrzymuje program w wybranej linii i pozwala obejrzeć wartości zmiennych. W aplikacji Springa to szczególnie
    // cenne, bo większość „magii” dzieje się poza Twoim kodem.
    //
    // Jak zacząć:
    //   • Debug zamiast Run: obok zielonego ▶ jest ikona „owada” (Debug). Działa tak samo przy teście (▶ przy metodzie lub
    //     klasie) i przy metodzie main klasy @SpringBootApplication. Z Mavena: ./mvnw -Dmaven.surefire.debug test
    //     (Maven czeka na podłączenie debuggera, domyślnie port 5005).
    //   • Breakpoint (punkt przerwania): klik w lewy margines obok numeru linii (czerwona kropka) — w metodzie serwisu,
    //     kontrolera albo w metodzie @Bean. Program zatrzyma się PRZED wykonaniem tej linii.
    //   • Sterowanie: F8 (Step Over — wykonaj linię, nie wchodź do środka), F7 (Step Into — wejdź do wywoływanej metody),
    //     Shift+F8 (Step Out — wyjdź z metody), F9 (Resume — jedź dalej do następnego breakpointu), Shift+F7 (Smart Step
    //     Into — wybierz, do której z kilku wywołań w linii wejść).
    //   • Evaluate Expression (Alt+F8, „oblicz wyrażenie”): wpisujesz dowolny kod Javy i widzisz wynik w tej chwili,
    //     np. productRepository.findAll().size() albo sku.equals("ELE-002"). Wywołanie metody może mieć efekt uboczny
    //     (zmienić dane), więc nie obliczaj tak operacji zapisu.
    //   • Breakpoint warunkowy: prawy klik w czerwoną kropkę → pole Condition, np. sku.equals("ELE-002"). Program stanie
    //     tylko dla tego produktu, a nie przy każdym z tysiąca przebiegów pętli.
    //
    // Breakpoint na wyjątku: Run → View Breakpoints (Ctrl+Shift+F8) → plus → Java Exception Breakpoints → wpisz klasę
    // wyjątku. Program staje w chwili RZUCENIA wyjątku, nie dopiero tam, gdzie go złapano i wypisano.
    // Spring jednak opakowuje wyjątki w kilka warstw (test wrappedException):
    //
    //   BeanCreationException          ← powstaje NA KOŃCU, w kodzie Springa, po tym jak coś już poszło źle
    //     BeanInstantiationException   ← pośrednia warstwa
    //       IllegalStateException      ← TWOJA przyczyna: „Magazyn niedostępny” — rzucona w twojej metodzie @Bean
    //
    // Dlatego breakpoint ustaw na klasie PRZYCZYNY (IllegalStateException, własny wyjątek), a nie na opakowaniu
    // (BeanCreationException). Opakowanie wpadnie dopiero głęboko w kodzie Springa i nie powie Ci, gdzie jest błąd.
    //
    // Ramki stosu (Frames) w kontekście Springa mają dziwne nazwy. Przykład z testu cglibFramesInStack, gdy kontener
    // wywołuje Twoją metodę @Bean:
    //
    //   BrokenConfigs$StackProbe$$SpringCGLIB$$0.stackSnapshot(<generated>)    ← proxy: podklasa wygenerowana przez Springa
    //   org.springframework.beans.factory.support.SimpleInstantiationStrategy... ← kod Springa
    //   BrokenConfigs$StackProbe.stackSnapshot(BrokenConfigs.java:NN)           ← TWÓJ kod
    //
    // Jak to czytać: ramki z „$$SpringCGLIB$$” to proxy (obiekt pośredniczący), który Spring wstawił między wywołującego a Twoją
    // klasę — dokłada transakcję, cache albo (tu) pilnuje, by metoda @Bean zwracała ten sam singleton. „<generated>” znaczy,
    // że nie ma pliku źródłowego (klasa powstała w czasie działania programu). Ramki bibliotek możesz ukryć w panelu Frames
    // (ikona filtra) i widzieć tylko swój kod; F7 do proxy wchodzi do kodu Springa, więc częściej używaj Shift+F7
    // (Smart Step Into) albo ustaw breakpoint wprost w swojej metodzie.
    //
    // PUŁAPKA: debugowanie metody, która nigdy nie jest wywoływana, bo wołasz ją z tej samej klasy (omija proxy). Dlaczego
    //   to mylące? Adnotacja (np. @Transactional) nie zadziała, a w debuggerze nie zobaczysz żadnej ramki proxy.
    //   Szczegóły: s10_transactions/Tx03ProxyPitfalls.
    //
    // PUŁAPKA: breakpoint w kodzie wykonywanym przy starcie, a uruchomienie przez Run (zwykłe ▶). Dlaczego? Bez Debug
    //   program po prostu przeleci przez linię. Upewnij się, że nad oknem uruchamiania jest tryb Debug (owad).
    //
    // DOBRA PRAKTYKA: najpierw ustaw breakpoint w SWOJEJ metodzie (kontroler, serwis), a krok po kroku wchodź do Springa
    //   tylko wtedy, gdy musisz. Dlaczego? Kod Springa jest ogromny, a ramek jest kilkadziesiąt — łatwo zgubić wątek.

    // =================================================================================================
    // 4. LOGI
    // =================================================================================================
    //
    // Boot loguje przez SLF4J i Logback (dwie biblioteki: SLF4J to interfejs, Logback — silnik zapisu), domyślnie od poziomu
    // INFO. Poziomy od najbardziej gadatliwego: TRACE, DEBUG, INFO, WARN, ERROR. Poziom ustawia się właściwością
    // logging.level.<nazwa loggera>, gdzie nazwa to zwykle pakiet lub klasa:
    //
    //   logging.level.s00_start.start08_debugging=DEBUG   → wpisy DEBUG tylko z tego pakietu (i podpakietów)
    //   logging.level.org.springframework=DEBUG           → wpisy DEBUG całego Springa (BARDZO dużo, tysiące linii)
    //   logging.level.root=WARN                           → wycisza wszystko poniżej WARN
    //
    // Test packageLogLevel uruchamia tę samą aplikację dwa razy: bez właściwości wpisu DEBUG nie ma, z właściwością jest.
    // Właściwość podajesz w application.properties, w argumentach (--logging.level.s00_start=DEBUG), w zmiennej
    // środowiskowej albo w teście: @SpringBootTest(properties = "logging.level.s00_start.start08_debugging=DEBUG").
    //
    // Raport warunków autokonfiguracji: uruchom aplikację z argumentem --debug (albo właściwością debug=true). Boot
    // wypisze CONDITIONS EVALUATION REPORT z sekcjami „Positive matches” (co się włączyło i dlaczego), „Negative matches”
    // (co się NIE włączyło i dlaczego) oraz „Exclusions” i „Unconditional classes” (test conditionsReport). To najlepszy
    // sposób, by sprawdzić, czemu Boot stworzył albo pominął jakiś bean (szczegóły: s03_boot_basics/Boot01AutoConfiguration).
    // Gdy start się nie udał przy działającej autokonfiguracji, Boot dopisuje wprost:
    //   „Error starting ApplicationContext. To display the condition evaluation report re-run your application with
    //   'debug' enabled.”
    //
    // PUŁAPKA: włączenie logging.level.org.springframework=DEBUG „na wszelki wypadek” i szukanie igły w tysiącach linii.
    //   Dlaczego? Utoniesz w szumie. Zacznij od DEBUG dla JEDNEGO pakietu (swojego albo konkretnej biblioteki), a szerszy
    //   zakres włącz dopiero, gdy wiesz, czego szukasz.
    //
    // PUŁAPKA: zostawienie DEBUG w application.properties po zakończeniu debugowania. Dlaczego? Logi na poziomie DEBUG potrafią
    //   ujawnić dane wrażliwe i znacząco spowolnić aplikację. Poziom DEBUG ustawiaj tymczasowo (argument lub test).
    //
    // DOBRA PRAKTYKA: do ustalania „co się dzieje” używaj loggera (poziom DEBUG), a nie System.out.println. Dlaczego? Logger
    //   pokazuje czas, wątek i nazwę klasy, można go wyłączyć jedną właściwością i zostaje w kodzie bez szkody dla produkcji.

    // =================================================================================================
    // 5. BŁĄD ŚRODOWISKA A BŁĄD KODU
    // =================================================================================================
    //
    // Nie każdy czerwony komunikat oznacza błąd w Twoim kodzie. Zanim zaczniesz zmieniać klasy, zapytaj: „czy ten sam kod
    // działa gdzie indziej?”. Trzy klasyczne błędy ŚRODOWISKA (nic w kodzie nie jest zepsute):
    //
    //   „error: release version 21 not supported”   (kompilacja)
    //       Maven działa na starszym JDK (np. 17), a projekt wymaga Javy 21+. Napraw: ustaw JAVA_HOME na JDK 21 lub
    //       nowszy (kurs zaleca 25) i uruchom ponownie; w IntelliJ: File → Project Structure → SDK.
    //   „Could not self-attach to current VM”   (testy z Mockito)
    //       Mockito próbuje podłączyć się do działającej JVM, a system (nowe JDK, antywirus) mu na to nie pozwala.
    //       Ten kurs konfiguruje Mockito jako agenta Javy w parent pom.xml (argLine), więc uruchamiaj testy przez mvnw; ▶ w
    //       IntelliJ też przekazuje ten argLine (Settings → Build Tools → Maven → Running Tests).
    //   „Web server failed to start. Port 8080 was already in use.”   (start aplikacji)
    //       Inny proces (np. poprzednio uruchomiona aplikacja) zajmuje port. Napraw: zatrzymaj tamten proces albo ustaw
    //       inny port (server.port=8081). Test portAlreadyInUse zajmuje port prawdziwym gniazdem i pokazuje cały komunikat.
    //
    // Klasa ErrorCatalog w tym pakiecie rozpoznaje takie komunikaty po fragmencie tekstu i mówi, czy to błąd środowiska
    // (ENVIRONMENT), czy kodu (CODE) — brak beana, dwóch kandydatów i brak właściwości to błędy kodu/konfiguracji.
    //
    // PUŁAPKA: „naprawianie” kodu przy błędzie środowiska. Dlaczego? Dopisując @Primary czy zmieniając klasy przy zajętym
    //   porcie, nic nie naprawisz, a możesz zepsuć działający kod. Najpierw ustal kategorię błędu.
    //
    // PUŁAPKA: ucinanie komunikatu przy kopiowaniu do wyszukiwarki lub pytaniu o pomoc. Dlaczego? Wskazówka często jest w
    //   ostatnim „Caused by” albo w sekcji Action, a nie w pierwszej linii.
    //
    // DOBRA PRAKTYKA: zawsze podawaj (sobie i innym) trzy rzeczy: pełny tekst błędu, wersję JDK (java -version) i polecenie,
    //   które go wywołało. Dlaczego? Połowa „błędów Springa” to błędy środowiska (zła wersja Javy, zajęty port, stary build).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Błąd startu: blok APPLICATION FAILED TO START → Description (co) + Action (co zrobić); wynik FailureAnalyzera.
     *   • Gdy brak analizatora: „Application run failed” i stos; wtedy czytaj OSTATNI „Caused by” (pierwotna przyczyna).
     *   • Typowe: brak beana (NoSuchBeanDefinition), dwóch kandydatów (NoUniqueBeanDefinition), brak właściwości
     *     (Could not resolve placeholder), zajęty port (PortInUseException).
     *   • Test: „Failed to load ApplicationContext” (z przyczyną) → kolejne testy „failure threshold (1) exceeded” (bez
     *     przyczyny, tylko echo). Szukaj PIERWSZEGO błędu.
     *   • Debugger: Debug (owad), breakpoint, F8/F7/Shift+F8/F9, Evaluate Expression (Alt+F8), warunek, breakpoint na
     *     wyjątku ustawiaj na klasie PRZYCZYNY; ramki $$SpringCGLIB$$ to proxy.
     *   • Logi: logging.level.pakiet=DEBUG (wąsko!), --debug / debug=true → raport warunków autokonfiguracji.
     *   • Środowisko: „release version 21 not supported” (JAVA_HOME), „Could not self-attach to current VM” (agent Mockito
     *     w parent pom), „Port 8080 was already in use” (zatrzymaj proces / inny port).
     *
     * PYTANIA KONTROLNE:
     *   1. W jakiej kolejności czytasz łańcuch „Caused by” i dlaczego?
     *   2. Co się stanie? Aplikacja ma dwa beany typu Notifier (emailNotifier i smsNotifier), a OrderService w konstruktorze
     *      prosi o jeden Notifier. Co zobaczysz przy starcie i jaką radę da Boot?
     *   3. ZNAJDŹ BŁĄD: widzisz w teście tylko „ApplicationContext failure threshold (1) exceeded...” i próbujesz naprawić
     *      błąd, podnosząc próg. Co jest nie tak i gdzie szukać przyczyny?
     *   4. Dlaczego breakpoint na wyjątku ustawiasz na klasie przyczyny (np. IllegalStateException), a nie na
     *      BeanCreationException?
     *   5. Co się stanie? Uruchomisz tę samą aplikację z właściwością logging.level.s00_start.start08_debugging=DEBUG i bez
     *      niej. Czym różni się wynik?
     *   6. ZNAJDŹ BŁĄD: „Port 8080 was already in use — poprawię kontroler i zmienię mapowanie adresu.” Co jest nie tak?
     *   7. Co zrobisz, gdy Maven wypisze „release version 21 not supported”, mimo że kod jest poprawny?
     *   8. Co zobaczysz po uruchomieniu aplikacji z argumentem --debug i do czego tego użyjesz?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Start08DebuggingExercisesTest (rozwiązania: s00_start.solutions.start08_debugging)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć PIERWOTNĄ przyczynę błędu, czyli ostatni wyjątek w łańcuchu {@code getCause()}.
     * Wyjątek bez przyczyny jest własną pierwotną przyczyną. Napisz pętlę sam (Spring ma gotową
     * {@code NestedExceptionUtils.getMostSpecificCause}, ale chodzi o to, żebyś zrozumiał mechanizm).
     * Podpowiedź: dopóki {@code getCause()} nie jest null, przesuwaj się w głąb.
     */
    static Throwable exercise1(Throwable failure) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /*
     * ĆWICZENIE 2 (średnie): napraw celowo zepsutą konfigurację w pliku ExerciseConfig.java (ten pakiet). Kontekst nie
     * startuje, bo są DWA beany typu Notifier, a OrderService chce jednego. Po naprawie kontekst ma się uruchomić, a
     * OrderService ma dostać powiadamiacz SMS — bez zmiany konstruktora OrderService.
     * Podpowiedź: przeczytaj komunikat z Action (sekcja 1). Są co najmniej trzy sposoby: @Primary na jednym beanie,
     * @Qualifier przy parametrze albo nazwa parametru zgodna z nazwą beana. Test: Start08DebuggingExercisesTest
     * (ApplicationContextRunner — bez adnotacji skanowania, uruchamia tylko ExerciseConfig).
     */

    /*
     * ĆWICZENIE 3 (trudniejsze): dopisz własny FailureAnalyzer — uzupełnij metodę analyze w StockFailureAnalyzer.java
     * (ten pakiet). Gdy w łańcuchu przyczyn jest OutOfStockException, zwróć FailureAnalysis z opisem zawierającym SKU
     * (cause.getSku()), radą (Action) i przyczyną. Dla innych wyjątków analizator ma nic nie zwracać (null) — zadba o to
     * klasa bazowa AbstractFailureAnalyzer. Rejestracja w prawdziwej aplikacji: plik META-INF/spring.factories z wpisem
     * org.springframework.boot.diagnostics.FailureAnalyzer=pełna.nazwa.StockFailureAnalyzer (klucz sprawdzony w pliku
     * spring.factories samego Boota). My NIE rejestrujemy analizatora: plik spring.factories dotyczyłby wtedy całego modułu
     * i wszystkich lekcji, więc test wywołuje analizator bezpośrednio.
     */

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Od dołu — od OSTATNIEGO „Caused by”. Wyjątki na wierzchu to opakowania, które Spring dokłada po drodze
     *      („Error creating bean...”); pierwotna przyczyna jest na dnie łańcucha. Testy missingBean, twoCandidates i
     *      missingProperty sprawdzają to uruchomieniem.
     *   2. Aplikacja się nie uruchomi: blok APPLICATION FAILED TO START z Description „Parameter 0 of method orderService in
     *      ...TwoCandidates required a single bean, but 2 were found:” i listą kandydatów, oraz Action: „Consider marking
     *      one of the beans as @Primary, updating the consumer to accept multiple beans, or using @Qualifier to identify
     *      the bean that should be consumed”. Pierwotna przyczyna: NoUniqueBeanDefinitionException. Sprawdzone
     *      uruchomieniem (testy twoCandidates i twoCandidatesAnalysis).
     *   3. Ten komunikat nie niesie żadnej przyczyny — oznacza tylko, że Spring już raz nie załadował kontekstu z tą
     *      konfiguracją i pomija kolejne próby. Podniesienie progu (spring.test.context.failure.threshold) tylko powtórzy
     *      ten sam błąd więcej razy. Przyczyny szukaj w PIERWSZYM nieudanym teście („Failed to load ApplicationContext”,
     *      ostatni „Caused by”). Sprawdzone uruchomieniem (test contextFailureThreshold: pierwszy błąd ma przyczynę
     *      NoSuchBeanDefinitionException, drugi nie ma przyczyny w ogóle).
     *   4. Opakowanie (BeanCreationException) powstaje na końcu, w kodzie Springa, po tym, jak coś już poszło źle — stanie
     *      tam niczego nie wyjaśni. Breakpoint na przyczynie zatrzymuje program w chwili, gdy błąd naprawdę zaszedł, z
     *      Twoim kodem na stosie. Sprawdzone: test wrappedException pokazuje łańcuch BeanCreationException →
     *      BeanInstantiationException → IllegalStateException.
     *   5. Bez właściwości wpis DEBUG się nie pojawi (są tylko wpisy INFO i wyższe), z właściwością w wyjściu jest wiersz
     *      z poziomem DEBUG i tekstem „[Start08] wpis DEBUG: tworzę bean diagnosticMarker”. Sprawdzone uruchomieniem (test
     *      packageLogLevel).
     *   6. Zajęty port to błąd ŚRODOWISKA (inny proces trzyma port), nie kodu; zmiana kontrolera nic nie da. Zatrzymaj
     *      tamten proces albo ustaw inny port (server.port=8081). Test portAlreadyInUse: komunikat „Web server failed to
     *      start. Port N was already in use.”.
     *   7. To błąd środowiska: Maven działa na starszym JDK. Ustaw JAVA_HOME na JDK 21 lub nowszy (kurs zaleca 25) i
     *      uruchom ponownie (w IntelliJ sprawdź SDK projektu). Kodu nie zmieniasz.
     *   8. Raport CONDITIONS EVALUATION REPORT z sekcjami „Positive matches” i „Negative matches”: pokazuje, które
     *      autokonfiguracje się włączyły i dlaczego, a które pominięto i z jakiego powodu — używasz go, gdy bean, którego
     *      się spodziewasz, nie powstał (albo powstał nieoczekiwany). Sprawdzone uruchomieniem (test conditionsReport;
     *      bez --debug raportu nie ma).
     */
    // </editor-fold>
}

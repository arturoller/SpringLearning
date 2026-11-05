package s03_boot_basics.boot04_starters;

import java.util.List;

/**
 * <pre>
 * TEMAT: Startery Spring Boota — jedna zależność zamiast dwudziestu, i skąd Boot „wie”, co skonfigurować
 *        (starter = zestaw zależności pod jedną nazwą; BOM = lista wersji bibliotek; classpath = ścieżka klas;
 *         DevTools = narzędzia programisty; module = moduł)
 *
 * W SKRÓCIE:
 *   Starter to „pusty” jar: nie ma w nim ani jednej klasy, tylko plik pom z listą zależności. Gdy dopiszesz
 *   {@code spring-boot-starter-jdbc}, Maven ściąga spring-jdbc, pulę HikariCP i moduł autokonfiguracji. Autokonfiguracja
 *   (Boot01AutoConfiguration) zagląda na classpath, widzi te klasy i tworzy beany. Czyli: DODAJESZ BIBLIOTEKĘ →
 *   BOOT TO ZAUWAŻA → KONFIGURUJE. Wersje wszystkich bibliotek pilnuje BOM, dlatego w pom.xml nie piszesz numerów.
 *   W Boot 4 startery są zmodularyzowane i mają nazwy jak technologie (webmvc, restclient, flyway…).
 *
 * ANALOGIA: zestaw „ZRÓB TO SAM” w sklepie budowlanym.
 *   Zamiast kupować osobno deski, wkręty, klej i lakier (zależności), bierzesz jedno pudełko „regał” (starter) — jest
 *   w nim wszystko, co pasuje do siebie. Na pudełku jest lista zgodnych wersji (BOM), więc nie sprawdzasz, czy wkręty
 *   pasują do desek. Majster (Boot) widząc pudełko „regał” w koszyku wie, że trzeba zmontować regał.
 *
 * JAK TO DZIAŁA:
 *   1. W pom.xml dopisujesz {@code spring-boot-starter-jdbc} (bez {@code <version>}).
 *   2. Wersję podaje rodzic: spring-boot-starter-parent → spring-boot-dependencies (BOM, czyli spis wersji).
 *   3. Maven ściąga starter i jego zależności przechodnie (transitive — zależności zależności).
 *   4. Na classpath pojawiają się klasy: DataSource, HikariDataSource, JdbcTemplate…
 *   5. Przy starcie Boot sprawdza warunki @ConditionalOnClass (Boot01AutoConfiguration) — klasy są → beany powstają.
 *   6. Typ aplikacji też wynika z classpath: są klasy serwletów → aplikacja webowa; nie ma → zwykły program.
 *
 * SŁÓWKA:
 *   starter = zestaw zależności; dependency = zależność; transitive dependency = zależność przechodnia;
 *   BOM (bill of materials) = lista wersji kompatybilnych bibliotek; parent = rodzic (projekt dziedziczony);
 *   classpath = ścieżka klas; optional = opcjonalna (nie przechodzi do projektów zależnych); restart = ponowne
 *   uruchomienie; class loader = ładowacz klas; live reload = odświeżanie przeglądarki po zmianie;
 *   deprecated = przestarzały (do wycofania).
 *
 * ZOBACZ TEŻ: s03_boot_basics/Boot01AutoConfiguration (warunki @ConditionalOnClass, FilteredClassLoader),
 *             s03_boot_basics/Boot02WritingAutoConfiguration (własna autokonfiguracja — druga połowa własnego startera),
 *             s03_boot_basics/Boot03SpringApplication (typ aplikacji NONE/SERVLET/REACTIVE),
 *             s02_configuration/Cfg05Environment (ustawienia, które startery czytają).
 * </pre>
 */
public final class Boot04Starters {

    private Boot04Starters() {
    }

    // =================================================================================================
    // 1. CZYM JEST STARTER — I CO ZMIENIŁO SIĘ W BOOT 4
    // =================================================================================================
    //
    // Starter to zwykły artefakt Mavena (groupId org.springframework.boot), w którym NIE MA kodu. W Boot 4.1.1 jar
    // spring-boot-starter-jdbc zawiera tylko trzy pliki: MANIFEST.MF, LICENSE.txt i NOTICE.txt (sprawdzone). Cała treść
    // to jego pom.xml — lista zależności:
    //
    //   spring-boot-starter-jdbc
    //     ├─ spring-boot-starter        (rdzeń: logowanie, autokonfiguracja, YAML)
    //     ├─ spring-boot-jdbc           (autokonfiguracje JDBC — DataSourceAutoConfiguration…)
    //     └─ HikariCP                   (pula połączeń)
    //
    // Zobacz drzewo zależności własnego projektu:
    //   ./mvnw -pl s03-boot-basics dependency:tree         (Windows: mvnw.cmd -pl s03-boot-basics dependency:tree)
    // Fragment dla tego modułu (Boot 4.1.1; wcięcia = „kto kogo ściągnął”):
    //   +- org.springframework.boot:spring-boot-starter:jar:4.1.1:compile
    //   |  +- org.springframework.boot:spring-boot-starter-logging:jar:4.1.1:compile   ← logback, slf4j
    //   |  \- org.springframework.boot:spring-boot-autoconfigure:jar:4.1.1:compile
    //   +- org.springframework.boot:spring-boot-starter-jdbc:jar:4.1.1:compile
    //   |  +- org.springframework.boot:spring-boot-jdbc:jar:4.1.1:compile             ← DataSourceAutoConfiguration
    //   |  \- com.zaxxer:HikariCP:jar:7.0.2:compile                                    ← pula połączeń
    //   +- com.h2database:h2:jar:2.4.240:runtime                                       ← baza w pamięci
    //   +- org.springframework.boot:spring-boot-devtools:jar:4.1.1:compile (optional)  ← sekcja 4
    //   \- org.springframework.boot:spring-boot-starter-test:jar:4.1.1:test             ← JUnit, AssertJ, Mockito
    // Jak czytać: na końcu każdej linii jest zakres (scope): compile = potrzebne od kompilacji, runtime = dopiero
    // przy uruchomieniu, test = tylko w testach. W IntelliJ to samo zobaczysz w oknie Maven (gałąź Dependencies) —
    // wybierz moduł s03-boot-basics i rozwiń zależności.
    //
    // BOOT 4 vs STARE TUTORIALE — startery są ZMODULARYZOWANE. W Boot 3 był jeden wielki jar autokonfiguracji,
    // w Boot 4 autokonfiguracje leżą w modułach technologii (spring-boot-jdbc, spring-boot-webmvc…), a startery
    // nazywają się jak technologia. Dodatkowo każda technologia ma własny starter TESTOWY (np. webmvc-test).
    // Najważniejsze zmiany (sprawdzone w BOM spring-boot-dependencies 4.1.1 i w plikach pom z Maven Central):
    //
    //   Boot 3 (stare tutoriale)                    Boot 4 (ten kurs)
    //   ------------------------------------------  ----------------------------------------------------
    //   spring-boot-starter-web                     spring-boot-starter-webmvc
    //   spring-boot-starter-aop                     spring-boot-starter-aspectj
    //   spring-boot-starter-oauth2-client           spring-boot-starter-security-oauth2-client
    //   spring-boot-starter-oauth2-resource-server  spring-boot-starter-security-oauth2-resource-server
    //   (jeden starter testowy dla wszystkiego)     spring-boot-starter-test + np. -webmvc-test, -data-jpa-test
    //
    // Nowe nazwy "webmvc" i "security-oauth2-…" są w 4.1.1 oznaczone w opisie starych artefaktów jako docelowe
    // ("deprecated in favor of …"), a starter "spring-boot-starter-aop" nie ma już w BOM wcale. Bez zmian nazwy
    // zostały m.in.: spring-boot-starter, -jdbc, -data-jpa, -validation, -security, -actuator, -flyway.
    // Z nowości: spring-boot-starter-restclient (klient HTTP RestClient), spring-boot-starter-webclient.
    // Dla starych projektów istnieją „klasyczne” startery (spring-boot-starter-classic, -test-classic) — ułatwiają
    // migrację, ale w nowym kodzie ich nie używaj.
    //
    // PUŁAPKA: kopiowanie zależności ze starego tutoriala. Dlaczego źle? Stara nazwa albo już nie istnieje (Maven nie
    //   znajdzie artefaktu), albo jest przestarzała i któregoś dnia zniknie, a razem z nią często stary kod (inne pakiety,
    //   inne nazwy klas) przestaje się kompilować. Zawsze sprawdź nazwę w BOM wersji, której używasz.
    //
    // DOBRA PRAKTYKA: dodawaj startery, a nie pojedyncze biblioteki (np. spring-jdbc + HikariCP osobno). Dlaczego?
    //   Starter to zestaw przetestowany razem i zgodny z autokonfiguracją; ręcznie złożony zestaw łatwo rozjedzie się
    //   wersjami albo zapomnisz o zależności, bez której autokonfiguracja po cichu się nie włączy.
    //
    // PYTANIE REKRUTACYJNE: czym jest starter Spring Boota i czym różni się od autokonfiguracji?
    //   (starter = zależności, bez kodu; autokonfiguracja = klasy z beanami włączane warunkami; razem tworzą „dodaj
    //   zależność i działa”)

    // =================================================================================================
    // 2. JAK BOOT „ZGADUJE” — OBECNOŚĆ KLASY NA CLASSPATH
    // =================================================================================================
    //
    // Boot nie czyta Ci w myślach: patrzy, jakie klasy ma na classpath. Najprostszy ślad to warunek
    // @ConditionalOnClass (Boot01AutoConfiguration), który pod spodem robi to samo co
    //   ClassUtils.isPresent("pełna.nazwa.Klasy", classLoader)       // org.springframework.util.ClassUtils
    // czyli „spróbuj załadować klasę; jest → true, nie ma → false” (bez wyjątku).
    //
    // Wykrywanie TYPU APLIKACJI działa tak samo. W Boot 4.1.1 wynik zwraca publiczna metoda
    // {@code WebApplicationType.deduce()}:
    //   • są klasy serwletów i kontekstu webowego (jakarta.servlet.Servlet…) → SERVLET (np. Tomcat + Spring MVC),
    //   • jest DispatcherHandler (WebFlux), a nie ma DispatcherServlet                → REACTIVE,
    //   • żadnych klas webowych                                                        → NONE (zwykły program).
    // Dlatego dopisanie {@code spring-boot-starter-webmvc} zmienia program, który kończył się po sekundzie, w serwer
    // nasłuchujący na porcie — bez ani jednej linijki kodu.
    //
    // Test sekcji 2 sprawdza (uruchomieniem), że w TYM module:
    //   • klasy H2 i HikariCP oraz JdbcTemplate SĄ (bo starter JDBC i H2),
    //   • klas Tomcata, serwletów i DispatcherServlet NIE MA (nie dodaliśmy startera webowego),
    //   • więc WebApplicationType.deduce() daje NONE,
    //   • przez FilteredClassLoader (Boot01AutoConfiguration) można „odinstalować” bibliotekę na potrzeby testu:
    //     konfiguracja z {@code @ConditionalOnClass(name = "org.h2.Driver")} tworzy bean tylko wtedy, gdy H2 jest widoczne.
    //
    // PUŁAPKA: „dodałem zależność, a nic się nie dzieje”. Dlaczego? Autokonfiguracja zwykle wymaga KILKU klas naraz
    //   (np. DataSource + sterownik bazy). Brakuje jednej → warunek niespełniony i cisza w logach. Odpowiedź ma raport
    //   warunków (--debug, Boot01AutoConfiguration sekcja 4): w Negative matches zobaczysz „did not find required class”.
    //
    // DOBRA PRAKTYKA: dołączaj do projektu tylko startery, które naprawdę są potrzebne. Dlaczego? Każdy starter
    //   zmienia zachowanie CAŁEJ aplikacji (autokonfiguracja włącza się od samej obecności klas). Przykład z tego
    //   kursu: dlatego każdy dział to osobny moduł — security w jednym projekcie zablokowałby wszystkie punkty końcowe.

    // =================================================================================================
    // 3. BOM I WERSJE — DLACZEGO W POM.XML NIE MA {@code <version>}
    // =================================================================================================
    //
    //   <parent>
    //       <groupId>org.springframework.boot</groupId>
    //       <artifactId>spring-boot-starter-parent</artifactId>        ← rodzic
    //       <version>4.1.1</version>                                    ← JEDYNY numer, który wpisujesz
    //   </parent>
    //   ...
    //   <dependency>
    //       <groupId>org.springframework.boot</groupId>
    //       <artifactId>spring-boot-starter-jdbc</artifactId>           ← bez <version>
    //   </dependency>
    //
    // Łańcuch: spring-boot-starter-parent → (jego rodzic) spring-boot-dependencies. W tym drugim pliku jest
    // <dependencyManagement> ze wszystkimi wersjami (setki bibliotek: Spring Framework, Hibernate, Jackson, Hikari,
    // JUnit, H2…). Maven, widząc zależność bez <version>, sięga po numer z tej listy. To jest właśnie BOM.
    // Zysk: Boot 4.1.1 wydano z zestawem bibliotek PRZETESTOWANYM RAZEM (Spring Framework 7.0.x, HikariCP 7.0.2,
    // H2 2.4.240 w tym module). Zmieniasz jedną liczbę w <parent> — zmieniasz cały zestaw.
    //
    // Gdy musisz nadpisać wersję jednej biblioteki (np. pilna poprawka bezpieczeństwa), użyj właściwości z BOM,
    // np. {@code <h2.version>…</h2.version>} w <properties>, a nie wpisywania <version> w każdej zależności.
    //
    // Test sekcji 3 odczytuje wersje z jarów (przez klasy {@code SpringBootVersion} i {@code SpringVersion}):
    // Spring Boot zaczyna się od "4.", a Spring Framework od "7." — Boot 4 jest zbudowany na Frameworku 7.
    //
    // PUŁAPKA: wpisanie <version> przy zależności zarządzanej przez BOM „dla pewności”. Dlaczego źle? Twoja liczba
    //   wygrywa z BOM, więc po następnej aktualizacji Boota ta jedna biblioteka zostaje w starej wersji, niezgodna z resztą —
    //   błędy typu NoSuchMethodError pojawiają się dopiero w działaniu.
    //
    // DOBRA PRAKTYKA: aktualizuj Boota zmianą wersji w <parent> i czytaj notatki wydania (release notes) i przewodnik
    //   migracji. Dlaczego? Zmiany głównych wersji (3 → 4) przemianowują klasy, pakiety i startery (sekcja 1) —
    //   lepiej przeczytać listę niż zgadywać po błędach kompilacji.

    // =================================================================================================
    // 4. DEVTOOLS — SZYBKA PĘTLA PROGRAMOWANIA
    // =================================================================================================
    //
    // Dependency: {@code spring-boot-devtools}, w pom.xml tego modułu z {@code <optional>true</optional>}.
    // Co robi (tylko na komputerze programisty):
    //   • SZYBKI RESTART. DevTools ładuje klasy w DWÓCH ładowaczach klas (class loader): „bazowy” trzyma biblioteki,
    //     które się nie zmieniają (jary Springa, H2…), a „restartowy” — Twój kod (target/classes). Po zmianie plików
    //     w projekcie wyrzuca TYLKO ładowacz restartowy i buduje kontekst od nowa. Biblioteki nie są ładowane drugi
    //     raz, więc restart jest dużo szybszy niż zimny start.
    //   • LIVERELOAD. Wbudowany serwer odświeża stronę w przeglądarce (z rozszerzeniem LiveReload) po restarcie.
    //   • USTAWIENIA DEWELOPERSKIE. Ustawia domyślne wartości wygodne przy pracy, np. wyłącza pamięć podręczną
    //     szablonów, żeby zmiana HTML była widoczna od razu (można wyłączyć:
    //     {@code spring.devtools.add-properties=false}; sam restart: {@code spring.devtools.restart.enabled=false}).
    //   • przyjazne zachowanie bazy w pamięci przy restarcie (zamyka ją porządnie, żeby nie wisiały połączenia).
    //
    // JAK URUCHOMIĆ RESTART W INTELLIJ. DevTools reaguje na zmianę skompilowanych klas na classpath, a IntelliJ
    // domyślnie nie kompiluje sam. Masz dwie drogi: (a) po zmianie kodu użyj polecenia „Build Project” (skrót
    // widoczny w menu Build), albo (b) włącz automatyczne budowanie: w ustawieniach (Settings) w sekcji
    // Build, Execution, Deployment → Compiler zaznacz opcję o nazwie „Build project automatically”, a w Advanced
    // Settings poszukaj opcji o „auto-make” (pozwala budować automatycznie także wtedy, gdy aplikacja działa).
    // Nazwy opcji mogą się nieco różnić między wersjami IntelliJ — szukaj w ustawieniach po słowach „auto-make”
    // i „Build project automatically”.
    //
    // DLACZEGO TO NIE TRAFIA NA PRODUKCJĘ?
    //   1. {@code <optional>true</optional>} — nie przechodzi dalej do projektów, które zależą od Twojego.
    //   2. DevTools SAM się wyłącza, gdy aplikację uruchomiono jako spakowany jar ({@code java -jar app.jar}) albo
    //      testy (JUnit i klasy testowe Boota są rozpoznawane po śladzie stosu wątku). Metoda
    //      {@code DevToolsEnablementDeducer.shouldEnable(Thread)} zwraca wtedy false.
    //   3. Plugin Mavena spring-boot:repackage domyślnie wyklucza DevTools z końcowego jara (w kursie nie pakujemy jara).
    //   Dlaczego tak ostrożnie? Restart po zmianie plików i otwarty serwer LiveReload na produkcji to dziura i wyciek
    //   zasobów.
    //
    // Test sekcji 4: klasa Restarter z DevTools JEST na classpath (zależność działa), ale w teście
    // {@code DevToolsEnablementDeducer.shouldEnable(Thread.currentThread())} daje false, a uruchomiona aplikacja
    // nie ma źródła ustawień o nazwie "devtools" (czyli DevTools niczego nie zmienił). W logu testów zobaczysz
    // linię „Restart disabled due to context in which it is running”.
    //
    // PUŁAPKA: spodziewanie się restartu w teście albo przy ▶ w IntelliJ bez przebudowy. Dlaczego? W testach DevTools
    //   jest wyłączony celowo (restarty w środku testu byłyby koszmarem), a przy zwykłym ▶ nic się nie zmieni, dopóki
    //   IntelliJ nie skompiluje zmienionych klas. „Zmieniłem kod, a restart nie wchodzi” zwykle znaczy: brak budowania.
    //
    // DOBRA PRAKTYKA: DevTools dodawaj jako optional (albo w Gradle jako developmentOnly). Dlaczego? Jest to narzędzie
    //   dewelopera; chcesz je mieć lokalnie, ale nie w artefakcie, który jedzie na serwer.

    // =================================================================================================
    // 5. WŁASNY STARTER — KONWENCJA NAZW
    // =================================================================================================
    //
    // Gdy piszesz bibliotekę dla wielu projektów (np. firmowe cenniki z Boot02WritingAutoConfiguration), dzielisz ją
    // zwykle na DWA artefakty:
    //   acme-spring-boot-autoconfigure   — klasy: @AutoConfiguration, @ConfigurationProperties, plik
    //                                      META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    //   acme-spring-boot-starter         — pusty artefakt (sam pom), który ciągnie autoconfigure + potrzebne biblioteki
    // (małe biblioteki łączą oba w jednym artefakcie — to też jest w porządku).
    // KONWENCJA NAZW: nazwy zaczynające się od {@code spring-boot-starter-} są ZAREZERWOWANE dla projektu Spring Boot.
    // Własny starter nazywasz {@code nazwa-spring-boot-starter} (nazwa firmy/biblioteki na początku). Dlaczego?
    // Gdybyś użył spring-boot-starter-xyz, ludzie pomyśleliby, że to oficjalny starter, a przyszły oficjalny o tej samej
    // nazwie wszedłby Ci w drogę. Test sekcji 5 nie tworzy nowego modułu — sprawdza tylko regułę przedrostka;
    // klasyfikację nazw ćwiczysz w ćwiczeniu 3, a budowę autokonfiguracji ćwiczysz w Boot02WritingAutoConfiguration.
    //
    // DOBRA PRAKTYKA: w starterze nie umieszczaj kodu — tylko zależności. Dlaczego? Kod należy do modułu
    //   autokonfiguracji; starter ma być prostą „paczką”, którą da się zrozumieć jednym spojrzeniem na pom.xml.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Starter = pusty jar z pom.xml, który ciągnie zestaw zależności; autokonfiguracja robi resztę.
     *   • Boot 4: startery nazywają się jak technologia (webmvc, jdbc, data-jpa, flyway, restclient…) i mają
     *     osobne startery testowe (webmvc-test…); web → webmvc, aop → aspectj, oauth2-client → security-oauth2-client.
     *   • Drzewo zależności: ./mvnw -pl <moduł> dependency:tree.
     *   • Boot zgaduje z classpath: @ConditionalOnClass / ClassUtils.isPresent; typ aplikacji: WebApplicationType.deduce().
     *   • Wersje z BOM (spring-boot-starter-parent → spring-boot-dependencies): zależność bez <version>.
     *   • DevTools: szybki restart (dwa ładowacze klas), LiveReload, ustawienia deweloperskie; optional; wyłączony
     *     w jar-ze i w testach. IntelliJ restartuje po przebudowie projektu.
     *   • Własny starter: nazwa-spring-boot-starter + nazwa-spring-boot-autoconfigure; spring-boot-starter-* tylko dla Springa.
     *
     * PYTANIA KONTROLNE:
     *   1. Co jest w jarze startera i czym różni się starter od autokonfiguracji?
     *   2. Dlaczego zależność spring-boot-starter-jdbc w pom.xml nie ma {@code <version>}? Skąd Maven bierze numer?
     *   3. Co się stanie? Dopisujesz do tego modułu spring-boot-starter-webmvc. Co zmieni się w typie aplikacji
     *      i w zachowaniu programu?
     *   4. ZNAJDŹ BŁĄD: w tutorialu jest zależność {@code spring-boot-starter-web}, a Ty pracujesz na Boot 4.
     *      Czemu to zły pomysł i czym ją zastąpić?
     *   5. Co się stanie? W pom.xml wpisujesz przy HikariCP własne {@code <version>}, starsze niż w BOM.
     *   6. Dlaczego DevTools nie działa w testach i w uruchomionym jar-ze? Po co jest {@code optional}?
     *   7. Zmieniłeś kod, a DevTools nie zrestartował aplikacji w IntelliJ. Co sprawdzisz najpierw?
     *   8. Jak nazwiesz własny starter firmy „Acme” i czemu nie spring-boot-starter-acme?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Boot04StartersExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwę startera w Boot 4 dla nazwy z Boot 3. Znasz cztery zmiany:
     * {@code spring-boot-starter-web} → {@code spring-boot-starter-webmvc}, {@code spring-boot-starter-aop} →
     * {@code spring-boot-starter-aspectj}, {@code spring-boot-starter-oauth2-client} →
     * {@code spring-boot-starter-security-oauth2-client}, {@code spring-boot-starter-oauth2-resource-server} →
     * {@code spring-boot-starter-security-oauth2-resource-server}. Każdą inną nazwę zwróć bez zmian.
     * Podpowiedź: sekcja 1 — {@code Map.of(...)} i {@code getOrDefault}.
     */
    static String exercise1Boot4StarterName(String boot3ArtifactId) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć posortowaną listę nazw klas z podanych, których NIE MA w podanym class loaderze
     * (czyli brakujących na classpath). Gdy wszystkie są — pusta lista.
     * Podpowiedź: sekcja 2 — {@code ClassUtils.isPresent(nazwa, classLoader)} (org.springframework.util.ClassUtils),
     * potem {@code filter}, {@code sorted}, {@code toList}.
     */
    static List<String> exercise2MissingClasses(ClassLoader classLoader, String... classNames) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): sklasyfikuj nazwę artefaktu startera i zwróć jeden z trzech tekstów:
     * {@code "oficjalny"} — zaczyna się od {@code spring-boot-starter} (zarezerwowane dla Springa),
     * {@code "własny"} — kończy się na {@code -spring-boot-starter} i nie zaczyna od {@code spring-},
     * {@code "niepoprawny"} — wszystko inne. Przykłady: spring-boot-starter-jdbc → oficjalny,
     * acme-spring-boot-starter → własny, spring-boot-starter-acme → oficjalny (tak widzi to konwencja — dlatego
     * nie wolno tak nazywać własnych!), acme-starter → niepoprawny, spring-acme-spring-boot-starter → niepoprawny.
     * Podpowiedź: sekcja 5 — {@code startsWith}, {@code endsWith}.
     */
    static String exercise3ClassifyStarterName(String artifactId) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): z jednej linii wyniku {@code dependency:tree} wyczytaj ZAKRES (scope) zależności.
     * Linia ma ozdobniki drzewa na początku ({@code +- }, {@code \\- }, {@code |  }) i bywa zakończona
     * {@code " (optional)"}. Przykłady: {@code "|  +- org.springframework.boot:spring-boot-jdbc:jar:4.1.1:compile"}
     * → {@code "compile"}; {@code "+- com.h2database:h2:jar:2.4.240:runtime"} → {@code "runtime"};
     * {@code "\\- org.springframework.boot:spring-boot-devtools:jar:4.1.1:compile (optional)"} → {@code "compile"}.
     * Podpowiedź: sekcja 1 — usuń ozdobniki z początku ({@code replaceFirst} z wyrażeniem regularnym), odetnij
     * {@code " (optional)"}, potem {@code split(":")} i ostatni element.
     */
    static String exercise4ScopeOf(String treeLine) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. W jarze startera nie ma klas (tylko manifest, licencja i notatka); liczy się jego pom.xml z listą zależności.
     *      Autokonfiguracja to klasy z beanami włączane warunkami. Starter DOSTARCZA biblioteki, autokonfiguracja
     *      je KONFIGURUJE.
     *   2. Numer bierze Maven z BOM (<dependencyManagement> w spring-boot-dependencies), który dziedziczysz przez
     *      spring-boot-starter-parent. Jedyna wersja, którą wpisujesz, to wersja Boota w <parent>.
     *   3. Na classpath pojawią się klasy serwletów i Tomcat, więc WebApplicationType.deduce() da SERVLET (zamiast
     *      NONE), a program zacznie działać jak serwer nasłuchujący na porcie, zamiast zakończyć się po starcie.
     *      (W tym module tego nie robimy — dlatego sekcja 2 sprawdza, że takich klas nie ma.)
     *   4. W Boot 4 nazwa docelowa to spring-boot-starter-webmvc; stara jest przestarzała ("deprecated in favor of
     *      spring-boot-starter-webmvc"). Tutorial jest po prostu napisany dla Boot 3.
     *   5. Twoja liczba wygrywa z BOM — HikariCP zostanie w starej wersji, niezgodnej z resztą zestawu (ryzyko
     *      błędów w działaniu, np. NoSuchMethodError). Zamiast tego zmień właściwość wersji z BOM albo zostaw BOM.
     *   6. DevTools wyłącza się sam, gdy rozpozna jar (java -jar) albo test (po śladzie stosu wątku). Pokazuje to test
     *      sekcji 4. optional sprawia, że zależność nie przechodzi do projektów, które zależą od Twojego.
     *   7. Czy projekt został PRZEBUDOWANY. DevTools reaguje na zmianę skompilowanych klas, a IntelliJ domyślnie sam
     *      nie kompiluje — użyj polecenia budowania projektu albo włącz automatyczne budowanie (sekcja 4).
     *   8. acme-spring-boot-starter (i acme-spring-boot-autoconfigure). Przedrostek spring-boot-starter- jest
     *      zarezerwowany dla projektu Spring Boot — własny starter udawałby oficjalny i mógłby kolidować z przyszłym.
     */
    // </editor-fold>
}

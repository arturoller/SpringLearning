package s11_testing.test05_test_configuration;

/**
 * <pre>
 * TEMAT: Konfiguracja testów — @TestConfiguration, @TestPropertySource, @DynamicPropertySource, @ActiveProfiles
 *        i stały zegar (test configuration = konfiguracja tylko dla testów; property source = źródło właściwości;
 *         dynamic = dynamiczny, wyliczany w czasie działania; profile = profil; fixed clock = zegar zatrzymany)
 *
 * W SKRÓCIE:
 *   Test często potrzebuje aplikacji „prawie takiej samej” jak produkcyjna: z zegarem zatrzymanym w znanej chwili,
 *   z inną właściwością, z adresem lokalnej zaślepki zamiast prawdziwego API. Spring daje do tego cztery narzędzia:
 *   {@code @TestConfiguration} (dodatkowe beany tylko dla testów), {@code @TestPropertySource} i
 *   {@code @SpringBootTest(properties)} (stałe właściwości), {@code @DynamicPropertySource} (właściwości znane
 *   dopiero w czasie działania testu, np. losowy port) i {@code @ActiveProfiles} (pliki profili). Każde z nich zmienia
 *   klucz kontekstu — więc zbyt wiele wariantów spowalnia testy.
 *
 * ANALOGIA: próba generalna z rekwizytami.
 *   Spektakl (aplikacja) jest ten sam, ale na próbie zegar na ścianie jest namalowany na stałe na 23:30 (stały
 *   zegar), aktor czyta z kartki inną kwestię (właściwość testu), a zamiast prawdziwego telefonu na scenie stoi atrapa,
 *   której numer reżyser zapisuje dopiero, gdy ją podłączy (@DynamicPropertySource). Rekwizyty przynosisz jawnie —
 *   to, co leży w magazynie teatru, nie powinno samo wjechać na scenę (PUŁAPKA ze zwykłą @Configuration).
 *
 * JAK TO DZIAŁA:
 *   1. {@code @TestConfiguration} to {@code @Configuration}, którą skanowanie komponentów aplikacji POMIJA (filtr
 *      TypeExcludeFilter w {@code @SpringBootApplication}). Zagnieżdżona statyczna klasa {@code @TestConfiguration}
 *      w klasie testowej jest dołączana automatycznie (OPRÓCZ konfiguracji aplikacji); osobną trzeba wskazać
 *      {@code @Import}.
 *   2. Właściwości z testu trafiają do Environment jako źródła o wysokim priorytecie — wygrywają z plikami
 *      {@code application*.properties}. Między sobą (sprawdzone testem) najsilniejsze jest @DynamicPropertySource,
 *      potem @TestPropertySource(properties), potem @SpringBootTest(properties), na końcu plik profilu.
 *   3. {@code @DynamicPropertySource}: statyczna metoda z {@code DynamicPropertyRegistry}; dostawca
 *      ({@code () -> ...}) jest wywoływany dopiero, gdy wartość jest potrzebna — po starcie zaślepki/kontenera.
 *   4. {@code @ActiveProfiles("test05")} włącza profil — Boot wczytuje {@code application-test05.properties}.
 *   5. Wszystko powyżej wchodzi do klucza pamięci podręcznej kontekstów (s11_testing/Test01TestSlices, sekcja 6).
 *
 * SŁÓWKA:
 *   test configuration = konfiguracja testowa; import = dołączyć; primary = główny (wybierany przy kilku kandydatach);
 *   override = nadpisać; property = właściwość; precedence = pierwszeństwo; registry = rejestr; stub = zaślepka;
 *   fixed = zatrzymany, stały; zone = strefa czasowa; profile = profil; dynamic = dynamiczny.
 *
 * ZOBACZ TEŻ: s02_configuration/Cfg01Properties (właściwości), s02_configuration/Cfg03Profiles (profile),
 *             s02_configuration/Cfg05Environment (Environment i źródła właściwości), s01_core_ioc/Ioc05MultipleCandidates
 *             (@Primary), t17_datetime/DateTime04ZonesInstant (strefy i Instant), s11_testing/Test02MockitoBeans (@TestBean),
 *             s11_testing/Test06Testcontainers (@DynamicPropertySource a @ServiceConnection), s13_rest_client/Rc01RestClient.
 * </pre>
 */
public final class Test05TestConfiguration {

    private Test05TestConfiguration() {
    }

    // =================================================================================================
    // 1. @TestConfiguration: ZAGNIEŻDŻONA (SAMA) A OSOBNA (@Import)
    // =================================================================================================
    //
    //   @SpringBootTest
    //   class PromocjaTest {
    //       @TestConfiguration                       // ZAGNIEŻDŻONA statyczna klasa → dołączana automatycznie
    //       static class Zegar { @Bean @Primary Clock blackWeekendClock() { ... } }
    //   }
    //
    //   @SpringBootTest @Import(Test05FixedClockConfiguration.class)   // OSOBNA klasa w src/test → trzeba @Import
    //
    // Zagnieżdżona @TestConfiguration działa OPRÓCZ konfiguracji aplikacji (Test05Application nadal jest znajdowana);
    // zagnieżdżona zwykła @Configuration działa ZAMIAST niej (w kontekście są wtedy tylko jej beany — sprawdzone
    // testem). Osobnej klasy @TestConfiguration skanowanie nie znajdzie (bez @Import jej beana nie ma — sprawdzone
    // testem). Testy: Test05TestConfigurationNestedTest,
    // Test05TestConfigurationImportTest.
    //
    // PUŁAPKA: zwykła @Configuration w src/test/java w pakiecie aplikacji. Dlaczego? Skanowanie komponentów obejmuje
    //   też klasy testowe z tego pakietu — jej beany trafiają do kontekstu KAŻDEGO testu, nawet tych, które o niej nic
    //   nie wiedzą. Na żywo: klasa AccidentallyScannedConfiguration w testach tej lekcji — jej bean
    //   „accidentallyScanned” jest w kontekście testu, który jej nie importuje (sprawdzone testem). Dla testów zawsze
    //   @TestConfiguration.
    //
    // PUŁAPKA: @SpringBootTest(classes = Aplikacja.class) i zagnieżdżona @TestConfiguration. Dlaczego? Spring wykrywa
    //   zagnieżdżone klasy konfiguracji TYLKO wtedy, gdy nie podałeś classes. Z classes = ... zagnieżdżona konfiguracja
    //   jest po cichu pomijana — test dostaje zegar systemowy zamiast zatrzymanego (sprawdzone uruchomieniem i testem).
    //   Rozwiązanie: dopisz ją do classes albo dodaj @Import(TwojaKonfiguracja.class).

    // =================================================================================================
    // 2. STAŁY ZEGAR — CZAS DETERMINISTYCZNY
    // =================================================================================================
    //
    //   Aplikacja:  @Bean Clock clock() { return Clock.systemDefaultZone(); }        // prawdziwe „teraz”
    //   Serwis:     LocalDate.now(clock)                                             // NIGDY LocalDate.now()
    //   Test:       @Bean @Primary Clock blackWeekendClock() {
    //                   return Clock.fixed(Instant.parse("2026-11-28T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
    //               }
    //
    // Test sprawdzający promocję „czarny weekend” daje ten sam wynik dziś, w Wigilię i za 5 lat — bo „dziś” ustala
    // test, nie kalendarz. @Primary: w kontekście są wtedy DWA zegary, a wstrzykiwany jest ten główny. Inne sposoby
    // podmiany: @TestBean Clock clock (s11_testing/Test02MockitoBeans, sekcja 4) albo @MockitoBean.
    //
    // PUŁAPKA: w konfiguracji testowej @Bean o TEJ SAMEJ nazwie co bean aplikacji (Clock clock()). Co się stanie?
    //   Kontekst nie wstaje: BeanDefinitionOverrideException „Invalid bean definition with name 'clock' defined in
    //   …$ClockWithSameName: Cannot register bean definition […] for bean 'clock' since there is already […] bound.”,
    //   a Boot podpowiada: „Consider renaming one of the beans or enabling overriding by setting
    //   spring.main.allow-bean-definition-overriding=true”. Dlaczego? Boot domyślnie ZABRANIA nadpisywania definicji
    //   beanów, żeby przypadkowa kolizja nazw nie zmieniała po cichu działania aplikacji. Zmień nazwę i dodaj @Primary.
    //   Sprawdzone testem.
    //
    // PUŁAPKA: strefa czasowa. Clock.fixed(Instant.parse("2026-11-30T23:30:00Z"), Europe/Warsaw) to w Warszawie już
    //   1 GRUDNIA, 00:30 (zimą Polska ma UTC+1) — promocja się skończyła. Instant (litera Z) to czas UTC; godzinę
    //   lokalną zamieniaj przez ZonedDateTime.of(..., ZoneId.of("Europe/Warsaw")).toInstant().

    // =================================================================================================
    // 3. WŁAŚCIWOŚCI TESTU I ICH PIERWSZEŃSTWO
    // =================================================================================================
    //
    //   @SpringBootTest(properties = "shop.name=...")                // właściwości „przy okazji” adnotacji Boota
    //   @TestPropertySource(properties = "shop.name=...")            // adnotacja Springa (też locations = plik)
    //
    // Gdy kilka źródeł ustawia tę samą właściwość, wygrywa (sprawdzone testem Test05TestConfigurationPropertiesTest):
    //   @DynamicPropertySource  >  @TestPropertySource(properties)  >  @SpringBootTest(properties)  >  plik profilu
    // Każde z nich wygrywa z application.properties i plikami profili.
    //
    // DOBRA PRAKTYKA: w jednym projekcie wybierz jeden sposób na stałe właściwości testów (np. zawsze
    //   @SpringBootTest(properties) albo zawsze @TestPropertySource). Dlaczego? Gdy ta sama właściwość przychodzi
    //   z dwóch adnotacji, czytelnik musi pamiętać tabelkę pierwszeństwa, żeby wiedzieć, która wartość obowiązuje.

    // =================================================================================================
    // 4. @DynamicPropertySource — WARTOŚCI ZNANE DOPIERO W CZASIE DZIAŁANIA
    // =================================================================================================
    //
    //   static final HttpServer RATES_STUB = startStub();     // zaślepka na porcie 0 = losowy wolny port
    //
    //   @DynamicPropertySource
    //   static void exchangeRatesUrl(DynamicPropertyRegistry registry) {
    //       registry.add("shop.exchange.base-url", () -> "http://localhost:" + RATES_STUB.getAddress().getPort());
    //   }
    //
    // Portu zaślepki nie da się wpisać do pliku ani adnotacji — znamy go dopiero po jej starcie. Metoda musi być
    // statyczna (Spring woła ją przed utworzeniem obiektu testu). Test sekcji 4 (Test05TestConfigurationDynamicTest):
    // klient kursów dostał adres zaślepki i odczytał kurs 4.25; kalkulator przeliczył 42.50 zł na 10.00 euro.
    // Klasyczne użycie to adres bazy z kontenera Testcontainers — w Boot 4 zwykle zastępuje je @ServiceConnection
    // (s11_testing/Test06Testcontainers). Od Spring Framework 6.2 istnieje też bean DynamicPropertyRegistrar
    // (rejestracja właściwości z poziomu beana) — w tej lekcji go nie używamy.
    //
    // PUŁAPKA: prawdziwy adres zewnętrznego API w teście. Dlaczego? Test zależy od internetu i cudzego serwera (awaria,
    //   limit zapytań, inne dane jutro) — przestaje być powtarzalny. Zaślepka na localhost odpowiada zawsze tak samo.

    // =================================================================================================
    // 5. @ActiveProfiles — PLIK PROFILU DLA TESTU
    // =================================================================================================
    //
    //   @ActiveProfiles("test05")   →   application-test05.properties: shop.name=Sklep z profilu test05
    //
    // Profil to wygodny sposób na wiele właściwości naraz (np. adres bazy, katalog migracji — jak w lekcji
    // s11_testing/Test04DataJpaTest). Test sekcji 5 sprawdza aktywny profil i wartości z pliku.

    // =================================================================================================
    // 6. WPŁYW NA PAMIĘĆ PODRĘCZNĄ KONTEKSTÓW
    // =================================================================================================
    //
    // Inna zagnieżdżona @TestConfiguration, inny @Import, inne właściwości, inny profil, inna metoda
    // @DynamicPropertySource → inny klucz → OSOBNY kontekst (test sekcji 6: dwie konfiguracje = dwa różne obiekty
    // kontekstu). Każdy wariant to kolejny start aplikacji.
    //
    // DOBRA PRAKTYKA: wspólną konfigurację testów trzymaj w jednym miejscu (np. adnotacja złożona albo klasa bazowa
    //   z @SpringBootTest, @ActiveProfiles i @Import), a wariantów używaj tylko tam, gdzie test naprawdę ich potrzebuje.
    //   Dlaczego? Mniej wariantów = mniej startów kontekstu = szybszy zestaw testów.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @TestConfiguration zagnieżdżona (static) → dołączana sama, OPRÓCZ konfiguracji aplikacji;
     *     osobna klasa → @Import(Klasa.class). Przy @SpringBootTest(classes = ...) zagnieżdżona jest pomijana!
     *   • Zwykła @Configuration w src/test w pakiecie aplikacji jest SKANOWANA — wpada do każdego testu.
     *   • Czas: bean Clock w aplikacji, LocalDate.now(clock) w kodzie, Clock.fixed(...) + @Primary w teście.
     *   • Ta sama nazwa beana w teście i aplikacji → BeanDefinitionOverrideException (nadpisywanie wyłączone).
     *   • Pierwszeństwo: @DynamicPropertySource > @TestPropertySource > @SpringBootTest(properties) > pliki.
     *   • @DynamicPropertySource: static void m(DynamicPropertyRegistry r) { r.add("klucz", () -> wartość); }
     *   • @ActiveProfiles("x") → application-x.properties.
     *   • Każdy wariant konfiguracji = osobny kontekst w pamięci podręcznej.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się zagnieżdżona @TestConfiguration od zagnieżdżonej @Configuration w klasie testowej?
     *   2. Co się stanie? W klasie @TestConfiguration definiujesz @Bean Clock clock(), a aplikacja ma już bean
     *      o nazwie „clock”.
     *   3. Co się stanie? Test ma @SpringBootTest(classes = Test05Application.class) i zagnieżdżoną
     *      @TestConfiguration ze stałym zegarem. Jaki zegar dostanie PromotionService?
     *   4. ZNAJDŹ BŁĄD: klasa FakeMailConfig z adnotacją @Configuration leży w src/test/java w pakiecie aplikacji.
     *      Po jej dodaniu „zepsuły się” testy, które jej nie używają.
     *   5. Ta sama właściwość jest w @SpringBootTest(properties), @TestPropertySource i @DynamicPropertySource.
     *      Która wartość obowiązuje?
     *   6. Dlaczego adres zaślepki podajemy przez @DynamicPropertySource, a nie przez @TestPropertySource?
     *   7. Dlaczego PromotionService bierze datę z Clock, a nie z LocalDate.now()?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — uzupełniasz TESTY w pliku Test05TestConfigurationExercisesTest (src/test/java, ten sam pakiet).
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): w zagnieżdżonej @TestConfiguration zwróć zegar zatrzymany 30.11.2026 o 23:30 CZASU
    //   POLSKIEGO. Podpowiedź: ZonedDateTime.of(2026, 11, 30, 23, 30, 0, 0, ZoneId.of("Europe/Warsaw")).toInstant()
    //   i Clock.fixed(..., ta sama strefa). Uwaga na pułapkę z sekcji 2!
    //
    // ĆWICZENIE 2 (średnie): dodaj do klasy adnotację, która na czas testu ustawia shop.name=Sklep ćwiczeniowy
    //   (wygrywa z plikiem profilu test05). Podpowiedź: sekcja 3.
    //
    // ĆWICZENIE 3 (średnie): w metodzie @DynamicPropertySource zarejestruj shop.exchange.base-url wskazujący na
    //   zaślepkę STUB. Podpowiedź: "http://localhost:" + STUB.getAddress().getPort().
    //
    // ĆWICZENIE 4 (trudniejsze): podmień bean ExchangeRateClient tak, żeby eurRate() zwracało 4.10 — bez żadnego
    //   serwera. Podpowiedź: atrapa z lekcji s11_testing/Test02MockitoBeans (albo @TestBean / @TestConfiguration).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zagnieżdżona @TestConfiguration jest dołączana OPRÓCZ głównej konfiguracji (Test05Application nadal jest
     *      wyszukiwana i użyta); zagnieżdżona zwykła @Configuration jest używana ZAMIAST niej — Boot nie szuka wtedy
     *      @SpringBootConfiguration i nie ma reszty aplikacji.
     *   2. Kontekst nie wstaje: BeanDefinitionOverrideException „Invalid bean definition with name 'clock' …
     *      Cannot register bean definition … for bean 'clock' since there is already … bound.” — Boot domyślnie
     *      zabrania nadpisywania beanów. Zmień nazwę metody i dodaj @Primary. Sprawdzone testem sekcji 2.
     *   3. Zegar systemowy: przy jawnym classes = ... Spring nie wykrywa zagnieżdżonych klas konfiguracji, więc stały
     *      zegar w ogóle nie trafia do kontekstu. Trzeba go dopisać do classes albo dodać @Import. Sprawdzone testem.
     *   4. Zwykła @Configuration w pakiecie aplikacji (także w src/test) jest znajdowana przez skanowanie komponentów
     *      Test05Application — jej beany trafiają do KAŻDEGO kontekstu testów tej aplikacji. Zamień na
     *      @TestConfiguration i dołączaj jawnie (@Import) tam, gdzie jest potrzebna. Sprawdzone testem sekcji 1.
     *   5. Ta z @DynamicPropertySource; bez niej — z @TestPropertySource; a @SpringBootTest(properties) wygrywa tylko
     *      z plikami. Sprawdzone testem sekcji 3.
     *   6. Bo port zaślepki jest losowy (port 0) i znamy go dopiero po jej starcie; adnotacje przyjmują tylko stałe
     *      wartości znane podczas kompilacji.
     *   7. Bo test może wtedy podstawić zegar zatrzymany w znanej chwili — wynik nie zależy od dnia uruchomienia testu.
     *      Z LocalDate.now() test promocji przechodziłby tylko w czarny weekend.
     */
    // </editor-fold>
}

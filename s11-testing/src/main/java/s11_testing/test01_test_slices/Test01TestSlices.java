package s11_testing.test01_test_slices;

/**
 * <pre>
 * TEMAT: Piramida testów, wycinki testowe (test slices), @SpringBootTest i pamięć podręczna kontekstów
 *        (slice = wycinek — test, który ładuje tylko jedną warstwę aplikacji; context = kontekst Springa, czyli
 *         kontener z beanami; context cache = pamięć podręczna kontekstów; web environment = środowisko webowe)
 *
 * W SKRÓCIE:
 *   Nie każdy test potrzebuje całej aplikacji. Logikę bez Springa sprawdzasz zwykłym testem jednostkowym
 *   (milisekundy). Kontroler — wycinkiem {@code @WebMvcTest} (bez bazy). Repozytorium — wycinkiem {@code @DataJpaTest}
 *   (bez weba). Całość — rzadko, przez {@code @SpringBootTest}. Im węższy test, tym szybszy i tym dokładniej wskazuje,
 *   co się zepsuło. Spring dodatkowo PAMIĘTA zbudowane konteksty: klasy testowe o tej samej konfiguracji dostają ten
 *   sam, gotowy kontekst — więc każda „drobna różnica” w konfiguracji testu kosztuje nowy start.
 *
 * ANALOGIA: próby w teatrze.
 *   Próba czytana przy stole (test jednostkowy) — sam tekst, bez sceny, można ją powtarzać co chwilę.
 *   Próba jednej sceny (wycinek) — dekoracje tylko tej sceny, a nieobecnych aktorów zastępują dublerzy (atrapy,
 *   {@code @MockitoBean}). Próba generalna (@SpringBootTest) — cały spektakl ze światłem i kostiumami: najpewniejsza,
 *   ale najdroższa, więc rzadka. A dekoracje, które zostają na scenie dla następnej próby z tym samym planem,
 *   to pamięć podręczna kontekstów.
 *
 * JAK TO DZIAŁA:
 *   1. Adnotacja testowa Springa ({@code @SpringBootTest}, {@code @WebMvcTest}…) zawiera
 *      {@code @ExtendWith(SpringExtension.class)} — rozszerzenie JUnit, które przed testami buduje kontekst i wstrzykuje
 *      beany do pól z {@code @Autowired}.
 *   2. Konfigurację test znajduje SAM: szuka klasy z {@code @SpringBootConfiguration} (zawiera ją
 *      {@code @SpringBootApplication}) w pakiecie testu i W GÓRĘ drzewa pakietów — tu {@code Test01Application}.
 *   3. {@code @SpringBootTest} ładuje wszystko: każdy bean ze skanowania i całą autokonfigurację.
 *   4. Wycinek robi dwie rzeczy: (a) filtruje skanowanie komponentów — zostają tylko typy jego warstwy
 *      (dla @WebMvcTest m.in. {@code @Controller}, {@code @ControllerAdvice}, {@code WebMvcConfigurer}, filtry);
 *      (b) włącza tylko wybrane autokonfiguracje (lista w pliku {@code META-INF/spring/…AutoConfigureWebMvc.imports}
 *      modułu testowego). Czego brakuje — podajesz atrapą {@code @MockitoBean} albo dołączasz przez {@code @Import}.
 *   5. Z adnotacji, właściwości, profili i atrap Spring składa KLUCZ konfiguracji (MergedContextConfiguration —
 *      scalona konfiguracja kontekstu). Ten sam klucz = ten sam kontekst z pamięci podręcznej (cache).
 *
 * SŁÓWKA:
 *   slice = wycinek; unit test = test jednostkowy; integration test = test integracyjny; mock = atrapa;
 *   context = kontekst; cache = pamięć podręczna; hit = trafienie; miss = chybienie; dirty = brudny (do wyrzucenia);
 *   web environment = środowisko webowe; random port = losowy port; defined port = ustalony port; tester = tester;
 *   bootstrap = rozruch; filter = filtr.
 *
 * ZOBACZ TEŻ: t32_junit_mockito/JUnit01Basics (JUnit bez Springa), s00_start/Start04TestingIntro (pierwsze testy),
 *             s07_data_jdbc/Jdbc05Repository (@JdbcTest), s08_data_jpa/Jpa02Repositories (@DataJpaTest),
 *             s11_testing/Test02MockitoBeans (atrapy), s11_testing/Test03MockMvcTester (testy kontrolerów),
 *             s11_testing/Test04DataJpaTest (wycinek JPA dokładniej).
 * </pre>
 */
public final class Test01TestSlices {

    private Test01TestSlices() {
    }

    // =================================================================================================
    // 1. PIRAMIDA TESTÓW: DUŻO MAŁYCH, MAŁO DUŻYCH
    // =================================================================================================
    //
    //            /\          @SpringBootTest — cała aplikacja; kilka testów „czy to wszystko razem działa”
    //           /  \
    //          /----\        wycinki — @WebMvcTest, @DataJpaTest, @JdbcTest, @JsonTest; po kilka na warstwę
    //         /      \
    //        /--------\      testy jednostkowe — czysta Java (+ Mockito); setki, każdy trwa milisekundy
    //
    // Klasy tej lekcji to mała aplikacja w trzech warstwach: ProductController (web) → ProductService (logika) →
    // ProductRepository (Spring Data JPA) → encja Product. Zamiana groszy na złote (ProductDto.from) to czysta
    // logika — test jednostkowy, bez żadnej adnotacji Springa. Test sekcji 1: Test01TestSlicesUnitTest.
    //
    // DOBRA PRAKTYKA: zanim napiszesz @SpringBootTest, zapytaj: „czy ten test potrzebuje Springa?”.
    //   Dlaczego? Start kontekstu to sekundy, a test jednostkowy — milisekundy. Zestaw 500 testów z pełnym
    //   kontekstem (różnie skonfigurowanym) potrafi trwać minutami; ten sam zestaw jednostkowy — sekundami.
    //   Szybkie testy uruchamiasz po każdej zmianie, wolne — „kiedyś”, czyli za rzadko.

    // =================================================================================================
    // 2. @WebMvcTest — WYCINEK WEBOWY
    // =================================================================================================
    //
    //   @WebMvcTest(ProductController.class)   // Boot 4: org.springframework.boot.webmvc.test.autoconfigure
    //   class ... {
    //       @Autowired MockMvc mockMvc;         // MockMvc = „udawany” klient HTTP, bez prawdziwego serwera
    //       @MockitoBean ProductService service; // serwisu w wycinku NIE MA → atrapa
    //   }
    //
    // W kontekście jest: wskazany kontroler, DispatcherServlet, konwertery JSON (Jackson), obsługa błędów MVC,
    // MockMvc. NIE MA: @Service, @Repository, DataSource, JPA. Zapytanie przechodzi przez prawdziwe mapowanie adresu,
    // prawdziwą konwersję parametrów i prawdziwą serializację JSON — testujesz więc kontroler „od strony HTTP”.
    // Bez atrybutu (samo @WebMvcTest) wycinek bierze WSZYSTKIE kontrolery znalezione przez skanowanie.
    // Test sekcji 2: Test01TestSlicesWebMvcTest.
    //
    // PUŁAPKA: zapomniana atrapa zależności kontrolera. Kontroler potrzebuje ProductService, wycinek go nie ma →
    //   kontekst nie wstaje: „Failed to load ApplicationContext”, a na dnie (root cause)
    //   NoSuchBeanDefinitionException: „No qualifying bean of type 's11_testing.test01_test_slices.ProductService'
    //   available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}”.
    //   Dlaczego? Wycinek celowo nie skanuje @Service — to Ty decydujesz, czym zastąpić warstwę niżej.
    //   Sprawdzone testem sekcji 2.
    //
    // PUŁAPKA: pakiety z Boot 3 w starych tutorialach. Import
    //   org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest w Boot 4 kończy się błędem kompilacji
    //   „package org.springframework.boot.test.autoconfigure.web.servlet does not exist”. Dlaczego? Boot 4 podzielił
    //   wsparcie testów na moduły technologii (spring-boot-webmvc-test, spring-boot-data-jpa-test…), a każdy ma
    //   własny starter testowy (spring-boot-starter-webmvc-test, spring-boot-starter-data-jpa-test…).

    // =================================================================================================
    // 3. @DataJpaTest — WYCINEK JPA
    // =================================================================================================
    //
    //   @DataJpaTest                           // Boot 4: org.springframework.boot.data.jpa.test.autoconfigure
    //   class ... { @Autowired ProductRepository repository; }
    //
    // W kontekście jest: DataSource, Hibernate, repozytoria Spring Data, TestEntityManager, Flyway (gdy włączony).
    // Baza z konfiguracji jest PODMIENIANA na świeżą wbudowaną (H2 o losowej nazwie), a każdy test działa
    // w transakcji wycofywanej po teście. NIE MA kontrolerów, serwisów ani MockMvc. Test sekcji 3:
    // Test01TestSlicesDataJpaTest. Szczegóły (wycofanie, flush, @Sql, podmiana bazy): s11_testing/Test04DataJpaTest.

    // =================================================================================================
    // 4. MNIEJSZE WYCINKI: @JsonTest I @JdbcTest
    // =================================================================================================
    //
    // @JsonTest (org.springframework.boot.test.autoconfigure.json — bez zmian względem Boot 3): tylko Jackson
    //   i testery JSON (JacksonTester). Sprawdzasz, jak rekord wygląda w JSON-ie i czy da się go odczytać z powrotem.
    // @JdbcTest (Boot 4: org.springframework.boot.jdbc.test.autoconfigure): DataSource, JdbcClient, JdbcTemplate,
    //   transakcje, Flyway — ale BEZ JPA (Hibernate nie startuje, więc nie tworzy tabel z encji).
    // Dwa różne wycinki w jednym pliku? Dwie klasy @Nested (zagnieżdżone), każda z własną adnotacją — każda
    // dostaje własny kontekst. Test sekcji 4: Test01TestSlicesJsonJdbcTest.
    //
    // DOBRA PRAKTYKA: wybieraj NAJWĘŻSZY wycinek, który jeszcze sprawdza to, co chcesz.
    //   Dlaczego? Mniej beanów = szybszy start i mniej powodów do awarii niezwiązanych z testem (np. test JSON-a
    //   nie powinien paść, bo ktoś zepsuł migrację bazy).

    // =================================================================================================
    // 5. @SpringBootTest I webEnvironment
    // =================================================================================================
    //
    //   @SpringBootTest                                        // MOCK (domyślne): kontekst webowy, BEZ serwera
    //   @SpringBootTest(webEnvironment = RANDOM_PORT)          // prawdziwy Tomcat na losowym wolnym porcie
    //   @SpringBootTest(webEnvironment = DEFINED_PORT)         // port z konfiguracji (zwykle 8080) — nie w testach!
    //   @SpringBootTest(webEnvironment = NONE)                 // zwykły kontekst, bez warstwy web
    //
    // MOCK: zapytania wysyłasz przez MockMvc (dodaj @AutoConfigureMockMvc). RANDOM_PORT: prawdziwe HTTP, port
    // wstrzykuje @LocalServerPort. NONE: kontroler jest zwykłym beanem, ale nie ma DispatcherServlet ani serwera —
    // dobre dla testów serwisów z prawdziwą bazą. Test sekcji 5: Test01TestSlicesWebEnvironmentTest.
    //
    // PUŁAPKA: DEFINED_PORT w testach. Dlaczego? Gdy port 8080 zajmuje uruchomiona aplikacja (albo inny test),
    //   kontekst nie wstanie; testy nie mogą też biec równolegle. RANDOM_PORT losuje wolny port za każdym razem.
    //
    // PUŁAPKA: MockMvc (środowisko MOCK) to nie prawdziwy serwer. Dlaczego to ważne? Nie ma przekierowania na
    //   stronę błędu /error Boota ani filtrów kontenera serwletów — to, co widzi klient HTTP, sprawdzisz tylko
    //   przez RANDOM_PORT (przykład w lekcji s06_error_handling/Err01DefaultErrors).

    // =================================================================================================
    // 6. PAMIĘĆ PODRĘCZNA KONTEKSTÓW (CONTEXT CACHE)
    // =================================================================================================
    //
    // Spring trzyma zbudowane konteksty w statycznej pamięci podręcznej — jednej na JVM z testami (Maven uruchamia
    // wszystkie klasy testowe modułu w jednej JVM). Klucz = scalona konfiguracja: klasy konfiguracji, aktywne
    // profile, właściwości z adnotacji, atrapy @MockitoBean, @Import, typ środowiska webowego… Gdy klucz się
    // zgadza — trafienie (hit), kontekst NIE startuje ponownie. Gdy różni się czymkolwiek — chybienie (miss) i nowy
    // start. Domyślnie pamięć mieści 32 konteksty (właściwość spring.test.context.cache.maxSize); najdawniej używane
    // są zamykane. Test sekcji 6 liczy starty: Test01TestSlicesContextCacheTest.
    //
    //   ConfigA: @SpringBootTest(properties = "test01.cache=demo")                → start nr 1
    //   ConfigB: to samo                                                         → 0 nowych startów (ten sam obiekt)
    //   + "shop.name=Sklep testowy"                                              → +1 start
    //   + @MockitoBean ProductService                                            → +1 start
    //   @DirtiesContext                                                          → po klasie kontekst zamknięty,
    //                                                                              następne użycie: +1 start
    //
    // Podgląd w logu: logging.level.org.springframework.test.context.cache=DEBUG wypisuje po każdym pobraniu
    // kontekstu statystyki, np. „Spring test ApplicationContext cache statistics: [DefaultContextCache@5e6b2641
    // size = 3, maxSize = 32, contextUsageCount = 3, parentContextCount = 0, hitCount = 1, missCount = 3,
    // failureCount = 0]” (skopiowane z prawdziwego uruchomienia).
    //
    // PUŁAPKA: każda klasa testowa z INNYM zestawem atrap/właściwości. Dlaczego? Każda różnica to osobny kontekst:
    //   20 klas × 2 sekundy startu = 40 sekund samego startowania. Ujednolicaj konfigurację (wspólna klasa bazowa,
    //   wspólna adnotacja), a atrapy ograniczaj do tego, czego naprawdę nie da się użyć „na prawdziwie”.
    //
    // PUŁAPKA: @DirtiesContext „na wszelki wypadek”. Dlaczego? Wyrzuca kontekst z pamięci — następna klasa
    //   startuje go od nowa. Używaj tylko wtedy, gdy test naprawdę psuje stan kontekstu (np. zmienia singleton,
    //   którego nie da się przywrócić).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Piramida: dużo testów jednostkowych (bez Springa), mniej wycinków, mało @SpringBootTest.
     *   • @WebMvcTest(Kontroler.class) — kontroler + MVC + MockMvc; zależności kontrolera → @MockitoBean.
     *   • @DataJpaTest — JPA + repozytoria + baza wbudowana + transakcja z wycofaniem; bez weba i serwisów.
     *   • @JdbcTest — JDBC bez JPA; @JsonTest — sam Jackson + JacksonTester.
     *   • Pakiety Boot 4: org.springframework.boot.webmvc.test.autoconfigure (@WebMvcTest),
     *     org.springframework.boot.data.jpa.test.autoconfigure (@DataJpaTest),
     *     org.springframework.boot.jdbc.test.autoconfigure (@JdbcTest), org.springframework.boot.test.autoconfigure.json
     *     (@JsonTest), org.springframework.boot.test.context (@SpringBootTest).
     *   • @SpringBootTest: MOCK (domyślne, bez serwera) / RANDOM_PORT (prawdziwy serwer) / NONE (bez weba);
     *     DEFINED_PORT — nie w testach.
     *   • Ten sam klucz konfiguracji = ten sam kontekst z pamięci; inne właściwości, profile, @MockitoBean,
     *     @Import → nowy kontekst; @DirtiesContext → zamknięcie i ponowny start.
     *   • Statystyki pamięci: logging.level.org.springframework.test.context.cache=DEBUG.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego testy jednostkowe stanowią podstawę piramidy, a @SpringBootTest jej czubek?
     *   2. Co się stanie? Piszesz @WebMvcTest(ProductController.class) i nie dodajesz @MockitoBean dla ProductService.
     *   3. Których beanów NIE ma w kontekście @DataJpaTest, a których nie ma w @WebMvcTest?
     *   4. Czym różnią się webEnvironment MOCK, RANDOM_PORT i NONE? Kiedy który wybierzesz?
     *   5. Co się stanie? Dwie klasy testowe mają identyczne @SpringBootTest, a trzecia to samo plus
     *      @MockitoBean ProductService. Ile razy wystartuje kontekst, gdy uruchomisz wszystkie trzy?
     *   6. ZNAJDŹ BŁĄD: w projekcie Boot 4 kopiujesz z poradnika
     *      import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; — co zobaczysz i dlaczego?
     *   7. Po co @DirtiesContext i dlaczego nie warto dodawać go „na wszelki wypadek”?
     *   8. Jak sprawdzisz, ile kontekstów zbudował Twój zestaw testów?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — uzupełniasz TESTY w pliku Test01TestSlicesExercisesTest (src/test/java, ten sam pakiet).
    // Każde ćwiczenie to klasa @Nested z komentarzem TODO; usuń wywołanie todo() i dopisz brakujący kawałek.
    // Uruchom: ▶ przy klasie albo ./mvnw -pl s11-testing test -Dgroups=cwiczenie -DexcludedGroups=none
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): klasa Exercise1 nie ma żadnej adnotacji. Dodaj adnotację wycinka, w którym jest
    //   repozytorium i baza, a NIE MA kontrolera. Podpowiedź: sekcja 3.
    //
    // ĆWICZENIE 2 (średnie): klasa Exercise2 to gotowy @WebMvcTest z atrapą serwisu. Naucz atrapę, że
    //   search("kaw") zwraca dwa produkty: Ekspres do kawy (189900 gr, "1899.00", id 13) i Kawa ziarnista 1kg
    //   (6499 gr, "64.99", id 5) — w tej kolejności. Podpowiedź: given(service.search("kaw")).willReturn(List.of(...)).
    //
    // ĆWICZENIE 3 (średnie): klasa Exercise3 ma @SpringBootTest w środowisku MOCK, a test chce wysłać PRAWDZIWE
    //   zapytanie HTTP. Zmień środowisko webowe tak, żeby wystartował serwer. Podpowiedź: sekcja 5.
    //
    // ĆWICZENIE 4 (trudniejsze): klasa Exercise4 (@JsonTest). Wpisz tekst JSON, z którego JacksonTester odczyta
    //   ProductDto(14, "Lampka biurkowa", 12900, "129.00"), oraz ścieżkę JSON (JsonPath), pod którą w zapisanym
    //   JSON-ie leży cena w złotych. Podpowiedź: nazwy pól JSON = nazwy składowych rekordu; ścieżka zaczyna się od "$.".

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Testy jednostkowe są szybkie (milisekundy) i dokładnie wskazują zepsute miejsce, więc może ich być dużo
     *      i uruchamiasz je ciągle. @SpringBootTest startuje całą aplikację (sekundy) i sprawdza „czy wszystko
     *      razem działa” — wystarczy kilka takich testów.
     *   2. Kontekst nie wstanie: IllegalStateException „Failed to load ApplicationContext”, a przyczyna na dnie to
     *      NoSuchBeanDefinitionException „No qualifying bean of type 's11_testing.test01_test_slices.ProductService'
     *      available…” — wycinek webowy nie skanuje @Service. Sprawdzone testem sekcji 2.
     *   3. @DataJpaTest: brak kontrolerów, serwisów, MockMvc. @WebMvcTest: brak serwisów, repozytoriów, DataSource
     *      i JPA. Sprawdzone testami sekcji 2 i 3.
     *   4. MOCK — kontekst webowy bez serwera (zapytania przez MockMvc); RANDOM_PORT — prawdziwy serwer na losowym
     *      porcie (prawdziwe HTTP, np. strona błędu /error); NONE — bez weba, do testów serwisów i repozytoriów.
     *   5. Dwa razy: dwie pierwsze klasy mają ten sam klucz i dzielą kontekst, trzecia (z atrapą) ma inny klucz
     *      i dostaje nowy kontekst. Sprawdzone testem sekcji 6 (licznik startów).
     *   6. Błąd kompilacji: „package org.springframework.boot.test.autoconfigure.web.servlet does not exist”.
     *      W Boot 4 @WebMvcTest leży w org.springframework.boot.webmvc.test.autoconfigure (moduł
     *      spring-boot-webmvc-test, starter spring-boot-starter-webmvc-test).
     *   7. @DirtiesContext zamyka kontekst po klasie (albo metodzie) i usuwa go z pamięci podręcznej — potrzebne,
     *      gdy test trwale psuje stan beanów. Nadużywany sprawia, że kolejne klasy startują kontekst od nowa, a cały
     *      zestaw testów zwalnia. Sprawdzone testem sekcji 6.
     *   8. Włącz logging.level.org.springframework.test.context.cache=DEBUG — Spring wypisze statystyki
     *      (size, hitCount, missCount); missCount = liczba zbudowanych kontekstów.
     */
    // </editor-fold>
}

package s11_testing.test01_test_slices;

/**
 * <pre>
 * TEMAT: Piramida testów i wycinki testowe — kiedy zwykły JUnit, kiedy @WebMvcTest, @DataJpaTest, @JsonTest,
 *        a kiedy cała aplikacja (@SpringBootTest)
 *        (test slice = wycinek testowy, test tylko jednej warstwy aplikacji; context = kontekst, czyli kontener
 *         Springa z beanami; cache = pamięć podręczna; unit test = test jednostkowy; mock = atrapa)
 *
 * W SKRÓCIE:
 *   Test może sprawdzać jedną klasę, jedną warstwę albo całą aplikację. Im wyżej, tym wolniej i trudniej znaleźć
 *   przyczynę błędu. Dlatego robimy PIRAMIDĘ: dużo tanich testów jednostkowych (zwykły JUnit + Mockito, bez
 *   Springa), mniej wycinków (Spring startuje tylko jedną warstwę: web, dane, JSON) i garstkę testów całej
 *   aplikacji. Boot daje do tego adnotacje-wycinki; każda ładuje TYLKO potrzebne beany i nic więcej.
 *   Drugi temat lekcji: Spring Test trzyma uruchomione konteksty w pamięci i używa ich ponownie — jeśli
 *   konfiguracja testów jest taka sama. Każda zbędna odmiana konfiguracji to kolejny, wolny start kontekstu.
 *
 * ANALOGIA: kontrola jakości w fabryce rowerów.
 *   Najwięcej kontroli robisz na stole: sprawdzasz jedną śrubę, jedno koło (test jednostkowy — sekundy). Rzadziej
 *   składasz sam napęd: łańcuch + przerzutki + korba (wycinek). Na końcu, rzadko, wyprowadzasz gotowy rower
 *   na drogę (@SpringBootTest). Gdyby każdą śrubę sprawdzać jazdą próbną, fabryka stałaby w miejscu — a przy
 *   awarii i tak nie wiedziałbyś, która śruba zawiodła.
 *
 * JAK TO DZIAŁA:
 *   1. Adnotacja testowa (np. @WebMvcTest) mówi bootstrapperowi Boota, JAKI wycinek zbudować. Szuka on klasy
 *      @SpringBootConfiguration (u nas Test01Application) w górę drzewa pakietów.
 *   2. Z całej aplikacji bierze tylko beany pasujące do wycinka (filtr typów), reszty nie skanuje, i włącza
 *      tylko wybrane autokonfiguracje (te z plików {@code *.imports} danego wycinka).
 *   3. Gotowy kontekst trafia do pamięci podręcznej pod kluczem = cała konfiguracja testu. Kolejna klasa testowa
 *      z identycznym kluczem dostaje TEN SAM kontekst, bez ponownego startu.
 *   4. Beany wstrzykiwane do testu ({@code @Autowired}) pochodzą z tego kontekstu; adnotacje typu {@code @MockitoBean}
 *      zmieniają kontekst przed startem (i klucz w pamięci podręcznej!).
 *
 * SŁÓWKA:
 *   pyramid = piramida; slice = wycinek, plaster; layer = warstwa; given/when/then = mając/gdy/wtedy; nested =
 *   zagnieżdżony; fixed clock = stały zegar; random port = losowy port; dirty = brudny (tu: kontekst do wyrzucenia);
 *   threshold = próg; assertion = asercja, sprawdzenie; fixture = stałe tło testu (dane startowe).
 *
 * ZOBACZ TEŻ: s00_start/Start04TestingIntro (pierwsze testy w kursie),
 *             s03_boot_basics/Boot01AutoConfiguration (autokonfiguracja, ApplicationContextRunner),
 *             s04_web_rest/Rest05CrudApi (trzy wysokości testów na przykładzie CRUD),
 *             s08_data_jpa/Jpa02Repositories (@DataJpaTest po raz pierwszy),
 *             s10_transactions/Tx01Transactional (transakcja testu i rollback),
 *             s11_testing/Test02MockitoBeans (atrapy w kontekście), s11_testing/Test04DataJpaTest (wycinek danych),
 *             t32_junit_mockito/JUnit04Mockito (Mockito bez Springa), t32_junit_mockito/JUnit03AssertJ (asercje).
 * </pre>
 */
public final class Test01TestSlices {

    private Test01TestSlices() {
    }

    // =================================================================================================
    // 1. PIRAMIDA TESTÓW — im niżej, tym tańszy i szybszy test (Test01ProductServiceTest)
    // =================================================================================================
    //
    //   Poziom                        | Przykład w tej lekcji            | Co uruchamia                 | Czas
    //   ------------------------------+----------------------------------+------------------------------+----------
    //   Jednostkowy (bez Springa)     | Test01ProductServiceTest         | jedna klasa + atrapy         | ms
    //   Wycinek                       | Test01WebMvcSliceTest, ...       | jedna warstwa Springa        | ~0,1–2 s
    //   Cała aplikacja (@SpringBootTest) | Test01SpringBootTestEnvironmentsTest | wszystkie beany + baza  | 1–6 s
    //   End-to-end (cała aplikacja + przeglądarka/inne systemy) | poza kursem | wszystko, także z zewnątrz | minuty
    //
    // Test jednostkowy serwisu NIE używa Springa: ProductService ma konstruktor, więc w teście robimy zwykłe
    // {@code new ProductService(atrapaRepozytorium, Clock.fixed(...))}. Atrapę repozytorium robi Mockito
    // ({@code @Mock} + {@code @ExtendWith(MockitoExtension.class)}), czas stoi w miejscu ({@code Clock.fixed}).
    //
    // PRZED (zły pomysł — cała aplikacja, żeby sprawdzić jeden wzór):
    //   @SpringBootTest class DiscountTest { @Autowired ProductService s; ... }   // start kontekstu, baza, Flyway
    // PO (zwykły test): {@code new ProductService(repository, clock).priceAfterDiscount(1L, 10)}.
    //
    // Dlaczego najwięcej testów jednostkowych?
    //   • Szybkość: kilkaset takich testów trwa sekundę — uruchamiasz je po każdej zmianie, bez czekania.
    //   • Precyzja: czerwony test wskazuje JEDNĄ klasę. Czerwony @SpringBootTest może znaczyć: konfigurację, bazę,
    //     migrację, JSON, kolejność beanów…
    //   • Kombinacje: w logice jest wiele przypadków brzegowych (rabat 0%, 100%, 101%, brak produktu) — taniej
    //     sprawdzić je w ms niż w sekundach.
    // Czego test jednostkowy NIE sprawdzi: czy adnotacje działają (mapowanie URL, @Valid, @Transactional), czy SQL
    // pasuje do tabel, czy JSON ma poprawny kształt. To zadanie wycinków i testu całej aplikacji.
    //
    // DOBRA PRAKTYKA: wybieraj NAJNIŻSZY poziom, który potrafi wykryć dany błąd. Dlaczego? Każdy poziom wyżej
    //   płacisz czasem (start kontekstu) i trudniejszą diagnozą; tanie testy dostajesz za darmo przy każdym zapisie
    //   pliku, drogie uruchamiasz rzadziej.
    //
    // DOBRA PRAKTYKA: wstrzykuj zależności przez KONSTRUKTOR i podawaj zegar jako {@code Clock}. Dlaczego? Wtedy
    //   test jednostkowy tworzy obiekt zwykłym {@code new} (bez refleksji i bez Springa) i ustawia czas na stałe,
    //   więc wynik nie zależy od zegara systemowego (Start04TestingIntro, determinizm).

    // =================================================================================================
    // 2. WYCINKI — co dokładnie jest w kontekście (Test01WebMvcSliceTest, Test01DataJpaSliceTest, ...)
    // =================================================================================================
    //
    //   Adnotacja      | Pakiet (Boot 4)                                   | W kontekście jest...                 | NIE MA
    //   ---------------+---------------------------------------------------+--------------------------------------+---------------------------
    //   @WebMvcTest    | org.springframework.boot.webmvc.test.autoconfigure| @Controller/@RestController,         | @Service, @Repository,
    //                  |                                                   | @RestControllerAdvice, konwertery,   | JPA, bean Clock z @Configuration
    //                  |                                                   | filtry, MockMvc/MockMvcTester        |
    //   @DataJpaTest   | org.springframework.boot.data.jpa.test.autoconfigure | encje, repozytoria Spring Data,   | kontrolery, serwisy, MVC
    //                  |                                                   | EntityManager, TestEntityManager,    |
    //                  |                                                   | baza wbudowana, Flyway               |
    //   @JdbcTest      | org.springframework.boot.jdbc.test.autoconfigure  | DataSource, JdbcClient/JdbcTemplate, | JPA, repozytoria, MVC
    //                  |                                                   | transakcje, Flyway                   |
    //   @JsonTest      | org.springframework.boot.test.autoconfigure.json  | Jackson 3 (JsonMapper), moduły i     | wszystko inne
    //                  |                                                   | @JacksonComponent, JacksonTester     |
    //   @SpringBootTest| org.springframework.boot.test.context             | WSZYSTKO (cała aplikacja)            | —
    //
    // • @WebMvcTest(ProductController.class) tworzy kontroler i ProductErrorAdvice, ale nie ProductService. Kontroler
    //   potrzebuje serwisu w konstruktorze — dlatego test dostarcza atrapę przez {@code @MockitoBean} (pełna lekcja:
    //   s11_testing/Test02MockitoBeans). Test01WebMvcSliceTest sprawdza, że advice DZIAŁA w wycinku (wyjątek serwisu
    //   → 404 z ProblemDetail), a repozytorium i EntityManagerFactory nie istnieją.
    // • @DataJpaTest tworzy repozytorium z danymi z Flyway (2 produkty z V2), ale nie ma w nim kontrolera, serwisu
    //   ani DispatcherServlet (Test01DataJpaSliceTest). Szczegóły: s11_testing/Test04DataJpaTest.
    // • @JsonTest daje JacksonTester<T>: zamienia obiekt na JSON i z powrotem, bez serwera. W Boot 4 pod spodem jest
    //   Jackson 3 (pakiety tools.jackson, nie com.fasterxml). Test01JsonSliceTest: rekord → JSON z datą jako
    //   tekstem ISO, liczba bez cudzysłowu; w wycinku nie ma żadnego naszego beana (kontroler, serwis, repozytorium).
    // • @JdbcTest (Test01JdbcSliceTest) — wycinek dla aplikacji pisanych na samym JDBC (s07_data_jdbc).
    //
    // PUŁAPKA: @WebMvcTest bez atrapy serwisu. Dlaczego to boli? Wycinek NIE skanuje @Service, więc kontroler nie
    //   ma czego wstrzyknąć i cały kontekst się nie podnosi: IllegalStateException "Failed to load ApplicationContext"
    //   z przyczyną NoSuchBeanDefinitionException (brak beana ProductService). Naprawa: {@code @MockitoBean} albo
    //   {@code @Import(ProductService.class)} (prawdziwy serwis, ale wtedy brakuje JEGO zależności — repozytorium).
    //   Pokazuje to Test01MissingServiceTest. Po JEDNYM nieudanym ładowaniu kontekstu kolejne próby w tym samym
    //   procesie są pomijane (próg awarii = 1: "ApplicationContext failure threshold (1) exceeded"), więc reszta
    //   testów z tą konfiguracją wywala się z mylącym komunikatem — szukaj PIERWSZEGO błędu w logu.
    //
    // PUŁAPKA: oczekiwanie, że @Configuration z aplikacji (np. ClockConfig z beanem Clock) trafi do wycinka.
    //   Dlaczego? Wycinki nie skanują klas @Configuration — w wycinku WebMvc nie ma beana Clock (sprawdza to
    //   Test01WebMvcSliceTest). Gdy potrzebujesz własnych beanów w wycinku, dodaj je @Import-em
    //   (s11_testing/Test05TestConfiguration).
    //
    // PUŁAPKA: @Bean w samej klasie @SpringBootApplication. Dlaczego? Wycinki ładują klasę @SpringBootConfiguration
    //   w całości, więc jej beany (np. dodatkowe filtry, źródła danych) trafiają do KAŻDEGO wycinka i potrafią
    //   go niepostrzeżenie rozbudować. Trzymaj konfigurację w osobnych klasach @Configuration.

    // =================================================================================================
    // 3. @SpringBootTest — cała aplikacja i jej środowisko webowe (Test01SpringBootTestEnvironmentsTest)
    // =================================================================================================
    //
    //   webEnvironment | Serwer HTTP?              | Jak wysyłasz zapytania                        | Kiedy
    //   ---------------+---------------------------+-----------------------------------------------+-----------------------------
    //   MOCK (domyślny)| NIE (atrapa serwletów)    | MockMvc/MockMvcTester z @AutoConfigureMockMvc | zwykle: szybkie, pełny kontekst
    //   RANDOM_PORT    | TAK, Tomcat, port losowy  | RestClient / TestRestTemplate + @LocalServerPort | gdy liczy się prawdziwy HTTP
    //   DEFINED_PORT   | TAK, port z server.port   | jak wyżej, ale sztywny adres                  | prawie nigdy (port bywa zajęty)
    //   NONE           | NIE, kontekst nie-webowy  | brak — wołasz beany wprost                    | testy serwisu z bazą, aplikacje bez HTTP
    //
    // • MOCK. Kontekst jest webowy (WebApplicationContext), ale bez prawdziwego serwera. @AutoConfigureMockMvc
    //   (pakiet org.springframework.boot.webmvc.test.autoconfigure) dodaje MockMvc; BEZ niej wstrzyknięcie MockMvc
    //   nie uda się — NoSuchBeanDefinitionException dla MockMvc (Test01MissingServiceTest). W wycinku @WebMvcTest
    //   MockMvc jest od razu.
    // • RANDOM_PORT. Startuje Tomcat na wolnym porcie, a @LocalServerPort (pakiet
    //   org.springframework.boot.test.web.server) podaje ten port. RestClient rzuca wyjątek przy 4xx/5xx (np.
    //   HttpClientErrorException.NotFound dla 404), dlatego 404 sprawdzasz przez assertThatThrownBy.
    // • NONE. Brak DispatcherServlet i brak kontekstu webowego; serwis i baza działają (pełne repozytorium, Flyway).
    // • DEFINED_PORT. Używa server.port z konfiguracji (domyślnie 8080). W testach unikamy go: gdy port jest zajęty
    //   (druga aplikacja, równoległy test), test wywraca się losowo.
    //
    // W Boot 4 klasa TestRestTemplate leży w pakiecie org.springframework.boot.resttestclient (adnotacja
    // @AutoConfigureTestRestTemplate) i NIE jest już dostępna "z automatu" z samego spring-boot-starter-test:
    // wymaga modułu spring-boot-restclient (starter-restclient). Bez niego kontekst nie startuje —
    // NoClassDefFoundError: org/springframework/boot/restclient/RestTemplateBuilder (sprawdzone). W starych
    // tutorialach (Boot 2/3) TestRestTemplate działał od ręki — dziś wygodniej użyć RestClient, który jest
    // częścią Spring Web. Boot 4 ma też RestTestClient (adnotacja @AutoConfigureRestTestClient, pakiet
    // org.springframework.boot.resttestclient.autoconfigure).
    //
    // DOBRA PRAKTYKA: domyślnie MOCK + MockMvc; RANDOM_PORT tylko dla scenariusza, który naprawdę musi przejść
    //   przez sieć (filtry serwletowe, sesje, nagłówki). Dlaczego? MOCK jest szybszy, nie zajmuje portu i działa
    //   w tym samym wątku co test — więc pozwala np. na wycofanie transakcji testu (przy RANDOM_PORT zapytanie
    //   obsługuje inny wątek serwera, transakcja testu go nie obejmuje).
    //
    // PUŁAPKA: @SpringBootTest "na wszystko". Dlaczego? Start całej aplikacji to sekundy, a przy ~100 testach — minuty.
    //   Do sprawdzenia kontrolera wystarcza @WebMvcTest, do repozytorium @DataJpaTest. Całą aplikację zostaw na
    //   kilka scenariuszy "od drzwi do bazy" — pokazują, że warstwy współpracują.

    // =================================================================================================
    // 4. PAMIĘĆ PODRĘCZNA KONTEKSTÓW — dlaczego testy zwalniają (Test01ContextCacheTest)
    // =================================================================================================
    //
    // Start kontekstu kosztuje (skanowanie, Hibernate, Flyway, baza). Dlatego Spring Test trzyma uruchomione
    // konteksty w pamięci podręcznej (jedna na cały proces testów) i wydaje ten sam, gdy klucz jest taki sam.
    // Klucz to cała konfiguracja testu:
    //   • klasy konfiguracji i wycinek (@WebMvcTest vs @SpringBootTest),
    //   • profile (@ActiveProfiles), właściwości (@SpringBootTest(properties = ...), @TestPropertySource),
    //   • webEnvironment i dodatki (@AutoConfigureMockMvc, @Import),
    //   • ZESTAW atrap (@MockitoBean/@MockitoSpyBean) — typ, nazwa i ustawienia każdej z nich.
    // Test01ContextCacheTest ładuje konteksty ręcznie (TestContextManager) i dowodzi:
    //   • dwie klasy z tą samą konfiguracją → TEN SAM obiekt ApplicationContext (isSameAs),
    //   • inna właściwość (properties = "demo.flag=true") → drugi kontekst,
    //   • ten sam zestaw atrap w dwóch klasach → wspólny kontekst; atrapa innego beana → osobny,
    //   • unieważnienie kontekstu (to robi @DirtiesContext) → stary zamknięty (isActive == false), następny nowy.
    // Pojemność: domyślnie 32 konteksty (właściwość spring.test.context.cache.maxSize); najdawniej używane
    // wypadają z pamięci. Błąd ładowania nie jest wpisywany jako sukces: próg awarii wynosi 1 (sekcja 2).
    //
    // PRZED (każda klasa ma własny zestaw atrap — 3 klasy = 3 starty):
    //   class ATest { @MockitoBean ProductService s; }
    //   class BTest { @MockitoBean ProductRepository r; }
    //   class CTest { @SpringBootTest(properties = "x=1") ... }
    // PO (jedna wspólna konfiguracja w klasie bazowej albo @TestConfiguration — jeden kontekst dla wszystkich).
    //
    // PUŁAPKA: "dla wygody" inna właściwość, inny profil albo inny zestaw @MockitoBean w każdej klasie testowej.
    //   Dlaczego? Każda odmiana to NOWY kontekst, czyli kolejny wolny start. Zestaw 20 klas z 20 odmianami to
    //   20 startów zamiast 1. Trzymaj konfigurację testów jednakową tam, gdzie to możliwe, a atrapy wspólne
    //   zbieraj w jednej klasie bazowej.
    //
    // PUŁAPKA: {@code @DirtiesContext} na klasie "żeby było czysto". Dlaczego? Po klasie kontekst jest zamykany
    //   i wyrzucany z pamięci, więc następna klasa płaci pełny start. Czystość stanu zapewnij taniej: sprzątaj
    //   w {@code @BeforeEach} (np. {@code service.reset()}, jak w Rest05) albo wycofuj transakcję testu.
    //   Szczegóły i uzasadnione użycia: s11_testing/Test05TestConfiguration.

    // =================================================================================================
    // 5. NAZEWNICTWO I STRUKTURA TESTU (Test01ProductServiceTest)
    // =================================================================================================
    //
    //   • GIVEN / WHEN / THEN (mając / gdy / wtedy): w teście trzy bloki oddzielone pustą linią lub komentarzem:
    //     przygotuj dane i atrapy, wykonaj JEDNĄ akcję, sprawdź wynik. Przy dłuższych testach tylko komentarze
    //     // given, // when, // then — czytelniejsze niż same asercje.
    //   • @DisplayName po polsku: opisuje ZACHOWANIE ("rabat spoza 0..100 to IllegalArgumentException"), a nie
    //     nazwę metody. Nazwa metody służy programiście, @DisplayName — raportowi i kolegom.
    //   • @Nested grupuje testy jednej metody (Create, Discount) i ich wspólne przygotowanie.
    //   • Asercje AssertJ ({@code assertThat(...)}): płynny zapis, czytelne komunikaty błędów, podpowiedzi IDE
    //     (t32_junit_mockito/JUnit03AssertJ). Dla wyjątków: {@code assertThatThrownBy(...)}.
    //   • Jeden test = jedna myśl. Gdy test ma pięć niepowiązanych asercji, po pierwszym błędzie nie wiesz, czy
    //     reszta działa.
    //
    // DOBRA PRAKTYKA: test nie zależy od kolejności ani od stanu zostawionego przez inny test. Dlaczego? JUnit nie
    //   gwarantuje kolejności; test, który "działa tylko po tamtym", wywraca się losowo i trudno znaleźć powód.
    //
    // PUŁAPKA: asercja, która przechodzi także wtedy, gdy kod jest zepsuty (np. {@code assertThat(result).isNotNull()}
    //   dla metody, która nigdy nie zwraca null). Dlaczego? Test na zielono nic nie dowodzi. Sprawdzaj konkretną
    //   wartość i, gdy możesz, uruchom test na zepsutym kodzie, żeby zobaczyć, że robi się czerwony.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Piramida: dużo testów jednostkowych (zwykły JUnit + Mockito, konstruktor + Clock.fixed), mniej wycinków,
     *     garstka @SpringBootTest. Wybieraj najniższy poziom, który wykryje błąd.
     *   • @WebMvcTest: kontrolery, advice, konwertery, filtry, MockMvc. Brak @Service/@Repository i klas @Configuration
     *     → serwis podstaw @MockitoBean. @DataJpaTest: encje, repozytoria, baza, Flyway. @JsonTest: Jackson 3 +
     *     JacksonTester. @JdbcTest: DataSource, JdbcClient.
     *   • @SpringBootTest: MOCK (domyślny, bez serwera, potrzebuje @AutoConfigureMockMvc), RANDOM_PORT (Tomcat,
     *     @LocalServerPort, RestClient), DEFINED_PORT (sztywny port — unikaj), NONE (bez weba).
     *   • Boot 4: TestRestTemplate wymaga spring-boot-restclient; pakiety adnotacji testowych zmieniły się
     *     (np. org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest) — stare importy z tutoriali nie działają.
     *   • Pamięć podręczna kontekstów: klucz = klasy + profile + właściwości + webEnvironment + zestaw atrap;
     *     ten sam klucz = ten sam kontekst; domyślnie 32 konteksty; @DirtiesContext zamyka kontekst.
     *   • Struktura testu: given/when/then, @DisplayName po polsku, @Nested, AssertJ, jeden test = jedna myśl.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego w piramidzie testów jest najwięcej testów jednostkowych, a najmniej testów całej aplikacji?
     *   2. Co się stanie? Test z {@code @WebMvcTest(ProductController.class)} nie ma żadnej atrapy serwisu.
     *      Kontroler przyjmuje ProductService w konstruktorze.
     *   3. Co się stanie? W klasie z {@code @SpringBootTest} (bez dodatkowych adnotacji) piszesz
     *      {@code @Autowired MockMvc mockMvc;}.
     *   4. Które beany z naszej aplikacji są w kontekście {@code @DataJpaTest}, a których tam nie ma?
     *   5. ZNAJDŹ BŁĄD: test ma {@code @SpringBootTest(webEnvironment = DEFINED_PORT)} i odpytuje
     *      {@code http://localhost:8080}. Przechodzi na Twoim komputerze, a w CI co kilka uruchomień jest czerwony.
     *      Dlaczego i co zmienić?
     *   6. Co się stanie? Dwie klasy testowe mają identyczną konfigurację @SpringBootTest, trzecia dodaje
     *      {@code properties = "demo.flag=true"}. Ile kontekstów uruchomi Spring?
     *   7. ZNAJDŹ BŁĄD: 15 klas testowych, w każdej inny zestaw @MockitoBean, a cały zestaw testów trwa minuty.
     *      Gdzie jest przyczyna i jak ją zmniejszyć?
     *   8. Czym różni się @SpringBootTest(webEnvironment = MOCK) od RANDOM_PORT i w której wersji możesz użyć
     *      wycofania transakcji testu dla zapytań HTTP?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — tym razem piszesz TESTY. Szkielet: Test01SlicesExercisesTest (@Tag("cwiczenie")),
    // rozwiązania: s11_testing.solutions.test01_test_slices.Test01SlicesSolutionsTest (@Tag("wzorzec"))
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): napisz ZWYKŁY test jednostkowy (bez Springa) serwisu: rabat 0% nie zmienia ceny,
    //   rabat 100% daje 0,00. Podpowiedź: sekcja 1 — @Mock ProductRepository, Clock.fixed, new ProductService(...).
    //
    // ĆWICZENIE 2 (łatwe): napisz test @WebMvcTest: GET /test01/products zwraca JSON z dwoma produktami z atrapy
    //   serwisu. Podpowiedź: sekcja 2 — @MockitoBean; id produktu ustaw przez ReflectionTestUtils.setField.
    //
    // ĆWICZENIE 3 (średnie): w @WebMvcTest sprawdź, że POST z pustą nazwą daje 400, a serwis NIE został zawołany.
    //   Podpowiedź: verifyNoInteractions(service).
    //
    // ĆWICZENIE 4 (trudniejsze): w @JsonTest sprawdź, że JSON z dodatkowym polem daje się odczytać na ProductRequest,
    //   a JSON bez ceny daje price == null. Podpowiedź: JacksonTester.parseObject; zachowanie dla nieznanych pól
    //   sprawdź uruchomieniem, nie zgadując.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Testy jednostkowe są najtańsze (ms), wskazują jedną klasę i pozwalają sprawdzić wiele przypadków
     *      brzegowych. Testy całej aplikacji są wolne (start kontekstu, baza) i trudne w diagnozie — wystarczy kilka,
     *      żeby pokazać, że warstwy współpracują. Pokazuje to tabela w sekcji 1 i czasy testów w tej lekcji.
     *   2. Kontekst się nie podnosi: IllegalStateException "Failed to load ApplicationContext", a pierwotną
     *      przyczyną jest NoSuchBeanDefinitionException o braku beana ProductService (wycinek nie skanuje @Service).
     *      Sprawdzone w Test01MissingServiceTest.
     *   3. Wstrzyknięcie się nie uda: NoSuchBeanDefinitionException dla MockMvc. W @SpringBootTest MockMvc pojawia się
     *      dopiero z @AutoConfigureMockMvc (w @WebMvcTest jest od razu). Sprawdzone w Test01MissingServiceTest.
     *   4. Są: encja Product, ProductRepository, EntityManagerFactory, baza i Flyway (2 produkty z migracji). Nie
     *      ma: ProductController, ProductService i DispatcherServlet. Sprawdzone w Test01DataJpaSliceTest.
     *   5. Port 8080 bywa zajęty (druga aplikacja, równoległy test, inny proces CI), więc start serwera się nie
     *      udaje albo test trafia do cudzej aplikacji. Zmiana: webEnvironment = RANDOM_PORT i @LocalServerPort
     *      (adres składasz z podanego portu). Port losowy sprawdzony w Test01SpringBootTestEnvironmentsTest.
     *   6. Dwa: jeden wspólny dla dwóch klas o identycznej konfiguracji i drugi dla klasy z inną właściwością.
     *      Sprawdzone w Test01ContextCacheTest (isSameAs i isNotSameAs).
     *   7. Każdy inny zestaw @MockitoBean to inny klucz, więc inny kontekst — 15 klas to do 15 startów. Zmniejszenie:
     *      wspólna klasa bazowa (lub @TestConfiguration) z jednym zestawem atrap dla wielu klas. Sprawdzone
     *      w Test01ContextCacheTest (ten sam zestaw → wspólny kontekst, atrapa innego beana → osobny).
     *   8. MOCK: kontekst webowy bez prawdziwego serwera, zapytania przez MockMvc w wątku testu. RANDOM_PORT:
     *      Tomcat na losowym porcie i prawdziwy HTTP; zapytanie obsługuje wątek serwera. Transakcję testu
     *      (z wycofaniem) obejmie tylko tryb MOCK, bo wątek testu i wątek obsługi zapytania to jeden wątek.
     *      Opis zachowania wątków — z dokumentacji Spring Test; w tej lekcji sprawdzono różnice kontekstu i portu.
     */
    // </editor-fold>
}

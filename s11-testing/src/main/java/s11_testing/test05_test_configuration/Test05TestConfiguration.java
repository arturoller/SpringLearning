package s11_testing.test05_test_configuration;

/**
 * <pre>
 * TEMAT: Konfiguracja testów — @TestConfiguration i @Import, @TestBean, właściwości i profile testów oraz ich
 *        priorytety, @DynamicPropertySource, @DirtiesContext, ApplicationContextRunner i OutputCaptureExtension
 *        (test configuration = konfiguracja testowa; property = właściwość; dynamic = dynamiczny, obliczany
 *         w czasie działania; dirty = brudny; runner = uruchamiacz; output capture = przechwytywanie wyjścia)
 *
 * W SKRÓCIE:
 *   Test często potrzebuje aplikacji „prawie takiej samej” jak prawdziwa: z zegarem ustawionym na konkretny dzień,
 *   z adresem serwera-atrapy zamiast prawdziwej usługi, z jedną właściwością zmienioną. Spring daje do tego kilka
 *   narzędzi i każde ma swoje miejsce. {@code @TestConfiguration} DOKŁADA beany do konfiguracji aplikacji (nie
 *   zastępuje jej), {@code @TestBean} ZASTĘPUJE jeden bean, właściwości ustawiasz w adnotacjach, plikach profili
 *   albo dynamicznie, a {@code @DirtiesContext} wyrzuca zepsuty kontekst z pamięci podręcznej. Lekcja pokazuje,
 *   co wygrywa z czym i gdzie łatwo się pomylić.
 *
 * ANALOGIA: próba w restauracji przed otwarciem.
 *   Kuchnia (aplikacja) jest prawdziwa, ale szef zmiany DOKŁADA rzeczy na próbę: zegar ścienny przestawiony na
 *   piątek wieczór (@TestConfiguration z Clock.fixed), kartkę „dziś sól zamiast pieprzu” przypiętą NAD menu
 *   (właściwości testu wygrywają z plikami), a numer dostawcy wpisuje dopiero, gdy ten zadzwoni (@DynamicPropertySource).
 *   Jeśli ktoś na próbie rozleje sos, trzeba posprzątać przed następną próbą: przetrzeć blat (reset w @BeforeEach)
 *   albo wymienić całą kuchnię (@DirtiesContext) — to drugie działa zawsze, ale trwa długo.
 *
 * JAK TO DZIAŁA:
 *   1. {@code @SpringBootTest} szuka w górę pakietów klasy {@code @SpringBootConfiguration} (u nas Test05Application)
 *      i buduje z niej kontekst. Zagnieżdżona statyczna klasa {@code @TestConfiguration} w klasie testowej jest
 *      DODAWANA do tej konfiguracji. Zagnieżdżona zwykła {@code @Configuration} — ZASTĘPUJE ją (Spring wtedy w ogóle
 *      nie szuka Test05Application).
 *   2. W testach Boot rejestruje filtr skanowania (TestTypeExcludeFilter), który pomija klasy {@code @TestConfiguration}
 *      i klasy testowe. Osobną klasę {@code @TestConfiguration} dołączasz więc jawnie: {@code @Import(...)}.
 *   3. Beanów o tej samej nazwie nie wolno nadpisywać (Boot ustawia {@code allow-bean-definition-overriding=false}).
 *      Bean testowy o innej nazwie to DRUGI bean tego typu — bez {@code @Primary} Spring wybiera po nazwie parametru.
 *   4. Właściwości testu trafiają do {@code Environment} jako źródła o wyższym priorytecie niż pliki
 *      {@code application*.properties}. Inny zestaw właściwości = inny klucz pamięci podręcznej = osobny kontekst.
 *   5. {@code @DynamicPropertySource} rejestruje dostawcę (Supplier) wartości; Spring pyta go przy pierwszym odczycie
 *      właściwości, czyli w trakcie budowania kontekstu — wtedy port serwera-atrapy jest już znany.
 *
 * SŁÓWKA:
 *   primary = główny (pierwszeństwo przy wstrzykiwaniu); override = nadpisać, zastąpić; import = dołączyć;
 *   inline = wpisany w miejscu (w adnotacji); location = położenie pliku; registry = rejestr; supplier = dostawca
 *   wartości; stub = zaślepka, atrapa z gotowymi odpowiedziami; class mode = tryb klasy; condition = warunek;
 *   captured output = przechwycone wyjście.
 *
 * ZOBACZ TEŻ: s01_core_ioc/Ioc05MultipleCandidates (@Primary, wybór kandydata), s02_configuration/Cfg01Properties,
 *             s02_configuration/Cfg03Profiles (profile), s02_configuration/Cfg05Environment (źródła właściwości),
 *             s03_boot_basics/Boot01AutoConfiguration (ApplicationContextRunner, warunki),
 *             s11_testing/Test01TestSlices (pamięć podręczna kontekstów), s11_testing/Test02MockitoBeans (@MockitoBean),
 *             s11_testing/Test06Testcontainers (@DynamicPropertySource i @ServiceConnection dla bazy w kontenerze),
 *             t32_junit_mockito/JUnit01Basics (cykl życia testu JUnit).
 * </pre>
 */
public final class Test05TestConfiguration {

    private Test05TestConfiguration() {
    }

    // =================================================================================================
    // 1. @TestConfiguration — DOKŁADANIE BEANÓW (Test05NestedTestConfigurationTest, Test05ImportedTestConfigurationTest,
    //    Test05TestBeanTest, Test05NestedConfigurationTest, Test05PitfallsTest)
    // =================================================================================================
    //
    // Najczęstszy powód: CZAS. PromotionService liczy rabat Black Week (23–29 listopada 2026) na podstawie
    // wstrzykniętego beana {@code Clock}. Test „czy w promocji Kawa kosztuje 19.92” musi znać datę — dlatego
    // podstawiamy zegar zatrzymany ({@code Clock.fixed}).
    //
    // PRZED (źle) — w serwisie:   {@code LocalDate today = LocalDate.now();}
    //   test przechodzi tylko w ostatnim tygodniu listopada, a przez resztę roku jest czerwony (albo testujesz
    //   tylko jedną gałąź kodu).
    // PO (dobrze) — w serwisie:   {@code LocalDate today = LocalDate.now(clock);}   (Clock z konstruktora)
    //   aplikacja dostaje {@code Clock.systemUTC()} (ClockConfig), test — {@code Clock.fixed(...)}.
    //
    // DOBRA PRAKTYKA: czas zawsze przez wstrzyknięty {@code Clock}. Dlaczego? Test jest deterministyczny (ten sam
    //   wynik każdego dnia), możesz sprawdzić granice (ostatnia sekunda promocji) i nie musisz czekać na nic.
    //
    // Trzy sposoby podania zegara testowego:
    //   a) zagnieżdżona statyczna klasa w klasie testowej (Test05NestedTestConfigurationTest):
    //        {@code @TestConfiguration static class BeforeBlackWeekClock { @Bean @Primary Clock fixedClock() {...} }}
    //      Spring wykrywa ją SAM i DOKŁADA do konfiguracji aplikacji — w kontekście są oba zegary
    //      ("clock" i "fixedClock"), a ShopInfo, PriceClient itd. dalej istnieją.
    //   b) osobna klasa FixedClockTestConfig (w src/test) + {@code @Import(FixedClockTestConfig.class)}
    //      (Test05ImportedTestConfigurationTest) — gdy ten sam zegar przyda się w wielu klasach testowych.
    //      Bez {@code @Import} nic się nie dzieje: skanowanie w testach pomija klasy {@code @TestConfiguration}
    //      (Test05PitfallsTest: tylko bean "clock").
    //   c) {@code @TestBean} (Spring Framework 6.2+, pakiet org.springframework.test.context.bean.override.convention):
    //        {@code @TestBean Clock clock;  static Clock clock() { return Clock.fixed(...); }}
    //      ZASTĘPUJE istniejący bean (tu: jedyny Clock w kontekście), obiekt daje statyczna metoda o nazwie pola
    //      (Test05TestBeanTest). Działa jak {@code @MockitoBean} (Test02MockitoBeans), ale z prawdziwym obiektem.
    //
    // PUŁAPKA: bean testowy o TEJ SAMEJ nazwie co w aplikacji ({@code @Bean Clock clock()}). Dlaczego? Boot wyłącza
    //   nadpisywanie definicji beanów — kontekst nie wstaje: BeanDefinitionOverrideException ("The bean 'clock' …
    //   could not be registered … overriding is disabled"). Test05PitfallsTest, sameBeanNameFails.
    //   Włączanie {@code spring.main.allow-bean-definition-overriding=true} „żeby działało” to zły pomysł — ukrywa
    //   też przypadkowe kolizje nazw w aplikacji.
    //
    // PUŁAPKA: drugi zegar o innej nazwie BEZ {@code @Primary}. Dlaczego? Kontekst wstaje bez błędu, ale w kontekście
    //   są dwa beany Clock. Przy dwóch kandydatach Spring próbuje dopasować NAZWĘ parametru konstruktora — parametr
    //   PromotionService nazywa się {@code clock}, więc dostaje zegar SYSTEMOWY, a Twój testClock leży nieużywany
    //   (Test05PitfallsTest, secondClockWithoutPrimaryIsIgnored). Test sprawdza wtedy nie to, co myślisz — i jest
    //   zielony albo czerwony zależnie od dnia. Lekarstwo: {@code @Primary} na beanie testowym albo {@code @TestBean}.
    //
    // PUŁAPKA: zagnieżdżona ZWYKŁA {@code @Configuration} zamiast {@code @TestConfiguration} (fakt znany z
    //   s03_boot_basics). Dlaczego to groźne? Spring uznaje ją za JEDYNĄ konfigurację testu i nie szuka Test05Application —
    //   w kontekście jest tylko zegar, nie ma PromotionService ani ShopInfo (Test05NestedConfigurationTest).
    //   Prawdziwy test serwisu skończyłby się NoSuchBeanDefinitionException przy wstrzykiwaniu.
    //
    // DOBRA PRAKTYKA: {@code @TestConfiguration(proxyBeanMethods = false)}. Dlaczego? Metody {@code @Bean} nie wołają się
    //   tu nawzajem, więc podklasa-pośrednik CGLIB jest niepotrzebna — start jest odrobinę szybszy (jak w
    //   s01_core_ioc/Ioc04BeanMethods).

    // =================================================================================================
    // 2. WŁAŚCIWOŚCI I PROFILE TESTU — KTO WYGRYWA (Test05PropertiesTest)
    // =================================================================================================
    //
    // Ta sama właściwość {@code test05.shop.name} ustawiona w wielu miejscach. Od NAJSŁABSZEGO do NAJMOCNIEJSZEGO
    // (każdy szczebel sprawdzony osobną klasą {@code @Nested}):
    //   1. {@code application-test05.properties} z src/main/resources (profil z {@code @ActiveProfiles("test05")})
    //   2. drugi profil: {@code @ActiveProfiles({"test05", "test05extra"})} → plik application-test05extra.properties
    //      z src/test/resources. Później wymieniony profil wygrywa z wcześniejszym.
    //   3. {@code @TestPropertySource(locations = "classpath:test05/override.properties")} — plik tylko dla tej klasy
    //   4. właściwości wpisane w adnotację: {@code @SpringBootTest(properties = "...")} i
    //      {@code @TestPropertySource(properties = "...")}; gdy obie ustawiają to samo — wygrywa @TestPropertySource
    //   5. {@code @DynamicPropertySource} — wygrywa ze wszystkim.
    // Zasada ogólna: „im bliżej testu, tym ważniejsze” — test zawsze może nadpisać konfigurację aplikacji.
    //
    // Gdzie co trzymać:
    //   • jedna–dwie wartości dla jednej klasy → {@code @SpringBootTest(properties = ...)} (widać je od razu w teście),
    //   • zestaw wartości wspólny dla wielu klas testowych → profil w {@code src/test/resources/application-xxx.properties}
    //     + {@code @ActiveProfiles}, albo plik + {@code @TestPropertySource(locations)}.
    //
    // PUŁAPKA: plik w src/test/resources o TEJ SAMEJ nazwie co w src/main/resources (np. application-test05.properties).
    //   Dlaczego? Na classpath testów katalog test-classes jest PRZED classes, a Boot wczytuje plik o danej nazwie raz —
    //   plik testowy ZASŁANIA cały plik główny, nie tylko te klucze, które zmieniłeś. Sprawdzone ręcznie: plik testowy
    //   z samym {@code test05.shop.name} sprawił, że kontekst nie wstał — "Could not resolve placeholder
    //   'test05.price-service.url'". Dlatego testowy profil ma własną nazwę (test05extra).
    //
    // PUŁAPKA: każda klasa testowa z innym zestawem właściwości. Dlaczego? Właściwości są częścią klucza pamięci
    //   podręcznej kontekstów (Test01TestSlices, sekcja 4) — 6 klas {@code @Nested} w Test05PropertiesTest to 6 startów
    //   aplikacji. W prawdziwym projekcie ujednolicaj konfigurację testów (wspólna klasa bazowa albo profil).

    // =================================================================================================
    // 3. @DynamicPropertySource — WARTOŚCI ZNANE DOPIERO W CZASIE URUCHOMIENIA (Test05DynamicPropertySourceTest)
    // =================================================================================================
    //
    // PriceClient woła zewnętrzną usługę cen pod adresem {@code test05.price-service.url}. W teście uruchamiamy
    // serwer-atrapę (PriceStubServer, wbudowany w JDK HttpServer) na porcie 0 — system przydziela WOLNY port. Jaki?
    // Wiadomo dopiero po starcie, więc nie wpiszesz go do pliku ani do adnotacji. Rozwiązanie:
    //
    //   {@code static final PriceStubServer STUB = PriceStubServer.start(Map.of("KAWA", "24.90"));}
    //   {@code @DynamicPropertySource}
    //   {@code static void priceServiceUrl(DynamicPropertyRegistry registry) {}
    //   {@code     registry.add("test05.price-service.url", STUB::baseUrl);   // Supplier — odczyt przy budowie kontekstu}
    //   {@code }}
    //
    // • Metoda MUSI być statyczna: Spring woła ją przed zbudowaniem kontekstu, gdy obiektu testu jeszcze nie ma.
    // • Serwer startuje w polu statycznym (razem z klasą), więc jest gotowy, zanim Spring zapyta o adres.
    // • Dla nieznanego kodu atrapa zwraca 404, a RestClient rzuca {@code HttpClientErrorException.NotFound} — ścieżkę
    //   błędu też sprawdzisz bez prawdziwej usługi.
    // • To samo narzędzie służyło przez lata do podawania adresu bazy z kontenera Testcontainers — w Boot 3.1+ zastępuje
    //   je {@code @ServiceConnection} (Test06Testcontainers, sekcja 2, pokazuje oba).
    // • Nowość Framework 6.2: bean {@code DynamicPropertyRegistrar} robi to samo z poziomu konfiguracji (np. w
    //   {@code @TestConfiguration}), gdy wartość zależy od innego beana. Tu wystarcza adnotacja.
    //
    // PUŁAPKA: metoda {@code @DynamicPropertySource} bez {@code static}. Dlaczego? Spring wymaga metody statycznej
    //   i od razu przerywa test: IllegalStateException "@DynamicPropertySource method '…' must be static" — kontekst
    //   nie powstaje (sprawdzone ręcznie).
    //
    // DOBRA PRAKTYKA: port 0 (wolny) zamiast stałego 8089. Dlaczego? Stały port bywa zajęty (inny proces, równoległe
    //   testy na serwerze CI) — test pada „losowo”, nie z winy kodu.

    // =================================================================================================
    // 4. @DirtiesContext — KIEDY I ILE KOSZTUJE (Test05DirtiesContextTest, Test05ResetStateTest)
    // =================================================================================================
    //
    // ViewCounter to singleton ZE STANEM (licznik wyświetleń). Kontekst z pamięci podręcznej jest współdzielony
    // przez wszystkie klasy testowe o tej samej konfiguracji — licznik nabity w jednej klasie widzi następna.
    //
    // {@code @DirtiesContext} (pakiet org.springframework.test.annotation) mówi: „ten kontekst jest brudny, wyrzuć go
    // z pamięci podręcznej”. Tryby ({@code classMode}): AFTER_CLASS (domyślny), AFTER_EACH_TEST_METHOD, BEFORE_CLASS,
    // BEFORE_EACH_TEST_METHOD; na metodzie: {@code methodMode} BEFORE_METHOD / AFTER_METHOD.
    // Test05DirtiesContextTest (BEFORE_EACH_TEST_METHOD): test 1 nabija licznik do 3, test 2 dostaje NOWY kontekst
    // (inny obiekt ApplicationContext) i licznik od zera.
    //
    // PUŁAPKA: tryb AFTER_* nie chroni PRZED brudem innych klas. Dlaczego? AFTER wyrzuca kontekst PO teście, więc
    //   PIERWSZY test klasy dostaje z pamięci podręcznej kontekst używany wcześniej przez inną klasę. Sprawdzone: z
    //   AFTER_EACH_TEST_METHOD test 1 oczekiwał 3, a dostał 5 — 2 wyświetlenia zostawił Test05ResetStateTest, który
    //   biegł wcześniej w tym samym kontekście. AFTER sprząta po sobie; BEFORE chroni przed innymi.
    //
    // Ile to kosztuje? Każdy wyrzucony kontekst to ponowny start całej aplikacji (u nas Hibernate, pula połączeń,
    // skanowanie) — w tym module ok. 0,1–1 s, w prawdziwym projekcie często kilka sekund. {@code @DirtiesContext}
    // w kilkudziesięciu klasach potrafi wydłużyć testy z minuty do kilkunastu minut.
    //
    // DOBRA PRAKTYKA: zamiast {@code @DirtiesContext} — reset stanu w {@code @BeforeEach} (Test05ResetStateTest:
    //   {@code counter.reset()}; dla bazy: rollback z Test04DataJpaTest albo {@code @Sql} z czyszczeniem). Dlaczego?
    //   Kontekst zostaje w pamięci podręcznej (jeden start dla wielu klas), a test i tak zaczyna od znanego stanu.
    //   {@code @DirtiesContext} zostaw na sytuacje, których nie da się cofnąć (zmieniony singleton bez metody reset,
    //   zamknięty zasób, zmieniona konfiguracja w locie).
    //
    // PUŁAPKA: reset w {@code @BeforeEach} chroni CIEBIE przed innymi, ale nie innych przed Tobą. Dlaczego?
    //   Test05ResetStateTest kończy z licznikiem 2 — klasa bez własnego resetu zobaczy ten stan. Albo resetujesz
    //   wszędzie (wspólna klasa bazowa), albo sprzątasz także po sobie ({@code @AfterEach}).

    // =================================================================================================
    // 5. LEKKIE NARZĘDZIA: ApplicationContextRunner I OutputCaptureExtension (Test05ContextRunnerTest,
    //    Test05OutputCaptureTest)
    // =================================================================================================
    //
    // • {@code ApplicationContextRunner} (pakiet org.springframework.boot.test.context.runner) — znany z
    //   s03_boot_basics/Boot01AutoConfiguration. Buduje mały kontekst TYLKO z podanej konfiguracji
    //   ({@code withUserConfiguration(NewsletterConfig.class)}), z właściwościami ({@code withPropertyValues(...)}),
    //   bez skanowania i bez {@code @SpringBootTest}. Asercje: {@code assertThat(context).hasSingleBean(...)},
    //   {@code hasBean("nazwa")}, {@code doesNotHaveBean(...)}, {@code hasFailed()}. Idealny do sprawdzania warunków
    //   {@code @ConditionalOnProperty}: każdy wariant to milisekundy, nie nowy start aplikacji.
    // • {@code OutputCaptureExtension} (pakiet org.springframework.boot.test.system) — {@code @ExtendWith(...)} na
    //   klasie i parametr {@code CapturedOutput output} w metodzie. Przechwytuje System.out/System.err, a więc
    //   i logi z konsoli. Test05OutputCaptureTest: w Black Week serwis zapisuje „Promocja Black Week aktywna — rabat
    //   20% dla ceny 50.00”, poza promocją — nic. Spring niepotrzebny: serwis tworzymy sami z {@code Clock.fixed}.
    //
    // PUŁAPKA: testowanie logów zamiast zachowania. Dlaczego? Treść logu zmienia się często (ktoś poprawi
    //   literówkę) i test pęka bez zmiany działania aplikacji. Sprawdzaj logi tylko tam, gdzie log JEST
    //   wymaganiem (audyt, ostrzeżenie dla administratora), i tylko kluczowy fragment.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Zegar: serwis bierze {@code Clock} z konstruktora; test podstawia {@code Clock.fixed(instant, ZoneOffset.UTC)}.
     *   • {@code @TestConfiguration} zagnieżdżona (statyczna) → wykrywana sama, DOKŁADA beany.
     *     Osobna → {@code @Import(Klasa.class)}. Zagnieżdżona {@code @Configuration} → ZASTĘPUJE aplikację (błąd!).
     *   • Drugi bean tego samego typu: {@code @Primary}. Ta sama nazwa: BeanDefinitionOverrideException.
     *     Zastąpienie jednego beana: {@code @TestBean} (prawdziwy obiekt) albo {@code @MockitoBean} (atrapa).
     *   • Priorytet właściwości (od najsłabszego): application.properties → profil → późniejszy profil →
     *     {@code @TestPropertySource(locations)} → {@code @SpringBootTest(properties)} → {@code @TestPropertySource(properties)}
     *     → {@code @DynamicPropertySource}.
     *   • Plik testowy o tej samej nazwie co główny ZASŁANIA go w całości — daj mu własną nazwę profilu.
     *   • {@code @DynamicPropertySource}: metoda static, {@code registry.add("klucz", supplier)}; port 0 = wolny port.
     *   • {@code @DirtiesContext}: drogi (nowy start aplikacji); BEFORE_* chroni przed brudem innych, AFTER_* sprząta po
     *     sobie. Tańsza alternatywa: reset stanu w {@code @BeforeEach}.
     *   • {@code ApplicationContextRunner}: warunki konfiguracji w milisekundach; {@code OutputCaptureExtension}: logi.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się zagnieżdżona {@code @TestConfiguration} od zagnieżdżonej {@code @Configuration} w klasie
     *      testowej {@code @SpringBootTest}?
     *   2. Co się stanie? W {@code @TestConfiguration} definiujesz {@code @Bean Clock clock()}, a aplikacja ma już bean
     *      {@code clock} typu Clock.
     *   3. ZNAJDŹ BŁĄD: w {@code @TestConfiguration} jest {@code @Bean Clock testClock()} zwracający 27 listopada, a test
     *      „w Black Week jest rabat” przechodzi tylko w ostatnim tygodniu listopada. Dlaczego i jak to naprawić?
     *   4. Właściwość ustawiono w application-test05.properties, w {@code @SpringBootTest(properties)} i w
     *      {@code @DynamicPropertySource}. Którą wartość zobaczy bean? A gdy dojdzie {@code @TestPropertySource(properties)}
     *      bez {@code @DynamicPropertySource}?
     *   5. Co się stanie? Do src/test/resources dodajesz application-test05.properties z jedną linią
     *      {@code test05.shop.name=Test}, a plik o tej nazwie w src/main/resources ma też adres usługi cen.
     *   6. Dlaczego metoda {@code @DynamicPropertySource} jest statyczna i po co serwerowi-atrapie port 0?
     *   7. ZNAJDŹ BŁĄD: klasa ma {@code @DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)}, a jej pierwszy test widzi
     *      licznik większy niż zero. Co się stało i jak to naprawić bez {@code @DirtiesContext}?
     *   8. Kiedy wybierzesz {@code ApplicationContextRunner} zamiast {@code @SpringBootTest}?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — piszesz TESTY. Szkielet: Test05TestConfigurationExercisesTest (@Tag("cwiczenie")),
    // rozwiązania: s11_testing.solutions.test05_test_configuration.Test05TestConfigurationSolutionsTest (@Tag("wzorzec"))
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): zagnieżdżona {@code @TestConfiguration} z zegarem {@code @Primary} na 2026-11-30T00:00:00Z —
    //   sprawdź, że promocja już się skończyła i że serwis ma TWÓJ zegar. Podpowiedź: sekcja 1, sposób a).
    //
    // ĆWICZENIE 2 (łatwe): {@code @SpringBootTest(properties = "test05.shop.name=Mój sklep")} — ShopInfo zwraca nazwę
    //   z testu, nie z profilu test05. Podpowiedź: sekcja 2, szczebel 4.
    //
    // ĆWICZENIE 3 (średnie): uruchom PriceStubServer z ceną MYSZ = 59.00 i podaj jego adres przez
    //   {@code @DynamicPropertySource}; PriceClient.currentPrice("MYSZ") ma zwrócić 59.00. Podpowiedź: sekcja 3
    //   (pole statyczne, metoda statyczna, {@code @AfterAll} zatrzymuje serwer).
    //
    // ĆWICZENIE 4 (średnie): {@code ApplicationContextRunner} z NewsletterConfig i właściwością
    //   {@code test05.newsletter.enabled=false} — jedyny NewsletterSender zwraca „Newsletter wyłączony”.
    //   Podpowiedź: sekcja 5.
    //
    // ĆWICZENIE 5 (trudniejsze): napisz ZMIENNY zegar testowy (podklasa {@code Clock} z metodą {@code set(Instant)}),
    //   dodaj go jako bean {@code @Primary} i w JEDNYM teście sprawdź chwilę przed promocją (2026-11-22T23:59:59Z),
    //   jej początek (2026-11-23T00:00:00Z) i dzień po (2026-11-30T00:00:00Z). Podpowiedź: Clock ma trzy metody
    //   abstrakcyjne — instant(), getZone(), withZone(ZoneId); wstrzyknij swój zegar do testu przez jego typ.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. {@code @TestConfiguration} jest DODAWANA do konfiguracji znalezionej w górę pakietów (Test05Application) —
     *      aplikacja działa, a beany testowe dochodzą. Zwykła {@code @Configuration} ZASTĘPUJE wyszukiwanie: kontekst ma
     *      tylko jej beany (brak PromotionService, ShopInfo). Sprawdzone: Test05NestedTestConfigurationTest
     *      i Test05NestedConfigurationTest.
     *   2. Kontekst nie wstaje: IllegalStateException „Failed to load ApplicationContext”, a przyczyną jest
     *      BeanDefinitionOverrideException (nadpisywanie beanów wyłączone). Sprawdzone: Test05PitfallsTest, sameBeanNameFails.
     *   3. W kontekście są dwa zegary, a bez {@code @Primary} Spring dopasowuje po nazwie parametru ({@code clock}) —
     *      serwis używa zegara systemowego, więc wynik zależy od dzisiejszej daty. Naprawa: {@code @Primary} na
     *      testClock albo {@code @TestBean} zastępujący bean {@code clock}. Sprawdzone: Test05PitfallsTest,
     *      secondClockWithoutPrimaryIsIgnored.
     *   4. Wartość z {@code @DynamicPropertySource} (najwyższy priorytet). Bez niej, przy obu wpisanych w adnotacje —
     *      wartość z {@code @TestPropertySource(properties)}. Sprawdzone: Test05PropertiesTest (DynamicOnTop, BothInline).
     *   5. Plik testowy zasłania cały plik główny: znika {@code test05.price-service.url} i kontekst nie wstaje
     *      („Could not resolve placeholder 'test05.price-service.url'”). Sprawdzone ręcznie (sekcja 2) — plik usunięty.
     *   6. Spring woła ją przed zbudowaniem kontekstu i przed utworzeniem obiektu testu, więc nie ma jeszcze instancji
     *      (stąd static). Port 0 = system daje dowolny wolny port, więc test nie wywróci się na zajętym porcie;
     *      właśnie dlatego adres trzeba podać dynamicznie. Sprawdzone: Test05DynamicPropertySourceTest.
     *   7. AFTER_EACH_TEST_METHOD wyrzuca kontekst dopiero PO teście; pierwszy test dostał z pamięci podręcznej kontekst,
     *      w którym inna klasa nabiła licznik (Test05ResetStateTest zostawił 2, test widział 5 zamiast 3). Naprawa bez
     *      {@code @DirtiesContext}: {@code counter.reset()} w {@code @BeforeEach} (Test05ResetStateTest); z nim —
     *      BEFORE_EACH_TEST_METHOD (Test05DirtiesContextTest).
     *   8. Gdy sprawdzasz samą konfigurację: warunki ({@code @ConditionalOnProperty}, {@code @ConditionalOnClass}),
     *      zestaw beanów dla różnych właściwości, błąd startu. Jest szybki (bez skanowania, bez autokonfiguracji)
     *      i każdy wariant buduje od nowa. {@code @SpringBootTest} — gdy chcesz zobaczyć działanie całej aplikacji.
     */
    // </editor-fold>
}

package s11_testing.test02_mockito_beans;

/**
 * <pre>
 * TEMAT: Atrapy w testach — czysty Mockito, @MockitoBean, @MockitoSpyBean i @TestBean
 *        (mock = atrapa; spy = szpieg, czyli prawdziwy obiekt z nagrywaniem wywołań; stub/stubbing = nauczenie
 *         atrapy odpowiedzi; verify = sprawdzić wywołanie; captor = „łapacz” argumentu; bean override = podmiana
 *         beana w kontekście)
 *
 * W SKRÓCIE:
 *   Atrapa zastępuje zależność, której w teście nie chcesz albo nie możesz użyć naprawdę: wysyłkę e-maili, płatności,
 *   zewnętrzne API. Bez Springa atrapy tworzy Mockito ({@code @Mock}, {@code @InjectMocks}) — to najszybszy rodzaj
 *   testu. W kontekście Springa podmieniasz bean na atrapę adnotacją {@code @MockitoBean} (szpieg:
 *   {@code @MockitoSpyBean}, własny obiekt: {@code @TestBean}). Stare {@code @MockBean}/{@code @SpyBean} z Boota
 *   zostały w Boot 4 USUNIĘTE. Złota zasada: atrapa zastępuje SĄSIADÓW testowanej klasy, nigdy ją samą.
 *
 * ANALOGIA: próbny alarm pożarowy w biurze.
 *   Nie podpalasz budynku i nie dzwonisz na prawdziwe 112 (efekt uboczny!) — generator dymu (atrapa) robi dokładnie
 *   to, co ustawi instruktor („teraz dym na 2. piętrze”). Obserwator z notesem (szpieg) chodzi za PRAWDZIWYMI
 *   pracownikami i zapisuje, kto którędy wyszedł. Sprawdzasz ludzi i procedurę — czyli to, co naprawdę testujesz.
 *   Gdyby i pracowników zastąpić manekinami, ćwiczenie „udałoby się” zawsze — i nie dowiodłoby niczego.
 *
 * JAK TO DZIAŁA:
 *   1. Mockito generuje w locie klasę atrapy (biblioteka ByteBuddy), która zapisuje każde wywołanie i zwraca
 *      wartości domyślne (null, 0, false, puste kolekcje) albo to, czego ją nauczysz: {@code when(...).thenReturn(...)}.
 *   2. Bez Springa: {@code @ExtendWith(MockitoExtension.class)} tworzy przed każdym testem świeże {@code @Mock},
 *      wstrzykuje je przez konstruktor do {@code @InjectMocks}, a po teście sprawdza tryb ścisły (STRICT_STUBS).
 *   3. Ze Springiem (Spring Framework 6.2+, pakiet {@code org.springframework.test.context.bean.override.mockito}):
 *      ZANIM kontekst wystartuje, Spring podmienia definicję beana danego typu na atrapę ({@code @MockitoBean})
 *      albo owija istniejący bean szpiegiem ({@code @MockitoSpyBean}); ten sam obiekt trafia do pola testu.
 *   4. {@code @TestBean} ({@code org.springframework.test.context.bean.override.convention}) podmienia bean na
 *      obiekt zwrócony przez statyczną metodę fabryczną klasy testowej (domyślnie: metoda o nazwie pola).
 *   5. Każda podmiana zmienia klucz konfiguracji — klasa testowa z inną atrapą dostaje INNY kontekst
 *      (s11_testing/Test01TestSlices, sekcja 6). Po każdym teście Spring zeruje atrapy (reset = AFTER).
 *
 * SŁÓWKA:
 *   mock = atrapa; spy = szpieg; stub = „nauczyć” odpowiedzi; verify = sprawdzić; captor = łapacz argumentu;
 *   inject = wstrzyknąć; strict = ścisły; unnecessary = niepotrzebny; reset = wyzerować; override = podmienić;
 *   factory method = metoda fabryczna; invocation = wywołanie; never = nigdy; answer = odpowiedź atrapy.
 *
 * ZOBACZ TEŻ: t32_junit_mockito/JUnit04Mockito (Mockito od podstaw), t32_junit_mockito/JUnit03AssertJ (asercje),
 *             s11_testing/Test01TestSlices (wycinki i pamięć kontekstów), s11_testing/Test05TestConfiguration
 *             (podmiana beanów klasą @TestConfiguration), s01_core_ioc/Ioc02ConstructorInjection (dlaczego konstruktor
 *             ułatwia testy).
 * </pre>
 */
public final class Test02MockitoBeans {

    private Test02MockitoBeans() {
    }

    // =================================================================================================
    // 1. CZYSTY MOCKITO — TEST JEDNOSTKOWY BEZ SPRINGA
    // =================================================================================================
    //
    //   @ExtendWith(MockitoExtension.class)
    //   class ... {
    //       @Mock ProductCatalog catalog;          // atrapa
    //       @Mock OrderNotifier notifier;          // atrapa
    //       @InjectMocks OrderService service;     // PRAWDZIWY serwis: new OrderService(catalog, notifier)
    //       @Captor ArgumentCaptor<OrderConfirmation> sent;
    //
    //       when(catalog.getBySku("SPO-001")).thenReturn(kawa);      // nauczenie (stubbing)
    //       service.placeOrder("jan@example.com", "SPO-001", 2);     // działanie
    //       verify(notifier).orderPlaced(sent.capture());            // sprawdzenie wywołania + złapanie argumentu
    //   }
    //
    // Narzędzia: thenReturn / thenThrow (odpowiedź), verify(x) / verify(x, never()) / verifyNoInteractions(...)
    // (sprawdzenie), ArgumentCaptor (co dokładnie przekazano). Testowany OrderService ma zależności w KONSTRUKTORZE —
    // dlatego @InjectMocks nie potrzebuje żadnej magii (tak samo jak Spring). Test sekcji 1: Test02MockitoBeansUnitTest.
    //
    // PUŁAPKA: zapomniane nauczenie. Nienauczona atrapa zwraca null, więc serwis pada w środku:
    //   NullPointerException „Cannot invoke "s11_testing.test02_mockito_beans.Product.priceGrosze()" because
    //   "product" is null” (komunikat z prawdziwego uruchomienia). Dlaczego to podstępne? Błąd wskazuje na kod
    //   produkcyjny, a winny jest test. Gdy widzisz NPE w teście z atrapami — najpierw sprawdź nauczenia.
    //
    // DOBRA PRAKTYKA: zostaw tryb ścisły (STRICT_STUBS) — MockitoExtension włącza go domyślnie.
    //   Dlaczego? Nauczenie, którego nikt nie użył, kończy test błędem UnnecessaryStubbingException („Unnecessary
    //   stubbings detected.”). To sygnał, że test opisuje zachowanie, którego już nie ma (martwy kod w teście).
    //   Test: Test02MockitoBeansStrictStubsTest.

    // =================================================================================================
    // 2. @MockitoBean — ATRAPA W KONTEKŚCIE SPRINGA
    // =================================================================================================
    //
    //   @SpringBootTest(webEnvironment = NONE)
    //   class ... {
    //       @Autowired OrderService service;       // prawdziwy, z kontekstu
    //       @MockitoBean OrderNotifier notifier;   // bean emailOrderNotifier ZASTĄPIONY atrapą (nazwa zostaje)
    //   }
    //
    // Kiedy? Gdy testujesz kilka PRAWDZIWYCH beanów razem (serwis + katalog), a tylko jeden sąsiad jest kłopotliwy
    // (e-mail, płatność). Atrapa trafia wszędzie, gdzie wstrzykiwano oryginał. Gdy beana danego typu w kontekście
    // NIE MA, @MockitoBean tworzy nowy (sprawdzone uruchomieniem — powstaje bean o nazwie np.
    // „java.util.function.Supplier#0”). Test sekcji 2: Test02MockitoBeansMockitoBeanTest.
    //
    // PUŁAPKA: @MockBean z poradników. W Boot 4 adnotacji org.springframework.boot.test.mock.mockito.MockBean już nie
    //   ma — import kończy się błędem kompilacji „package org.springframework.boot.test.mock.mockito does not exist”.
    //   Dlaczego? Boot oznaczył @MockBean/@SpyBean jako przestarzałe w 3.4 (gdy Spring Framework 6.2 dał własne
    //   @MockitoBean/@MockitoSpyBean) i usunął je w 4.0. Zamiana jest prosta: @MockBean → @MockitoBean,
    //   @SpyBean → @MockitoSpyBean (inny pakiet: org.springframework.test.context.bean.override.mockito).
    //
    // PYTANIE REKRUTACYJNE: czym różni się @Mock od @MockitoBean? @Mock tworzy atrapę w czystym teście (bez
    //   kontekstu); @MockitoBean umieszcza atrapę W KONTEKŚCIE Springa, zamiast beana — i zmienia klucz kontekstu.

    // =================================================================================================
    // 3. @MockitoSpyBean — SZPIEG NA PRAWDZIWYM BEANIE
    // =================================================================================================
    //
    //   @MockitoSpyBean ProductCatalog catalog;   // prawdziwy InMemoryProductCatalog + nagrywanie wywołań
    //   verify(catalog).getBySku("SPO-002");       // prawdziwe dane, a wywołanie sprawdzone
    //   doReturn(odkurzacz).when(catalog).getBySku("DOM-003");   // podmiana JEDNEJ odpowiedzi
    //
    // Szpieg wymaga ISTNIEJĄCEGO beana — bez niego kontekst nie wstaje: „Unable to select a bean to wrap: there
    // are no beans of type …” (sprawdzone uruchomieniem). Test sekcji 3: Test02MockitoBeansSpyBeanTest.
    //
    // PUŁAPKA: when(spy.metoda(...)).thenReturn(...) na szpiegu. Dlaczego? Wyrażenie spy.metoda(...) WYKONUJE się
    //   najpierw naprawdę (zanim Mockito dostanie when) — tu prawdziwy katalog rzuca NoSuchElementException
    //   „Nieznany produkt: DOM-003” i nauczenie nigdy nie dochodzi do skutku. Na szpiegach używaj
    //   doReturn(...).when(spy).metoda(...) — wtedy prawdziwa metoda nie jest wołana. Sprawdzone testem sekcji 3.

    // =================================================================================================
    // 4. @TestBean — WŁASNY OBIEKT ZAMIAST BEANA (BEZ MOCKITO)
    // =================================================================================================
    //
    //   @TestBean ProductCatalog catalog;                   // Spring szuka statycznej metody catalog()
    //   static ProductCatalog catalog() { return sku -> new Product(sku, "Produkt testowy " + sku, 100); }
    //
    // Metoda fabryczna: statyczna, bez parametrów, w klasie testowej; domyślnie nazywa się TAK JAK POLE (inną nazwę
    // podasz w atrybucie methodName). Kiedy zamiast atrapy? Gdy prosta, prawdziwa implementacja (tu katalog „każdy
    // SKU po 1 zł”) jest czytelniejsza niż kilka nauczeń when(...). Test sekcji 4: Test02MockitoBeansTestBeanTest.

    // =================================================================================================
    // 5. ZEROWANIE ATRAP MIĘDZY TESTAMI
    // =================================================================================================
    //
    // Czysty Mockito: JUnit tworzy nowy obiekt klasy testowej dla każdej metody, a MockitoExtension — nowe atrapy.
    // Spring: atrapa żyje w kontekście, który jest WSPÓŁDZIELONY przez wiele testów (pamięć podręczna), więc po
    // każdym teście Spring ją zeruje — znika nauczenie i zapis wywołań (domyślnie reset = MockReset.AFTER; inne
    // wartości: BEFORE, NONE). Test sekcji 5: Test02MockitoBeansResetTest (test 2 nie widzi wyjątku z testu 1).
    //
    // PUŁAPKA: atrapa zrobiona ręcznie w klasie @TestConfiguration (Mockito.mock(...) w metodzie @Bean).
    //   Dlaczego? Spring jej nie zeruje — nauczenia i zapis wywołań przeciekają między testami i klasami testowymi
    //   (kontekst jest współdzielony). Sprawdzone uruchomieniem: w drugim teście taka atrapa pokazywała 2 wywołania
    //   zamiast 1 (zostało wywołanie z pierwszego testu). @MockitoBean robi zerowanie za Ciebie.

    // =================================================================================================
    // 6. PUŁAPKA: ATRAPA WSZYSTKIEGO = TEST NICZEGO
    // =================================================================================================
    //
    //   @MockitoBean OrderService service;   // ŹLE: atrapa KLASY TESTOWANEJ
    //   given(service.placeOrder(...)).willReturn(potwierdzenieZKwotą99999);
    //   assertThat(service.placeOrder(...).totalGrosze()).isEqualTo(99999);   // zielone — i bezwartościowe
    //
    // Test sekcji 6 (Test02MockitoBeansPitfallTest) przechodzi z kwotą 99999, choć prawdziwy serwis liczy 12998:
    // sprawdza wyłącznie to, czego sami nauczyliśmy atrapę (tautologia). To samo, w mniej oczywistej postaci: test,
    // w którym atrapami są WSZYSTKIE zależności, a asercje sprawdzają tylko „czy wywołano metodę X” — zmień logikę
    // serwisu, a test nadal świeci na zielono.
    //
    // DOBRA PRAKTYKA: atrapą zastępuj tylko granice systemu (e-mail, płatności, zewnętrzne API, zegar), a logikę
    //   i proste zależności zostaw prawdziwe. Dlaczego? Test ma wykrywać błędy w PRAWDZIWYM kodzie; każda atrapa to
    //   kawałek, którego test już nie sprawdza.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Bez Springa: @ExtendWith(MockitoExtension.class) + @Mock + @InjectMocks + @Captor (najszybciej).
     *   • when(x.m()).thenReturn(v) / thenThrow(e); verify(x).m(); verify(x, never()).m(any());
     *     verifyNoInteractions(x); ArgumentCaptor.capture() / getValue().
     *   • W kontekście: @MockitoBean (atrapa zamiast beana; brak beana → nowy bean), @MockitoSpyBean (szpieg na
     *     istniejącym beanie), @TestBean (obiekt z metody fabrycznej o nazwie pola).
     *   • Pakiety: org.springframework.test.context.bean.override.mockito (@MockitoBean, @MockitoSpyBean),
     *     org.springframework.test.context.bean.override.convention (@TestBean).
     *   • @MockBean / @SpyBean — USUNIĘTE w Boot 4 (przestarzałe od 3.4).
     *   • Szpieg: doReturn(v).when(spy).m() — nigdy when(spy.m()).
     *   • Atrapy w kontekście są zerowane po każdym teście (MockReset.AFTER).
     *   • Atrapa zastępuje sąsiadów, nigdy testowaną klasę.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się @Mock (Mockito) od @MockitoBean (Spring)? Kiedy wybierzesz które?
     *   2. Co się stanie? W teście z @Mock ProductCatalog zapominasz o when(...), a serwis woła
     *      catalog.getBySku("SPO-001").priceGrosze().
     *   3. Co się stanie? Na szpiegu @MockitoSpyBean ProductCatalog piszesz
     *      when(catalog.getBySku("DOM-003")).thenReturn(odkurzacz), a prawdziwy katalog nie zna DOM-003.
     *   4. ZNAJDŹ BŁĄD: w projekcie Boot 4 test ma import org.springframework.boot.test.mock.mockito.MockBean.
     *   5. Do czego służy ArgumentCaptor i czym różni się od zwykłego verify(x).m(argument)?
     *   6. Dlaczego @MockitoBean musi zerować atrapy po teście, a @Mock w MockitoExtension — nie?
     *   7. ZNAJDŹ BŁĄD: test ma @MockitoBean OrderService i sprawdza wynik service.placeOrder(...). Co tu jest nie tak?
     *   8. Co się stanie, gdy w teście z MockitoExtension nauczysz atrapę odpowiedzi, której test nigdy nie użyje?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — uzupełniasz TESTY w pliku Test02MockitoBeansExercisesTest (src/test/java, ten sam pakiet).
    // Usuń todo() i dopisz brakujący kawałek; asercje są gotowe.
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): naucz atrapę katalogu, że getBySku("KSI-002") zwraca Product("KSI-002",
    //   "Java. Podstawy", 12900). Podpowiedź: when(...).thenReturn(...).
    //
    // ĆWICZENIE 2 (średnie): sprawdź, że serwis powiadomił o zamówieniu, i złap przekazane potwierdzenie
    //   ArgumentCaptor-em. Podpowiedź: verify(notifier).orderPlaced(captor.capture()); captor.getValue().
    //
    // ĆWICZENIE 3 (średnie): naucz atrapę katalogu rzucać NoSuchElementException("Nieznany produkt: ODZ-003")
    //   dla "ODZ-003". Test sprawdzi też, że powiadomienie nie wyszło. Podpowiedź: thenThrow(...).
    //
    // ĆWICZENIE 4 (trudniejsze): w teście ze Springiem naucz SZPIEGA katalogu nowego produktu DOM-003
    //   (Odkurzacz, 59900 gr) tak, żeby stare produkty zostały prawdziwe. Podpowiedź: sekcja 3 — która składnia
    //   nie woła prawdziwej metody?

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @Mock tworzy atrapę w czystym teście Mockito — bez kontekstu, w milisekundach; testowaną klasę budujesz
     *      sam (albo @InjectMocks). @MockitoBean wstawia atrapę do kontekstu Springa w miejsce beana — przydaje się,
     *      gdy testujesz kilka prawdziwych beanów razem (albo kontroler w @WebMvcTest). Domyślnie wybieraj @Mock;
     *      @MockitoBean tylko tam, gdzie i tak potrzebujesz kontekstu.
     *   2. Atrapa bez nauczenia zwraca null, więc serwis rzuca NullPointerException: „Cannot invoke
     *      "s11_testing.test02_mockito_beans.Product.priceGrosze()" because "product" is null”. Sprawdzone testem
     *      sekcji 1.
     *   3. Wyrażenie catalog.getBySku("DOM-003") wykonuje się naprawdę, zanim Mockito dostanie when — prawdziwy
     *      katalog rzuca NoSuchElementException „Nieznany produkt: DOM-003”. Poprawnie:
     *      doReturn(odkurzacz).when(catalog).getBySku("DOM-003"). Sprawdzone testem sekcji 3.
     *   4. Błąd kompilacji „package org.springframework.boot.test.mock.mockito does not exist” — @MockBean usunięto
     *      w Boot 4. Zamień na @MockitoBean z org.springframework.test.context.bean.override.mockito.
     *   5. ArgumentCaptor „łapie” obiekt przekazany do atrapy, żeby sprawdzić go dokładnie (wiele pól, AssertJ).
     *      verify(x).m(argument) tylko porównuje argument przez equals — wystarcza, gdy znasz cały obiekt z góry.
     *   6. Atrapa z @MockitoBean żyje w kontekście współdzielonym przez wiele testów (pamięć podręczna), więc bez
     *      zerowania nauczenia i zapis wywołań przeciekałyby między testami. @Mock z MockitoExtension powstaje od
     *      nowa dla każdej metody testowej. Sprawdzone testem sekcji 5.
     *   7. Atrapą jest klasa testowana — test sprawdza tylko to, czego nauczono atrapę (tautologia), prawdziwy kod
     *      w ogóle się nie wykonuje. Atrapą zastąp sąsiadów (np. OrderNotifier), a OrderService weź prawdziwy.
     *      Sprawdzone testem sekcji 6.
     *   8. Po teście MockitoExtension (tryb STRICT_STUBS) zgłosi UnnecessaryStubbingException „Unnecessary stubbings
     *      detected.” — nieużyte nauczenie to błąd testu. Sprawdzone testem Test02MockitoBeansStrictStubsTest.
     */
    // </editor-fold>
}

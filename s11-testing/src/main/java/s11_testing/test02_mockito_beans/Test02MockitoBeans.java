package s11_testing.test02_mockito_beans;

/**
 * <pre>
 * TEMAT: Atrapy w kontekście Springa — @MockitoBean i @MockitoSpyBean, reset atrap, atrapa zewnętrznej usługi
 *        (mock = atrapa, udawany obiekt z ustawionymi odpowiedziami; spy = szpieg, prawdziwy obiekt z podsłuchem;
 *         stub = ustawienie odpowiedzi atrapy; verify = sprawdzenie, jak atrapę wywołano; captor = łapacz argumentu)
 *
 * W SKRÓCIE:
 *   Test jednej warstwy nie chce wołać prawdziwej reszty: kontroler nie powinien dotykać bazy, a serwis — zewnętrznej
 *   usługi przez internet. W Springu podmieniasz wtedy BEAN w kontekście testu. {@code @MockitoBean} wstawia atrapę
 *   Mockito w miejsce beana (albo dokłada ją, gdy beana nie było), {@code @MockitoSpyBean} owija prawdziwy bean
 *   szpiegiem. Test ustawia odpowiedzi atrapy (given/willReturn/willThrow) i po akcji sprawdza wywołania (verify).
 *   Uwaga: stare {@code @MockBean} i {@code @SpyBean} z tutoriali zniknęły w Boot 4.
 *
 * ANALOGIA: dubler w filmie.
 *   Scena z karambolem nie może się odbyć z prawdziwym aktorem. Wstawiasz dublera (atrapę): wygląda jak aktor,
 *   robi to, co mu zapiszesz w scenariuszu (stub), a po zdjęciu reżyser sprawdza, czy zrobił, co miał (verify).
 *   Szpieg to aktor z mikrofonem: gra naprawdę, ale wszystko, co powiedział, jest nagrane. Po każdym ujęciu
 *   dublera ubiera się od nowa (reset) — żeby ujęcia nie wpływały na siebie.
 *
 * JAK TO DZIAŁA:
 *   1. Adnotacja na polu klasy testowej ({@code @MockitoBean ProductService service}) trafia do mechanizmu
 *      "bean override" (nadpisywanie beanów) Spring Test — jest częścią Spring Framework (pakiet
 *      {@code org.springframework.test.context.bean.override.mockito}), a nie Boota.
 *   2. Przed odświeżeniem kontekstu Spring zamienia definicję beana tego typu na atrapę Mockito. Pole testu
 *      dostaje TEN SAM obiekt, który trafił do kontekstu — dlatego {@code given(service.find(1L))} wpływa na kontroler.
 *   3. Gdy beana tego typu nie ma, {@code @MockitoBean} dokłada nowy bean-atrapę (chyba że ustawisz
 *      {@code enforceOverride = true} — wtedy błąd). {@code @MockitoSpyBean} bez beana to błąd.
 *   4. Zestaw atrap jest częścią klucza pamięci podręcznej kontekstów (Test01TestSlices, sekcja 4): inny
 *      zestaw = nowy kontekst.
 *   5. Po każdym teście Spring "czyści" atrapy (reset AFTER): znikają ustawione odpowiedzi i zapamiętane wywołania.
 *
 * SŁÓWKA:
 *   override = nadpisać, zastąpić; bean override = nadpisanie beana; stubbing = ustawianie odpowiedzi atrapy;
 *   interaction = interakcja, wywołanie metody atrapy; fake = prosta działająca podróbka (np. lambda); side effect =
 *   efekt uboczny; argument matcher = dopasowanie argumentu (np. any()); invocation = wywołanie.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest05CrudApi (@MockitoBean w teście kontrolera),
 *             s11_testing/Test01TestSlices (wycinki, pamięć podręczna kontekstów),
 *             s11_testing/Test03MockMvcTester (asercje na odpowiedzi kontrolera),
 *             s11_testing/Test05TestConfiguration (własne beany w teście zamiast atrap),
 *             s13_rest_client/* (klient HTTP, którego tu zastępujemy atrapą),
 *             t32_junit_mockito/JUnit04Mockito (Mockito bez Springa: mock, when, verify, captor).
 * </pre>
 */
public final class Test02MockitoBeans {

    private Test02MockitoBeans() {
    }

    // =================================================================================================
    // 1. @MockitoBean — atrapa zamiast beana (Test02MockitoBeanWebTest)
    // =================================================================================================
    //
    //   @WebMvcTest(ProductController.class)
    //   class Test02MockitoBeanWebTest {
    //       @Autowired MockMvc mockMvc;
    //       @MockitoBean ProductService service;       // atrapa wstawiona do kontekstu
    //
    //       given(service.find(1L)).willReturn(new Product(...));    // ustaw odpowiedź (BDDMockito)
    //       mockMvc.perform(get("/test02/products/1"));              // kontroler woła atrapę
    //       verify(service).find(1L);                                // sprawdź wywołanie
    //   }
    //
    // • given(...).willReturn(...) — odpowiedź atrapy; willThrow(...) — wyjątek; willAnswer(...) — obliczana.
    //   To ten sam mechanizm co when(...).thenReturn(...) (when/then to starszy zapis, given/will to styl BDD).
    // • verify(service).find(1L) — "zawołano dokładnie raz z 1"; verify(service, never()).create(any()) — "nie
    //   zawołano"; verifyNoInteractions(service) — "atrapy nie dotknięto wcale" (np. gdy dane były błędne).
    //   Styl BDD: {@code then(service).should().create(...)}.
    // • ArgumentCaptor — łapie argument, z którym zawołano atrapę, żeby go dokładnie obejrzeć:
    //   {@code verify(service).create(captor.capture()); assertThat(captor.getValue()).isEqualTo(...)}.
    //   Przydatny, gdy kontroler SAM buduje argument (tu: zamienia JSON na ProductRequest) i chcesz sprawdzić jego
    //   treść. (Test: argumentCaptor w Test02MockitoBeanWebTest.)
    // • Atrapa niczego nie umie, dopóki jej nie nauczysz: nieustawiona metoda zwraca null (dla obiektów),
    //   0, false albo pustą kolekcję (dla List i Optional Mockito zwraca puste). Dlatego w teście "unstubbedAndNever"
    //   wywołanie bez ustawienia daje pustą odpowiedź.
    //
    // PUŁAPKA: {@code @MockBean} i {@code @SpyBean} z tutoriali i StackOverflow. Dlaczego? Zostały USUNIĘTE w Boot 4
    //   (były przestarzałe od Boot 3.4). Import {@code org.springframework.boot.test.mock.mockito.MockBean} się nie
    //   skompiluje — klasy nie ma (Test02PitfallsTest to sprawdza). Użyj {@code @MockitoBean} i
    //   {@code @MockitoSpyBean} z pakietu {@code org.springframework.test.context.bean.override.mockito}.
    //
    // DOBRA PRAKTYKA: ustawiaj odpowiedzi tylko tych metod, które test naprawdę wykorzystuje, i sprawdzaj (verify)
    //   tylko wywołania, które są CZĘŚCIĄ umowy (np. "serwis dostał dane z żądania"). Dlaczego? Każdy zbędny stub i
    //   verify wiąże test z wewnętrznymi szczegółami; przy refaktoryzacji zielone dotąd testy robią się czerwone
    //   mimo poprawnego zachowania (sekcja 5).

    // =================================================================================================
    // 2. @MockitoSpyBean — prawdziwy bean z podsłuchem (Test02SpyBeanTest)
    // =================================================================================================
    //
    // Szpieg opakowuje ISTNIEJĄCY bean: wszystkie metody działają naprawdę, ale Mockito zapisuje wywołania. U nas
    // AuditLog (dziennik zdarzeń) — chcemy, żeby naprawdę zapisywał, a jednocześnie sprawdzić, co serwis do niego wysłał.
    //
    //   @MockitoSpyBean AuditLog audit;
    //   service.create(...);
    //   assertThat(audit.events()).containsExactly("created:3");   // prawdziwy efekt
    //   verify(audit).record("created:3");                         // i podsłuch
    //
    // Kiedy atrapa, a kiedy szpieg? Atrapa — gdy zależność jest zewnętrzna, wolna lub niedeterministyczna (HTTP, poczta,
    // zegar). Szpieg — gdy zależność jest tania i prawdziwa, a Ty chcesz tylko zobaczyć wywołania albo podmienić
    // JEDNĄ metodę. Jeszcze lepiej: nie użyć ani jednego, gdy wystarczy sprawdzić wynik (sekcja 5).
    //
    // PUŁAPKA: ustawianie odpowiedzi szpiega zapisem {@code when(szpieg.metoda(...)).thenReturn(...)}. Dlaczego?
    //   W nawiasie {@code szpieg.metoda(...)} wykonuje się PRAWDZIWA metoda (i jej efekt uboczny!), dopiero potem
    //   Mockito dostaje wartość do podmiany. Test "whenCallsRealMethodDoReturnDoesNot": po
    //   {@code when(audit.record("x")).thenReturn(100)} wpis "x" JEST już w dzienniku. Poprawnie:
    //   {@code doReturn(100).when(audit).record("x")} — prawdziwa metoda nie jest wołana ani przy ustawianiu, ani
    //   po nim.
    //
    // PUŁAPKA: stan prawdziwego beana przecieka między testami. Dlaczego? Reset szpiega czyści tylko dane Mockito
    //   (ustawienia i wywołania), a NIE stan wewnętrzny prawdziwego obiektu — lista zdarzeń w AuditLog zostaje.
    //   Dlatego test sprząta sam w {@code @BeforeEach} ({@code audit.clear()}, {@code service.reset()}).
    //
    // PUŁAPKA: {@code @MockitoSpyBean} na beanie, którego nie ma. Dlaczego? Nie ma czego owijać — kontekst się nie
    //   podnosi: "Unable to select a bean to wrap: there are no beans of type …" (Test02PitfallsTest).

    // =================================================================================================
    // 3. RESET ATRAP MIĘDZY TESTAMI I @Nested (Test02MockResetTest)
    // =================================================================================================
    //
    // Kontekst jest wspólny dla wielu testów (pamięć podręczna — Test01TestSlices), więc wspólna jest też atrapa.
    // Spring musi ją czyścić, inaczej ustawienia i wywołania z jednego testu wpływałyby na następny. Steruje tym
    // pole {@code reset} w adnotacji — typ {@code MockReset}:
    //
    //   MockReset.AFTER  (DOMYŚLNIE) — reset po każdym teście,
    //   MockReset.BEFORE — reset przed każdym testem,
    //   MockReset.NONE   — bez resetu (stan przecieka do następnych testów).
    //
    // Test "defaultResetIsAfter" sprawdza wartość domyślną refleksją. Test w dwóch odsłonach (@Order 1 i 2)
    // pokazuje skutek: w teście 2 atrapa z reset=AFTER nie ma ani wywołań, ani ustawionej odpowiedzi (find → null),
    // a atrapa z reset=NONE nadal pamięta wywołanie z testu 1. Kolejność jest tam ustalona CELOWO — to demonstracja;
    // w prawdziwych testach nie zależ od kolejności.
    //
    // Klasa {@code @Nested} dostaje konfigurację z klasy zewnętrznej: ta sama atrapa, ten sam MockMvc, ten sam
    // kontekst (test "nestedUsesOuterMock"). Dlatego wspólne atrapy deklarujesz raz, w klasie zewnętrznej.
    //
    // DOBRA PRAKTYKA: zostaw domyślny reset (AFTER) i ustawiaj odpowiedzi w samym teście (albo w @BeforeEach).
    //   Dlaczego? Test jest wtedy samodzielny i czytelny; "ustawienie w jednym miejscu, użycie w innym" daje
    //   testy, które da się zrozumieć tylko po przeczytaniu całego pliku.

    // =================================================================================================
    // 4. ATRAPA ZEWNĘTRZNEJ USŁUGI I ŚCIEŻKA BŁĘDU (Test02ServiceWithMockClientTest)
    // =================================================================================================
    //
    // ProductService.refreshPrice pyta PriceClient (w produkcji RemotePriceClient — HTTP) o aktualną cenę.
    // W teście NIE WOLNO używać sieci (usługa może nie działać, odpowiadać wolno, zmieniać dane), więc
    //   @MockitoBean PriceClient priceClient;
    // zastępuje bean RemotePriceClient — jest dokładnie jeden bean tego typu, i to atrapa (test "mockReplacesTheRealBean").
    // Reszta (serwis, dziennik, zegar) jest prawdziwa: sprawdzamy ich WSPÓŁPRACĘ.
    //
    //   • Ścieżka szczęśliwa: given(priceClient.currentPrice("Kawa")).willReturn(26.50) → cena zapisana,
    //     w dzienniku "price-refreshed:1".
    //   • Ścieżka błędu: given(...).willThrow(new PriceUnavailableException("timeout")) → wyjątek leci dalej, cena
    //     zostaje 24,90, w dzienniku "price-refresh-failed:1". Takiego scenariusza prawie nie da się wywołać na
    //     żądanie z prawdziwą usługą — atrapa daje go za darmo.
    //   • Na poziomie kontrolera (Test02MockitoBeanWebTest): willThrow w atrapie SERWISU daje odpowiedź 503 z
    //     ProblemDetail — sprawdzasz, czy advice mapuje wyjątek na kod.
    // Tę samą ścieżkę możesz zobaczyć na żywo: uruchom Test02Application i wykonaj zapytanie 4 z pliku
    // http/test02_mockito_beans.http (usługa cen nie istnieje → 503).
    //
    // PUŁAPKA: atrapa bez ustawionej odpowiedzi. Dlaczego? Zwraca null, więc serwis w teście
    //   "unstubbedMockReturnsNull" zapisuje produkt z ceną null i test nie zauważa problemu, dopóki
    //   ktoś nie sprawdzi ceny. Zawsze ustaw odpowiedź każdej metody, od której zależy wynik, i sprawdź wynik.
    //
    // DOBRA PRAKTYKA: zewnętrzną usługę chowaj za interfejsem (PriceClient) we własnym pakiecie. Dlaczego?
    //   Test podstawia atrapę albo fake bez znajomości HTTP, a zmiana dostawcy (inny adres, inny protokół) nie
    //   dotyka serwisu. Kod HTTP dostaje własny, osobny test (np. z MockRestServiceServer — s13_rest_client).

    // =================================================================================================
    // 5. PUŁAPKI: NADMIERNE MOCKOWANIE, TYPY WARTOŚCI, BRAKUJĄCY BEAN (Test02FakeVsMockTest, Test02PitfallsTest)
    // =================================================================================================
    //
    // • Nadmierne mockowanie. Test, w którym każda zależność jest atrapą, a asercje to same verify(...), sprawdza
    //   IMPLEMENTACJĘ (kto kogo zawołał), a nie ZACHOWANIE (co dostaje użytkownik).
    //
    //   PRZED (implementacja — pęka przy każdej refaktoryzacji):
    //     verify(priceClient).currentPrice("Kawa"); verify(repository).save(any()); verifyNoMoreInteractions(...);
    //   PO (zachowanie — przetrwa refaktoryzację):
    //     assertThat(service.refreshPrice(1L).price()).isEqualByComparingTo("26.50");
    //
    //   Najprostsze lekarstwo: fake (prosta działająca podróbka) zamiast atrapy. PriceClient ma jedną metodę, więc
    //   fake to lambda: {@code name -> new BigDecimal("26.50")}. Test Test02FakeVsMockTest sprawdza wynik
    //   (nowa cena, wpis w dzienniku) bez Mockito i bez Springa.
    //
    // • Typy wartości (rekordy, DTO, encje bez zachowań). Nie mockuj ich — utwórz prawdziwy obiekt. Mockito 5
    //   POTRAFI zamockować nawet rekord (jest final), ale zwraca z niego null i 0, więc to bezsensowne
    //   (test "mockingValueTypeIsPointless"). Mockuj tylko to, co ma ZACHOWANIE i jest zewnętrzne, wolne albo
    //   niedeterministyczne.
    //
    // • @MockitoBean na beanie, którego nie ma. Po cichu powstaje NOWY bean-atrapa — literówka w typie albo
    //   zmiana nazwy klasy nie zostanie zauważona, a test sprawdza coś, co aplikacja nie używa. Zabezpieczenie:
    //   {@code @MockitoBean(enforceOverride = true)} — wtedy brak beana kończy się błędem kontekstu
    //   "Unable to override bean: there are no beans of type …". (Test02PitfallsTest.)
    //
    // • Wpływ na pamięć podręczną kontekstów. Każdy inny zestaw @MockitoBean/@MockitoSpyBean to nowy kontekst
    //   (Test01TestSlices, sekcja 4). Wspólne atrapy zbieraj w klasie bazowej albo @TestConfiguration.
    //
    // PUŁAPKA: mockowanie klasy, którą testujesz. Dlaczego? Sprawdzasz wtedy atrapę, a nie swój kod: test zawsze
    //   przechodzi. Mockuj jej ZALEŻNOŚCI (tu: PriceClient), nigdy testowany obiekt.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code @MockitoBean} (org.springframework.test.context.bean.override.mockito) zastępuje bean atrapą (albo
     *     dokłada nowy, gdy beana nie było; enforceOverride = true wymusza zastąpienie). {@code @MockBean}/{@code @SpyBean}
     *     usunięte w Boot 4.
     *   • Ustawianie: {@code given(m.f()).willReturn(x)}, {@code willThrow(e)}; sprawdzanie: {@code verify(m).f()},
     *     {@code never()}, {@code verifyNoInteractions(m)}, {@code then(m).should()}; argumenty: ArgumentCaptor.
     *   • Nieustawiona atrapa zwraca null/0/false/pustą kolekcję.
     *   • {@code @MockitoSpyBean}: prawdziwy bean + podsłuch; na szpiegu {@code doReturn(x).when(spy).f()}, NIE
     *     {@code when(spy.f())} (wykonuje prawdziwą metodę). Stan prawdziwego beana sprzątaj sam.
     *   • Reset: domyślnie MockReset.AFTER (po każdym teście); BEFORE, NONE; atrapa z klasy zewnętrznej działa w @Nested.
     *   • Zewnętrzną usługę chowaj za interfejsem; ścieżkę błędu testuj przez willThrow; fake (lambda) bywa lepszy niż atrapa.
     *   • Nie mockuj typów wartości ani testowanej klasy; testuj zachowanie (wynik), nie implementację (verify wszędzie).
     *   • Inny zestaw atrap = inny kontekst w pamięci podręcznej.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się atrapa ({@code @MockitoBean}) od szpiega ({@code @MockitoSpyBean}) i kiedy wybierzesz którego?
     *   2. ZNAJDŹ BŁĄD: test zaczyna się od {@code import org.springframework.boot.test.mock.mockito.MockBean;}
     *      (kopia z tutoriala). Co się stanie przy kompilacji w Boot 4 i czym to zastąpisz?
     *   3. Co się stanie? W teście z {@code @MockitoBean PriceClient} wołasz {@code service.refreshPrice(2L)}
     *      i nie ustawiasz żadnej odpowiedzi atrapy.
     *   4. Co się stanie? {@code @MockitoBean MissingPort missing;} — a w aplikacji nie ma żadnego beana tego typu.
     *      A co, gdy dopiszesz {@code enforceOverride = true}? A przy {@code @MockitoSpyBean}?
     *   5. ZNAJDŹ BŁĄD: {@code when(audit.record("x")).thenReturn(100);} na szpiegu {@code @MockitoSpyBean AuditLog audit}.
     *      Test jest zielony, ale w dzienniku zdarzeń jest już wpis "x", którego nikt się nie spodziewał. Dlaczego i jak
     *      to naprawić?
     *   6. Test 1 ustawił odpowiedź atrapy i ją wywołał. Czy test 2 w tej samej klasie zobaczy ustawienie i zapisane
     *      wywołanie? Co zmienia {@code reset = MockReset.NONE}?
     *   7. Dlaczego w teście jednostkowym serwisu lepiej użyć lambdy {@code name -> new BigDecimal("26.50")} niż atrapy
     *      z {@code verify}?
     *   8. Jak wpływa na szybkość testów to, że 10 klas testowych ma 10 różnych zestawów {@code @MockitoBean}?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — tym razem piszesz TESTY. Szkielet: Test02MockitoExercisesTest (@Tag("cwiczenie")),
    // rozwiązania: s11_testing.solutions.test02_mockito_beans.Test02MockitoSolutionsTest (@Tag("wzorzec"))
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): w @WebMvcTest z atrapą ProductService sprawdź, że GET /test02/products/1 daje 200 i nazwę
    //   "Kawa", a serwis został zawołany z numerem 1. Podpowiedź: given(...).willReturn(new Product(...)), verify(...).
    //
    // ĆWICZENIE 2 (łatwe): napisz zwykły test jednostkowy z fake: PriceClient jako lambda zwracająca 30,00; po
    //   refreshPrice(2) cena Herbaty ma wynosić 30,00. Podpowiedź: sekcja 5 — new ProductService(lambda, new AuditLog(), clock).
    //
    // ĆWICZENIE 3 (średnie): w @SpringBootTest z @MockitoBean PriceClient ustaw willThrow(PriceUnavailableException)
    //   i sprawdź: wyjątek, cena Herbaty bez zmian, w dzienniku "price-refresh-failed:2".
    //   Podpowiedź: sekcja 4; sprzątaj stan (service.reset(), audit.clear()).
    //
    // ĆWICZENIE 4 (trudniejsze): z @MockitoSpyBean AuditLog i ArgumentCaptor sprawdź kolejność zdarzeń po
    //   service.create(...) i service.refreshPrice(1L): "created:3", "price-refreshed:1".
    //   Podpowiedź: sekcja 2 — verify(audit, times(2)).record(captor.capture()); captor.getAllValues().

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Atrapa jest pusta — nic nie robi, dopóki jej nie nauczysz; szpieg to prawdziwy bean z podsłuchem (działa
     *      naprawdę). Atrapa: zależność zewnętrzna, wolna, niedeterministyczna (HTTP, poczta). Szpieg: tania prawdziwa
     *      zależność, której wywołania chcesz zobaczyć albo której jedną metodę chcesz podmienić. Sprawdzone w
     *      Test02ServiceWithMockClientTest i Test02SpyBeanTest.
     *   2. Błąd kompilacji "package … does not exist" / "cannot find symbol": @MockBean i @SpyBean zostały usunięte w
     *      Boot 4 (klas nie ma na classpath — Test02PitfallsTest). Zastąp je {@code @MockitoBean}/{@code @MockitoSpyBean}
     *      z {@code org.springframework.test.context.bean.override.mockito}.
     *   3. Atrapa zwróci null, serwis zapisze produkt z ceną null i test nie zgłosi żadnego błędu. Sprawdzone w
     *      Test02ServiceWithMockClientTest (unstubbedMockReturnsNull).
     *   4. {@code @MockitoBean} po cichu utworzy NOWY bean-atrapę tego typu. Z {@code enforceOverride = true}: kontekst
     *      się nie podniesie ("Unable to override bean: there are no beans of type …"). {@code @MockitoSpyBean}: też
     *      błąd ("Unable to select a bean to wrap: there are no beans of type …"). Sprawdzone w Test02PitfallsTest.
     *   5. Wyrażenie {@code audit.record("x")} w nawiasie when(...) wykonuje PRAWDZIWĄ metodę szpiega, więc wpis "x"
     *      powstaje, zanim Mockito dostanie wartość do podmiany. Naprawa: {@code doReturn(100).when(audit).record("x")}.
     *      Sprawdzone w Test02SpyBeanTest (whenCallsRealMethodDoReturnDoesNot).
     *   6. Nie: domyślnie (MockReset.AFTER) po każdym teście atrapa traci ustawienia i zapisane wywołania — test 2
     *      zobaczy czystą atrapę. Z {@code reset = MockReset.NONE} atrapa niczego nie traci i test 2 widzi wywołanie
     *      z testu 1. Sprawdzone w Test02MockResetTest.
     *   7. Lambda to prosta, działająca podróbka: test sprawdza WYNIK (nowa cena, wpis w dzienniku), nie to, kto
     *      kogo zawołał, więc nie pęka przy refaktoryzacji i nie wymaga ani Mockito, ani Springa. Sprawdzone
     *      w Test02FakeVsMockTest.
     *   8. Każdy inny zestaw atrap to inny klucz pamięci podręcznej, więc Spring uruchamia do 10 osobnych kontekstów
     *      zamiast jednego — każdy z pełnym, wolnym startem. Rozwiązanie: wspólne atrapy w klasie bazowej albo
     *      w @TestConfiguration. Dowód mechanizmu: Test01ContextCacheTest (mockitoBeanSetIsPartOfTheKey).
     */
    // </editor-fold>
}

package s11_testing.test03_mock_mvc_tester;

/**
 * <pre>
 * TEMAT: MockMvcTester — testy kontrolerów z asercjami AssertJ zamiast {@code perform().andExpect()}
 *        (MockMvc = udawany serwer MVC, wołany bez sieci; tester = tu: pomocnik z asercjami; assertion = asercja,
 *         sprawdzenie; JSONPath = zapis ścieżki do pola w JSON, np. {@code $.name}; lenient = luźny; strict = ścisły)
 *
 * W SKRÓCIE:
 *   MockMvc wysyła żądanie prosto do DispatcherServlet — bez Tomcata i bez portu — i oddaje odpowiedź do sprawdzenia.
 *   Dotąd robiłeś to zapisem {@code mockMvc.perform(get(...)).andExpect(status().isOk())}. Od Spring Framework 6.2
 *   jest drugi zapis, MockMvcTester: {@code assertThat(mvc.get().uri(...)).hasStatusOk()}. To ten sam mechanizm
 *   pod spodem, ale asercje są w stylu AssertJ (jak reszta Twoich testów), IDE podpowiada metody, a błędy są
 *   czytelniejsze. Nieobsłużony wyjątek kontrolera nie leci z {@code perform}, tylko zostaje w wyniku
 *   ({@code hasFailed()}, {@code failure()}).
 *
 * ANALOGIA: kontrola paczki na poczcie.
 *   Klasyczny MockMvc to formularz z rubrykami do skreślenia: "status OK? [x]", "nagłówek? [x]". MockMvcTester to
 *   rozmowa z kontrolerem w zwykłym języku: "paczka ma być: przyjęta, z etykietą Location, a w środku książka
 *   o tytule…". Jedno i drugie sprawdza tę samą paczkę, ale drugi zapis czyta się jak zdanie.
 *
 * JAK TO DZIAŁA:
 *   1. {@code @WebMvcTest} (albo {@code @SpringBootTest} z {@code @AutoConfigureMockMvc}) tworzy MockMvc — i od razu
 *      MockMvcTester, który go owija ({@code @Autowired MockMvcTester mvc}).
 *   2. {@code mvc.get().uri("/x")} tylko BUDUJE żądanie (nic jeszcze nie leci). Dopiero {@code assertThat(...)} albo
 *      {@code exchange()} je wykonuje i daje wynik: MvcTestResult.
 *   3. Wynik jest "dostawcą asercji" ({@code AssertProvider}): {@code assertThat(wynik)} zwraca
 *      MvcTestResultAssert z metodami hasStatus…, hasHeader, bodyJson, hasFailed…
 *   4. {@code bodyJson()} przekłada treść na JSON i daje asercje JSON: ścieżki (JSONPath), porównanie z tekstem
 *      lub plikiem, zamianę na obiekt ({@code convertTo}) — do obiektów używa konwerterów aplikacji (Jackson 3).
 *   5. Gdy kontroler rzuci wyjątek, którego nikt nie obsłużył, wynik zapamiętuje go jako porażkę (failure).
 *
 * SŁÓWKA:
 *   request = żądanie; response = odpowiedź; header = nagłówek; body = treść (ciało) odpowiedzi; status = kod
 *   odpowiedzi; path = ścieżka; convert = zamienić; failure = porażka; standalone = samodzielny (bez kontekstu);
 *   resource = zasób (tu: plik z classpath); exchange = wykonać żądanie i odebrać odpowiedź.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest05CrudApi (pierwsze użycie MockMvcTester i klasyczny MockMvc),
 *             s05_validation/Val02ValidInControllers (walidacja @Valid, którą tu testujemy),
 *             s06_error_handling/Err04ProblemDetail (ProblemDetail — treść błędów, którą tu sprawdzamy),
 *             s11_testing/Test01TestSlices (@WebMvcTest, @AutoConfigureMockMvc),
 *             s11_testing/Test02MockitoBeans (atrapa serwisu w teście kontrolera),
 *             t32_junit_mockito/JUnit03AssertJ (asercje AssertJ).
 * </pre>
 */
public final class Test03MockMvcTester {

    private Test03MockMvcTester() {
    }

    // =================================================================================================
    // 1. KLASYCZNY MockMvc I MockMvcTester — TA SAMA ASERCJA DWOMA SPOSOBAMI (Test03ClassicVsTesterTest)
    // =================================================================================================
    //
    // KLASYCZNIE (MockMvc.perform + andExpect; metoda musi mieć {@code throws Exception}):
    //   mockMvc.perform(get("/test03/products/1"))
    //          .andExpect(status().isOk())
    //          .andExpect(jsonPath("$.name").value("Kawa"));
    //
    // MockMvcTester (bez {@code throws Exception}, w stylu AssertJ):
    //   assertThat(mvc.get().uri("/test03/products/1"))
    //          .hasStatusOk()
    //          .bodyJson().extractingPath("$.name").asString().isEqualTo("Kawa");
    //
    //   Co zyskujesz?
    //   • Asercje w jednym stylu z resztą testów ({@code assertThat}) i podpowiedzi IDE po kropce (w klasycznym
    //     zapisie trzeba pamiętać, które statyczne importy wybrać: status(), jsonPath(), header()…).
    //   • Metody czytelne jak zdania: hasStatusOk, hasStatus(HttpStatus.NOT_FOUND), hasHeader, hasContentType.
    //   • Nieobsłużony wyjątek kontrolera jest daną do sprawdzenia, a nie przerwaniem testu (sekcja 5).
    //   Co zostaje bez zmian? Ten sam silnik, te same wyniki. W teście bothReportFailures obie wersje zgłaszają
    //   nieudany status jako AssertionError z kodami oczekiwanym (404) i faktycznym (200).
    //
    // Skąd go wziąć?
    //   • {@code @WebMvcTest} — @Autowired MockMvcTester od razu (Test03ClassicVsTesterTest).
    //   • {@code @SpringBootTest} — dopiero z {@code @AutoConfigureMockMvc} (Test03SpringBootTesterTest); zapytanie
    //     przechodzi wtedy przez prawdziwy serwis i advice z całej aplikacji.
    //   • BEZ Springa: {@code MockMvcTester.of(new ProductController(new ProductService()))} — "samodzielny" MockMvc
    //     zbudowany z podanych kontrolerów (Test03StandaloneTest). Najtańszy test mapowań i JSON-a w milisekundach.
    //     Ale: standalone nie zna Twojego advice ani konfiguracji z aplikacji — brak produktu to tam nieobsłużony
    //     wyjątek, a nie 404 (chyba że advice podasz ręcznie: {@code MockMvcTester.of(List.of(kontroler),
    //     builder -> builder.setControllerAdvice(new ProductErrorAdvice()).build())}).
    //   • Z istniejącego MockMvc: {@code MockMvcTester.create(mockMvc)}; z kontekstu:
    //     {@code MockMvcTester.from(webApplicationContext)}.
    //
    // DOBRA PRAKTYKA: w nowym kodzie pisz MockMvcTester, a klasyczny MockMvc zostaw tam, gdzie już jest. Dlaczego?
    //   Oba działają i można je mieszać w jednym projekcie (w @WebMvcTest są oba beany), więc nie ma potrzeby
    //   przepisywać starych testów; nowe są krótsze i bardziej czytelne.

    // =================================================================================================
    // 2. KATALOG ASERCJI NA ODPOWIEDZI (Test03AssertionsTest)
    // =================================================================================================
    //
    //   Co sprawdzasz        | Zapis
    //   ---------------------+-------------------------------------------------------------------------------
    //   status               | hasStatusOk(); hasStatus(HttpStatus.NOT_FOUND); hasStatus(404); hasStatus4xxClientError()
    //   typ treści           | hasContentType(MediaType.APPLICATION_JSON); hasContentType("application/problem+json")
    //   nagłówki             | hasHeader("Location", "..."); containsHeader("X"); doesNotContainHeader("X")
    //   treść jako tekst     | bodyText().contains("Kawa"); bodyText().isEmpty(); hasBodyTextEqualTo("...")
    //   wartość z JSON       | bodyJson().extractingPath("$.name").asString().isEqualTo("Kawa")
    //   istnienie ścieżki    | bodyJson().hasPath("$.price"); doesNotHavePath("$.internalCode")
    //   ścieżka + warunek    | bodyJson().hasPathSatisfying("$.name", n -> n.assertThat().asString().startsWith("Ka"))
    //   porównanie z JSON    | bodyJson().isLenientlyEqualTo("{...}") albo isStrictlyEqualTo("{...}")
    //   porównanie z plikiem | bodyJson().isLenientlyEqualTo(new ClassPathResource("test03/products.json"))
    //
    // • JSONPath ({@code $.name}, {@code $[*].name}, {@code $.length()}) wskazuje pole w dokumencie; {@code extractingPath}
    //   zwraca wartość, na której dopiero wybierasz typ ({@code asString()}, {@code asList()}, …).
    // • Porównanie LUŹNE ({@code isLenientlyEqualTo}): oczekiwany JSON może być PODZBIOREM odpowiedzi — dodatkowe pola
    //   odpowiedzi nie przeszkadzają, a kolejność pól i (dla tablic) elementów nie ma znaczenia. Dobre domyślnie:
    //   test sprawdza to, na czym Ci zależy, a dodanie nowego pola w DTO go nie psuje.
    // • Porównanie ŚCISŁE ({@code isStrictlyEqualTo}): odpowiedź musi mieć dokładnie te pola i tablice w tej samej
    //   kolejności. Dodatkowe pole to błąd ("Unexpected: ..."). Dobre dla umowy, której nie wolno zmienić.
    // • Pliki JSON trzymaj w {@code src/test/resources} — dłuższe odpowiedzi czytają się lepiej niż tekst w Javie.
    //
    // PUŁAPKA: porównanie ścisłe wszędzie. Dlaczego? Każde nowe pole w DTO (np. {@code description}) czerwieni
    //   wszystkie testy, choć nic się nie zepsuło. Ścisłego używaj świadomie — dla kluczowych kontraktów.
    //
    // PUŁAPKA: sprawdzanie samego statusu 200 bez treści. Dlaczego? Kontroler może zwrócić 200 z pustym albo złym JSON-em
    //   i test zielony nic nie dowodzi. Sprawdź choć jedno pole z treści.
    //
    // PUŁAPKA: błąd to NIE zawsze {@code application/json}. Dlaczego? ProblemDetail ma typ
    //   {@code application/problem+json}; {@code hasContentType(MediaType.APPLICATION_JSON)} dla błędu by nie przeszło
    //   (test: headersAndContentType).

    // =================================================================================================
    // 3. POST Z JSON-EM, WALIDACJA I TREŚĆ BŁĘDU (Test03PostAndConvertTest, Test03NoAdviceTest)
    // =================================================================================================
    //
    //   assertThat(mvc.post().uri("/test03/products")
    //           .contentType(MediaType.APPLICATION_JSON)
    //           .content("{\"name\":\"Ciastka\",\"price\":12.50}"))
    //       .hasStatus(HttpStatus.CREATED)
    //       .hasHeader("Location", "http://localhost/test03/products/3");
    //
    // • Konstruktor żądania ma te same metody co klasyczny builder: contentType, content, accept, header, param,
    //   queryParam, principal. Adres z parametrami: {@code uri("/test03/products/{id}", 1)}.
    // • Poprawny POST → 201, nagłówek Location z adresem nowego zasobu i produkt w ciele.
    // • Zła nazwa i cena ({@code {"name":" ","price":-1}}) → 400 i ProblemDetail zbudowany przez advice: tytuł
    //   "Niepoprawne dane" i pole {@code errors} z komunikatem dla każdego błędnego pola
    //   ({@code $.errors.name}, {@code $.errors.price}). Treść błędu to też kontrakt API — warto ją sprawdzić.
    // • Niepoprawny JSON (urwany nawias) → 400 jeszcze PRZED kontrolerem (Spring nie umie go odczytać),
    //   a brak nagłówka Content-Type przy ciele → 415 Unsupported Media Type.
    //
    // PUŁAPKA: kontroler bez własnej obsługi błędów. Dlaczego? W teście Test03NoAdviceTest (advice wyłączone filtrem)
    //   błąd walidacji to 400 z PUSTĄ treścią — klient nie wie, które pole poprawić — a brak produktu to
    //   nieobsłużony wyjątek zamiast 404. Test to ujawnia dopiero, gdy sprawdzisz treść i status (nie tylko "nie 200").
    //   Rozwiązanie: wspólne advice z ProblemDetail (s06_error_handling/Err03ControllerAdvice).
    //
    // DOBRA PRAKTYKA: w jednym teście jeden scenariusz ("zła nazwa → 400 z błędem pola name"), z nazwą w
    //   {@code @DisplayName} opisującą zachowanie. Dlaczego? Gdy pęka test "wszystko o POST", nie wiesz od razu, co
    //   dokładnie przestało działać.

    // =================================================================================================
    // 4. convertTo — ODPOWIEDŹ JAKO OBIEKT (Test03PostAndConvertTest)
    // =================================================================================================
    //
    //   assertThat(mvc.get().uri("/test03/products/1")).bodyJson()
    //           .convertTo(ProductResponse.class)
    //           .isEqualTo(new ProductResponse(1, "Kawa", new BigDecimal("24.90")));
    //
    //   assertThat(mvc.get().uri("/test03/products")).bodyJson()
    //           .convertTo(InstanceOfAssertFactories.list(ProductResponse.class))
    //           .hasSize(2).extracting(ProductResponse::name).containsExactly("Kawa", "Herbata");
    //
    // • {@code convertTo(Klasa)} zamienia JSON z odpowiedzi na obiekt (tym samym Jacksonem 3, co aplikacja) i daje
    //   asercje AssertJ na obiekcie — wygodne, gdy sprawdzasz kilka pól albo równość całego rekordu.
    // • Dla LIST: {@code convertTo(InstanceOfAssertFactories.list(Klasa.class))} — dostajesz asercje na liście
    //   (hasSize, extracting, containsExactly…).
    // • {@code exchange()} wykonuje żądanie od razu i daje wynik, który możesz sprawdzać wiele razy
    //   (test exchangeOnce) — jedno wywołanie kontrolera zamiast kilku. Zwykłe {@code assertThat(mvc.get()…)}
    //   wykonuje żądanie przy każdym wywołaniu assertThat; dla GET to bez znaczenia, dla POST — każde wywołanie
    //   utworzyłoby produkt od nowa!
    //
    // PUŁAPKA: ponowne użycie wyrażenia {@code mvc.post()...} w kilku {@code assertThat}. Dlaczego? Każde wykonuje
    //   żądanie jeszcze raz — POST tworzy kolejne produkty (w teście validationError widać to w dwóch wywołaniach,
    //   które specjalnie nic nie zmieniają, bo walidacja odrzuca dane). Gdy żądanie ma skutki, użyj
    //   {@code exchange()} raz i sprawdzaj wynik.
    //
    // DOBRA PRAKTYKA: JSON z odpowiedzi porównuj z JSON-em (isLenientlyEqualTo), a convertTo zostaw na przypadki,
    //   gdy potrzebujesz asercji na obiekcie. Dlaczego? Porównanie z tekstem JSON sprawdza prawdziwy kształt na
    //   drucie (nazwy pól, formaty), a convertTo zakłada, że klasa DTO i JSON są zgodne.

    // =================================================================================================
    // 5. NIEOBSŁUŻONY WYJĄTEK: ServletException KONTRA hasFailed() (Test03UnhandledExceptionTest)
    // =================================================================================================
    //
    // Endpoint /test03/boom rzuca IllegalStateException, na który nie ma @ExceptionHandler.
    //
    //   KLASYCZNIE: perform(...) RZUCA jakarta.servlet.ServletException ("Request processing failed"),
    //               a nasz wyjątek jest jego przyczyną:
    //     assertThatThrownBy(() -> mockMvc.perform(get("/test03/boom")))
    //             .isInstanceOf(ServletException.class).hasRootCauseInstanceOf(IllegalStateException.class);
    //
    //   MockMvcTester: nic nie leci. Wynik zapamiętuje porażkę:
    //     assertThat(mvc.get().uri("/test03/boom")).hasFailed()
    //             .failure().isInstanceOf(ServletException.class)
    //             .hasRootCauseInstanceOf(IllegalStateException.class).hasRootCauseMessage("Awaria serwisu");
    //
    //   • {@code failure()} to TEN SAM ServletException co w klasycznym zapisie (opakowanie robi Spring MVC),
    //     więc przyczynę sprawdzasz przez {@code hasRootCause…}.
    //   • {@code doesNotHaveFailed()} potwierdza, że żądanie nie zakończyło się porażką.
    //   • Sprawdzanie statusu dla żądania, które zawiodło ({@code hasStatus(500)}), kończy się AssertionError z
    //     komunikatem o wyjątku — nie dostajesz po cichu kodu 500.
    //
    // DLACZEGO nie ma kodu 500? W MockMvc nie ma kontenera serwletów, więc nie ma też mechanizmu błędu Tomcata
    // ({@code /error}), który w prawdziwej aplikacji zamienia nieobsłużony wyjątek na odpowiedź 500. Test widzi
    // wyjątek zamiast kodu. Prawdziwy kod zobaczysz przy uruchomionej aplikacji (http/test03_mock_mvc_tester.http,
    // zapytanie 5: 500 z JSON-em {@code timestamp/status/error/path}) albo w @SpringBootTest(RANDOM_PORT).
    //
    // PUŁAPKA: oczekiwanie kodu 500 w teście MockMvc dla nieobsłużonego wyjątku. Dlaczego? Test rzuci
    //   ServletException (albo, w MockMvcTester, pokaże porażkę) i nie dojdzie do żadnego kodu. Aby w teście
    //   dostać 500/404/400 z treścią, napisz własne advice — wtedy wyjątek zamienia się w odpowiedź.
    //
    // DOKUMENTACJA API: do generowania dokumentacji REST z testów istnieje Spring REST Docs (wzmianka —
    //   w kursie jej nie używamy; dokumentuje wynik testów MockMvc).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • MockMvcTester ({@code org.springframework.test.web.servlet.assertj}) = MockMvc z asercjami AssertJ:
     *     {@code assertThat(mvc.get().uri("/x")).hasStatusOk().bodyJson()…}. W {@code @WebMvcTest} jest od razu;
     *     w {@code @SpringBootTest} z {@code @AutoConfigureMockMvc}; bez Springa: {@code MockMvcTester.of(kontroler)}.
     *   • Asercje: hasStatus*, hasHeader, hasContentType, bodyText, bodyJson (extractingPath, hasPath,
     *     isLenientlyEqualTo / isStrictlyEqualTo z tekstem lub plikiem), convertTo(Klasa / list(Klasa)).
     *   • Luźne porównanie JSON: podzbiór pól wystarczy; ścisłe: dokładnie te pola.
     *   • POST: contentType(APPLICATION_JSON).content("..."); błąd walidacji → 400 + treść z advice (ProblemDetail
     *     ma typ application/problem+json). Bez advice: 400 z pustą treścią.
     *   • Nieobsłużony wyjątek: klasyczny MockMvc rzuca ServletException; MockMvcTester → hasFailed()/failure().
     *     Kodu 500 w MockMvc nie ma (brak /error).
     *   • {@code exchange()} wykonuje żądanie raz i pozwala sprawdzać wynik wiele razy.
     *   • Klasyczny MockMvc nadal działa i można go mieszać z MockMvcTester.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się zapis {@code mockMvc.perform(get(...)).andExpect(...)} od
     *      {@code assertThat(mvc.get().uri(...))...} i czy któryś z nich wysyła prawdziwe zapytanie po sieci?
     *   2. Czym różni się {@code isLenientlyEqualTo} od {@code isStrictlyEqualTo}? Odpowiedź ma dodatkowe pole
     *      {@code id}, a oczekiwany JSON to {@code {"name":"Kawa"}} — który zapis przejdzie?
     *   3. Co się stanie? W teście {@code @WebMvcTest} z kontrolerem, który rzuca nieobsłużony wyjątek, wywołujesz
     *      {@code mockMvc.perform(get("/test03/boom"))}. A co, gdy zrobisz to przez MockMvcTester?
     *   4. Co się stanie? Wysyłasz {@code mvc.post().uri("/test03/products").content("{...}")} bez
     *      {@code contentType(...)}.
     *   5. ZNAJDŹ BŁĄD: {@code assertThat(mvc.get().uri("/test03/products/99")).hasContentType(MediaType.APPLICATION_JSON)}
     *      — kontroler zwraca ProblemDetail 404, a test jest czerwony. Dlaczego?
     *   6. ZNAJDŹ BŁĄD: w teście używasz {@code MockMvcTester.of(new ProductController(...))} i oczekujesz
     *      {@code hasStatus(404)} dla brakującego produktu, choć w aplikacji jest advice zamieniające wyjątek na 404.
     *      Test pokazuje porażkę żądania. Dlaczego i jak to naprawić?
     *   7. Jak sprawdzisz, że odpowiedź dla {@code GET /test03/products} ma dwa produkty o nazwach Kawa i Herbata
     *      w tej kolejności — dwoma sposobami (przez JSONPath i przez convertTo)?
     *   8. Po co istnieje {@code exchange()} i kiedy jest ważny przy POST?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — tym razem piszesz TESTY. Szkielet: Test03MockMvcTesterExercisesTest (@Tag("cwiczenie")),
    // rozwiązania: s11_testing.solutions.test03_mock_mvc_tester.Test03MockMvcTesterSolutionsTest (@Tag("wzorzec"))
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): GET /test03/products/2 → 200 i nazwa "Herbata".
    //   Podpowiedź: sekcja 1 — hasStatusOk() i bodyJson().extractingPath("$.name").
    //
    // ĆWICZENIE 2 (łatwe): GET /test03/products/99 → 404, typ application/problem+json i tytuł "Nie ma takiego produktu".
    //   Podpowiedź: sekcja 2 — hasStatus(HttpStatus.NOT_FOUND), hasContentType("application/problem+json").
    //
    // ĆWICZENIE 3 (średnie): POST poprawnego produktu → 201, Location "http://localhost/test03/products/3" i JSON
    //   zawierający nazwę. Podpowiedź: sekcja 3 — contentType + content; isLenientlyEqualTo.
    //
    // ĆWICZENIE 4 (średnie): GET /test03/boom → żądanie zakończone porażką; przyczyną jest IllegalStateException
    //   z komunikatem "Awaria serwisu". Podpowiedź: sekcja 5 — hasFailed(), failure(), hasRootCause….
    //
    // ĆWICZENIE 5 (trudniejsze): POST z pustą nazwą i ujemną ceną → 400, a w treści błędu są OBA pola: errors.name
    //   i errors.price. Podpowiedź: sekcja 3 — bodyJson().hasPath(...).hasPath(...).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Oba zapisy robią to samo — przekazują żądanie do DispatcherServlet w pamięci, bez sieci i bez serwera.
     *      Różnica to styl asercji: andExpect(status()…) (klasyczny, throws Exception) kontra AssertJ w
     *      MockMvcTester (podpowiedzi IDE, czytelniejsze błędy, wyjątek kontrolera jako dana do sprawdzenia).
     *      Sprawdzone w Test03ClassicVsTesterTest.
     *   2. Przejdzie {@code isLenientlyEqualTo} (oczekiwany JSON może być podzbiorem odpowiedzi). {@code isStrictlyEqualTo}
     *      rzuci AssertionError z informacją o nieoczekiwanym polu. Sprawdzone w Test03AssertionsTest.
     *   3. Klasyczny MockMvc: perform rzuci ServletException ("Request processing failed"), z IllegalStateException
     *      jako przyczyną. MockMvcTester nic nie rzuci — wynik pamięta porażkę: {@code hasFailed()},
     *      {@code failure()}. Sprawdzone w Test03UnhandledExceptionTest.
     *   4. Brak Content-Type przy ciele → 415 Unsupported Media Type (Spring nie wie, jak odczytać ciało).
     *      Sprawdzone w Test03PostAndConvertTest (missingContentType).
     *   5. ProblemDetail ma Content-Type {@code application/problem+json}, a nie {@code application/json}. Poprawnie:
     *      {@code hasContentType("application/problem+json")}. Sprawdzone w Test03AssertionsTest.
     *   6. Standalone nie zna Twojego advice ani konfiguracji — ProductNotFoundException jest tam nieobsłużony.
     *      Naprawa: użyj {@code @WebMvcTest} (advice się ładuje) albo wskaż advice przy budowaniu standalone:
     *      {@code MockMvcTester.of(List.of(kontroler), builder -> builder.setControllerAdvice(new ProductErrorAdvice()).build())}.
     *      Obie wersje sprawdzone w Test03StandaloneTest (standaloneHasNoAdvice, standaloneWithAdvice).
     *   7. JSONPath: {@code bodyJson().extractingPath("$[*].name").asList().containsExactly("Kawa", "Herbata")};
     *      convertTo: {@code bodyJson().convertTo(InstanceOfAssertFactories.list(ProductResponse.class)).extracting(ProductResponse::name)
     *      .containsExactly("Kawa", "Herbata")}. Sprawdzone w Test03AssertionsTest i Test03PostAndConvertTest.
     *   8. {@code exchange()} wykonuje żądanie od razu i daje wynik do wielokrotnego sprawdzania. Przy POST ważny, bo
     *      każde {@code assertThat(mvc.post()…)} wykonuje żądanie od nowa — dwa assertThat = dwa utworzone produkty.
     *      Sprawdzone w Test03PostAndConvertTest (exchangeOnce).
     */
    // </editor-fold>
}

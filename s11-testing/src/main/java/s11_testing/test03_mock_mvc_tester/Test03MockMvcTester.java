package s11_testing.test03_mock_mvc_tester;

/**
 * <pre>
 * TEMAT: Testy kontrolerów w stylu AssertJ — MockMvcTester (i RestTestClient)
 *        (tester = narzędzie do sprawdzania; assertion = asercja, sprawdzenie; fluent API = „płynne” API, w którym
 *         wywołania łączy się kropkami; JSON path = ścieżka JSON, np. $.name; lenient = luźny; strict = ścisły)
 *
 * W SKRÓCIE:
 *   MockMvc wysyła „udawane” zapytania HTTP do kontrolera bez prawdziwego serwera. Klasyczne API
 *   ({@code perform(...).andExpect(...)}) wymaga wielu statycznych importów i rzuca wyjątki kontrolowane. Od Spring
 *   Framework 6.2 jest MockMvcTester: to samo MockMvc, ale sprawdzenia piszesz jak zwykłe asercje AssertJ —
 *   {@code assertThat(mvc.get().uri("/api/products/SPO-001")).hasStatusOk().bodyJson()...}. W Boot 4 dostajesz go
 *   w {@code @WebMvcTest} automatycznie. Gdy potrzebujesz PRAWDZIWEGO HTTP (serwer na losowym porcie) — Spring
 *   Framework 7 daje {@code RestTestClient}.
 *
 * ANALOGIA: kontrola jakości w fabryce.
 *   Klasyczny MockMvc to formularz kontroli z rubrykami do skreślania — działa, ale trzeba znać numer każdej rubryki
 *   (statyczne importy {@code status()}, {@code jsonPath()}, {@code header()}). MockMvcTester to kontroler jakości,
 *   który mówi pełnymi zdaniami: „status OK, typ JSON, pole name równe «Kawa ziarnista 1kg»” — a gdy coś się nie
 *   zgadza, pokazuje różnicę. RestTestClient to klient, który naprawdę kupuje produkt w sklepie (prawdziwy serwer).
 *
 * JAK TO DZIAŁA:
 *   1. {@code @WebMvcTest} zawiera {@code @AutoConfigureMockMvc}; autokonfiguracja MockMvc w Boot 4 tworzy bean
 *      MockMvc i — gdy AssertJ jest na ścieżce klas (jest, przez spring-boot-starter-test) — bean MockMvcTester.
 *   2. {@code mvc.get().uri(...)} buduje żądanie; {@code assertThat(żądanie)} samo je wykonuje (exchange) i zwraca
 *      asercję wyniku (MvcTestResultAssert). Możesz też wykonać je raz: {@code var wynik = ....exchange()} i sprawdzać
 *      wiele razy.
 *   3. {@code bodyJson()} parsuje ciało jako JSON: ścieżki ({@code extractingPath}), porównanie z tekstem
 *      ({@code isLenientlyEqualTo} / {@code isStrictlyEqualTo}), zamiana na rekord ({@code convertTo}).
 *   4. Wyjątek nieobsłużony przez kontroler: klasyczne {@code perform()} go RZUCA, a MockMvcTester zapisuje go
 *      w wyniku — sprawdzasz {@code hasFailed().failure()}.
 *   5. {@code RestTestClient} + {@code @SpringBootTest(webEnvironment = RANDOM_PORT)} + {@code @AutoConfigureRestTestClient}
 *      (moduł spring-boot-resttestclient, przychodzi ze spring-boot-starter-webmvc-test) — prawdziwe HTTP.
 *
 * SŁÓWKA:
 *   tester = narzędzie testujące; exchange = wymiana (wysłanie żądania i odebranie odpowiedzi); perform = wykonać;
 *   expect = oczekiwać; body = ciało odpowiedzi; header = nagłówek; path = ścieżka; lenient = luźny;
 *   strict = ścisły; convert = zamienić; failure = porażka (wyjątek); location = położenie (nagłówek z adresem
 *   nowego zasobu); conflict = konflikt (409).
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest01Controllers (kontrolery i droga żądania), s04_web_rest/Rest03Responses
 *             (ResponseEntity, 201 + Location), s06_error_handling/Err01DefaultErrors (strona /error Boota),
 *             s11_testing/Test01TestSlices (wycinek @WebMvcTest), t32_junit_mockito/JUnit03AssertJ (AssertJ).
 * </pre>
 */
public final class Test03MockMvcTester {

    private Test03MockMvcTester() {
    }

    // =================================================================================================
    // 1. KLASYCZNY MockMvc: perform() + andExpect()
    // =================================================================================================
    //
    //   mockMvc.perform(get("/api/products/SPO-001"))             // MockMvcRequestBuilders.get — import statyczny
    //          .andExpect(status().isOk())                         // MockMvcResultMatchers.status — import statyczny
    //          .andExpect(jsonPath("$.name").value("Kawa ziarnista 1kg"))
    //          .andExpect(jsonPath("$.price").value(64.99));
    //
    // Tak wygląda 90% testów w tutorialach i w istniejących projektach — musisz go umieć czytać. Wady: kilka
    // statycznych importów z różnych klas, metoda testowa musi deklarować throws Exception, a komunikaty błędów
    // bywają ubogie. Test sekcji 1: Test03MockMvcTesterTest.classicMockMvc.

    // =================================================================================================
    // 2. MockMvcTester — TO SAMO W STYLU AssertJ
    // =================================================================================================
    //
    //   @WebMvcTest(ProductController.class)
    //   class ... {
    //       @Autowired MockMvcTester mvc;                         // Boot 4: bean tworzony automatycznie
    //
    //       assertThat(mvc.get().uri("/api/products/{sku}", "SPO-001"))
    //               .hasStatusOk()
    //               .hasContentType(MediaType.APPLICATION_JSON)
    //               .bodyJson().extractingPath("$.name").isEqualTo("Kawa ziarnista 1kg");
    //   }
    //
    // Jeden import (assertThat z AssertJ), podpowiedzi IDE po kropce, bez throws Exception. Pakiet:
    // org.springframework.test.web.servlet.assertj (Spring Framework 6.2+). Poza wycinkiem utworzysz go sam:
    // MockMvcTester.from(webApplicationContext) albo MockMvcTester.create(mockMvc). Test sekcji 2.
    //
    // DOBRA PRAKTYKA: w nowych testach używaj MockMvcTester. Dlaczego? Czytelniejsze asercje, lepsze komunikaty
    //   przy porażce i jednolity styl z resztą testów (AssertJ). Stare testy z perform() działają dalej — nie musisz
    //   ich przepisywać naraz.

    // =================================================================================================
    // 3. bodyJson(): ŚCIEŻKI, PORÓWNANIE LUŹNE I ŚCISŁE, ZAMIANA NA REKORD
    // =================================================================================================
    //
    //   var wynik = mvc.get().uri("/api/products").param("category", "SPOZYWCZE").exchange();
    //   assertThat(wynik).bodyJson().extractingPath("$[*].sku").asArray().containsExactly("SPO-001", "SPO-002");
    //   assertThat(wynik).bodyJson().isLenientlyEqualTo("[ {\"sku\": \"SPO-001\"}, {\"sku\": \"SPO-002\"} ]");
    //   assertThat(wynik).bodyJson().extractingPath("$[0]").convertTo(ProductResponse.class).isEqualTo(KAWA);
    //
    // isLenientlyEqualTo (luźno): w odpowiedzi mogą być pola, których nie wymieniłeś. isStrictlyEqualTo (ściśle):
    // odpowiedź musi mieć DOKŁADNIE te pola — ten sam niepełny JSON już nie przechodzi (sprawdzone testem sekcji 3).
    // convertTo zamienia fragment JSON-a na rekord przez Jacksona aplikacji — porównujesz obiekty, nie teksty.
    //
    // PUŁAPKA: porównanie ścisłe tam, gdzie chodzi o jedno pole. Dlaczego? Każde nowe pole w odpowiedzi (np. ktoś
    //   doda "stock") psuje test, choć nic się nie zepsuło. Ściśle sprawdzaj tylko kontrakt, który ma być zamknięty;
    //   w pozostałych testach — luźno albo konkretną ścieżkę.

    // =================================================================================================
    // 4. POST, NAGŁÓWKI, KODY STATUSU
    // =================================================================================================
    //
    //   assertThat(mvc.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON).content(json))
    //           .hasStatus(HttpStatus.CREATED)
    //           .hasHeader("Location", "/api/products/DOM-001");
    //   verify(service).create(oczekiweŻądanie);    // atrapa serwisu: czy kontroler przekazał poprawny rekord?
    //
    // Test sprawdza całą drogę: JSON → rekord NewProductRequest (Jackson) → metoda kontrolera → ResponseEntity →
    // status, nagłówek i ciało. Duplikat: serwis rzuca DuplicateSkuException → @ExceptionHandler → 409 Conflict.
    // DELETE → 204 No Content. Testy sekcji 4.

    // =================================================================================================
    // 5. BŁĘDY: 404 I NIEOBSŁUŻONY WYJĄTEK
    // =================================================================================================
    //
    // ResponseStatusException(NOT_FOUND, "Nie ma produktu XXX-999") w MockMvc daje status 404 i KOMUNIKAT BŁĘDU
    // (hasErrorMessage), ale PUSTE ciało — przekierowanie do strony /error Boota robi kontener serwletów (Tomcat),
    // którego w MockMvc nie ma. Na prawdziwym serwerze to samo zapytanie ma ciało JSON (sekcja 6).
    //
    // Nieobsłużony wyjątek (GET /api/products/SPO-001/reviews rzuca IllegalStateException):
    //   • mockMvc.perform(...) RZUCA jakarta.servlet.ServletException: „Request processing failed:
    //     java.lang.IllegalStateException: Opinie o produktach nie są jeszcze gotowe”;
    //   • MockMvcTester NIE rzuca: assertThat(...).hasFailed().failure() — porażka to ten sam ServletException,
    //     a jego najgłębsza przyczyna (root cause) — IllegalStateException.
    // Na prawdziwym serwerze klient dostałby 500. Sprawdzone testami sekcji 5.
    //
    // PUŁAPKA: test 404 przez MockMvc „udowadnia”, że klient dostaje JSON z błędem. Dlaczego to złudzenie? MockMvc nie
    //   przechodzi przez /error — puste ciało w teście nie znaczy, że klient dostanie puste ciało (i odwrotnie).
    //   Treść odpowiedzi błędu z serwera sprawdzaj przez RANDOM_PORT; własne odpowiedzi błędów (ProblemDetail,
    //   @RestControllerAdvice — dział s06_error_handling) MockMvc widzi normalnie.

    // =================================================================================================
    // 6. RestTestClient — PRAWDZIWE HTTP (Spring Framework 7)
    // =================================================================================================
    //
    //   @SpringBootTest(webEnvironment = RANDOM_PORT)
    //   @AutoConfigureRestTestClient                 // org.springframework.boot.resttestclient.autoconfigure
    //   class ... {
    //       @Autowired RestTestClient client;        // org.springframework.test.web.servlet.client
    //       client.get().uri("/api/products/{sku}", "KSI-001").exchange()
    //             .expectStatus().isOk()
    //             .expectBody(ProductResponse.class).isEqualTo(czystyKod);
    //   }
    //
    // Boot kieruje klienta na port uruchomionego serwera (przy RANDOM_PORT). Ten sam klient umie też działać bez
    // serwera — gdy w kontekście jest MockMvc, Boot wiąże go z nim (RestTestClient.bindTo(mockMvc)). Test sekcji 6
    // pokazuje, że prawdziwy serwer na 404 odsyła JSON ze strony /error: status 404, error "Not Found", path.
    // Starszy odpowiednik z tutoriali: TestRestTemplate (w Boot 4 w module spring-boot-resttestclient, wymaga
    // @AutoConfigureTestRestTemplate).
    //
    // PUŁAPKA: testy z prawdziwym serwerem, które ZMIENIAJĄ dane (POST, DELETE). Dlaczego? Kontekst (i dane w nim)
    //   jest współdzielony przez wiele testów, a zapytanie HTTP idzie w osobnym wątku serwera — transakcja testu go
    //   nie obejmuje i nic się samo nie wycofa. Takie testy muszą same sprzątać albo używać unikalnych danych.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @WebMvcTest → @Autowired MockMvcTester mvc (Boot 4 tworzy bean, gdy AssertJ jest na ścieżce klas).
     *   • assertThat(mvc.get().uri("/x/{id}", id)).hasStatusOk() / hasStatus(HttpStatus.CREATED) / hasHeader(...)
     *     / hasContentType(...) / hasErrorMessage(...) / hasBodyTextEqualTo(...).
     *   • var wynik = mvc.post().uri(...).contentType(APPLICATION_JSON).content(json).exchange();
     *   • bodyJson().extractingPath("$.pole") / isLenientlyEqualTo(json) / isStrictlyEqualTo(json)
     *     / convertTo(Rekord.class).
     *   • Nieobsłużony wyjątek: perform() rzuca ServletException; MockMvcTester → hasFailed().failure().
     *   • MockMvc nie renderuje /error — treść błędów serwera sprawdzaj przez RANDOM_PORT.
     *   • RestTestClient: @SpringBootTest(RANDOM_PORT) + @AutoConfigureRestTestClient; exchange().expectStatus()...
     *
     * PYTANIA KONTROLNE:
     *   1. Skąd w teście @WebMvcTest bierze się bean MockMvcTester i od czego zależy jego utworzenie?
     *   2. Czym różni się isLenientlyEqualTo od isStrictlyEqualTo? Kiedy który?
     *   3. Co się stanie? Kontroler rzuca IllegalStateException, którego nikt nie obsługuje, a test woła
     *      mockMvc.perform(get(...)).andExpect(status().isInternalServerError()).
     *   4. Co się stanie? W MockMvcTester sprawdzasz 404 z ResponseStatusException i oczekujesz w ciele pola "status".
     *   5. ZNAJDŹ BŁĄD: test z RANDOM_PORT i RestTestClient usuwa produkt DELETE-em, a inny test tej samej konfiguracji
     *      zakłada, że produkt istnieje. Raz przechodzą, raz nie.
     *   6. Po co convertTo(ProductResponse.class), skoro można sprawdzić każde pole ścieżką?
     *   7. Kiedy wybierzesz RestTestClient z RANDOM_PORT zamiast MockMvcTester?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — uzupełniasz TESTY w pliku Test03MockMvcTesterExercisesTest (src/test/java, ten sam pakiet).
    // Atrapa serwisu jest już nauczona; Ty piszesz zapytanie MockMvcTester (albo oczekiwany JSON). Asercje gotowe.
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): wyślij GET /api/products/KSI-001 i zwróć wynik (MvcTestResult).
    //   Podpowiedź: mvc.get().uri(...).exchange().
    //
    // ĆWICZENIE 2 (średnie): wyślij GET /api/products z parametrem category=KSIAZKI.
    //   Podpowiedź: .param("category", "KSIAZKI") albo "?category=KSIAZKI" w adresie.
    //
    // ĆWICZENIE 3 (średnie): wyślij POST /api/products z ciałem JSON nowego produktu ODZ-001 „Kurtka zimowa”,
    //   kategoria ODZIEZ, cena 459.00. Podpowiedź: .contentType(MediaType.APPLICATION_JSON).content("""...""").
    //
    // ĆWICZENIE 4 (trudniejsze): wpisz PEŁNY JSON produktu KSI-002, tak żeby przeszło porównanie ŚCISŁE
    //   (isStrictlyEqualTo). Podpowiedź: nazwy pól = składowe rekordu ProductResponse; cena jako liczba.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @WebMvcTest zawiera @AutoConfigureMockMvc, a autokonfiguracja MockMvc (Boot 4, moduł spring-boot-webmvc-test)
     *      importuje konfigurację MockMvcTester z warunkiem: klasa org.assertj.core.api.Assert na ścieżce klas
     *      (AssertJ przychodzi ze spring-boot-starter-test). Sprawdzone testem sekcji 2.
     *   2. Luźne porównanie pozwala, by w odpowiedzi były dodatkowe pola (i dowolną kolejność elementów tablic);
     *      ścisłe wymaga dokładnie tych pól (i kolejności). Luźne — gdy sprawdzasz wybrane pola; ścisłe — gdy cały
     *      kształt odpowiedzi jest kontraktem. Sprawdzone testami sekcji 3.
     *   3. perform() nie dojdzie do andExpect — rzuci jakarta.servlet.ServletException „Request processing failed:
     *      java.lang.IllegalStateException: …”, a test skończy się błędem. W MockMvc nie ma statusu 500 dla
     *      nieobsłużonego wyjątku — to robi dopiero prawdziwy serwer. Sprawdzone testem sekcji 5.
     *   4. Asercja nie przejdzie: w MockMvc ciało jest puste (hasBodyTextEqualTo("") przechodzi), bo JSON błędu tworzy
     *      strona /error, do której przekierowuje dopiero kontener serwletów. Jest tylko status 404 i komunikat błędu
     *      (hasErrorMessage). Sprawdzone testem sekcji 5.
     *   5. Testy zależą od kolejności: kontekst (i dane w pamięci serwisu) jest współdzielony, a zmiana przez HTTP nie
     *      jest wycofywana żadną transakcją testu. Każdy test powinien tworzyć własne dane (unikalne SKU) albo sprzątać
     *      po sobie; w @WebMvcTest problem znika, bo dane daje atrapa.
     *   6. convertTo porównuje cały obiekt jednym isEqualTo — mniej kodu, a typy (np. BigDecimal) są takie jak
     *      w aplikacji, bo zamianę robi ten sam Jackson. Ścieżki są lepsze, gdy sprawdzasz jedno-dwa pola.
     *   7. Gdy liczy się to, co naprawdę widzi klient HTTP: strona /error, filtry i nagłówki kontenera serwletów,
     *      prawdziwa sieć. Do zwykłych testów kontrolera wystarcza MockMvcTester (szybszy, bez serwera).
     */
    // </editor-fold>
}

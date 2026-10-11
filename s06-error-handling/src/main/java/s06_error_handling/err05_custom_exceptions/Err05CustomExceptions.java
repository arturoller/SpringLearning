package s06_error_handling.err05_custom_exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Własne wyjątki domenowe i obsługa błędów w całym API — hierarchia, mapowanie na HTTP, walidacja, logowanie
 *        (custom exception = własny wyjątek; domain = dziedzina; hierarchy = hierarchia; validation = walidacja;
 *         logging = logowanie, zapis do dziennika; internals = szczegóły wewnętrzne)
 *
 * W SKRÓCIE:
 *   Dobra obsługa błędów to połączenie czterech rzeczy: (1) hierarchia własnych wyjątków (ShopException → ProductNotFound,
 *   OutOfStock, PaymentGateway), która opisuje problemy BIZNESOWE; (2) jedno advice, które zamienia je na kody HTTP
 *   i ProblemDetail; (3) walidacja wejścia z czytelnymi błędami pól; (4) zasady: loguj błąd serwera RAZ, a klientowi
 *   nie pokazuj szczegółów wewnętrznych. Serwis rzuca wyjątki domenowe i NIE zna HTTP.
 *
 * ANALOGIA: pogotowie ratunkowe i dyspozytor.
 *   Ratownicy (serwis) zgłaszają fachowo: „zatrzymanie krążenia”, „złamanie” (wyjątki domenowe). Nie dzwonią do
 *   rodziny. Dyspozytor (advice) wie, komu i jak o tym powiedzieć: rodzinie — spokojny komunikat; szpitalowi — pełna
 *   dokumentacja (log). Nikt nie zapisuje tej samej karty pacjenta pięć razy (log raz) i nikt nie czyta rodzinie
 *   wewnętrznych numerów kart (brak szczegółów wewnętrznych).
 *
 * JAK TO DZIAŁA:
 *   POST /api/orders {"customer":"Ala","productId":2,"quantity":3}
 *     → @Valid sprawdza OrderRequest (błąd → MethodArgumentNotValidException → 400 z mapą pól)
 *     → OrderController → OrderService.place(...) → OutOfStockException(2, 3, 0)          ← wyjątek domenowy, bez HTTP
 *     → ShopExceptionAdvice.shopException: ErrorCode.OUT_OF_STOCK → 409 (switch po enum), ProblemDetail + pola
 *     → klient: 409 application/problem+json {"title":"Brak towaru","code":"OUT_OF_STOCK","requested":3,"available":0,...}
 *   Błąd serwera (5xx): ten sam advice zapisuje w logu JEDEN wpis ERROR z całym łańcuchem przyczyn (stack trace),
 *   klient dostaje stały, bezpieczny komunikat.
 *
 * SŁÓWKA:
 *   custom exception = własny wyjątek; hierarchy = hierarchia; error code = kod błędu; wrap = opakować (przyczynę);
 *   cause = przyczyna; validation = walidacja; constraint = ograniczenie (np. @NotBlank); field error = błąd pola;
 *   log and throw = zaloguj i rzuć dalej (antywzorzec); logger = obiekt zapisujący logi; appender = odbiorca logów;
 *   exhaustive = wyczerpujący (obejmujący wszystkie przypadki); leak = wyciek.
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions05CustomExceptions (jak pisać własne wyjątki w Javie),
 *             t10_exceptions/Exceptions07BestPractices (dobre praktyki wyjątków), t10_exceptions/Exceptions06ChainingWrapping
 *             (łańcuch przyczyn), s06_error_handling/Err03ControllerAdvice (advice), s06_error_handling/Err04ProblemDetail
 *             (ProblemDetail), s00_start/Start04TestingIntro (@WebMvcTest, @MockitoBean).
 * </pre>
 */
public final class Err05CustomExceptions {

    private Err05CustomExceptions() {
    }

    // =================================================================================================
    // 1. HIERARCHIA WYJĄTKÓW DOMENOWYCH (Err05CustomExceptionsHierarchyTest)
    // =================================================================================================
    //
    //   ShopException (abstract, RuntimeException, ErrorCode)
    //     ├── ProductNotFoundException(productId)
    //     ├── OutOfStockException(productId, requested, available)
    //     └── PaymentGatewayException(cause)
    // Po co wspólny korzeń? Jedno {@code catch (ShopException e)} albo jeden @ExceptionHandler(ShopException.class) obsłuży
    // całą rodzinę, a nowy wyjątek domenowy nie wymaga nowej metody w advice — wystarczy nowy ErrorCode. Wyjątki są
    // niesprawdzane (RuntimeException): dzięki temu nie zaśmiecają sygnatur metod serwisu klauzulą throws (t10_exceptions/Exceptions02CheckedUnchecked).
    // Wyjątki niosą DANE jako pola (requested, available), a nie tylko tekst: advice dołącza je do JSON-a bez parsowania napisów.
    //
    // DOBRA PRAKTYKA: wyjątek domenowy opisuje problem BIZNESOWY ("za mało towaru"), a nie techniczny ("HTTP 409").
    //   Dlaczego? Ten sam serwis ma działać z REST-a, z kolejki komunikatów i z zadania wsadowego; kod HTTP ma sens
    //   tylko w warstwie web. Mapowanie ErrorCode → HttpStatus jest w ShopExceptionAdvice w switch po enum, który jest
    //   wyczerpujący: dodanie nowego kodu bez obsługi w switch nie skompiluje się.

    // =================================================================================================
    // 2. CO WOLNO ODESŁAĆ KLIENTOWI — komunikat vs przyczyna (Err05CustomExceptionsHierarchyTest)
    // =================================================================================================
    //
    // Umowa w tej hierarchii: {@code getMessage()} każdego ShopException jest napisany dla klienta (po polsku, bez
    // szczegółów technicznych), a szczegóły siedzą w {@code cause} (przyczynie) i trafiają tylko do logu.
    // PaymentGatewayException opakowuje "Connection refused: pay.internal:8443": klient dostaje "Płatność chwilowo
    // niedostępna...", log — pełny łańcuch. Test sprawdza, że komunikat nie zawiera "pay.internal", a cause go ma.
    //
    // PUŁAPKA: odsyłanie e.getMessage() dla WSZYSTKIEGO (także dla obcych wyjątków: NullPointerException, SQLException).
    //   Dlaczego? Komunikaty cudzych wyjątków nie są pisane dla klienta i zdradzają adresy, SQL, nazwy tabel. Dlatego
    //   advice odsyła komunikat tylko dla ShopException (umowa wyżej), a dla wszystkiego innego — stały tekst.

    // =================================================================================================
    // 3. JEDNO ADVICE: wyjątek → ProblemDetail (Err05CustomExceptionsWebTest, Err05CustomExceptionsEndToEndTest)
    // =================================================================================================
    //
    // ShopExceptionAdvice (dziedziczy po ResponseEntityExceptionHandler, Err04ProblemDetail):
    //   ProductNotFoundException  → 404, title "Produkt nie istnieje", code PRODUCT_NOT_FOUND
    //   OutOfStockException       → 409 + pola requested, available
    //   PaymentGatewayException   → 502 Bad Gateway ("zła brama": nasz serwer nie dogadał się z systemem zewnętrznym)
    //   każdy inny Exception      → 500, stały komunikat
    // Kontroler jest cienki: nie ma żadnego try/catch ani @ExceptionHandler. Serwis jest testowalny bez HTTP
    // (Err05OrderServiceTest). Nowy wyjątek domenowy = nowa klasa + nowy ErrorCode + jedna linijka w switch.
    //
    // PUŁAPKA: łapanie wyjątku w kontrolerze tylko po to, by zwrócić ResponseEntity z kodem (try/catch w każdej metodzie).
    //   Dlaczego? Powtarzasz to samo w każdym kontrolerze, łatwo o niespójność (jeden zwróci 400, drugi 404 dla tego
    //   samego wyjątku), a logika biznesowa tonie w obsłudze błędów.

    // =================================================================================================
    // 4. WALIDACJA JAKO ProblemDetail (Err05CustomExceptionsWebTest)
    // =================================================================================================
    //
    // {@code @Valid @RequestBody OrderRequest} uruchamia Bean Validation; naruszenie → MethodArgumentNotValidException.
    // Advice nadpisuje handleMethodArgumentNotValid z klasy bazowej i buduje ProblemDetail 400 z polem "errors":
    //   {"customer": ["Podaj nazwę klienta"], "productId": [...], "quantity": [...]}   ← pole → lista komunikatów
    // Mapa jest posortowana (TreeMap), więc odpowiedź i testy są deterministyczne. Komunikaty ustawiamy po polsku
    // w adnotacjach (message = "..."). Uszkodzony JSON obsługuje klasa bazowa (400 problem+json), a 405 zostaje 405 —
    // catch-all Exception w TEJ SAMEJ klasie nie przesłania handlerów odziedziczonych (wygrywa najbliższy typ), w
    // odróżnieniu od catch-all w osobnym advice (Err03ControllerAdvice, test catchAllSwallowsMvcErrors).
    //
    // Uwaga (Spring 6.1+): jeśli ograniczenia stoją bezpośrednio na parametrach metody (np. {@code @RequestParam @Min(1) int page}),
    // zamiast MethodArgumentNotValidException leci HandlerMethodValidationException — ResponseEntityExceptionHandler ma dla
    // niej osobną metodę handleHandlerMethodValidationException (sprawdzone w Spring Framework 7.0.9; tej lekcji tego nie testujemy).
    //
    // DOBRA PRAKTYKA: błędy walidacji zwracaj WSZYSTKIE naraz (mapa pól), nie tylko pierwszy. Dlaczego? Użytkownik
    //   formularza poprawia wszystko jednym podejściem, zamiast odkrywać błędy po jednym przy kolejnych wysyłkach.

    // =================================================================================================
    // 5. LOGOWANIE: RAZ, NA WŁAŚCIWYM POZIOMIE (Err05CustomExceptionsWebTest)
    // =================================================================================================
    //
    //   5xx (nasza wina)      → log.error("...", e)   w advice, RAZ, z wyjątkiem (stack trace + cause)
    //   4xx (wina klienta)    → bez ERROR (opcjonalnie debug) — to nie awaria, a zalew logów utrudnia znalezienie prawdziwych
    // Test przechwytuje logi (ListAppender z Logback) i sprawdza: jeden wpis ERROR dla 502 i dla 500, zero wpisów dla 404/400.
    //
    // PUŁAPKA: "log and throw" — zalogować wyjątek w serwisie i rzucić go dalej, a potem zalogować jeszcze raz w advice.
    //   Dlaczego? Jeden błąd = trzy wpisy w logu z tym samym stack trace; wygląda jak trzy awarie, rośnie koszt logów
    //   i łatwo przeoczyć prawdziwy problem. Wyjątek loguje ten, kto go OBSŁUGUJE (advice), a nie ten, kto go przekazuje.
    //
    // PUŁAPKA: {@code log.error("Błąd: " + e.getMessage())} bez przekazania wyjątku. Dlaczego? Gubisz stack trace i
    //   łańcuch przyczyn — w logu nie ma, GDZIE błąd powstał. Wyjątek zawsze jako ostatni argument: log.error("opis", e).
    //
    // PYTANIE REKRUTACYJNE: „Gdzie powinno się obsługiwać wyjątki w aplikacji warstwowej?” — Rzucaj wyjątki domenowe
    //   w serwisach, łap i zamieniaj na odpowiedź w jednym miejscu na brzegu systemu (advice/filtr), tam też loguj raz.
    //   Nie łap wyjątku, jeśli nie umiesz sensownie zareagować; nie połykaj ich (pusty catch).

    // =================================================================================================
    // 6. SIATKA BEZPIECZEŃSTWA (Err05CustomExceptionsWebTest)
    // =================================================================================================
    //
    // Nieprzewidziany wyjątek (tu: IllegalStateException z "ORDERS_V2" w komunikacie) → 500, detail "Wystąpił
    // nieoczekiwany błąd", ten sam format problem+json. Szczegóły są w logu (jeden wpis). Dzięki temu KAŻDA odpowiedź
    // błędu w aplikacji ma ten sam kształt, a klient nie musi obsługiwać dwóch formatów (nasz i domyślny z /error).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Hierarchia: abstrakcyjny korzeń (ShopException) + konkretne wyjątki z danymi w polach i kodem błędu (enum).
     *   • Wyjątki domenowe opisują problem biznesowy; zamiana na kod HTTP — w advice (switch po enum jest wyczerpujący).
     *   • Komunikat wyjątku domenowego = dla klienta; szczegóły techniczne → cause → tylko log.
     *   • Jedno @RestControllerAdvice (extends ResponseEntityExceptionHandler): ShopException, walidacja, catch-all.
     *   • Walidacja: nadpisz handleMethodArgumentNotValid; zwróć mapę pole → komunikaty (posortowaną).
     *   • Loguj 5xx raz, z wyjątkiem (log.error("opis", e)); 4xx nie jako ERROR; zakaz "log and throw".
     *   • Klientowi nigdy: stack trace, SQL, nazwy tabel, adresy wewnętrzne.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego wyjątki domenowe nie powinny znać kodów HTTP? Gdzie robimy zamianę?
     *   2. Co się stanie? Dodasz do enum ErrorCode nową stałą, a w switch w ShopExceptionAdvice jej nie obsłużysz.
     *   3. Co się stanie? Serwis rzuca wyjątek z komunikatem "ORDERS_V2" (zwykły IllegalStateException) — co zobaczy klient,
     *      a co trafi do logu?
     *   4. ZNAJDŹ BŁĄD: w serwisie {@code catch (ShopException e) { log.error("Błąd", e); throw e; }}, a advice też loguje.
     *      Co jest nie tak?
     *   5. ZNAJDŹ BŁĄD: {@code log.error("Błąd płatności: " + e.getMessage());} — czego brakuje i jaki to ma skutek?
     *   6. Co się stanie? Klient wyśle POST /api/orders z pustym customer, productId = -1 i quantity = 0. Ile błędów
     *      zobaczy w odpowiedzi i w jakiej formie?
     *   7. Dlaczego 404 i 400 nie logujemy jako ERROR, a 502 tak?
     *   8. Co się stanie? Klient wyśle GET na adres, który przyjmuje tylko POST, a advice ma catch-all Exception.
     *      Jaki kod dostanie — 405 czy 500?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Err05CustomExceptionsExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zamień kod błędu domenowego na status HTTP: PRODUCT_NOT_FOUND → 404, OUT_OF_STOCK → 409,
     * PAYMENT_UNAVAILABLE → 502. Podpowiedź: {@code switch} z wyrażeniem po enum (bez {@code default} — kompilator
     * dopilnuje kompletności).
     */
    static HttpStatus exercise1(ErrorCode code) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Pojedyncze naruszenie walidacji: pole i komunikat (do ćwiczenia 2). */
    record FieldMessage(String field, String message) {
    }

    /**
     * ĆWICZENIE 2 (średnie): zgrupuj naruszenia walidacji w mapę "pole → lista komunikatów". Klucze posortowane
     * alfabetycznie, komunikaty każdego pola też alfabetycznie. Pusta lista wejściowa → pusta mapa.
     * Podpowiedź: {@code TreeMap}, {@code computeIfAbsent(pole, p -> new ArrayList<>()).add(...)}, potem posortuj listy.
     */
    static Map<String, List<String>> exercise2(List<FieldMessage> violations) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): to samo co shopException w advice, ale jako zwykła metoda — zbuduj ProblemDetail dla
     * wyjątku sklepu: status z ćwiczenia 1 (skopiuj logikę), detail = komunikat wyjątku, własne pole "code" = nazwa
     * ErrorCode, a dla OutOfStockException dodatkowo pola "requested" i "available". Tytuł nie jest sprawdzany.
     * Podpowiedź: {@code ProblemDetail.forStatusAndDetail(...)}, {@code e instanceof OutOfStockException stock},
     * {@code problem.setProperty(...)}.
     */
    static ProblemDetail exercise3(ShopException e) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo serwis ma działać niezależnie od kanału (REST, kolejka, zadanie wsadowe); kod HTTP ma sens tylko w warstwie
     *      web. Zamianę robimy w jednym miejscu na brzegu: ShopExceptionAdvice (switch po ErrorCode).
     *   2. Kod się nie skompiluje: switch jako wyrażenie po enum musi być wyczerpujący (bez default), więc kompilator
     *      wskaże brakującą stałą. Dzięki temu nikt nie zapomni zmapować nowego kodu. Sprawdzone kompilacją: błąd "the switch expression does not cover all possible input values".
     *   3. Klient: 500 i stały tekst "Wystąpił nieoczekiwany błąd" (bez ORDERS_V2). Log: jeden wpis ERROR z wyjątkiem
     *      i jego komunikatem. Sprawdzone testem unexpectedExceptionIsGeneric.
     *   4. "Log and throw": ten sam błąd zapisuje się dwa razy (serwis i advice) z tym samym stack trace — wygląda jak dwie
     *      awarie. Loguje ten, kto wyjątek obsługuje (advice); serwis ma rzucić wyjątek i nic więcej.
     *   5. Brakuje wyjątku jako ostatniego argumentu: w logu nie ma stack trace ani łańcucha przyczyn, więc nie wiadomo,
     *      gdzie błąd powstał. Poprawnie: {@code log.error("Błąd płatności", e);}. W teście serverErrorLoggedOnceAndHidden
     *      sprawdzamy, że wpis ma throwableProxy z przyczyną.
     *   6. Trzy błędy naraz, w mapie "errors": customer → ["Podaj nazwę klienta"], productId → ["Id produktu musi być
     *      dodatnie"], quantity → ["Minimalna ilość to 1"], status 400, application/problem+json. Sprawdzone testem
     *      validationErrors.
     *   7. 404 i 400 to błędy klienta — serwer działa poprawnie, więc ERROR wywołałby fałszywy alarm i zalew logów.
     *      502 oznacza kłopot po naszej stronie (system zewnętrzny nie działa), który ktoś musi zbadać. Sprawdzone
     *      testami serverErrorLoggedOnceAndHidden i clientErrorsAreNotLoggedAsErrors.
     *   8. 405. Wygrywa najbliższy typ wyjątku w obrębie jednego advice: HttpRequestMethodNotSupportedException ma
     *      handler odziedziczony z ResponseEntityExceptionHandler, więc nie trafia do Exception.class. Sprawdzone
     *      testem springMvcErrorsKeepTheirCodes.
     */
    // </editor-fold>
}

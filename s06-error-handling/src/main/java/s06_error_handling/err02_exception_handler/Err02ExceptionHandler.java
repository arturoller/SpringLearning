package s06_error_handling.err02_exception_handler;

import java.util.List;

/**
 * <pre>
 * TEMAT: Własna obsługa wyjątków — {@code @ExceptionHandler}, {@code @ResponseStatus} i {@code ResponseStatusException}
 *        (exception handler = obsługa wyjątku; response status = status odpowiedzi)
 *
 * W SKRÓCIE:
 *   Domyślny błąd Boota (lekcja Err01) mówi klientowi tylko „coś poszło nie tak”. Gdy chcesz oddać konkretny kod statusu
 *   albo własne ciało błędu, masz trzy narzędzia: (1) adnotację {@code @ResponseStatus} na klasie wyjątku — „ten wyjątek
 *   to zawsze 404”; (2) {@code ResponseStatusException} — wyjątek, który sam niesie status i powód; (3) metodę
 *   {@code @ExceptionHandler} w kontrolerze — pełna kontrola nad statusem i ciałem. Pierwsze dwa tylko ustawiają status
 *   (ciało nadal buduje /error), trzecie buduje całą odpowiedź.
 *
 * ANALOGIA: reklamacje w sklepie.
 *   Wyjątek to reklamacja złożona w trakcie obsługi klienta. {@code @ResponseStatus} to pieczątka „ZWROT” przybita
 *   na formularzu z góry — kasjer wie, co robić, ale nie napisze klientowi listu. {@code @ExceptionHandler} to dział
 *   reklamacji w TYM sklepie: sam decyduje o odpowiedzi i pisze list. Dział reklamacji jednego sklepu nie zajmie się
 *   reklamacjami z innej sieci — tak samo handler w jednym kontrolerze nie widzi wyjątków z innego.
 *
 * JAK TO DZIAŁA:
 *   Gdy z metody kontrolera wyleci wyjątek, DispatcherServlet pyta po kolei rozwiązywaczy wyjątków
 *   (HandlerExceptionResolver) — pierwszy, który wie, co zrobić, wygrywa. Kolejność (sprawdzona testem):
 *     1. ExceptionHandlerExceptionResolver — szuka metody {@code @ExceptionHandler} w kontrolerze
 *        (i w {@code @RestControllerAdvice}, następna lekcja),
 *     2. ResponseStatusExceptionResolver   — czyta {@code @ResponseStatus} z klasy wyjątku i obsługuje
 *        {@code ResponseStatusException}; wywołuje response.sendError(status) → dalej /error (lekcja Err01),
 *     3. DefaultHandlerExceptionResolver   — wyjątki samego Springa (zły typ, brak adresu...) → 400/404/405/415...
 *   Jeżeli nikt nie obsłuży — wyjątek wychodzi z DispatcherServlet i kończy jako 500 przez /error.
 *
 *   Wybór handlera w ramach jednego kontrolera: wygrywa ten, którego typ wyjątku jest NAJBLIŻSZY (najmniej kroków w górę
 *   hierarchii klas) typowi rzuconego wyjątku. CardDeclinedException ← PaymentException ← RuntimeException:
 *   dla CardDeclinedException wygra handler(CardDeclinedException), a dla zwykłego PaymentException — handler(PaymentException).
 *
 *   Handler może przyjmować: wyjątek, a także WebRequest, HttpServletRequest, Model itp.; zwracać: ciało (rekord → JSON),
 *   ResponseEntity (pełna kontrola) albo nic. Status ustawiasz przez {@code @ResponseStatus} na metodzie handlera
 *   albo przez ResponseEntity.
 *
 * SŁÓWKA:
 *   handler = obsługujący (metoda, która „bierze na siebie” wyjątek); resolver = rozwiązywacz (obiekt rozstrzygający, co zrobić
 *   z wyjątkiem); reason = powód; conflict = konflikt (409); gone = zniknął (410); payment required = wymagana płatność
 *   (402); ambiguous = niejednoznaczny (dwa równie dobre wybory); most specific = najbardziej szczegółowy;
 *   hierarchy = hierarchia (drzewo dziedziczenia klas).
 *
 * ZOBACZ TEŻ: s06_error_handling/Err01DefaultErrors (domyślny błąd i /error), s06_error_handling/Err03ControllerAdvice
 *             (handlery wspólne dla wielu kontrolerów), s06_error_handling/Err05CustomExceptions (hierarchia wyjątków
 *             domeny), t10_exceptions/Exceptions05CustomExceptions (własne wyjątki w Javie),
 *             t07_inheritance_polymorphism/Inherit02Override (dziedziczenie).
 * </pre>
 */
public final class Err02ExceptionHandler {

    private Err02ExceptionHandler() {
    }

    // =================================================================================================
    // 1. {@code @ResponseStatus} NA KLASIE WYJĄTKU (testy: Err02ExceptionHandlerTest #1, Err02DefaultJsonTest #1)
    // =================================================================================================
    //
    // ProductNotFoundException ma nad klasą @ResponseStatus(HttpStatus.NOT_FOUND). Kontroler po prostu rzuca wyjątek:
    //     throw new ProductNotFoundException(sku);
    // i klient dostaje 404 — bez żadnego try/catch w kontrolerze. Ciało to nadal domyślny JSON Boota (timestamp, status,
    // error, path) — adnotacja ustawia TYLKO status.
    //
    // Pole reason: @ResponseStatus(value = GONE, reason = "Produkt wycofany ze sprzedaży"). Po włączeniu
    // spring.web.error.include-message=always pole message w JSON-ie ma wtedy wartość „Produkt wycofany ze sprzedaży”,
    // a NIE tekst z konstruktora wyjątku (test Err02MessageVisibilityTest). Bez reason w message trafia tekst wyjątku
    // („Nie ma produktu o SKU XXX”) — pamiętaj o tym, zanim włączysz include-message.
    //
    // PUŁAPKA: {@code @ResponseStatus} zrobi z wyjątku odpowiedź HTTP, ale NIE pozwoli ułożyć własnego ciała (kod błędu,
    //   lista pól). Dlaczego? Adnotacja jest stałą na klasie — nie ma w niej żadnej logiki ani dostępu do zapytania.
    //   Gdy potrzebujesz więcej, użyj {@code @ExceptionHandler}.
    //
    // PUŁAPKA: adnotacja na wyjątku wiąże klasę domenową z HTTP („404” w kodzie, który nie wie, że istnieje internet).
    //   Dlaczego to problem? Ten sam wyjątek wyrzucony z zadania w tle albo z konsoli nie ma statusu HTTP. Lepsze
    //   rozwiązanie na większą skalę: czysty wyjątek domeny + jedno miejsce mapujące go na status (Err03, Err05).

    // =================================================================================================
    // 2. ResponseStatusException (testy: Err02ExceptionHandlerTest #2)
    // =================================================================================================
    //
    // W kontrolerze: throw new ResponseStatusException(HttpStatus.CONFLICT, "Towar " + sku + " jest chwilowo niedostępny");
    // Nie musisz tworzyć osobnej klasy wyjątku — status i powód podajesz w miejscu użycia. Efekt jak w punkcie 1:
    // status 409, ciało z /error, a powód widać w polu message dopiero po include-message=always.
    //
    // DOBRA PRAKTYKA: ResponseStatusException stosuj do szybkich prototypów i jednorazowych przypadków; dla błędów, które
    //   powtarzają się w wielu miejscach, rób własny wyjątek. Dlaczego? Własna klasa ma nazwę mówiącą, co się stało
    //   (ProductNotFoundException), da się ją przechwycić jednym handlerem i przetestować, a „404 z tekstem” rozsiane po
    //   kontrolerach trzeba poprawiać w dziesięciu miejscach naraz.

    // =================================================================================================
    // 3. {@code @ExceptionHandler} W KONTROLERZE (testy: Err02ExceptionHandlerTest #3, #5, #9)
    // =================================================================================================
    //
    // W PaymentController obok metody pay(...) stoją metody oznaczone @ExceptionHandler(PaymentException.class) itd.
    // Gdy pay(...) rzuci wyjątek pasującego typu, Spring wywoła handler i jego wynik stanie się odpowiedzią:
    //     POST /api/payments/other → 402 {"code":"PLATNOSC","message":"Płatność nie powiodła się"}
    // Odpowiedź NIE przechodzi przez /error, więc ciało jest dokładnie takie, jakie zbudował handler (dlatego widać je
    // nawet w MockMvc — Err02ExceptionHandlerTest używa @WebMvcTest zamiast pełnego serwera).
    //
    // PUŁAPKA: handler bez {@code @ResponseStatus} i bez ResponseEntity odpowiada statusem 200 OK, choć ciało opisuje błąd
    //   (test #9: POST /api/payments/refund → 200 i {"code":"NIEOBSLUGIWANE",...}). Dlaczego? Handler to zwykła metoda
    //   kontrolera; jeśli nie powiesz inaczej, jej wynik to „sukces”. Zawsze ustawiaj status jawnie.

    // =================================================================================================
    // 4. KTO WYGRYWA (testy: Err02ExceptionHandlerTest #4, #7, #8, #8b, #10)
    // =================================================================================================
    //
    // a) Najbardziej szczegółowy typ. Dla CardDeclinedException istnieją dwa pasujące handlery: (CardDeclinedException)
    //    i (PaymentException). Wygrywa pierwszy → {"code":"KARTA_ODRZUCONA",...} (test #4). Dla zwykłego PaymentException
    //    zostaje drugi → "PLATNOSC".
    // b) Handler kontra {@code @ResponseStatus}. CouponExpiredException ma @ResponseStatus(GONE), a CouponController ma
    //    handler zwracający 409. Klient dostaje 409 — @ExceptionHandler jest sprawdzany przed @ResponseStatus (test #7, #10).
    // c) Dwa handlery na ten sam typ w jednej klasie to błąd, i to wyjątkowo podstępny: aplikacja STARTUJE bez zarzutu, a
    //    IllegalStateException „Ambiguous @ExceptionHandler method mapped for [ExceptionHandler{exceptionType=
    //    java.lang.IllegalStateException, mediaType=*/*}]: {...first(...), ...second(...)}” leci dopiero przy pierwszym
    //    wyjątku z tego kontrolera (testy #8, #8b — dla handlerów lokalnych w kontrolerze).
    //
    // PUŁAPKA: handler z jednego kontrolera nie obsługuje wyjątków z innego. OrderController rzuca ten sam PaymentException
    //   co PaymentController, ale klient dostaje 500 i domyślny JSON (testy #6, Err02DefaultJsonTest #3). Dlaczego? Metoda
    //   {@code @ExceptionHandler} w klasie kontrolera jest widoczna tylko dla metod tej klasy. Chcesz wspólnej obsługi —
    //   użyj {@code @RestControllerAdvice} (lekcja Err03ControllerAdvice).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @ResponseStatus na klasie wyjątku: stały status (+ opcjonalny reason); ciało nadal z /error.
     *   • ResponseStatusException(status, powód): status w miejscu użycia, bez osobnej klasy; ciało z /error.
     *   • @ExceptionHandler(TypWyjątku.class) w kontrolerze: własne ciało i status; tylko dla metod TEGO kontrolera.
     *   • Status handlera: @ResponseStatus na metodzie albo ResponseEntity — inaczej będzie 200.
     *   • Wygrywa handler o najbliższym typie wyjątku; dwa handlery na ten sam typ → "Ambiguous" przy pierwszym błędzie.
     *   • Kolejność: ExceptionHandlerExceptionResolver → ResponseStatusExceptionResolver → DefaultHandlerExceptionResolver.
     *   • Wyjątek bez żadnej obsługi → 500 przez /error.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się {@code @ResponseStatus} na klasie wyjątku od {@code @ExceptionHandler} pod względem ciała odpowiedzi?
     *   2. Co się stanie? W PaymentController dodajesz handler {@code @ExceptionHandler(UnsupportedOperationException.class)}
     *      zwracający rekord ErrorBody, ale bez {@code @ResponseStatus}. Jaki status zobaczy klient?
     *   3. Co się stanie? Kontroler ma dwa handlery: (PaymentException) i (CardDeclinedException). Metoda rzuca
     *      CardDeclinedException. Który handler odpowie?
     *   4. Co się stanie? Dopisujesz w tym samym kontrolerze drugi handler na IllegalStateException (jest już jeden).
     *      Kiedy i jak dowiesz się o błędzie — przy starcie aplikacji czy później?
     *   5. ZNAJDŹ BŁĄD: w PaymentController jest handler na PaymentException, a OrderController rzuca PaymentException.
     *      Programista oczekuje 402 i własnego ciała, a dostaje 500. Dlaczego i jak to naprawić?
     *   6. ZNAJDŹ BŁĄD: wyjątek ma {@code @ResponseStatus(NOT_FOUND)}, a programista chce, żeby klient dostał kod błędu
     *      "PRODUKT_NIE_ISTNIEJE" w ciele. Czy sama adnotacja to załatwi?
     *   7. Wyjątek ma {@code @ResponseStatus(GONE)}, a kontroler ma handler zwracający 409. Co dostanie klient?
     *   8. Po co ma sens ResponseStatusException, skoro jest {@code @ResponseStatus}?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Err02ExceptionHandlerExercisesTest (rozwiązania: solutions.err02_exception_handler)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): czy handler zadeklarowany dla typu {@code handled} obsłuży wyjątek typu {@code thrown}?
     * Obsłuży, gdy {@code thrown} jest tym samym typem albo jego podklasą.
     * Podpowiedź: metoda isAssignableFrom z klasy Class (t07_inheritance_polymorphism/Inherit05Polymorphism).
     */
    static boolean canHandle(Class<? extends Throwable> handled, Class<? extends Throwable> thrown) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): odległość między typem handlera a rzuconym wyjątkiem — ile kroków trzeba zrobić w górę
     * hierarchii (getSuperclass) od {@code thrown}, żeby dojść do {@code handled}. Ten sam typ → 0, rodzic → 1, dziadek → 2.
     * Gdy handler nie pasuje, zwróć -1.
     * Przykład: odległość (PaymentException, CardDeclinedException) = 1.
     * Podpowiedź: pętla {@code for (Class<?> c = thrown; c != null; c = c.getSuperclass())} i licznik kroków.
     */
    static int distance(Class<? extends Throwable> handled, Class<? extends Throwable> thrown) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): z listy typów obsługiwanych przez handlery wybierz ten, który wygra dla rzuconego wyjątku
     * (najmniejsza odległość z ćwiczenia 2). Gdy żaden nie pasuje, zwróć null. Gdy dwa mają tę samą odległość (to samo
     * w liście dwa razy), zgłoś IllegalStateException z tekstem zaczynającym się od "Ambiguous" — tak jak Spring.
     * Podpowiedź: nie zakładaj kolejności elementów listy; policz odległość dla każdego i znajdź minimum.
     */
    static Class<? extends Throwable> pickHandler(List<Class<? extends Throwable>> handled, Class<? extends Throwable> thrown) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @ResponseStatus ustawia tylko status; ciało buduje /error (domyślny JSON Boota). @ExceptionHandler buduje całą
     *      odpowiedź — status i ciało są takie, jak zwróci metoda. Sprawdzone: Err02DefaultJsonTest #1 i #2.
     *   2. 200 OK — handler to zwykła metoda kontrolera, a bez @ResponseStatus/ResponseEntity wynik jest „sukcesem”,
     *      mimo że ciało opisuje błąd. Sprawdzone: Err02ExceptionHandlerTest #9 (POST /api/payments/refund).
     *   3. Handler (CardDeclinedException) — wygrywa najbliższy typ. Sprawdzone: Err02ExceptionHandlerTest #4.
     *   4. Aplikacja startuje poprawnie. IllegalStateException „Ambiguous @ExceptionHandler method mapped for
     *      [ExceptionHandler{exceptionType=java.lang.IllegalStateException, ...}]: {...}” pojawia się
     *      dopiero przy pierwszym wyjątku z tego kontrolera (handlery lokalne w kontrolerze są wyszukiwane leniwie).
     *      Sprawdzone: Err02ExceptionHandlerTest #8 i #8b.
     *   5. Handler w klasie PaymentController obowiązuje tylko dla jej metod, więc OrderController nie jest nim objęty
     *      i wyjątek kończy jako 500 z domyślnym JSON-em. Naprawa: handler przenieść do klasy z @RestControllerAdvice
     *      (Err03ControllerAdvice) albo dodać handler w OrderController. Sprawdzone: Err02DefaultJsonTest #3.
     *   6. Nie. Adnotacja ustawia sam status; ciało to domyślny JSON Boota (bez własnego kodu). Potrzebny jest
     *      @ExceptionHandler z własnym rekordem odpowiedzi.
     *   7. 409 z ciałem z handlera — @ExceptionHandler jest sprawdzany przed @ResponseStatus (ExceptionHandlerExceptionResolver
     *      stoi w kolejce przed ResponseStatusExceptionResolver). Sprawdzone: Err02ExceptionHandlerTest #7 i #10.
     *   8. ResponseStatusException nie wymaga nowej klasy ani adnotacji, a status/powód da się ustawić dynamicznie
     *      (np. status zależny od danych) — adnotacja na klasie jest stała. Do powtarzających się błędów lepszy
     *      jednak własny wyjątek.
     */
    // </editor-fold>
}

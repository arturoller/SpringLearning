package s06_error_handling.err05_custom_exceptions;

/**
 * <pre>
 * TEMAT: Własne wyjątki domenowe sklepu i ich mapowanie na odpowiedzi HTTP
 *        (custom exception = własny wyjątek; domain = dziedzina, "świat" aplikacji — tu sklep; hierarchy = hierarchia,
 *         drzewo klas dziedziczących po sobie; business rule = reguła biznesowa; mapping = mapowanie, przyporządkowanie
 *         wyjątku do statusu; unchecked = niekontrolowany, bez wymuszania obsługi przez kompilator)
 *
 * W SKRÓCIE:
 *   Do tej pory (Err01-Err04) wyjątki były dodatkiem do przykładów. Tu budujemy je świadomie jako część projektu: mała hierarchia
 *   klas {@code ShopException → NotFoundException / BusinessRuleException → konkretne wyjątki}, każdy z własnym kodem biznesowym
 *   ({@code OUT_OF_STOCK}). Warstwa serwisu rzuca je i NIE zna HTTP. Jedno {@code @RestControllerAdvice} (Err04ProblemDetail)
 *   zamienia je na {@code ProblemDetail} wg tabeli "wyjątek → status": 404 (nie ma), 409 (konflikt ze stanem), 422 (naruszona
 *   reguła). Kod biznesowy trafia do odpowiedzi, więc klient rozróżnia błędy bez parsowania tekstu. Błędy klienta (4xx) logujemy
 *   jedną linią bez śladu stosu, błędy serwera (5xx) z pełnym śladem. Całość testujemy trzema poziomami: serwis, kontroler z atrapą
 *   serwisu, jeden test na prawdziwym serwerze.
 *
 * ANALOGIA: system zgłoszeń w sklepie zamiast krzyku przez halę.
 *   Kasjer, magazynier i księgowa nie wołają "coś jest nie tak!". Używają ponumerowanych formularzy zgłoszeń z grupami:
 *   "A — nie ma takiej rzeczy" (NotFound), "B — rzecz jest, ale nie wolno" (BusinessRule). Każdy formularz ma stały numer
 *   (kod biznesowy). Pracownicy NIE wiedzą, jak kierownictwo odpowie klientowi (jaki kod HTTP) — wypełniają tylko formularz.
 *   Dopiero biuro obsługi klienta (advice) ma tablicę: "grupa A → odpowiedź 404, B/brak towaru → 409, B/reszta → 422".
 *   Zmiana wzoru odpowiedzi wymaga zmiany jednej tablicy w biurze, a nie przeszkolenia wszystkich pracowników.
 *
 * JAK TO DZIAŁA:
 *   1. Serwis ({@code OrderService}) sprawdza reguły i rzuca konkretny wyjątek: {@code new OutOfStockException(...)}.
 *   2. Wyjątek wylatuje przez kontroler (bez {@code try/catch}) do {@code DispatcherServlet}, a ten oddaje go do
 *      {@code ExceptionHandlerExceptionResolver} (Err02-Err03).
 *   3. {@code Err05Advice} wybiera handler dla NAJBLIŻSZEGO typu w hierarchii wyjątku: dla {@code OutOfStockException} jest handler
 *      dedykowany (409), dla {@code MinimumOrderValueException} dopiero ten dla {@code BusinessRuleException} (422).
 *   4. Handler loguje (INFO dla 4xx, ERROR ze śladem dla 5xx) i buduje {@code ProblemDetail} z kodem biznesowym.
 *   5. Spring ustawia status i {@code application/problem+json}; klient czyta {@code code}.
 *
 * SŁÓWKA:
 *   unchecked exception = wyjątek niekontrolowany; hierarchy = hierarchia; root = korzeń; branch = gałąź; conflict = konflikt;
 *   unprocessable content = treść nie do przetworzenia (422); correlation id = identyfikator korelacji (numer łączący wszystkie
 *   wpisy w logach jednego żądania); flow control = sterowanie przepływem programu; swallow = połknąć (ukryć wyjątek).
 *
 * ZOBACZ TEŻ: t10_exceptions/Exceptions02CheckedUnchecked (wyjątki kontrolowane i niekontrolowane),
 *             t10_exceptions/Exceptions05CustomExceptions (własne wyjątki w Javie),
 *             t10_exceptions/Exceptions07BestPractices (dobre praktyki obsługi wyjątków),
 *             t14_optional/Optional03BestPractices (Optional zamiast wyjątku w pytaniach),
 *             s05_validation/Val05ServiceValidation (walidacja w serwisie, reguły biznesowe),
 *             s06_error_handling/Err03ControllerAdvice (najbliższy typ wyjątku, kolejność advice),
 *             s06_error_handling/Err04ProblemDetail (ProblemDetail, ResponseEntityExceptionHandler, kody w type i code).
 * </pre>
 */
public final class Err05CustomExceptions {

    private Err05CustomExceptions() {
    }

    // =================================================================================================
    // 1. HIERARCHIA WYJĄTKÓW SKLEPU (ShopException i potomkowie, Err05OrderServiceTest.hierarchy)
    // =================================================================================================
    //
    //     RuntimeException
    //       └── ShopException (abstract; pole code)
    //             ├── NotFoundException (abstract)  ← "nie ma takiego zasobu"
    //             │     ├── ProductNotFoundException        PRODUCT_NOT_FOUND
    //             │     └── OrderNotFoundException          ORDER_NOT_FOUND
    //             └── BusinessRuleException (abstract)  ← "jest, ale reguła tego zabrania"
    //                   ├── OutOfStockException             OUT_OF_STOCK
    //                   ├── OrderAlreadyShippedException    ORDER_ALREADY_SHIPPED
    //                   └── MinimumOrderValueException      MINIMUM_ORDER_VALUE
    //
    // Po co hierarchia, skoro wystarczyłoby pięć osobnych klas? Dzięki niej handler łapie CAŁĄ gałąź jednym wpisem
    // ({@code @ExceptionHandler(NotFoundException.class)}), a dodanie nowego wyjątku ("klient nie istnieje") nie wymaga zmian
    // w advice (test newSubclassNeedsNoNewHandler: klasa lokalna dziedzicząca po NotFoundException dostaje 404 od razu).
    //
    // Dlaczego {@code abstract}? Nie ma sensu rzucać "ogólnego wyjątku sklepu" ani "ogólnego braku zasobu" — zawsze chcemy wiedzieć
    // CO dokładnie zawiodło. Klasa abstrakcyjna wymusza wybór konkretnego wyjątku (i jego kodu).
    //
    // Dlaczego niekontrolowane ({@code extends RuntimeException}, a nie {@code Exception})? Trzy powody:
    //   • kontrolowany wyjątek wymusza {@code throws} w każdej metodzie po drodze (kontroler → serwis → repozytorium), więc
    //     sygnatury zapełniają się wyjątkami, których te warstwy i tak nie obsłużą — obsługa jest w JEDNYM miejscu, w advice;
    //   • domyślnie Spring wycofuje transakcję (rollback) tylko dla wyjątków niekontrolowanych — kontrolowany wyjątek
    //     z serwisu zostawiłby zapisane połowicznie dane, o ile nie dopiszesz {@code rollbackFor};
    //   • lambdy i strumienie (Optional.orElseThrow, stream.map) nie lubią wyjątków kontrolowanych.
    // Wiele więcej o różnicy — w t10_exceptions/Exceptions02CheckedUnchecked i Exceptions07BestPractices.
    //
    // Kod biznesowy ({@code getCode()}) siedzi W wyjątku, a nie jest wyliczany z nazwy klasy. Dlaczego? Nazwę klasy można zmienić
    // przy refaktoryzacji (a z nią zmieniłby się kontrakt API), a stała tekstowa jest częścią umowy z klientem.
    //
    // DOBRA PRAKTYKA: komunikat wyjątku domenowego pisz dla człowieka, bez danych wrażliwych ("Brak towaru: Kawa (chcesz 9, jest 5)").
    //   Dlaczego? Trafia do pola {@code detail} odpowiedzi (sekcja 3) — a to jedyne miejsce, w którym świadomie wpuszczamy tekst
    //   wyjątku do klienta. Wyjątki techniczne (SQL, IO) tego nie robią i dostają ogólny opis (sekcja 4).
    //
    // DOBRA PRAKTYKA: mała hierarchia, 2-3 poziomy. Dlaczego? Głębokie drzewa (10 poziomów) są trudne do ogarnięcia, a mapowanie
    //   i tak opiera się na kilku kategoriach: nie ma / konflikt / reguła / błąd serwera.

    // =================================================================================================
    // 2. SERWIS BEZ HTTP, MAPOWANIE W JEDNYM ADVICE (OrderService, Err05Advice, Err05CustomExceptionsTest.statusTable)
    // =================================================================================================
    //
    // Serwis rzuca {@code new OutOfStockException("Kawa", 9, 5)} i NIE wie, co z tego wyniknie dla klienta. W pliku OrderService nie ma
    // ani jednego importu z pakietów HTTP Springa. Zysk: ten sam serwis obsłuży REST, zadanie nocne i kolejkę komunikatów,
    // a test serwisu (Err05OrderServiceTest) to zwykły {@code new OrderService()} bez Springa i bez MockMvc.
    //
    // Cała wiedza "wyjątek → status" jest w Err05Advice, w jednej tabeli:
    //
    //     wyjątek                                                  status
    //     ───────────────────────────────────────────────────────  ──────────────────────────────
    //     NotFoundException (Product…, Order…)                     404 Not Found
    //     OutOfStockException, OrderAlreadyShippedException        409 Conflict
    //     BusinessRuleException (reszta, np. MinimumOrderValue)    422 Unprocessable Content
    //     Exception (nieoczekiwany)                                500 Internal Server Error
    //
    // Skąd te statusy? 404 — zasób nie istnieje. 409 — żądanie jest poprawne, ale koliduje z obecnym STANEM systemu (towaru
    // brak, zamówienie już wysłane); zmiana stanu (dostawa) może sprawić, że to samo żądanie za chwilę się uda. 422 —
    // żądanie jest zrozumiałe, ale treść łamie regułę biznesową (za mała ilość), więc ponowienie bez zmiany treści nic nie da.
    // Granica bywa płynna; liczy się, żeby cały zespół trzymał się jednej tabeli.
    //
    // UWAGA (Spring Framework 7): kod 422 ma w {@code HttpStatus} nową nazwę {@code UNPROCESSABLE_CONTENT} (zgodną z RFC 9110).
    // Stara stała {@code UNPROCESSABLE_ENTITY} jest oznaczona jako przestarzała (od wersji 7.0) — w starych poradnikach
    // zobaczysz jeszcze starą nazwę.
    //
    // NAJBLIŻSZY TYP: dla {@code OutOfStockException} pasują dwa handlery — dedykowany (409) i ogólniejszy dla
    // {@code BusinessRuleException} (422). Wygrywa bliższy typ, czyli 409 (Err03ControllerAdvice, sekcja o kolejności). Dlatego
    // handler 409 jest wyjątkiem od "domyślnych" 422 bez żadnej dodatkowej konfiguracji.
    //
    // KONTRA: {@code @ResponseStatus} na klasie wyjątku. Przykład w kodzie: {@code SupplierNotFoundException} z
    // {@code @ResponseStatus(NOT_FOUND)} (Rest03Responses). Wygodne, ale:
    //   • wyjątek zaczyna "znać HTTP" — status jest wpisany w klasę domenową, więc nie użyjesz jej tak samo w zadaniu wsadowym;
    //   • sam status bez ProblemDetail: bez kodu biznesowego i bez własnych pól (test Err05ResponseStatusWithoutAdviceTest: 404 z pustym ciałem);
    //   • status jest sztywny — jedna klasa nie może być raz 404, raz 409 zależnie od kontekstu.
    //
    // PUŁAPKA: adnotacja {@code @ResponseStatus} na wyjątku przy obecnym handlerze {@code Exception.class} w advice. Dlaczego? Handlery
    //   z {@code @ExceptionHandler} są sprawdzane PIERWSZE, a handler {@code Exception.class} pasuje do wszystkiego — adnotacja
    //   na wyjątku w ogóle nie zostaje uwzględniona. Test Err05ResponseStatusWithCatchAllTest: {@code SupplierNotFoundException} daje 500
    //   (UNEXPECTED) zamiast 404. Rozwiązanie: usuń adnotację i dziedzicz po {@code NotFoundException} (wtedy handler z tabeli
    //   zadziała), albo dopisz osobny handler. W nowym kodzie mapuj statusy w advice, nie w wyjątkach.
    //
    // PUŁAPKA: rzucanie z serwisu {@code ResponseStatusException} albo {@code ErrorResponseException} (Err04ProblemDetail). Dlaczego?
    //   Serwis zaczyna znać HTTP, a wyjątek jest "jednorazowy" — status zakodowany przy każdym {@code throw}, zamiast w jednej
    //   tabeli. Te wyjątki są dobre w warstwie web (kontroler, filtr), nie w logice biznesowej.

    // =================================================================================================
    // 3. KOD BIZNESOWY W PROBLEMDETAIL (Err05CustomExceptionsTest.businessCodeInProblemDetail)
    // =================================================================================================
    //
    // Odpowiedź na {@code POST /err05/orders} z ilością 9 (a w magazynie jest 5):
    //
    //     HTTP/1.1 409
    //     Content-Type: application/problem+json
    //
    //     {
    //       "type": "https://sklep.example/errors/out-of-stock",
    //       "title": "Konflikt ze stanem sklepu",
    //       "status": 409,
    //       "detail": "Brak towaru: Kawa (chcesz 9, jest 5)",
    //       "instance": "/err05/orders",
    //       "code": "OUT_OF_STOCK"
    //     }
    //
    // Klient (front, aplikacja mobilna) pisze {@code switch (response.code)}: dla {@code OUT_OF_STOCK} pokazuje "Zmniejsz ilość",
    // dla {@code MINIMUM_ORDER_VALUE} "Zamów co najmniej 2 sztuki". Nie parsuje tekstu z {@code detail} (który można poprawić
    // albo przetłumaczyć) i nie zgaduje po samym statusie (409 ma tu dwa różne powody: brak towaru i wysłane zamówienie).
    //
    // Adres {@code type} powstaje z kodu: {@code OUT_OF_STOCK} → {@code .../out-of-stock} (małe litery, myślniki). Dzięki temu nowy wyjątek
    // dostaje poprawny {@code type} bez ręcznej pracy. {@code title} jest wspólny dla kategorii (404 / 409 / 422), a {@code detail}
    // pochodzi z wyjątku — zgodnie z podziałem ról z Err04ProblemDetail.
    //
    // Wspólna metoda {@code clientError(…)} w advice składa wszystkie trzy kategorie, więc format jest identyczny. Dlaczego to ważne?
    // Gdyby każdy handler budował ProblemDetail po swojemu, pierwszy nowy handler zepsułby spójność (Err03, "jeden format").
    //
    // DOBRA PRAKTYKA: kody biznesowe w stałym formacie (WIELKIE_LITERY_Z_PODKREŚLNIKAMI), po angielsku, nigdy nie używane ponownie
    //   dla innego znaczenia. Dlaczego? Kod jest umową z klientem — zmiana znaczenia istniejącego kodu psuje cudze aplikacje
    //   po cichu, bez błędu kompilacji.

    // =================================================================================================
    // 4. LOGOWANIE: 4xx CICHO, 5xx GŁOŚNO (Err05Advice, Err05LoggingTest)
    // =================================================================================================
    //
    //   • 4xx (nie ma, konflikt, reguła) to błąd KLIENTA, spodziewany w normalnej pracy: ktoś wpisał zły numer, ktoś zamówił za dużo.
    //     Logujemy JEDNĄ linię na poziomie INFO (można WARN, jeśli chcesz je zliczać), bez śladu stosu:
    //       INFO … Err05Advice : 404 PRODUCT_NOT_FOUND: Nie ma produktu o id 9
    //     Test clientErrorIsOneInfoLine: jest "404 PRODUCT_NOT_FOUND", nie ma "ERROR" ani linii "\tat s06_error_handling".
    //   • 5xx (nieoczekiwany wyjątek) to błąd NASZ: poziom ERROR i PEŁNY ślad stosu, bo tylko on mówi, gdzie w kodzie to pękło.
    //     Wyjątek przekazujemy jako OSTATNI argument ({@code log.error("…", exception)}), inaczej logger zapisze sam komunikat.
    //     Test serverErrorHasStackTrace: w logu jest ERROR, nazwa wyjątku i linia śladu z kontrolera.
    //
    // Dlaczego nie ślad stosu dla każdego 4xx? Po pierwsze, szum: na ruchliwym API "nie znaleziono" zdarza się tysiące razy
    // dziennie, ślady stosu zasłoniłyby prawdziwe awarie. Po drugie, koszt: ślad stosu to kilkadziesiąt linii na każde żądanie.
    // Po trzecie, ślad stosu błędu klienta nie niesie informacji — wiemy, skąd się wziął (reguła w serwisie). Odwrotnie, brak śladu
    // przy 5xx utrudnia diagnozę, bo nie wiadomo, KTÓRA linia rzuciła NullPointerException.
    //
    // Alarmy: monitoring (dział s15) zwykle zlicza ERROR, więc jeśli 4xx logujesz jako ERROR, zespół jest budzony w nocy przez
    // literówki w adresach.
    //
    // Identyfikator korelacji (correlation id): w prawdziwym systemie każde żądanie dostaje numer, który trafia do wszystkich linii
    // logów i do odpowiedzi błędu (np. jako dodatkowa właściwość ProblemDetail) — klient podaje go w zgłoszeniu, a Ty
    // znajdujesz wpis w logach. Tu tylko o tym wspominamy; w kursie wraca w dziale s15.
    //
    // DOBRA PRAKTYKA: loguj wyjątek RAZ, w advice. Dlaczego? Jeśli serwis loguje i rzuca dalej, kontroler loguje, a advice loguje
    //   jeszcze raz, to samo zdarzenie ma trzy ślady stosu, a licznik błędów w monitoringu pokazuje trzykrotnie zawyżoną liczbę.
    //
    // PUŁAPKA: dopisywanie do odpowiedzi 500 tekstu wyjątku "żeby było wiadomo, co się stało". Dlaczego? Klient dostaje SQL,
    //   nazwy hostów i ścieżki (Err01DefaultErrors). Test unexpectedException: odpowiedź nie zawiera "hasło" z komunikatu wyjątku.

    // =================================================================================================
    // 5. TESTOWANIE I PUŁAPKI (Err05OrderServiceTest, Err05CustomExceptionsTest, Err05ServerTest, katalog err05_pitfalls)
    // =================================================================================================
    //
    // Trzy poziomy testów, od najtańszego:
    //   1. SERWIS — zwykły test jednostkowy z {@code assertThatThrownBy(() -> service.placeOrder(1, 9)).isInstanceOf(OutOfStockException.class)}.
    //      Sprawdzamy typ i kod, nie status HTTP (serwis go nie zna). Bez Springa, milisekundy.
    //   2. KONTROLER + ADVICE — {@code @WebMvcTest(ShopController.class)} z {@code @MockitoBean OrderService}: atrapa serwisu rzuca wyjątki
    //      ({@code given(service.getProduct(9)).willThrow(new ProductNotFoundException(9))}), a my sprawdzamy status i JSON. To jedyne
    //      sensowne miejsce na tabelę mapowania — nie trzeba odtwarzać stanu magazynu, żeby uzyskać każdy wyjątek.
    //   3. SERWER — jeden test {@code @SpringBootTest(RANDOM_PORT)} z prawdziwym serwisem: dowód, że całość składa się w jedną
    //      odpowiedź (404, 409, 409, 422 i 500), z nagłówkiem {@code application/problem+json}. Tylko żądania, które nie zmieniają danych —
    //      kontekst Springa bywa współdzielony między klasami testowymi, więc stan nie może zależeć od kolejności testów.
    //   Slice {@code @WebMvcTest} wystarcza dla poziomu 2, bo sprawdzamy warstwę web (Err03).
    //
    // PUŁAPKA: łapanie wyjątku w kontrolerze i zwracanie 200 z {@code {"error": "…"}}. Dlaczego? Kod 200 mówi całej infrastrukturze
    //   (przeglądarka, bramka API, cache, monitoring, klient HTTP z automatycznym {@code raiseForStatus}) "wszystko w porządku".
    //   Test Err05CatchingControllerTest: ten sam błąd to 200 w {@code CatchingController} i 404 w {@code ShopController}.
    //   Dodatkowo każdy kontroler wymyśla własny kształt błędu. Rozwiązanie: nie łap — niech wyjątek poleci do advice.
    //
    // PUŁAPKA: wyjątki do sterowania przepływem w zwykłych przypadkach, np. "czy produkt istnieje?" przez try/catch na
    //   {@code ProductNotFoundException}. Dlaczego? Brak produktu w pytaniu nie jest błędem, tylko jedną z dwóch normalnych odpowiedzi.
    //   Tworzenie wyjątku zbiera ślad stosu (koszt), a kod z try/catch jest dłuższy i mniej czytelny. Użyj {@code Optional}
    //   (t14_optional/Optional03BestPractices): {@code OrderService} ma dwie metody — {@code findProduct} ("pytam", zwraca Optional)
    //   i {@code getProduct} ("wymagam", rzuca wyjątek). Test existsUsesOptional: 200 z {@code {"exists": false}}, a nie 404.
    //   Zasada: wyjątek = sytuacja, w której wołający oczekiwał, że coś JEST; Optional = zwykłe pytanie "czy jest?".
    //
    // PUŁAPKA: wyjątek domenowy z niepoprawnym zakresem — np. {@code ProductNotFoundException} rzucany także dla błędnie sformułowanego
    //   żądania (id -5). Dlaczego? Mieszasz kategorie ("nie ma" i "źle zapytano"), przez co 404 i 400 tracą swoje znaczenie.
    //   Poprawność formalną (liczba dodatnia, pole wymagane) sprawdza walidacja (s05_validation/Val02ValidInControllers), reguły
    //   biznesowe — serwis (Val05ServiceValidation).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Hierarchia: ShopException (abstract, unchecked, pole code) → NotFoundException / BusinessRuleException → konkretne wyjątki.
     *   • Unchecked: brak przymusowego throws w warstwach, domyślny rollback transakcji, wygodne w lambdach.
     *   • Serwis rzuca wyjątki domenowe i nie zna HTTP; mapowanie wyjątek → status w JEDNYM advice (tabela).
     *   • Najbliższy typ wygrywa: OutOfStock/OrderAlreadyShipped → 409, reszta BusinessRule → 422, NotFound → 404, reszta → 500.
     *   • 422 = HttpStatus.UNPROCESSABLE_CONTENT (UNPROCESSABLE_ENTITY przestarzała od Spring 7).
     *   • @ResponseStatus na wyjątku: wygodne, ale miesza warstwy, daje puste ciało i jest przesłaniane przez handler Exception.class.
     *   • Kod biznesowy w właściwości code (+ type z kodu): klient rozróżnia błędy bez parsowania tekstu.
     *   • Logowanie: 4xx — jedna linia INFO bez śladu stosu; 5xx — ERROR i pełny ślad (wyjątek jako ostatni argument); raz, w advice.
     *   • Testy: serwis jednostkowo, kontroler @WebMvcTest + @MockitoBean serwisu, jeden test na prawdziwym serwerze.
     *   • Pułapki: łapanie w kontrolerze i 200 z "error"; wyjątki zamiast Optional w zwykłym pytaniu; @ResponseStatus + Exception.class.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego {@code ShopException} dziedziczy po {@code RuntimeException}, a nie po {@code Exception}? Podaj dwa powody.
     *   2. Co się stanie? Dopisujesz {@code GiftCardExpiredException extends BusinessRuleException} i nie zmieniasz advice. Jaki status
     *      dostanie klient i dlaczego?
     *   3. Co się stanie? {@code Err05Advice} ma handlery dla {@code BusinessRuleException} (422) i dla {@code OutOfStockException} (409).
     *      Serwis rzuca {@code OutOfStockException}. Który handler odpowie?
     *   4. ZNAJDŹ BŁĄD: wyjątek {@code SupplierNotFoundException} ma {@code @ResponseStatus(NOT_FOUND)}, a klient dostaje 500
     *      z kodem UNEXPECTED. Dlaczego i jak to naprawić?
     *   5. ZNAJDŹ BŁĄD: kontroler robi {@code try { return service.getProduct(id); } catch (ProductNotFoundException e) { return Map.of("error", e.getMessage()); }}.
     *      Co jest nie tak z punktu widzenia klienta i monitoringu?
     *   6. Co się stanie w logu? Klient prosi o produkt, którego nie ma (404), a potem wywołujesz endpoint, który rzuca
     *      {@code IllegalStateException}. Jakie wpisy (poziom, ślad stosu) powstaną?
     *   7. Dlaczego serwis nie powinien rzucać {@code ResponseStatusException}? Co zyskujesz, gdy rzuca wyjątek domenowy?
     *   8. Kiedy użyjesz {@code Optional}, a kiedy wyjątku przy wyszukiwaniu produktu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, ExerciseAdvice; testy: Err05CustomExceptionsExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Rozwiązania wzorcowe: pakiet s06_error_handling.solutions.err05_custom_exceptions. Wszystkie adresy mają prefiks /err05/cw.
    // Hierarchia wyjątków ćwiczeń jest gotowa w ExerciseController (ExerciseException → ExerciseNotFoundException /
    // ExerciseRuleException → konkretne). Test buduje kontekst ręcznie (kontroler + ExerciseAdvice), bez Err05Advice z lekcji.
    // Wszystkie odpowiedzi to ProblemDetail. Type = "https://sklep.example/errors/" + kod małymi literami z myślnikami.
    //
    // ĆWICZENIE 1 (łatwe): klient — w ExerciseController.customer dla id 1 zwróć "Anna", dla innych rzuć CustomerNotFoundException.
    //   W ExerciseAdvice napisz JEDEN handler dla ExerciseNotFoundException → 404, title "Nie znaleziono zasobu", detail z
    //   wyjątku, code z wyjątku, type z kodu. Podpowiedź: wzoruj się na Err05Advice.onNotFound i metodzie clientError.
    //
    // ĆWICZENIE 2 (łatwe): płatność — pay rzuca PaymentDeclinedException. Handler dla ExerciseRuleException → 422 (UNPROCESSABLE_CONTENT),
    //   title "Naruszona reguła biznesowa". Podpowiedź: możesz wydzielić prywatną metodę budującą ProblemDetail, wspólną dla handlerów.
    //
    // ĆWICZENIE 3 (średnie): wyjątek od reguły — payInvoice rzuca InvoiceAlreadyPaidException. Dopisz osobny handler → 409, title
    //   "Konflikt ze stanem sklepu". Płatność z ćwiczenia 2 ma nadal dawać 422. Podpowiedź: nie zmieniaj handlera z ćwiczenia 2 —
    //   Spring wybierze handler dla najbliższego typu wyjątku.
    //
    // ĆWICZENIE 4 (średnie): "łap wszystko" — crash rzuca IllegalStateException z "tajnym" tekstem. Dopisz handler Exception.class →
    //   500, detail "Wystąpił nieoczekiwany błąd", code "UNEXPECTED", wyjątek zaloguj (log.error z wyjątkiem jako ostatnim argumentem).
    //   Test sprawdza też, że DELETE /err05/cw/crash daje nadal 405 (klasa bazowa ResponseEntityExceptionHandler pilnuje wyjątków Springa).
    //
    // ĆWICZENIE 5 (trudniejsze): magazyn zajęty — refreshStock rzuca WarehouseBusyException(30) (poza hierarchią ćwiczeń). Handler ma
    //   zwrócić ResponseEntity<ProblemDetail>: status 503, nagłówek Retry-After: 30, właściwości code = "WAREHOUSE_BUSY"
    //   i retryAfterSeconds = 30. Podpowiedź: ResponseEntity.status(...).header("Retry-After", ...).body(problem).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. (a) Wyjątek kontrolowany wymusza throws w każdej warstwie po drodze, choć obsługa jest w jednym miejscu (advice);
     *      (b) domyślnie Spring wycofuje transakcję tylko dla wyjątków niekontrolowanych, więc kontrolowany wyjątek z serwisu
     *      mógłby zostawić dane zapisane połowicznie; (c) wygodniejsze w lambdach, np. {@code orElseThrow}.
     *   2. 422 Unprocessable Content: dla nowego wyjątku z gałęzi BusinessRuleException najbliższy pasujący handler to handler
     *      klasy BusinessRuleException (test statusTable: MinimumOrderValueException → 422). Zero zmian w advice. Gdy chcesz 409,
     *      dopisz go do listy handlera konfliktów.
     *   3. Handler dla OutOfStockException (409): wygrywa najbliższy typ w hierarchii wyjątku (test statusTable: OUT_OF_STOCK → 409).
     *   4. Handler Exception.class w advice pasuje do wszystkiego i jest sprawdzany przed adnotacją {@code @ResponseStatus} na
     *      wyjątku, więc adnotacja jest pomijana (test Err05ResponseStatusWithCatchAllTest). Naprawa: usuń adnotację i dziedzicz po
     *      NotFoundException (albo dopisz osobny handler). Bez żadnego advice adnotacja działa, ale ciało jest puste
     *      (test Err05ResponseStatusWithoutAdviceTest).
     *   5. Klient widzi 200, czyli "sukces": przeglądarka, bramka, cache i monitoring nie wykryją błędu, a kształt błędu jest
     *      wymyślony przez ten jeden kontroler (test Err05CatchingControllerTest: 200 vs 404). Naprawa: nie łap, niech wyjątek poleci do advice.
     *   6. 404: jedna linia INFO "404 PRODUCT_NOT_FOUND: …", bez śladu stosu. 500: wpis ERROR z pełnym śladem stosu
     *      (testy clientErrorIsOneInfoLine i serverErrorHasStackTrace). Klient dostaje w obu przypadkach tylko ProblemDetail.
     *   7. Serwis zaczyna znać HTTP, status jest rozproszony po wszystkich miejscach {@code throw}, a ta sama logika nie nadaje się do
     *      zadania wsadowego czy kolejki. Wyjątek domenowy opisuje CO się stało, a advice decyduje, jak to pokazać klientowi: zmiana
     *      statusu to zmiana jednej tabeli, serwis testujesz bez Springa (Err05OrderServiceTest).
     *   8. Optional: pytanie "czy jest?", w którym brak jest zwykłą odpowiedzią (endpoint exists → 200 z false). Wyjątek: wołający
     *      oczekiwał, że produkt ISTNIEJE (zamówienie na nieistniejący produkt → ProductNotFoundException → 404).
     */
    // </editor-fold>
}

package s05_validation.val02_valid_in_controllers;

/**
 * <pre>
 * TEMAT: @Valid w kontrolerach — walidacja ciała żądania i parametrów oraz czytelne błędy
 *        (valid = poprawny/ważny; request body = ciało żądania HTTP; request parameter = parametr żądania;
 *         path variable = zmienna ścieżki; controller = kontroler, klasa obsługująca zapytania HTTP)
 *
 * W SKRÓCIE:
 *   Dane od klienta (JSON, parametry w adresie) są NIEZAUFANE — trzeba je sprawdzić, zanim trafią do logiki.
 *   Adnotacja @Valid przy argumencie @RequestBody każe Springowi uruchomić Bean Validation na obiekcie z JSON-a;
 *   gdy coś jest nie tak, kontroler się nie wykonuje, a klient dostaje 400 Bad Request (złe zapytanie). Ograniczenia
 *   na pojedynczych parametrach (@RequestParam @Positive, @PathVariable @Min(1)) od Frameworka 6.1 sprawdza
 *   WBUDOWANA walidacja metod — bez @Validated na klasie. Tu, w Springu 7, nadal tak jest.
 *
 * ANALOGIA: recepcja w biurowcu.
 *   Portier (Spring) sprawdza przepustkę (@Valid) przy wejściu — zanim gość dotrze do pokoju (metoda kontrolera).
 *   Bez portiera (brak @Valid) każdy wchodzi, nawet z fałszywą przepustką. Portier może odesłać kartkę „odmowa
 *   wejścia: brak zdjęcia, nieważna data” (czytelny JSON) albo tylko pokręcić głową (sam kod 400).
 *
 * JAK TO DZIAŁA:
 *   POST /api/products  {"name":"","code":"x","priceGrosze":0}
 *     1. DispatcherServlet znajduje metodę create(@Valid @RequestBody ProductRequest)
 *     2. RequestResponseBodyMethodProcessor zamienia JSON na ProductRequest (Jackson)
 *     3. przez @Valid woła walidator (LocalValidatorFactoryBean) — powstaje BindingResult z błędami pól
 *     4. są błędy → MethodArgumentNotValidException; metoda create(...) NIE jest wywołana
 *     5. @ExceptionHandler z kontrolera (lub domyślny resolver) zamienia wyjątek na odpowiedź 400
 *   GET /api/products?limit=0   (limit ma @Positive)
 *     1. HandlerMethod sprawdza ograniczenia parametrów PRZED wywołaniem metody (walidacja metod z Frameworka 6.1)
 *     2. są błędy → HandlerMethodValidationException (rozszerza ResponseStatusException) → domyślnie 400
 *
 * SŁÓWKA:
 *   bad request = złe zapytanie (400); binding result = wynik wiązania danych (lista błędów); field error = błąd pola;
 *   exception handler = obsługa wyjątku; method validation = walidacja metody (parametrów i wyniku);
 *   status code = kod statusu HTTP; response body = ciało odpowiedzi; default message = domyślny komunikat.
 *
 * ZOBACZ TEŻ: s05_validation/Val01BeanValidation (adnotacje), s05_validation/Val03NestedAndGroups,
 *             s00_start/Start04TestingIntro (@WebMvcTest i MockMvc),
 *             s06_error_handling/Err02ExceptionHandler, s06_error_handling/Err04ProblemDetail (pełna obsługa błędów),
 *             s04_web_rest/Rest02RequestData (skąd Spring bierze dane z zapytania).
 * </pre>
 */
public final class Val02ValidInControllers {

    private Val02ValidInControllers() {
    }

    // =================================================================================================
    // 1. @Valid @RequestBody → 400 i MethodArgumentNotValidException
    // =================================================================================================
    //
    //   @PostMapping
    //   public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) { ... }
    //
    // Reguły siedzą w rekordzie ProductRequest (adnotacje z Val01BeanValidation). @Valid przy argumencie to WŁĄCZNIK:
    // bez niego Spring tylko zamieni JSON na obiekt i nic nie sprawdzi.
    // Wyjątek MethodArgumentNotValidException niesie BindingResult (wynik wiązania) z listą FieldError — błędów pól.
    //
    // PUŁAPKA: rekord ma adnotacje, ale w kontrolerze zapomniałeś o @Valid. Dlaczego to groźne? Nic się nie wywala,
    //   kontroler dostaje niepoprawne dane i spokojnie je przetwarza. Regułom nie wystarczy „istnieć” — ktoś musi
    //   je uruchomić. Sprawdzone: PlainController.unchecked (bez @Valid) przyjmuje pustą nazwę.
    //
    // DOBRA PRAKTYKA: dla DTO (obiektów przesyłanych w API) pisz test z błędnym ciałem i oczekuj 400.
    //   Dlaczego? Zapomniane @Valid jest niewidoczne gołym okiem, a test wychwyci je od razu.
    //
    // Testy: Val02ValidInControllersTest.bodyValidation.

    // =================================================================================================
    // 2. Czytelny JSON błędu zamiast pustego 400
    // =================================================================================================
    //
    // Domyślnie błąd 400 mówi klientowi niewiele. W ProductController jest metoda z @ExceptionHandler
    // (obsługa wyjątku), która z wyjątku wyciąga błędy pól i oddaje:
    //   {"error":"Niepoprawne dane","problems":[{"field":"code","message":"Kod ma postać ABC-1234"}, ...]}
    // Dzięki temu aplikacja frontendowa pokaże komunikat przy właściwym polu. Pola sortujemy, bo kolejność błędów
    // w BindingResult nie jest gwarantowana.
    //
    // DOBRA PRAKTYKA: używaj jednego, stałego formatu błędów w całym API. Dlaczego? Klient pisze jedną obsługę,
    //   a nie osobną dla każdego punktu końcowego. Gotowy standard (ProblemDetail) i wspólna obsługa dla wszystkich
    //   kontrolerów (@ControllerAdvice) — w dziale s06_error_handling.
    //
    // PUŁAPKA: do odpowiedzi trafia fe.getDefaultMessage() — komunikat z adnotacji. Dlaczego to ważne? Jeśli nie ustawisz
    //   własnego message, dostaniesz tekst w języku serwera („must not be blank”), a nie klienta.
    //
    // Testy: Val02ValidInControllersTest.cleanErrorJson.

    // =================================================================================================
    // 3. Ograniczenia na @RequestParam i @PathVariable — walidacja metod bez @Validated
    // =================================================================================================
    //
    //   @GetMapping
    //   public ... list(@RequestParam @Positive @Max(100) int limit, @RequestParam(required = false) @Size(min = 2) String search)
    //
    // W starych poradnikach (Spring 5, Boot 2) dla takich ograniczeń trzeba było dać @Validated NA KLASIE kontrolera;
    // walidował wtedy proxy AOP i rzucał ConstraintViolationException (zwykle kończące się 500!).
    // Od Frameworka 6.1 (a więc i w Springu 7 / Boot 4) wystarczą same adnotacje przy parametrach: Spring sam sprawdza je
    // przed wywołaniem metody i rzuca HandlerMethodValidationException, która domyślnie daje 400.
    // Sprawdzone testami (PlainController, ProductController) i uruchomieniem na prawdziwym serwerze.
    //
    // PUŁAPKA: kopiujesz ze starego tutoriala @Validated na klasę kontrolera. Dlaczego to pogarsza sprawę? Przy
    //   @Validated na klasie wbudowana walidacja się WYŁĄCZA, a robi ją proxy AOP, które rzuca
    //   ConstraintViolationException — bez własnej obsługi to 500 Internal Server Error (błąd serwera), czyli
    //   „winny jest serwer”, choć zawinił klient. Sprawdzone: ValidatedClassController w teście i na serwerze.
    //
    // DOBRA PRAKTYKA: w kontrolerach NIE dawaj @Validated na klasie. Dlaczego? Dostajesz 400 z domyślnej obsługi,
    //   a @Validated zostaw dla serwisów (lekcja Val05ServiceValidation).
    //
    // Testy: Val02ValidInControllersTest.parameterValidation, Val02ValidInControllersTest.validatedOnClass,
    //        Val02ValidInControllersServerTest.

    // =================================================================================================
    // 4. Ciało + ograniczenia parametrów w jednej metodzie — zmienia się wyjątek!
    // =================================================================================================
    //
    //   @PutMapping("/{id}")
    //   public ProductResponse update(@PathVariable @Min(1) long id, @Valid @RequestBody ProductRequest request)
    //
    // Gdy metoda ma ograniczenia na parametrach, CAŁA walidacja (także ciała z @Valid) idzie przez walidację metod.
    // Złe ciało → HandlerMethodValidationException, a NIE MethodArgumentNotValidException — nawet gdy id jest dobre!
    // Błędy ciała siedzą wtedy w ParameterErrors (ma getFieldErrors()), a błędy zwykłych parametrów w liście
    // komunikatów. ProductController.onParametersInvalid obsługuje oba przypadki.
    //
    // PUŁAPKA: masz @ExceptionHandler(MethodArgumentNotValidException.class) i dodajesz do metody @PathVariable @Min(1).
    //   Dlaczego Twoja obsługa przestaje działać dla ciała? Wyjątek zmienia typ na HandlerMethodValidationException,
    //   więc potrzebny jest drugi handler (albo jeden wspólny w @ControllerAdvice). Nazwą błędu ciała w ścieżce staje
    //   się też nazwa parametru („request”), jeśli czytasz ją z ParameterValidationResult zamiast z ParameterErrors.
    //
    // Testy: Val02ValidInControllersTest.bodyAndParameters.

    // =================================================================================================
    // 5. Co dzieje się PRZED walidacją
    // =================================================================================================
    //
    // Walidacja zaczyna się dopiero, gdy dane da się odczytać. Zanim do niej dojdzie, Spring może odrzucić zapytanie sam:
    //   limit=abc            → MethodArgumentTypeMismatchException (nie da się zamienić na int)      → 400
    //   JSON "{"             → HttpMessageNotReadableException (nie da się odczytać ciała)           → 400
    //   brak wymaganego limit → MissingServletRequestParameterException                              → 400
    // To nie są naruszenia ograniczeń — mają inne wyjątki, więc potrzebują osobnej obsługi (dział s06_error_handling).
    //
    // Testy: Val02ValidInControllersTest.beforeValidation.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Ciało: {@code @Valid @RequestBody Dto} → błędy = MethodArgumentNotValidException → 400, metoda się nie wykonuje.
     *   • Bez @Valid reguły z DTO są ignorowane — test z błędnym ciałem wychwytuje to od razu.
     *   • Parametry: @RequestParam/@PathVariable z ograniczeniami → HandlerMethodValidationException → 400
     *     (Framework 6.1+, BEZ @Validated na klasie).
     *   • @Validated na klasie kontrolera wyłącza to i daje ConstraintViolationException → 500. Nie rób tego.
     *   • Ciało + ograniczenia parametrów w jednej metodzie → cała walidacja to HandlerMethodValidationException;
     *     błędy ciała w ParameterErrors.
     *   • @ExceptionHandler zamienia wyjątek na JSON; jeden format błędów w całym API (więcej: s06_error_handling).
     *   • Zły typ, zepsuty JSON, brak parametru → osobne wyjątki, też 400, ale to nie walidacja.
     *
     * PYTANIA KONTROLNE:
     *   1. PYTANIE REKRUTACYJNE: co robi @Valid przy @RequestBody i co się dzieje, gdy go zabraknie?
     *   2. Co się stanie? Wysyłasz POST /plain/unchecked z ciałem {"name":"","code":"x","priceGrosze":0} (bez @Valid).
     *   3. Co się stanie? GET /plain/items?limit=0 przy {@code @RequestParam @Positive int limit} i BEZ @Validated na klasie.
     *   4. Co się stanie? To samo zapytanie do kontrolera z @Validated na klasie (ValidatedClassController) —
     *      jaki wyjątek i jaki status na prawdziwym serwerze?
     *   5. ZNAJDŹ BŁĄD: metoda ma {@code @PathVariable @Min(1) long id, @Valid @RequestBody Dto dto}, a jedyny
     *      handler to @ExceptionHandler(MethodArgumentNotValidException.class). Wysyłasz zły dto — czy handler zadziała?
     *   6. Co się stanie? GET /api/products?limit=abc.
     *   7. Dlaczego pola błędów w JSON-ie sortujemy przed oddaniem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Val02ValidInControllersExercisesTest
    // =================================================================================================

    /*
     * ĆWICZENIE 1 (łatwe): w ExerciseController.createOrder dopisz brakującą adnotację, żeby ciało zamówienia
     *   było sprawdzane. POST /exercise/orders z {"customer":"","quantity":0} ma dawać 400, a z {"customer":"Ala","quantity":2} — 200.
     *   Podpowiedź: reguły są w rekordzie OrderRequest, brakuje włącznika przy argumencie.
     *
     * ĆWICZENIE 2 (średnie): w ExerciseController.findOrder dodaj ograniczenia: id co najmniej 1, quantity od 1 do 99.
     *   GET /exercise/orders/0?quantity=5 i GET /exercise/orders/1?quantity=100 mają dawać 400.
     *   Podpowiedź: {@code @Min}, {@code @Max} przy parametrach; NIE dodawaj @Validated na klasie (sekcja 3).
     *
     * ĆWICZENIE 3 (trudniejsze): w ExerciseController dopisz metodę z @ExceptionHandler(MethodArgumentNotValidException.class),
     *   która dla POST /exercise/customers ze złym ciałem zwraca 400 i JSON {"errors": {"pole": "komunikat", ...}}.
     *   Podpowiedź: e.getBindingResult().getFieldErrors(), zbierz do mapy pole → komunikat (Collectors.toMap z funkcją
     *   łączącą duplikaty, bo jedno pole może mieć kilka błędów — zachowaj pierwszy).
     */

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @Valid włącza Bean Validation na obiekcie z ciała żądania: błędy → MethodArgumentNotValidException, 400,
     *      metoda kontrolera się nie wykonuje. Bez @Valid reguły są ignorowane, a niepoprawne dane przechodzą dalej.
     *   2. Odpowiedź 200 — bez @Valid nic nie jest sprawdzane. Sprawdzone testem Val02ValidInControllersTest.bodyValidation.
     *   3. 400, wyjątek HandlerMethodValidationException (Framework 6.1+, wbudowana walidacja metod).
     *      Sprawdzone testem Val02ValidInControllersTest.parameterValidation.
     *   4. Wyjątek ConstraintViolationException (z proxy AOP); w MockMvc wypływa jako ServletException z tą przyczyną,
     *      a na prawdziwym serwerze daje 500. Sprawdzone testami Val02ValidInControllersTest.validatedOnClass
     *      i Val02ValidInControllersServerTest.
     *   5. Nie — przy ograniczeniach na parametrach wyjątek to HandlerMethodValidationException, więc handler dla
     *      MethodArgumentNotValidException nie zadziała. Sprawdzone testem Val02ValidInControllersTest.bodyAndParameters.
     *   6. 400, ale wyjątek to MethodArgumentTypeMismatchException (nie da się zamienić „abc” na int) — walidacja
     *      w ogóle nie startuje. Sprawdzone testem Val02ValidInControllersTest.beforeValidation.
     *   7. Zbiór błędów w BindingResult nie ma gwarantowanej kolejności, więc bez sortowania odpowiedź (i test) mogłaby
     *      raz wyglądać tak, raz inaczej.
     */
    // </editor-fold>
}

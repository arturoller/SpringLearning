package s05_validation.val02_valid_in_controllers;

/**
 * <pre>
 * TEMAT: @Valid w kontrolerach — walidacja ciała, parametrów ścieżki i zapytania w Spring MVC
 *        (controller = kontroler; request body = ciało żądania; path variable = zmienna ścieżki, część adresu;
 *         request parameter = parametr zapytania, to co po znaku ? w adresie; binding = wiązanie, przypisanie danych
 *         z żądania do obiektu; exception = wyjątek; handler = obsługa, procedura obsługi)
 *
 * W SKRÓCIE:
 *   Adnotacje z Val01BeanValidation same nic nie robią — ktoś musi wywołać Validator. W kontrolerze robi to Spring MVC,
 *   gdy przy parametrze stoi {@code @Valid}. Błędne dane = wyjątek = odpowiedź 400, a metoda kontrolera w ogóle się
 *   NIE wykonuje. Bez {@code @Valid} adnotacje w DTO są martwe. Ograniczenia na parametrach ścieżki i zapytania
 *   ({@code @PathVariable @Min(1) long id}) działają od Springa 6.1 bez dodatkowej konfiguracji.
 *
 * ANALOGIA: recepcja w biurowcu.
 *   Gość (żądanie) podchodzi do recepcji. Recepcjonistka najpierw sprawdza, czy formularz da się w ogóle odczytać (czy to
 *   poprawny JSON), potem czy jest wypełniony poprawnie (walidacja). Jeśli nie — odsyła gościa od razu ("niepoprawny
 *   formularz") i NIE dzwoni do żadnego pracownika na piętrze (serwis). Pracownicy zajmują się tylko sprawami, które
 *   przeszły recepcję. Napis "Sprawdzamy formularze" na drzwiach to {@code @Valid} — bez niego recepcjonistka przepuszcza
 *   każdego.
 *
 * JAK TO DZIAŁA:
 *   1. Klient wysyła POST z JSON-em. Spring wybiera metodę kontrolera (Rest01Controllers) i buduje jej argumenty.
 *   2. Argument z {@code @RequestBody} tworzy {@code RequestResponseBodyMethodProcessor}: zamienia JSON na DTO
 *      (Jackson — Rest04Json). Jeśli JSON jest zepsuty, leci {@code HttpMessageNotReadableException} (400) i walidacja
 *      w ogóle nie startuje.
 *   3. Gdy przy parametrze stoi {@code @Valid} (albo {@code @Validated}), ten sam procesor woła Validator (bean z
 *      Val01BeanValidation, sekcja 4) na gotowym DTO.
 *   4. Są naruszenia? Spring zbiera je w {@code BindingResult} i — jeśli kontroler go nie zażądał — rzuca
 *      {@code MethodArgumentNotValidException}. Metoda kontrolera NIE zostaje wywołana.
 *   5. {@code DefaultHandlerExceptionResolver} zamienia wyjątek na status 400. Treść odpowiedzi zależy od tego, kto ją
 *      jeszcze uzupełni (sekcja 2): w {@code @WebMvcTest} jest pusta, na prawdziwym serwerze Boot dokłada ogólny JSON.
 *   6. Ograniczenia na PARAMETRACH metody (nie na DTO) obsługuje wbudowana walidacja metod (method validation) i rzuca
 *      {@code HandlerMethodValidationException} — też 400 (sekcja 4).
 *
 * SŁÓWKA:
 *   field = pole; rejected value = odrzucona wartość; code = kod (nazwa złamanego ograniczenia); default message =
 *   domyślny komunikat; sorted = posortowany; deterministic = deterministyczny (zawsze taki sam wynik); lenient =
 *   pobłażliwy; strict = ścisły; legacy = spuścizna, "stary styl"; proxy = pośrednik.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest02RequestData (@RequestBody, @PathVariable, @RequestParam),
 *             s04_web_rest/Rest04Json (zepsuty JSON, DTO, Jackson 3),
 *             s04_web_rest/Rest05CrudApi (ręczne isValid(), które tu zastępujemy adnotacjami),
 *             s05_validation/Val01BeanValidation (adnotacje i Validator),
 *             s05_validation/Val03NestedAndGroups (@Valid na zagnieżdżonych obiektach, grupy),
 *             s05_validation/Val05ServiceValidation (@Validated na klasie i proxy),
 *             s06_error_handling/Err02ExceptionHandler (@ExceptionHandler),
 *             s06_error_handling/Err03ControllerAdvice (jedno miejsce na obsługę błędów),
 *             s06_error_handling/Err04ProblemDetail (standardowy format błędu),
 *             s11_testing/Test02MockitoBeans (@MockitoBean w szczegółach).
 * </pre>
 */
public final class Val02ValidInControllers {

    private Val02ValidInControllers() {
    }

    // =================================================================================================
    // 1. @Valid @RequestBody: BŁĘDNE DANE = 400, METODA SIĘ NIE WYKONUJE (ProductController.create)
    // =================================================================================================
    //
    // PRZED (ręcznie, jak w Rest05CrudApi):
    //     public ResponseEntity<…> create(@RequestBody ProductRequest request) {
    //         if (request.name() == null || request.name().isBlank() || request.price() == null || …) {
    //             return ResponseEntity.badRequest().build();
    //         }
    //
    // PO (deklaratywnie):
    //     @PostMapping
    //     public Product create(@Valid @RequestBody ProductRequest request) { return service.create(request); }
    //
    // Reguły siedzą w DTO (ProductRequest: @NotBlank, @DecimalMin, @Min…), kontroler ma jedną linijkę. Test
    // Val02ValidInControllersTest (@WebMvcTest + @MockitoBean serwisu):
    //   • poprawne dane → 201, a serwis dostaje DTO (verify);
    //   • błędne dane → 400, wyjątek {@code MethodArgumentNotValidException} i {@code verifyNoInteractions(service)} —
    //     DOWÓD, że do logiki nie dotarło nic;
    //   • pusty obiekt {@code {}} → trzy błędy naraz (nazwa, cena, ilość): Spring zbiera WSZYSTKIE, nie tylko pierwszy.
    //
    // Można użyć {@code @Valid} (jakarta.validation — to ze specyfikacji) albo {@code @Validated} (org.springframework —
    // adnotacja Springa). Przy {@code @RequestBody} działają tak samo; różnica to możliwość podania grup (Val03NestedAndGroups)
    // i użycie na klasie (Val05ServiceValidation).
    //
    // PYTANIE REKRUTACYJNE: czym różni się {@code @Valid} od {@code @Validated}? ({@code @Valid} to adnotacja Jakarta Bean
    //   Validation: włącza walidację i "schodzi" do zagnieżdżonych obiektów, ale nie przyjmuje grup. {@code @Validated} to
    //   adnotacja Springa: przyjmuje grupy walidacji, a postawiona na klasie włącza walidację metod przez proxy.)
    //
    // PUŁAPKA: brak {@code @Valid} przy {@code @RequestBody}. Dlaczego? Adnotacje w DTO to tylko metadane — nikt ich nie
    //   czyta, dopóki ktoś nie wywoła Validatora. Kompilator nie ostrzeże, test "na szczęśliwej ścieżce" przejdzie, a w
    //   produkcji zapisujesz produkt z pustą nazwą i ceną 0. Endpoint {@code POST /val02/products/unchecked} robi to na
    //   złość: ten sam zły JSON daje 201, a serwis dostaje {@code ProductRequest("", 0, -1)} (sprawdzone testem).
    //
    // DOBRA PRAKTYKA: dla każdej metody z {@code @RequestBody} napisz test "zły JSON → 400 i verifyNoInteractions".
    //   Dlaczego? To jedyny sposób, żeby wyłapać zapomniane {@code @Valid} — brak adnotacji nie zostawia żadnego śladu.

    // =================================================================================================
    // 2. CO ZAWIERA MethodArgumentNotValidException I CO WIDZI KLIENT
    // =================================================================================================
    //
    // Wyjątek niesie {@code BindingResult} ({@code exception.getBindingResult()}). Z niego:
    //   • {@code getObjectName()}      — nazwa obiektu: "productRequest" (nazwa klasy z małej litery);
    //   • {@code getFieldErrors()}     — lista {@code FieldError}, po jednym na każde naruszenie;
    //   • {@code FieldError.getField()}           — pole: "name", "price", "quantity";
    //   • {@code FieldError.getCode()}            — kod = nazwa ograniczenia: "NotBlank", "DecimalMin", "Min";
    //   • {@code FieldError.getRejectedValue()}   — co klient wysłał: "", 0, -1;
    //   • {@code FieldError.getDefaultMessage()}  — komunikat: tu własne, polskie z ProductRequest.
    // (Wszystko sprawdza test exceptionContents.) Zwróć uwagę: to inne obiekty niż {@code ConstraintViolation} z
    // Val01BeanValidation — Spring tłumaczy naruszenia Bean Validation na swój model błędów ({@code FieldError}).
    //
    // CO WIDZI KLIENT? Zależy od otoczenia:
    //   • @WebMvcTest/MockMvc: status 400 i PUSTE ciało (test defaultBodyIsEmptyInMockMvc).
    //   • Prawdziwy serwer (Val02ServerTest): 400 i ogólny JSON Boota {@code {"timestamp":…,"status":400,"error":"Bad Request",
    //     "path":"/val02/products"}} — wygenerowany przez kontroler błędów {@code /error}. Klient NIE dowiaduje się, które pole
    //     jest złe! Domyślnie Boot nie ujawnia szczegółów (nie chce wyciekać informacji).
    //
    // Dlatego prawie każde API dodaje własną obsługę. Najprostsza wersja to {@code @ExceptionHandler} w kontrolerze
    // (FriendlyProductController): mapa pole → komunikat, posortowana (TreeMap — inaczej kolejność kluczy byłaby
    // przypadkowa, a test migotałby):
    //
    //     @ExceptionHandler(MethodArgumentNotValidException.class)
    //     @ResponseStatus(HttpStatus.BAD_REQUEST)
    //     public Map<String, String> onInvalidBody(MethodArgumentNotValidException exception) { … }
    //
    // Odpowiedź: {@code {"name":"Nazwa jest wymagana","price":"Cena musi wynosić co najmniej 0.01","quantity":"Ilość nie
    // może być ujemna"}} (test Val02FriendlyTest i Val02ServerTest).
    //
    // UWAGA: to tylko ZAPOWIEDŹ. Handler w jednym kontrolerze działa tylko dla tego kontrolera. Jedno miejsce dla całej
    // aplikacji ({@code @RestControllerAdvice}) i standardowy format {@code ProblemDetail} (RFC 9457) to dział
    // s06_error_handling: Err02ExceptionHandler, Err03ControllerAdvice, Err04ProblemDetail.
    //
    // DOBRA PRAKTYKA: odpowiedź o błędzie walidacji musi mówić klientowi, KTÓRE pole jest złe i DLACZEGO (kod lub komunikat).
    //   Dlaczego? Bez tego programista klienta (albo użytkownik formularza) zgaduje, co poprawić. Zawsze sortuj pola —
    //   dzięki temu odpowiedź jest powtarzalna i da się ją sensownie testować.

    // =================================================================================================
    // 3. BindingResult: KONTROLER SAM DECYDUJE (ProductController.createManual)
    // =================================================================================================
    //
    // Jeśli zaraz PO parametrze z {@code @Valid} dodasz parametr {@code BindingResult}, Spring nie rzuca wyjątku — wkłada
    // do niego błędy i wywołuje Twoją metodę:
    //
    //     public ResponseEntity<?> createManual(@Valid @RequestBody ProductRequest request, BindingResult errors) {
    //         if (errors.hasErrors()) { return ResponseEntity.badRequest().body(…); }
    //
    // Test bindingResultKeepsControlInTheController: odpowiedź 400 z listą {@code ["name:NotBlank","price:DecimalMin",
    // "quantity:Min"]}, brak wyjątku ({@code getResolvedException()} jest null), serwis nietknięty.
    //
    // Kolejność parametrów jest ważna: BindingResult MUSI stać bezpośrednio po obiekcie, którego dotyczy. Zapomnisz
    // i wstawisz między nie inny parametr — Spring nie wie, czyje to błędy, i rzuca wyjątek o braku powiązania.
    //
    // Kiedy to ma sens? Rzadko. W API REST prawie zawsze chcesz tego samego dla każdej metody, a to robi jeden
    // {@code @ExceptionHandler}. {@code BindingResult} bywa przydatny w aplikacjach z formularzami HTML (wracasz do tego
    // samego widoku z komunikatami) i gdy przy błędach część pracy MUSI się jednak wykonać (np. zapis do logu audytu).
    //
    // PUŁAPKA: dodanie {@code BindingResult} i niesprawdzenie go. Dlaczego? Spring przestaje rzucać wyjątek, więc błędne
    //   dane przechodzą dalej tak samo, jak bez {@code @Valid}. Czyli dostajesz dokładnie tę pułapkę z sekcji 1, tylko
    //   trudniejszą do zauważenia.

    // =================================================================================================
    // 4. WALIDACJA PARAMETRÓW: @PathVariable, @RequestParam (HandlerMethodValidationException)
    // =================================================================================================
    //
    //     @GetMapping("/{id}")  find(@PathVariable @Min(1) long id)
    //     @GetMapping           search(@RequestParam(required = false) @Size(max = 10) String q)
    //
    // Od Springa 6.1 (czyli w Springu 7 / Boot 4 jest to norma) kontroler z ograniczeniami na parametrach jest walidowany
    // WBUDOWANĄ walidacją metod (method validation): Spring sam sprawdza argumenty przed wywołaniem metody. Co ważne:
    //   • NIE trzeba dawać {@code @Validated} na klasie kontrolera;
    //   • błąd to {@code org.springframework.web.method.annotation.HandlerMethodValidationException} → 400
    //     (test pathVariableConstraint, requestParamConstraint);
    //   • wyjątek zawiera listę {@code ParameterValidationResult}: dla każdego sprawdzonego parametru jego nazwę
    //     ({@code getMethodParameter().getParameterName()}), wartość ({@code getArgument()}) i błędy
    //     ({@code getResolvableErrors()}); dla ciała z {@code @Valid} wynikiem jest {@code ParameterErrors}.
    //
    // PRZED (stare tutoriale, sprzed Springa 6.1): na klasie kontrolera trzeba było dać {@code @Validated}. Klasa
    // dostawała proxy (Val05ServiceValidation), a błąd to {@code jakarta.validation.ConstraintViolationException}, którego
    // Spring nie zamieniał na 400, tylko pozostawiał jako 500! Sprawdzone: LegacyValidatedController — test
    // classLevelValidatedThrowsConstraintViolation oraz Val02ServerTest (500 na prawdziwym serwerze).
    //
    // PUŁAPKA: (zmiana, która zaskakuje) jeśli metoda ma CHOĆ JEDEN parametr z ograniczeniem (np. {@code @Min} przy id),
    //   to także błąd ciała z {@code @Valid} zmienia typ wyjątku: zamiast {@code MethodArgumentNotValidException} dostajesz
    //   {@code HandlerMethodValidationException} (metoda {@code replace} w ProductController; test
    //   bodyErrorTurnsIntoMethodValidationException). Dlaczego to ważne? Twój {@code @ExceptionHandler} dla
    //   {@code MethodArgumentNotValidException} NIE złapie tych przypadków i klient dostanie ogólną odpowiedź. Rozwiązanie: handler
    //   dla OBU wyjątków (FriendlyProductController ma oba) albo wspólna obsługa w s06.
    //
    // DOBRA PRAKTYKA: ograniczaj długość tekstowych parametrów zapytania ({@code @Size(max = …)}). Dlaczego? Parametr
    //   {@code q} trafi do wyszukiwarki, do logów, a potem być może do zapytania SQL — krótki limit to tania ochrona przed
    //   nadużyciami i przypadkowym wklejeniem całej strony do pola szukania.

    // =================================================================================================
    // 5. KOLEJNOŚĆ: NAJPIERW JSON, POTEM WALIDACJA (test sekcji 5)
    // =================================================================================================
    //
    //   zapytanie ─► [1] Jackson czyta JSON ─► [2] Validator sprawdza DTO ─► [3] metoda kontrolera ─► [4] serwis
    //                 │ błąd składni/typu         │ błąd reguł                 │
    //                 ▼                          ▼                            ▼
    //     HttpMessageNotReadableException   MethodArgumentNotValidException   normalny wynik
    //                 400                           400                         2xx
    //
    // Walidator dostaje obiekt dopiero po udanej zamianie JSON-a na DTO. Zepsuty JSON ({@code {"name":}) albo wartość złego typu
    // (cena "abc") kończy się na etapie 1 jako {@code HttpMessageNotReadableException}; adnotacje nie mają nic do powiedzenia
    // (testy brokenJsonFailsBeforeValidation i wrongTypeFailsBeforeValidation). Obie sytuacje to 400, ale dwa różne
    // wyjątki — jeśli piszesz własną obsługę błędów, obsłuż oba (s06).
    //
    // Dodatkowo: pole w ogóle nieobecne w JSON-ie daje w DTO wartość null (dla typów opakowanych) i dopiero walidator ją
    // wyłapie (test emptyObjectViolatesAllRequiredFields). Prymitywy (int, long) w Jackson 3 odrzucają brakujące pole już
    // w etapie 1 — dlatego w DTO używamy BigDecimal i Integer, żeby błąd miał formę "pole: komunikat" z walidatora
    // (Rest04Json).
    //
    // PUŁAPKA: oczekiwanie, że {@code @NotNull} wyłapie zły TYP. Dlaczego? Typ sprawdza deserializacja, a nie walidacja.
    //   Walidator widzi tylko obiekt, który się udało zbudować.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code @Valid @RequestBody Dto dto} → błędne dane = {@code MethodArgumentNotValidException} → 400; metoda się nie wykonuje.
     *   • Bez {@code @Valid} adnotacje w DTO są ignorowane (nic nie ostrzega!). Test: zły JSON → 400 + verifyNoInteractions.
     *   • Wyjątek niesie BindingResult → FieldError: pole, kod, odrzucona wartość, komunikat. Domyślnie klient widzi
     *     pusty (MockMvc) lub ogólny JSON Boota (serwer) — bez listy pól.
     *   • Prosta obsługa: {@code @ExceptionHandler} + TreeMap pole → komunikat. Docelowo: @RestControllerAdvice, ProblemDetail (s06).
     *   • BindingResult tuż po {@code @Valid} → brak wyjątku, kontroler decyduje; nigdy nie ignoruj go (pułapka).
     *   • {@code @PathVariable @Min(1) long id}, {@code @RequestParam @Size(max=10) String q} → HandlerMethodValidationException
     *     → 400; od Springa 6.1 NIE trzeba {@code @Validated} na klasie.
     *   • Metoda z ograniczeniem na parametrze: nawet błąd ciała to HandlerMethodValidationException.
     *   • {@code @Validated} na klasie (stary styl) → ConstraintViolationException → 500.
     *   • Kolejność: Jackson (400 przy złym JSON) → walidacja (400) → kontroler.
     *
     * PYTANIA KONTROLNE:
     *   1. Co robi {@code @Valid} przy {@code @RequestBody}? Co się dzieje z metodą kontrolera, gdy dane są błędne?
     *   2. Co się stanie? Klient wysyła {@code {"name":"","price":0,"quantity":-1}} na endpoint, którego parametr to
     *      {@code @RequestBody ProductRequest request} (bez {@code @Valid}). Jaki status i co dostaje serwis?
     *   3. Co się stanie? Wysyłasz ten sam JSON na endpoint z {@code @Valid}. Ile błędów zawiera wyjątek i jaki status?
     *   4. Co się stanie? W kontrolerze jest {@code @Valid @RequestBody Dto dto, BindingResult errors}, a metoda nie
     *      sprawdza {@code errors}. Co się dzieje przy błędnych danych?
     *   5. Jaki wyjątek leci przy {@code @PathVariable @Min(1) long id} i id = 0? Czy trzeba dodać {@code @Validated}
     *      na klasie kontrolera?
     *   6. ZNAJDŹ BŁĄD: {@code @ExceptionHandler(MethodArgumentNotValidException.class)} istnieje, ale PUT z {@code @Min}
     *      przy {@code @PathVariable} i błędnym ciałem nadal zwraca ogólną odpowiedź 400. Dlaczego?
     *   7. ZNAJDŹ BŁĄD: w kontrolerze {@code @Validated} jest na klasie i {@code @Min(1)} na {@code @PathVariable}. Zapytanie
     *      z id = 0 zwraca 500. Dlaczego i jak to naprawić?
     *   8. Co się stanie? Klient wysyła {@code {"name":}} (zepsuty JSON) na endpoint z {@code @Valid}. Który wyjątek
     *      poleci i czy walidator zdąży zadziałać?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, testy: Val02ValidInControllersExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Budujesz API klientów (/val02/cw/customers) na GOTOWYM serwisie Customers (find, add, update). Mapowania są gotowe.
    // Rozwiązania wzorcowe: Val02SolutionsController (s05_validation.solutions.val02_valid_in_controllers).
    //
    // ĆWICZENIE 1 (łatwe): create — dodaj ograniczenia do rekordu CustomerRequest (name: @NotBlank; email: @NotBlank
    //   @Email), przy parametrze dodaj {@code @Valid}, dodaj klienta (customers.add) i odpowiedz 201 z klientem w ciele.
    //   Błędne dane mają dać 400 i nie dodać klienta. Podpowiedź: ResponseEntity.status(HttpStatus.CREATED).body(…).
    //
    // ĆWICZENIE 2 (łatwe): get — 200 z klientem albo 404; id mniejsze niż 1 → 400 (zanim ruszy serwis).
    //   Podpowiedź: ograniczenie {@code @Min(1)} przy parametrze {@code @PathVariable}; ResponseEntity.of(optional).
    //
    // ĆWICZENIE 3 (średnie): createManual — dodaj {@code BindingResult} po parametrze z {@code @Valid}. Przy błędach 400
    //   i lista tekstów "pole:Kod" posortowana alfabetycznie; inaczej 201 z nowym klientem.
    //   Podpowiedź: errors.getFieldErrors(), error.getField() i error.getCode(); sorted().
    //
    // ĆWICZENIE 4 (średnie): messagesByField — zamień BindingResult na mapę pole → komunikat (TreeMap, posortowana po polu);
    //   gdy pole ma kilka błędów, połącz posortowane komunikaty tekstem "; ".
    //   Podpowiedź: Collectors.toMap z funkcją łączącą i TreeMap::new.
    //
    // ĆWICZENIE 5 (trudniejsze): replace — PUT: 200 z klientem albo 404; id < 1 → 400; błędne ciało → 400; w obu błędach
    //   serwis nie może zostać wywołany. Podpowiedź: ograniczenie przy id ORAZ {@code @Valid} przy ciele (zobacz pułapkę z
    //   sekcji 4: jaki wyjątek poleci?).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Włącza walidację DTO po zamianie JSON-a na obiekt. Przy błędnych danych Spring rzuca
     *      MethodArgumentNotValidException (→ 400) i metoda kontrolera w ogóle się nie wykonuje — serwis nie jest
     *      wołany (verifyNoInteractions w teście invalidBodyIs400AndMethodDoesNotRun).
     *   2. Status 201 (!): adnotacje w DTO są ignorowane, a serwis dostaje {@code ProductRequest("", 0, -1)}. Sprawdzone
     *      testem withoutValidAnnotationsAreIgnored (endpoint /unchecked).
     *   3. Trzy błędy (name: NotBlank, price: DecimalMin, quantity: Min), status 400. Spring zbiera wszystkie naruszenia,
     *      nie tylko pierwsze. Sprawdzone w teście exceptionContents.
     *   4. Spring nie rzuca wyjątku, więc metoda wykona się z błędnymi danymi — jak bez {@code @Valid}. To pułapka:
     *      {@code BindingResult} trzeba zawsze sprawdzić ({@code errors.hasErrors()}).
     *   5. {@code HandlerMethodValidationException} (400). Nie, {@code @Validated} na klasie nie jest potrzebne od Springa
     *      6.1 — wbudowana walidacja metod włącza się sama, gdy parametr ma ograniczenie. Test pathVariableConstraint.
     *   6. Bo metoda ma ograniczenie na parametrze, więc cała walidacja idzie drogą method validation i błąd ciała
     *      też jest {@code HandlerMethodValidationException}, a nie {@code MethodArgumentNotValidException}. Dodaj handler
     *      dla drugiego wyjątku (test bodyErrorTurnsIntoMethodValidationException).
     *   7. {@code @Validated} na klasie włącza starą drogę przez proxy (AOP), która rzuca ConstraintViolationException;
     *      Spring MVC nie zamienia jej na 400, więc jest 500 (testy Val02LegacyValidatedTest i Val02ServerTest).
     *      Naprawa: usuń {@code @Validated} z klasy kontrolera (od Springa 6.1 niepotrzebne) albo obsłuż
     *      ConstraintViolationException w @ExceptionHandler (s06).
     *   8. {@code HttpMessageNotReadableException} (400) z etapu czytania JSON-a. Walidator nie zdąży zadziałać — nie
     *      dostaje żadnego obiektu. Test brokenJsonFailsBeforeValidation.
     */
    // </editor-fold>
}

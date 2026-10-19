package s05_validation.val02_valid_in_controllers;

import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

/**
 * <pre>
 * TEMAT: Walidacja w kontrolerach Spring MVC — {@code @Valid @RequestBody}, ograniczenia na parametrach i kod 400
 *        (controller = kontroler, klasa obsługująca zapytania HTTP; request body = ciało zapytania;
 *        request parameter = parametr zapytania; path variable = zmienna ze ścieżki)
 *
 * W SKRÓCIE:
 *   Same adnotacje na rekordzie ({@code @NotBlank} i spółka) niczego nie sprawdzają — ktoś musi uruchomić walidator.
 *   W kontrolerze robi to Spring MVC, ale tylko tam, gdzie go o to poprosisz: {@code @Valid} przy {@code @RequestBody}
 *   albo ograniczenia wprost przy parametrach ({@code @RequestParam @Min(1) int limit}). Błąd walidacji kończy się
 *   wyjątkiem, zanim wykona się choćby jedna linia metody kontrolera, a Spring zamienia go na odpowiedź 400.
 *
 * ANALOGIA: bramka z ochroniarzem przy wejściu na halę targową.
 *   Hala (metoda kontrolera) obsługuje tylko gości z ważnym biletem. Ochroniarz (walidacja) stoi przy bramce — ale tylko
 *   przy tych bramkach, przy których ktoś mu kazał stanąć ({@code @Valid}). Bramka bez ochroniarza przepuści każdego,
 *   nawet jeśli bilet jest podrobiony. Odrzucony gość nigdy nie wchodzi do środka.
 *
 * JAK TO DZIAŁA:
 *   Zapytanie → DispatcherServlet → handler (metoda kontrolera) → ZANIM metoda się wykona:
 *     1) konwersja: JSON → rekord (HttpMessageConverter / Jackson), napis "5" → int     błąd → 400
 *        HttpMessageNotReadableException (zły JSON), MethodArgumentTypeMismatchException (zły typ)
 *     2) walidacja: Validator sprawdza ograniczenia                                      błąd → 400
 *        MethodArgumentNotValidException albo HandlerMethodValidationException
 *     3) dopiero teraz wywołanie metody
 *   Wyjątek łapie DefaultHandlerExceptionResolver i zamienia na status 400. W logu (poziom WARN, "Resolved [...]") zostaje
 *   ślad dla MethodArgumentNotValidException, HttpMessageNotReadableException i MethodArgumentTypeMismatchException —
 *   ale NIE dla HandlerMethodValidationException (sprawdzone uruchomieniem: ten wyjątek nie zostawia wpisu w logu przy domyślnych ustawieniach).
 *
 *   Który wyjątek? (sprawdzone testami)
 *     {@code @Valid @RequestBody Rekord} jako JEDYNE ograniczenie w metodzie  → MethodArgumentNotValidException
 *     {@code @Valid Rekord} z parametrów adresu (jak @ModelAttribute)           → MethodArgumentNotValidException
 *     ograniczenie na {@code @RequestParam}/{@code @PathVariable}              → HandlerMethodValidationException
 *     {@code @Valid @RequestBody} i DODATKOWO ograniczenie na innym parametrze → HandlerMethodValidationException (oba!)
 *     {@code @Validated} na klasie kontrolera + ograniczenie na parametrze     → ConstraintViolationException (500!)
 *
 * SŁÓWKA:
 *   valid = ważny, poprawny; validated = zwalidowany; binding result = wynik wiązania (zbiór błędów po wiązaniu danych);
 *   field error = błąd pola; bad request = złe zapytanie (400); handler method = metoda obsługująca zapytanie;
 *   exception resolver = rozwiązywacz wyjątków (zamienia wyjątek na odpowiedź); problem details = szczegóły problemu
 *   (standardowy format błędu RFC 9457, application/problem+json); echo = odpowiedź odsyłająca to, co przyszło.
 *
 * ZOBACZ TEŻ: s05_validation/Val01BeanValidation (same adnotacje i Validator), s05_validation/Val05ServiceValidation
 *             (walidacja w serwisie — tam @Validated na klasie jest właściwy), s06_error_handling/Err03ControllerAdvice
 *             (czytelne komunikaty błędów zamiast pustego 400), s06_error_handling/Err04ProblemDetail (format RFC 9457),
 *             s01_core_ioc/Ioc03Stereotypes (kontroler jako bean).
 * </pre>
 */
public final class Val02ValidInControllers {

    private Val02ValidInControllers() {
    }

    // =================================================================================================
    // 1. @Valid @RequestBody → 400 i MethodArgumentNotValidException (ProductController.create)
    // =================================================================================================
    //
    //   @PostMapping
    //   public ResponseEntity<...> create(@Valid @RequestBody NewProductRequest request) { ... }
    //
    // Jackson zamienia JSON na rekord, potem @Valid uruchamia walidator. Gdy są naruszenia, Spring rzuca
    // MethodArgumentNotValidException (nie wywołuje metody) i odpowiada 400. W wyjątku siedzi BindingResult
    // (wynik wiązania) z listą błędów pól: ex.getBindingResult().getFieldErrors() — tam jest nazwa pola i komunikat.
    // Błędy zbierane są WSZYSTKIE naraz (name, sku i price w jednej odpowiedzi), nie po jednym.
    //
    // Co z JSON-em, który w ogóle nie jest JSON-em? To inny wyjątek (HttpMessageNotReadableException) — też 400,
    // ale do walidacji w ogóle nie dochodzi: najpierw trzeba odczytać dane.
    //
    // Test: section1_validBody.

    // =================================================================================================
    // 2. PUŁAPKA: brak @Valid = brak walidacji (ProductController.unchecked)
    // =================================================================================================
    //
    // PUŁAPKA: rekord z adnotacjami (@NotBlank, @Positive...), ale w kontrolerze @RequestBody
    //   bez @Valid. Dlaczego to błąd, który łatwo przeoczyć? Kod się kompiluje, aplikacja działa, a ograniczenia
    //   są tylko ozdobą — do metody wchodzą dane z pustą nazwą i ujemną ceną. Nie ma żadnego ostrzeżenia.
    //   Zawsze sprawdź, czy przy parametrze z ciałem stoi @Valid (albo @Validated).
    //
    // Test: section2_missingValid.

    // =================================================================================================
    // 3. OGRANICZENIA NA PARAMETRACH: @RequestParam, @PathVariable (od Spring Framework 6.1)
    // =================================================================================================
    //
    //   public Map list(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit)
    //   public Map one(@PathVariable @Pattern(regexp = "[A-Z]{3}-[0-9]{3}") String sku)
    //
    // Od Spring Framework 6.1 Spring MVC sam waliduje takie parametry — bez @Validated na klasie. Wystarczy, że
    // przy parametrze (albo przy metodzie) stoi adnotacja ograniczenia. Błąd → HandlerMethodValidationException → 400.
    // W wyjątku: getParameterValidationResults() — dla każdego parametru nazwa, wartość i komunikaty.
    // (W starszych tutorialach: @Validated na klasie kontrolera i ConstraintViolationException — patrz sekcja 5.)
    //
    // Walidacja to nie konwersja: ?limit=abc dla parametru int kończy się INNYM wyjątkiem —
    // MethodArgumentTypeMismatchException (nie da się zamienić "abc" na liczbę) — też 400, ale walidator nie startuje.
    // Zero i 500 to już liczby, więc złamią @Min(1) / @Max(100) i dadzą HandlerMethodValidationException.
    //
    // Test: section3_parameterConstraints.

    // =================================================================================================
    // 4. KTÓRY WYJĄTEK? @Valid @RequestBody + ograniczenie na innym parametrze (ProductController.copies)
    // =================================================================================================
    //
    // Gdy metoda ma TYLKO @Valid @RequestBody, błąd to MethodArgumentNotValidException. Ale gdy w tej samej
    // metodzie jest też ograniczenie na innym parametrze (@RequestParam @Min(1) int copies), Spring włącza
    // walidację całej metody i wtedy NAWET błąd w ciele zgłasza HandlerMethodValidationException (z wynikiem
    // typu ParameterErrors dla ciała). Sprawdzone uruchomieniem — ciało złe, parametr dobry → HandlerMethodValidationException.
    //
    // PUŁAPKA: napisać @ExceptionHandler(MethodArgumentNotValidException.class), a potem dodać do metody
    //   parametr z ograniczeniem i zdziwić się, że handler przestał działać. Dlaczego? Ten sam błąd w ciele
    //   zmienił typ wyjątku. Dlatego w obsłudze błędów (dział s06) łapie się OBA wyjątki.
    //
    // Test: section4_bodyAndParameter.

    // =================================================================================================
    // 5. PUŁAPKA: @Validated NA KLASIE KONTROLERA → ConstraintViolationException i 500 (ValidatedController)
    // =================================================================================================
    //
    // W tutorialach sprzed 2023 roku: „dodaj @Validated na klasę kontrolera, żeby działały ograniczenia na parametrach”.
    // Od Spring Framework 6.1 jest to szkodliwe. Spring MVC, widząc @Validated na klasie, ODDAJE walidację
    // parametrów proxy AOP (jak w serwisie, Val05) i własną walidację wyłącza. Efekt: wyjątek
    // jakarta.validation.ConstraintViolationException — którego MVC domyślnie NIE zamienia na 400 — więc
    // klient dostaje 500 Internal Server Error, a w logu pojawia się ERROR ze stosem wywołań.
    //
    // PUŁAPKA: zostawić @Validated na kontrolerze „na wszelki wypadek”. Dlaczego? Zamienia poprawny 400
    //   (błąd klienta) na 500 (rzekomy błąd serwera). Zostaw tę adnotację dla serwisów (Val05); w kontrolerze
    //   ograniczenia na parametrach działają bez niej.
    //
    // Test: section5_validatedOnController (MockMvc) i Val02ValidInControllersServerTest (prawdziwy serwer: status 500).

    // =================================================================================================
    // 6. @Valid + BindingResult: ręczna obsługa błędów (ProductController.manual)
    // =================================================================================================
    //
    // Jeśli tuż ZA parametrem z @Valid dodasz parametr typu BindingResult, Spring NIE rzuca wyjątku —
    // wkłada błędy do BindingResult, a kontroler sam decyduje, co zrobić (tu: odpowiedź 400 z mapą pole → komunikat).
    //
    // DOBRA PRAKTYKA: nie używaj BindingResult w każdym kontrolerze. Dlaczego? Powtarzasz tę samą obsługę w każdej
    //   metodzie, a łatwo zapomnieć if (errors.hasErrors()) — wtedy kontroler pracuje na błędnych danych.
    //   Lepiej pozwolić wyjątkowi wyjść i obsłużyć go raz, centralnie (s06_error_handling/Err03ControllerAdvice).
    //
    // Test: section6_bindingResult.

    // =================================================================================================
    // 7. DOMYŚLNA ODPOWIEDŹ 400 NIE POKAZUJE, CO JEST ŹLE
    // =================================================================================================
    //
    // Spring Boot 4.1.1 odpowiada na błąd walidacji tylko tak (sprawdzone na prawdziwym serwerze):
    //   {"timestamp":"...","status":400,"error":"Bad Request","path":"/api/products"}
    // Ani słowa o tym, które pole jest złe. W logu serwera (WARN) są szczegóły tylko dla MethodArgumentNotValidException; dla
    // HandlerMethodValidationException nie ma nawet wpisu w logu (przy domyślnych ustawieniach). Klient (frontend,
    // uczeń z plikiem .http) widzi „złe zapytanie” i musi zgadywać. Test na MockMvc tego NIE pokaże: strona błędu
    // /error to mechanizm kontenera serwletów, więc wymaga prawdziwego serwera (RANDOM_PORT).
    //
    // Włączenie spring.mvc.problemdetails.enabled=true daje format application/problem+json
    // ({"detail":"Invalid request content.","status":400,"title":"Bad Request",...}) — nadal BEZ listy pól.
    // Czytelną listę błędów (pole → komunikat) trzeba zbudować samemu: s06_error_handling/Err03ControllerAdvice.
    //
    // Test: Val02ValidInControllersServerTest (sekcje 7a i 7b).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Same ograniczenia w rekordzie nic nie robią — w kontrolerze potrzebny jest @Valid przy @RequestBody.
     *   • @Valid @RequestBody → błąd: MethodArgumentNotValidException → 400, metoda się nie wykonuje.
     *   • Ograniczenia na @RequestParam/@PathVariable (od Framework 6.1, bez @Validated): HandlerMethodValidationException → 400.
     *   • Metoda z @Valid @RequestBody i ograniczeniem na innym parametrze: oba błędy → HandlerMethodValidationException.
     *   • @Validated na klasie kontrolera: ConstraintViolationException → 500. Zostaw ją serwisom.
     *   • Zły JSON → HttpMessageNotReadableException (400); zły typ parametru → MethodArgumentTypeMismatchException (400).
     *   • @Valid + BindingResult: bez wyjątku, błędy w BindingResult — kontroler obsługuje je sam.
     *   • Domyślne 400 nie mówi, które pole jest złe — czytelne błędy: dział s06.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? POST /api/products/unchecked z ciałem {"name":" ","sku":"x","price":-1}. Jaki status i dlaczego?
     *   2. Co się stanie? GET /api/products?limit=0 — jaki wyjątek zgłosi Spring MVC i jaki będzie status?
     *   3. Co się stanie? GET /api/products?limit=abc — ten sam wyjątek co dla limit=0? Dlaczego?
     *   4. ZNAJDŹ BŁĄD: ktoś dodał @Validated na klasę kontrolera „żeby ograniczenia na parametrach zadziałały”.
     *      Klient dla ?limit=0 dostaje 500 zamiast 400. Co jest przyczyną i jak to naprawić?
     *   5. Co się stanie? POST /api/products/copies?copies=1 z ciałem o złym SKU — jaki wyjątek (MethodArgumentNotValid czy
     *      HandlerMethodValidation)? Dlaczego to ważne dla obsługi błędów?
     *   6. ZNAJDŹ BŁĄD: programista napisał test MockMvc, który sprawdza, że w treści odpowiedzi 400 jest nazwa złego pola.
     *      Test padnie (a na prawdziwym serwerze też nie znajdzie nazwy). Dlaczego?
     *   7. Kiedy zamiast wyjątku dostaniesz błędy w parametrze BindingResult i czemu nie robić tego w każdej metodzie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Val02ValidInControllersExercisesTest
    // =================================================================================================

    // ĆWICZENIE 1 (łatwe): otwórz ExerciseCustomerController. Metoda register przyjmuje rekord CustomerRequest
    // (ma już ograniczenia), ale nikt ich nie sprawdza. Popraw kontroler tak, aby zły formularz dawał 400.
    // Podpowiedź: jedna adnotacja przy parametrze (sekcja 2).
    //
    // ĆWICZENIE 2 (średnie): w tym samym kontrolerze metoda one przyjmuje @PathVariable long id.
    // Dopisz ograniczenie, by id mniejsze od 1 dawało 400, a poprawne id — 200.
    // Podpowiedź: @Min(1) przy parametrze; nie dodawaj @Validated na klasie (sekcja 5).


    // ĆWICZENIE 3 (trudniejsze): zamień wyjątek walidacji ciała na mapę „pole → komunikat”, posortowaną po nazwie pola
    // (TreeMap). Gdy jedno pole ma kilka błędów, zostaw komunikat leksykograficznie najmniejszy.
    // Podpowiedź: ex.getBindingResult().getFieldErrors(), w każdym błędzie getField() i getDefaultMessage();
    // potem Map.merge(pole, komunikat, funkcja wybierająca mniejszy z dwóch tekstów).
    static Map<String, String> fieldErrors(MethodArgumentNotValidException ex) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 200 OK — metoda się wykonała z błędnymi danymi (odpowiedź {"sku":"x","name":" "}). Przy parametrze brakuje
     *      @Valid, więc ograniczenia w rekordzie nie zostały sprawdzone. Sprawdzone testem (section2).
     *   2. HandlerMethodValidationException (wbudowana walidacja metod, Spring 6.1+), status 400. Sprawdzone testem (section3).
     *   3. Nie. "abc" nie da się zamienić na int, więc wyjątek to MethodArgumentTypeMismatchException (też 400) —
     *      to błąd konwersji, a walidator w ogóle nie startuje. Sprawdzone testem (section3).
     *   4. @Validated na klasie oddaje walidację parametrów proxy AOP, które rzuca ConstraintViolationException,
     *      a MVC nie zamienia go na 400 → 500. Naprawa: usunąć @Validated z kontrolera (ograniczenia na
     *      parametrach działają bez niej od Framework 6.1). Sprawdzone testami (section5 i test na prawdziwym serwerze).
     *   5. HandlerMethodValidationException — mimo że błąd jest w ciele. Handler łapiący tylko
     *      MethodArgumentNotValidException by go nie obsłużył. Sprawdzone testem (section4).
     *   6. Domyślna odpowiedź 400 w Boot 4.1.1 zawiera tylko timestamp, status, error i path — bez pól i komunikatów
     *      walidacji. W MockMvc ciało jest wręcz puste (strona błędu /error nie jest tam renderowana). Sprawdzone
     *      testami (section1 i Val02ValidInControllersServerTest).
     *   7. Gdy tuż za parametrem z @Valid stoi parametr BindingResult — wyjątku nie ma, kontroler odpowiada
     *      sam. Nie rób tego wszędzie: powtarzasz kod i łatwo zapomnieć o errors.hasErrors(), a centralna
     *      obsługa (s06) robi to raz. Sprawdzone testem (section6).
     */
    // </editor-fold>
}

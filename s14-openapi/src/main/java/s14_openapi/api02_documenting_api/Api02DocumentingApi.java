package s14_openapi.api02_documenting_api;

import java.util.List;

/**
 * <pre>
 * TEMAT: Dokumentowanie API adnotacjami — @Operation, @ApiResponse, @Parameter, @Schema, @Tag, @Hidden
 *        (operation = operacja; response = odpowiedź; parameter = parametr; schema = schemat; tag = znacznik; hidden = ukryty)
 *
 * W SKRÓCIE:
 *   Dokument z Api01Springdoc był poprawny, ale „goły”: bez opisów, bez przykładów, z błędnymi statusami (200 zamiast 201)
 *   i bez żadnej wzmianki o 400 i 404. Adnotacje z pakietu io.swagger.v3.oas.annotations dopisują to, czego springdoc
 *   nie odczyta z samego kodu. Do tego springdoc sam odczytuje ograniczenia Bean Validation (@NotBlank, @Size, @Min…)
 *   i zamienia je na reguły schematu (required, minLength, minimum…) — walidację piszesz raz, a dokument ją pokazuje.
 *   Dokumentacja i kod mają jednak dwa życia: dlatego na końcu lekcji jest test, który PILNUJE, żeby dokument mówił to,
 *   co robi API.
 *
 * ANALOGIA: etykiety na półkach i instrukcja obsługi.
 *   Springdoc sam przepisze z półek nazwy i ceny (typy i adresy). Ale „do czego to służy”, „co zrobić, gdy brakuje”,
 *   „przykładowy produkt” — to etykiety, które musisz przykleić sam (adnotacje). A kontrola jakości (test) sprawdza, czy
 *   etykiety nie wiszą na złych półkach po przestawieniu towaru.
 *
 * JAK TO DZIAŁA:
 *   Adnotacja na metodzie kontrolera        →  pole w dokumencie
 *   @Tag(name, description) na klasie       →  tags[] + znacznik operacji (zastępuje nazwę klasy "product-controller")
 *   @Operation(summary, description)        →  operation.summary, operation.description
 *   @Parameter(description, example)        →  parameter.description, parameter.example
 *   @ApiResponse(responseCode, description, content)  →  operation.responses["201"], ["400"], ["404"]…
 *   @Schema(description, example, allowableValues) na polu rekordu  →  schema.properties.<pole>.description/example/enum
 *   @Hidden                                 →  punkt końcowy znika z dokumentu (nadal działa!)
 *   @NotBlank @Size @Pattern @DecimalMin @Min @Max  →  required, minLength/maxLength, pattern, minimum, maximum
 *
 *   Odpowiedź 201 w dokumencie:  "responses": {"201": {"description": "Produkt utworzony", ...}, "400": {...}}
 *   Błąd 400 naprawdę wygląda tak:  Content-Type: application/problem+json  {"detail":"Invalid request content.",
 *                                   "instance":"/api/products","status":400,"title":"Bad Request"}
 *
 * SŁÓWKA:
 *   summary = streszczenie (krótki tytuł operacji); description = opis; example = przykład; response code = kod
 *   odpowiedzi (status); content = zawartość (typ mediów + schemat); required = wymagane; constraint = ograniczenie;
 *   pattern = wzorzec (wyrażenie regularne); ProblemDetail = standardowe ciało błędu (RFC 9457, application/problem+json);
 *   hidden = ukryty; deprecated = przestarzały; contract = umowa (dokument jako umowa z odbiorcą API).
 *
 * ZOBACZ TEŻ: s14_openapi/Api01Springdoc (dokument bez adnotacji), s05_validation/Val01BeanValidation (adnotacje walidacji),
 *             s06_error_handling/Err04ProblemDetail (ProblemDetail), s04_web_rest/Rest03Responses (statusy odpowiedzi),
 *             s11_testing/Test01TestSlices (wycinki testów, MockMvc), s14_openapi/Api03GroupsAndVersions (grupy i wersje).
 * </pre>
 */
public final class Api02DocumentingApi {

    private Api02DocumentingApi() {
    }

    // =================================================================================================
    // 1. OPISY: @Tag, @Operation, @Parameter (test: operationTagAndParameterDescriptions)
    // =================================================================================================
    //
    // Zobacz ProductController w tym pakiecie:
    //   @Tag(name = "Produkty", description = "Katalog produktów sklepu")     na klasie
    //   @Operation(summary = "Lista produktów", description = "...")          na metodzie
    //   @Parameter(description = "...", example = "KSIAZKI")                  przed @RequestParam / @PathVariable
    // Swagger UI pokazuje summary jako tytuł wiersza operacji, a description po rozwinięciu. Zasada: summary — krótko
    // (do ~6 słów, co robi), description — szczegóły (reguły, skutki uboczne, uprawnienia).
    //
    // UWAGA: adnotacja {@code @Tag} z springdoc to {@code io.swagger.v3.oas.annotations.tags.Tag} — nie myl jej z
    // {@code @Tag} z JUnit (org.junit.jupiter.api.Tag), której używamy w ćwiczeniach. Nazwy są takie same, pakiety różne.
    //
    // DOBRA PRAKTYKA: pisz summary i description w języku odbiorców API, a przykłady (example) bierz z prawdziwych,
    //   realistycznych danych (ELE-001, Laptop Pro 14). Dlaczego? Przykład z dokumentu trafia do „Try it out” — odbiorca
    //   klika wyślij i od razu dostaje poprawną odpowiedź, zamiast zgadywać format.

    // =================================================================================================
    // 2. @Schema NA POLACH REKORDU (test: schemaAnnotationAddsDescriptionAndExample)
    // =================================================================================================
    //
    // W NewProduct każde pole rekordu ma @Schema(description, example). allowableValues = {...} daje "enum" (lista
    // dozwolonych wartości). @Schema na samym rekordzie opisuje cały obiekt. Adnotację kładziesz PRZY składowej rekordu —
    // springdoc (swagger-core) przenosi ją na właściwość schematu.
    //
    // PUŁAPKA: @Schema(example = "5499.99") jest tylko PRZYKŁADEM w dokumencie — niczego nie sprawdza. Dlaczego to ważne?
    //   Łatwo pomyśleć, że allowableValues lub example „pilnują” danych. Nie pilnują: przy zapytaniu z kategorią
    //   "INNA" kontroler nadal ją przyjmie. Walidację robią adnotacje Bean Validation + @Valid (sekcja 3).

    // =================================================================================================
    // 3. BEAN VALIDATION → SCHEMAT (test: beanValidationConstraintsAppearInSchema)
    // =================================================================================================
    //
    // Springdoc 3.1.1 odwzorowuje (sprawdzone testem na NewProduct):
    //   @NotNull, @NotBlank    → nazwa pola na liście "required" (@NotBlank dodatkowo minLength: 1)
    //   @Size(min=3, max=60)   → minLength: 3, maxLength: 60
    //   @Pattern(regexp=...)   → pattern
    //   @DecimalMin("0.01")    → minimum: 0.01
    //   @Min(0), @Max(10000)   → minimum: 0, maximum: 10000
    // Pole prymitywne {@code int stock} z samym {@code @Min(0)} NIE trafia na listę "required" — samo @Min nie jest
    // @NotNull. Gdybyś chciał je oznaczyć jako wymagane, użyj {@code @Schema(requiredMode = REQUIRED)}.
    //
    // PRZED: ograniczenia opisane ręcznie w description ("od 3 do 60 znaków") — rozjadą się z walidacją.
    // PO: jedna adnotacja @Size działa w kodzie (walidacja) i w dokumencie (minLength/maxLength).
    //
    // DOBRA PRAKTYKA: ograniczenia danych wyrażaj adnotacjami Bean Validation, a nie opisem w tekście. Dlaczego? Jedno
    //   źródło prawdy: zmieniasz @Size(max = 60) → 80 i dokument zmienia się sam, bez ryzyka, że ktoś zapomni poprawić opis.

    // =================================================================================================
    // 4. ODPOWIEDZI: @ApiResponse I ProblemDetail (test: apiResponsesMatchRealBehaviour)
    // =================================================================================================
    //
    // W Api01Springdoc dokument mówił 200 dla POST. Tu: {@code @ApiResponse(responseCode = "201", ...)} dopisuje 201
    // (i POST nie ma już 200 — adnotacja zastępuje domyślną odpowiedź). Błędy opisujemy tak:
    //   @ApiResponse(responseCode = "400", description = "...",
    //                content = @Content(mediaType = "application/problem+json",
    //                                   schema = @Schema(implementation = ProblemDetail.class)))
    // ProblemDetail (klasa Springa) trafia do components.schemas z polami type, title, status, detail, instance.
    // Stała {@code MediaType.APPLICATION_PROBLEM_JSON_VALUE} = "application/problem+json". Bez jawnego mediaType springdoc
    // wpisałby "*/*" — prawdziwa odpowiedź błędu ma jednak typ application/problem+json.
    //
    // Dlaczego błąd 400 NAPRAWDĘ jest ProblemDetail? W tej lekcji ProductErrorAdvice dziedziczy po
    // {@code ResponseEntityExceptionHandler}, który zamienia błędy Spring MVC (w tym walidację {@code @Valid})
    // na ProblemDetail. Test sprawdza to na prawdziwym zapytaniu (status 400, application/problem+json).
    //
    // PUŁAPKA: opisujesz w dokumencie ProblemDetail, ale aplikacja tego nie robi. Dlaczego to częsty błąd? Sama
    //   adnotacja {@code @ApiResponse} niczego w aplikacji nie zmienia — to tylko tekst. Bez
    //   {@code ResponseEntityExceptionHandler} (albo włączonej właściwości spring.mvc.problemdetails.enabled — istnieje w
    //   Boocie 4.1.1) błąd walidacji w MockMvc kończy się statusem 400 BEZ ciała i bez typu application/problem+json
    //   (sprawdzone uruchomieniem tej lekcji, zanim dodaliśmy dziedziczenie). Zawsze porównuj dokument z prawdziwą odpowiedzią.

    // =================================================================================================
    // 5. @ExceptionHandler Z @ResponseStatus — GLOBALNE ODPOWIEDZI (test: klasa StatusOnAdvice)
    // =================================================================================================
    //
    // Springdoc czyta też klasy {@code @RestControllerAdvice}. Gdy metoda {@code @ExceptionHandler} ma
    // {@code @ResponseStatus(HttpStatus.CONFLICT)}, springdoc dopisuje odpowiedź 409 (z ProblemDetail) do WSZYSTKICH
    // operacji — także do GET /api/products, który nie ma jak dać konfliktu. Sprawdzone testem StatusOnAdvice
    // (klasa pomocnicza StatusAdviceDemo). W ProductErrorAdvice status 404 siedzi wewnątrz ProblemDetail (a nie w
    // {@code @ResponseStatus}), więc springdoc go nie widzi i NIE dopisuje globalnie — dlatego 404 opisujemy
    // {@code @ApiResponse} tylko tam, gdzie może wystąpić (GET /{sku}).
    //
    // PUŁAPKA: dokument pełen „obcych” statusów (409 przy odczycie listy). Dlaczego to problem? Odbiorca API pisze
    //   obsługę błędów, które nigdy nie wystąpią, i przestaje ufać dokumentowi. Statusy specyficzne dla operacji opisuj
    //   adnotacją {@code @ApiResponse} na tej operacji; globalne (@ResponseStatus w advice) zostaw dla naprawdę
    //   globalnych błędów (np. 500).

    // =================================================================================================
    // 6. @Hidden (test: hiddenEndpointWorksButIsNotDocumented)
    // =================================================================================================
    //
    // {@code @Hidden} na metodzie (albo klasie) wyrzuca punkt końcowy z dokumentu. GET /api/products/internal/count
    // dalej działa (200), ale nie ma go w paths. Użyj do punktów technicznych (diagnostyka).
    //
    // PUŁAPKA: traktowanie @Hidden jako zabezpieczenia. Dlaczego to błąd? Ukrycie w dokumencie nie blokuje dostępu —
    //   każdy, kto zna adres, wywoła punkt końcowy. Ochronę zapewnia dopiero uwierzytelnianie i autoryzacja (s12_security).

    // =================================================================================================
    // 7. PUŁAPKA GŁÓWNA: DOKUMENTACJA ROZJEŻDŻA SIĘ Z KODEM (test: documentStaysInSyncWithBehaviour)
    // =================================================================================================
    //
    // Adnotacje to tekst obok kodu. Ktoś zmieni {@code ResponseEntity.created} na {@code ok}, a {@code @ApiResponse(201)}
    // zostanie — dokument skłamie, a kompilator nic nie powie. Remedium: test, który wykonuje prawdziwe zapytania i sprawdza,
    // że zwrócony status jest WŚRÓD udokumentowanych (i że nieprawdziwy np. 200 dla POST nie jest). Test w tej lekcji:
    //   1. pobiera /v3/api-docs,
    //   2. wywołuje POST (poprawny i błędny), GET istniejącego i nieistniejącego produktu, GET listy,
    //   3. dla każdej odpowiedzi sprawdza, czy jej status jest w sekcji "responses" odpowiedniej operacji.
    // Tak samo można pilnować operationId, listy "required" albo istnienia ścieżek.
    //
    // PUŁAPKA: dokumentacja „po wdrożeniu” — pisana raz i zapomniana. Dlaczego rozjeżdża się najszybciej? Nic jej nie
    //   wymusza; nikt nie dostaje czerwonego testu, gdy zmienia kod.
    //
    // DOBRA PRAKTYKA: test dokumentu w każdym API, które mają używać inni. Dlaczego? Kosztuje kilkanaście linii, a
    //   zamienia „dokumentacja chyba jest aktualna” w „dokumentacja jest sprawdzana przy każdym budowaniu”.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Pakiet adnotacji: io.swagger.v3.oas.annotations (@Operation, @Parameter, @Tag, @Hidden), ...media (@Schema,
     *     @Content), ...responses (@ApiResponse).
     *   • @Tag na klasie = grupa w Swagger UI; @Operation(summary, description); @Parameter(description, example).
     *   • @Schema na polach rekordu: description, example, allowableValues (→ enum). To tylko dokumentacja — nie waliduje.
     *   • Bean Validation → schemat: @NotNull/@NotBlank → required; @Size → minLength/maxLength; @Pattern → pattern;
     *     @DecimalMin/@Min → minimum; @Max → maximum. Prymityw z samym @Min nie jest "required".
     *   • @ApiResponse(responseCode, description, content = @Content(mediaType, schema = @Schema(implementation = ...)))
     *     ustawia statusy, także 201 zamiast domyślnego 200. Błędy: ProblemDetail, application/problem+json.
     *   • @ExceptionHandler + @ResponseStatus w advice → ten status w KAŻDEJ operacji.
     *   • @Hidden ukrywa z dokumentu, ale nie zabezpiecza.
     *   • Pilnuj dokumentu testem: prawdziwe zapytania → status musi być w "responses".
     *
     * PYTANIA KONTROLNE:
     *   1. Co springdoc odczytuje sam, a co musisz opisać adnotacjami?
     *   2. Jak adnotacja @Size(min = 3, max = 60) na polu rekordu wpływa na dokument? A @NotBlank?
     *   3. Co się stanie? Pole {@code int stock} ma tylko {@code @Min(0)} — czy trafi na listę "required" w schemacie?
     *   4. ZNAJDŹ BŁĄD: {@code @Schema(allowableValues = {"A", "B"})} na polu category — „teraz kontroler odrzuci
     *      kategorię C”. Co jest nie tak?
     *   5. Co się stanie? Dodasz {@code @Hidden} do metody kontrolera i wywołasz jej adres — jaki status? A co jest w
     *      dokumencie?
     *   6. ZNAJDŹ BŁĄD: {@code @ExceptionHandler(IllegalStateException.class) @ResponseStatus(HttpStatus.CONFLICT)} w
     *      advice — „springdoc doda 409 tylko do operacji, które rzucają IllegalStateException”. Prawda?
     *   7. Co się stanie? Zmienisz w ProductController {@code ResponseEntity.created(...)} na {@code ResponseEntity.ok(...)},
     *      zostawisz {@code @ApiResponse(responseCode = "201")} — który test lekcji to wykryje?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Api02DocumentingApiExercisesTest
    // =================================================================================================
    //
    // Ćwiczenia to małe „lintery” dokumentu OpenAPI (tekst JSON). Czytasz go Jacksonem 3:
    // {@code tools.jackson.databind.json.JsonMapper.builder().build().readTree(json)} zwraca {@code JsonNode};
    // {@code get("pole")} daje podwęzeł albo null, {@code propertyNames()} — nazwy pól obiektu, {@code isArray()} i
    // iteracja {@code for (JsonNode x : tablica)} — elementy, {@code asString()} — tekst z węzła.

    /**
     * ĆWICZENIE 1 (łatwe): zwróć posortowaną listę nazw pól z "required" schematu {@code components.schemas.<schemaName>}.
     * Gdy schemat nie ma "required" — pusta lista. Przykład: required = ["sku","name"] → ["name", "sku"].
     */
    static List<String> exercise1(String openApiJson, String schemaName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć posortowaną listę pól schematu {@code components.schemas.<schemaName>.properties},
     * które NIE mają opisu ("description"). To kontrola „czy każde pole ma etykietę”.
     * Podpowiedź: {@code properties.get(nazwa).get("description") == null}.
     */
    static List<String> exercise2(String openApiJson, String schemaName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): znajdź operacje, które mają parametr ścieżki ({@code "in": "path"}), a NIE mają
     * odpowiedzi "404" — odbiorca nie dowie się, że szukany zasób może nie istnieć. Zwróć posortowaną listę
     * "METODA ścieżka" (metoda wielkimi literami, np. "GET /api/products/{sku}"). Sprawdzaj tylko metody get, put, post,
     * delete, patch; pomijaj inne pola obiektu ścieżki. Operacja bez parametrów w ogóle (brak "parameters") nie ma
     * parametru ścieżki.
     */
    static List<String> exercise3(String openApiJson) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Sam odczytuje ścieżki, metody HTTP, parametry, typy z sygnatur (schematy rekordów), statusy z
     *      @ResponseStatus i ograniczenia Bean Validation. Adnotacjami opisujesz: opisy i przykłady, statusy wybierane w
     *      ciele metody (201, 404), typy błędów (ProblemDetail), grupy (@Tag) i to, co ma zniknąć (@Hidden).
     *   2. @Size(min=3, max=60) → minLength: 3 i maxLength: 60 w schemacie pola. @NotBlank → pole trafia na listę
     *      "required" i dostaje minLength: 1. Sprawdzone testem beanValidationConstraintsAppearInSchema.
     *   3. Nie. Lista "required" w NewProduct to sku, name, category, price — pole stock (prymityw z samym @Min) w niej
     *      nie ma; dostaje tylko minimum: 0. Sprawdzone testem.
     *   4. @Schema(allowableValues) to tylko opis w dokumencie (enum). Kontroler niczego nie odrzuci — do tego potrzebna
     *      walidacja (@Pattern, własna adnotacja albo typ wyliczeniowy enum).
     *   5. Status 200 (adnotacja nie wyłącza metody). W dokumencie ścieżki nie ma — sprawdzone testem
     *      hiddenEndpointWorksButIsNotDocumented (GET /api/products/internal/count → 200, paths ma 2 wpisy).
     *   6. Fałsz. Springdoc dopisuje taką odpowiedź do WSZYSTKICH operacji (także GET listy). Sprawdzone testem
     *      StatusOnAdvice. Statusy specyficzne dla operacji dodawaj @ApiResponse na tej operacji.
     *   7. Padnie dokładnie jeden test: documentStaysInSyncWithBehaviour (asercja „POST 201”) — prawdziwy POST zwróci 200,
     *      a dokument opisuje tylko 201. Sprawdzone uruchomieniem tej zmiany; pozostałe testy lekcji zostają zielone,
     *      bo żaden inny nie wykonuje poprawnego POST-a i nie porównuje statusu z dokumentem.
     */
    // </editor-fold>
}

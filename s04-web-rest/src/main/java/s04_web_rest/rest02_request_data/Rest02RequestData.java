package s04_web_rest.rest02_request_data;

/**
 * <pre>
 * TEMAT: Dane z zapytania HTTP — {@code @PathVariable}, {@code @RequestParam}, {@code @RequestBody}, {@code @RequestHeader}
 *        i obiekt z parametrów zapytania ({@code @ModelAttribute})
 *        (path variable = zmienna ścieżki; request parameter = parametr zapytania; request body = ciało zapytania;
 *        request header = nagłówek zapytania; model attribute = atrybut modelu)
 *
 * W SKRÓCIE:
 *   Zapytanie HTTP niesie dane w czterech miejscach: w ŚCIEŻCE (/api/products/ELE-001), w PARAMETRACH po „?”
 *   ({@code ?limit=3&offset=0}), w NAGŁÓWKACH (Accept-Language: pl) i w CIELE (JSON). Do każdego miejsca jest
 *   adnotacja parametru metody kontrolera. Spring sam wyciąga tekst, zamienia go na typ parametru (int, BigDecimal,
 *   enum, rekord) i woła metodę. Gdy się nie da (brak wymaganego parametru, „abc” zamiast liczby, zły JSON) —
 *   odpowiada 400 Bad Request, zanim Twoja metoda w ogóle ruszy.
 *
 * ANALOGIA: list polecony.
 *   Adres na kopercie to ŚCIEŻKA (do kogo: /api/products/ELE-001). Dopiski na kopercie „pilne”, „nie zginać” to
 *   PARAMETRY i NAGŁÓWKI — krótkie informacje o tym, jak obsłużyć przesyłkę. Zawartość koperty to CIAŁO — tu może być
 *   długi dokument (JSON). Listonosz (Spring) czyta kopertę i przekazuje adresatowi (metodzie) gotowe rzeczy.
 *
 * JAK TO DZIAŁA:
 *   RequestMappingHandlerAdapter ma listę „rozwiązywaczy argumentów” (HandlerMethodArgumentResolver). Dla każdego
 *   parametru metody pyta je po kolei: „umiesz go wypełnić?”. PathVariableMethodArgumentResolver czyta zmienne ścieżki,
 *   RequestParamMethodArgumentResolver — parametry, RequestHeaderMethodArgumentResolver — nagłówki,
 *   RequestResponseBodyMethodProcessor — ciało (przez HttpMessageConverter, czyli Jacksona). Tekst zamienia na typ
 *   parametru ConversionService (usługa konwersji): "3" → int, "ELEKTRONIKA" → Category.ELEKTRONIKA, "99.90" → BigDecimal.
 *   Skąd Spring zna NAZWĘ parametru (limit)? Z pliku .class — o ile kompilator zapisał tam nazwy (flaga -parameters).
 *
 * SŁÓWKA:
 *   required = wymagany; default value = wartość domyślna; query string = część adresu po „?”; conversion = konwersja;
 *   type mismatch = niezgodność typu; missing = brakujący; not readable = nieczytelny; argument resolver = rozwiązywacz
 *   argumentów (kod, który wypełnia parametr metody); binding = wiązanie (przepisywanie parametrów do pól obiektu);
 *   paging = stronicowanie; offset = przesunięcie; limit = ograniczenie liczby wyników.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest01Controllers (mapowanie metod), s04_web_rest/Rest04Json (Jackson i ciało JSON),
 *             s05_validation/Val02ValidInControllers (walidacja danych z zapytania — {@code @Valid}),
 *             t28_networking_http/Http01UriUrl (budowa adresu i query string), t14_optional/Optional01Basics (Optional),
 *             t19_annotations_reflection/Annotations03ReflectionBasics (refleksja — skąd Spring zna nazwy parametrów).
 * </pre>
 */
public final class Rest02RequestData {

    private Rest02RequestData() {
    }

    // =================================================================================================
    // 1. @PathVariable — dane ze ŚCIEŻKI (ProductQueryController.one, CategoryController)
    // =================================================================================================
    //
    //   @GetMapping("/{sku}")                    GET /api/products/ELE-001  →  sku = "ELE-001"
    //   Product one(@PathVariable String sku)
    //
    // Nazwa zmiennej w nawiasach klamrowych ({sku}) musi pasować do nazwy parametru — albo podajesz ją wprost:
    // @PathVariable("sku") String code (CategoryController.product). Zmienna ścieżki może mieć typ inny niż String:
    // long, BigDecimal, enum — Spring zamieni tekst sam. Dla enuma to Enum.valueOf, więc wielkość liter ma znaczenie:
    // /api/categories/KSIAZKI/products → 200, /api/categories/ksiazki/products → 400 (MethodArgumentTypeMismatchException).
    //
    // Konkretny adres wygrywa ze wzorcem: GET /api/products/search trafia do search(), a nie do one("search"), mimo że
    // /{sku} też by pasował. Spring wybiera NAJBARDZIEJ szczegółowe dopasowanie.
    //
    // DOBRA PRAKTYKA: zmienna ścieżki = IDENTYFIKATOR zasobu (sku, id), parametr po „?” = sposób odczytu (filtr,
    //   sortowanie, strona). Dlaczego? /api/products/ELE-001 to „ten jeden produkt”, a /api/products?limit=3 to
    //   „ta sama kolekcja, inaczej pokrojona”. Tak projektuje się czytelne API.

    // =================================================================================================
    // 2. @RequestParam — PARAMETRY po „?” (ProductQueryController.page, search, cheaperThan, bySku)
    // =================================================================================================
    //
    //   @RequestParam String name                         wymagany (required = true domyślnie); brak → 400
    //   @RequestParam(defaultValue = "5") int limit       opcjonalny z wartością domyślną (defaultValue ⇒ required = false)
    //   @RequestParam Optional<Category> category        opcjonalny bez wartości domyślnej: brak → Optional.empty()
    //   @RequestParam(required = false) Integer page      opcjonalny: brak → null (dlatego typ obiektowy, nie int!)
    //   @RequestParam List<String> sku                    ?sku=ELE-001&sku=KSI-001 albo ?sku=ELE-001,KSI-001 → lista
    //
    // Brak wymaganego parametru → 400 i MissingServletRequestParameterException:
    //   „Required request parameter 'name' for method parameter type String is not present”.
    //
    // PUŁAPKA: @RequestParam(required = false) int limit (PitfallController). Brak parametru → Spring chce wstawić null,
    //   a int nie przyjmie null. Wynik: 500 (nie 400!) i IllegalStateException: „Optional int parameter 'limit' is present
    //   but cannot be translated into a null value due to being declared as a primitive type. Consider declaring it as
    //   object wrapper for the corresponding primitive type.” Dlaczego 500? To błąd PROGRAMISTY w deklaracji metody,
    //   a nie klienta. Poprawnie: Integer, Optional<Integer> albo defaultValue = "10".
    //
    // PUŁAPKA: pusty parametr ?name= to NIE brak parametru. Spring dostaje pusty tekst "", więc search("") zwraca WSZYSTKIE
    //   produkty (każda nazwa zawiera ""). Dlaczego to ważne? Klient, który wyśle pusty formularz, dostanie całą bazę —
    //   takie przypadki obsłuż sam albo walidacją (s05_validation).

    // =================================================================================================
    // 3. KONWERSJA TYPÓW I BŁĄD 400 (test: typeConversionErrorGives400)
    // =================================================================================================
    //
    // Parametry i ścieżka to zawsze TEKST. Spring zamienia go na typ parametru przez ConversionService.
    // GET /api/products?limit=abc → „abc” nie jest liczbą → 400 i MethodArgumentTypeMismatchException:
    //   „Method parameter 'limit': Failed to convert value of type 'java.lang.String' to required type 'int';
    //    For input string: "abc"”.
    // To dobra wiadomość: Twoja metoda nigdy nie dostanie śmieci — błąd klienta kończy się po stronie Springa.
    // Ładniejsze komunikaty błędów (JSON z opisem) — dział s06_error_handling.

    // =================================================================================================
    // 4. @RequestBody — CIAŁO zapytania (ProductQueryController.priceCheck)
    // =================================================================================================
    //
    //   POST /api/products/price-check     Content-Type: application/json     {"sku":"KSI-001","quantity":3}
    //   PriceCheckResponse priceCheck(@RequestBody PriceCheckRequest request)
    //
    // Jackson (konwerter wiadomości) czyta JSON i tworzy rekord PriceCheckRequest przez jego konstruktor.
    // Błędy kończą się 400 i HttpMessageNotReadableException:
    //   • brak ciała → „Required request body is missing: public ...PriceCheckResponse ...priceCheck(...)”,
    //   • zły JSON {"quantity":} → „JSON parse error: Unexpected character ('}' (code 125)): expected a value”,
    //   • brak pola quantity (int) → „JSON parse error: Cannot map `null` into type `int` ...” — Jackson 3 domyślnie
    //     nie wstawia 0 za brakujący prymityw (szczegóły w s04_web_rest/Rest04Json).
    //
    // PUŁAPKA: @RequestBody na kilku parametrach jednej metody. Dlaczego nie zadziała? Ciało zapytania to strumień,
    //   który da się przeczytać RAZ. Jeden @RequestBody na metodę; więcej danych = jeden rekord z kilkoma polami.
    //
    // PUŁAPKA: GET z ciałem. Dlaczego unikać? Specyfikacja HTTP nie nadaje ciału GET znaczenia, a wiele serwerów
    //   pośrednich i bibliotek je gubi. Dane do wyszukiwania w GET → parametry po „?” albo @ModelAttribute (sekcja 7).

    // =================================================================================================
    // 5. @RequestHeader — NAGŁÓWKI (ProductQueryController.label, stockReport)
    // =================================================================================================
    //
    //   @RequestHeader(name = "Accept-Language", defaultValue = "pl") String language   opcjonalny z domyślną wartością
    //   @RequestHeader("X-Shop-Id") String shopId                                        wymagany; brak → 400
    //
    // Brak wymaganego nagłówka → 400 i MissingRequestHeaderException: „Required request header 'X-Shop-Id' for method
    // parameter type String is not present”. Nazwy nagłówków nie rozróżniają wielkości liter (x-shop-id = X-Shop-Id).
    //
    // DOBRA PRAKTYKA: nagłówki na dane „o zapytaniu” (język, klient, identyfikator śledzenia), nie na dane biznesowe.
    //   Dlaczego? Nagłówki są niewidoczne w adresie, przeglądarka i serwery pośrednie mogą je zmieniać lub usuwać,
    //   a dokumentacja API rzadko je dobrze opisuje. Ilość, cena, produkt — w ścieżce, parametrach albo ciele.

    // =================================================================================================
    // 6. NAZWY PARAMETRÓW A FLAGA -parameters (test: Rest02RequestDataParametersTest)
    // =================================================================================================
    //
    // W @RequestParam String name nie podajesz nazwy "name" — Spring bierze ją z nazwy parametru metody. Skąd ją zna?
    // Kompilator javac domyślnie NIE zapisuje nazw parametrów w pliku .class (zostają arg0, arg1). Zapisuje je dopiero
    // z flagą -parameters, a wtedy Spring czyta je refleksją (Method.getParameters()).
    // spring-boot-starter-parent (rodzic naszego parent pom) ustawia w maven-compiler-plugin <parameters>true</parameters>,
    // więc w tym kursie wszystko działa. IntelliJ przejmuje to ustawienie przy imporcie projektu Mavena.
    //
    // PUŁAPKA: projekt bez spring-boot-starter-parent (albo kompilacja „ręczna”) i kontroler bez nazw w adnotacjach.
    //   Aplikacja startuje, a zapytanie kończy się 500 i IllegalArgumentException: „Name for argument of type
    //   [java.lang.String] not specified, and parameter name information not available via reflection. Ensure that the
    //   compiler uses the '-parameters' flag.” Dlaczego to dziwne? Dawniej (Spring 5) działało i bez flagi — Spring
    //   czytał nazwy z informacji dla debuggera; Spring 6.1 przestał to robić, więc stare projekty nagle się psują.
    //   Test kompiluje kontroler w locie (javax.tools.JavaCompiler) raz bez flagi, raz z nią.

    // =================================================================================================
    // 7. OBIEKT Z PARAMETRÓW ZAPYTANIA — @ModelAttribute (ProductQueryController.filter, ProductFilter)
    // =================================================================================================
    //
    // Wyszukiwarka z pięcioma parametrami to pięć @RequestParam — nieczytelne. Zamiast tego jeden rekord:
    //   record ProductFilter(Category category, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock)
    //   List<Product> filter(@ModelAttribute ProductFilter filter)
    // GET /api/products/filter?category=ELEKTRONIKA&maxPrice=1500&inStock=true → Spring wywołuje konstruktor rekordu,
    // dopasowując nazwy parametrów zapytania do nazw składowych. Brakujące parametry → null (dlatego typy obiektowe).
    // Adnotację @ModelAttribute można pominąć — parametr typu złożonego (nie String, nie liczba) bez adnotacji Spring
    // i tak traktuje jak @ModelAttribute. Piszemy ją dla czytelności.
    //
    // Błąd konwersji w takim obiekcie (?maxPrice=abc) → 400, ale INNY wyjątek niż w sekcji 3:
    // MethodArgumentNotValidException z błędem pola: „Field error in object 'productFilter' on field 'maxPrice':
    // rejected value [abc] ...”. Ten sam wyjątek zobaczysz przy walidacji (s05_validation) — Spring zbiera WSZYSTKIE
    // błędy pól naraz, zamiast przerywać na pierwszym.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Ścieżka: @PathVariable String sku ← /api/products/{sku}; inna nazwa: @PathVariable("sku") String code.
     *   • Parametry: @RequestParam (wymagany), defaultValue = "5" (opcjonalny), Optional<T>, List<T> (?x=1&x=2).
     *   • Ciało: @RequestBody Rekord — JSON przez Jacksona; jeden na metodę; brak/zły JSON → 400.
     *   • Nagłówki: @RequestHeader("X-Shop-Id"), opcjonalny z defaultValue; nazwy bez rozróżniania wielkości liter.
     *   • Wiele parametrów → jeden rekord: @ModelAttribute ProductFilter (brak parametru = null).
     *   • Błędy klienta → 400: MissingServletRequestParameterException, MissingRequestHeaderException,
     *     MethodArgumentTypeMismatchException, HttpMessageNotReadableException, MethodArgumentNotValidException.
     *   • required = false + typ prymitywny → 500 (błąd programisty). Używaj Integer/Optional/defaultValue.
     *   • Nazwy parametrów bez adnotacji wymagają -parameters (spring-boot-starter-parent włącza je sam).
     *
     * PYTANIA KONTROLNE:
     *   1. Gdzie w zapytaniu HTTP mogą być dane i jaka adnotacja czyta każde z tych miejsc?
     *   2. Co się stanie? GET /api/products?limit=abc — jaki status i jaki wyjątek?
     *   3. Co się stanie? GET /api/pitfalls/primitive (bez parametru limit) — jaki status i dlaczego nie 400?
     *   4. ZNAJDŹ BŁĄD (PitfallController): @GetMapping("/products/{id}") String byId(@PathVariable("sku") String id)
     *      — zapytanie GET /api/pitfalls/products/ELE-001 kończy się błędem. Dlaczego i jaki to status?
     *   5. Czym różni się defaultValue = "5" od Optional<Integer> w @RequestParam?
     *   6. Co się stanie? GET /api/categories/ksiazki/products (małe litery) — jaki status?
     *   7. ZNAJDŹ BŁĄD: projekt bez spring-boot-starter-parent, metoda hello(@RequestParam String name), zapytanie
     *      GET /api/hello?name=Jan kończy się 500. Co jest nie tak i jak to naprawić na dwa sposoby?
     *   8. Kiedy wybierzesz @ModelAttribute z rekordem zamiast kilku @RequestParam?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w klasie Rest02ExerciseController (adresy /api/cw02/...), testy: Rest02RequestDataExercisesTest.
    // Rozwiązania wzorcowe: s04_web_rest.solutions.rest02_request_data.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Ścieżka — @PathVariable; parametry po „?” — @RequestParam (albo wiele naraz — @ModelAttribute); nagłówki —
     *      @RequestHeader; ciało — @RequestBody.
     *   2. 400 Bad Request i MethodArgumentTypeMismatchException: „Method parameter 'limit': Failed to convert value of
     *      type 'java.lang.String' to required type 'int'; For input string: "abc"”. Metoda page() nie zostaje wywołana.
     *      Sprawdzone testem typeConversionErrorGives400.
     *   3. 500 i IllegalStateException („Optional int parameter 'limit' is present but cannot be translated into a null
     *      value due to being declared as a primitive type...”). To nie błąd klienta (400), tylko programisty: parametr
     *      opcjonalny musi przyjmować null, a int nie może. Sprawdzone testem primitiveOptionalParameterGives500.
     *   4. W adnotacji jest nazwa "sku", a w adresie zmienna {id}. Spring szuka zmiennej ścieżki „sku”, nie znajduje
     *      jej i zgłasza MissingPathVariableException — status 500, bo to błąd w kodzie kontrolera, nie w zapytaniu.
     *      Sprawdzone testem wrongPathVariableNameGives500. Poprawka: @PathVariable("id") albo zmiana {id} na {sku}.
     *   5. Z defaultValue brak parametru daje gotową wartość 5 (i można użyć typu int). Z Optional<Integer> brak daje
     *      Optional.empty() — sam decydujesz w kodzie, co wtedy zrobić.
     *   6. 400 Bad Request — MethodArgumentTypeMismatchException, bo Category.valueOf("ksiazki") nie istnieje (enum
     *      rozróżnia wielkość liter). Sprawdzone testem enumPathVariableIsCaseSensitive.
     *   7. Kompilator nie zapisał nazw parametrów (brak flagi -parameters), więc Spring nie wie, że parametr nazywa się
     *      „name”: IllegalArgumentException „Name for argument of type [java.lang.String] not specified...”. Naprawa:
     *      (a) włącz -parameters (maven-compiler-plugin: <parameters>true</parameters>) albo (b) podaj nazwę wprost:
     *      @RequestParam("name"). Sprawdzone testem Rest02RequestDataParametersTest.
     *   8. Gdy parametrów jest kilka i tworzą jedną całość (filtr wyszukiwania, stronicowanie) — rekord jest czytelny,
     *      da się go przekazać dalej do serwisu i zwalidować jednym @Valid (s05_validation).
     */
    // </editor-fold>
}

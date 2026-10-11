package s04_web_rest.rest02_request_data;

/**
 * <pre>
 * TEMAT: Dane z zapytania HTTP — @PathVariable, @RequestParam, @RequestHeader i @RequestBody z rekordami;
 *        konwersja typów i błędy 400 oraz 415
 *        (path variable = zmienna ze ścieżki; request param = parametr zapytania; request header = nagłówek zapytania;
 *        request body = ciało zapytania; conversion = konwersja, zamiana typu)
 *
 * W SKRÓCIE:
 *   Zapytanie HTTP niesie dane w czterech miejscach, a Spring ma adnotację na każde z nich:
 *     ścieżka     /api/products/42                → @PathVariable long id
 *     parametry   /api/search?name=kawa&page=2    → @RequestParam String name, @RequestParam int page
 *     nagłówki    X-Client-Id: app-7              → @RequestHeader("X-Client-Id") String clientId
 *     ciało       {"customer":"Anna", ...}        → @RequestBody NewOrder order (JSON → rekord przez Jacksona)
 *   Spring sam zamienia tekst na typ parametru (String → long, int, List...). Gdy się nie da albo brakuje czegoś
 *   wymaganego — odpowiada 400 Bad Request, ZANIM Twoja metoda się wykona. Ciało bez nagłówka
 *   Content-Type: application/json → 415 Unsupported Media Type.
 *
 * ANALOGIA: list polecony.
 *   Adres na kopercie to ścieżka (/api/products/42 — „mieszkanie 42”). Dopiski na kopercie („pilne”, „do rąk
 *   własnych”) to parametry. Pieczątki urzędu pocztowego to nagłówki — informacje o przesyłce, a nie jej treść.
 *   Zawartość koperty to ciało. Content-Type to napis „w środku dokumenty po polsku” — bez niego adresat nie wie,
 *   jakim słownikiem czytać treść (415).
 *
 * JAK TO DZIAŁA:
 *   POST /api/orders                                  RequestMappingHandlerAdapter dla każdego parametru metody
 *   Content-Type: application/json                    wybiera „rozwiązywacz argumentów” (HandlerMethodArgumentResolver):
 *                                                       @PathVariable  → PathVariableMethodArgumentResolver
 *   {"customer":"Anna","lines":[...]}                  @RequestParam  → RequestParamMethodArgumentResolver
 *                                                       @RequestHeader → RequestHeaderMethodArgumentResolver
 *                                                       @RequestBody   → RequestResponseBodyMethodProcessor
 *                                                         → HttpMessageConverter wybrany po Content-Type (JSON → Jackson)
 *   Tekst → typ: ConversionService (usługa konwersji): "42" → 42L, "bio,fair" → [bio, fair].
 *   Błąd konwersji / brak wymaganego → wyjątek → DefaultHandlerExceptionResolver → 400 (albo 415).
 *
 * SŁÓWKA:
 *   query string = część adresu po „?”; required = wymagany; default value = wartość domyślna; header = nagłówek;
 *   content type = typ zawartości; media type = typ nośnika (np. application/json); unsupported = nieobsługiwany;
 *   bad request = złe zapytanie; mismatch = niezgodność (typów); missing = brakujący; parse error = błąd odczytu
 *   (JSON-a); argument resolver = „rozwiązywacz” argumentów (komponent przygotowujący parametr metody).
 *
 * ZOBACZ TEŻ: t28_networking_http/Http01UriUrl (budowa adresu: ścieżka i query string), t09_records/Records01Basics
 *             (rekordy), t14_optional/Optional01Basics (Optional), s04_web_rest/Rest01Controllers,
 *             s04_web_rest/Rest04Json (Jackson od środka), s05_validation (sprawdzanie poprawności danych).
 * </pre>
 */
public final class Rest02RequestData {

    private Rest02RequestData() {
    }

    // =================================================================================================
    // 1. @PathVariable — dane z samej ścieżki (Rest02RequestDataTest, sekcja 1)
    // =================================================================================================
    //
    //   @GetMapping("/api/products/{id}")                       {id} w adresie = miejsce na zmienną
    //   public ProductRef byId(@PathVariable long id)           nazwa parametru Javy = nazwa w klamrach
    //
    //   @GetMapping("/api/categories/{category}/products/{id}")
    //   ... (@PathVariable String category, @PathVariable("id") long productId)   ← jawna nazwa, gdy się różni
    //
    // GET /api/products/42 → id = 42L. GET /api/products/abc → „abc” nie da się zamienić na long →
    // MethodArgumentTypeMismatchException (niezgodność typu argumentu) → 400 Bad Request. Metoda się nie wykona.
    //
    // PUŁAPKA: Spring dopasowuje @PathVariable bez nazwy po NAZWIE PARAMETRU w kodzie. Dlaczego to działa?
    //   spring-boot-starter-parent kompiluje z opcją -parameters, więc nazwy parametrów są zachowane w pliku .class.
    //   Kompilujesz bez tej opcji (np. bez rodzica Boota)? Dostaniesz błąd przy zapytaniu — wtedy pisz nazwę jawnie:
    //   @PathVariable("id").
    //
    // DOBRA PRAKTYKA: zmienna ścieżki identyfikuje ZASÓB (/products/42), a parametry go filtrują lub stronicują
    //   (?page=2). Dlaczego? Adres /products/42 to „adres konkretnej rzeczy” — można go zapisać, wysłać, zapamiętać
    //   w pamięci podręcznej (cache); filtr zmienia się co chwilę i nie jest „nową rzeczą”.

    // =================================================================================================
    // 2. @RequestParam — parametry zapytania po „?” (sekcja 2)
    // =================================================================================================
    //
    //   @RequestParam String name                               wymagany — brak → 400 (MissingServletRequestParameterException)
    //   @RequestParam(required = false) Integer maxPrice        nieobowiązkowy — brak → null (typ obiektowy!)
    //   @RequestParam Optional<Integer> minPrice                nieobowiązkowy — brak → Optional.empty()
    //   @RequestParam(defaultValue = "0") int page              brak → "0" → 0 (defaultValue = sam staje się nieobowiązkowy)
    //   @RequestParam(name = "tag", required = false) List<String> tags
    //       ?tag=bio&tag=fair → [bio, fair]; ?tag=bio,fair → też [bio, fair] (przecinek dzieli wartości)
    //
    // GET /api/search?name=kawa → {"name":"kawa","maxPrice":null,"minPrice":null,"page":0,"size":20,"tags":[]}.
    //
    // PUŁAPKA: @RequestParam(required = false) int limit — typ PROSTY. Gdy parametru brak, Spring musiałby wstawić null,
    //   a int nie może być null → IllegalStateException („Optional int parameter 'limit' is present but cannot be
    //   translated into a null value...”) → 500, a nie 400! Dlaczego tak źle? To błąd programisty, nie klienta —
    //   Spring nie maskuje go jako „złe zapytanie”. Używaj Integer, Optional<Integer> albo defaultValue.
    //   Sprawdzone testem (sekcja 2, /api/search/limited).
    //
    // PUŁAPKA: pusty parametr (?name=) to NIE brak parametru — name = "" (pusty tekst), status 200. Dlaczego ważne?
    //   „Wymagany” w Springu znaczy tylko „obecny w adresie”; sprawdzenie, czy nie jest pusty, to walidacja
    //   (s05_validation). Sprawdzone testem.

    // =================================================================================================
    // 3. @RequestHeader — nagłówki (sekcja 3)
    // =================================================================================================
    //
    //   @RequestHeader("X-Client-Id") String clientId                           wymagany — brak → 400
    //   @RequestHeader(name = "Accept-Language", defaultValue = "pl") String language
    // Nazwy nagłówków HTTP NIE rozróżniają wielkości liter: x-client-id = X-Client-Id (sprawdzone testem).
    // Nagłówki niosą informacje O zapytaniu: kto pyta (klucz, token), w jakim języku, w jakim formacie (Accept).
    //
    // DOBRA PRAKTYKA: dane biznesowe (co zamawiam) — w ciele lub ścieżce; dane techniczne (kim jestem, jaki format,
    //   identyfikator śledzenia) — w nagłówkach. Dlaczego? Serwery pośredniczące, logi i bramki API czytają nagłówki;
    //   mieszanie jednego z drugim utrudnia i jedno, i drugie.

    // =================================================================================================
    // 4. @RequestBody z rekordem — JSON w ciele (sekcja 4)
    // =================================================================================================
    //
    //   POST /api/orders
    //   Content-Type: application/json
    //   {"customer":"Anna","lines":[{"productId":1,"quantity":2},{"productId":2,"quantity":3}]}
    //   → @RequestBody NewOrder order → OrderSummary {"customer":"Anna","lineCount":2,"totalQuantity":5}
    // Jackson tworzy rekord przez jego konstruktor kanoniczny — pola JSON dopasowuje po nazwach komponentów rekordu.
    // Zagnieżdżone obiekty i listy (List<OrderLine>) — też same.
    //
    //   co wyślesz                                      wynik (sprawdzone testem)
    //   bez Content-Type / Content-Type: text/plain     415 Unsupported Media Type (HttpMediaTypeNotSupportedException)
    //   uszkodzony JSON  {"customer":                   400 (HttpMessageNotReadableException: JSON parse error)
    //   brak ciała                                      400 (Required request body is missing)
    //   brak pola int, np. {"productId":1} bez quantity 400 — Jackson 3 nie wstawia po cichu 0 (szczegóły: Rest04Json)
    //   nieznane pole, np. "vip":true                   200 — pole zignorowane (szczegóły: Rest04Json)
    //
    // PUŁAPKA: zapomniany nagłówek Content-Type: application/json. Dlaczego 415? Spring wybiera konwerter po typie
    //   zawartości; gdy nagłówka brak, przyjmuje application/octet-stream (dowolne bajty), a żaden konwerter nie umie
    //   zamienić bajtów na NewOrder. „Przecież wysłałem poprawny JSON!” — serwer nie zgaduje formatu po treści.
    //   PYTANIE REKRUTACYJNE: „Czym różni się 400 od 415?”
    //
    // PUŁAPKA: @RequestBody może być tylko JEDEN na metodę. Dlaczego? Ciało zapytania to strumień czytany raz —
    //   po przeczytaniu przez pierwszy parametr nie ma już czego czytać. Kilka obiektów → opakuj je w jeden rekord.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • /api/products/{id} + @PathVariable long id; inna nazwa → @PathVariable("id").
     *   • ?name=kawa → @RequestParam String name (wymagany); required = false → typ obiektowy (Integer) albo Optional;
     *     defaultValue = "0" → nieobowiązkowy z wartością domyślną; ?tag=a&tag=b albo ?tag=a,b → List<String>.
     *   • @RequestHeader("X-Client-Id") — nazwy nagłówków bez rozróżniania wielkości liter.
     *   • @RequestBody Rekord — JSON → rekord przez Jacksona; tylko jeden na metodę; potrzebny Content-Type: application/json.
     *   • 400: zły typ (abc zamiast liczby), brak wymaganego parametru/nagłówka/ciała, uszkodzony JSON.
     *   • 415: ciało w formacie, którego serwer nie przyjmuje (brak lub zły Content-Type).
     *   • required = false na int → 500 przy braku parametru (błąd programisty) — używaj Integer/Optional/defaultValue.
     *
     * PYTANIA KONTROLNE:
     *   1. Gdzie w zapytaniu HTTP mogą być dane i jaka adnotacja Springa odpowiada każdemu miejscu?
     *   2. Co się stanie? GET /api/products/abc przy metodzie z @PathVariable long id.
     *   3. ZNAJDŹ BŁĄD: @GetMapping("/api/search/limited") public int limited(@RequestParam(required = false) int limit).
     *      Co dostanie klient, który nie poda parametru limit, i jak to naprawić?
     *   4. Co się stanie? POST /api/orders z poprawnym JSON-em, ale bez nagłówka Content-Type.
     *   5. Jak odebrać ?tag=bio&tag=fair? A co da ?tag=bio,fair?
     *   6. Czym różni się @RequestParam(required = false) Integer x od @RequestParam(defaultValue = "0") int x?
     *   7. Co się stanie? GET /api/search?name= (pusta wartość) — 400 czy 200?
     *   8. Dlaczego metoda kontrolera nie może mieć dwóch parametrów z @RequestBody?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielety w ShopExerciseController (ćwiczenia 1–3). Testy: Rest02RequestDataExercisesTest
    // =================================================================================================
    // Uwaga: w szkielecie metody nie mają parametrów — dopisujesz je sam. Dopóki tego nie zrobisz, testy są czerwone.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Ścieżka → @PathVariable; parametry po „?” → @RequestParam; nagłówki → @RequestHeader; ciało → @RequestBody.
     *   2. 400 Bad Request — „abc” nie zamienia się na long (MethodArgumentTypeMismatchException); metoda kontrolera
     *      w ogóle się nie wykonuje. Sprawdzone testem (sekcja 1).
     *   3. Brak parametru → Spring nie może wstawić null do int → IllegalStateException → 500 Internal Server Error
     *      (w MockMvc: wyjątek ServletException). Naprawa: Integer, Optional<Integer> albo defaultValue = "...".
     *      Sprawdzone testem (sekcja 2).
     *   4. 415 Unsupported Media Type — bez Content-Type Spring przyjmuje application/octet-stream i nie ma konwertera
     *      do rekordu. Sprawdzone testem (sekcja 4).
     *   5. @RequestParam(name = "tag") List<String> tags (z required = false, jeśli może go nie być). ?tag=bio,fair też
     *      daje [bio, fair] — Spring dzieli wartość po przecinku. Sprawdzone testem (sekcja 2).
     *   6. Pierwszy przy braku parametru daje null (trzeba to obsłużyć), drugi — 0. defaultValue sam czyni parametr
     *      nieobowiązkowym. Oba przy złej wartości (?x=abc) dają 400.
     *   7. 200 — parametr JEST (pusty tekst ""), więc wymaganie „obecny” jest spełnione. Pustość sprawdza walidacja
     *      (s05_validation). Sprawdzone testem (sekcja 2).
     *   8. Ciało to strumień bajtów czytany raz — drugi parametr nie miałby czego czytać. Rozwiązanie: jeden rekord
     *      zawierający oba obiekty.
     */
    // </editor-fold>
}

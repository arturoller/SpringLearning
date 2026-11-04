package s04_web_rest.rest02_request_data;

/**
 * <pre>
 * TEMAT: Dane z zapytania — ścieżka, parametry, nagłówki, ciasteczka i ciało
 *        (request data = dane zapytania; path variable = zmienna ścieżki; request parameter = parametr zapytania;
 *         request header = nagłówek zapytania; cookie = ciasteczko; request body = ciało zapytania;
 *         binding = wiązanie, zamiana tekstu z zapytania na argumenty metody)
 *
 * W SKRÓCIE:
 *   Zapytanie HTTP niesie dane w pięciu miejscach: w ścieżce (/products/3), w parametrach po znaku zapytania
 *   (?category=napoje), w nagłówkach (Accept-Language: pl), w ciasteczkach i w ciele (zwykle JSON). Do każdego miejsca
 *   jest adnotacja na parametrze metody: @PathVariable, @RequestParam, @RequestHeader, @CookieValue, @RequestBody.
 *   Spring wyciąga wartość, ZAMIENIA tekst na typ parametru (Long, int, List, rekord) i wywołuje metodę. Gdy czegoś
 *   brakuje albo wartość jest zła — odpowiada 400 Bad Request, zanim Twój kod w ogóle się uruchomi.
 *
 * ANALOGIA: formularz zamówienia w sklepie internetowym.
 *   Numer zamówienia jest wpisany w adresie koperty (ścieżka), filtry „tylko napoje do 20 zł” — na naklejkach na kopercie
 *   (parametry), język klienta i nazwa programu, z którego pisze — w pieczątkach (nagłówki), a sam list w środku
 *   (ciało). Pracownik poczty (Spring) czyta koperty i wkłada do Twojego okienka tylko to, o co prosisz. Jeśli
 *   w kopercie brakuje czegoś obowiązkowego — odsyła ją z adnotacją „błędne zgłoszenie” (400), a Ty nic o tym nie wiesz.
 *
 * JAK TO DZIAŁA:
 *   1. DispatcherServlet znajduje metodę (Rest01Controllers) i przygotowuje jej argumenty.
 *   2. Dla każdego parametru metody HandlerMethodArgumentResolver (rozwiązywacz argumentów) sprawdza adnotację
 *      i pobiera surową wartość: ze ścieżki, parametrów, nagłówka, ciasteczka albo ciała.
 *   3. Tekst zamieniany jest na typ parametru — dla zwykłych typów robi to ConversionService (usługa konwersji,
 *      np. "3" → Long), dla ciała — HttpMessageConverter (JSON → rekord, Jackson 3).
 *   4. Błąd na którymkolwiek kroku (brak wymaganej wartości, zły typ, zły JSON) to wyjątek, który Spring zamienia na 400.
 *      Metoda kontrolera nie jest wtedy wołana.
 *   5. Jeśli wszystko się udało — metoda dostaje gotowe argumenty.
 *
 * SŁÓWKA:
 *   required = wymagany; default value = wartość domyślna; optional = opcjonalny; query string = część adresu po
 *   znaku „?”; type conversion = konwersja typu; missing = brakujący; mismatch = niezgodność; form = formularz;
 *   payload = ładunek, zawartość ciała; deserialization = zamiana JSON-a na obiekt Javy.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest01Controllers (mapowania i droga zapytania),
 *             s04_web_rest/Rest03Responses (odpowiedzi: statusy, nagłówki),
 *             s04_web_rest/Rest04Json (Jackson: nazwy pól, daty, nieznane pola),
 *             s05_validation/Val02ValidInControllers (sprawdzanie poprawności danych z zapytania),
 *             s06_error_handling/Err01DefaultErrors (jak wyglądają odpowiedzi 400 i jak je ładnie opisać),
 *             t09_records/Records01Basics (rekordy).
 * </pre>
 */
public final class Rest02RequestData {

    private Rest02RequestData() {
    }

    // =================================================================================================
    // 1. @PathVariable — DANE ZE ŚCIEŻKI (ProductController.byId)
    // =================================================================================================
    //
    //   @GetMapping("/{id}")
    //   public ResponseEntity<Product> byId(@PathVariable Long id) { … }       // GET /rest02/products/3 → id = 3
    //
    // {id} w ścieżce to miejsce na wartość. @PathVariable mówi: „wstaw tu tę wartość i zamień na Long”. Używaj do
    // IDENTYFIKACJI zasobu: który produkt, które zamówienie. Kilka zmiennych — kilka adnotacji:
    // /customers/{customerId}/orders/{orderId} (ćwiczenie 1).
    //
    // Nazwa zmiennej to domyślnie nazwa parametru Javy (id). Możesz podać ją jawnie: @PathVariable("productId") Long number
    // — metoda byIdExplicitName. Test sekcji 1 (uruchomiony):
    //   a) /rest02/products/3 → 200 i Czekolada (tekst "3" zamieniony na Long 3);
    //   b) /rest02/products/999 → 404 (ResponseEntity.of(Optional.empty()) — o statusach więcej w Rest03Responses);
    //   c) /rest02/products/abc → 400 Bad Request, wyjątek MethodArgumentTypeMismatchException
    //      („Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'”).
    //      Czyli „zły format identyfikatora” to błąd KLIENTA (400), nie „nie ma takiego zasobu” (404) i nie błąd serwera.
    //
    // DOBRA PRAKTYKA: typ zmiennej ścieżki dobieraj do danych: Long/UUID zamiast String. Dlaczego? Konwersja robi
    //   darmową walidację formatu — metoda nigdy nie zobaczy "abc" w miejscu liczby, a klient dostaje od razu 400.
    //
    // PYTANIE REKRUTACYJNE: czym różni się @PathVariable od @RequestParam i kiedy użyjesz którego?
    //   (Ścieżka identyfikuje zasób: /products/3. Parametry zapytania zawężają, sortują, stronicują: ?category=napoje.)

    // =================================================================================================
    // 2. @RequestParam — PARAMETRY ZAPYTANIA (ProductController.list, search)
    // =================================================================================================
    //
    //   GET /rest02/products?category=napoje&maxPrice=2000&tag=bio&tag=import&limit=5
    //
    //   @RequestParam                              String category   → WYMAGANY (domyślnie!) — brak = 400
    //   @RequestParam(required = false)            String category   → brak = null
    //   @RequestParam                  Optional<Long> maxPrice       → brak = Optional.empty() (zamiast sprawdzania null)
    //   @RequestParam(defaultValue = "10")         int limit         → brak = 10 (parametr przestaje być wymagany)
    //   @RequestParam(required = false)            List<String> tag  → ?tag=a&tag=b ALBO ?tag=a,b → lista
    //
    // Test sekcji 2 (uruchomiony) sprawdza m.in.:
    //   • GET /search bez ?q → 400, wyjątek MissingServletRequestParameterException („Required request parameter 'q'…”);
    //   • limit=abc oraz maxPrice=abc → 400 (nie da się zamienić na liczbę);
    //   • ?tag=bio&tag=import i ?tag=bio,import dają ten sam wynik: 4 produkty (z tagiem bio LUB import);
    //   • category=napoje + maxPrice=2000 (20,00 zł w groszach) → tylko Herbata.
    //
    // Który sposób na „opcjonalny” wybrać? required=false (jest null, trzeba sprawdzać), Optional (czytelniej, bez
    // null), defaultValue (gdy sensowna wartość domyślna istnieje — np. limit 10, strona 0).
    //
    // PUŁAPKA: pusty parametr to NIE brak parametru. {@code ?category=} (znak równości, nic po nim) daje pusty napis "",
    //   a nie null. Test sekcji 2 pokazuje: filtr „kategoria równa pustemu napisowi” nie znajduje nic → lista pusta.
    //   Dlaczego groźne? Kod sprawdzający tylko {@code category == null} potraktuje "" jako prawdziwy filtr i zwróci
    //   pustą listę, choć klient „nie chciał filtrować”. Gdy to ważne, sprawdzaj też {@code isBlank()}.
    //
    // DOBRA PRAKTYKA: parametry zapytania — do FILTROWANIA, SORTOWANIA i STRONICOWANIA (zawężają listę zasobów);
    //   identyfikator zasobu — w ścieżce. Dlaczego? Adres /products/3 jest stałym „adresem” zasobu (można go zapisać
    //   w zakładce, zbuforować), a ?category=… opisuje widok listy.

    // =================================================================================================
    // 3. @RequestHeader I @CookieValue — NAGŁÓWKI I CIASTECZKA (ClientController)
    // =================================================================================================
    //
    //   @RequestHeader("X-Shop-Client") String client                              → wymagany, brak = 400
    //   @RequestHeader(name = "Accept-Language", defaultValue = "pl") String lang  → brak = "pl"
    //   @CookieValue(name = "shopSession", required = false) String session        → brak = null
    //
    // Nagłówki niosą informacje o KLIENCIE i o zapytaniu, nie o zasobie: język (Accept-Language), typ klienta,
    // token logowania (Authorization — s12_security), identyfikator śledzenia. Nazwa nagłówka ma myślniki, więc nie da
    // się jej użyć jako nazwy parametru Javy — podajesz ją napisem w adnotacji. Wielkość liter w nazwie nagłówka
    // nie ma znaczenia. Ciasteczko to nagłówek Cookie rozbity na części; przeglądarka dokleja je sama.
    // Test sekcji 3: brak X-Shop-Client → 400 (MissingRequestHeaderException); brak Accept-Language → wartość "pl";
    // ciasteczko shopSession=abc123 → odczytane; bez ciasteczka (required = false) → null.
    //
    // DOBRA PRAKTYKA: dane BIZNESOWE (co kupić, ile) wysyłaj w ścieżce, parametrach albo ciele; nagłówki zostaw na
    //   dane o samym zapytaniu. Dlaczego? Nagłówki są niewidoczne w adresie i łatwo je zgubić przy testowaniu,
    //   kopiowaniu linku czy w pamięci podręcznej — kto ich nie zna, nie odtworzy zapytania.

    // =================================================================================================
    // 4. @RequestBody — CIAŁO ZAPYTANIA, JSON → REKORD (ReviewController)
    // =================================================================================================
    //
    //   @PostMapping("/json")
    //   public ReviewRequest fromJson(@RequestBody ReviewRequest review) { … }
    //   record ReviewRequest(String author, Integer rating, String text) {}
    //
    // Klient wysyła nagłówek Content-Type: application/json i ciało {"author":"Ola","rating":5,"text":"Super"}.
    // HttpMessageConverter (Jackson 3) zamienia JSON na rekord: nazwy pól JSON dopasowuje do składowych rekordu.
    // Test sekcji 4 (uruchomiony):
    //   • poprawny JSON → rekord wypełniony, 200;
    //   • BRAKUJĄCE pole (tylko author) → rating i text są NULL — Spring nie zgłasza błędu, bo „brak” nie jest błędem;
    //   • pole, którego rekord nie ma ("zzz": 1) → po cichu pomijane (domyślne ustawienie Boota; Rest04Json);
    //   • zły JSON ({author:) → 400, HttpMessageNotReadableException („JSON parse error: …”);
    //   • brak ciała → 400 („Required request body is missing”); @RequestBody jest domyślnie wymagane;
    //   • tekst w miejscu liczby ("rating":"dużo") → 400.
    //
    // JACKSON 3 vs STARE TUTORIALE: pakiety zmieniły się z com.fasterxml.jackson.databind na tools.jackson.databind
    // (adnotacje, np. @JsonProperty, zostały w com.fasterxml.jackson.annotation). Boot 4 sam konfiguruje JsonMapper
    // (dawniej ObjectMapper) — w tej lekcji nie dotykasz go bezpośrednio. Dodatkowo Jackson 3 ma ostrzejsze domyślne
    // ustawienia niż Jackson 2: brak pola prymitywnego (record Score(int points), JSON {}) to w teście sekcji 4
    // błąd 400 („…FAIL_ON_NULL_FOR_PRIMITIVES…”), a nie po cichu 0, jak w starych tutorialach.
    //
    // PUŁAPKA: dane z JSON-a, które „przeszły” z null-ami, bo żadnej walidacji nie ma. Dlaczego? @RequestBody
    //   sprawdza tylko, czy JSON da się zamienić na typ — nie czy autor jest wpisany ani czy ocena jest w zakresie
    //   1–5. Takie reguły to walidacja: s05_validation/Val02ValidInControllers.
    //
    // DOBRA PRAKTYKA: jako @RequestBody używaj osobnego rekordu na dane z zapytania (DTO — Data Transfer Object, obiekt
    //   do przenoszenia danych), a nie klasy z bazy. Dlaczego? Klient mógłby wtedy ustawić pola, których nie
    //   powinien (np. id, rolę). Rekord opisuje dokładnie to, co wolno przysłać.

    // =================================================================================================
    // 5. PUŁAPKI — -parameters ORAZ FORMULARZ KONTRA JSON
    // =================================================================================================
    //
    // a) Nazwy parametrów. W @PathVariable Long id Spring musi wiedzieć, że parametr nazywa się „id”, a kompilator
    //    domyślnie ZAPOMINA nazwy parametrów (w pliku .class zostaje arg0, arg1…). Pomaga flaga kompilatora
    //    -parameters, którą włącza spring-boot-starter-parent (w IntelliJ import projektu Mavena też ją przejmuje).
    //    Test parametersFlag sprawdza: Parameter.isNamePresent() = true, nazwa „id”.
    //    Gdyby flagi zabrakło (np. projekt bez parenta Boota), pierwsze zapytanie skończy się błędem „Name for
    //    argument of type […] not specified, and parameter name information not available via reflection. Ensure that
    //    the compiler uses the '-parameters' flag.” Dawniej Spring próbował odczytać nazwy z informacji
    //    debugowania — od Spring Framework 6.1 już tego nie robi. Obejście bez flagi: nazwa jawnie w adnotacji,
    //    @PathVariable("id") / @RequestParam("q").
    //
    // b) Formularz a JSON. Dane formularza HTML (Content-Type: application/x-www-form-urlencoded, ciało "author=Ola&rating=4")
    //    Spring traktuje jak PARAMETRY zapytania — czyta je @RequestParam (metoda fromForm). @RequestBody czyta
    //    ciało jako JSON i ODRZUCA formularz. Test sekcji 5 sprawdza oba błędne kierunki:
    //      • JSON wysłany do metody z @RequestParam → 400 (parametrów „author” i „rating” nie znaleziono);
    //      • formularz wysłany do metody z @RequestBody (tylko JSON) → 415 Unsupported Media Type.
    //    Zasada: API dla programów (JavaScript, aplikacje mobilne) — JSON + @RequestBody; klasyczne formularze HTML
    //    — @RequestParam.
    //
    // PUŁAPKA: kopiowanie przykładu z formularza do API JSON (albo odwrotnie) i zdziwienie, że „wszystkie pola są
    //   puste” lub że jest 400/415. Dlaczego? Kod odpowiedzi 415 i komunikat „Required request parameter… is not
    //   present” to wskazówka, że sposób wysłania danych (Content-Type) nie zgadza się z adnotacją — sprawdź nagłówek
    //   Content-Type zapytania w pierwszej kolejności.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @PathVariable — identyfikator zasobu ze ścieżki; zły typ (abc dla Long) → 400.
     *   • @RequestParam — parametr po „?”; domyślnie WYMAGANY (brak → 400); required=false / Optional / defaultValue
     *     czynią go opcjonalnym; lista: ?tag=a&tag=b lub ?tag=a,b; ?x= daje "" (nie null).
     *   • @RequestHeader — nagłówek (nazwa z myślnikami podawana jawnie); @CookieValue — ciasteczko.
     *   • @RequestBody — ciało JSON → rekord (Jackson 3); brak pola = null (referencyjne) lub 400 (prymitywne);
     *     zły JSON / brak ciała → 400; nieznane pole pomijane.
     *   • Formularz → @RequestParam; JSON → @RequestBody. Pomyłka → 400 albo 415.
     *   • Nazwy parametrów Javy działają dzięki -parameters (włącza je parent Boota); bez flagi — nazwa jawnie w adnotacji.
     *   • Testy: @WebMvcTest + MockMvc, dane przez .param(), .header(), .cookie(), .content().
     *
     * PYTANIA KONTROLNE:
     *   1. Do czego służy @PathVariable, a do czego @RequestParam? Podaj po jednym adresie jako przykład.
     *   2. Co się stanie? Klient wysyła {@code GET /rest02/products/abc}, a metoda ma {@code @PathVariable Long id}.
     *   3. Co się stanie? Metoda ma {@code @RequestParam String q}, a klient wywołuje adres bez parametru q.
     *   4. Co się stanie? Klient wysyła {@code GET /rest02/products?category=} (pusty parametr), a kod sprawdza
     *      {@code if (category == null) { zwróć wszystko }}.
     *   5. Na trzy sposoby zrób parametr opcjonalnym. Który wybierzesz, gdy brak parametru ma oznaczać „10 sztuk”?
     *   6. ZNAJDŹ BŁĄD: klient wysyła JSON {@code {"author":"Ola"}} do metody
     *      {@code create(@RequestBody ReviewRequest r)}, a programista narzeka, że dostaje błąd, „bo brakuje oceny”.
     *      Co naprawdę się dzieje z brakującą oceną typu Integer? A co, gdyby pole miało typ int?
     *   7. ZNAJDŹ BŁĄD: formularz HTML wysyła {@code author=Ola&rating=4}, a metoda ma {@code @RequestBody ReviewRequest}.
     *      Jaki kod odpowiedzi dostanie przeglądarka i dlaczego?
     *   8. Dlaczego {@code @PathVariable Long id} działa bez podawania nazwy w adnotacji i kiedy przestałoby działać?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, testy: Rest02RequestDataExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Mapowania metod w ExerciseController są gotowe — dopisujesz adnotacje przy parametrach i treść metod.
    // Rozwiązania wzorcowe: Rest02SolutionsController (s04_web_rest.solutions.rest02_request_data).
    //
    // ĆWICZENIE 1 (łatwe): order(customerId, orderId) ma zwrócić "klient 7, zamówienie 12" dla
    //   GET /rest02/cw/customers/7/orders/12. Podpowiedź: dwie zmienne w ścieżce → dwa @PathVariable (sekcja 1).
    //
    // ĆWICZENIE 2 (łatwe): greet(name) ma zwrócić "Cześć, Ola!" dla ?name=Ola, a bez parametru "Cześć, gościu!".
    //   Podpowiedź: @RequestParam z atrybutem defaultValue (sekcja 2).
    //
    // ĆWICZENIE 3 (średnie): lang(language) ma zwrócić "język: " + wartość nagłówka X-Lang; bez nagłówka ma być 400.
    //   Podpowiedź: @RequestHeader("X-Lang") — nagłówek jest domyślnie wymagany (sekcja 3).
    //
    // ĆWICZENIE 4 (średnie): coupon(Coupon) ma zamienić JSON {"code":"ZIMA10","percent":10} na "kod ZIMA10: -10%".
    //   Podpowiedź: adnotacja dla ciała zapytania; rekord Coupon jest już w szkielecie (sekcja 4).
    //
    // ĆWICZENIE 5 (trudniejsze): sum() ma przyjąć listę liczb z powtórzonego parametru n i zwrócić ich sumę jako napis:
    //   ?n=1&n=2&n=3 → "6"; ?n=abc → 400. Podpowiedź: dopisz do metody parametr List<Integer> n (bez parametrów) z adnotacją
    //   @RequestParam; zamianę tekstu na liczby wykona Spring (sekcja 2).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @PathVariable czyta wartość ze ścieżki i IDENTYFIKUJE zasób: /rest02/products/3. @RequestParam czyta
     *      parametr po „?” i zawęża/ustawia widok: /rest02/products?category=napoje&limit=5.
     *   2. 400 Bad Request — "abc" nie da się zamienić na Long (MethodArgumentTypeMismatchException); metoda kontrolera
     *      nie jest wywołana. Sprawdzone testem (sekcja 1).
     *   3. 400 Bad Request — @RequestParam jest domyślnie wymagany (MissingServletRequestParameterException).
     *      Sprawdzone testem (sekcja 2).
     *   4. Parametr jest obecny, więc category to pusty napis "", nie null; warunek {@code category == null} się nie
     *      spełni i filtr „kategoria = ''” zwróci pustą listę (test sekcji 2: 0 produktów). Napraw: sprawdzaj też isBlank().
     *   5. required = false (brak → null), Optional (brak → Optional.empty()), defaultValue (brak → wartość domyślna).
     *      Dla „brak = 10 sztuk” — {@code @RequestParam(defaultValue = "10") int limit}: nie trzeba obsługiwać null.
     *   6. Nic złego: brakujące pole referencyjne (Integer) to po prostu null (test sekcji 4) — błąd, jeśli taki jest,
     *      pochodzi z Twojej logiki, którą trzeba zabezpieczyć walidacją (Val02ValidInControllers). Gdyby pole miało typ int,
     *      Jackson 3 odrzuciłby zapytanie (400, FAIL_ON_NULL_FOR_PRIMITIVES) — Jackson 2 wstawiłby po cichu 0.
     *   7. 415 Unsupported Media Type — @RequestBody czyta JSON, a formularz ma Content-Type application/x-www-form-urlencoded.
     *      Sprawdzone testem (sekcja 5). Dla formularza użyj @RequestParam (albo @ModelAttribute).
     *   8. Spring odczytuje nazwę parametru Javy (id) z pliku .class, bo kompilator dostał flagę -parameters
     *      (spring-boot-starter-parent włącza ją). Bez flagi nazw nie ma w pliku i dostaniesz błąd „parameter name
     *      information not available” — wtedy podaj nazwę w adnotacji: {@code @PathVariable("id")}.
     */
    // </editor-fold>
}

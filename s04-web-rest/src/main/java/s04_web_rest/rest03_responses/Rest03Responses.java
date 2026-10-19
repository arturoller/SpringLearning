package s04_web_rest.rest03_responses;

/**
 * <pre>
 * TEMAT: Odpowiedzi HTTP — {@code ResponseEntity}, kody statusu (200, 201 + Location, 204, 404, 409),
 *        {@code @ResponseStatus}, {@code ResponseStatusException}, produces/consumes (406, 415) i pułapka zwracania null
 *        (response entity = encja odpowiedzi; status code = kod statusu; produces = produkuje; consumes = konsumuje)
 *
 * W SKRÓCIE:
 *   Odpowiedź HTTP to trzy rzeczy: STATUS (liczba mówiąca, co się stało), NAGŁÓWKI (np. Location, Content-Type) i CIAŁO.
 *   Gdy metoda kontrolera zwraca zwykły obiekt, Spring ustawia 200 i wpisuje obiekt do ciała. Gdy chcesz decydować
 *   o statusie i nagłówkach — zwracasz {@code ResponseEntity<T>}. Błędy zgłaszasz wyjątkiem:
 *   {@code ResponseStatusException(HttpStatus.NOT_FOUND, "...")} albo własnym wyjątkiem z {@code @ResponseStatus}.
 *   produces/consumes w mapowaniu mówią, jakie typy zawartości metoda oddaje i przyjmuje — niezgodność kończy się 406
 *   albo 415, zanim metoda ruszy. Zwrócenie null to cichy błąd: klient dostaje 200 i puste ciało.
 *
 * ANALOGIA: okienko na poczcie.
 *   Status to odpowiedź urzędnika jednym słowem: „proszę” (200), „nadane — oto numer” (201 + Location, czyli adres,
 *   pod którym śledzisz przesyłkę), „załatwione, nic więcej” (204), „nie ma takiej paczki” (404), „taka paczka już jest”
 *   (409). Nagłówki to pieczątki na kwicie, ciało — sam kwit lub paczka. produces/consumes to tabliczka „przyjmujemy
 *   tylko listy polecone” (415, gdy przyniesiesz coś innego) i „wydajemy tylko na papierze” (406, gdy chcesz e-mail).
 *
 * JAK TO DZIAŁA:
 *   Zwykły obiekt (Product) → RequestResponseBodyMethodProcessor wpisuje go do ciała przez HttpMessageConverter; status 200.
 *   {@code ResponseEntity} → HttpEntityMethodProcessor bierze z niego status i nagłówki, a ciało pisze tym samym konwerterem.
 *   Wyjątek z kontrolera → łańcuch HandlerExceptionResolver (rozwiązywaczy wyjątków):
 *     ResponseStatusExceptionResolver obsługuje ResponseStatusException i wyjątki z {@code @ResponseStatus},
 *     DefaultHandlerExceptionResolver — wyjątki Springa (405, 406, 415, 400...). Oba wołają response.sendError(status),
 *     a na prawdziwym serwerze Tomcat przekazuje zapytanie pod /error, gdzie Boot buduje JSON błędu.
 *   produces/consumes są WARUNKAMI MAPOWANIA (jak metoda HTTP): RequestMappingHandlerMapping porównuje je z nagłówkami
 *   Accept i Content-Type zapytania przy wyborze metody.
 *
 * SŁÓWKA:
 *   created = utworzono (201); no content = brak treści (204); conflict = konflikt (409); not acceptable = nie do
 *   przyjęcia (406 — serwer nie umie oddać typu, którego chce klient); unsupported media type = nieobsługiwany typ
 *   zawartości (415); location = położenie (adres nowego zasobu); reason = powód; content negotiation = negocjacja
 *   treści (wybór formatu odpowiedzi na podstawie nagłówka Accept); builder = budowniczy (obiekt do składania krok po kroku).
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest01Controllers (mapowanie, 404/405), s04_web_rest/Rest02RequestData (dane zapytania),
 *             s04_web_rest/Rest05CrudApi (statusy w pełnym CRUD), s06_error_handling/Err01DefaultErrors (strona /error
 *             Boota), s06_error_handling/Err04ProblemDetail (ProblemDetail — standardowe ciało błędu),
 *             t14_optional/Optional02Transform (Optional.map/orElseGet), t28_networking_http/Http04JsonApi.
 * </pre>
 */
public final class Rest03Responses {

    private Rest03Responses() {
    }

    // =================================================================================================
    // 1. ResponseEntity — status, nagłówki i ciało w jednym obiekcie (ProductController.all, one)
    // =================================================================================================
    //
    //   ResponseEntity.ok(body)                                   200 + ciało
    //   ResponseEntity.ok().header("X-Total-Count", "3").body(l)  200 + własny nagłówek + ciało
    //   ResponseEntity.created(uri).body(body)                    201 + Location + ciało
    //   ResponseEntity.noContent().build()                        204, bez ciała
    //   ResponseEntity.notFound().build()                         404, bez ciała
    //   ResponseEntity.status(HttpStatus.CONFLICT).body(...)      dowolny status
    //   ResponseEntity.of(optional)                               Optional z wartością → 200 + ciało, pusty → 404
    //
    // Typ w nawiasach ostrych to typ CIAŁA: ResponseEntity<Product>, ResponseEntity<List<Product>>, ResponseEntity<Void>
    // (Void = „bez ciała”, np. dla 204). build() kończy budowę odpowiedzi bez ciała, body(...) — z ciałem.
    //
    // DOBRA PRAKTYKA: ResponseEntity tylko tam, gdzie status albo nagłówki ZALEŻĄ od wyniku (200 albo 404, 201 + Location).
    //   Dlaczego? Gdy zawsze jest 200, zwykły typ (Product, List<Product>) jest krótszy i czytelniejszy.

    // =================================================================================================
    // 2. 201 Created + nagłówek Location (ProductController.create, quickCreate)
    // =================================================================================================
    //
    // Po utworzeniu zasobu REST odpowiada 201 Created i podaje w nagłówku Location ADRES nowego zasobu — klient nie musi
    // zgadywać, gdzie go szukać. ServletUriComponentsBuilder (budowniczy adresów) składa ten adres z bieżącego zapytania:
    //   ServletUriComponentsBuilder.fromCurrentRequest()   → http://localhost:8080/api/products   (adres zapytania POST)
    //       .path("/{sku}")                                → http://localhost:8080/api/products/{sku}
    //       .buildAndExpand(product.sku())                 → http://localhost:8080/api/products/ODZ-002
    //       .toUri()
    // W MockMvc serwer to „localhost” bez portu: Location = http://localhost/api/products/ODZ-002.
    //
    // @ResponseStatus(HttpStatus.CREATED) NA METODZIE (quickCreate) — status 201 bez ResponseEntity. Krócej, ale nie
    // dodasz nagłówka Location ani nie zmienisz statusu zależnie od wyniku.
    //
    // PUŁAPKA: Location sklejony ręcznie z "/api/products/" + sku. Dlaczego gorzej? Gdy aplikacja działa pod innym
    //   adresem (kontekst /sklep, serwer pośredni, inny port), ręczny adres jest błędny; ServletUriComponentsBuilder
    //   bierze schemat, host, port i ścieżkę z prawdziwego zapytania (i koduje znaki specjalne).

    // =================================================================================================
    // 3. 204 No Content i 404 przy DELETE (ProductController.delete)
    // =================================================================================================
    //
    // DELETE, który się udał, nie ma czego pokazać → 204 No Content i ResponseEntity<Void>. Drugi DELETE tego samego
    // produktu → 404 (już go nie ma). Czy to łamie idempotentność DELETE? Nie — idempotentność dotyczy STANU serwera
    // (po 1 i po 2 wywołaniach produktu nie ma), a nie identycznych kodów odpowiedzi. Szerzej w s04_web_rest/Rest05CrudApi.

    // =================================================================================================
    // 4. TRZY SPOSOBY NA 404 (one, details, stock)
    // =================================================================================================
    //
    //   a) ResponseEntity.notFound().build() / ResponseEntity.of(Optional.empty())   — zwracasz odpowiedź (one)
    //   b) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma produktu " + sku)   — wyjątek Springa (details)
    //   c) throw new ProductNotFoundException(sku)   — własny wyjątek z @ResponseStatus(code = NOT_FOUND) na klasie (stock)
    //
    // a) jest najprostsze w jednym kontrolerze. b) i c) działają z każdej głębokości — także z serwisu wywołanego przez
    // kontroler — bo wyjątek „przelatuje” do Springa. c) daje nazwę błędu z dziedziny sklepu (ProductNotFoundException),
    // ale wiąże klasę wyjątku z HTTP; elegantsze rozwiązanie (@ExceptionHandler, @RestControllerAdvice) — s06_error_handling.
    //
    // Co widzi klient? W MockMvc tylko status (powód jest w response.getErrorMessage(), ciało puste). Na prawdziwym
    // serwerze Boot odpowiada JSON-em {"timestamp":...,"status":404,"error":"Not Found","path":...} — BEZ tekstu powodu
    // (Boot domyślnie go ukrywa: server.error.include-message=never — s06_error_handling/Err01DefaultErrors).
    //
    // PUŁAPKA: @ResponseStatus(reason = "...") na klasie wyjątku. Dlaczego uważać? Spring używa wtedy STAŁEGO powodu
    //   „Nie ma takiego produktu”, a komunikat konkretnego wyjątku („Nie ma produktu XXX”) jako powód odpowiedzi znika.
    //   Test reasonFromAnnotationWins to pokazuje.

    // =================================================================================================
    // 5. produces i consumes — 406 i 415 (ProductController.one/oneAsText, create)
    // =================================================================================================
    //
    // produces = MediaType.APPLICATION_JSON_VALUE → metoda oddaje JSON. Klient z Accept: application/xml → żadna metoda
    // nie umie oddać XML → 406 Not Acceptable (HttpMediaTypeNotAcceptableException: „No acceptable representation”).
    // Dwie metody na ten SAM adres z różnym produces (one i oneAsText) → Spring wybiera po nagłówku Accept:
    //   Accept: application/json → JSON, Accept: text/plain → „Laptop Pro 14 — 5499.99 zł”, Accept: application/xml → 406.
    // Bez Accept (albo */*) pasują obie; przy remisie Spring porównuje typy alfabetycznie, więc wygrywa application/json.
    //
    // consumes = MediaType.APPLICATION_JSON_VALUE → metoda przyjmuje tylko JSON. Content-Type: text/plain → 415
    // Unsupported Media Type (HttpMediaTypeNotSupportedException: „Content-Type 'text/plain' is not supported”),
    // a w odpowiedzi nagłówek Accept: application/json podpowiada, co serwer przyjmie.
    //
    // PUŁAPKA: 406 na prawdziwym serwerze ma PUSTE ciało. Dlaczego? Boot chce odpowiedzieć JSON-em błędu, ale klient
    //   właśnie powiedział, że JSON-a nie przyjmie (Accept: application/xml). Zostaje sam status — sprawdź nagłówek Accept.

    // =================================================================================================
    // 6. PUŁAPKA: return null (NullPitfallController)
    // =================================================================================================
    //
    //   @GetMapping("/null-product/{sku}")
    //   Product nullProduct(...) { return found ? LAPTOP : null; }   → nieznany sku: 200 OK i PUSTE ciało (Content-Length: 0)
    //
    // To samo dla ResponseEntity<Product> zwracającego null. Dlaczego to groźne? Klient widzi „sukces”, próbuje czytać
    // JSON z pustego ciała i wywraca się w zupełnie innym miejscu. Brak zasobu to 404 — zwróć ResponseEntity.notFound(),
    // ResponseEntity.of(optional) albo rzuć wyjątek (sekcja 4). Null z kontrolera prawie zawsze jest błędem.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • ResponseEntity.ok(x) / created(uri).body(x) / noContent().build() / notFound().build() / of(optional) /
     *     status(HttpStatus.X).header(...).body(x).
     *   • 201 + Location: ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri().
     *   • @ResponseStatus(HttpStatus.CREATED) na metodzie — stały status bez ResponseEntity.
     *   • 404 wyjątkiem: ResponseStatusException(HttpStatus.NOT_FOUND, "...") albo własny wyjątek z @ResponseStatus.
     *   • produces = typ ODPOWIEDZI (Accept), consumes = typ CIAŁA zapytania (Content-Type); niezgodność → 406 / 415.
     *   • Ten sam adres + różne produces = negocjacja treści po nagłówku Accept.
     *   • return null → 200 i puste ciało — błąd; używaj 404.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie trzy części ma odpowiedź HTTP i jak ustawić każdą z nich w ResponseEntity?
     *   2. Co się stanie? POST /api/products z produktem ODZ-002 (Content-Type: application/json) — jaki status, jaki
     *      nagłówek Location (w MockMvc) i co w ciele?
     *   3. Co się stanie? Ten sam POST wyślesz drugi raz.
     *   4. ZNAJDŹ BŁĄD: metoda GET /api/pitfalls/null-product/{sku} zwraca null, gdy produktu nie ma. Co dostaje klient
     *      i dlaczego to błąd?
     *   5. Co się stanie? GET /api/products/ELE-001 z nagłówkiem Accept: application/xml — status i ciało na
     *      prawdziwym serwerze.
     *   6. Czym różni się 406 od 415? Który nagłówek zapytania „winny” jest każdemu z nich?
     *   7. Kiedy @ResponseStatus na metodzie, a kiedy ResponseEntity?
     *   8. Czy DELETE, który za pierwszym razem daje 204, a za drugim 404, jest idempotentny?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w klasie Rest03ExerciseController (adresy /api/cw03/...), testy: Rest03ResponsesExercisesTest.
    // Rozwiązania wzorcowe: s04_web_rest.solutions.rest03_responses.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Status, nagłówki, ciało. ResponseEntity.status(HttpStatus.CREATED) (albo ok(), created(uri), noContent()...),
     *      .header("Nazwa", "wartość") / .location(uri), a na końcu .body(obiekt) albo .build() (bez ciała).
     *   2. 201 Created, Location: http://localhost/api/products/ODZ-002 (MockMvc nie ma portu; na prawdziwym serwerze
     *      z portem, np. http://localhost:8080/api/products/ODZ-002), a w ciele JSON utworzonego produktu.
     *      Sprawdzone testem createReturns201WithLocation.
     *   3. 409 Conflict — kontroler rzuca ResponseStatusException(HttpStatus.CONFLICT, "Produkt ODZ-002 już istnieje").
     *      POST nie jest idempotentny; tu drugi raz nie tworzy duplikatu, tylko zgłasza konflikt. Sprawdzone testem
     *      secondCreateGives409.
     *   4. 200 OK i puste ciało (Content-Length: 0). Klient myśli, że się udało, a potem nie może odczytać JSON-a.
     *      Brak zasobu to 404. Sprawdzone testami nullReturnGives200WithEmptyBody (MockMvc) i
     *      Rest03ResponsesRealServerTest (prawdziwy serwer).
     *   5. 406 Not Acceptable i PUSTE ciało — Boot nie może wysłać JSON-a błędu klientowi, który JSON-a nie przyjmuje.
     *      W odpowiedzi nagłówek Accept z typami application/json i text/plain (ich kolejność bywa różna).
     *      Sprawdzone testem Rest03ResponsesRealServerTest.
     *   6. 406 — serwer nie umie oddać typu, którego klient chce (nagłówek Accept). 415 — serwer nie umie przyjąć typu,
     *      który klient wysłał (nagłówek Content-Type).
     *   7. @ResponseStatus — gdy status jest zawsze ten sam (np. zawsze 201) i nie potrzebujesz nagłówków.
     *      ResponseEntity — gdy status lub nagłówki zależą od wyniku (200/404, 201 + Location, X-Total-Count).
     *   8. Tak. Idempotentność dotyczy stanu serwera: po pierwszym i po drugim DELETE produktu nie ma. Różne kody
     *      odpowiedzi są dozwolone (RFC 9110 — specyfikacja HTTP).
     */
    // </editor-fold>
}

package s04_web_rest.rest05_crud_api;

/**
 * <pre>
 * TEMAT: Pełne API CRUD produktów — DTO oddzielone od modelu, repozytorium w pamięci, kody statusu każdej operacji,
 *        idempotentność PUT i DELETE, PATCH jako częściowa zmiana
 *        (CRUD = Create, Read, Update, Delete — utwórz, odczytaj, zmień, usuń; DTO = Data Transfer Object — obiekt
 *        do przenoszenia danych; idempotent = idempotentny, czyli „powtórzenie nie zmienia wyniku”)
 *
 * W SKRÓCIE:
 *   Typowe API zasobu to sześć punktów końcowych: lista, jeden element, utworzenie, zastąpienie, częściowa zmiana,
 *   usunięcie. Każdy ma swój kod statusu (200, 201 + Location, 204, 404, 409). Kontroler przyjmuje i zwraca DTO —
 *   osobne rekordy na każdą operację — a nie model, którego używa sklep w środku. Dzięki temu klient nie zobaczy ceny
 *   zakupu i nie nadpisze pól, których nie powinien. PUT i DELETE są idempotentne (powtórzone dają ten sam STAN),
 *   POST — nie. PATCH zmienia tylko podane pola.
 *
 * ANALOGIA: karta katalogowa w bibliotece.
 *   Bibliotekarz ma w szafie pełną kartę książki (model: z ceną zakupu i uwagami wewnętrznymi). Czytelnikowi pokazuje
 *   wydruk z wybranymi polami (DTO odpowiedzi). Gdy czytelnik zgłasza nową książkę, wypełnia formularz (DTO żądania),
 *   na którym NIE MA rubryki „cena zakupu” — więc nie może jej wpisać. „Wymień kartę na nową” to PUT (cała karta),
 *   „popraw tylko stan egzemplarzy” to PATCH, „wycofaj z katalogu” to DELETE.
 *
 * JAK TO DZIAŁA:
 *   Zapytanie → ProductCrudController (warstwa webowa: adresy, statusy, DTO) → ProductRepository (dane: tu mapa
 *   w pamięci, od s07 — baza) → model Product → ProductResponse.from(product) → JSON. Kontroler dostaje repozytorium
 *   przez konstruktor (wstrzykiwanie zależności — s01_core_ioc/Ioc02ConstructorInjection). W prawdziwej aplikacji
 *   między nimi jest jeszcze serwis z logiką biznesową — tu pomijamy go, żeby skupić się na HTTP.
 *
 * SŁÓWKA:
 *   resource = zasób (rzecz pod adresem, np. produkt); collection = kolekcja (/api/products); replace = zastąpić;
 *   partial update = częściowa zmiana; conflict = konflikt (409); over-posting = „nadmiarowe pola” (klient wysyła
 *   pola, których nie powinien zmieniać); race condition = wyścig (dwa wątki naraz psują sobie dane);
 *   safe method = metoda bezpieczna (nic nie zmienia: GET); repository = repozytorium (miejsce przechowywania danych).
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest03Responses (ResponseEntity i statusy), s04_web_rest/Rest04Json (Jackson i @JsonIgnore),
 *             t34_toward_spring/Spring02Layers (warstwy aplikacji), t35_capstone/Capstone04RestTodo (REST bez Springa),
 *             t21_concurrency/Concurrency06ConcurrentCollections (ConcurrentHashMap), s07_data_jdbc/Jdbc05Repository
 *             (repozytorium na prawdziwej bazie), s05_validation/Val02ValidInControllers (walidacja DTO).
 * </pre>
 */
public final class Rest05CrudApi {

    private Rest05CrudApi() {
    }

    // =================================================================================================
    // 1. SZEŚĆ PUNKTÓW KOŃCOWYCH CRUD I ICH KODY (ProductCrudController)
    // =================================================================================================
    //
    //   operacja      metoda i adres              sukces                 błąd
    //   lista         GET    /api/products        200 + tablica          —
    //   jeden         GET    /api/products/{sku}  200 + produkt          404 (nie ma)
    //   utwórz        POST   /api/products        201 + Location + body  409 (sku zajęty), 400 (zły JSON)
    //   zastąp        PUT    /api/products/{sku}  200 + produkt          404 (nie ma), 400 (brak pola)
    //   zmień część   PATCH  /api/products/{sku}  200 + produkt          404 (nie ma)
    //   usuń          DELETE /api/products/{sku}  204, bez ciała         404 (nie ma)
    //
    // Pusta lista to 200 i [], a nie 404 ani 204 — kolekcja istnieje, tylko nic w niej nie ma.
    // PUT na nieistniejący sku daje tu 404: sku nadaje katalog, więc tworzenie idzie przez POST. (Są API, w których PUT
    // tworzy zasób pod wskazanym adresem — wtedy odpowiada 201. Ważne, żeby zachowanie było opisane i stałe.)

    // =================================================================================================
    // 2. DTO ODDZIELONE OD MODELU (Product vs ProductDtos; testy responseIsDtoNotModel, dtoProtectsFields)
    // =================================================================================================
    //
    //   model Product:                sku, name, category, price, purchasePrice, stock   ← zna sklep
    //   CreateProductRequest:         sku, name, category, price, purchasePrice, stock   ← POST (wszystko)
    //   ReplaceProductRequest:        name, category, price, purchasePrice, stock        ← PUT (bez sku — jest w adresie)
    //   PatchProductRequest:          name, price, stock (wszystkie mogą być null)        ← PATCH
    //   ProductResponse:              sku, name, category, price, stock, available       ← odpowiedź (bez purchasePrice)
    //
    // Dlaczego nie zwracać modelu? (1) Ukrywasz dane wewnętrzne (purchasePrice). (2) Możesz zmienić model (np. rozbić
    // price na netto i VAT) bez łamania klientów. (3) Odpowiedź może mieć pola wyliczone (available).
    // Dlaczego osobne DTO na każde żądanie? Każda operacja przyjmuje DOKŁADNIE to, co wolno zmienić. Pole, którego nie
    // ma w rekordzie, Jackson po cichu pominie (nieznane pola są ignorowane — s04_web_rest/Rest04Json).
    //
    // PUŁAPKA: over-posting (nadmiarowe pola). Kontroler przyjmuje model: @RequestBody Product product w PATCH.
    //   Dlaczego groźne? Klient dopisze do JSON-a "purchasePrice":1.00 i zmieni dane, do których nie miał prawa.
    //   Z PatchProductRequest (bez purchasePrice) to pole po prostu nie istnieje — test dtoProtectsFields.
    //
    // DOBRA PRAKTYKA: konwersja model ↔ DTO w jednym miejscu (ProductResponse.from, request.toProduct()). Dlaczego?
    //   Gdy dojdzie pole, zmieniasz jedną metodę, a nie każdy endpoint. W większych projektach pomaga biblioteka
    //   MapStruct, ale ręczne metody w rekordach w zupełności wystarczą.

    // =================================================================================================
    // 3. REPOZYTORIUM W PAMIĘCI: ConcurrentHashMap + sortowanie przy odczycie (ProductRepository)
    // =================================================================================================
    //
    // Repozytorium jest singletonem używanym przez wiele wątków naraz → ConcurrentHashMap. Mapa nie pamięta kolejności,
    // więc findAll() sortuje po sku — klient (i test) zawsze dostaje listę w tym samym porządku.
    //
    // PUŁAPKA: (PYTANIE REKRUTACYJNE: „co to jest check-then-act?”) „sprawdź, potem zrób” na dwóch osobnych operacjach:
    //     if (!map.containsKey(sku)) { map.put(sku, product); }      ← dwa zapytania POST naraz mogą OBA przejść if-a
    //   Dlaczego? Między containsKey a put drugi wątek może wstawić ten sam sku — oba dostaną 201, a jeden produkt
    //   zniknie. Rozwiązanie: jedna atomowa operacja mapy: putIfAbsent (saveIfAbsent przy POST) i replace
    //   (replaceIfPresent przy PUT). W bazie danych zadba o to ograniczenie UNIQUE i transakcja (s10_transactions).

    // =================================================================================================
    // 4. IDEMPOTENTNOŚĆ (testy postTwiceGives409, putIsIdempotent, deleteIsIdempotentButStatusDiffers)
    // =================================================================================================
    //
    // Idempotentna operacja: wykonana raz albo wiele razy zostawia serwer w TYM SAMYM STANIE. Ważne, bo sieć zawodzi:
    // klient, który nie dostał odpowiedzi, chce bezpiecznie powtórzyć zapytanie.
    //   GET     — bezpieczny (nic nie zmienia) i idempotentny,
    //   PUT     — idempotentny: „ustaw produkt na TAKI” dwa razy = ten sam produkt,
    //   DELETE  — idempotentny: po pierwszym i drugim wywołaniu produktu nie ma (choć kody to 204, potem 404),
    //   POST    — NIE jest idempotentny: drugi raz tworzy nowy zasób albo (u nas) kończy się 409 Conflict,
    //   PATCH   — zależy od treści: {"stock":0} jest idempotentny, a {"stockDelta":-1} („zmniejsz o 1”) — nie.
    //
    // PUŁAPKA: (PYTANIE REKRUTACYJNE: „czy DELETE jest idempotentny, skoro drugi raz zwraca 404?”) — TAK. Idempotentność
    //   dotyczy STANU serwera, nie identycznej odpowiedzi. Dlaczego to ważne? Klient po 404 przy ponownym DELETE nie
    //   powinien zgłaszać błędu — cel („produktu nie ma”) został osiągnięty.

    // =================================================================================================
    // 5. PUT KONTRA PATCH (testy putRequiresAllFields, patchChangesOnlyGivenFields)
    // =================================================================================================
    //
    // PUT = „oto CAŁY nowy produkt”. Brakujące pole to błąd (stock jako int → Jackson 3 → 400), a nie „zostaw stare”.
    // PATCH = „zmień TYLKO to”. W PatchProductRequest wszystkie pola są obiektowe (String, BigDecimal, Integer), więc
    // brak pola = null = „bez zmian”; pusty PATCH {} niczego nie zmienia.
    //
    // PUŁAPKA: przez taki PATCH nie da się ustawić pola NA null — null znaczy „nie zmieniaj”. Dlaczego to ma znaczenie?
    //   Jeśli kiedyś pole „opis” ma dać się wyczyścić, potrzebujesz innej umowy: np. pusty tekst "" albo standard
    //   JSON Merge Patch (RFC 7396, typ application/merge-patch+json), w którym null jawnie oznacza „usuń wartość”.
    //
    // DOBRA PRAKTYKA: identyfikator zasobu tylko w adresie (PUT /api/products/{sku}), nie w ciele. Dlaczego? Gdy jest
    //   w obu miejscach, prędzej czy później ktoś wyśle różne wartości. ReplaceProductRequest nie ma pola sku, a "sku"
    //   przysłane w ciele Jackson pominie (test dtoProtectsFields).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • GET lista 200 · GET jeden 200/404 · POST 201 + Location/409 · PUT 200/404 · PATCH 200/404 · DELETE 204/404.
     *   • Kontroler przyjmuje i zwraca DTO (rekordy), nigdy model. Osobny rekord na każdą operację.
     *   • Odpowiedź: ProductResponse.from(model) — ukrywa pola wewnętrzne, dodaje wyliczone.
     *   • Mapa w pamięci: ConcurrentHashMap + sortowanie przy odczycie; atomowo: putIfAbsent, replace, remove.
     *   • Idempotentne: GET, PUT, DELETE (ten sam STAN po powtórzeniu). Nieidempotentny: POST. PATCH — zależy.
     *   • PUT = całość (brak pola → 400), PATCH = część (null = bez zmian).
     *   • Identyfikator w adresie, nie w ciele PUT/PATCH.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie kody statusu zwraca każda z sześciu operacji CRUD w tej lekcji (sukces i błąd)?
     *   2. Co się stanie? Wyślesz dwa razy ten sam POST /api/products (z tym samym sku).
     *   3. Co się stanie? Wyślesz dwa razy DELETE /api/products/KSI-001 — jakie statusy i jaki stan serwera?
     *   4. ZNAJDŹ BŁĄD: metoda POST robi if (!products.containsKey(sku)) products.put(sku, product); — przy dużym ruchu
     *      dwóch klientów dostaje 201 dla tego samego sku, a jeden produkt znika.
     *   5. ZNAJDŹ BŁĄD: @PatchMapping("/{sku}") ResponseEntity<Product> patch(@RequestBody Product product) — co może
     *      zrobić złośliwy klient i co jeszcze jest nie tak z typem odpowiedzi?
     *   6. Czym różni się PUT od PATCH? Co się stanie, gdy w PUT zabraknie pola stock?
     *   7. Dlaczego pusta lista to 200 [], a nie 404?
     *   8. Czy PATCH {"stockDelta": -1} byłby idempotentny? Dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w klasie Rest05ExerciseController (klienci pod /api/cw05/customers), testy: Rest05CrudApiExercisesTest.
    // Rozwiązania wzorcowe: s04_web_rest.solutions.rest05_crud_api.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. GET lista — 200; GET jeden — 200 albo 404; POST — 201 + Location albo 409 (sku zajęty) / 400 (zły JSON);
     *      PUT — 200 albo 404 / 400 (brak pola); PATCH — 200 albo 404; DELETE — 204 albo 404.
     *   2. Pierwszy: 201 Created + Location. Drugi: 409 Conflict — saveIfAbsent zwraca false, bo sku jest zajęty.
     *      POST nie jest idempotentny. Sprawdzone testem postTwiceGives409.
     *   3. Pierwszy DELETE: 204 No Content, drugi: 404 Not Found. Stan po obu ten sam — produktu nie ma — więc DELETE
     *      jest idempotentny. Sprawdzone testem deleteIsIdempotentButStatusDiffers.
     *   4. Check-then-act: dwa wątki mogą jednocześnie sprawdzić containsKey (oba false) i oba wstawić produkt — drugi
     *      nadpisze pierwszy. Poprawka: jedna atomowa operacja — putIfAbsent (zwraca null, gdy wstawiono).
     *   5. Model w @RequestBody pozwala klientowi zmienić KAŻDE pole, także purchasePrice (over-posting). Do tego
     *      ResponseEntity<Product> wysyła klientowi purchasePrice. Poprawka: PatchProductRequest na wejściu
     *      i ProductResponse na wyjściu.
     *   6. PUT zastępuje cały zasób (wszystkie pola wymagane), PATCH zmienia tylko podane pola. Brak stock (int) w PUT →
     *      400 Bad Request (Jackson 3: Cannot map `null` into type `int`). Sprawdzone testem putRequiresAllFields.
     *   7. Kolekcja /api/products istnieje — tylko jest pusta. 404 znaczyłoby „nie ma takiego adresu/zasobu”, co
     *      wprowadzałoby klienta w błąd. Pusta tablica jest też wygodna: klient nie musi obsługiwać wyjątku.
     *   8. Nie. Każde powtórzenie zmniejsza stan o 1 — trzy wysłania to inny stan niż jedno. Dlatego zmiany względne
     *      (o ile) robi się zwykle przez POST na osobny zasób (np. POST /api/products/{sku}/stock-movements).
     */
    // </editor-fold>
}

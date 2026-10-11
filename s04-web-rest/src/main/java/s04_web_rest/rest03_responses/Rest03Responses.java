package s04_web_rest.rest03_responses;

/**
 * <pre>
 * TEMAT: Odpowiedzi HTTP — ResponseEntity, kody statusu 200/201/202/204/404, nagłówek Location, @ResponseStatus,
 *        własne nagłówki i ResponseEntity.of(Optional)
 *        (response entity = encja odpowiedzi, czyli status + nagłówki + ciało; status code = kod statusu;
 *        location = położenie; created = utworzono; no content = brak treści; accepted = przyjęto)
 *
 * W SKRÓCIE:
 *   Kontroler zwracający zwykły obiekt zawsze odpowiada 200 OK. Gdy chcesz wybrać status lub dodać nagłówki,
 *   zwróć ResponseEntity — pudełko z trzema przegródkami: status, nagłówki, ciało. Wzorce REST:
 *   GET → 200 albo 404; POST → 201 Created + Location (adres nowego zasobu); DELETE → 204 No Content;
 *   przyjęte do późniejszej obróbki → 202 Accepted. @ResponseStatus ustawia stały status metody albo wyjątku.
 *
 * ANALOGIA: odpowiedź z urzędu.
 *   Zwykły obiekt to sama kartka z treścią — urzędnik zawsze dopisze „załatwiono” (200). ResponseEntity to pełne
 *   pismo: pieczątka z wynikiem (status: „utworzono”, „nie znaleziono”), adnotacje na marginesie (nagłówki:
 *   „sprawa ma numer 4 — pokój 12”, czyli Location) i treść (ciało). Pismo „usunięto” (204) nie potrzebuje treści.
 *
 * JAK TO DZIAŁA:
 *   return ResponseEntity.created(uri).body(product);
 *     → HttpEntityMethodProcessor (obsługa wyniku typu ResponseEntity) ustawia status 201 i nagłówek Location,
 *       a ciało zapisuje przez HttpMessageConverter (Jackson → JSON).
 *   return product;   (zwykły obiekt w @RestController)
 *     → RequestResponseBodyMethodProcessor zapisuje ciało; status 200 (albo ten z @ResponseStatus na metodzie).
 *   throw new ProductNotFoundException(id);   (klasa z @ResponseStatus(NOT_FOUND))
 *     → ResponseStatusExceptionResolver (rozwiązywacz wyjątków ze statusem) → 404.
 *
 * SŁÓWKA:
 *   ok = w porządku (200); created = utworzono (201); accepted = przyjęto (202); no content = brak treści (204);
 *   not found = nie znaleziono (404); location = położenie (adres nowego zasobu); builder = budowniczy (obiekt
 *   składający coś krok po kroku); current request = bieżące zapytanie; cache control = sterowanie pamięcią
 *   podręczną; max age = maksymalny wiek (ile sekund wolno pamiętać odpowiedź); header = nagłówek.
 *
 * ZOBACZ TEŻ: t34_toward_spring/Spring03RestConcepts (kody statusu w REST), t14_optional/Optional02Transform
 *             (map/orElseGet na Optional), s04_web_rest/Rest02RequestData, s04_web_rest/Rest05CrudApi (wszystko razem),
 *             s06_error_handling (odpowiedzi błędów: ProblemDetail).
 * </pre>
 */
public final class Rest03Responses {

    private Rest03Responses() {
    }

    // =================================================================================================
    // 1. ZWYKŁY OBIEKT = ZAWSZE 200 (Rest03ResponsesTest, sekcja 1)
    // =================================================================================================
    //
    // @GetMapping("/plain/{id}") public Product plain(...) — Spring zapisuje obiekt jako JSON i odpowiada 200 OK.
    // To wygodne, dopóki zawsze jest co zwrócić.
    //
    // PUŁAPKA: zwrócenie null z metody @RestController NIE daje 404, tylko 200 OK z PUSTYM ciałem. Dlaczego to groźne?
    //   Klient widzi „sukces”, próbuje czytać JSON z pustego ciała i wywraca się u siebie — a w logach serwera nic.
    //   GET /api/products/plain/99 → 200 i puste ciało. Sprawdzone testem. Brak zasobu = 404, czyli ResponseEntity
    //   albo wyjątek z @ResponseStatus.

    // =================================================================================================
    // 2. ResponseEntity i ResponseEntity.of(Optional) — 200 albo 404 (sekcja 2)
    // =================================================================================================
    //
    //   return ResponseEntity.of(store.findById(id));          Optional pełny → 200 + ciało; pusty → 404 bez ciała
    //
    //   return store.findById(id)                              to samo ręcznie — gdy potrzebujesz czegoś więcej
    //           .map(p -> ResponseEntity.ok().header("X-...", ...).body(p))       (np. nagłówka tylko przy 200)
    //           .orElseGet(() -> ResponseEntity.notFound().build());
    //
    // Fabryki ResponseEntity: ok(body), ok().header(...).body(...), created(uri), accepted(), noContent(),
    // badRequest(), notFound(), status(HttpStatus.XXX). Te bez ciała kończysz build(), te z ciałem — body(...).
    //
    // DOBRA PRAKTYKA: repozytorium/magazyn zwraca Optional, a kontroler zamienia go na ResponseEntity.of(...).
    //   Dlaczego? Brak wyniku jest widoczny w typie (Optional), więc nie da się go „zgubić” jak null, a kontroler ma
    //   jedną linię zamiast if-a. PYTANIE REKRUTACYJNE: „Jak zwrócić 404 z kontrolera REST?”

    // =================================================================================================
    // 3. 201 Created + Location; 204 No Content (sekcja 3)
    // =================================================================================================
    //
    //   POST /api/products  {"name":"Ciastka","priceGrosze":1299}
    //   → 201 Created, Location: http://localhost/api/products/4, ciało {"id":4,...}
    //   URI location = ServletUriComponentsBuilder.fromCurrentRequest()   ← http://localhost/api/products
    //           .path("/{id}").buildAndExpand(created.id()).toUri();       ← + /4
    // ServletUriComponentsBuilder (budowniczy adresów z bieżącego zapytania serwletu) bierze schemat, host i port
    // z zapytania — więc adres jest pełny i poprawny niezależnie od tego, na jakim porcie działa aplikacja.
    // Można też podać adres względny: ResponseEntity.created(URI.create("/api/products/" + id)) — tak robiła lekcja
    // s00_start/Start05HttpFiles; HTTP na to pozwala, ale pełny adres jest jednoznaczny.
    //
    //   DELETE /api/products/2 → 204 No Content (usunięto, nic do pokazania); drugi raz → 404 (już nie ma).
    //
    // PUŁAPKA: 204 z ciałem. Dlaczego nie? 204 z definicji NIE MA ciała — klienci i serwery pośredniczące je pomijają
    //   lub zgłaszają błąd. Chcesz coś zwrócić po usunięciu? Użyj 200 z ciałem.

    // =================================================================================================
    // 4. @ResponseStatus — na metodzie i na wyjątku (sekcja 4)
    // =================================================================================================
    //
    //   @PostMapping("/quick") @ResponseStatus(HttpStatus.CREATED) public Product createQuick(...)
    //     → 201, ale BEZ nagłówka Location (nie ma jak go dodać do zwykłego obiektu).
    //   @ResponseStatus(HttpStatus.NOT_FOUND) class ProductNotFoundException extends RuntimeException
    //     → GET /api/products/99/details rzuca wyjątek → 404. Kod kontrolera jest krótki: orElseThrow(...).
    //
    // PUŁAPKA: @ResponseStatus na metodzie, która zwraca ResponseEntity. Kto wygrywa? ResponseEntity — status
    //   z @ResponseStatus zostaje nadpisany. GET /api/products/conflict (@ResponseStatus(CREATED) +
    //   ResponseEntity.ok(...)) → 200. Sprawdzone testem. Dlaczego warto to wiedzieć? Dwa źródła statusu w jednej
    //   metodzie to proszenie się o pomyłkę — wybierz jedno.

    // =================================================================================================
    // 5. WŁASNE NAGŁÓWKI i inne kody (sekcja 5)
    // =================================================================================================
    //
    //   ResponseEntity.ok()
    //       .header("X-Total-Count", "3")                              ← własny nagłówek (np. liczba wszystkich wyników)
    //       .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))   ← Cache-Control: max-age=60
    //       .body(products);
    //   ResponseEntity.status(HttpStatus.ACCEPTED).body("Import przyjęty do realizacji")   ← 202
    //
    // DOBRA PRAKTYKA: używaj stałych HttpStatus (HttpStatus.ACCEPTED), a nie liczb (202). Dlaczego? Nazwa mówi, co
    //   znaczy kod, a literówka w nazwie się nie skompiluje — literówka w liczbie (220) skompiluje się bez słowa.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Zwykły obiekt → 200; null → 200 z pustym ciałem (NIE 404!).
     *   • ResponseEntity = status + nagłówki + ciało: ok(body), created(uri).body(...), noContent().build(),
     *     notFound().build(), badRequest().build(), status(HttpStatus.ACCEPTED).body(...).
     *   • ResponseEntity.of(Optional) → 200 z ciałem albo 404 bez ciała.
     *   • POST → 201 + Location: ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri().
     *   • DELETE → 204 No Content (bez ciała), brak zasobu → 404.
     *   • @ResponseStatus na metodzie → stały status; na klasie wyjątku → status po rzuceniu wyjątku.
     *   • ResponseEntity wygrywa z @ResponseStatus na tej samej metodzie.
     *   • Własne nagłówki: .header("X-...", wartość); pamięć podręczna: .cacheControl(CacheControl.maxAge(...)).
     *
     * PYTANIA KONTROLNE:
     *   1. Kiedy wystarczy zwrócić zwykły obiekt, a kiedy potrzebujesz ResponseEntity?
     *   2. Co się stanie? Metoda @GetMapping zwraca store.findById(id).orElse(null), a produktu nie ma.
     *   3. Co powinna zawierać odpowiedź na udany POST tworzący zasób i dlaczego? (PYTANIE REKRUTACYJNE)
     *   4. Czym różni się 204 od 200 z pustym ciałem? Kiedy którego użyć?
     *   5. Co się stanie? Metoda ma @ResponseStatus(HttpStatus.CREATED) i zwraca ResponseEntity.ok("...").
     *   6. ZNAJDŹ BŁĄD: {@code return ResponseEntity.created(location);} — kod się nie kompiluje. Czego brakuje?
     *   7. Po co ServletUriComponentsBuilder, skoro można napisać "http://localhost:8080/api/products/" + id?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielety w ResponseExerciseController (ćwiczenia 1–3). Testy: Rest03ResponsesExercisesTest
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zwykły obiekt — gdy odpowiedź to zawsze 200 z ciałem. ResponseEntity — gdy status zależy od sytuacji
     *      (200/404, 201/400) albo trzeba dodać nagłówki (Location, X-Total-Count, Cache-Control).
     *   2. 200 OK z pustym ciałem — null nie zamienia się w 404. Sprawdzone testem (sekcja 1). Naprawa:
     *      ResponseEntity.of(...) albo wyjątek z @ResponseStatus(NOT_FOUND).
     *   3. Status 201 Created, nagłówek Location z adresem nowego zasobu i (zwykle) sam zasób w ciele — z nadanym
     *      przez serwer id. Klient od razu wie, gdzie zasób jest, i nie musi go szukać.
     *   4. 204 z definicji nie ma ciała — mówi „zrobione, nic do pokazania” (typowo DELETE, czasem PUT). 200 obiecuje
     *      treść; 200 z pustym ciałem myli klienta, który spodziewa się JSON-a.
     *   5. Odpowiedź ma status 200 — ResponseEntity nadpisuje @ResponseStatus. Sprawdzone testem (sekcja 4).
     *   6. created(uri) zwraca budowniczego (BodyBuilder), a nie ResponseEntity — trzeba zakończyć .body(obiekt)
     *      albo .build().
     *   7. Bo host i port zależą od tego, gdzie i jak aplikacja działa (8080 lokalnie, losowy port w testach,
     *      inna domena na serwerze). ServletUriComponentsBuilder bierze je z bieżącego zapytania — zawsze się zgadzają.
     *      Sprawdzone testem: w MockMvc Location to http://localhost/api/products/{id}, a na RANDOM_PORT — z portem.
     */
    // </editor-fold>
}

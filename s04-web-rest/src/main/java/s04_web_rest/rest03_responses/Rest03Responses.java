package s04_web_rest.rest03_responses;

/**
 * <pre>
 * TEMAT: Odpowiedzi — kody statusu HTTP, nagłówki i ResponseEntity
 *        (response = odpowiedź; status code = kod statusu; header = nagłówek; body = ciało;
 *         ResponseEntity = „cała odpowiedź w jednym obiekcie”: status + nagłówki + ciało)
 *
 * W SKRÓCIE:
 *   Odpowiedź HTTP to trzy rzeczy: KOD STATUSU (czy się udało i jak), NAGŁÓWKI (informacje o odpowiedzi) i CIAŁO
 *   (dane). Gdy metoda zwraca zwykły obiekt, Spring ustawia status 200 i sam zamienia obiekt na ciało. Żeby zwrócić
 *   coś innego niż 200 — 201 po utworzeniu, 204 po usunięciu, 404 gdy czegoś nie ma — używasz ResponseEntity,
 *   adnotacji @ResponseStatus albo rzucasz wyjątek. Klient (przeglądarka, aplikacja mobilna) czyta kod statusu PIERWSZY,
 *   dlatego dobór kodu jest częścią umowy API.
 *
 * ANALOGIA: odpowiedź z okienka w urzędzie.
 *   Pani w okienku daje Ci kartkę z pieczątką (kod statusu: „przyjęte”, „odrzucone”, „nie ma takiego dokumentu”),
 *   dopisuje adnotacje na marginesie (nagłówki: gdzie odebrać dokument, jak długo jest ważny) i wręcza dokument
 *   (ciało). Czasem pieczątka wystarcza i dokumentu nie ma (204 — „załatwione, nie ma czego wręczać”). Pomylona
 *   pieczątka jest gorsza niż brak odpowiedzi: klient podejmie złą decyzję.
 *
 * JAK TO DZIAŁA:
 *   1. Metoda kontrolera zwraca wartość (obiekt, String, ResponseEntity) albo rzuca wyjątek.
 *   2. Zwykły obiekt → status 200 + ciało przez HttpMessageConverter (Rest01Controllers, sekcja 3).
 *   3. ResponseEntity → Spring bierze z niej status, nagłówki i ciało dokładnie takie, jak ustawiłeś.
 *   4. @ResponseStatus na metodzie zmienia status zwykłej odpowiedzi; na klasie wyjątku ustawia status odpowiedzi,
 *      gdy ten wyjątek wyleci z kontrolera.
 *   5. ResponseStatusException i wyjątki z adnotacją zamienia na kod HTTP wyspecjalizowany rozwiązywacz wyjątków
 *      (ExceptionResolver) — szczegóły obsługi błędów w dziale s06_error_handling.
 *
 * SŁÓWKA:
 *   created = utworzono; no content = brak treści; not found = nie znaleziono; bad request = błędne zapytanie;
 *   conflict = konflikt; location = lokalizacja, adres nowego zasobu; cache = pamięć podręczna (kopia, której nie
 *   trzeba pobierać ponownie); builder = budowniczy (obiekt do składania czegoś krok po kroku); reason = powód;
 *   idempotent = idempotentny (wielokrotne wykonanie daje ten sam skutek co jedno).
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest01Controllers (mapowania, 404/405/406/415),
 *             s04_web_rest/Rest02RequestData (dane z zapytania; źródło wielu 400),
 *             s04_web_rest/Rest05CrudApi (komplet endpointów produktów z tymi kodami),
 *             s06_error_handling/Err01DefaultErrors (jak Boot opisuje błędy w ciele odpowiedzi),
 *             s06_error_handling/Err04ProblemDetail (standardowy format opisu błędu),
 *             t10_exceptions/Exceptions05CustomExceptions (własne wyjątki).
 * </pre>
 */
public final class Rest03Responses {

    private Rest03Responses() {
    }

    // =================================================================================================
    // 1. ResponseEntity — STATUS, NAGŁÓWKI I CIAŁO W RĘKACH (ProductController)
    // =================================================================================================
    //
    // ResponseEntity<T> składasz „budowniczym” (builder), od statusu do ciała:
    //
    //   ResponseEntity.ok(produkt)                                      → 200 + ciało
    //   ResponseEntity.ok().header("X-Total-Count", "2").body(lista)    → 200 + własny nagłówek + ciało
    //   ResponseEntity.created(location).body(produkt)                  → 201 + nagłówek Location + ciało
    //   ResponseEntity.noContent().build()                              → 204, bez ciała
    //   ResponseEntity.notFound().build()                               → 404, bez ciała
    //   ResponseEntity.badRequest().build()                             → 400
    //   ResponseEntity.status(HttpStatus.CONFLICT).build()              → dowolny kod, tu 409
    //   ResponseEntity.of(optional)                                     → 200 z wartością albo 404 dla pustego Optional
    //
    // Typ w nawiasie (ResponseEntity<Product>) to typ ciała w odpowiedzi „udanej”. Gdy ciała nie będzie (204),
    // używa się ResponseEntity<Void>.
    //
    // Location po utworzeniu (metoda create). 201 Created mówi „utworzone”, a nagłówek Location — GDZIE (adres
    // nowego zasobu; klient może od razu zrobić na nim GET). Adres budujemy z UriComponentsBuilder, który Spring
    // wstrzykuje jako parametr metody, już ustawiony na adres bieżącego serwera:
    //
    //   URI location = uriBuilder.path("/rest03/products/{id}").buildAndExpand(created.id()).toUri();
    //   return ResponseEntity.created(location).body(created);
    //
    // Test sekcji 1 (uruchomiony): POST → 201 i Location: http://localhost/rest03/products/3 (w MockMvc host to
    // localhost; na prawdziwym serwerze będzie jego adres i port), a GET pod tym adresem zwraca nowy produkt.
    // Inny zapis tego samego: ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri()
    // (buduje od adresu bieżącego zapytania; nie potrzebuje parametru, ale jest „ukrytą zależnością” od zapytania).
    //
    // Test sekcji 1 sprawdza też 400 (brak nazwy), 409 (nazwa „kawa” koliduje z „Kawa”), 204 po usunięciu
    // (ciało puste; drugie usunięcie tego samego → 404) i 404 bez ciała.
    //
    // DOBRA PRAKTYKA: ResponseEntity.created(location) — nie „status(201)” i nie samo 200 — po utworzeniu zasobu.
    //   Dlaczego? Klient dostaje standardowy sygnał „utworzono” i adres do pobrania zasobu; nie musi zgadywać
    //   adresu z id w ciele odpowiedzi.
    //
    // PUŁAPKA: ResponseEntity<Product> z błędem, który ma nieść opis (np. ResponseEntity.badRequest().body("zła cena")).
    //   Dlaczego źle? Typ ciała jest wtedy inny dla sukcesu (Product) i błędu (String) — kod się nie kompiluje albo
    //   trzeba użyć ResponseEntity<?>, które gubi informacje o typie. Opisy błędów wysyłaj spójnie przez mechanizm
    //   błędów (s06_error_handling), a w kontrolerze zwracaj sam status.

    // =================================================================================================
    // 2. KODY HTTP W REST — KIEDY KTÓRY, ORAZ @ResponseStatus
    // =================================================================================================
    //
    //   Kod | Nazwa                | Kiedy w API REST
    //   ----+----------------------+--------------------------------------------------------------------------
    //   200 | OK                   | GET udany; PUT/PATCH udany z odesłaniem zmienionego zasobu
    //   201 | Created              | POST utworzył zasób (+ nagłówek Location)
    //   204 | No Content           | DELETE udany; PUT/PATCH udany, gdy nie odsyłasz ciała
    //   400 | Bad Request          | zapytanie jest błędne: zły JSON, zły typ parametru, brak wymaganych danych
    //   404 | Not Found            | zasobu o tym id nie ma
    //   405 | Method Not Allowed   | ścieżka jest, ale nie dla tej metody HTTP (Rest01Controllers)
    //   409 | Conflict             | zapytanie jest poprawne, ale koliduje ze stanem (duplikat, zła wersja, wyprzedane)
    //   Reguła kciuka: 2xx — sukces, 4xx — błąd KLIENTA (popraw zapytanie), 5xx — błąd SERWERA (klient nic nie poradzi).
    //
    // @ResponseStatus — status bez ResponseEntity:
    //   a) NA METODZIE: metoda quick() ma @ResponseStatus(HttpStatus.CREATED) i zwraca zwykły Product. Test sekcji 2:
    //      odpowiedź 201, ciało z produktem, ale BRAK nagłówka Location. (Dlatego do tworzenia zasobów lepsza jest
    //      ResponseEntity.created.)
    //   b) NA KLASIE WYJĄTKU: SoldOutException ma @ResponseStatus(value = CONFLICT, reason = "Towar wyprzedany").
    //      Gdy kontroler rzuci ten wyjątek (reserve dla id 2), klient dostaje 409 z powodem — bez try/catch
    //      w kontrolerze. Test sekcji 2: id 1 → 200, id 2 → 409, powód „Towar wyprzedany”, a wyjątek to SoldOutException.
    //
    // PUŁAPKA: @ResponseStatus na metodzie, która zwraca ResponseEntity. Dlaczego źle? ResponseEntity ma własny
    //   status i on wygrywa — adnotacja jest po cichu ignorowana (test responseEntityWinsOverResponseStatus: metoda
    //   z @ResponseStatus(CREATED) i ResponseEntity.ok odpowiada 200). Wybierz jedno z dwóch.
    //
    // PUŁAPKA: 200 z komunikatem błędu w ciele („{"error":"brak produktu"}” przy statusie 200). Dlaczego źle?
    //   Klienci (przeglądarki, biblioteki, narzędzia monitorujące) patrzą na KOD. Błąd z kodem 200 jest dla nich
    //   sukcesem — nie ponowią zapytania, nie zalogują błędu, a cache może go zapamiętać.
    //
    // PYTANIE REKRUTACYJNE: jaka jest różnica między 200 a 204 i między 400 a 409? (204: sukces bez ciała; 400:
    //   zapytanie błędne samo w sobie; 409: poprawne, ale sprzeczne z bieżącym stanem zasobu.)

    // =================================================================================================
    // 3. ResponseStatusException — SZYBKI BŁĄD Z KODEM (byIdOrFail, reserve)
    // =================================================================================================
    //
    //   return store.find(id)
    //           .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produkt " + id + " nie istnieje"));
    //
    // Jeden wiersz zamiast osobnej klasy wyjątku: kod i powód podajesz w miejscu rzucenia. Zaleta względem
    // ResponseEntity: metoda zwraca zwykły Product (nie ResponseEntity<Product>), a błąd „wylatuje” przez wyjątek
    // z dowolnie głębokiego wywołania (np. z serwisu). Test sekcji 3: istniejący produkt → 200; brak → 404
    // z powodem „Produkt 99 nie istnieje” (MockMvc pokazuje powód przez status().reason).
    //
    // Co zobaczy klient na prawdziwym serwerze? Powód trafia do standardowej strony błędu Boota. Jak ta odpowiedź
    // wygląda i jak zrobić z niej własny, spójny format — s06_error_handling/Err01DefaultErrors i
    // s06_error_handling/Err04ProblemDetail (tu tylko zapowiedź).
    //
    // DOBRA PRAKTYKA: w kontrolerze używaj ResponseEntity do „normalnych” rozgałęzień (jest/nie ma), a wyjątków
    //   (ResponseStatusException, własne z @ResponseStatus) do sytuacji, gdy błąd powstaje głęboko w kodzie
    //   i nie chcesz przekazywać go przez wszystkie warstwy w zwracanych wartościach. Dlaczego? Wyjątek przerywa
    //   przetwarzanie i nie zaśmieca sygnatur metod; ResponseEntity jest jawna i dobrze się czyta tam, gdzie
    //   rozgałęzienie jest w samym kontrolerze.

    // =================================================================================================
    // 4. NAGŁÓWKI ODPOWIEDZI — header(...), CacheControl, ETag (list, cached)
    // =================================================================================================
    //
    //   ResponseEntity.ok().header("X-Total-Count", String.valueOf(all.size())).body(all)
    //   ResponseEntity.ok()
    //           .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
    //           .eTag("\"v1-1\"")
    //           .body(product)
    //
    // Test sekcji 4: lista → nagłówek X-Total-Count: 2; produkt z cached → Cache-Control: max-age=300, public
    // i ETag: "v1-1" (wartość ETag w cudzysłowach — tak wymaga standard).
    //   • X-Total-Count — własny nagłówek z liczbą wszystkich elementów (klient nie musi liczyć); przy własnych
    //     nagłówkach tradycyjnie dopisywano „X-”.
    //   • Cache-Control — mówi, jak długo klient i pośrednicy mogą trzymać kopię odpowiedzi bez pytania serwera;
    //     CacheControl to budowniczy (maxAge, noStore, noCache, cachePublic, cachePrivate).
    //   • ETag — „odcisk palca” wersji zasobu. Klient przy kolejnym pobraniu może zapytać „czy zmienione?”
    //     (nagłówek If-None-Match) i dostać krótkie 304 Not Modified zamiast całego ciała. Pełne wsparcie
    //     (ShallowEtagHeaderFilter, WebRequest.checkNotModified) tylko wspominamy — tu ustawiamy sam nagłówek.
    //
    // PUŁAPKA: Cache-Control: public na odpowiedziach z danymi konkretnego użytkownika (koszyk, profil). Dlaczego groźne?
    //   Pamięć podręczna pośrednika może pokazać odpowiedź JEDNEGO użytkownika innemu. Dla danych osobistych użyj
    //   cachePrivate() albo noStore().
    //
    // DOBRA PRAKTYKA: nagłówek własny ustawiaj tam, gdzie klient naprawdę go użyje (np. liczba elementów przy listach
    //   stronicowanych). Dlaczego? Każdy nagłówek to kolejny element umowy API, który trzeba utrzymać i opisać.

    // =================================================================================================
    // 5. PUŁAPKA null — 200 Z PUSTYM CIAŁEM
    // =================================================================================================
    //
    //   @GetMapping("/{id}/maybe")
    //   public Product maybe(@PathVariable long id) { return store.find(id).orElse(null); }
    //
    // Dla nieistniejącego id ta metoda zwraca null. Spring NIE zamienia null na 404 — wysyła 200 OK z pustym ciałem.
    // Sprawdzone testem nullIsNot404: status 200, ciało "" (a poprawna wersja z ResponseEntity.notFound() daje 404).
    //
    // PUŁAPKA: metoda zwracająca null albo pusty wynik bez żadnego kodu. Dlaczego groźne? Klient widzi „sukces” bez
    //   danych i nie ma jak odróżnić „nie ma takiego produktu” od „produkt istnieje, ale puste ciało”.
    //   Napraw: zwróć ResponseEntity.notFound().build(), ResponseEntity.of(optional) albo rzuć ResponseStatusException(404).
    //   (Pusta KOLEKCJA to co innego: lista bez elementów to poprawne 200 z [] — nikt nie szukał konkretnego zasobu.)

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Zwykły obiekt → 200. Inny status/nagłówki → ResponseEntity (ok, created, noContent, notFound, badRequest, status).
     *   • 201 + Location po utworzeniu: ResponseEntity.created(uriBuilder.path("/…/{id}").buildAndExpand(id).toUri()).
     *   • 200 — odczyt; 201 — utworzono; 204 — sukces bez ciała (DELETE); 400 — zapytanie błędne; 404 — brak zasobu;
     *     409 — konflikt ze stanem; 4xx = wina klienta, 5xx = wina serwera.
     *   • @ResponseStatus na metodzie (zwykła odpowiedź) albo na klasie wyjątku; przy ResponseEntity adnotacja jest
     *     ignorowana.
     *   • ResponseStatusException(HttpStatus, powód) — szybki błąd bez własnej klasy; pełna obsługa: s06.
     *   • Nagłówki: .header(nazwa, wartość), .cacheControl(CacheControl…), .eTag("\"wersja\"").
     *   • null z metody → 200 z pustym ciałem (NIE 404).
     *
     * PYTANIA KONTROLNE:
     *   1. Jak ustawić kod 404 w metodzie zwracającej {@code ResponseEntity<Product>}? A jak, gdy metoda zwraca zwykły Product?
     *   2. Który kod i który nagłówek wyślesz po udanym POST tworzącym zasób? Co powinien zawierać ten nagłówek?
     *   3. Co się stanie? Metoda {@code @GetMapping("/{id}") Product byId(long id)} zwraca null dla nieznanego id.
     *   4. Co się stanie? Metoda ma {@code @ResponseStatus(HttpStatus.CREATED)}, ale zwraca
     *      {@code ResponseEntity.ok(produkt)}.
     *   5. Kiedy 400, a kiedy 409? Podaj po jednym przykładzie ze sklepu.
     *   6. ZNAJDŹ BŁĄD: {@code return ResponseEntity.ok("{\"error\":\"produkt nie istnieje\"}");} w metodzie, która
     *      szuka produktu po id. Co jest nie tak z punktu widzenia klienta?
     *   7. ZNAJDŹ BŁĄD: DELETE zwraca {@code ResponseEntity.ok().build()} z pustym ciałem, a drugie usunięcie tego
     *      samego id też daje 200. Co byś poprawił i jaki kod jest tu bardziej właściwy?
     *   8. Co to jest ETag i do czego służy w połączeniu z Cache-Control?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, testy: Rest03ResponsesExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Mapowania są gotowe. Rozwiązania wzorcowe: Rest03SolutionsController (s04_web_rest.solutions.rest03_responses).
    //
    // ĆWICZENIE 1 (łatwe): order(id) ma zwrócić 200 z ciałem "zamówienie 1" dla id = 1, a dla innych id — 404 bez ciała.
    //   Podpowiedź: ResponseEntity.ok(...) i ResponseEntity.notFound().build() (sekcja 1).
    //
    // ĆWICZENIE 2 (łatwe): deleteOrder(id) ma odpowiedzieć 204 No Content bez ciała. Podpowiedź: typ ResponseEntity<Void>.
    //
    // ĆWICZENIE 3 (średnie): createOrder(uriBuilder) ma odpowiedzieć 201 z ciałem "utworzono" i nagłówkiem Location
    //   z pełnym adresem .../rest03/cw/orders/42. Podpowiedź: uriBuilder.path("/rest03/cw/orders/{id}").buildAndExpand(42).
    //
    // ĆWICZENIE 4 (średnie): pay(id) dla id = 13 ma rzucić PaymentRejectedException, a klient ma dostać 402
    //   (Payment Required); dla innych id ma zwrócić "opłacono". Podpowiedź: klasa wyjątku w szkielecie nie ma
    //   statusu — dodaj jej @ResponseStatus(HttpStatus.PAYMENT_REQUIRED) (sekcja 2).
    //
    // ĆWICZENIE 5 (trudniejsze): report() ma zwrócić 200, ciało "raport", nagłówek X-Total-Count: 3 i nagłówek
    //   Cache-Control: no-store. Podpowiedź: ResponseEntity.ok().header(...).cacheControl(CacheControl.noStore()).body(...) (sekcja 4).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. W ResponseEntity<Product>: {@code return ResponseEntity.notFound().build();}. W metodzie zwracającej zwykły
     *      Product: rzucić {@code new ResponseStatusException(HttpStatus.NOT_FOUND, "…")} albo wyjątek z
     *      {@code @ResponseStatus(NOT_FOUND)}. Sprawdzone testami (sekcje 1 i 3).
     *   2. 201 Created i nagłówek Location z adresem nowego zasobu (np. http://localhost/rest03/products/3), pod
     *      którym klient wykona GET. Sprawdzone testem (sekcja 1).
     *   3. Odpowiedź 200 OK z pustym ciałem — Spring nie zamienia null na 404. Sprawdzone testem nullIsNot404 (sekcja 5).
     *   4. Wygrywa ResponseEntity: odpowiedź to 200, adnotacja @ResponseStatus jest ignorowana. (Wniosek: wybierz
     *      jedno z dwóch.) Sprawdzone testem responseEntityWinsOverResponseStatus (sekcja 2).
     *   5. 400 — samo zapytanie jest błędne (np. nowy produkt bez nazwy). 409 — zapytanie poprawne, ale sprzeczne ze
     *      stanem (np. produkt o takiej nazwie już istnieje albo towar jest wyprzedany). Sprawdzone testami (sekcje 1 i 2).
     *   6. Kod 200 mówi „sukces”, więc klient, biblioteka HTTP, monitoring i pamięć podręczna uznają odpowiedź za udaną
     *      i zlekceważą błąd. Błąd musi mieć kod 4xx/5xx (tu: 404).
     *   7. Pierwsze usunięcie ma dać 204 No Content (sukces bez ciała) — nie 200 z pustym ciałem; drugie usunięcie
     *      tego samego id najczęściej daje 404 (zasobu już nie ma). Sprawdzone testem noContent (sekcja 1).
     *   8. ETag to odcisk wersji zasobu. Cache-Control mówi, jak długo kopię można trzymać; po jej wygaśnięciu klient
     *      pyta serwer z If-None-Match: <ETag> i, jeśli zasób się nie zmienił, dostaje krótkie 304 Not Modified
     *      zamiast pełnej odpowiedzi.
     */
    // </editor-fold>
}

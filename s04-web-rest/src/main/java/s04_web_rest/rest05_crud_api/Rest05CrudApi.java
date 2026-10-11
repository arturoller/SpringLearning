package s04_web_rest.rest05_crud_api;

/**
 * <pre>
 * TEMAT: Kompletne API CRUD — GET, POST, PUT, PATCH, DELETE na produktach w pamięci; kody statusu, idempotentność,
 *        PUT kontra PATCH, bezpieczeństwo wątków
 *        (CRUD = create, read, update, delete — utwórz, odczytaj, zmień, usuń; idempotent = idempotentny: wielokrotne
 *        wykonanie daje ten sam stan co jednokrotne; replace = zastąp; patch = łata, częściowa zmiana)
 *
 * W SKRÓCIE:
 *   Pięć metod HTTP na jednym zasobie (/api/products) daje pełne API. Każda ma swój kontrakt: GET czyta (200/404),
 *   POST tworzy (201 + Location), PUT zastępuje CAŁY zasób (200/404), PATCH zmienia WYBRANE pola (200/404),
 *   DELETE usuwa (204/404). Błędne dane → 400. GET, PUT i DELETE są idempotentne, POST nie, PATCH — zależy od treści.
 *   Kontroler jest cienki (HTTP ↔ wywołania), a dane trzyma repozytorium bezpieczne dla wielu wątków.
 *
 * ANALOGIA: kartoteka w bibliotece.
 *   GET — przeglądasz kartę. POST — zakładasz nową kartę; bibliotekarka nadaje numer i mówi, w której szufladzie
 *   leży (Location). PUT — wyrzucasz kartę i wkładasz NOWĄ, wypełnioną od zera (czego nie wpiszesz, tego nie będzie).
 *   PATCH — poprawiasz korektorem jedno pole. DELETE — wyjmujesz kartę; drugi raz nie ma już czego wyjmować, ale
 *   kartoteka wygląda tak samo (idempotentność).
 *
 * JAK TO DZIAŁA:
 *   PATCH /api/products/2  {"priceGrosze":1499}
 *     → DispatcherServlet → ProductCrudController.patch(2, ProductPatch(null, 1499))     (Rest01, Rest02)
 *     → repository.patch(...) → ConcurrentHashMap.computeIfPresent(2, ...)               (jedna niepodzielna operacja)
 *         jest produkt 2 → nowy rekord: nazwa bez zmian (null w łacie), cena 1499 → Optional z produktem
 *         brak produktu  → Optional.empty()
 *     → ResponseEntity.of(optional) → 200 + JSON albo 404                                (Rest03)
 *
 * SŁÓWKA:
 *   resource = zasób (rzecz pod adresem, np. produkt 2); collection = kolekcja (/api/products); repository =
 *   repozytorium (magazyn danych); idempotent = idempotentny; safe = bezpieczna (metoda, która niczego nie zmienia,
 *   np. GET); partial update = częściowa zmiana; thread-safe = bezpieczny dla wątków; atomic = niepodzielny;
 *   DTO (data transfer object) = obiekt do przesyłania danych.
 *
 * ZOBACZ TEŻ: t34_toward_spring/Spring03RestConcepts (REST i idempotentność), t34_toward_spring/Spring02Layers
 *             (warstwy aplikacji), t21_concurrency/Concurrency06ConcurrentCollections (ConcurrentHashMap),
 *             s04_web_rest/Rest03Responses (kody statusu), s05_validation (walidacja zamiast ręcznych if-ów),
 *             s07_data_jdbc (dane w bazie zamiast w mapie).
 * </pre>
 */
public final class Rest05CrudApi {

    private Rest05CrudApi() {
    }

    // =================================================================================================
    // 1. ODCZYT — GET kolekcji i GET jednego zasobu (Rest05CrudApiTest, sekcja 1)
    // =================================================================================================
    //
    //   GET /api/products     → 200 i [{"id":1,"name":"Kawa",...},{"id":2,...},{"id":3,...}]  (posortowane po id)
    //   GET /api/products/2   → 200 i {"id":2,"name":"Herbata","priceGrosze":1599}
    //   GET /api/products/99  → 404
    // GET jest BEZPIECZNY (niczego nie zmienia) i idempotentny — można go powtarzać i zapamiętywać (cache).
    //
    // PUŁAPKA: ConcurrentHashMap (i HashMap) nie gwarantują kolejności. Dlaczego to ważne? Lista bez sortowania
    //   „zwykle” wychodzi po kolei, aż pewnego dnia nie — klient i testy, które na tym polegają, zaczynają się
    //   sypać losowo. Repozytorium sortuje jawnie: sorted(Comparator.comparingLong(Product::id)).

    // =================================================================================================
    // 2. TWORZENIE — POST (sekcja 2)
    // =================================================================================================
    //
    //   POST /api/products  {"name":"Ciastka","priceGrosze":1299}
    //   → 201 Created, Location: http://localhost/api/products/4, ciało {"id":4,"name":"Ciastka","priceGrosze":1299}
    //   {"name":"","priceGrosze":100} → 400 {"error":"Nazwa produktu nie może być pusta"}
    //   {"name":"Ciastka"} (bez ceny) → 400 — Jackson 3 nie wstawi 0 do int (Rest04Json)
    // POST NIE jest idempotentny: dwa identyczne zapytania tworzą DWA produkty (id 4 i 5). Sprawdzone testem.
    //
    // DOBRA PRAKTYKA: id nadaje serwer, nie klient (ProductRequest nie ma pola id). Dlaczego? Dwóch klientów
    //   wybrałoby to samo id; serwer z AtomicLong (licznik niepodzielny) gwarantuje unikalność.

    // =================================================================================================
    // 3. ZASTĄPIENIE i ZMIANA CZĘŚCI — PUT kontra PATCH (sekcja 3)
    // =================================================================================================
    //
    //   PUT   /api/products/2  {"name":"Herbata zielona","priceGrosze":1799}  → 200, CAŁY produkt zastąpiony
    //   PATCH /api/products/2  {"priceGrosze":1499}                            → 200, zmieniona tylko cena
    //   PUT   /api/products/2  {"name":"Herbata"}  (bez ceny)                  → 400 — PUT wymaga KOMPLETU danych
    //   PUT/PATCH /api/products/99                                             → 404
    // PUT jest idempotentny: ten sam PUT wysłany 3 razy zostawia ten sam stan. PATCH „ustaw cenę na 1499” też, ale
    // PATCH bywa nieidempotentny (np. „dodaj 1 do stanu magazynu”) — dlatego HTTP go za idempotentny nie uznaje.
    // PYTANIE REKRUTACYJNE: „Czym różni się PUT od PATCH?” i „Które metody HTTP są idempotentne?”
    //
    // PUŁAPKA: w tej lekcji PATCH traktuje null jako „bez zmian”. Dlaczego to ograniczenie? Nie da się przez PATCH
    //   wyczyścić pola (ustawić null) — {"name":null} znaczy „nie ruszaj nazwy”. Gdy potrzebujesz czyszczenia pól,
    //   sięga się po JSON Merge Patch (RFC 7396, typ application/merge-patch+json) albo osobne flagi — to temat na
    //   poważne API, nie na pierwsze CRUD-y.
    //
    // PUŁAPKA: „sprawdź, potem zmień” w dwóch krokach: if (map.containsKey(id)) map.put(id, nowy). Dlaczego źle?
    //   Między sprawdzeniem a zapisem inny wątek może produkt usunąć — i put „wskrzesi” go z powrotem.
    //   computeIfPresent robi oba kroki niepodzielnie (atomowo).

    // =================================================================================================
    // 4. USUWANIE — DELETE i idempotentność (sekcja 4)
    // =================================================================================================
    //
    //   DELETE /api/products/3 → 204 No Content; drugi raz → 404; GET /api/products/3 → 404.
    // Czy DELETE jest idempotentny, skoro raz daje 204, a raz 404? TAK. Idempotentność dotyczy STANU SERWERA, nie kodu
    // odpowiedzi: po pierwszym i po dziesiątym DELETE produktu 3 nie ma. Niektóre API odpowiadają 204 także drugi raz —
    // to też poprawne; ważne, żeby było opisane w dokumentacji API.
    //
    // DOBRA PRAKTYKA: dzięki idempotentności klient może bezpiecznie POWTÓRZYĆ PUT i DELETE po zerwanym połączeniu.
    //   Dlaczego to cenne? Nie wie, czy serwer dostał pierwsze zapytanie — powtórka niczego nie zepsuje.
    //   Powtórzony POST mógłby utworzyć duplikat.

    // =================================================================================================
    // 5. BEZPIECZEŃSTWO WĄTKÓW i TEST NA PRAWDZIWYM SERWERZE (Rest05ProductRepositoryTest, Rest05CrudApiServerTest)
    // =================================================================================================
    //
    // Kontroler i repozytorium to singletony — jeden obiekt dla wszystkich zapytań, a Tomcat obsługuje zapytania
    // równolegle w wielu wątkach. Test: 200 równoległych create(...) na wątkach wirtualnych → 200 produktów
    // i 200 RÓŻNYCH id. Z HashMap i zwykłym long nextId++ zdarzałyby się zgubione wpisy i powtórzone id.
    // Test na RANDOM_PORT przechodzi cały cykl przez prawdziwy serwer i RestClient (z PATCH-em włącznie).
    //
    // PUŁAPKA: pole w kontrolerze typu List/HashMap „na szybko”. Dlaczego groźne? Działa na Twoim komputerze
    //   (jedno zapytanie naraz), a pod obciążeniem gubi dane albo rzuca ConcurrentModificationException.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • GET kolekcji 200; GET jednego 200/404; POST 201 + Location / 400; PUT 200/400/404; PATCH 200/400/404;
     *     DELETE 204/404.
     *   • PUT = zastąp całość (wymaga kompletu danych); PATCH = zmień przysłane pola (null = bez zmian).
     *   • Idempotentne: GET, PUT, DELETE (stan po N wywołaniach = stan po 1). POST — nie. PATCH — zależy od treści.
     *   • Idempotentność dotyczy stanu serwera, nie kodu odpowiedzi (DELETE: 204, potem 404 — OK).
     *   • Osobne DTO: ProductRequest (POST/PUT), ProductPatch (pola obiektowe), Product (odpowiedź).
     *   • Repozytorium w pamięci: ConcurrentHashMap + AtomicLong + computeIfPresent; sortuj przed zwróceniem listy.
     *   • Testy: @WebMvcTest + @Import(repozytorium) + reset danych przed każdym testem; RANDOM_PORT + RestClient.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się PUT od PATCH? Pokaż na przykładzie produktu {"name":"Kawa","priceGrosze":2999}.
     *   2. Co się stanie? Wyślesz DELETE /api/products/3 dwa razy. Jakie kody dostaniesz i czy DELETE jest idempotentny?
     *   3. Co się stanie? PUT /api/products/2 z ciałem {"name":"Herbata"} (bez ceny).
     *   4. Które metody HTTP są idempotentne, a które bezpieczne? (PYTANIE REKRUTACYJNE)
     *   5. ZNAJDŹ BŁĄD: repozytorium ma pola HashMap<Long, Product> map i long nextId oraz metodę
     *      create(...) { long id = nextId++; map.put(id, ...); }. Co się stanie pod obciążeniem?
     *   6. Co się stanie? PATCH /api/products/1 z ciałem {"name":null}.
     *   7. Dlaczego klient może bezpiecznie powtórzyć PUT po zerwanym połączeniu, a POST — nie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet CustomerExerciseController (ćwiczenia 1–3). Testy: Rest05CrudApiExercisesTest
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. PUT zastępuje cały zasób — trzeba wysłać komplet: {"name":"Kawa","priceGrosze":2499}. PATCH zmienia tylko
     *      przysłane pola: {"priceGrosze":2499} — nazwa zostaje. PUT bez części pól to błąd (tu 400).
     *   2. Pierwszy raz 204 No Content, drugi raz 404 Not Found. Tak, DELETE jest idempotentny — po obu wywołaniach
     *      stan serwera jest ten sam (produktu nie ma); idempotentność nie wymaga identycznego kodu odpowiedzi.
     *      Sprawdzone testem (sekcja 4).
     *   3. 400 Bad Request — ProductRequest ma int priceGrosze, a Jackson 3 nie wstawia 0 za brakujące pole typu
     *      prostego (FAIL_ON_NULL_FOR_PRIMITIVES). Sprawdzone testem (sekcja 3).
     *   4. Bezpieczne (niczego nie zmieniają): GET, HEAD, OPTIONS. Idempotentne: bezpieczne + PUT i DELETE.
     *      POST nie jest ani jednym, ani drugim; PATCH nie jest uznawany za idempotentny.
     *   5. Wyścig wątków: nextId++ to odczyt + zapis, więc dwa wątki mogą dostać to samo id (jeden produkt nadpisze
     *      drugi), a równoległe put do HashMap może zgubić wpisy. Naprawa: AtomicLong + ConcurrentHashMap.
     *      Sprawdzone testem dla poprawnej wersji (200 równoległych create → 200 różnych id).
     *   6. 200 i produkt bez zmian — null w łacie znaczy „nie zmieniaj tego pola”. Sprawdzone testem (sekcja 3).
     *   7. PUT jest idempotentny: drugi raz ustawia ten sam stan. POST tworzy nowy zasób przy każdym wywołaniu,
     *      więc powtórka utworzy duplikat. Sprawdzone testem (sekcja 2: dwa POST-y → dwa różne id).
     */
    // </editor-fold>
}

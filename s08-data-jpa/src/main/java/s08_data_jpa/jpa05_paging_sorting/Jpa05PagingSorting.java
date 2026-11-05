package s08_data_jpa.jpa05_paging_sorting;

/**
 * <pre>
 * TEMAT: Stronicowanie i sortowanie — {@code Pageable}, {@code Page}, {@code Slice}, {@code Sort}, {@code Limit},
 *        paginacja klucza ({@code ScrollPosition}, {@code Window})
 *        (paging/pagination = stronicowanie, dzielenie wyniku na strony; pageable = dający się stronicować;
 *         page = strona; slice = wycinek; sort = sortowanie; limit = ograniczenie; keyset = zestaw kluczy;
 *         scroll position = pozycja przewijania; window = okno)
 *
 * W SKRÓCIE:
 *   Sklep ma 100 000 produktów — nikt nie wczytuje ich naraz. Do metody repozytorium dokładasz OSTATNI parametr
 *   {@code Pageable} (np. {@code PageRequest.of(2, 20, Sort.by("price"))}), a Spring Data dopisuje do zapytania
 *   ORDER BY, OFFSET i FETCH FIRST. Typ wyniku decyduje, ile to kosztuje: {@code Page} wie, ile jest wszystkiego
 *   (drugie zapytanie count), {@code Slice} wie tylko, czy jest następna strona, {@code List} — nic poza treścią.
 *   Bez jednoznacznego sortowania strony mogą się nakładać — zawsze kończ sortowanie polem unikalnym (id).
 *
 * ANALOGIA: katalog w księgarni internetowej.
 *   Page to wynik wyszukiwania „strona 3 z 57 (1 134 książki)” — żeby napisać „z 57”, ktoś musiał policzyć
 *   wszystkie książki (count). Slice to przewijanie w aplikacji „pokaż więcej” — wystarczy wiedzieć, czy coś
 *   jeszcze jest. Keyset to zakładka w książce: „czytam dalej OD tej strony”, zamiast za każdym razem liczyć
 *   strony od początku (OFFSET).
 *
 * JAK TO DZIAŁA:
 *   1. Spring Data rozpoznaje parametry specjalne po TYPIE: Pageable, Sort, Limit, ScrollPosition nie są warunkami
 *      WHERE, tylko zmieniają kształt zapytania (ORDER BY / OFFSET / FETCH FIRST / warunek klucza).
 *   2. Page: wykonuje SELECT strony, a potem — jeśli z samej strony nie da się wywnioskować sumy — SELECT count(*)
 *      z tym samym WHERE. Wynik: PageImpl (treść + totalElements + totalPages).
 *   3. Slice: pobiera size + 1 wierszy; jeśli przyszedł dodatkowy wiersz, hasNext() = true (sam wiersz odrzuca).
 *   4. Window (keyset): zapamiętuje wartości kluczy sortowania ostatniego elementu; kolejne zapytanie ma warunek
 *      „większe niż ten klucz” zamiast OFFSET.
 *
 * SŁÓWKA:
 *   page number = numer strony (od 0!); page size = rozmiar strony; total elements = liczba wszystkich elementów;
 *   offset = przesunięcie (ile wierszy pominąć); ascending/descending = rosnąco/malejąco; tie = remis (równe
 *   wartości); tie-breaker = rozstrzygacz remisu; deterministic = deterministyczny (zawsze taki sam);
 *   count query = zapytanie liczące; unpaged = bez stronicowania.
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa03QueryMethods (Top/First w nazwie, OrderBy),
 *             s08_data_jpa/Jpa04CustomQueries (@Query — tu z Pageable i countQuery),
 *             s04_web_rest/Rest05CrudApi (REST API — tam przydałyby się parametry page, size i sort, sekcja 5),
 *             s09_jpa_relations/Rel05NPlusOne (stronicowanie z JOIN FETCH — dlaczego trudne).
 * </pre>
 */
public final class Jpa05PagingSorting {

    private Jpa05PagingSorting() {
    }

    // =================================================================================================
    // 1. Pageable i PageRequest — strony liczymy OD ZERA
    // =================================================================================================
    //
    //   {@code Page<Product> findByCategory(Category category, Pageable pageable);}
    //   {@code repository.findByCategory(COMPUTERS, PageRequest.of(1, 2, Sort.by("price").and(Sort.by("id"))));}
    //
    // PageRequest (żądanie strony — implementacja Pageable) = numer strony + rozmiar + sortowanie. SQL z logu (test):
    //   {@code … where p1_0.category=? order by p1_0.price,p1_0.id offset ? rows fetch first ? rows only}
    // (składnię OFFSET/FETCH FIRST, LIMIT albo ROWNUM dobiera dialekt Hibernate do bazy.)
    //
    // Walidacja w PageRequest.of (sprawdzone testem): numer ujemny → IllegalArgumentException „Page index must not
    // be less than zero”, rozmiar 0 → „Page size must not be less than one”. Strona za końcem (np. 5 z 3) NIE jest
    // błędem — dostajesz pustą treść i poprawne totalElements.
    // {@code Pageable.unpaged()} — „bez stronicowania”: ta sama metoda zwróci wszystko (przydatne w testach i eksporcie).
    //
    // PUŁAPKA: strony od 0, a ludzie liczą od 1. Dlaczego groźne? Użytkownik klika „strona 1”, Ty wysyłasz
    //   PageRequest.of(1, …) i pokazujesz mu DRUGĄ stronę — pierwsza jest niedostępna. Przelicz numer na granicy
    //   (w kontrolerze) albo ustaw {@code spring.data.web.pageable.one-indexed-parameters=true} (sekcja 5).

    // =================================================================================================
    // 2. Page vs Slice vs List — ile zapytań kosztuje strona
    // =================================================================================================
    //
    //   typ wyniku     zapytania (sprawdzone testem)                  co wiesz
    //   Page<T>        SELECT strony + SELECT count(*)                treść, totalElements, totalPages, hasNext
    //   Slice<T>       1 SELECT, pobiera size + 1 wierszy             treść, hasNext (bez sumy)
    //   List<T>        1 SELECT                                       tylko treść
    //
    // Page — DOMYŚLNIE dwa zapytania: {@code select count(*) from product p1_0 where p1_0.category=?} (to samo WHERE,
    //   bez ORDER BY i OFFSET). Spring Data pomija count, gdy sumę widać z samej strony (sprawdzone testem):
    //   pierwsza strona niepełna (3 wyniki przy size 10 → total = 3) albo ostatnia niepełna (offset 3 + 2 wiersze →
    //   total = 5). Pusta strona za końcem — count jest wykonywany.
    // Slice — statystyki pokazują 4 wczytane encje przy size 3: czwarty wiersz mówi tylko „jest dalej” i nie trafia
    //   do treści.
    //
    // PUŁAPKA: Page na dużej tabeli. Dlaczego? count(*) z warunkiem przy milionach wierszy potrafi być wolniejszy niż
    //   samo pobranie strony — i wykonuje się przy KAŻDYM kliknięciu „dalej”. Gdy interfejs nie pokazuje „strona 3 z 57”
    //   (np. „pokaż więcej”, nieskończone przewijanie), wybierz Slice.
    //
    // DOBRA PRAKTYKA: Page — gdy UI potrzebuje liczby stron; Slice — przewijanie „dalej/wstecz”; List z Pageable —
    //   „pierwsze 20 do podglądu”. Dlaczego? Płacisz tylko za informację, której naprawdę używasz.
    //   PYTANIE REKRUTACYJNE: „Czym różni się Page od Slice w Spring Data?”

    // =================================================================================================
    // 3. Sort — kierunek, kilka pól, Sort.Order; jednoznaczna kolejność
    // =================================================================================================
    //
    //   {@code Sort.by("price")}                                     order by price (rosnąco — domyślnie ASC)
    //   {@code Sort.by("price").descending()}                         order by price desc
    //   {@code Sort.by(Sort.Direction.DESC, "price").and(Sort.by("id"))} order by price desc, id
    //   {@code Sort.by(Sort.Order.asc("price"), Sort.Order.desc("name"))} order by price, name desc (każde pole swój kierunek)
    //   {@code Sort.by("price", "id")}                                order by price, id
    //
    // Sort to parametr metody ({@code List<Product> findByCategory(Category, Sort)}) albo część PageRequest;
    // JpaRepository ma gotowe {@code findAll(Sort)}. W przeciwieństwie do OrderBy w nazwie (Jpa03) kolejność wybiera
    // WYWOŁUJĄCY — np. użytkownik klikający nagłówek kolumny tabeli.
    //
    // PUŁAPKA: stronicowanie bez sortowania albo sortowanie po polu z remisami. Dlaczego? SQL bez ORDER BY (albo
    //   z remisem — trzy produkty po 59.00) nie gwarantuje kolejności MIĘDZY zapytaniami. Strona 1 i strona 2 to dwa
    //   osobne SELECT-y: baza może za drugim razem ułożyć remisy inaczej i ten sam produkt pojawi się na obu stronach,
    //   a inny nie pojawi się wcale. H2 w testach akurat zwraca remisy w kolejności id — to przypadek, nie gwarancja
    //   (PostgreSQL przy innym planie zapytania zwróci inaczej). Zawsze dodawaj na końcu pole unikalne:
    //   {@code Sort.by("price").and(Sort.by("id"))} — test: remisy 59.00 i 24.50 rozstrzyga id.
    //
    // PUŁAPKA: nazwa pola sortowania z zewnątrz (np. {@code ?sort=prize}). Dlaczego groźne? To błąd w CZASIE
    //   WYKONANIA, nie przy starcie: PropertyReferenceException „No property 'prize' found for type 'Product'; Did you
    //   mean 'price'” (sprawdzone testem; do bazy nic nie idzie). W REST zamień go na 400 Bad Request
    //   (s06_error_handling/Err03ControllerAdvice) albo dopuszczaj tylko pola z białej listy.
    //
    // DOBRA PRAKTYKA: domyślne sortowanie ustaw w kodzie, a nie zostawiaj bazie. Dlaczego? „Bez sortowania” w SQL
    //   znaczy „w dowolnej kolejności”, a dowolna kolejność zmienia się po aktualizacji bazy albo indeksu.

    // =================================================================================================
    // 4. @Query + Pageable (countQuery), Limit, Top10
    // =================================================================================================
    //
    //   {@code @Query("select p from Product p where p.price >= :minPrice")}
    //   {@code Page<Product> findPricedFrom(BigDecimal minPrice, Pageable pageable);}
    //
    // Spring Data dokleja ORDER BY z Pageable do Twojego JPQL i SAM wyprowadza zapytanie count (test:
    // {@code select count(*) from product p1_0 where p1_0.price>=?}). Przy prostych zapytaniach to wystarcza.
    // {@code countQuery = "select count(p.id) from …"} — własne zapytanie liczące, gdy automatyczne jest złe albo
    // wolne: złączenia (JOIN, s09), DISTINCT, GROUP BY, natywny SQL. Test: w logu widać nasze count(p1_0.id).
    //
    // Limit (Spring Data 3.2+, sprawdzone w Spring Data 2026): liczba wyników jako ARGUMENT —
    //   {@code List<Product> findByCategoryOrderByPriceAscIdAsc(Category category, Limit limit);}
    //   {@code Limit.of(2)} → {@code fetch first ? rows only}; {@code Limit.unlimited()} → zapytanie bez ograniczenia.
    // Top10/First w nazwie (Jpa03) robią to samo, ale liczba jest zaszyta w nazwie — Limit, gdy liczbę wybiera
    // wywołujący. Limit nie liczy stron (brak offsetu) — to „pierwsze N”, nie stronicowanie.
    //
    // DOBRA PRAKTYKA: gdy @Query ma JOIN albo DISTINCT, zawsze podaj countQuery. Dlaczego? Automatyczne count
    //   z takiego zapytania bywa niepoprawne (liczy wiersze złączenia zamiast encji) albo bardzo wolne.

    // =================================================================================================
    // 5. Paginacja klucza (keyset) — ScrollPosition i Window; Pageable w REST — tylko opis
    // =================================================================================================
    //
    // Dlaczego OFFSET jest wolny na dalekich stronach? {@code offset 100000 rows fetch first 20} każe bazie
    // ZNALEŹĆ i PRZESKOCZYĆ 100 000 wierszy, żeby oddać 20 — im dalsza strona, tym wolniej. Do tego, gdy w trakcie
    // przeglądania ktoś doda produkt, wszystko się przesuwa (duplikaty/pominięcia między stronami).
    //
    // Keyset: „daj 4 następne PO produkcie (price = 24.50, id = 11)” — baza używa indeksu i od razu skacze w to miejsce:
    //   {@code Window<Product> findFirst4ByOrderByPriceAscIdAsc(ScrollPosition position);}
    //   Window<Product> first = repository.findFirst4ByOrderByPriceAscIdAsc(ScrollPosition.keyset()); // początek
    //   ScrollPosition next = first.positionAt(first.size() - 1);   // KeysetScrollPosition {price=24.50, id=11}
    //   Window<Product> second = repository.findFirst4ByOrderByPriceAscIdAsc(next);
    // SQL drugiego okna (sprawdzone testem) — bez OFFSET:
    //   {@code … where p1_0.price>? or p1_0.price=? and p1_0.id>? order by p1_0.price,p1_0.id fetch first ? rows only}
    // Ostatnie okno ma hasNext() = false. Warunek: sortowanie musi być JEDNOZNACZNE (id na końcu), bo klucz ostatniego
    // elementu musi wskazywać dokładnie jedno miejsce. Wada: nie skoczysz od razu na „stronę 57” — tylko dalej/wstecz.
    // {@code ScrollPosition.offset()} daje Window oparte na OFFSET — to samo API, stara metoda.
    //
    // Pageable w kontrolerze REST (moduł bez weba — tylko opis): z modułem spring-boot-starter-webmvc i Spring Data
    // w kontrolerze wystarczy parametr {@code Pageable pageable} — Spring Data Web zbuduje go z
    // {@code ?page=0&size=20&sort=price,desc&sort=id}. Ustawienia: {@code spring.data.web.pageable.default-page-size}
    // (domyślnie 20), {@code max-page-size} (2000 — ochrona przed {@code size=1000000}), {@code one-indexed-parameters}.
    //
    // PUŁAPKA: zwracanie {@code Page<T>} prosto z kontrolera jako JSON. Dlaczego? Struktura JSON z PageImpl nie jest
    //   stabilna między wersjami — Spring Data 2026 przy takiej serializacji loguje ostrzeżenie „Serializing PageImpl
    //   instances as-is is not supported…” (sprawdzone w kodzie spring-data-commons 4.1.1; domyślny tryb to nadal
    //   DIRECT). Rozwiązanie: {@code @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)} albo właściwość
    //   {@code spring.data.web.pageable.serialization-mode=via-dto} — wtedy Page zamienia się w stabilny PagedModel
    //   (treść + blok „page” z size, number, totalElements, totalPages). Albo zwracaj własny rekord-DTO.
    //
    // DOBRA PRAKTYKA: listy, które mogą rosnąć bez końca (historia zamówień, logi), przewijaj kluczem (Window), a nie
    //   OFFSET. Dlaczego? Czas pobrania okna nie rośnie z numerem strony, a dopisane w trakcie wiersze nie przesuwają
    //   wyników.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Ostatni parametr Pageable / Sort / Limit / ScrollPosition — Spring Data rozpoznaje po typie.
     *   • {@code PageRequest.of(page, size, sort)} — page OD ZERA; ujemny / size 0 → IllegalArgumentException.
     *   • Page = SELECT + count (pomijany, gdy sumę widać ze strony); Slice = size + 1 wierszy, bez count;
     *     List z Pageable = sama treść.
     *   • {@code Sort.by("price").descending().and(Sort.by("id"))}, {@code Sort.Order.asc/desc} — zawsze kończ
     *     polem unikalnym (id), inaczej strony mogą się nakładać.
     *   • Złe pole w Sort → PropertyReferenceException przy WYWOŁANIU (nie przy starcie).
     *   • @Query + Pageable: count wyprowadzany sam; przy JOIN/DISTINCT podaj {@code countQuery}.
     *   • {@code Limit.of(n)} — „pierwsze n” jako argument; Top10 — n w nazwie.
     *   • Keyset: {@code Window<T> findFirstNBy…OrderBy…IdAsc(ScrollPosition)}, start {@code ScrollPosition.keyset()},
     *     dalej {@code window.positionAt(window.size() - 1)}; bez OFFSET.
     *   • REST: {@code ?page=&size=&sort=pole,desc}; Page jako JSON → VIA_DTO / PagedModel albo własne DTO.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? {@code PageRequest.of(1, 10)} dla kategorii, która ma 7 produktów — co zawiera strona
     *      i ile wynosi getTotalElements()?
     *   2. Ile zapytań SQL wykona metoda zwracająca Page, a ile zwracająca Slice? Kiedy Page wykona tylko jedno?
     *   3. ZNAJDŹ BŁĄD: {@code findAll(PageRequest.of(page, 20, Sort.by("price")))} — użytkownicy zgłaszają, że ten
     *      sam produkt widzą na stronie 1 i 2, a innego nie ma nigdzie.
     *   4. Co się stanie? Kontroler przekazuje do repozytorium {@code Sort.by(sortParam)}, a użytkownik wysłał
     *      {@code ?sort=prize}. Kiedy i jaki błąd?
     *   5. Kiedy podasz własne {@code countQuery} w @Query z Pageable?
     *   6. Czym różni się Limit od Pageable i od Top10 w nazwie metody?
     *   7. Dlaczego paginacja klucza (keyset) jest szybsza od OFFSET na dalekich stronach i jaki ma warunek?
     *   8. ZNAJDŹ BŁĄD: kontroler REST zwraca {@code Page<ProductDto>} prosto jako JSON. Co ostrzega Spring Data
     *      i jak to poprawić?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku ExerciseProductRepository.java (metody default z TODO), testy: Jpa05PagingSortingExercisesTest.
    // Dopisujesz metody zapytań z parametrami Pageable / Limit / ScrollPosition i wywołujesz je w ciałach metod default.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Strona 1 to DRUGA strona (od 0) — pomija 10 wierszy, a jest ich 7, więc treść jest pusta; to nie błąd.
     *      getTotalElements() = 7 (Spring Data wykonał count). Sprawdzone testem (sekcja 2: strona 5 z 3 → pusta, total 5).
     *   2. Page: zwykle 2 (SELECT strony + SELECT count); Slice: zawsze 1 (pobiera size + 1 wierszy). Page wykona
     *      jedno, gdy sumę widać z samej strony: pierwsza strona niepełna albo ostatnia niepełna. Sprawdzone testem.
     *   3. Sortowanie po samej cenie ma remisy, a każda strona to osobny SELECT — baza może ułożyć równe ceny za każdym
     *      razem inaczej. Poprawka: {@code Sort.by("price").and(Sort.by("id"))} — ostatnie pole unikalne.
     *   4. Przy WYWOŁANIU metody (nie przy starcie): PropertyReferenceException „No property 'prize' found for type
     *      'Product'; Did you mean 'price'” — zapytanie nie idzie do bazy. Sprawdzone testem (sekcja 3). W REST: obsłuż
     *      wyjątek jako 400 albo sprawdzaj pole z białą listą.
     *   5. Gdy automatyczne count byłoby błędne lub wolne: zapytania z JOIN (liczy wiersze złączenia), DISTINCT,
     *      GROUP BY, natywny SQL, albo gdy da się policzyć prościej (np. bez zbędnych złączeń).
     *   6. Limit to tylko „pierwsze n” (bez numeru strony, bez count), podawane jako argument. Pageable to numer
     *      strony + rozmiar + sortowanie (offset). Top10 to „pierwsze 10” zaszyte na stałe w nazwie metody.
     *   7. OFFSET każe bazie przejść i odrzucić wszystkie wcześniejsze wiersze; keyset zaczyna od miejsca wskazanego
     *      przez klucz ostatniego elementu (warunek WHERE, indeks), więc koszt nie rośnie z numerem strony. Warunek:
     *      jednoznaczne sortowanie (kończące się polem unikalnym) i przewijanie tylko dalej/wstecz.
     *   8. Spring Data loguje ostrzeżenie „Serializing PageImpl instances as-is is not supported…” — struktura JSON
     *      nie jest stabilna. Poprawka: {@code @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)}
     *      / {@code spring.data.web.pageable.serialization-mode=via-dto} (PagedModel) albo własny rekord-DTO strony.
     */
    // </editor-fold>
}

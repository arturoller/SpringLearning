package s08_data_jpa.jpa05_paging_sorting;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * <pre>
 * TEMAT: Stronicowanie i sortowanie — Pageable, PageRequest, Sort, Page kontra Slice
 *        (paging = stronicowanie, dzielenie wyników na strony; page = strona; pageable = „dający się stronicować”,
 *         opis żądanej strony; sort = sortowanie; slice = wycinek; limit = ograniczenie liczby wyników)
 *
 * W SKRÓCIE:
 *   Sklep ma 14 produktów, prawdziwy — 140 000. Nikt nie wczytuje wszystkich naraz: lista w aplikacji pokazuje
 *   stronę po 20. Spring Data robi to parametrem Pageable: {@code PageRequest.of(numerStrony, rozmiar, sort)}.
 *   Baza dostaje OFFSET i FETCH FIRST (LIMIT), a ty dostajesz Page — zawartość strony plus łączną liczbę wyników
 *   (dodatkowe zapytanie count) — albo tańszy Slice, który wie tylko, czy jest następna strona. Najważniejsza
 *   pułapka: stronicowanie bez jednoznacznego sortowania potrafi gubić i dublować wiersze między stronami.
 *
 * ANALOGIA: książka telefoniczna.
 *   Nie czytasz całej książki — otwierasz stronę 3 (Pageable). Spis treści mówi, ile jest stron (Page z count),
 *   ale jego przygotowanie kosztuje. Czasem wystarczy „przewróć kartkę, jeśli jest następna” (Slice). Gdy wpisy
 *   z tym samym nazwiskiem nie są dodatkowo ułożone po imieniu, dwa wydania książki mogą rozłożyć je inaczej —
 *   i ten sam Kowalski trafi raz na stronę 3, a raz na 4.
 *
 * JAK TO DZIAŁA:
 *   1. Pageable/Sort/Limit jako parametr metody repozytorium — Spring Data rozpoznaje je po TYPIE (nie są warunkami).
 *   2. Sort → ORDER BY (nazwy PÓL encji, sprawdzane dopiero przy wywołaniu); Pageable → OFFSET ? ROWS FETCH FIRST ? ROWS ONLY
 *      (składnia H2; inna baza dostanie np. LIMIT/OFFSET — tłumaczy Hibernate).
 *   3. Typ wyniku Page → po zapytaniu o zawartość drugie zapytanie: select count(...) z tym samym WHERE.
 *      Typ wyniku Slice → jedno zapytanie o rozmiar+1 wierszy; nadmiarowy wiersz mówi tylko „jest dalej”.
 *   4. Numer strony liczony OD ZERA: PageRequest.of(0, 5) to pierwsza strona.
 *
 * SŁÓWKA:
 *   page = strona; size = rozmiar; offset = przesunięcie (ile wierszy pominąć); total = łącznie; ascending /
 *   descending = rosnąco / malejąco; ignore case = bez rozróżniania wielkości liter; unpaged = bez stronicowania;
 *   tie = remis (równe wartości w sortowanej kolumnie); tie-breaker = rozstrzygnięcie remisu (drugi klucz sortowania);
 *   keyset = paginacja po kluczu („daj produkty po ostatnim widzianym”).
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa03QueryMethods (OrderBy i Top w nazwie metody),
 *             s08_data_jpa/Jpa04CustomQueries (@Query — tu z Pageable),
 *             t29_jdbc_databases/Jdbc01SqlBasics (ORDER BY i LIMIT w czystym SQL),
 *             t12_collections/Collections07ComparableComparator (sortowanie po kilku kluczach w Javie).
 * </pre>
 */
public final class Jpa05PagingSorting {

    private Jpa05PagingSorting() {
    }

    // =================================================================================================
    // 1. Sort — sortowanie przekazywane z zewnątrz
    // =================================================================================================
    //
    //   Sort.by("price")                                        → order by p1_0.price            (domyślnie rosnąco)
    //   Sort.by("price").descending().and(Sort.by("sku"))       → order by p1_0.price desc,p1_0.sku
    //   Sort.by(Sort.Direction.DESC, "price")                   → order by p1_0.price desc
    //   Sort.by(Sort.Order.asc("name").ignoreCase())            → order by lower(p1_0.name)
    //   repository.findByPriceLessThan(new BigDecimal("100"), Sort.by(...))   — Sort jako parametr metody z nazwy
    // Ta sama metoda repozytorium obsłuży każde sortowanie wybrane przez użytkownika — bez dziesięciu wariantów
    // findBy...OrderBy.... Sprawdzone testem (sekcja 1).
    //
    // PUŁAPKA: nazwa pola w Sort to zwykły String — kompilator jej nie sprawdzi, a Spring Data dopiero przy WYWOŁANIU:
    //   repository.findAll(PageRequest.of(0, 5, Sort.by("cena")))
    //   → org.springframework.data.core.PropertyReferenceException: No property 'cena' found for type 'Product'
    //   Dlaczego nie przy starcie (jak literówka w nazwie metody)? Sort przychodzi w czasie działania — np. od
    //   użytkownika. Dlatego sortowanie z zewnątrz ogranicza się do listy dozwolonych pól. Sprawdzone testem (sekcja 1).

    // =================================================================================================
    // 2. Pageable i Page — strona + łączna liczba wyników
    // =================================================================================================
    //
    //   Page<Product> page = repository.findAll(PageRequest.of(0, 5, Sort.by("price").descending().and(Sort.by("sku"))));
    // Prawdziwy SQL (dwa zapytania):
    //   select ... from products p1_0 order by p1_0.price desc,p1_0.sku offset ? rows fetch first ? rows only
    //   select count(p1_0.id) from products p1_0
    // Wynik: getContent() = 5 produktów, getTotalElements() = 14, getTotalPages() = 3, getNumber() = 0,
    // hasNext() = true, isLast() = false; nextPageable() = Page request [number: 1, size 5, sort: price: DESC,sku: ASC].
    // Strona za końcem (PageRequest.of(5, 5)) = pusta zawartość, ale total nadal 14 — bez wyjątku.
    // page.map(Product::getName) zamienia zawartość (np. na DTO), zachowując numer strony i liczby.
    // Sprawdzone testem (sekcja 2).
    //
    // Ciekawostka: Spring Data POMIJA zapytanie count, gdy wynik da się policzyć bez niego — np. pierwsza strona
    // ma mniej elementów niż rozmiar strony (findByCategory(KSIAZKI, PageRequest.of(0, 5)) → 3 produkty,
    // total = 3, jedno zapytanie). Sprawdzone testem (sekcja 2).
    //
    // PUŁAPKA: strony liczone od ZERA. PageRequest.of(1, 5) to DRUGA strona (wiersze 6–10), a toString() strony
    //   pokazuje „Page 1 of 3” dla getNumber() == 0 — dla człowieka numeruje od jedynki. Ujemny numer albo rozmiar 0:
    //   IllegalArgumentException: Page index must not be less than zero / Page size must not be less than one.
    //   Sprawdzone testem (sekcja 2).

    // =================================================================================================
    // 3. Slice — „czy jest następna strona?” bez liczenia wszystkiego
    // =================================================================================================
    //
    //   Slice<Product> findByStockGreaterThan(int stock, Pageable pageable);
    //   findByStockGreaterThan(0, PageRequest.of(0, 5, Sort.by("sku")))
    // Prawdziwy SQL — JEDNO zapytanie, z limitem 6 (rozmiar + 1; widać to w logu org.hibernate.orm.jdbc.bind):
    //   select ... from products p1_0 where p1_0.stock>? order by p1_0.sku fetch first ? rows only
    // Szósty wiersz nie trafia do wyniku — mówi tylko, że hasNext() == true. Slice nie ma getTotalElements().
    // Sprawdzone testem (sekcja 3).
    //
    // DOBRA PRAKTYKA: Slice do przewijania („pokaż więcej”, nieskończona lista, przetwarzanie paczkami),
    //   Page tylko tam, gdzie naprawdę wyświetlasz „strona 3 z 120”.
    //   Dlaczego? count(*) na dużej tabeli z warunkami bywa droższy niż samo pobranie strony — i wykonuje się
    //   przy każdej stronie.
    //
    // Inne narzędzia: Limit.of(2) jako parametr (findByCategoryOrderByPriceDesc(ELEKTRONIKA, Limit.of(2)) →
    // "fetch first ? rows only"), Pageable.unpaged() (wszystko, bez LIMIT i bez count). Przy bardzo głębokich
    // stronach (OFFSET 100000) baza i tak musi przejść pominięte wiersze — wtedy szuka się paginacji po kluczu
    // (keyset, w Spring Data: ScrollPosition i Window) — poza zakresem tej lekcji.

    // =================================================================================================
    // 4. PUŁAPKA: sortowanie po kolumnie z powtórzeniami bez drugiego klucza
    // =================================================================================================
    //
    // W sklepie KSI-002 (Java. Podstawy) i DOM-002 (Lampka biurkowa) kosztują po 129.00. Stronicujemy po 7,
    // sortując TYLKO po cenie rosnąco: 7. i 8. pozycja to te dwa produkty — granica stron wypada w środku remisu.
    //   strona 0: ..., KSI-003 99.00, [jeden ze 129.00]
    //   strona 1: [drugi ze 129.00], ELE-003 349.90, ...
    // KTÓRY jest na której stronie — decyduje baza. SQL nie gwarantuje kolejności wierszy o równych wartościach
    // sortowania, a każda strona to OSOBNE zapytanie (inny OFFSET), więc każda może ułożyć remis po swojemu.
    // To nie teoria. Prawdziwe wyniki z H2 2.4 (pięć uruchomień tego samego kodu, strony po 7, Sort.by("price")):
    //   3 razy: KSI-002 na końcu strony 0 I na początku strony 1, DOM-002 — na żadnej (zgubiony!)
    //   1 raz:  odwrotnie — DOM-002 dwa razy, KSI-002 zgubiony
    //   1 raz:  poprawnie
    // Przy Sort.by("category") i stronach po 3 każde z pięciu uruchomień gubiło i dublowało inne produkty.
    // Skąd losowość? Przy OFFSET/FETCH H2 nie sortuje całej tabeli, tylko potrzebny fragment (Utils.sortTopN →
    // partialQuickSort) i wybiera element osiowy LOSOWO (MathUtils.randomInt) — sprawdzone w jarze h2-2.4.240.
    // Bez LIMIT (findAll(Sort.by("price"))) H2 sortował całość stabilnie i kolejność była stała. Inne bazy mają
    // własne powody (dokumentacja PostgreSQL ostrzega, że różne LIMIT/OFFSET mogą dać różne plany zapytania).
    //
    //   ŹLE:   PageRequest.of(page, 7, Sort.by("price"))
    //   DOBRZE: PageRequest.of(page, 7, Sort.by("price").and(Sort.by("sku")))     ← sku jest unikalne = rozstrzyga remis
    // Sprawdzone testem (sekcja 4: remis na granicy stron i dziesięć powtórzeń stabilnego podziału) oraz
    // uruchomieniami opisanymi wyżej (test nie sprawdza wersji „źle” — jej wynik jest losowy).
    //
    // DOBRA PRAKTYKA: ostatnim kluczem sortowania przy stronicowaniu zawsze rób kolumnę UNIKALNĄ (id, sku).
    //   Dlaczego? Dopiero wtedy kolejność jest całkowicie wyznaczona i każda strona jest „ciągiem dalszym” poprzedniej.

    // =================================================================================================
    // 5. @Query + Pageable — zapytanie liczące wyprowadzone automatycznie
    // =================================================================================================
    //
    //   @Query("select p from Product p where p.price >= :minPrice")
    //   Page<Product> findPricedFrom(BigDecimal minPrice, Pageable pageable);
    // Spring Data dokleja ORDER BY z Pageable i sam buduje zapytanie liczące:
    //   select ... from products p1_0 where p1_0.price>=? order by p1_0.price fetch first ? rows only
    //   select count(*) from products p1_0 where p1_0.price>=?
    // Przy skomplikowanym JPQL (złączenia, GROUP BY) podaj je sam: @Query(value = "...", countQuery = "select count(p) ...").
    // Sprawdzone testem (sekcja 5).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • PageRequest.of(numer od 0, rozmiar[, Sort]); Sort.by("pole").descending().and(Sort.by("unikalne")).
     *   • Page: zawartość + getTotalElements/getTotalPages (dodatkowe select count); Slice: zawartość + hasNext
     *     (pobiera rozmiar+1, bez count). Limit.of(n) — samo ograniczenie.
     *   • Pageable/Sort/Limit działają w findAll, w zapytaniach z nazw metod i w @Query (count wyprowadzany sam;
     *     przy trudnym JPQL — countQuery).
     *   • Nazwy pól w Sort sprawdzane dopiero przy wywołaniu → PropertyReferenceException.
     *   • Strona za końcem = pusta, bez wyjątku. Page.map(...) — zamiana zawartości z zachowaniem metadanych.
     *   • Stronicowanie: ostatni klucz sortowania UNIKALNY — inaczej remisy mogą skakać między stronami.
     *
     * PYTANIA KONTROLNE:
     *   1. Ile zapytań SQL wysyła findAll(PageRequest.of(0, 5)) dla 14 produktów, a ile findByStockGreaterThan(0,
     *      PageRequest.of(0, 5)) zwracające Slice? Jakie to zapytania?
     *   2. Co się stanie? {@code repository.findAll(PageRequest.of(0, 5, Sort.by("cena")))} — pole w encji to price.
     *   3. Co się stanie? {@code repository.findAll(PageRequest.of(5, 5))}, a w bazie jest 14 produktów.
     *   4. ZNAJDŹ BŁĄD: lista produktów w sklepie internetowym stronicuje po 20 z {@code Sort.by("category")}.
     *      Klienci zgłaszają, że niektóre produkty pojawiają się na dwóch stronach, a innych nie ma wcale.
     *   5. Dlaczego PageRequest.of(1, 10) nie zwraca pierwszych dziesięciu wyników?
     *   6. Kiedy wybierzesz Slice zamiast Page i dlaczego?
     *   7. Czy findByCategory(KSIAZKI, PageRequest.of(0, 5)) dla trzech książek wyśle zapytanie count? Dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jpa05PagingSortingExercisesTest (uruchamiasz sam ▶; @Tag("cwiczenie"))
    // =================================================================================================
    //
    // Używasz ProductRepository z tej lekcji (nic do niego nie dopisujesz). Przed każdym testem w bazie jest
    // 14 produktów sklepu.

    /**
     * ĆWICZENIE 1 (łatwe): DRUGA strona wszystkich produktów, po 4 na stronę, posortowanych po nazwie rosnąco.
     * Oczekiwane nazwy: Kawa ziarnista 1kg, Kurtka zimowa, Lampka biurkowa, Laptop Pro 14 (14 produktów, 4 strony).
     * Podpowiedź: findAll(PageRequest.of(..., ..., Sort.by(...))) — pamiętaj, od ilu liczą się strony.
     */
    static Page<Product> exercise1(ProductRepository repository) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbierz SKU wszystkich produktów na stanie (stock większy niż 0), posortowanych po sku,
     * przechodząc po kolejnych wycinkach (Slice) o rozmiarze {@code pageSize} — tak, jak przetwarza się duże
     * tabele paczkami. Podpowiedź: findByStockGreaterThan(0, PageRequest.of(0, pageSize, Sort.by("sku"))), potem
     * pętla: dopóki slice.hasNext() → kolejny wycinek z slice.nextPageable().
     */
    static List<String> exercise2(ProductRepository repository, int pageSize) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): DRUGA strona (po 7) produktów posortowanych po cenie MALEJĄCO, z jednoznaczną
     * kolejnością przy równych cenach (rozstrzygaj po sku rosnąco). Pierwszy na stronie ma być KSI-002
     * (129.00 — remis z DOM-002, który przy sortowaniu po sku trafia na koniec strony pierwszej).
     */
    static Page<Product> exercise3(ProductRepository repository) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): strona NAZW produktów z kategorii (posortowanych po cenie rosnąco, remisy po sku)
     * jako {@code Page<String>} z zachowanymi metadanymi (getTotalElements, getTotalPages, getNumber).
     * Dla SPOZYWCZE, strona 1, rozmiar 2: zawartość [Kawa ziarnista 1kg], razem 3 elementy, 2 strony.
     * Podpowiedź: findByCategory(..., PageRequest...) i Page.map(...).
     */
    static Page<String> exercise4(ProductRepository repository, Category category, int page, int size) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Page: dwa — select ... offset ? rows fetch first ? rows only oraz select count(p1_0.id) from products p1_0.
     *      Slice: jedno — select ... fetch first ? rows only z limitem 6 (rozmiar + 1), bez count.
     *      Sprawdzone testem (sekcje 2 i 3).
     *   2. org.springframework.data.core.PropertyReferenceException: No property 'cena' found for type 'Product' —
     *      przy WYWOŁANIU metody (nie przy starcie), bo Sort powstaje w czasie działania. Sprawdzone testem (sekcja 1).
     *   3. Pusta zawartość strony (strona 5 to wiersze 26–30), getTotalElements() = 14, getTotalPages() = 3 —
     *      bez wyjątku. Sprawdzone testem (sekcja 2).
     *   4. Sortowanie tylko po kategorii: dziesiątki produktów mają tę samą kategorię, a kolejność remisów nie jest
     *      określona — każde zapytanie o stronę (inny OFFSET) może ułożyć je inaczej. W H2 z tej lekcji stronicowanie
     *      po kategorii przy każdym z pięciu uruchomień gubiło i dublowało produkty. Poprawka: dodać unikalny
     *      klucz na końcu, np. Sort.by("category").and(Sort.by("sku")). Sprawdzone uruchomieniem i testem (sekcja 4).
     *   5. Strony liczone są od zera: of(1, 10) to druga strona (wiersze 11–20). Pierwsza to of(0, 10).
     *   6. Gdy nie potrzebujesz łącznej liczby wyników (przewijanie, „pokaż więcej”, przetwarzanie paczkami) —
     *      Slice oszczędza zapytanie count, które przy dużych tabelach i każdej stronie bywa kosztowne.
     *   7. Nie: pierwsza strona ma mniej elementów (3) niż rozmiar (5), więc Spring Data wie, że to wszystko,
     *      i ustawia total = 3 bez zapytania count. Sprawdzone testem (sekcja 2).
     */
    // </editor-fold>
}

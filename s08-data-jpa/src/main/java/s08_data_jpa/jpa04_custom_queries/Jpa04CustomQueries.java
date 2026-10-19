package s08_data_jpa.jpa04_custom_queries;

import java.math.BigDecimal;
import java.util.List;

/**
 * <pre>
 * TEMAT: Własne zapytania — @Query (JPQL i natywne SQL), projekcje i @Modifying
 *        (query = zapytanie; JPQL = Jakarta Persistence Query Language, język zapytań o encje; native = natywny,
 *         czyli SQL konkretnej bazy; projection = projekcja, wynik z wybranymi polami; DTO = data transfer object,
 *         obiekt do przenoszenia danych; modifying = modyfikujący, zmieniający dane)
 *
 * W SKRÓCIE:
 *   Gdy nazwa metody robi się za długa albo potrzebujesz czegoś, czego nie da się w niej wyrazić (agregacja,
 *   GROUP BY, funkcje), piszesz zapytanie sam w @Query. JPQL wygląda jak SQL, ale mówi o ENCJACH i POLACH
 *   (Product, p.price), więc Spring Data sprawdza je już przy starcie. Natywne SQL mówi o TABELACH i KOLUMNACH —
 *   daje pełną moc bazy, ale nikt go nie sprawdza przed wykonaniem. Projekcje (interfejs albo rekord) pobierają
 *   tylko potrzebne kolumny. @Modifying pozwala wykonać UPDATE/DELETE na wielu wierszach naraz — z jedną
 *   groźną pułapką: encje w kontekście utrwalania o tym nie wiedzą.
 *
 * ANALOGIA: zamówienie w restauracji.
 *   Zapytanie z nazwy metody to danie z karty („schabowy z ziemniakami”). @Query z JPQL to danie spoza karty
 *   opisane językiem kuchni — kucharz od razu powie, czy ma składniki (walidacja przy starcie). Natywne SQL to
 *   wejście na zaplecze i gotowanie samemu: wszystko wolno, ale nikt nie sprawdzi przepisu, zanim się przypali.
 *   Projekcja to „poproszę tylko surówkę” — nie płacisz za cały zestaw.
 *
 * JAK TO DZIAŁA:
 *   1. Przy starcie Spring Data widzi @Query na metodzie i NIE rozbiera jej nazwy (Jpa03QueryMethods).
 *   2. Zapytanie JPQL jest od razu przekazywane do Hibernate'a do sprawdzenia (Query validation): zła nazwa pola
 *      albo encji = błąd startu. Natywne SQL trafia do bazy dopiero przy wywołaniu metody.
 *   3. Parametry wiążą się po nazwie (:category — nazwa parametru metody, dostępna dzięki flagze kompilatora
 *      -parameters ustawionej przez spring-boot-starter-parent) albo po numerze (?1).
 *   4. Typ wyniku decyduje o projekcji: encja (Product), interfejs (proxy z getterami), rekord/klasa DTO
 *      (konstruktor). Dla projekcji Spring Data pobiera tylko potrzebne kolumny.
 *   5. @Modifying przełącza wykonanie z getResultList() na executeUpdate(); wynik int = liczba zmienionych wierszy.
 *      UPDATE/DELETE wymaga aktywnej transakcji.
 *
 * SŁÓWKA:
 *   query = zapytanie; native = natywny; parameter = parametr; named = nazwany; positional = pozycyjny;
 *   projection = projekcja; alias = nazwa zastępcza (as ...); aggregate = agregat (count, sum, avg);
 *   group by = grupuj według; bulk update = zbiorcza aktualizacja; stale = nieaktualny; flush = wypchnij zmiany;
 *   clear = wyczyść; constructor expression = wyrażenie konstruktora (select new ...).
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa03QueryMethods (zapytania z nazw metod),
 *             s08_data_jpa/Jpa01Entities (kontekst utrwalania, dirty checking),
 *             t29_jdbc_databases/Jdbc07SqlAggregationIndexes (GROUP BY i agregaty w czystym SQL),
 *             t09_records/Records01Basics (rekordy jako DTO),
 *             s09_jpa_relations/Rel06EntityToDto (projekcje i DTO przy relacjach),
 *             s10_transactions/Tx01Transactional (skąd bierze się transakcja).
 * </pre>
 */
public final class Jpa04CustomQueries {

    private Jpa04CustomQueries() {
    }

    // =================================================================================================
    // 1. @Query z JPQL — zapytanie o encje, sprawdzane przy starcie
    // =================================================================================================
    //
    //   @Query("select p from Product p where p.category = :category and p.price <= :maxPrice order by p.price")
    //   List<Product> findInCategoryUpTo(Category category, BigDecimal maxPrice);
    // Prawdziwy SQL:
    //   select p1_0.id,p1_0.category,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock from products p1_0
    //   where p1_0.category=? and p1_0.price<=? order by p1_0.price
    //
    // JPQL = nazwa ENCJI (Product, nie products) i nazwy PÓL (p.price). Parametry:
    //   :category      nazwany — wiąże się z parametrem metody o tej nazwie (@Param("category") zbędne, bo
    //                  spring-boot-starter-parent kompiluje z flagą -parameters; @Param zadziała zawsze);
    //   ?1             pozycyjny — pierwszy parametr metody (starszy styl, łatwo pomylić kolejność).
    // Sprawdzone testem (sekcja 1).
    //
    // Co się stanie przy błędzie w JPQL? Kontekst NIE wystartuje (prawdziwy komunikat):
    //   BeanCreationException: ... Query validation failed for 'select p from Product p where p.cena > 100'
    //   └ ... UnknownPathException: Could not resolve attribute 'cena' of 's08_data_jpa.jpa04_custom_queries.Product'
    // Sprawdzone testem (sekcja 1: mała aplikacja z repozytorium JpqlTypoRepository).
    //
    // DOBRA PRAKTYKA: dłuższe zapytania w bloku tekstowym (text block """ ... """ — Java 15+), jak findCategoryStats.
    //   Dlaczego? Bez sklejania stringów i zgubionych spacji typu "...p.price" + "from Product" → "p.pricefrom".

    // =================================================================================================
    // 2. Natywne SQL — @NativeQuery albo @Query(nativeQuery = true)
    // =================================================================================================
    //
    //   @NativeQuery("select * from products where stock = 0 order by sku")                    ← nowszy skrót
    //   @Query(value = "select * from products where category = :category order by price desc limit :limit",
    //          nativeQuery = true)                                                             ← starszy zapis
    // Tu piszesz nazwy TABEL i KOLUMN oraz składnię swojej bazy (LIMIT w H2/PostgreSQL/MySQL; w Oracle inaczej).
    // Hibernate przekazuje SQL do bazy prawie bez zmian:
    //   select * from products where category = ? order by price desc limit ?
    // W natywnym SQL nie ma enumów ani encji — kategorię przekazujesz jako tekst ("ELEKTRONIKA").
    // Sprawdzone testem (sekcja 2).
    //
    // PUŁAPKA: natywnego SQL nikt nie sprawdza przy starcie. ProductMistakesRepository.findExpensiveWithTypo
    //   ("where cena > 100") NIE psuje startu — wybucha dopiero przy wywołaniu:
    //   InvalidDataAccessResourceUsageException: Could not prepare statement [... "CENA" ...]
    //   (dalszą część komunikatu pisze H2 w języku systemu — na polskim Windowsie: Kolumna "CENA" nie istnieje).
    //   Dlaczego? Spring Data nie zna schematu bazy, a Hibernate nie parsuje natywnego SQL. Zapytanie natywne
    //   MUSI mieć test, który je wykonuje. Sprawdzone testem (sekcja 2).
    //
    // DOBRA PRAKTYKA: najpierw JPQL, natywne SQL tylko wtedy, gdy JPQL nie wystarcza (funkcje okna, podpowiedzi
    //   dla optymalizatora, specyficzne typy bazy). Dlaczego? JPQL jest sprawdzany przy starcie i przenośny
    //   między bazami; natywne SQL wiąże cię z jedną bazą i ujawnia błędy dopiero w działaniu.

    // =================================================================================================
    // 3. Projekcje interfejsowe — tylko potrzebne kolumny
    // =================================================================================================
    //
    //   interface ProductSummary { String getName(); BigDecimal getPrice(); }
    //   List<ProductSummary> findByCategoryOrderByPriceAsc(Category category);
    // Prawdziwy SQL — tylko dwie kolumny zamiast sześciu:
    //   select p1_0.name,p1_0.price from products p1_0 where p1_0.category=? order by p1_0.price
    // Wynik to obiekty-pośrednicy (proxy JDK) implementujące interfejs — NIE encje: nie trafiają do kontekstu
    // utrwalania, zmiana ich nic nie zapisze (zresztą nie mają setterów).
    //
    // Agregacja (czego nie da się zapisać nazwą metody) — aliasy muszą pasować do getterów:
    //   select p.category as category, count(p) as productCount, sum(p.price * p.stock) as stockValue
    //   from Product p group by p.category order by p.category                      → List<CategoryStats>
    // SQL: select p1_0.category,count(p1_0.id),sum((p1_0.price*p1_0.stock)) from products p1_0
    //      group by p1_0.category order by p1_0.category
    // Sprawdzone testem (sekcja 3).
    //
    // DOBRA PRAKTYKA: do ekranów „tylko do odczytu” (listy, raporty) zwracaj projekcje, a nie encje.
    //   Dlaczego? Mniej kolumn z bazy, brak śledzenia zmian przez Hibernate i brak ryzyka, że ktoś zmieni encję
    //   przy okazji wyświetlania listy.

    // =================================================================================================
    // 4. Rekord DTO jako wynik — z „new” i BEZ „new”
    // =================================================================================================
    //
    //   record ProductPrice(String name, BigDecimal price) {}
    //
    //   a) z nazwy metody:   List<ProductPrice> findByCategoryOrderByPriceDesc(Category category);
    //   b) JPQL bez new:     @Query("select p from Product p where p.category = :category order by p.price")
    //                        List<ProductPrice> findPrices(Category category);
    //   c) JPQL z new:       @Query("select new s08_data_jpa.jpa04_custom_queries.ProductPrice(p.name, p.price) ...")
    //   d) natywne SQL:      @NativeQuery("select name, price from products where category = :category order by price")
    // Wszystkie cztery działają w Spring Data 2026.0 (spring-data-jpa 4.1) — sprawdzone testem (sekcja 4).
    // Ciekawostka b): zapytanie mówi "select p", a do bazy idzie
    //   select p1_0.name,p1_0.price from products p1_0 where p1_0.category=? order by p1_0.price
    // — Spring Data sam przepisał zapytanie na kolumny konstruktora rekordu.
    // W starszych tutorialach zobaczysz tylko wariant c) (pełna nazwa klasy w stringu — literówka w pakiecie
    // wychodzi przy starcie). Sprawdzone zachowanie b) i d) dotyczy wersji z tego kursu — w starych wersjach
    // Spring Data DTO bez „new” nie działało.
    //
    // Interfejs czy rekord? Rekord to zwykły, niezmienny obiekt (equals, toString, łatwy do testów i JSON-a);
    // interfejs to proxy (equals po tożsamości). Do nowego kodu wybieraj rekordy.

    // =================================================================================================
    // 5. @Modifying — UPDATE i DELETE na wielu wierszach
    // =================================================================================================
    //
    //   @Transactional
    //   @Modifying
    //   @Query("update Product p set p.price = p.price * :factor where p.category = :category")
    //   int changePrices(BigDecimal factor, Category category);          // wynik = liczba zmienionych wierszy
    // Prawdziwy SQL — JEDNO polecenie zamiast wczytywania i zapisywania każdej encji:
    //   update products p1_0 set price=(p1_0.price*cast(? as numeric(10,2))) where p1_0.category=?
    //   delete from products p1_0 where p1_0.stock=0                     (deleteSoldOut)
    //
    // Dwa błędy, przy których aplikacja WSTAJE, a wybucha wywołanie (ProductMistakesRepository):
    //   • brak @Modifying → InvalidDataAccessApiUsageException: Query executed via 'getResultList()' or
    //     'getSingleResult()' must be a 'select' query [update Product p ...]
    //   • brak transakcji → InvalidDataAccessApiUsageException: No active transaction for update or delete query
    //     (przyczyna: jakarta.persistence.TransactionRequiredException).
    // Sprawdzone testem (sekcja 5).
    //
    // PUŁAPKA: brak @Transactional przy @Modifying NIE wychodzi w testach @DataJpaTest — test ma własną
    //   transakcję, więc metoda działa. Wybucha dopiero w aplikacji, gdy wołasz ją z serwisu bez transakcji.
    //   Dlaczego? Metody zadeklarowane w twoim interfejsie (z @Query) nie dziedziczą @Transactional
    //   z SimpleJpaRepository. Test sekcji 5 kończy transakcję testu (TestTransaction.end()) i pokazuje wyjątek.
    //   Zasada: @Transactional przy każdej metodzie @Modifying (albo na serwisie, który ją woła — s10_transactions).

    // =================================================================================================
    // 6. Nieaktualne encje po zbiorczym UPDATE — clearAutomatically i flushAutomatically
    // =================================================================================================
    //
    // Zbiorczy UPDATE idzie prosto do bazy, z POMINIĘCIEM kontekstu utrwalania. Prawdziwy przebieg (test sekcji 6):
    //   A. book = findBySku("KSI-001")      → encja w kontekście, cena 79.00
    //      changePrices(2, KSIAZKI)          → w bazie 158.00
    //      findById(book.getId())            → TEN SAM obiekt z pamięci, cena 79.00 (nieaktualna!)
    //   B. book.setStock(99); flush           → Hibernate zapisuje WSZYSTKIE kolumny encji, także starą cenę 79.00
    //                                           → wynik zbiorczego UPDATE PRZEPADŁ (w bazie znowu 79.00)
    //   C. @Modifying(flushAutomatically = true, clearAutomatically = true):
    //      przed UPDATE flush oczekujących zmian, po UPDATE clear kontekstu → kolejny findById robi SELECT
    //      i widzi 158.00, a niezapisana wcześniej zmiana stanu (50) nie ginie.
    // Sprawdzone testem (sekcja 6).
    //
    // PUŁAPKA: clearAutomatically bez flush mogłoby wyrzucić z kontekstu niezapisane zmiany. W teście Hibernate
    //   i tak wypchnął zmianę przed UPDATE (flush automatyczny przy zapytaniu JPQL dotyczącym tej samej tabeli),
    //   ale flushAutomatically = true gwarantuje to zawsze, niezależnie od trybu flush i rodzaju zapytania.
    //
    // DOBRA PRAKTYKA: zbiorcze UPDATE/DELETE z @Modifying(flushAutomatically = true, clearAutomatically = true)
    //   i najlepiej na początku transakcji albo w osobnej transakcji — zanim wczytasz encje, które zmieniają.
    //   Dlaczego? Clear odłącza WSZYSTKIE encje — kod, który dalej trzyma stare obiekty, zmienia je już na próżno.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Query("JPQL") — encje i pola (Product, p.price), parametry :nazwa albo ?1; błąd = błąd STARTU.
     *   • @NativeQuery("SQL") / @Query(value = "SQL", nativeQuery = true) — tabele i kolumny; błąd dopiero przy WYWOŁANIU.
     *   • Projekcja interfejsowa: gettery = pola encji albo aliasy (as nazwa); pobiera tylko potrzebne kolumny; proxy.
     *   • Rekord DTO: z nazwy metody, z JPQL (z new albo — w Spring Data 2026.0 — bez new), z natywnego SQL.
     *   • UPDATE/DELETE: @Modifying + @Transactional; wynik int = liczba wierszy. Bez @Modifying → "must be a
     *     'select' query", bez transakcji → "No active transaction for update or delete query".
     *   • Po zbiorczym UPDATE encje w kontekście są nieaktualne, a ich flush nadpisuje bazę starymi wartościami →
     *     @Modifying(flushAutomatically = true, clearAutomatically = true).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się JPQL od natywnego SQL? Podaj dwie różnice widoczne w treści zapytania.
     *   2. Co się stanie? W @Query napiszesz "select p from Product p where p.cena > 100" i uruchomisz testy.
     *   3. Co się stanie? To samo zapytanie zapiszesz jako @NativeQuery("select * from products where cena > 100").
     *   4. ZNAJDŹ BŁĄD: {@code @Query("update Product p set p.stock = 0 where p.category = :category")
     *      int clearStock(Category category);} — testy @DataJpaTest przechodzą? Co się dzieje w aplikacji?
     *   5. Dlaczego projekcja ProductSummary jest szybsza od wczytania encji Product? Co jest w zapytaniu SQL?
     *   6. Co się stanie? W transakcji wczytujesz produkt (cena 79.00), wołasz changePrices(2, KSIAZKI) bez
     *      clearAutomatically, zmieniasz stan produktu i robisz flush. Jaka cena jest w bazie po wszystkim?
     *   7. Jak sprawić, żeby Spring Data wypełnił rekord ProductPrice bez wyrażenia "select new ..."?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jpa04CustomQueriesExercisesTest (uruchamiasz sam ▶; @Tag("cwiczenie"))
    // =================================================================================================
    //
    // Dopisuj metody do ProductExerciseRepository (ten pakiet) i wywołuj je w metodach exerciseN.
    // Przed każdym testem w bazie jest 14 produktów sklepu.

    /**
     * ĆWICZENIE 1 (łatwe): JPQL z parametrem nazwanym — produkty, których stan jest MNIEJSZY niż {@code threshold},
     * posortowane po stanie rosnąco, a przy równym stanie po sku. Dla 5: ELE-002, SPO-003, DOM-001, KSI-003, ELE-004.
     * Podpowiedź: {@code @Query("select p from Product p where ... order by p.stock, p.sku")}.
     */
    static List<Product> exercise1(ProductExerciseRepository repository, int threshold) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): wartość towaru w magazynie (cena × stan) dla każdego produktu z kategorii, jako rekord
     * {@link StockValue}, posortowane po sku. Dla KSIAZKI: KSI-001 = 1185.00, KSI-002 = 1161.00, KSI-003 = 297.00.
     * Policz w zapytaniu, nie w Javie. Podpowiedź: {@code select p.sku, p.price * p.stock from Product p ...}
     * i typ wyniku {@code List<StockValue>} — kolejność kolumn = kolejność składowych rekordu.
     */
    static List<StockValue> exercise2(ProductExerciseRepository repository, Category category) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): natywne SQL — {@code limit} najtańszych produktów z kategorii (kategoria jako tekst,
     * np. "SPOZYWCZE"). Dla SPOZYWCZE i 2: SPO-002, SPO-003. Podpowiedź: {@code @NativeQuery}, ORDER BY ... LIMIT.
     */
    static List<Product> exercise3(ProductExerciseRepository repository, String category, int limit) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): obniż ceny wszystkich produktów z kategorii o {@code percent} procent JEDNYM
     * zapytaniem UPDATE i zwróć liczbę zmienionych wierszy. Test najpierw wczyta produkt tej kategorii (będzie
     * w kontekście), a po twojej metodzie zrobi findById — musi zobaczyć NOWĄ cenę.
     * Podpowiedź: mnożnik = (100 - percent) / 100 jako BigDecimal; @Transactional, @Modifying(...= true).
     */
    static int exercise4(ProductExerciseRepository repository, Category category, int percent) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Pomocnik dla ćwiczenia 4 (możesz użyć): mnożnik ceny po obniżce o {@code percent} procent, np. 10 → 0.90. */
    static BigDecimal discountFactor(int percent) {
        return BigDecimal.valueOf(100 - percent).movePointLeft(2);
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. JPQL używa nazw encji i pól (Product p, p.price) i jest tłumaczony przez Hibernate'a na SQL danej bazy;
     *      natywne SQL używa nazw tabel i kolumn (products, price) i składni konkretnej bazy (np. LIMIT). JPQL jest
     *      sprawdzany przy starcie, natywne SQL — dopiero przy wykonaniu.
     *   2. Testy lekcji nie wystartują: BeanCreationException ... Query validation failed for 'select p from Product p
     *      where p.cena > 100', a na końcu łańcucha UnknownPathException: Could not resolve attribute 'cena' of
     *      's08_data_jpa.jpa04_custom_queries.Product'. Sprawdzone testem (sekcja 1).
     *   3. Kontekst wystartuje, a wyjątek InvalidDataAccessResourceUsageException: Could not prepare statement
     *      [... "CENA" ...] poleci dopiero przy wywołaniu metody. Sprawdzone testem (sekcja 2).
     *   4. Brak @Modifying (i brak @Transactional). Testy NIE przechodzą: Spring Data wykona UPDATE przez
     *      getResultList() → InvalidDataAccessApiUsageException: Query executed via 'getResultList()' or
     *      'getSingleResult()' must be a 'select' query. Po dodaniu samego @Modifying testy przejdą (mają transakcję),
     *      ale w aplikacji bez transakcji poleci "No active transaction for update or delete query".
     *      Poprawka: @Transactional + @Modifying. Sprawdzone testem (sekcja 5).
     *   5. Zapytanie pobiera tylko dwie kolumny (select p1_0.name,p1_0.price ...), a wynik to lekkie proxy — Hibernate
     *      nie buduje encji, nie robi im zdjęć do dirty checking i nie trzyma ich w kontekście. Sprawdzone testem (sekcja 3).
     *   6. 79.00 — wynik zbiorczego UPDATE przepada. Encja w kontekście ma starą cenę; flush zapisuje wszystkie jej
     *      kolumny, więc nadpisuje 158.00 starą wartością. Sprawdzone testem (sekcja 6).
     *   7. Zadeklarować typ wyniku List<ProductPrice>: z nazwy metody (findBy...), z @Query JPQL (Spring Data 2026.0
     *      sam przepisze "select p" na kolumny konstruktora) albo z @NativeQuery (kolumny w kolejności składowych).
     *      Sprawdzone testem (sekcja 4).
     */
    // </editor-fold>
}

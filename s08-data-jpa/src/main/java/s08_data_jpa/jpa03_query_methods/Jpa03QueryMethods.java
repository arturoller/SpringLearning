package s08_data_jpa.jpa03_query_methods;

/**
 * <pre>
 * TEMAT: Zapytania z nazw metod — findBy…, countBy…, existsBy…, deleteBy…, Top/First, typy wyniku
 *        (derived query = zapytanie pochodne, wyprowadzone z nazwy metody; keyword = słowo kluczowe;
 *         PartTree = drzewo części nazwy; subject = podmiot (co zwrócić); predicate = warunek (które wiersze))
 *
 * W SKRÓCIE:
 *   W repozytorium Spring Data wystarczy zadeklarować metodę o odpowiedniej NAZWIE, np.
 *   {@code List<Product> findByCategoryAndPriceLessThan(Category category, BigDecimal maxPrice)}, a Spring Data przy
 *   starcie aplikacji rozbierze nazwę na słowa (find… By Category And Price LessThan), sprawdzi, czy encja ma takie
 *   pola, i zbuduje zapytanie JPA. Typ zwracany decyduje o kształcie wyniku: lista, Optional, pojedynczy obiekt,
 *   Stream, boolean, long. Literówka w nazwie wychodzi przy STARCIE, nie na produkcji o trzeciej w nocy.
 *
 * ANALOGIA: zamawianie w barze według szablonu.
 *   Barman (Spring Data) rozumie zdania ułożone ze stałych klocków: „daj mi (find) kawy (Product), które są (By)
 *   duże (Size) i (And) tańsze niż (PriceLessThan) 10 zł”. Nie musisz znać przepisu (SQL). Ale jeśli powiesz
 *   „daj mi kawę o smaku nmae” — barman od razu odpowie, że nie ma takiej rubryki w menu (błąd przy starcie).
 *   A gdy zamówienie robi się na pół strony — lepiej podać kartkę z przepisem (@Query, Jpa04).
 *
 * JAK TO DZIAŁA:
 *   1. Przy tworzeniu beana repozytorium (start aplikacji) Spring Data dla każdej metody bez ciała i bez @Query
 *      tworzy PartTree: przedrostek (find/read/get/query/search/stream, count, exists, delete/remove), opcjonalne
 *      Distinct/TopN/FirstN, potem po „By” warunki połączone And/Or, na końcu OrderBy.
 *   2. Każdy człon warunku to ścieżka do POLA encji (nie kolumny!) + słowo kluczowe (LessThan, Containing…).
 *      Pole nie istnieje → PropertyReferenceException → repozytorium nie powstaje → aplikacja nie startuje.
 *   3. Z PartTree powstaje zapytanie JPA (Criteria), Hibernate tłumaczy je na SQL. Parametry metody wiąże się po
 *      kolejności z członami warunku (pierwszy parametr → pierwszy człon). Zobaczysz SQL w logu org.hibernate.SQL.
 *   4. Typ zwracany wybiera sposób wykonania: lista (getResultList), pojedynczy obiekt/Optional (getSingleResult —
 *      stąd wyjątek przy 2+ wynikach), Stream (kursor), count/exists (osobne, lżejsze zapytanie).
 *
 * SŁÓWKA:
 *   derive = wyprowadzić; containing = zawierający; ignore case = ignoruj wielkość liter; between = pomiędzy;
 *   greater than = większy niż; less than = mniejszy niż; order by = sortuj według; descending/ascending =
 *   malejąco/rosnąco; top/first = pierwsze; is null = jest pusty (NULL); stream = strumień; cursor = kursor
 *   (czytanie wyniku kawałkami); probe = próbka (Query by Example).
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa02Repositories (JpaRepository, save, findById, transakcje repozytorium),
 *             s08_data_jpa/Jpa04CustomQueries (@Query, gdy nazwa robi się za długa),
 *             s08_data_jpa/Jpa05PagingSorting (Pageable, Sort, Limit zamiast Top),
 *             t16_streams/Streams01Intro (strumienie — tu Stream z bazy).
 * </pre>
 */
public final class Jpa03QueryMethods {

    private Jpa03QueryMethods() {
    }

    // =================================================================================================
    // 1. PARSOWANIE NAZW — słowa kluczowe (przeczytaj ProductRepository.java: pod każdą metodą jest jej JPQL)
    // =================================================================================================
    //
    // Budowa nazwy:   find  [Top3|First|Distinct]  By  Warunek [And|Or Warunek…]  [OrderBy Pole Asc|Desc…]
    //                 └ co zrobić                    └ które wiersze                └ kolejność
    //
    //   metoda (dane z migracji V2)                                     SQL (fragment z logu)          wynik testu
    //   findByName("Mysz")                                               where name=?                   Mysz (Optional)
    //   findByCategoryAndPriceLessThan(COMPUTERS, 200)                   category=? and price<?         Klawiatura, Mysz
    //   findByNameContainingIgnoreCase("KUBEK")                          upper(name) like upper(?)      Kubek, Kubek termiczny
    //   findByPriceBetweenOrderByPriceAscNameAsc(59.00, 79.00)           price between ? and ?          oba końce WŁĄCZNIE
    //   findByStockGreaterThanOrderByPriceDesc(50)                       stock>? order by price desc    Kubek, Zeszyt, Długopis
    //   existsByName("Mysz")                                             select id … fetch first 1      true
    //   countByCategory(OFFICE)                                          select count(…)                3
    //   deleteByStock(0)                                                 SELECT, potem DELETE × 2       2 (liczba usuniętych)
    //   findTop3ByOrderByPriceDesc()                                     fetch first 3 rows only        Laptop, Monitor, Krzesło
    //   findFirstByCategoryOrderByPriceAsc(HOME)                         fetch first 1 rows only        Kubek
    //
    // Inne często spotykane słowa: Or, Not, In (kolekcja), IsNull/IsNotNull, StartingWith/EndingWith, Like (sam
    // podajesz %), LessThanEqual/GreaterThanEqual, Before/After (daty), True/False (pola boolean), Distinct.
    //
    // Containing/StartingWith/EndingWith same dodają % i UCIEKAJĄ (escape) znaki specjalne parametru — test:
    // findByNameContainingIgnoreCase("%") nie zwraca wszystkiego, tylko nic (szuka dosłownego znaku %).
    //
    // deleteBy… / removeBy… (sprawdzone testem): Spring Data najpierw WCZYTUJE pasujące encje (SELECT), potem woła
    // em.remove dla każdej (osobny DELETE na encję) i zwraca ich liczbę. Dlaczego tak „wolno”? Żeby zadziałały
    // zdarzenia cyklu życia (@PreRemove) i kaskady relacji (s09). Masowy DELETE jednym poleceniem: @Modifying @Query (Jpa04).
    // Metody zapisujące potrzebują transakcji: deleteByStock ma @Transactional i działa nawet bez transakcji
    // z zewnątrz; removeBySupplier bez adnotacji kończy się InvalidDataAccessApiUsageException (przyczyna:
    // TransactionRequiredException „No EntityManager with actual transaction available”) — test na pełnej aplikacji.
    // Dlaczego? @Transactional z SimpleJpaRepository (Jpa02, sekcja 4) obejmuje TYLKO metody tej klasy, a nie metody
    // zadeklarowane przez Ciebie w interfejsie.
    //
    // PUŁAPKA: lista z find…By bez OrderBy. Dlaczego? SQL bez ORDER BY nie gwarantuje kolejności — dziś H2 zwraca
    //   wiersze „po kolei”, jutro PostgreSQL po aktualizacji zwróci inaczej. Kolejność ma znaczenie → OrderBy
    //   (albo parametr Sort, Jpa05). W testach bez OrderBy porównuj containsExactlyInAnyOrder.
    //
    // DOBRA PRAKTYKA: po dopisaniu metody spójrz w log na wygenerowany SQL. Dlaczego? Nazwa może znaczyć coś
    //   innego, niż myślisz (np. And/Or nie mają nawiasów: {@code findByAOrBAndC} to {@code A or (B and C)}).

    // =================================================================================================
    // 2. TYPY WYNIKU — List, Optional, pojedynczy obiekt, Stream, boolean, long
    // =================================================================================================
    //
    //   typ zwracany        0 wierszy               1 wiersz        2+ wiersze (sprawdzone testem)
    //   List<Product>       pusta lista             [p]             [p1, p2, …]
    //   Optional<Product>   Optional.empty()        Optional[p]     IncorrectResultSizeDataAccessException
    //   Product             null (!)                p               IncorrectResultSizeDataAccessException
    //   Stream<Product>     pusty strumień          …               … (czytane kursorem, w miarę przetwarzania)
    //   boolean (exists)    false                   true            true
    //   long (count)        0                       1               n
    //
    // IncorrectResultSizeDataAccessException (Springa) opakowuje org.hibernate.NonUniqueResultException
    // „Query did not return a unique result: 3 results were returned”. Optional NIE chroni przed tym wyjątkiem —
    // Optional znaczy „zero albo jeden”, nie „pierwszy z wielu”. Chcesz pierwszy z wielu → findFirstBy…OrderBy….
    //
    // Stream<Product> (strumień): Hibernate czyta wiersze kursorem bazy, zamiast ładować całą listę do pamięci.
    // Dwa warunki (test): (1) musi trwać transakcja — inaczej InvalidDataAccessApiUsageException „streaming query
    // method without a surrounding transaction”; (2) strumień trzeba ZAMKNĄĆ (try-with-resources), bo trzyma otwarte
    // połączenie z bazą. Do zwykłych, małych wyników — List.
    //
    // PUŁAPKA: typ pojedynczy (Product) dla zapytania, które może zwrócić kilka wierszy. Dlaczego groźne? Działa,
    //   dopóki w bazie jest jeden pasujący wiersz — wybucha, gdy pojawi się drugi (np. drugi produkt w kategorii).
    //   Pojedynczy wynik/Optional tylko dla warunków unikalnych (nazwa z UNIQUE, id). A przy 0 wierszy typ Product
    //   daje null — łatwo o NullPointerException; wybieraj Optional.
    //
    // DOBRA PRAKTYKA: exists…By zamiast find…By(…).isPresent() albo count…By(…) > 0. Dlaczego? exists pobiera
    //   najwyżej jeden numer (fetch first 1 rows) i nie wczytuje encji do kontekstu (test: zero wczytanych encji).

    // =================================================================================================
    // 3. BŁĄD W NAZWIE — aplikacja NIE startuje (test: pakiet testowy jpa03_pitfalls)
    // =================================================================================================
    //
    // {@code List<Product> findByNmae(String name);} (literówka). Przy starcie (sprawdzone testem):
    //   BeanCreationException: Error creating bean with name 'brokenNameProductRepository' …
    //     ← QueryCreationException: Cannot create query for method [BrokenNameProductRepository.findByNmae(…)]
    //       ← PropertyReferenceException: No property 'nmae' found for type 'Product'; Did you mean 'name'
    //
    // Spring Data podpowiada nawet poprawną nazwę („Did you mean”). To ogromna zaleta metod z nazw w porównaniu
    // z SQL-em w stringu (s07), gdzie literówka w nazwie kolumny wychodzi dopiero przy wykonaniu zapytania.
    // UWAGA: w Spring Data 2026 klasa PropertyReferenceException jest w pakiecie org.springframework.data.core
    // (wcześniej org.springframework.data.mapping) — w starszym kodzie i tutorialach zobaczysz stary import.
    //
    // Dlaczego test w osobnym pakiecie? Repozytorium z literówką w pakiecie lekcji zepsułoby start KAŻDEGO testu
    // lekcji (klasa startowa skanuje wszystkie repozytoria pod sobą). Dlatego BrokenNameApplication ma własny pakiet
    // testowy, a encję Product bierze przez @EntityScan.
    //
    // DOBRA PRAKTYKA: miej choć jeden test, który uruchamia kontekst z repozytorium (@DataJpaTest wystarczy).
    //   Dlaczego? Wtedy błędna nazwa metody zatrzyma build, a nie wdrożenie.

    // =================================================================================================
    // 4. null JAKO PARAMETR — IS NULL, a nie „pomiń warunek”
    // =================================================================================================
    //
    // findBySupplierOrderByIdAsc(null) → {@code where supplier is null} (sprawdzone w logu i testem: Kubek, Lampka,
    // Zeszyt — produkty BEZ dostawcy). Spring Data zamienia „= null” (które w SQL nigdy nie jest prawdą) na IS NULL.
    // findByCategory(null) → {@code category is null} → pusta lista (kolumna NOT NULL).
    //
    // PUŁAPKA: oczekiwanie, że null „wyłączy” filtr (np. formularz wyszukiwania z pustym polem kategorii).
    //   Dlaczego? Dostaniesz produkty BEZ kategorii, czyli zwykle nic, zamiast wszystkich. Opcjonalne filtry:
    //   osobne metody, Query by Example (sekcja 5), Specifications albo @Query z warunkiem
    //   {@code (:category is null or p.category = :category)} (Jpa04).
    //
    // DOBRA PRAKTYKA: gdy naprawdę pytasz o brak wartości, pisz to w nazwie: findBySupplierIsNull() (bez parametru).
    //   Dlaczego? Czytelnik od razu wie, że chodzi o NULL, a nie o zapomnianą wartość.

    // =================================================================================================
    // 5. GRANICE — kiedy nazwa robi się za długa; Query by Example, Specifications
    // =================================================================================================
    //
    // {@code findByCategoryAndStockGreaterThanAndPriceLessThanAndSupplierIsNotNullOrderByPriceAscNameAsc} — działa,
    // ale nikt tego nie przeczyta. Reguła kciuka: więcej niż 2–3 warunki, złączenia (join), agregaty (sum, avg),
    // projekcje → {@code @Query} z JPQL (s08_data_jpa/Jpa04CustomQueries). Nazwa metody ma wtedy znaczyć coś
    // biznesowego (findCheapAvailable), a zapytanie widać w adnotacji.
    //
    // Dwa mechanizmy dla filtrów zmiennych (np. wyszukiwarka z opcjonalnymi polami):
    //   • Query by Example (zapytanie przez przykład; JpaRepository ma findAll(Example)): podajesz „próbkę” encji
    //     (probe), pola różne od null stają się warunkami „=”. Test: próbka z category = HOME.
    //     PUŁAPKA: pole typu prostego (int stock) nigdy nie jest null — 0 też staje się warunkiem (test: zamiast
    //     3 produktów HOME tylko Lampka ze stanem 0). Rozwiązanie: ExampleMatcher.withIgnorePaths("stock").
    //   • Specifications (specyfikacje, JpaSpecificationExecutor): warunki jako obiekty składane w kodzie
    //     (where(a).and(b)) na Criteria API — najbardziej elastyczne, ale najbardziej rozwlekłe. Tu tylko wzmianka.
    //
    // DOBRA PRAKTYKA: zaczynaj od metod z nazw (krótkie, sprawdzane przy starcie), przechodź na @Query, gdy nazwa
    //   przestaje być czytelna. Dlaczego? Każdy mechanizm ma swój „zasięg” — nie ma jednego najlepszego.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • find|read|get|query|search|stream / count / exists / delete|remove + [TopN|First|Distinct] + By + warunki +
     *     [OrderBy…Asc|Desc]. Człony to POLA encji, nie kolumny.
     *   • Słowa: And, Or, LessThan, GreaterThan(Equal), Between (włącznie), Containing/StartingWith (+ escape %),
     *     IgnoreCase, In, IsNull, True/False, Before/After.
     *   • Wynik: List (0..n), Optional (0..1; 2+ → IncorrectResultSizeDataAccessException), Product (0 → null!),
     *     Stream (transakcja + try-with-resources), boolean (exists), long (count, deleteBy).
     *   • deleteBy…: SELECT + osobny DELETE na encję; wymaga transakcji (@Transactional na metodzie albo w serwisie).
     *   • Literówka w nazwie → aplikacja nie startuje (PropertyReferenceException, „Did you mean…”).
     *   • Parametr null → IS NULL (nie „pomiń warunek”); jawnie: findBySupplierIsNull().
     *   • Lista bez OrderBy = kolejność niegwarantowana.
     *   • Więcej niż 2–3 warunki → @Query (Jpa04); filtry opcjonalne → Query by Example / Specifications.
     *
     * PYTANIA KONTROLNE:
     *   1. Z jakich części składa się nazwa {@code findTop3ByCategoryOrderByPriceDesc} i jaki SQL z niej powstanie?
     *   2. Co się stanie? Metoda {@code Optional<Product> findBySupplier(String supplier)} wywołana dla dostawcy,
     *      który ma cztery produkty.
     *   3. Co się stanie? {@code Product findOneByCategory(Category category)} dla kategorii bez produktów.
     *   4. ZNAJDŹ BŁĄD: {@code List<Product> findByPrize(BigDecimal prize);} — encja ma pole price. Kiedy zobaczysz błąd
     *      i jaki?
     *   5. Co się stanie? {@code repository.findBySupplierOrderByIdAsc(null)} — wszystkie produkty czy żaden?
     *   6. ZNAJDŹ BŁĄD: metoda {@code long removeBySupplier(String s);} bez @Transactional wywołana z kontrolera
     *      (bez transakcji) kończy się wyjątkiem. Jakim i jak to naprawić?
     *   7. Dlaczego Stream z repozytorium trzeba zamykać i wywoływać w transakcji?
     *   8. Kiedy porzucić metodę z nazwy na rzecz @Query?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku ExerciseProductRepository.java (metody default z TODO), testy: Jpa03QueryMethodsExercisesTest.
    // Dopisujesz metody zapytań z nazw w interfejsie i wywołujesz je w ciałach metod default.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. find (odczyt) + Top3 (pierwsze trzy) + By + Category (warunek category = ?) + OrderBy Price Desc.
     *      SQL: {@code select … from product where category=? order by price desc fetch first 3 rows only}
     *      (por. findTop3ByOrderByPriceDesc w logu testu sekcji 1).
     *   2. IncorrectResultSizeDataAccessException („Query did not return a unique result: 4 results were returned”).
     *      Optional oznacza zero albo jeden wynik. Sprawdzone testem (sekcja 2, findOptionalBySupplier("Nowak")).
     *   3. Zwróci null — nie wyjątek i nie Optional. Sprawdzone testem (sekcja 2, kategoria GARDEN).
     *   4. Przy STARCIE aplikacji (tworzenie beana repozytorium): BeanCreationException z przyczyną
     *      QueryCreationException ← PropertyReferenceException „No property 'prize' found for type 'Product';
     *      Did you mean 'price'”. Sprawdzone testem analogicznej literówki (Jpa03BrokenNameTest).
     *   5. Żaden „zwykły” — dostaniesz produkty, które NIE MAJĄ dostawcy (where supplier is null): Kubek, Lampka,
     *      Zeszyt. Sprawdzone testem (sekcja 4).
     *   6. InvalidDataAccessApiUsageException z przyczyną TransactionRequiredException („No EntityManager with actual
     *      transaction available … 'remove' call”). Naprawa: @Transactional na metodzie repozytorium albo — lepiej —
     *      wywołanie z metody serwisu z @Transactional. Sprawdzone testem (Jpa03TransactionRequiredTest).
     *   7. Strumień czyta wiersze kursorem przez otwarte połączenie z bazą: bez transakcji Spring nie ma gdzie go
     *      trzymać (InvalidDataAccessApiUsageException — sprawdzone testem), a niezamknięty strumień blokuje
     *      połączenie z puli.
     *   8. Gdy nazwa ma więcej niż 2–3 warunki, potrzebne są złączenia, agregaty, projekcje albo SQL specyficzny dla
     *      bazy — wtedy @Query (Jpa04) z czytelną, biznesową nazwą metody.
     */
    // </editor-fold>
}

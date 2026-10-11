package s08_data_jpa.jpa03_query_methods;

/**
 * <pre>
 * TEMAT: Zapytania z nazw metod — findBy..., countBy..., existsBy..., deleteBy...
 *        (derived query = zapytanie wyprowadzone z nazwy metody; query method = metoda zapytania;
 *         keyword = słowo kluczowe, np. LessThan, Containing, IgnoreCase; JPQL = Jakarta Persistence Query Language,
 *         język zapytań JPA — podobny do SQL, ale operuje na encjach i polach, a nie na tabelach i kolumnach)
 *
 * W SKRÓCIE:
 *   Do repozytorium dopisujesz metodę BEZ ciała, np. {@code List<Product> findByPriceLessThan(BigDecimal max)}.
 *   Spring Data przy starcie rozbiera nazwę na kawałki: find (szukaj) + By (gdzie) + Price (pole price) + LessThan
 *   (mniejsze niż) i buduje z tego zapytanie JPQL: {@code select p from Product p where p.price < ?1}. Pole, którego nie
 *   ma w encji, zatrzymuje start aplikacji — błąd widać od razu, a nie dopiero u klienta.
 *
 * ANALOGIA: formularz zamówienia w hurtowni.
 *   Nie piszesz listu do magazynu (SQL), tylko wypełniasz rubryki gotowego formularza: „szukam / produktów / gdzie
 *   cena / mniejsza niż / ___”. Magazynier zna formularz na pamięć i wie, co zrobić. Jeśli wpiszesz rubrykę, której
 *   na formularzu nie ma („kolor opakowania”), odeśle Ci go od razu, zanim cokolwiek zacznie szukać.
 *
 * JAK TO DZIAŁA:
 *   start aplikacji:
 *     1. Spring Data tworzy repozytorium (Jpa02Repositories) i dla KAŻDEJ metody bez ciała szuka sposobu wykonania:
 *        @Query (Jpa04CustomQueries) → zapytanie nazwane → zapytanie z nazwy metody (PartTree — drzewo części nazwy)
 *     2. PartTree dzieli nazwę: [temat: find | count | exists | delete] By [warunki połączone And/Or] [OrderBy...]
 *     3. każde pole w warunku sprawdzane w metamodelu encji — brak pola → PropertyReferenceException → start STOP
 *   wywołanie metody:
 *     4. parametry metody podstawiane po kolei pod warunki; Hibernate tłumaczy JPQL na SQL bazy (H2)
 *
 * SŁÓWKA:
 *   find = znajdź; count = policz; exists = istnieje; delete = usuń; by = według (tu: „gdzie”); and/or = i/lub;
 *   less than = mniejsze niż; greater than = większe niż; between = pomiędzy; containing = zawierający;
 *   starting with = zaczynający się od; ignore case = ignoruj wielkość liter; top/first = pierwsze (n wyników);
 *   order by = sortuj według; asc/desc = rosnąco/malejąco; property = właściwość (pole encji); derived = wyprowadzony.
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa02Repositories (repozytorium i jego gotowe metody), s08_data_jpa/Jpa04CustomQueries
 *             (@Query, gdy nazwa robi się za długa), t29_jdbc_databases/Jdbc01SqlBasics (WHERE, LIKE, ORDER BY w SQL).
 * </pre>
 */
public final class Jpa03QueryMethods {

    private Jpa03QueryMethods() {
    }

    // =================================================================================================
    // 1. GRAMATYKA NAZWY: temat + By + warunki (+ OrderBy)
    // =================================================================================================
    //
    //   Optional{@code <Product>} findByName(String name);
    //            │      │  └─ pole encji "name" (z wielkiej litery w nazwie metody)
    //            │      └──── By — od tego miejsca warunki (WHERE)
    //            └─────────── find — temat: zwróć encje (to samo: read, get, query, search, stream)
    //   SQL z logu: select p1_0.id,p1_0.active,... from products p1_0 where p1_0.name=?
    //
    // Typ zwracany decyduje, ILE wyników się spodziewasz:
    //   Optional{@code <Product>} / Product   — co najwyżej jeden; DWA wyniki → IncorrectResultSizeDataAccessException
    //   List{@code <Product>}                 — dowolnie wiele (pusta lista, gdy nic)
    //   long / boolean                — dla countBy / existsBy (sekcja 4)
    //
    // PUŁAPKA: findByName z typem Product (bez Optional), gdy produktu nie ma, zwraca null. Dlaczego to źle?
    //   Kompilator nie przypomni Ci o sprawdzeniu — NullPointerException poleci gdzieś dalej. Optional wymusza
    //   świadome obsłużenie braku.
    //
    // DOBRA PRAKTYKA: dla wyszukiwania po polu unikalnym — Optional; po polu nieunikalnym — List. Dlaczego? Typ
    //   zwracany jest dokumentacją: od razu widać, czy wynik może nie istnieć albo być wielokrotny.

    // =================================================================================================
    // 2. SŁOWA KLUCZOWE PORÓWNAŃ — LessThan, Between, Containing, IgnoreCase...
    // =================================================================================================
    //
    //   findByPriceLessThan(max)              → where price {@code <} ?         (LessThanEqual → {@code <=})
    //   findByPriceGreaterThan(min)           → where price {@code >} ?
    //   findByPriceBetween(min, max)          → where price between ? and ?   (obie granice WŁĄCZNIE)
    //   findByNameContaining(fragment)        → where name like '%fragment%'
    //   findByNameStartingWith(prefix)        → where name like 'prefix%'
    //   findByNameContainingIgnoreCase(f)     → where upper(name) like upper(?) escape '\'  (z logu)
    //   findByCategoryIn(kolekcja)            → where category in (...)
    //   findByDescriptionIsNull()             → where description is null
    //
    // Spring Data sam dokleja % i ESCAPUJE znaki specjalne LIKE w parametrze: findByNameContainingIgnoreCase("%")
    // szuka dosłownie znaku %, a nie „wszystkiego” (test sekcji 2). Dzięki temu użytkownik nie „poszerzy” wyszukiwania.
    //
    // PUŁAPKA: findByName(null) NIE rzuca wyjątku — Spring Data zamienia warunek na „where name is null” (sprawdzone
    //   uruchomieniem). Dlaczego to groźne? Pusty parametr z formularza da „dziwne” wyniki zamiast błędu. Sprawdzaj
    //   parametry w serwisie.

    // =================================================================================================
    // 3. ŁĄCZENIE WARUNKÓW: And, Or, True/False
    // =================================================================================================
    //
    //   List{@code <Product>} findByCategoryAndActiveTrue(ProductCategory category);
    //   SQL z logu: ... where p1_0.category=? and p1_0.active=true
    //
    // True/False (i IsNull/IsNotNull) to warunki BEZ parametru — dlatego metoda ma tylko jeden argument (category).
    // Parametry metody idą PO KOLEI pod warunki, które parametru wymagają: findByCategoryAndPriceLessThan(cat, max).
    //
    // PUŁAPKA: zamiana kolejności parametrów. Dlaczego groźne? findByCategoryAndPriceLessThan(BigDecimal max, ...) się
    //   nie skompiluje tylko wtedy, gdy typy są różne. Przy dwóch polach tego samego typu (np. firstName, lastName)
    //   zła kolejność argumentów da po cichu złe wyniki — Spring Data patrzy na POZYCJĘ, nie na nazwę parametru.

    // =================================================================================================
    // 4. countBy i existsBy
    // =================================================================================================
    //
    //   long countByCategory(ProductCategory c);   → select count(p1_0.id) from products p1_0 where p1_0.category=?
    //   boolean existsByName(String name);         → select p1_0.id from products p1_0 where p1_0.name=?
    //                                                fetch first ? rows only
    //
    // existsBy pyta o JEDEN wiersz (fetch first 1 rows only) — nie wczytuje encji i nie liczy wszystkich.
    //
    // DOBRA PRAKTYKA: „czy jest?” → existsBy, a nie findBy(...).isPresent() ani countBy(...) {@code > 0}. Dlaczego? findBy
    //   wczytuje całą encję do kontekstu utrwalania, countBy liczy WSZYSTKIE pasujące wiersze; existsBy kończy na
    //   pierwszym.

    // =================================================================================================
    // 5. Top/First i OrderBy — „n najlepszych”
    // =================================================================================================
    //
    //   List{@code <Product>} findTop3ByOrderByPriceDesc();   → ... order by p1_0.price desc fetch first ? rows only
    //   Optional{@code <Product>} findFirstByOrderByPriceAsc();   (First = First1 = Top1)
    //   List{@code <Product>} findByCategoryOrderByNameAsc(c);    (sortowanie z warunkiem)
    //
    // Uwaga na „By” w findTop3ByOrderBy...: nie ma warunków, ale By musi zostać — oddziela temat od reszty.
    // Limit trafia do SQL (fetch first ? rows only) — baza zwraca tylko 3 wiersze, nie cała tabela.
    //
    // PUŁAPKA: Top3 bez OrderBy. Dlaczego? Bez sortowania baza zwraca wiersze w DOWOLNEJ kolejności — „pierwsze 3” to
    //   losowe 3, mogą się zmienić po restarcie bazy albo dodaniu indeksu. Limit zawsze z OrderBy.

    // =================================================================================================
    // 6. deleteBy — usuwanie z nazwy metody (i transakcja!)
    // =================================================================================================
    //
    //   @Transactional
    //   long deleteByActiveFalse();     → select ... where p1_0.active=false, potem em.remove dla KAŻDEGO wyniku
    //
    // deleteBy NIE robi jednego „delete from products where ...”. Najpierw wczytuje encje, potem usuwa je po jednej
    // (dzięki temu działają callbacki encji i kaskady). Zwraca liczbę usuniętych (albo listę usuniętych encji).
    //
    // PUŁAPKA: deleteBy bez transakcji. Dlaczego nie działa? Metody, które dopisujesz do interfejsu, NIE dostają
    //   transakcji do zapisu tak jak save czy deleteById z SimpleJpaRepository, a em.remove wymaga transakcji. Wywołane
    //   poza transakcją (np. z kontrolera bez @Transactional) kończy się:
    //     InvalidDataAccessApiUsageException: No EntityManager with actual transaction available for current thread -
    //     cannot reliably process 'remove' call
    //   Sprawdzone testem deleteByWithoutTransactionFails (deleteByCategory bez @Transactional). W @DataJpaTest tego
    //   nie zobaczysz, bo test ma własną transakcję — dlatego ten jeden test ją wyłącza (Propagation.NOT_SUPPORTED).
    //
    // DOBRA PRAKTYKA: metody deleteBy (i @Modifying z Jpa04) oznaczaj @Transactional w repozytorium albo wołaj
    //   z serwisu z @Transactional. Dlaczego? Działa niezależnie od tego, skąd metodę wywołasz. Do masowego
    //   usuwania tysięcy wierszy lepsze jest jedno zapytanie @Modifying @Query("delete from ...") — Jpa04CustomQueries.

    // =================================================================================================
    // 7. BŁĄD W NAZWIE = APLIKACJA NIE STARTUJE
    // =================================================================================================
    //
    //   interface BrokenProductRepository extends JpaRepository{@code <Product, Long>} {
    //       List{@code <Product>} findByTitle(String title);       // Product nie ma pola title!
    //   }
    //
    // Przy starcie (sprawdzone testem wrongPropertyNameFailsAtStartup):
    //   BeanCreationException: Error creating bean with name '...BrokenProductRepository' ...
    //     ← QueryCreationException: No property 'title' found for type 'Product'
    //       ← PropertyReferenceException: No property 'title' found for type 'Product'
    // W Spring Data 4 PropertyReferenceException jest w pakiecie org.springframework.data.core (wcześniej:
    // org.springframework.data.mapping) — przyda się, gdy znajdziesz go w starym kodzie.
    //
    // To ZALETA zapytań z nazw: literówkę widać przy starcie (i w każdym teście @DataJpaTest), a nie dopiero, gdy
    // klient kliknie wyszukiwanie. Dlatego repozytorium ćwiczeń (ProductExerciseRepository) ma @NoRepositoryBean —
    // literówka w ćwiczeniu nie może zatrzymać całej lekcji.

    // =================================================================================================
    // 8. KIEDY NAZWA ROBI SIĘ ZA DŁUGA → @Query
    // =================================================================================================
    //
    //   findByCategoryAndActiveTrueAndPriceBetweenAndNameContainingIgnoreCaseOrderByPriceAscNameAsc(...)
    //
    // Działa, ale nikt tego nie przeczyta. Granica jest umowna — zwykle 2–3 warunki. Dalej:
    //   • @Query("select p from Product p where ...") z czytelną nazwą metody (Jpa04CustomQueries),
    //   • albo metoda default w repozytorium, która woła krótszą metodę i nadaje jej dobrą nazwę.
    //
    // DOBRA PRAKTYKA: nazwa metody repozytorium mówi, CO znaczy wynik w języku sklepu (findAvailableCoffees), a nie
    //   JAK jest liczony. Dlaczego? Kod serwisu czyta się jak opis biznesu; szczegóły zapytania są w jednym miejscu.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Nazwa = temat (find/count/exists/delete) + By + warunki (And/Or) + opcjonalnie OrderBy...Asc/Desc.
     *   • Słowa: LessThan, GreaterThan(Equal), Between (włącznie), Containing/StartingWith (LIKE z escapowaniem),
     *     IgnoreCase, In, IsNull, True/False (bez parametru).
     *   • Typ zwracany: Optional (jeden), List (wiele), long (countBy), boolean (existsBy).
     *   • Top3/First + OrderBy — limit w SQL; bez OrderBy kolejność przypadkowa.
     *   • deleteBy wczytuje i usuwa po jednym; wymaga transakcji → @Transactional.
     *   • Nieistniejące pole w nazwie → PropertyReferenceException przy starcie aplikacji.
     *   • Za długa nazwa → @Query albo metoda default z dobrą nazwą.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie zapytanie zbuduje Spring Data dla findByCategoryAndActiveTrue(COFFEE)? Dlaczego metoda ma tylko jeden
     *      parametr?
     *   2. Co się stanie? Do ProductRepository dopiszesz List{@code <Product>} findByTitle(String title), a Product nie ma pola title.
     *   3. ZNAJDŹ BŁĄD: kontroler bez transakcji woła productRepository.deleteByCategory(TEA), metoda nie ma
     *      {@code @Transactional}. Co zobaczysz?
     *   4. Dlaczego existsByName jest lepsze niż findByName(...).isPresent()?
     *   5. Co się stanie? findByNameContainingIgnoreCase("%") — czy zwróci wszystkie produkty?
     *   6. Dlaczego findTop3By... powinno mieć OrderBy?
     *   7. Ile zapytań SQL wyśle deleteByActiveFalse(), gdy są 3 nieaktywne produkty?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku ProductExerciseRepository (metody default exercise1..3), testy: Jpa03QueryMethodsExercisesTest.
    // Dopisujesz metody zapytań do tego interfejsu i wywołujesz je z metod default.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. select ... from products where category = ? and active = true. Jeden parametr, bo „True” to warunek bez
     *      wartości (stała true w zapytaniu). SQL z logu testu findByCategoryAndActiveTrue.
     *   2. Aplikacja nie wystartuje: BeanCreationException z przyczyną PropertyReferenceException „No property 'title'
     *      found for type 'Product'”. Sprawdzone testem wrongPropertyNameFailsAtStartup.
     *   3. InvalidDataAccessApiUsageException „No EntityManager with actual transaction available for current thread -
     *      cannot reliably process 'remove' call” (przyczyna: TransactionRequiredException). Poprawka: @Transactional
     *      na metodzie repozytorium albo na metodzie serwisu. Sprawdzone testem deleteByWithoutTransactionFails.
     *   4. existsBy pyta bazę o jeden identyfikator (fetch first 1 rows only) i nie tworzy encji; findBy wczytuje całą
     *      encję i trzyma ją w kontekście utrwalania.
     *   5. Nie — zwróci pustą listę. Spring Data escapuje % w parametrze (like ... escape '\'), więc szuka dosłownego
     *      znaku %. Sprawdzone testem findByNameContainingIgnoreCase.
     *   6. Bez sortowania kolejność wierszy w bazie jest nieokreślona, więc „pierwsze 3” mogą być dowolne i zmieniać się
     *      między uruchomieniami.
     *   7. Cztery: jeden SELECT (wczytanie 3 encji) + trzy DELETE (po jednym na encję, przy flush). Sprawdzone
     *      uruchomieniem (log SQL).
     */
    // </editor-fold>
}

package s08_data_jpa.jpa03_query_methods;

import java.util.Collection;
import java.util.List;

/**
 * <pre>
 * TEMAT: Zapytania z nazw metod — Spring Data czyta nazwę i sam pisze zapytanie
 *        (derived query = zapytanie wyprowadzone z nazwy metody; query method = metoda zapytania;
 *         property = właściwość, tu: pole encji; keyword = słowo kluczowe, np. LessThan, OrderBy)
 *
 * W SKRÓCIE:
 *   Do repozytorium dopisujesz deklarację {@code List<Product> findByCategory(Category category);} — bez ciała,
 *   bez SQL. Przy starcie aplikacji Spring Data rozbiera nazwę na kawałki: „find” (szukaj), „By” (gdzie),
 *   „Category” (pole encji) i buduje zapytanie {@code where category = ?}. Słowa kluczowe (LessThan, Between,
 *   Containing, IgnoreCase, OrderBy, Top3…) dokładają operatory, sortowanie i limity. Literówka w nazwie pola
 *   zatrzymuje start aplikacji — to zaleta: błąd wychodzi od razu, a nie u klienta.
 *
 * ANALOGIA: zamówienie w barze mlecznym „po kodzie”.
 *   Nie tłumaczysz kucharzowi przepisu (SQL). Mówisz utarte hasło: „pierogi ruskie, duża porcja, bez cebulki”.
 *   Kucharz zna każde słowo i wie, co zrobić. Gdy powiesz „pierogi rukse”, od razu słyszysz „nie ma takiego dania”
 *   (błąd startu), zamiast dostać coś przypadkowego.
 *
 * JAK TO DZIAŁA:
 *   1. Przy starcie Spring Data tworzy proxy repozytorium i dla KAŻDEJ zadeklarowanej metody bez @Query
 *      buduje drzewo części nazwy (klasa PartTree): podmiot (find/exists/count/delete + Distinct/Top/First),
 *      warunki po „By” połączone And/Or, sortowanie po „OrderBy”.
 *   2. Każdy warunek to NAZWA POLA encji + słowo kluczowe (domyślnie równość). Spring Data sprawdza, czy pole
 *      istnieje — jeśli nie, rzuca PropertyReferenceException i kontekst się nie podnosi.
 *   3. Z drzewa powstaje zapytanie JPA (Criteria), a Hibernate zamienia je na SQL dla twojej bazy.
 *   4. Parametry metody wiążą się z warunkami W KOLEJNOŚCI: pierwszy parametr z pierwszym warunkiem itd.
 *   5. Typ wyniku decyduje o wykonaniu: List (wiele), Optional (najwyżej jeden), boolean (exists), long (count),
 *      Stream (strumień czytany z otwartego połączenia).
 *
 * SŁÓWKA:
 *   find = znajdź; by = według/gdzie; and = i; or = lub; less than = mniejszy niż; greater than = większy niż;
 *   between = pomiędzy; containing = zawierający; starting with = zaczynający się od; ignore case = bez
 *   rozróżniania wielkości liter; in = w zbiorze; order by = sortuj według; asc/desc = rosnąco/malejąco;
 *   top/first = pierwsze N; exists = istnieje; count = policz; distinct = bez powtórzeń.
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa02Repositories (metody CRUD z JpaRepository),
 *             s08_data_jpa/Jpa04CustomQueries (@Query, gdy nazwa robi się za długa),
 *             s08_data_jpa/Jpa05PagingSorting (Pageable i Sort jako parametr metody),
 *             t29_jdbc_databases/Jdbc01SqlBasics (WHERE, LIKE, ORDER BY w czystym SQL).
 * </pre>
 */
public final class Jpa03QueryMethods {

    private Jpa03QueryMethods() {
    }

    // =================================================================================================
    // 1. Anatomia nazwy: find + By + Pole — równość
    // =================================================================================================
    //
    //   List<Product> findByCategory(Category category);
    //   └─ find…By     podmiot: zwróć encje      (to samo znaczą read…By, get…By, query…By, search…By, stream…By)
    //      Category    pole encji Product (nazwa POLA w Javie, nie kolumny!) + domyślnie „równa się”
    //
    // Prawdziwy SQL (Hibernate 7.4, H2):
    //   select p1_0.id,p1_0.category,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock from products p1_0 where p1_0.category=?
    //   findBySku("ELE-001") → ... where p1_0.sku=?   (wynik: Optional<Product>)
    // Sprawdzone testem (sekcja 1).
    //
    // PUŁAPKA: null jako argument równości NIE oznacza „pomiń warunek”. findByCategory(null) wysyła
    //   "... where p1_0.category is null" — i zwraca pustą listę (kategorię mają wszystkie produkty).
    //   Dlaczego? Spring Data tłumaczy null wprost na IS NULL. Filtr „opcjonalny” zrób osobną metodą albo w @Query.
    //   Sprawdzone testem (sekcja 1).

    // =================================================================================================
    // 2. Operatory: LessThan, Between, Containing, IgnoreCase, In, And, Or
    // =================================================================================================
    //
    //   findByPriceLessThan(50)                      → where p1_0.price<?                          (< ostro)
    //   findByPriceBetween(79.00, 129.00)            → where p1_0.price between ? and ?            (WŁĄCZNIE z końcami)
    //   findByNameContainingIgnoreCase("KAW")        → where upper(p1_0.name) like upper(?) escape '\'
    //   findByNameStartingWith("K")                  → where p1_0.name like ? escape '\'
    //   findByCategoryIn(Set.of(DOM, ODZIEZ))        → where p1_0.category in (?,?)
    //   findByCategoryAndStockGreaterThan(ELE, 5)    → where p1_0.category=? and p1_0.stock>?
    //   findByStockOrPriceGreaterThan(0, 1500)       → where p1_0.stock=? or p1_0.price>?
    // Inne słowa: LessThanEqual, GreaterThanEqual, Before/After (daty), IsNull/IsNotNull, Not, NotIn, EndingWith,
    // Like (sam podajesz %), True/False (pola boolean).
    // Sprawdzone testem (sekcja 2).
    //
    // Containing/StartingWith same dokładają znaki % i UCIEKAJĄ (escape) znaki specjalne z argumentu:
    //   findByNameContainingIgnoreCase("%") zwraca pustą listę — "%" jest szukany dosłownie, a nie jako „cokolwiek”.
    //   Sprawdzone testem (sekcja 2).
    //
    // PUŁAPKA: w findByStockOrPriceGreaterThan słowo GreaterThan dotyczy TYLKO ceny — stock to zwykła równość.
    //   Każdy warunek ma własne słowo kluczowe; nie ma „wspólnego” operatora dla kilku pól.

    // =================================================================================================
    // 3. Sortowanie i limity: OrderBy, Top, First
    // =================================================================================================
    //
    //   findByCategoryOrderByPriceDesc(ELEKTRONIKA)       → ... where p1_0.category=? order by p1_0.price desc
    //   findTop3ByOrderByPriceDesc()                      → ... order by p1_0.price desc fetch first ? rows only
    //   findFirstByCategoryOrderByPriceAsc(SPOZYWCZE)     → ... order by p1_0.price fetch first ? rows only
    // Top3 i First (= Top1) zamieniają się na limit wierszy po stronie bazy (FETCH FIRST n ROWS ONLY w H2),
    // a nie na obcięcie listy w Javie. Sprawdzone testem (sekcja 3).
    //
    // DOBRA PRAKTYKA: zawsze łącz Top/First z OrderBy.
    //   Dlaczego? Bez sortowania baza zwraca wiersze w DOWOLNEJ kolejności — „trzy pierwsze” mogą być innymi
    //   trzema przy każdym uruchomieniu (więcej w Jpa05PagingSorting).

    // =================================================================================================
    // 4. exists…By i count…By — odpowiedź tak/nie i liczba bez wczytywania encji
    // =================================================================================================
    //
    //   existsBySku("ELE-001")        → select p1_0.id from products p1_0 where p1_0.sku=? fetch first ? rows only
    //   countByCategory(ELEKTRONIKA)  → select count(p1_0.id) from products p1_0 where p1_0.category=?
    // Sprawdzone testem (sekcja 4).
    //
    // DOBRA PRAKTYKA: „czy jest?” → existsBy, „ile?” → countBy, a nie findBy...().isEmpty() / .size().
    //   Dlaczego? findBy wczytuje wszystkie pasujące wiersze i buduje z nich encje tylko po to, żeby je policzyć.
    //   exists kończy po pierwszym trafieniu, count liczy w bazie.

    // =================================================================================================
    // 5. Typ wyniku: List, Optional, Stream — i pułapka „miał być jeden”
    // =================================================================================================
    //
    //   List<Product>      — zero lub więcej; brak wyników = pusta lista (nigdy null)
    //   Optional<Product>  — zero lub JEDEN; brak = Optional.empty()
    //   Product            — zero lub jeden; brak = null (dlatego wolimy Optional)
    //   Stream<Product>    — wiersze czytane na bieżąco z otwartego połączenia; wymaga transakcji i zamknięcia
    //
    // PUŁAPKA: Optional (albo pojedyncza encja) przy zapytaniu, które znajduje KILKA wierszy.
    //   findOneByCategory(DOM) — „One” jest tylko ozdobnikiem, warunek to category = DOM, a kategoria DOM ma
    //   2 produkty. Wynik: org.springframework.dao.IncorrectResultSizeDataAccessException:
    //   Query did not return a unique result: 2 results were returned. Dlaczego dopiero teraz, a nie przy starcie?
    //   Spring Data nie wie, ile wierszy zwróci baza — to wychodzi przy wykonaniu. Sprawdzone testem (sekcja 5).
    //
    // PUŁAPKA: Stream bez transakcji. streamByCategory poza transakcją kończy się
    //   InvalidDataAccessApiUsageException: You're trying to execute a streaming query method without a
    //   surrounding transaction that keeps the connection open so that the Stream can actually be consumed; ...
    //   Dlaczego? Strumień czyta wiersze z połączenia na bieżąco — połączenie musi żyć, aż skończysz czytać.
    //   Zawsze: @Transactional(readOnly = true) wokół kodu i try-with-resources na strumieniu.
    //   Sprawdzone testem (sekcja 5: TestTransaction.end() kończy transakcję testu).

    // =================================================================================================
    // 6. Literówka w nazwie → aplikacja nie wstaje
    // =================================================================================================
    //
    //   List<Product> findByNmae(String name);      // „nmae” zamiast „name”
    //
    // Prawdziwy łańcuch wyjątków przy starcie kontekstu lekcji (Spring Data 4.1, metoda dopisana do ProductRepository):
    //   BeanCreationException: Error creating bean with name 'productRepository' defined in
    //     s08_data_jpa.jpa03_query_methods.ProductRepository defined in @EnableJpaRepositories declared on
    //     DataJpaRepositoriesRegistrar.EnableJpaRepositoriesConfiguration: No property 'nmae' found for type
    //     'Product'; Did you mean 'name'
    //   └ QueryCreationException: Cannot create query for method [ProductRepository.findByNmae(java.lang.String)];
    //     No property 'nmae' found for type 'Product'; Did you mean 'name'
    //     └ org.springframework.data.core.PropertyReferenceException: No property 'nmae' found for type 'Product';
    //       Did you mean 'name'
    // W teście padają WSZYSTKIE metody klasy — kontekst nie wstaje, więc żaden test nie ma na czym działać.
    // Spring Data nawet podpowiada poprawną nazwę („Did you mean” = „czy chodziło ci o”).
    // Uwaga: w Spring Data 4 klasa PropertyReferenceException leży w pakiecie org.springframework.data.core
    // (w starszych wersjach — org.springframework.data.mapping).
    // Sprawdzone testem (sekcja 6: mała aplikacja z repozytorium zawierającym literówkę) i uruchomieniem testów
    // lekcji z metodą findByNmae dopisaną na chwilę do ProductRepository (wszystkie testy klasy: ERROR).
    //
    // DOBRA PRAKTYKA: błąd przy starcie to dobra wiadomość — testy z kontekstem (@DataJpaTest) wyłapią go,
    //   zanim kod trafi na produkcję. Dlatego każde repozytorium powinno mieć choć jeden test.

    // =================================================================================================
    // 7. Kiedy nazwa robi się za długa
    // =================================================================================================
    //
    //   findByCategoryInAndPriceBetweenAndStockGreaterThanAndNameContainingIgnoreCaseOrderByPriceDescNameAsc(...)
    // Działa, ale nikt tego nie przeczyta. Granica zdrowego rozsądku: 2–3 warunki. Dalej — @Query z czytelnym
    // JPQL i krótką nazwą metody (Jpa04CustomQueries) albo sortowanie przez parametr Sort (Jpa05PagingSorting).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • find…By + Pole [+ SłowoKluczowe] [And/Or …] [OrderBy Pole Asc/Desc]; pola = nazwy pól encji w Javie.
     *   • Słowa: LessThan, GreaterThan(Equal), Between (włącznie), Containing/StartingWith/EndingWith (same dodają %
     *     i uciekają znaki), IgnoreCase, In, IsNull, Not, True/False.
     *   • Top3 / First → limit w SQL (fetch first ? rows only); zawsze z OrderBy.
     *   • existsBy → boolean, countBy → long — bez wczytywania encji.
     *   • Wynik: List (pusta, nie null), Optional (max 1 — inaczej IncorrectResultSizeDataAccessException),
     *     Stream (tylko w transakcji, zamykaj!).
     *   • null w warunku równości = IS NULL, nie „pomiń warunek”.
     *   • Literówka w nazwie pola → PropertyReferenceException przy STARCIE ("No property 'x' found for type ...").
     *   • Więcej niż 2–3 warunki → @Query (Jpa04CustomQueries).
     *
     * PYTANIA KONTROLNE:
     *   1. Skąd Spring Data wie, jakie zapytanie wykonać dla metody findByPriceLessThan, skoro nie ma ona ciała?
     *   2. Co się stanie? Dopisujesz do ProductRepository {@code List<Product> findByNmae(String name);}
     *      i uruchamiasz testy lekcji.
     *   3. Co się stanie? W bazie są dwa produkty z kategorii DOM, a ty wywołujesz
     *      {@code Optional<Product> findOneByCategory(Category.DOM)}.
     *   4. ZNAJDŹ BŁĄD: {@code boolean hasBooks = !repository.findByCategory(Category.KSIAZKI).isEmpty();}
     *      — działa, ale co jest nie tak i jak to poprawić?
     *   5. Czym różni się findByPriceLessThan od findByPriceLessThanEqual? Czy Between obejmuje końce przedziału?
     *   6. ZNAJDŹ BŁĄD: {@code List<Product> findTop3ByCategory(Category category);} ma zwracać „trzy najdroższe
     *      produkty kategorii”.
     *   7. Co zwróci findByCategory(null) i dlaczego?
     *   8. Kiedy porzucić zapytanie z nazwy metody na rzecz @Query?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jpa03QueryMethodsExercisesTest (uruchamiasz sam ▶; @Tag("cwiczenie"))
    // =================================================================================================
    //
    // W każdym ćwiczeniu: dopisz do ProductExerciseRepository (ten pakiet) zapytanie z nazwy metody i wywołaj je
    // w metodzie exerciseN. Test wstawia do bazy 14 produktów sklepu (SampleProducts).
    // Uwaga: ProductExerciseRepository należy do kontekstu lekcji — literówka w nowej metodzie zatrzyma start
    // WSZYSTKICH testów tej lekcji (sekcja 6 w praktyce). Czytaj wtedy komunikat „No property … Did you mean …”.

    /**
     * ĆWICZENIE 1 (łatwe): książki (KSIAZKI) tańsze niż 100 zł, posortowane po cenie rosnąco.
     * Oczekiwane SKU: KSI-001 (79.00), KSI-003 (99.00). Podpowiedź: …And…LessThan…OrderBy…Asc.
     */
    static List<Product> exercise1(ProductExerciseRepository repository) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): ile produktów ma w nazwie „kaw” bez względu na wielkość liter? (oczekiwane: 2 —
     * Kawa ziarnista 1kg i Ekspres do kawy). Policz w bazie, nie w Javie. Podpowiedź: count…By…Containing…IgnoreCase.
     */
    static long exercise2(ProductExerciseRepository repository) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): dwa NAJTAŃSZE produkty z elektroniki, które są na stanie (stock większy niż 0).
     * Oczekiwane SKU: ELE-003, ELE-004 (Smartfon X ma stan 0). Podpowiedź: findTop2By…And…GreaterThan…OrderBy….
     */
    static List<Product> exercise3(ProductExerciseRepository repository) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): produkty z podanych kategorii o cenie od 40 do 500 zł WŁĄCZNIE, posortowane po
     * nazwie rosnąco. Dla kategorii SPOZYWCZE i ODZIEZ oczekiwane nazwy: Kawa ziarnista 1kg, Kurtka zimowa,
     * Oliwa z oliwek, T-shirt bawełniany. Podpowiedź: In, Between, OrderBy — kolejność parametrów = kolejność warunków.
     */
    static List<Product> exercise4(ProductExerciseRepository repository, Collection<Category> categories) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Przy starcie Spring Data rozbiera nazwę (PartTree): find…By = zwróć encje, Price = pole encji,
     *      LessThan = operator <. Z tego buduje zapytanie (where price < ?), a parametr metody wiąże z warunkiem.
     *   2. Testy lekcji nie wystartują: kontekst pada przy tworzeniu beana productRepository z
     *      PropertyReferenceException: No property 'nmae' found for type 'Product'; Did you mean 'name'
     *      (opakowanym w QueryCreationException i BeanCreationException) — każdy test klasy kończy się błędem.
     *      Sprawdzone uruchomieniem i testem (sekcja 6).
     *   3. IncorrectResultSizeDataAccessException: Query did not return a unique result: 2 results were returned.
     *      „One” w nazwie niczego nie ogranicza — Optional obiecuje najwyżej jeden wynik, a baza zwróciła dwa.
     *      Sprawdzone testem (sekcja 5).
     *   4. Wczytuje wszystkie książki i buduje z nich encje tylko po to, żeby sprawdzić, czy lista jest pusta.
     *      Poprawka: {@code boolean existsByCategory(Category category)} — baza kończy po pierwszym trafieniu.
     *   5. LessThan to „<”, LessThanEqual to „<=”. Between jest włączne z obu stron (SQL BETWEEN):
     *      findByPriceBetween(79.00, 129.00) zwraca też produkty za 79.00 i 129.00. Sprawdzone testem (sekcja 2).
     *   6. Brak sortowania: Top3 bierze trzy PIERWSZE wiersze w kolejności, jaką akurat wybierze baza — nie
     *      najdroższe. Poprawka: findTop3ByCategoryOrderByPriceDesc.
     *   7. Pustą listę: Spring Data zamienia null na warunek "category is null", a każdy produkt ma kategorię.
     *      Sprawdzone testem (sekcja 1).
     *   8. Gdy nazwa ma więcej niż 2–3 warunki, gdy potrzebujesz złączeń, funkcji, agregacji, projekcji z wyliczeniami
     *      albo zapytania UPDATE/DELETE na wielu wierszach — wtedy @Query (Jpa04CustomQueries).
     */
    // </editor-fold>
}

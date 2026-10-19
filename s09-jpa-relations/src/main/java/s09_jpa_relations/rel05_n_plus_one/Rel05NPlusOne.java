package s09_jpa_relations.rel05_n_plus_one;

import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * <pre>
 * TEMAT: Problem N+1 — jak go zmierzyć i jak go usunąć
 *        (N+1 = jedno zapytanie po listę + N zapytań po dane każdego elementu; join fetch = złącz i pobierz;
 *         entity graph = graf encji; batch fetching = pobieranie paczkami; projection = projekcja)
 *
 * W SKRÓCIE:
 *   Pobierasz 6 zamówień jednym zapytaniem, a potem w pętli czytasz nazwę klienta każdego z nich. Każdy leniwy
 *   klient to osobny SELECT — razem 1 + N zapytań. Na 6 wierszach nikt tego nie zauważy, na 600 — aplikacja
 *   „muli”, a baza dostaje setki identycznych zapytań. Problem N+1 nie rzuca wyjątku: widać go tylko w LICZBIE
 *   zapytań. Dlatego mierzymy ją statystykami Hibernate i usuwamy problem jednym z czterech narzędzi:
 *   join fetch, @EntityGraph, pobieranie paczkami (@BatchSize / default_batch_fetch_size) albo projekcja DTO.
 *
 * ANALOGIA: zakupy z listą.
 *   N+1 to jeżdżenie do sklepu osobno po każdy produkt z listy: raz po listę, potem sześć kursów. join fetch to
 *   jeden kurs z dużym koszykiem; pobieranie paczkami — kilka kursów, za każdym razem po 10 produktów; projekcja
 *   DTO — zamiast całych produktów bierzesz tylko paragon z tym, czego potrzebujesz.
 *
 * JAK TO DZIAŁA:
 *   orders.findAllByOrderByNumber()                    → select ... from orders ... order by number   (1)
 *   for (o : zamówienia) o.getCustomer().getName()     → select ... from customers where id=?         (N)
 *   Liczymy to w testach: hibernate.generate_statistics=true (profil rel05), potem
 *     Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
 *     stats.clear();  ...kod...  stats.getPrepareStatementCount()  ← ile instrukcji SQL wysłano do bazy
 *
 * SŁÓWKA:
 *   N+1 select problem = problem N+1 zapytań; statistics = statystyki; prepare statement count = liczba
 *   przygotowanych instrukcji SQL; join fetch = złącz i pobierz; entity graph = graf encji; batch = paczka;
 *   batch size = rozmiar paczki; projection = projekcja (wybór kolumn); pagination = stronicowanie;
 *   bag = worek (List bez kolejności); derived table = tabela pochodna (podzapytanie w FROM); count query =
 *   zapytanie liczące.
 *
 * ZOBACZ TEŻ: s09_jpa_relations/Rel04LazyLoading (proxy i leniwe kolekcje), s09_jpa_relations/Rel01ManyToOne
 *             (EAGER też powoduje N+1), s08_data_jpa/Jpa04CustomQueries (@Query, projekcje),
 *             s08_data_jpa/Jpa05PagingSorting (Page, Pageable), t29_jdbc_databases/Jdbc06SqlJoins (JOIN w SQL).
 * </pre>
 */
public final class Rel05NPlusOne {

    private Rel05NPlusOne() {
    }

    // =================================================================================================
    // 1. N+1 na relacji @ManyToOne — zmierzone
    // =================================================================================================
    //
    // Dane (V2__sample_data.sql): 6 zamówień od 5 różnych klientów (Jan Kowalski ma ZAM-001 i ZAM-005).
    //     orders.findAllByOrderByNumber().forEach(o -> o.getCustomer().getName());
    // SQL z logu:
    //     select so1_0.id,so1_0.customer_id,so1_0.number,so1_0.status from orders so1_0 order by so1_0.number
    //     select c1_0.id,c1_0.city,c1_0.name from customers c1_0 where c1_0.id=?      ← 5 razy, inne id
    // getPrepareStatementCount() = 6, czyli 1 + 5, a nie 1 + 6. Dlaczego? Oba zamówienia Jana wskazują TO SAMO
    // proxy (kontekst utrwalania trzyma jeden obiekt na jedno id) — po pierwszym wczytaniu drugie jest już gotowe.
    // „N” to liczba RÓŻNYCH niewczytanych obiektów, nie wierszy.
    //
    // PYTANIE REKRUTACYJNE: „Co to jest problem N+1 i jak go rozwiązać?” — jedno zapytanie po listę encji i po
    //   jednym dodatkowym na leniwą relację każdego elementu. Rozwiązania: sekcje 3–6.
    //
    // DOBRA PRAKTYKA: na ważnych ścieżkach (lista zamówień, raport) miej test, który LICZY zapytania
    //   (jak testy tej lekcji). Dlaczego? N+1 nie daje wyjątku ani złego wyniku — psuje tylko wydajność, więc
    //   bez pomiaru wróci przy pierwszej zmianie, np. gdy ktoś doda do widoku jedno leniwe pole.

    // =================================================================================================
    // 2. N+1 na kolekcji @OneToMany
    // =================================================================================================
    //
    //     orders.findAllByOrderByNumber().forEach(o -> o.getLines().size());
    //     select ... from orders so1_0 order by so1_0.number                                          (1)
    //     select l1_0.order_id,l1_0.id,l1_0.product_name,l1_0.quantity from order_lines l1_0
    //     where l1_0.order_id=?                                                     ← 6 razy           (N)
    // = 7 zapytań. Tu N = liczba zamówień, bo każde ma własną kolekcję.
    // Uwaga: EAGER nie leczy N+1 — tylko go ukrywa. Zapytanie JPQL z EAGER-relacją robi te same dodatkowe
    // SELECT-y, tylko zawsze (Rel01ManyToOne: 3 produkty = 4 zapytania).

    // =================================================================================================
    // 3. Rozwiązanie 1: join fetch w zapytaniu JPQL
    // =================================================================================================
    //
    // @Query("select o from ShopOrder o join fetch o.customer order by o.number")  → 1 zapytanie:
    //     select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.city,c1_0.name,so1_0.number,so1_0.status
    //     from orders so1_0 join customers c1_0 on c1_0.id=so1_0.customer_id order by so1_0.number
    //
    // Kolekcja też: @Query("select o from ShopOrder o join fetch o.lines order by o.number") → 1 zapytanie,
    // ale baza zwraca 10 WIERSZY (po jednym na pozycję) — zamówienie z 3 pozycjami występuje w wyniku SQL 3 razy.
    // Hibernate 7 sam scala powtórzone wiersze w JEDEN obiekt zamówienia: lista ma 6 elementów (test).
    // Duplikaty zobaczysz, gdy wybierasz pojedyncze kolumny: select o.number from ShopOrder o join o.lines l
    // → 10 numerów (ZAM-001 dwa razy, ZAM-003 trzy razy...).
    //
    // PUŁAPKA: starsze tutoriale (pisane dla Hibernate 5) każą pisać „select DISTINCT o ... join fetch o.lines”,
    //   bo wtedy lista encji zawierała duplikaty. Dlaczego to ważne? W Hibernate 7.4 lista encji jest bez
    //   duplikatów także BEZ DISTINCT (sprawdzone testem), a zapamiętana „magiczna” reguła myli przy zapytaniach
    //   o kolumny, gdzie powtórzenia są prawdziwe (test duplicatesVisibleInColumnQuery).

    // =================================================================================================
    // 4. Rozwiązanie 2: @EntityGraph na metodzie repozytorium
    // =================================================================================================
    //
    // @EntityGraph(attributePaths = "customer")
    // List<ShopOrder> findAllWithCustomerGraphByOrderByNumber();
    // Spring Data przekazuje Hibernate „graf ładowania”: pobierz też customer. Efekt jak join fetch — 1 zapytanie,
    // ten sam SQL (JOIN, bo relacja jest wymagana: optional = false; dla kolekcji graf daje LEFT JOIN —
    // Rel04LazyLoading). Zaleta: nie piszesz JPQL, działa z metodami pochodnymi (findBy...) i z findAll.
    // Wada: graf tylko DOCIĄGA dane — warunków (where) i sortowania nie zmienisz.

    // =================================================================================================
    // 5. Rozwiązanie 3: pobieranie paczkami — @BatchSize i default_batch_fetch_size
    // =================================================================================================
    //
    // Zamiast 1 + N zapytań — 1 + (liczba paczek). Gdy program sięga po PIERWSZE leniwe pole, Hibernate dociąga
    // od razu do 10 czekających obiektów tego samego rodzaju (SQL z logu):
    //     select c1_0.id,c1_0.city,c1_0.name from customers c1_0 where c1_0.id in (?,?,?,?,?,?,?,?,?,?)
    // Dwa sposoby:
    //   • @BatchSize(size = 10) — adnotacja Hibernate na jednej relacji (tu: Customer.orders);
    //     customers.findAllByOrderByName() + c.getOrders().size() dla 7 klientów = 2 zapytania;
    //   • spring.jpa.properties.hibernate.default_batch_fetch_size=10 — dla WSZYSTKICH leniwych relacji;
    //     test z tym ustawieniem: zamówienia + klienci = 2 zapytania, zamówienia + pozycje = 2 zapytania.
    // Kod pętli się NIE zmienia — to zaleta (nie trzeba pisać zapytań) i wada (problem nie znika, tylko maleje).
    //
    // DOBRA PRAKTYKA: ustaw default_batch_fetch_size (np. 10–50) jako siatkę bezpieczeństwa dla całej aplikacji,
    //   a na ważnych ścieżkach i tak pisz join fetch / projekcje. Dlaczego? Paczki zamieniają N+1 w kilka zapytań
    //   tam, gdzie nikt nie pomyślał o optymalizacji — kosztem jednego wiersza konfiguracji.

    // =================================================================================================
    // 6. Rozwiązanie 4: projekcja DTO — gdy dane są tylko do odczytu
    // =================================================================================================
    //
    // @Query("""
    //         select new s09_jpa_relations.rel05_n_plus_one.OrderSummary(o.number, c.name, c.city)
    //         from ShopOrder o join o.customer c order by o.number""")
    // List<OrderSummary> findSummaries();
    // SQL z logu: select so1_0.number,c1_0.name,c1_0.city from orders so1_0 join customers c1_0 on ... order by ...
    // Jedno zapytanie, tylko 3 kolumny, ŻADNEJ encji w kontekście utrwalania (getEntityLoadCount() = 0) — nie ma
    // proxy, nie ma leniwych pól, nie ma czego dociągać, a Hibernate nie śledzi zmian (dirty checking).
    //
    // DOBRA PRAKTYKA: listy i raporty „do wyświetlenia” czytaj projekcją DTO. Dlaczego? To najtańszy odczyt
    //   (mniej kolumn, zero encji) i całkowicie odporny na N+1 i LazyInitializationException. Encje wczytuj, gdy
    //   masz je ZMIENIAĆ.

    // =================================================================================================
    // 7. Pułapki: stronicowanie z join fetch kolekcji, Page w Spring Data, MultipleBagFetchException
    // =================================================================================================
    //
    // a) join fetch KOLEKCJI + setFirstResult/setMaxResults (stronicowanie). Stronę trzeba liczyć w ZAMÓWIENIACH,
    //    a SQL zwraca wiersze POZYCJI — „limit 2” na złączeniu uciąłby zamówienie w połowie. Hibernate 7.4
    //    (sprawdzone na H2) przenosi stronicowanie do podzapytania (tabeli pochodnej) — SQL z logu:
    //        select ... from (select so1_0.id,so1_0.customer_id,so1_0.number,so1_0.status from orders so1_0
    //        where exists(select 1 from order_lines l1_0 where so1_0.id=l1_0.order_id) order by so1_0.number
    //        offset ? rows fetch first ? rows only) so1_0(id,customer_id,number,status)
    //        join order_lines l1_0 on so1_0.id=l1_0.order_id order by so1_0.number
    //    Jedno zapytanie, wczytane tylko 2 zamówienia + ich pozycje (6 encji).
    //    PUŁAPKA: w starszych wersjach Hibernate (i to opisuje większość tutoriali) — a w 7.4 tylko wtedy, gdy
    //    podzapytania nie da się użyć — stronicowanie odbywa się W PAMIĘCI: Hibernate czyta WSZYSTKIE wiersze
    //    i dopiero w Javie wycina stronę, a w logu pisze (poziom WARN, treść z Hibernate 7.4):
    //        HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory
    //    Test wymusza ten tryb wskazówką org.hibernate.limitInMemory: wynik ten sam, ale wczytanych encji 16
    //    zamiast 6 — przy milionie zamówień to milion obiektów w pamięci. Ustawienie
    //    hibernate.query.fail_on_pagination_over_collection_fetch=true zamienia to ostrzeżenie w wyjątek.
    //    Dlaczego to ważne? Kod „działa” w testach na 6 wierszach i zabija serwer na produkcji. Bezpieczny wzorzec
    //    niezależny od wersji: najpierw strona samych id, potem join fetch dla tych id (ćwiczenie 3).
    //
    // b) PUŁAPKA: Page + join fetch kolekcji w Spring Data. Dla
    //        @Query("select o from ShopOrder o join fetch o.lines") Page<ShopOrder> ...(Pageable)
    //    Spring Data sam wylicza zapytanie liczące (count query) — z logu:
    //        select count(*) from orders so1_0 join order_lines l1_0 on so1_0.id=l1_0.order_id
    //    i liczy WIERSZE złączenia: getTotalElements() = 10 zamiast 6 zamówień, getTotalPages() = 5 zamiast 3.
    //    Dlaczego to ważne? Interfejs pokaże puste strony 4 i 5. Naprawa: podaj własne zapytanie liczące:
    //        @Query(value = "select o from ShopOrder o left join fetch o.lines",
    //               countQuery = "select count(o) from ShopOrder o")
    //
    // c) MultipleBagFetchException — dwie kolekcje typu List (worki) w jednym zapytaniu z join fetch:
    //        select c from Customer c join fetch c.orders o join fetch o.lines
    //    → java.lang.IllegalArgumentException: org.hibernate.loader.MultipleBagFetchException: cannot
    //      simultaneously fetch multiple bags: [s09_jpa_relations.rel05_n_plus_one.ShopOrder.lines,
    //      s09_jpa_relations.rel05_n_plus_one.Customer.orders]
    //    Dlaczego Hibernate odmawia? Złączenie dwóch kolekcji daje iloczyn kartezjański (każde zamówienie × każda
    //    pozycja), a w worku (List bez klucza) nie da się odróżnić prawdziwego duplikatu od powtórzenia z JOIN-a.
    //    Wyjścia: dwa zapytania z join fetch w tym samym kontekście utrwalania (ćwiczenie 4) albo pobieranie
    //    paczkami.
    //    PUŁAPKA: „zamień jedną List na Set” (rada z wielu forów) usuwa wyjątek, ale nie iloczyn kartezjański.
    //    Sprawdzone uruchomieniem: gdy pozycje były w Set, a zamówienia klienta w List, lista zamówień Jana
    //    Kowalskiego miała 4 elementy zamiast 2 (każde zamówienie powtórzone tyle razy, ile ma pozycji).
    //    Dlaczego to ważne? Błąd znika z logu, a w danych pojawiają się powtórzenia — dwa zapytania są bezpieczniejsze.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • N+1 = 1 zapytanie po listę + N po leniwe pola elementów; N = liczba RÓŻNYCH niewczytanych obiektów.
     *   • Mierz: hibernate.generate_statistics=true + Statistics.getPrepareStatementCount() (po stats.clear()).
     *   • join fetch — 1 zapytanie; dla kolekcji SQL powtarza wiersze rodzica, ale lista encji jest bez duplikatów.
     *   • @EntityGraph(attributePaths = "pole") — jak join fetch, bez JPQL.
     *   • @BatchSize(size = 10) / default_batch_fetch_size=10 — paczki: where id in (?,?,...), kod bez zmian.
     *   • Projekcja DTO (select new ...) — 1 zapytanie, tylko potrzebne kolumny, 0 encji. Najlepsza do odczytu.
     *   • Stronicowanie + join fetch kolekcji: sprawdź SQL (Hibernate 7.4: podzapytanie; stary tryb: w pamięci +
     *     HHH90003004). Page w Spring Data: podaj countQuery.
     *   • Dwie kolekcje List w jednym join fetch → MultipleBagFetchException → dwa zapytania.
     *
     * PYTANIA KONTROLNE:
     *   1. Co to jest problem N+1 i dlaczego nie widać go w wynikach testów, tylko w logu i statystykach?
     *   2. Co się stanie? 6 zamówień od 5 klientów (jeden ma dwa zamówienia). Pętla czyta getCustomer().getName()
     *      każdego zamówienia — ile zapytań wyśle Hibernate?
     *   3. Czym różni się join fetch od @EntityGraph, a czym od pobierania paczkami?
     *   4. Co się stanie? select o from ShopOrder o join fetch o.lines — ile WIERSZY zwróci baza, a ile elementów
     *      będzie na liście (6 zamówień, 10 pozycji)?
     *   5. ZNAJDŹ BŁĄD: {@code @Query("select o from ShopOrder o join fetch o.lines") Page<ShopOrder>
     *      findPage(Pageable p);} — interfejs pokazuje 5 stron, choć zamówień jest 6, a strona ma 2 elementy.
     *   6. Co się stanie? select c from Customer c join fetch c.orders o join fetch o.lines (obie kolekcje to List).
     *   7. Kiedy projekcja DTO jest lepsza od wczytywania encji z join fetch?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Rel05NPlusOneExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć zamówienia o podanym statusie RAZEM z klientami, posortowane po numerze — tak,
     * żeby pętla po getCustomer().getName() nie wysłała żadnego dodatkowego zapytania (test: razem 1).
     * Podpowiedź: {@code select o from ShopOrder o join fetch o.customer where o.status = :status order by o.number}.
     */
    static List<ShopOrder> exercise1(EntityManager em, String status) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): dla każdego zamówienia zwróć numer i liczbę pozycji (OrderLineCount), posortowane po
     * numerze — jednym zapytaniem i bez wczytywania encji. Podpowiedź: {@code select new
     * s09_jpa_relations.rel05_n_plus_one.OrderLineCount(o.number, count(l)) from ShopOrder o left join o.lines l
     * group by o.number order by o.number}.
     */
    static List<OrderLineCount> exercise2(EntityManager em) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć stronę zamówień (page liczone od 0, size elementów, sortowanie po numerze)
     * z ZAINICJOWANYMI pozycjami — w dwóch zapytaniach i bez stronicowania złączenia: (1) strona samych id:
     * {@code select o.id from ShopOrder o order by o.number} + setFirstResult/setMaxResults; (2) {@code select o
     * from ShopOrder o left join fetch o.lines where o.id in :ids order by o.number}.
     */
    static List<ShopOrder> exercise3(EntityManager em, int page, int size) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć klientów, którzy mają zamówienia (posortowanych po polu name), z
     * zainicjowanymi zamówieniami I pozycjami tych zamówień — w DWÓCH zapytaniach, bez MultipleBagFetchException.
     * Podpowiedź: (1) {@code select c from Customer c join fetch c.orders order by c.name}; (2) {@code select o
     * from ShopOrder o left join fetch o.lines where o.customer in :customers} — drugie zapytanie uzupełni pozycje
     * zamówień, które już są w kontekście utrwalania (to te same obiekty).
     */
    static List<Customer> exercise4(EntityManager em) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Jedno zapytanie po listę + po jednym na leniwe pole każdego elementu. Wynik jest poprawny i nie ma wyjątku
     *      — psuje się tylko wydajność (liczba zapytań rośnie z liczbą wierszy), więc widać to dopiero w logu SQL
     *      albo w statystykach (getPrepareStatementCount()).
     *   2. 6 zapytań: 1 po zamówienia + 5 po klientów. Drugie zamówienie tego samego klienta wskazuje to samo,
     *      już wczytane proxy. Sprawdzone testem nPlusOneOnManyToOne.
     *   3. join fetch (JPQL) i @EntityGraph (adnotacja z listą pól) dociągają dane w TYM SAMYM zapytaniu.
     *      Pobieranie paczkami zostawia leniwe ładowanie, ale dociąga do N czekających obiektów naraz (where id in
     *      (...)) — kod się nie zmienia, zapytań jest kilka zamiast N.
     *   4. Baza zwróci 10 wierszy (po jednym na pozycję), a lista będzie miała 6 zamówień — Hibernate 7 scala
     *      powtórzone wiersze w jeden obiekt. Sprawdzone testem joinFetchCollection.
     *   5. Spring Data wylicza zapytanie liczące z tego samego złączenia: select count(*) from orders join
     *      order_lines — liczy 10 wierszy pozycji zamiast 6 zamówień, więc stron jest 5 zamiast 3. Naprawa:
     *      countQuery = "select count(o) from ShopOrder o". Sprawdzone testem pageWithJoinFetchCountsJoinedRows.
     *   6. IllegalArgumentException z przyczyną MultipleBagFetchException „cannot simultaneously fetch multiple bags:
     *      [...ShopOrder.lines, ...Customer.orders]”. Sprawdzone testem multipleBagFetch.
     *   7. Gdy dane są tylko do odczytu (lista, raport, odpowiedź API): projekcja czyta tylko potrzebne kolumny, nie
     *      tworzy encji (0 w getEntityLoadCount()), nie ma proxy ani leniwych pól. Encje z join fetch — gdy będziesz
     *      je zmieniać.
     */
    // </editor-fold>
}

package s09_jpa_relations.rel05_n_plus_one;

/**
 * <pre>
 * TEMAT: Problem N+1 zapytań — skąd się bierze i jak go usunąć: {@code JOIN FETCH}, {@code @EntityGraph},
 *        batch fetching ({@code @BatchSize}) i projekcje DTO
 *        (N+1 = „jedno zapytanie o listę + N zapytań o powiązania”; fetch = pobierz; batch = paczka;
 *         entity graph = graf encji; projection = projekcja)
 *
 * W SKRÓCIE:
 *   Wczytujesz listę N zamówień jednym zapytaniem, a potem w pętli sięgasz po klienta albo pozycje KAŻDEGO z nich.
 *   Leniwe relacje grzecznie się dociągają — każda osobnym SELECT-em. Wynik: 1 + N zapytań, które w testach na
 *   10 wierszach są niewidoczne, a na produkcji przy 1000 wierszy zabijają wydajność. Lekarstwa: powiedz Hibernate
 *   Z GÓRY, czego potrzebujesz ({@code join fetch}, {@code @EntityGraph}), dociągaj paczkami ({@code @BatchSize},
 *   {@code default_batch_fetch_size}) albo nie wczytuj encji wcale (projekcja {@code select new} do rekordu).
 *
 * ANALOGIA: zakupy z listą.
 *   N+1 to chodzenie do sklepu po każdy produkt z listy osobno: 10 produktów = 10 wyjść z domu (plus jedno po listę).
 *   join fetch — jedno wyjście z dużym wózkiem. Batch fetching — wychodzisz po 5 produktów naraz (2 wyjścia zamiast
 *   10). Projekcja DTO — dzwonisz do sklepu i pytasz tylko o cenę całego koszyka, niczego nie przynosząc do domu.
 *
 * JAK TO DZIAŁA:
 *   1. {@code select o from ShopOrder o} wczytuje tylko tabelę orders. Klient to proxy, lista pozycji — niewczytany
 *      PersistentBag (s09_jpa_relations/Rel04LazyLoading, sekcja 1).
 *   2. W pętli {@code order.getLines().size()} każda lista osobno prosi sesję o swój SELECT ({@code where order_id=?}).
 *      Hibernate nie wie, że za chwilę zapytasz o następne — dlatego robi to „jedna po drugiej”.
 *   3. {@code join fetch} / {@code @EntityGraph} zmieniają SAMO zapytanie: dokładają JOIN i od razu wypełniają relacje
 *      z tych samych wierszy wyniku. Batch fetching zostawia zapytanie główne, ale gdy trzeba wczytać JEDNĄ relację,
 *      Hibernate wczytuje od razu kilka czekających w kontekście ({@code where order_id in (?,?,?,?,?)}).
 *   4. Projekcja {@code select new} wcale nie tworzy encji: baza liczy, Hibernate tylko wkłada kolumny do rekordu.
 *
 * SŁÓWKA:
 *   N+1 select problem = problem N+1 zapytań; join fetch = złącz i pobierz; left join = złączenie lewostronne
 *   (zostawia rodzica bez dzieci); cartesian product = iloczyn kartezjański (każdy z każdym); pagination =
 *   stronicowanie; in memory = w pamięci; bag = worek (List bez kolejności w bazie); batch size = rozmiar paczki;
 *   named entity graph = nazwany graf encji; projection = projekcja; DTO (data transfer object) = obiekt do
 *   przenoszenia danych.
 *
 * ZOBACZ TEŻ: s09_jpa_relations/Rel01ManyToOne (N+1 już przy EAGER @ManyToOne),
 *             s09_jpa_relations/Rel04LazyLoading (proxy, LazyInitializationException, join fetch i @EntityGraph),
 *             s09_jpa_relations/Rel06EntityToDto (DTO na zewnątrz aplikacji),
 *             s07_data_jdbc/Jdbc02JdbcTemplate (N+1 w czystym JDBC i ResultSetExtractor z jednego JOIN-a),
 *             s08_data_jpa/Jpa04CustomQueries (@Query, select new), s08_data_jpa/Jpa05PagingSorting (Pageable).
 * </pre>
 */
public final class Rel05NPlusOne {

    private Rel05NPlusOne() {
    }

    // =================================================================================================
    // 1. PROBLEM: lista zamówień + klient i pozycje każdego = 1 + N zapytań
    // =================================================================================================
    //
    //   PRZED (wygląda niewinnie):
    //     {@code List<ShopOrder> all = orders.findAllOrdered();}            // 10 zamówień
    //     {@code all.forEach(o -> o.getCustomer().getName());}             // klienci
    //     {@code all.forEach(o -> o.getLines().size());}                   // pozycje
    //
    //   Co poszło do bazy (sprawdzone testem, SqlCapture) — 15 zapytań:
    //     {@code select so1_0.id,so1_0.customer_id,so1_0.order_number from orders so1_0 order by so1_0.order_number}   ← 1
    //     {@code select c1_0.id,c1_0.email,c1_0.name from customer c1_0 where c1_0.id=?}                              ← ×4
    //     {@code select l1_0.order_id,l1_0.id,… from order_line l1_0 where l1_0.order_id=? order by l1_0.id}         ← ×10
    //
    // Dlaczego klientów 4, a pozycji 10? Klient wczytany raz siedzi w kontekście utrwalania — kolejne zamówienia
    // Anny już go nie czytają (s08_data_jpa/Jpa01Entities, sekcja 3). Lista pozycji należy do JEDNEGO zamówienia,
    // więc każda wymaga własnego zapytania. Ogólnie: 1 + (liczba różnych rodziców) dla „do jednego”,
    // 1 + (liczba wierszy listy) dla „do wielu”.
    //
    // PUŁAPKA: N+1 nie rzuca żadnego wyjątku i w testach na kilku wierszach „działa szybko”. Dlaczego groźne?
    //   Koszt rośnie liniowo z danymi: 1000 zamówień = 1001 zapytań, każde to podróż do bazy przez sieć (np. 1 ms) —
    //   sekunda czekania na jedną stronę. Widać go tylko w logu SQL albo w licznikach (statystyki Hibernate,
    //   StatementInspector — jak w testach tego działu).
    //
    // DOBRA PRAKTYKA: w testach repozytoriów i serwisów LICZ zapytania (asercja na liczbie SQL). Dlaczego? Ktoś za rok
    //   doda {@code order.getCustomer().getName()} do mapowania i test od razu powie: było 1, jest 1 + N.
    //   PYTANIE REKRUTACYJNE: „Co to jest problem N+1 i jak go wykryć oraz rozwiązać w JPA/Hibernate?”

    // =================================================================================================
    // 2. JOIN FETCH — wszystko jednym zapytaniem; PUŁAPKI: inner join, stronicowanie, duplikaty
    // =================================================================================================
    //
    //   {@code select o from ShopOrder o join fetch o.customer order by o.orderNumber}  (findAllWithCustomer) — 1 zapytanie:
    //     {@code select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,so1_0.order_number from orders so1_0}
    //     {@code join customer c1_0 on c1_0.id=so1_0.customer_id order by so1_0.order_number}
    //
    //   {@code … join fetch o.customer left join fetch o.lines l left join fetch l.product …}  (findAllWithCustomerAndLines):
    //     1 zapytanie z trzema JOIN-ami; baza zwraca 20 WIERSZY (19 pozycji + 1 wiersz pustego ZAM-010 z NULL-ami),
    //     a lista w Javie ma 10 ZAMÓWIEŃ (sprawdzone testem).
    //
    // Duplikaty: wiersz SQL na każdą pozycję oznacza, że ZAM-003 występuje w wyniku 3 razy. Hibernate 5 zwracał wtedy
    // listę z powtórzeniami (stąd w starych tutorialach {@code select distinct o …}). Hibernate 6+ (także 7) SAM
    // usuwa powtórzenia encji-korzenia przy fetch kolekcji — distinct nie jest już potrzebny (sprawdzone testem).
    //
    // PUŁAPKA: {@code join fetch} to złączenie WEWNĘTRZNE. Dlaczego groźne? {@code select o from ShopOrder o join fetch o.lines}
    //   zwraca 9 zamówień — puste ZAM-010 znika z listy (sprawdzone testem), choć nikt nie pisał żadnego warunku.
    //   Dla kolekcji prawie zawsze chcesz {@code left join fetch}.
    //
    // Stronicowanie (pagination) z {@code join fetch} kolekcji — klasyczna PUŁAPKA. LIMIT/OFFSET dotyczy WIERSZY, a
    // jeden rodzic zajmuje wiele wierszy; „3 pierwsze wiersze” to nie „3 pierwsze zamówienia”. Co robi Hibernate 7.4
    // (sprawdzone testem na H2 — zależy od bazy i od zapytania):
    //   • sortowanie tylko po polach RODZICA ({@code order by o.orderNumber}) + setFirstResult(3)/setMaxResults(3) —
    //     Hibernate 7 stronicuje w PODZAPYTANIU SQL i dopiero do niego dołącza pozycje:
    //       {@code select … from (select so1_0.id,so1_0.customer_id,so1_0.order_number from orders so1_0}
    //       {@code   order by so1_0.order_number offset ? rows fetch first ? rows only) so1_0(id,customer_id,order_number)}
    //       {@code left join order_line l1_0 on so1_0.id=l1_0.order_id order by so1_0.order_number,l1_0.id}
    //     → ZAM-004, ZAM-005, ZAM-006, bez ostrzeżenia;
    //   • sortowanie po polu KOLEKCJI ({@code order by l.id}) — podzapytanie nie wchodzi w grę: SQL bez offset/fetch
    //     first (baza oddaje WSZYSTKIE wiersze), stronicowanie w PAMIĘCI i w logu ostrzeżenie
    //     {@code HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory}.
    //     Przy milionie wierszy to milion wierszy w pamięci, żeby pokazać 3 zamówienia.
    //   W internecie (Hibernate 5/6) przeczytasz, że stronicowanie z fetch kolekcji ZAWSZE idzie w pamięci —
    //   Hibernate 7 robi to tylko wtedy, gdy nie umie inaczej.
    //
    // DOBRA PRAKTYKA: ustaw {@code spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true}.
    //   Dlaczego? Ciche stronicowanie w pamięci zamienia się w wyjątek (sprawdzone testem):
    //   {@code HibernateException: setFirstResult() or setMaxResults() specified with collection fetch join (in-memory}
    //   {@code pagination was about to be applied, but 'hibernate.query.fail_on_pagination_over_collection_fetch' is enabled)},
    //   a stronicowanie w podzapytaniu SQL nadal działa.
    //
    // DOBRA PRAKTYKA: stronicowanie rodziców z dziećmi w DWÓCH krokach (findIdsPage + findWithLinesByIdIn):
    //     {@code select so1_0.id from orders so1_0 order by so1_0.order_number offset ? rows fetch first ? rows only}
    //     {@code select … from orders so1_0 left join order_line l1_0 on … where so1_0.id in (?,?,?) order by …}
    //   Dlaczego? Strona id liczy BAZA (indeks, offset), a join fetch dotyczy już tylko kilku rodziców. Działa tak samo
    //   w każdej wersji Hibernate i w każdej bazie. (Spring Data: {@code Page} z {@code join fetch} wymaga osobnego
    //   {@code countQuery} — s08_data_jpa/Jpa05PagingSorting.)

    // =================================================================================================
    // 3. @EntityGraph na metodzie repozytorium; @NamedEntityGraph — krótko
    // =================================================================================================
    //
    //   {@code @EntityGraph(attributePaths = {"customer", "lines"})}
    //   {@code List<ShopOrder> findByCustomerIdOrderByOrderNumber(Long customerId);}
    //   SQL (sprawdzony testem) — zapytanie z nazwy metody + JOIN-y z grafu:
    //     {@code select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,l1_0.order_id,l1_0.id,…,so1_0.order_number}
    //     {@code from orders so1_0 join customer c1_0 on c1_0.id=so1_0.customer_id}
    //     {@code left join order_line l1_0 on so1_0.id=l1_0.order_id where so1_0.customer_id=? order by so1_0.order_number,l1_0.id}
    //   Klient — zwykły JOIN (relacja optional = false), pozycje — LEFT JOIN (puste ZAM-010 zostaje: 4 zamówienia Anny).
    //   Ścieżki z kropką sięgają głębiej: {@code "lines.product"} (ĆWICZENIE 3).
    //
    // @NamedEntityGraph — graf zapisany na ENCJI pod nazwą (ShopOrder: "ShopOrder.withCustomerAndLines"), a w
    // repozytorium tylko {@code @EntityGraph("ShopOrder.withCustomerAndLines")}. Ten sam SQL (test). Przydaje się, gdy
    // jeden graf jest używany w wielu metodach; na co dzień {@code attributePaths} jest krótsze i czytelniejsze.
    //
    // join fetch czy @EntityGraph? To samo pod spodem (JOIN + wypełnienie relacji). @EntityGraph działa z metodami
    // z nazwy i z odziedziczonymi {@code findAll(Pageable)}/{@code findById} (nadpisanymi w repozytorium) bez JPQL;
    // join fetch daje pełną kontrolę (INNER/LEFT, warunki, kolejność). Obie drogi mają te same PUŁAPKI z sekcji 2 i 4.

    // =================================================================================================
    // 4. BATCH FETCHING — 1 + N/rozmiar; dwie listy naraz → MultipleBagFetchException
    // =================================================================================================
    //
    // {@code @BatchSize(size = 5)} (org.hibernate.annotations — adnotacja Hibernate, nie JPA) na liście płatności:
    // zapytanie główne się nie zmienia, ale gdy trzeba wczytać płatności JEDNEGO zamówienia, Hibernate dociąga płatności
    // do 5 zamówień czekających w kontekście. 10 zamówień → 1 + 2 zapytania (sprawdzone testem):
    //     {@code select p1_0.order_id,p1_0.id,p1_0.amount from payment p1_0 where p1_0.order_id in (?,?,?,?,?) order by p1_0.id}  ×2
    //
    // Globalnie dla WSZYSTKICH leniwych relacji: {@code spring.jpa.properties.hibernate.default_batch_fetch_size=16}.
    // Test (Rel05HibernateSettingsTest): klienci i pozycje 10 zamówień → 1 + 1 + 1 zapytania:
    //     {@code select c1_0.id,c1_0.email,c1_0.name from customer c1_0 where c1_0.id in (?,?,…)}     ← 16 znaków ?
    //     {@code select l1_0.order_id,… from order_line l1_0 where l1_0.order_id in (?,?,…) order by l1_0.id}
    //   Ciekawostka: znaków {@code ?} jest zawsze 16, choć klientów są 4 — Hibernate dopełnia listę do rozmiaru paczki,
    //   żeby baza widziała ciągle TO SAMO polecenie (jeden plan zapytania zamiast osobnego na każdą długość listy).
    //
    // DOBRA PRAKTYKA: {@code default_batch_fetch_size} (np. 16–100) jako „siatka bezpieczeństwa” w całej aplikacji.
    //   Dlaczego? Nie usuwa N+1 do zera, ale zamienia 1 + 1000 w 1 + 10 bez zmiany ani jednego zapytania — a tam,
    //   gdzie wiesz, czego potrzebujesz, i tak używasz join fetch / DTO.
    //
    // PUŁAPKA: {@code join fetch} DWÓCH list (bagów) naraz: {@code … left join fetch o.lines left join fetch o.payments}.
    //   Sprawdzone testem — przy wykonaniu zapytania, zanim cokolwiek pójdzie do bazy:
    //   {@code IllegalArgumentException} ← {@code org.hibernate.loader.MultipleBagFetchException: cannot simultaneously}
    //   {@code fetch multiple bags: […ShopOrder.lines, …ShopOrder.payments]}. Dlaczego Hibernate odmawia? JOIN dwóch
    //   kolekcji to iloczyn kartezjański: wierszy jest {@code pozycje × płatności} na zamówienie (tu 24 zamiast 20 —
    //   test), a w worku (List bez indeksu) Hibernate nie odróżni prawdziwych duplikatów od powtórzeń z JOIN-a.
    //   PYTANIE REKRUTACYJNE: „Skąd się bierze MultipleBagFetchException i jak go obejść?”
    //
    // Rozwiązania:
    //   • DWA zapytania (najlepsze): najpierw {@code findAllWithCustomerAndLines()}, potem
    //     {@code select o from ShopOrder o left join fetch o.payments where o in :orders} (fetchPayments). Drugie
    //     zapytanie trafia na TE SAME obiekty z kontekstu utrwalania i dopisuje im płatności — razem 2 zapytania,
    //     obie listy wczytane (sprawdzone testem). Kolejność wyniku bierz z pierwszego zapytania.
    //   • {@code @BatchSize} / {@code default_batch_fetch_size} na drugiej liście — 1 + N/rozmiar.
    //   • zmiana jednej listy na {@code Set} — wyjątek znika, ale iloczyn kartezjański zostaje (24 wiersze zamiast 20,
    //     przy 50 pozycjach i 10 płatnościach — 500 wierszy na zamówienie). Działa, lecz to najdroższa „naprawa”.

    // =================================================================================================
    // 5. PROJEKCJA DTO — najtańszy odczyt; tabela porównawcza
    // =================================================================================================
    //
    //   {@code select new s09_jpa_relations.rel05_n_plus_one.OrderRow(}
    //   {@code     o.orderNumber, c.name, count(l), coalesce(sum(l.quantity * l.unitPrice), 0))}
    //   {@code from ShopOrder o join o.customer c left join o.lines l group by o.orderNumber, c.name order by o.orderNumber}
    //   SQL (sprawdzony testem):
    //     {@code select so1_0.order_number,c1_0.name,count(l1_0.id),coalesce(sum((l1_0.quantity*l1_0.unit_price)),0)}
    //     {@code from orders so1_0 join customer c1_0 on … left join order_line l1_0 on … group by … order by …}
    //   1 zapytanie, 10 wierszy (jeden na zamówienie), 0 wczytanych encji; ZAM-010: 0 pozycji, wartość 0.
    //
    //   Sposób (10 zamówień z klientem i pozycjami)  zapytań   wierszy z bazy   encje w pamięci   kiedy
    //   leniwe relacje w pętli (N+1)                   15          10+4+19         33               nigdy przy listach
    //   join fetch / @EntityGraph                       1            20             33               zmieniasz dane / potrzebujesz encji
    //   default_batch_fetch_size=16                     3          10+4+19         33               siatka bezpieczeństwa
    //   dwa zapytania (+ płatności)                     2          20 + 12          44 (33 + 11)     kilka kolekcji naraz
    //   projekcja {@code select new} (DTO)                     1            10              0               odczyt: listy, raporty, API
    //   (zapytania i wiersze sprawdzone testami; encje = 10 zamówień + 4 klientów + 19 pozycji, produkty pomijam)
    //
    // DOBRA PRAKTYKA: do ODCZYTU (lista na ekran, odpowiedź API, raport) — projekcja DTO; encje wczytuj, gdy zamierzasz
    //   je ZMIENIAĆ. Dlaczego? Projekcja pobiera tylko potrzebne kolumny, liczy w bazie, nie tworzy proxy ani kopii do
    //   dirty checking (wykrywania zmian) i nie ma w niej nic leniwego (s09_jpa_relations/Rel06EntityToDto).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • N+1: lista (1) + dociąganie relacji każdego elementu (N). „Do jednego”: +1 na RÓŻNEGO rodzica; lista: +1 na element.
     *   • Wykrywanie: log SQL, statystyki Hibernate, StatementInspector, asercja na liczbie zapytań w teście.
     *   • {@code join fetch} — 1 zapytanie; dla kolekcji {@code left join fetch} (inner gubi rodziców bez dzieci).
     *   • Hibernate 6+/7 sam usuwa duplikaty korzenia przy fetch kolekcji — distinct niepotrzebny.
     *   • Stronicowanie + fetch kolekcji: Hibernate 7 — podzapytanie SQL, gdy sortujesz po rodzicu; inaczej W PAMIĘCI
     *     (HHH90003004). {@code hibernate.query.fail_on_pagination_over_collection_fetch=true} → wyjątek zamiast tego.
     *     Najpewniej: strona id + {@code join fetch … where o.id in :ids}.
     *   • {@code @EntityGraph(attributePaths = {"customer", "lines", "lines.product"})} — JOIN-y bez JPQL; @NamedEntityGraph
     *     — graf na encji, używany przez nazwę.
     *   • {@code @BatchSize(size = n)} / {@code hibernate.default_batch_fetch_size} → 1 + N/n zapytań z {@code in (…)}.
     *   • Dwie listy w join fetch → MultipleBagFetchException; naprawa: dwa zapytania (albo batch); Set = iloczyn kartezjański.
     *   • Odczyt → projekcja {@code select new Rekord(…)}: 1 zapytanie, 0 encji.
     *
     * PYTANIA KONTROLNE:
     *   1. Co to jest problem N+1 i dlaczego testy na małych danych go nie pokazują?
     *   2. Co się stanie? 10 zamówień od 4 klientów, wczytane {@code select o from ShopOrder o}; w pętli
     *      {@code o.getCustomer().getName()}. Ile zapytań i dlaczego nie 11?
     *   3. ZNAJDŹ BŁĄD: {@code @Query("select o from ShopOrder o join fetch o.lines") List<ShopOrder> findAllWithLines();}
     *      — raport „wszystkie zamówienia” pokazuje 9 z 10.
     *   4. Co się stanie? {@code select o from ShopOrder o left join fetch o.lines l order by l.id} z
     *      {@code setMaxResults(3)} w Hibernate 7 — a co, gdy włączysz {@code fail_on_pagination_over_collection_fetch}?
     *   5. Co się stanie? {@code select o from ShopOrder o left join fetch o.lines left join fetch o.payments}, gdy obie
     *      kolekcje są typu List. Jak to naprawić bez zmiany typów?
     *   6. Ile zapytań wyśle dociągnięcie płatności 10 zamówień przy {@code @BatchSize(size = 5)} i jak wygląda SQL?
     *   7. Czy w Hibernate 7 trzeba pisać {@code select distinct o} przy {@code join fetch} kolekcji? Dlaczego kiedyś trzeba było?
     *   8. Kiedy wybierzesz projekcję DTO zamiast join fetch?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku NPlusOneExercises.java (metody z TODO), testy: Rel05NPlusOneExercisesTest (liczą
    // zapytania!). Potrzebne zapytania dopisujesz w ExerciseShopOrderRepository.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Jedno zapytanie o listę i po jednym dodatkowym na każdy element, którego leniwa relacja jest dociągana
     *      (w pętli, w mapowaniu, w toString). Koszt rośnie liniowo z liczbą wierszy; przy 10 wierszach to
     *      milisekundy, przy 1000 — sekundy. Nie ma wyjątku, więc widać go tylko w logu/licznikach SQL.
     *   2. 1 + 4 = 5 zapytań (sprawdzone testem): po jednym na KAŻDEGO RÓŻNEGO klienta. Klient wczytany raz jest
     *      w kontekście utrwalania, więc kolejne zamówienia tego samego klienta dostają go bez SQL.
     *   3. join fetch to złączenie wewnętrzne — zamówienie bez pozycji (ZAM-010) nie ma pary w order_line i znika
     *      (test: 9 zamówień). Poprawka: {@code left join fetch o.lines}.
     *   4. Sortowanie po polu kolekcji nie pozwala stronicować w podzapytaniu, więc Hibernate wczytuje wszystkie wiersze
     *      i bierze 3 zamówienia w pamięci, logując HHH90003004 „firstResult/maxResults specified with collection fetch;
     *      applying in memory”. Z włączonym ustawieniem — HibernateException „setFirstResult() or setMaxResults()
     *      specified with collection fetch join (in-memory pagination was about to be applied, but … is enabled)”.
     *      Oba sprawdzone testami.
     *   5. Przy wykonaniu: IllegalArgumentException z przyczyną MultipleBagFetchException „cannot simultaneously fetch
     *      multiple bags: [….lines, ….payments]”, nic nie idzie do bazy (test). Naprawa: dwa zapytania — najpierw
     *      left join fetch o.lines, potem left join fetch o.payments where o in :orders (te same obiekty z kontekstu
     *      dostają płatności) — albo @BatchSize na drugiej liście.
     *   6. 2 zapytania (10 / 5) plus zapytanie główne:
     *      {@code select p1_0.order_id,p1_0.id,p1_0.amount from payment p1_0 where p1_0.order_id in (?,?,?,?,?) order by p1_0.id}
     *      (sprawdzone testem).
     *   7. Nie: Hibernate 6+ (i 7) sam usuwa powtórzenia encji-korzenia przy fetch kolekcji — test bez distinct daje
     *      10 zamówień z 20 wierszy. Hibernate 5 zwracał zamówienie tyle razy, ile miało pozycji, stąd distinct
     *      w starszych tutorialach.
     *   8. Gdy tylko CZYTASZ dane do wyświetlenia/odpowiedzi API/raportu — zwłaszcza z agregatami (sumy, liczby).
     *      Projekcja to 1 zapytanie, tylko potrzebne kolumny i 0 encji. join fetch, gdy potrzebujesz encji do zmiany.
     */
    // </editor-fold>
}

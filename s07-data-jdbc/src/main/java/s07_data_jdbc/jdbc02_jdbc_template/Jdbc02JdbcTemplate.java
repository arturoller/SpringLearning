package s07_data_jdbc.jdbc02_jdbc_template;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <pre>
 * TEMAT: JdbcTemplate i NamedParameterJdbcTemplate — „klasyczne” API Springa do SQL-a
 *        (template = szablon — gotowy schemat postępowania, w który wstawiasz tylko SQL i mapowanie;
 *         batch = wsad, wiele poleceń wysłanych razem; extractor = ekstraktor, obiekt odczytujący cały wynik;
 *         JOIN = złączenie tabel)
 *
 * W SKRÓCIE:
 *   JdbcTemplate istnieje od pierwszych wersji Springa (początek lat 2000), więc spotkasz go w ogromnej liczbie
 *   istniejących projektów i tutoriali. Robi to samo co JdbcClient: pożycza połączenie, wykonuje SQL, mapuje wiersze,
 *   zamyka zasoby i tłumaczy wyjątki na DataAccessException. Różnice: API jest „płaskie” (metody z wieloma
 *   argumentami zamiast płynnego łańcucha), a parametry nazwane (:name) ma OSOBNA klasa
 *   NamedParameterJdbcTemplate. W nowym kodzie zwykle wybierzesz JdbcClient, ale czytać JdbcTemplate musisz umieć —
 *   a do operacji wsadowych i ResultSetExtractor jest wygodny także dziś.
 *
 * ANALOGIA: stary, solidny kombajn kontra nowoczesny.
 *   JdbcClient to kombajn z wygodną kabiną i joystickiem (jeden płynny ruch ręki). JdbcTemplate to ten sam silnik
 *   i te same noże, tylko z dziesięcioma dźwigniami. Pole nadal skoszą oba — a w gospodarstwie jest ich pełno
 *   i musisz wiedzieć, którą dźwignię pociągnąć.
 *
 * JAK TO DZIAŁA:
 *   1. Autokonfiguracja (JdbcTemplateAutoConfiguration) tworzy bean JdbcTemplate oraz NamedParameterJdbcTemplate.
 *   2. NamedParameterJdbcTemplate NIE ma własnego połączenia: opakowuje ten sam JdbcTemplate (test sprawdza, że
 *      getJdbcOperations() zwraca dokładnie ten bean). Zamienia :nazwy na ? i deleguje pracę do JdbcTemplate.
 *   3. JdbcClient (lekcja Jdbc01JdbcClient) jest zbudowany na NamedParameterJdbcTemplate — czyli pod spodem
 *      wszystko i tak kończy się w JdbcTemplate. Dlatego zachowanie (wyjątki, tłumaczenie błędów) jest takie samo.
 *   4. Wzorzec „template method”: Spring trzyma niezmienną część algorytmu (połączenie, zamknięcie, wyjątki),
 *      a Ty podajesz zmienną: SQL, parametry i RowMapper (zamiana wiersza na obiekt).
 *
 * SŁÓWKA:
 *   RowMapper = zamienia JEDEN wiersz na obiekt; ResultSetExtractor = dostaje CAŁY wynik i buduje z niego jeden obiekt
 *   (np. listę zamówień z pozycjami); SqlParameterSource = źródło parametrów nazwanych; batch update = zmiana wsadowa;
 *   foreign key = klucz obcy (kolumna wskazująca wiersz innej tabeli); N+1 = jedno zapytanie + N kolejnych.
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc01JdbcClient (nowsze API, te same pojęcia),
 *             t29_jdbc_databases/Jdbc03PreparedStatement (batch i parametry w czystym JDBC),
 *             t29_jdbc_databases/Jdbc06SqlJoins (JOIN — składnia SQL),
 *             s07_data_jdbc/Jdbc03SqlInit (skąd tabele tej lekcji),
 *             s09_jpa_relations/Rel05NPlusOne (ten sam problem N+1 w JPA).
 * </pre>
 */
public final class Jdbc02JdbcTemplate {

    private Jdbc02JdbcTemplate() {
    }

    // =================================================================================================
    // 1. JDBCTEMPLATE — queryForObject, query, update (i jak to się ma do JdbcClient)
    // =================================================================================================
    //
    // Najczęstsze metody (parametry pozycyjne ? podajesz NA KOŃCU, jako varargs):
    //   jdbcTemplate.queryForObject("SELECT COUNT(*) …", Long.class)                     → skalar
    //   jdbcTemplate.queryForObject("SELECT * FROM product WHERE id = ?", rowMapper, id) → jeden obiekt
    //   jdbcTemplate.query("SELECT * … WHERE category = ?", rowMapper, "dom")           → List
    //   jdbcTemplate.queryForList("SELECT name FROM product", String.class)              → lista wartości
    //   jdbcTemplate.update("UPDATE product SET stock = ? WHERE id = ?", stock, id)      → liczba wierszy
    //
    // RowMapper dla rekordów: new DataClassRowMapper<>(Product.class) — tworzy rekord konstruktorem i dopasowuje
    // kolumny po nazwach (snake_case → camelCase). Starszy BeanPropertyRowMapper wymaga konstruktora bez
    // argumentów i setterów, więc dla rekordu kończy się BeanInstantiationException (test sprawdza) — dla rekordów
    // i klas z konstruktorem zawsze DataClassRowMapper. (JdbcClient wybiera odpowiednie mapowanie sam.)
    //
    // PORÓWNANIE tego samego zapytania:
    //   JdbcTemplate:  jdbcTemplate.query("SELECT * FROM product WHERE category = ?", mapper, "dom");
    //   JdbcClient:    jdbc.sql("SELECT * FROM product WHERE category = ?").param("dom").query(Product.class).list();
    //
    // Test sekcji 1: zapytania, update i sprawdzenie, że NamedParameterJdbcTemplate opakowuje ten sam bean JdbcTemplate.
    //
    // DOBRA PRAKTYKA: wstrzykuj JdbcTemplate/JdbcClient konstruktorem i trzymaj SQL w jednej klasie (DAO/repozytorium
    //   — lekcja Jdbc05Repository). Dlaczego? Wtedy reszta aplikacji nie wie nic o SQL-u, a zmiana bazy dotyczy jednego miejsca.

    // =================================================================================================
    // 2. NamedParameterJdbcTemplate — parametry nazwane
    // =================================================================================================
    //
    // JdbcTemplate zna tylko ?, a przy 5 parametrach łatwo je pomylić kolejnością. NamedParameterJdbcTemplate
    // pozwala pisać :category, :minPrice. Parametry podajesz jako SqlParameterSource:
    //   • MapSqlParameterSource — budowany ręcznie: new MapSqlParameterSource().addValue("category", "dom")
    //   • BeanPropertySqlParameterSource(obiekt) — nazwy brane z obiektu (gettery/akcesory; działa też z rekordem)
    //   • zwykła Map<String,Object> — najkrótsza
    // Dodatkowa zaleta: LISTA w klauzuli IN. {@code WHERE id IN (:ids)} z listą [1, 2] zostanie rozwinięte do
    // {@code IN (?, ?)}. Zwykły JdbcTemplate tego nie potrafi.
    //
    // Test sekcji 2: wyszukiwanie po kategorii i przedziale cen, IN z listą (też pustą), INSERT z obiektu.
    //
    // PUŁAPKA: pusta lista w IN (:ids). Dlaczego źle? Po rozwinięciu powstaje {@code IN ()}. Test sprawdza (SQL
    //   zbudowany przez NamedParameterUtils), że tak właśnie jest. H2 2.4 przyjmuje taki zapis i zwraca pusty wynik,
    //   ale wiele baz (np. PostgreSQL, Oracle) odrzuca go jako błąd składni — kod „działający na H2” wywali się
    //   na produkcji. Sprawdź listę przed zapytaniem i zwróć pusty wynik sam.

    // =================================================================================================
    // 3. BATCH — batchUpdate dla wielu wierszy
    // =================================================================================================
    //
    //   int[] counts = jdbcTemplate.batchUpdate("INSERT INTO product (…) VALUES (?, ?, ?, ?)", listaTablicParametrów);
    //   int[] counts = namedTemplate.batchUpdate("… VALUES (:name, :category, :price, :stock)",
    //                                            SqlParameterSourceUtils.createBatch(listaObiektów));
    // Wynik to tablica liczników — po jednym na zestaw parametrów (tu: same jedynki). Są też wersje z
    // batchSize (dzielą dużą listę na paczki — wynik to int[][]) i z własnym ParameterizedPreparedStatementSetter.
    //
    // DLACZEGO SZYBCIEJ? Pętla z 1000 wywołań update() to 1000 osobnych „podróży” do bazy (po sieci). Batch
    // przygotowuje polecenie RAZ i wysyła zestawy parametrów razem — mniej podróży, mniej pracy bazy.
    // (Na H2 w pamięci różnica jest niewidoczna, na bazie po sieci — duża. Nie asertujemy czasu: testy mają być
    // deterministyczne.) Uwaga: niektóre sterowniki zamiast liczników zwracają Statement.SUCCESS_NO_INFO (-2),
    // czyli „udało się, ale nie wiem ile wierszy” — nie zakładaj, że w tablicy zawsze są jedynki.
    //
    // Test sekcji 3: oba warianty wstawiają po dwa wiersze i zwracają tablicę {1, 1}.
    //
    // DOBRA PRAKTYKA: wiele podobnych INSERT/UPDATE → batchUpdate, najlepiej w jednej transakcji (s10_transactions).
    //   Dlaczego? Bez transakcji awaria w połowie zostawi w bazie tylko część wierszy.

    // =================================================================================================
    // 4. ResultSetExtractor — zamówienie z pozycjami z jednego JOIN-a, i zapowiedź N+1
    // =================================================================================================
    //
    // Zamówienie ma wiele pozycji. JOIN zwraca JEDEN WIERSZ NA POZYCJĘ, np.:
    //     order_id | customer_name | order_date | product_id | quantity | unit_price
    //        1     | Anna Nowak    | 2026-03-01 |     1      |    2     |   59.00
    //        1     | Anna Nowak    | 2026-03-01 |     3      |    1     |   24.50
    //        2     | Jan Kowalski  | 2026-03-02 |     2      |    1     |  899.99
    // RowMapper (jeden wiersz → jeden obiekt) tu nie wystarczy — chcemy ZAMÓWIENIE z listą pozycji. Dlatego
    // ResultSetExtractor: dostaje cały ResultSet i sam grupuje wiersze po numerze zamówienia (Jdbc02OrderReader).
    // Wynik: 3 zamówienia z 2, 1 i 3 pozycjami — jedno zapytanie do bazy.
    //
    // WZORZEC „N+1” — ten sam wynik gorszą drogą: 1 zapytanie o zamówienia + po 1 o pozycje KAŻDEGO z N zamówień.
    // Dla 3 zamówień to 4 zapytania, dla 1000 zamówień — 1001. (ordersWithLinesNaive w Jdbc02OrderReader; test
    // sprawdza, że daje identyczne dane.) To samo zjawisko wróci jako klasyczny problem JPA/Hibernate
    // (s09_jpa_relations/Rel05NPlusOne) — tam przez ukryte ładowanie leniwych kolekcji, tu widzisz je gołym okiem.
    //
    // PUŁAPKA: pętla for z zapytaniem w środku. Dlaczego? Każde zapytanie to osobna podróż do bazy, więc czas rośnie
    //   liniowo z liczbą rekordów; na małych danych testowych nic nie widać, a na produkcji endpoint zwalnia.
    //
    // DOBRA PRAKTYKA: gdy potrzebujesz rodzica z dziećmi, pobierz je jednym JOIN-em (albo dwoma zapytaniami: rodzice
    //   i potem WHERE parent_id IN (:ids)) i połącz w Javie. Dlaczego? Stała liczba zapytań niezależnie od ilości danych.

    // =================================================================================================
    // 5. KIEDY CO — JdbcClient, JdbcTemplate czy JPA?
    // =================================================================================================
    //
    //   | Narzędzie              | Kiedy                                                                 |
    //   |------------------------|-----------------------------------------------------------------------|
    //   | JdbcClient             | NOWY kod z SQL-em: płynne API, parametry pozycyjne i nazwane w jednym |
    //   | JdbcTemplate           | istniejący kod; batch z własnym setterem; callbacki niskiego poziomu    |
    //   | NamedParameterJdbcT.   | istniejący kod z :parametrami; IN z listą; batch z obiektów            |
    //   | JPA/Hibernate (s08)    | model obiektowy z relacjami, dużo prostych CRUD-ów, mapowanie encji    |
    //
    // Reguła kciuka: raporty, zapytania pod wydajność i „dziwny” SQL → JDBC (masz pełną kontrolę nad zapytaniem);
    // klasyczne CRUD na grafie encji → JPA. Oba podejścia można mieszać w jednej aplikacji (ta sama transakcja).
    //
    // PUŁAPKA: queryForObject dla ZERA wierszy. Dlaczego? Nazwa sugeruje „daj obiekt albo null”, a metoda rzuca
    //   EmptyResultDataAccessException (test sekcji 1; dla 2+ wierszy — IncorrectResultSizeDataAccessException).
    //   Gdy brak wiersza jest normalny, użyj query(...) i stream().findFirst() (Optional) albo JdbcClient.optional().
    //   Dla skalara bywa też zwrócony null (np. kolumna NULL), dlatego wynik typu Long trzeba sprawdzić.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • JdbcTemplate: query / queryForObject / queryForList / update / batchUpdate; parametry ? na końcu wywołania.
     *   • NamedParameterJdbcTemplate: :nazwa + MapSqlParameterSource / BeanPropertySqlParameterSource / Map; IN (:ids).
     *   • Rekordy: DataClassRowMapper (nie BeanPropertyRowMapper).
     *   • NamedParameterJdbcTemplate opakowuje JdbcTemplate; JdbcClient opakowuje NamedParameterJdbcTemplate.
     *   • batchUpdate: jedno przygotowane polecenie, wiele zestawów parametrów, wynik int[] (albo int[][]).
     *   • ResultSetExtractor: cały wynik → jeden obiekt (np. zamówienie z pozycjami); RowMapper: wiersz → obiekt.
     *   • N+1: 1 + N zapytań w pętli — unikaj; JOIN albo IN (:ids).
     *   • queryForObject: 0 wierszy → EmptyResultDataAccessException, 2+ → IncorrectResultSizeDataAccessException.
     *   • Nowy kod: JdbcClient; stary kod: JdbcTemplate; relacje i CRUD na encjach: JPA.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaka jest zależność między JdbcClient, NamedParameterJdbcTemplate i JdbcTemplate?
     *   2. Co się stanie? {@code jdbcTemplate.queryForObject("SELECT name FROM product WHERE id = ?", String.class, 999)}
     *   3. Co się stanie? Ten sam kod, ale zapytanie {@code SELECT name FROM product} zwraca 4 wiersze.
     *   4. ZNAJDŹ BŁĄD: {@code new BeanPropertyRowMapper<>(Product.class)} dla rekordu Product — co się stanie
     *      i czym go zastąpić?
     *   5. Czym różni się RowMapper od ResultSetExtractor i kiedy potrzebny jest ten drugi?
     *   6. Dlaczego batchUpdate jest szybszy od pętli z update()?
     *   7. Co się stanie? {@code namedTemplate.query("… WHERE id IN (:ids)", new MapSqlParameterSource("ids", List.of()), mapper)}
     *   8. Na czym polega problem N+1 i jak go uniknąć w JDBC?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc02JdbcTemplateExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwy produktów z podanej kategorii, posortowane alfabetycznie.
     * Podpowiedź: {@code queryForList(sql, String.class, category)}, {@code ORDER BY name}.
     */
    static List<String> exercise1NamesInCategory(JdbcTemplate jdbcTemplate, String category) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć produkt o danym id albo {@code Optional.empty()}, gdy go nie ma.
     * Nie wolno rzucić wyjątku dla nieistniejącego id (pułapka queryForObject!).
     * Podpowiedź: {@code query(...)} zwraca listę; {@code new DataClassRowMapper<>(Product.class)}.
     */
    static Optional<Product> exercise2FindProduct(JdbcTemplate jdbcTemplate, long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć produkty o podanych numerach (lista może mieć kilka elementów), posortowane po id.
     * Dla pustej listy zwróć pustą listę (bez zapytania — patrz PUŁAPKA w sekcji 2).
     * Podpowiedź: {@code NamedParameterJdbcTemplate}, {@code IN (:ids)}.
     */
    static List<Product> exercise3ProductsByIds(NamedParameterJdbcTemplate namedTemplate, List<Long> ids) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): wstaw wszystkie produkty JEDNYM wywołaniem batchUpdate i zwróć sumę zmienionych
     * wierszy (dla poprawnych danych: liczba produktów). Podpowiedź: {@code batchUpdate} zwraca {@code int[]};
     * zsumuj ją pętlą albo {@code Arrays.stream(counts).sum()}.
     */
    static int exercise4InsertAll(JdbcTemplate jdbcTemplate, List<Product> products) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): oblicz w Javie wartość każdego zamówienia (suma ilość × cena jednostkowa z tabeli
     * order_line) i zwróć mapę numer zamówienia → wartość, w kolejności rosnących numerów (LinkedHashMap).
     * Podpowiedź: jedno zapytanie {@code SELECT order_id, quantity, unit_price FROM order_line ORDER BY order_id, id},
     * ResultSetExtractor z pętlą {@code while (rs.next())} i {@code Map.merge(klucz, wartość, BigDecimal::add)}.
     */
    static Map<Long, BigDecimal> exercise5OrderTotals(JdbcTemplate jdbcTemplate) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. JdbcClient jest zbudowany na NamedParameterJdbcTemplate, a ten opakowuje zwykły JdbcTemplate — wszystko
     *      kończy się w JdbcTemplate. Sprawdzone testem (sekcja 1: getJdbcOperations() to ten sam bean).
     *   2. EmptyResultDataAccessException („Incorrect result size: expected 1, actual 0”). Sprawdzone testem (sekcja 1).
     *   3. IncorrectResultSizeDataAccessException („expected 1, actual 4”). Sprawdzone testem (sekcja 1).
     *   4. Dla rekordu poleci BeanInstantiationException („Illegal arguments for constructor”) — BeanPropertyRowMapper
     *      chce konstruktora bez argumentów i setterów. Użyj DataClassRowMapper. Sprawdzone testem (sekcja 1).
     *   5. RowMapper zamienia jeden wiersz na jeden obiekt. ResultSetExtractor dostaje cały wynik naraz — potrzebny,
     *      gdy kilka wierszy składa się na jeden obiekt (zamówienie z pozycjami) albo trzeba zbudować mapę.
     *   6. Polecenie przygotowywane jest raz i wysyłane z wieloma zestawami parametrów — mniej podróży do bazy
     *      niż przy N osobnych wywołaniach.
     *   7. Spring rozwinie to do {@code IN ()}. H2 2.4 zwróci wtedy pusty wynik, ale inne bazy (PostgreSQL, Oracle)
     *      zgłoszą błąd składni, więc nie polegaj na tym zapisie — sprawdź listę przed zapytaniem. Sprawdzone testem
     *      (sekcja 2).
     *   8. Jedno zapytanie o rodziców + po jednym o dzieci każdego rodzica → 1 + N zapytań. Unikasz go JOIN-em albo
     *      dwoma zapytaniami (rodzice, potem dzieci przez IN (:ids)) i połączeniem w Javie. Sprawdzone testem (sekcja 4).
     */
    // </editor-fold>
}

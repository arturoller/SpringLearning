package s07_data_jdbc.jdbc01_jdbc_client;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * <pre>
 * TEMAT: JdbcClient — zapytania SQL w Springu bez ręcznego JDBC
 *        (JdbcClient = klient JDBC, nowoczesne API od Spring Framework 6.1; query = zapytanie;
 *         parameter = parametr; row = wiersz; mapping = mapowanie; generated key = klucz wygenerowany)
 *
 * W SKRÓCIE:
 *   JdbcClient to cienka, wygodna nakładka na JDBC. Piszesz SQL jako tekst, podajesz parametry
 *   i mówisz, w co zamienić wiersze: {@code jdbc.sql("SELECT ...").param(...).query(Product.class).list()}.
 *   Spring sam bierze połączenie z puli, przygotowuje zapytanie (PreparedStatement), mapuje wiersze, zamyka zasoby
 *   i zamienia wyjątek kontrolowany SQLException na wyjątek niekontrolowany (DataAccessException).
 *
 * ANALOGIA: zamówienie w barze przez kelnera.
 *   Czysty JDBC to samodzielne chodzenie do kuchni: bierzesz tacę (połączenie), wołasz kucharza (statement),
 *   przynosisz talerze (ResultSet) i sprzątasz stolik (zamykanie). JdbcClient to kelner: mówisz tylko CO chcesz
 *   („SELECT ... dla kategorii KSIAZKI”), a on załatwia całą resztę i podaje gotowy talerz (listę rekordów).
 *
 * JAK TO DZIAŁA:
 *   1. Boot widzi spring-boot-starter-jdbc i bazę H2 na classpath → tworzy DataSource (pulę HikariCP).
 *   2. Autokonfiguracja tworzy JdbcTemplate, NamedParameterJdbcTemplate i JdbcClient (beany gotowe do wstrzyknięcia).
 *   3. {@code sql(...)} zaczyna zapytanie; {@code param(...)} dodaje parametry; {@code query(...)} wybiera sposób
 *      mapowania wiersza; {@code list() / single() / optional()} wykonuje zapytanie i zwraca wynik.
 *   4. Pod spodem działa JdbcTemplate: PreparedStatement z parametrami, mapowanie, zwolnienie połączenia z powrotem
 *      do puli, tłumaczenie wyjątków SQL.
 *   5. Schemat i dane lekcji tworzą pliki db/jdbc01/schema.sql i data.sql (ustawione w application-jdbc01.properties).
 *
 * SŁÓWKA:
 *   query = zapytanie; param = parametr; positional = pozycyjny; named = nazwany; single = jeden;
 *   optional = opcjonalny (może go nie być); update = zmiana danych (INSERT/UPDATE/DELETE); row = wiersz;
 *   column = kolumna; key holder = „kieszeń” na klucz; injection = wstrzyknięcie; pool = pula połączeń.
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc03PreparedStatement (czysty JDBC i PreparedStatement w JavaLearning),
 *             t29_jdbc_databases/Jdbc05Dao (wzorzec DAO),
 *             s07_data_jdbc/Jdbc02JdbcTemplate (starsze API, które spotkasz w tutorialach),
 *             s07_data_jdbc/Jdbc05Repository (repozytorium na JdbcClient),
 *             s10_transactions/Tx01Transactional (transakcje).
 * </pre>
 */
public final class Jdbc01JdbcClient {

    private Jdbc01JdbcClient() {
    }

    // =================================================================================================
    // 1. ZAPYTANIE → LISTA REKORDÓW
    // =================================================================================================
    //
    //   List<Product> all = jdbc.sql("SELECT * FROM product ORDER BY id").query(Product.class).list();
    //
    // query(Product.class) mapuje każdy wiersz na rekord Product PO NAZWACH: kolumna sku → pole
    // sku. Spring zamienia też snake_case na camelCase (kolumna unit_price → pole unitPrice).
    // Kolejność kolumn w SQL nie ma znaczenia. Test sekcji 1: ProductQueries#findAll.
    //
    // PUŁAPKA: pole rekordu, dla którego nie ma kolumny w wyniku, to błąd, a nie wartość null.
    //   Dlaczego? Mapper czyta każdą kolumnę po nazwie; brak kolumny to SQLException, który Spring tłumaczy na
    //   BadSqlGrammarException (zły SQL). Zapytanie SELECT id, sku FROM product zmapowane na pełny rekord Product
    //   rzuca wyjątek. Wybieraj wszystkie kolumny rekordu albo zrób osobny, mniejszy rekord pod to zapytanie.
    //   Sprawdzone testem (sekcja 1).
    //
    // DOBRA PRAKTYKA: wypisuj kolumny jawnie (SELECT id, sku, name ...) zamiast SELECT *
    //   w kodzie produkcyjnym. Dlaczego? Gdy ktoś doda kolumnę (np. duży opis), SELECT * po cichu zacznie ją
    //   pobierać wszędzie. W lekcji używamy * tylko dla krótkości.

    // =================================================================================================
    // 2. PARAMETRY: POZYCYJNE (?) I NAZWANE (:nazwa)
    // =================================================================================================
    //
    //   pozycyjne:  "... WHERE category = ?"             .param(category)
    //   nazwane:    "... WHERE category = :category"     .param("category", category)
    //
    // Pozycyjne: wartości przypisywane są w kolejności wystąpienia znaków ?. Nazwane: po nazwie, więc kolejność
    // nie ma znaczenia, a ta sama nazwa może wystąpić w SQL kilka razy. Przy więcej niż dwóch parametrach nazwane
    // są czytelniejsze i bezpieczniejsze (nie pomylisz kolejności dwóch liczb). Test sekcji 2.
    //
    // PUŁAPKA: mieszanie ? i :nazwa w jednym zapytaniu.
    //   Dlaczego? JdbcClient przekazuje parametry jednym z dwóch sposobów i nie umie połączyć obu; rzuca
    //   IllegalStateException: Configure either named or indexed parameters, not both. Wybierz jeden styl na zapytanie.
    //   Sprawdzone testem (sekcja 2).

    // =================================================================================================
    // 3. single() KONTRA optional()
    // =================================================================================================
    //
    //   .list()      → wiele wierszy (może być pusta lista)
    //   .single()    → DOKŁADNIE jeden wiersz; zero → EmptyResultDataAccessException;
    //                  kilka → IncorrectResultSizeDataAccessException
    //   .optional()  → Optional: zero → pusty, jeden → wartość; kilka → IncorrectResultSizeDataAccessException
    //
    // Komunikaty z uruchomienia: „Incorrect result size: expected 1, actual 0” oraz „... actual 3”.
    // EmptyResultDataAccessException jest podklasą IncorrectResultSizeDataAccessException — obie to
    // DataAccessException, czyli wyjątek niekontrolowany (nie trzeba go łapać ani deklarować).
    //
    // PUŁAPKA: single() do szukania rekordu, którego może nie być (np. po numerze podanym przez użytkownika).
    //   Dlaczego? Brak wyniku to normalna sytuacja biznesowa, a nie wyjątek techniczny — single() zamieni ją
    //   w błąd 500. Gdy brak rekordu jest możliwy, używaj optional() i zdecyduj, co zrobić z pustym Optional.
    //   Sprawdzone testem (sekcja 3).
    //
    // DOBRA PRAKTYKA: single() tam, gdzie wynik MUSI istnieć (np. COUNT(*), odczyt po kluczu,
    //   który właśnie wstawiłeś). Dlaczego? Wtedy brak wyniku naprawdę jest błędem programisty i głośny wyjątek pomaga go znaleźć.

    // =================================================================================================
    // 4. WARTOŚCI PROSTE I WŁASNY RowMapper
    // =================================================================================================
    //
    //   .query(Integer.class).single()          // np. COUNT(*)
    //   .query(String.class).list()             // jedna kolumna → lista napisów
    //   .query((rs, rowNum) -> ...).list()      // RowMapper (mapowanie wiersza): sam czytasz kolumny z ResultSet
    //
    // Gdy wynik nie pasuje do rekordu (np. sklejasz kilka kolumn w napis albo dodajesz logikę), podaj własny
    // RowMapper — funkcję (ResultSet, numer wiersza) → obiekt. Test sekcji 4.

    // =================================================================================================
    // 5. INSERT / UPDATE / DELETE, LICZBA WIERSZY I KLUCZ GENEROWANY
    // =================================================================================================
    //
    //   int rows = jdbc.sql("UPDATE ...").param(...).update();        // liczba zmienionych wierszy
    //   jdbc.sql("INSERT ...").param(...).update(keyHolder, "id");   // + klucz nadany przez bazę (IDENTITY)
    //
    // update() zwraca liczbę wierszy, których dotyczyła zmiana: 0 znaczy „nic nie pasowało do WHERE”
    // — i NIE jest wyjątkiem. Zawsze sprawdzaj ten wynik, gdy brak zmiany to błąd biznesowy (np. zmiana stanu
    // nieistniejącego produktu). Klucz generowany odbierasz przez KeyHolder; podaj nazwę kolumny
    // ("id"), żeby baza zwróciła dokładnie ją. Parametry można też wziąć z pól rekordu:
    // .paramSource(product) — :sku czyta product.sku(). Test sekcji 5.
    //
    // PUŁAPKA: ignorowanie wyniku update().
    //   Dlaczego? UPDATE ... WHERE sku = 'LITEROWKA' kończy się „sukcesem” i zwraca 0. Bez sprawdzenia
    //   aplikacja uzna, że stan został zmieniony, a baza stoi bez zmian. Sprawdzone testem (sekcja 5).

    // =================================================================================================
    // 6. WSTRZYKIWANIE SQL (SQL INJECTION)
    // =================================================================================================
    //
    //   ŹLE:    jdbc.sql("SELECT * FROM product WHERE name = '" + name + "'")
    //   DOBRZE: jdbc.sql("SELECT * FROM product WHERE name = :name").param("name", name)
    //
    // Przy sklejaniu tekstu wartość od użytkownika staje się CZĘŚCIĄ polecenia SQL. Wartość x' OR '1'='1
    // zmienia sens zapytania — zwraca wszystkie produkty. Apostrof w nazwie typu O'Reilly wystarczy, by zapytanie
    // się wysypało (BadSqlGrammarException). Przy parametrze baza dostaje polecenie i wartość OSOBNO, więc wartość
    // nigdy nie zostanie wykonana jako SQL. Test sekcji 6 pokazuje obie wersje na tych samych danych.
    //
    // PUŁAPKA: parametry da się podać tylko tam, gdzie stoi WARTOŚĆ. Nazwy tabeli, kolumny ani kierunku sortowania
    //   (ORDER BY ?) nie podasz jako parametru. Gdy musisz je zmienić dynamicznie, wybieraj z zamkniętej listy
    //   (np. switch po enum), nigdy nie wklejaj tekstu od użytkownika. Dlaczego? Parametr to wartość, a nie
    //   fragment składni SQL.
    //
    // DOBRA PRAKTYKA: ZAWSZE parametry zamiast konkatenacji. Dlaczego? To najprostsza i najskuteczniejsza ochrona przed
    //   jedną z najgroźniejszych podatności aplikacji webowych.

    // =================================================================================================
    // 7. ILE KODU ZNIKA W PORÓWNANIU Z CZYSTYM JDBC
    // =================================================================================================
    //
    // PlainJdbcProducts#findByCategory (czysty JDBC): ok. 15 linii — połączenie, PreparedStatement, ResultSet,
    // pętla while, ręczne rs.getXxx(...) dla każdego pola, dwa zagnieżdżone try-with-resources i obsługa
    // SQLException. ProductQueries#findByCategory (JdbcClient): 5 linii samej treści. Wynik jest identyczny.
    // Test sekcji 7 porównuje obie wersje. Pod spodem JdbcClient robi dokładnie to samo, co kod ręczny — tylko
    // nie możesz zapomnieć o zamknięciu zasobów.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Zapytanie: jdbc.sql("SQL").param(...).query(Rekord.class).list() | single() | optional().
     *   • Parametry: ? + param(wartość) ALBO :nazwa + param("nazwa", wartość) — nie mieszaj stylów.
     *   • single(): dokładnie jeden wiersz (0 → EmptyResultDataAccessException, >1 → IncorrectResultSizeDataAccessException);
     *     optional(): może nie być wiersza.
     *   • Rekord mapuje się po nazwach kolumn (snake_case → camelCase); brak kolumny dla pola = BadSqlGrammarException.
     *   • update() zwraca liczbę wierszy (0 nie jest błędem); klucz generowany: update(keyHolder, "id").
     *   • Wartości zawsze jako parametry — nigdy sklejane w tekst SQL (wstrzykiwanie SQL).
     *   • JdbcClient to bean z autokonfiguracji (starter jdbc + DataSource); błędy SQL = DataAccessException.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie trzy rzeczy robi za Ciebie JdbcClient w porównaniu z czystym JDBC?
     *   2. Co się stanie? Wywołujesz getBySku("XXX-999") z single() — takiego produktu nie ma.
     *   3. Co się stanie? W jednym zapytaniu używasz WHERE category = ? AND stock >= :minStock.
     *   4. ZNAJDŹ BŁĄD: jdbc.sql("SELECT * FROM product WHERE name = '" + name + "'") — co jest nie tak
     *      i jak to naprawić?
     *   5. Czym różni się single() od optional() i kiedy użyjesz którego?
     *   6. Co się stanie? changeStock("LITEROWKA", 5) — czy poleci wyjątek? Co zwróci metoda?
     *   7. Jak odczytasz klucz nadany przez bazę po INSERT?
     *   8. ZNAJDŹ BŁĄD: zapytanie SELECT id, sku FROM product zmapowane na rekord Product — dlaczego rzuca wyjątek?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc01JdbcClientExercisesTest (tabela product, dane z db/jdbc01/data.sql)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć NAZWY produktów z podanej kategorii, od najdroższego do najtańszego.
     * Podpowiedź: query(String.class), parametr :category, ORDER BY price DESC.
     */
    static List<String> exercise1(JdbcClient jdbc, String category) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć NAJTAŃSZY produkt z podanej kategorii, którego jest na stanie co najmniej 1 sztuka
     * ({@code stock > 0}). Kategoria bez dostępnych produktów → Optional.empty().
     * Podpowiedź: ORDER BY price LIMIT 1 i optional().
     */
    static Optional<Product> exercise2(JdbcClient jdbc, String category) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): dodaj produkt do kategorii DOM ze stanem 0 i zwróć id nadane przez bazę.
     * Podpowiedź: GeneratedKeyHolder i update(keyHolder, "id").
     */
    static long exercise3(JdbcClient jdbc, String sku, String name, BigDecimal price) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zmniejsz stan magazynowy produktu o amount sztuk, ale tylko wtedy, gdy stan
     * wystarcza ({@code stock >= amount}); zwróć true, jeśli zmiana nastąpiła. Całość JEDNYM poleceniem
     * UPDATE — bez wcześniejszego SELECT. Podpowiedź: warunek w WHERE i update() zwracające liczbę wierszy.
     * (Dlaczego jednym poleceniem? Dwa kroki „najpierw sprawdź, potem zmień” mogłyby się rozjechać przy równoległych
     * zamówieniach.)
     */
    static boolean exercise4(JdbcClient jdbc, String sku, int amount) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bierze połączenie z puli i je zwalnia; przygotowuje PreparedStatement i mapuje wiersze na obiekty;
     *      tłumaczy SQLException (kontrolowany) na DataAccessException (niekontrolowany). Zamyka też wszystkie zasoby.
     *   2. EmptyResultDataAccessException: „Incorrect result size: expected 1, actual 0”. Sprawdzone testem (sekcja 3).
     *   3. IllegalStateException: „Configure either named or indexed parameters, not both” — nie wolno mieszać
     *      parametrów pozycyjnych i nazwanych. Sprawdzone testem (sekcja 2).
     *   4. Wartość od użytkownika jest częścią tekstu SQL, więc można zmienić sens zapytania (wstrzykiwanie SQL; nawet
     *      zwykły apostrof psuje zapytanie). Naprawa: WHERE name = :name i .param("name", name).
     *      Sprawdzone testem (sekcja 6).
     *   5. single(): wynik MUSI istnieć (brak = wyjątek). optional(): brak jest normalny, dostajesz
     *      pusty Optional. Oba rzucają wyjątek, gdy wierszy jest więcej niż jeden.
     *   6. Wyjątku nie ma. Metoda zwraca 0 (żaden wiersz nie pasuje do WHERE). Sprawdzone testem (sekcja 5).
     *   7. Przez new GeneratedKeyHolder() i update(keyHolder, "id"), potem keyHolder.getKey().
     *   8. Rekord Product ma pola name, category, price, stock, a wynik zapytania nie zawiera takich kolumn. Mapper nie
     *      wstawia null, tylko rzuca BadSqlGrammarException. Sprawdzone testem (sekcja 1).
     */
    // </editor-fold>
}

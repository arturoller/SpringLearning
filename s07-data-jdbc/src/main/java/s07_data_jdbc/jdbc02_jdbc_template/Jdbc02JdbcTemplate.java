package s07_data_jdbc.jdbc02_jdbc_template;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;

/**
 * <pre>
 * TEMAT: JdbcTemplate i NamedParameterJdbcTemplate — starsze, ale wszechobecne narzędzia JDBC
 *        (template = szablon, czyli klasa z gotowym "szkieletem" czynności; RowMapper = mapper wiersza, zamienia
 *         jeden wiersz wyniku na obiekt; batch = wsad, wiele poleceń wysłanych do bazy naraz)
 *
 * W SKRÓCIE:
 *   JdbcTemplate (od Spring 1.x) robi to samo co JdbcClient z lekcji Jdbc01JdbcClient, ale ma inne, starsze API:
 *   parametry podajesz jako ostatnie argumenty metody, a wynik mapujesz obiektem RowMapper. W starszych projektach
 *   spotkasz go WSZĘDZIE, więc trzeba go umieć czytać. NamedParameterJdbcTemplate to ten sam szablon z parametrami
 *   nazwanymi (:nazwa). Do NOWEGO kodu zwykle wybierasz JdbcClient — a JdbcTemplate zostaje tam, gdzie JdbcClient
 *   czegoś nie umie, np. w operacjach wsadowych (batch).
 *
 * ANALOGIA: stary i nowy pilot do telewizora.
 *   Oba sterują tym samym telewizorem (tą samą bazą, tym samym JDBC). Nowy pilot (JdbcClient) ma płynne, czytelne
 *   przyciski; stary (JdbcTemplate) ma więcej pokręteł i kilka wariantów tej samej metody — ale tylko on ma
 *   przycisk "nagraj paczkę odcinków naraz" (batchUpdate).
 *
 * JAK TO DZIAŁA:
 *   1. Autokonfiguracja Boota tworzy trzy beany na tym samym DataSource: JdbcTemplate, NamedParameterJdbcTemplate
 *      i JdbcClient. JdbcClient jest "fasadą" (nakładką) nad NamedParameterJdbcTemplate.
 *   2. jdbcTemplate.query("SELECT ... WHERE city = ?", mapper, "Warszawa"):
 *        a. bierze połączenie z puli, tworzy PreparedStatement i ustawia parametry z ostatnich argumentów,
 *        b. dla każdego wiersza woła Twój RowMapper (rs, rowNum) → obiekt,
 *        c. zamyka zasoby i zwraca List; błędy SQL zamienia na DataAccessException.
 *   3. jdbcTemplate.queryForObject(...) wymaga DOKŁADNIE jednego wiersza (inaczej wyjątek).
 *   4. batchUpdate(sql, lista parametrów) wysyła jedno polecenie z wieloma zestawami parametrów — mniej
 *      "rozmów" z bazą niż N osobnych update().
 *
 * SŁÓWKA:
 *   query = zapytanie; queryForObject = zapytanie o jeden obiekt/wartość; update = zmiana danych; batch update =
 *   zmiana wsadowa; row mapper = mapper wiersza; result set = zbiór wyników; parameter source = źródło parametrów;
 *   incorrect result size = zły rozmiar wyniku; empty result = pusty wynik; facade = fasada.
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc01JdbcClient, t29_jdbc_databases/Jdbc05Dao (to samo ręcznie, w czystym JDBC),
 *             t29_jdbc_databases/Jdbc03PreparedStatement.
 * </pre>
 */
public final class Jdbc02JdbcTemplate {

    private Jdbc02JdbcTemplate() {
    }

    // =================================================================================================
    // 1. JdbcTemplate.query + RowMapper (lambda)
    // =================================================================================================
    //
    //   List<Customer> list = jdbcTemplate.query(
    //           "SELECT id, name, city FROM customers WHERE city = ? ORDER BY name",
    //           (rs, rowNum) -> new Customer(rs.getLong("id"), rs.getString("name"), rs.getString("city")),
    //           "Warszawa");
    //
    // RowMapper to interfejs z JEDNĄ metodą mapRow(ResultSet rs, int rowNum), więc pasuje do lambdy (t13_lambdas/Lambda02FunctionalInterfaces).
    // Różnica wobec JdbcClient: tu MAPUJESZ RĘCZNIE (kolumna po kolumnie), a JdbcClient potrafi zamienić wiersz
    // na rekord sam. Parametry (?) to ostatnie argumenty metody — ich kolejność musi zgadzać się z kolejnością w SQL.
    //
    // PUŁAPKA: parametry pozycyjne w złej kolejności, np. query(sql, mapper, city, minId) przy SQL "... id > ? AND city = ?".
    //   Dlaczego? Baza dostanie tekst tam, gdzie oczekuje liczby (albo, gorzej, cicho dopasuje inne dane), a kompilator
    //   nic nie zauważy, bo parametry to Object... — dlatego nazwane parametry są bezpieczniejsze.

    // =================================================================================================
    // 2. queryForObject kontra optional(): co, gdy wiersza nie ma
    // =================================================================================================
    //
    //   Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Integer.class);   // zawsze 1 wiersz
    //   String name   = jdbcTemplate.queryForObject("SELECT name FROM customers WHERE id = ?", String.class, 99);
    //
    // queryForObject wymaga dokładnie jednego wiersza:
    //   • 0 wierszy  → EmptyResultDataAccessException (pusty wynik),
    //   • 2+ wierszy → IncorrectResultSizeDataAccessException (zły rozmiar wyniku).
    // Sprawdzone testem sekcji 2. JdbcClient ma na to wygodniejszą drogę: .query(...).optional() daje Optional,
    // a .single() zachowuje się tak jak queryForObject (to samo EmptyResultDataAccessException).
    //
    // PUŁAPKA: queryForObject do szukania po kluczu, którego może nie być. Dlaczego? Brak rekordu to zwykła sytuacja,
    //   a Ty dostajesz wyjątek, który łatwo przeoczyć i który wycieka aż do użytkownika jako błąd 500.
    // DOBRA PRAKTYKA: albo JdbcClient.optional(), albo query(...) i stream().findFirst(), albo świadomy catch.
    //   Dlaczego? Wtedy "nie ma takiego klienta" jest decyzją w Twoim kodzie, a nie niespodzianką.

    // =================================================================================================
    // 3. NamedParameterJdbcTemplate — parametry nazwane
    // =================================================================================================
    //
    //   namedTemplate.query("SELECT ... WHERE city = :city", Map.of("city", "Warszawa"), mapper)
    //
    // Parametry podajesz jako Map albo SqlParameterSource (MapSqlParameterSource, BeanPropertySqlParameterSource).
    // Nazwy w SQL (:city) łączą się z kluczami mapy — kolejność przestaje mieć znaczenie. To dokładnie ten mechanizm,
    // którego pod spodem używa JdbcClient dla swoich .param("city", ...).

    // =================================================================================================
    // 4. Operacje wsadowe (batch)
    // =================================================================================================
    //
    //   jdbcTemplate.batchUpdate("INSERT INTO customers (name, city) VALUES (?, ?)",
    //                            List.of(new Object[]{"A", "X"}, new Object[]{"B", "Y"}));      // int[] — po jednym wyniku na zestaw
    //   namedTemplate.batchUpdate("INSERT ... VALUES (:name, :city)", SqlParameterSourceUtils.createBatch(listaRekordow));
    //
    // Wsad wysyła wiele zestawów parametrów jednym poleceniem. Dlaczego to ważne? Każda "rozmowa" z bazą (zwłaszcza
    // przez sieć) kosztuje czas; 1000 osobnych INSERT-ów to 1000 podróży, wsad — kilka. Zwrócona tablica int[] ma
    // jeden element na zestaw parametrów (w H2: po 1 dla każdego wstawionego wiersza — sprawdzone testem sekcji 4).
    // JdbcClient NIE ma operacji wsadowych, więc do importów danych sięgasz po JdbcTemplate lub NamedParameterJdbcTemplate.
    // SqlParameterSourceUtils.createBatch(...) umie zamienić listę rekordów na zestawy parametrów (po nazwach komponentów).
    //
    // PUŁAPKA: pętla for z osobnym update() na każdy z tysięcy rekordów. Dlaczego? To tysiące osobnych zapytań sieciowych;
    //   import, który we wsadzie trwa sekundę, w pętli potrafi trwać minuty.

    // =================================================================================================
    // 5. Co wybrać?
    // =================================================================================================
    //
    //   JdbcClient                   NOWY kod: płynne API, parametry nazwane, mapowanie na rekordy, optional()
    //   NamedParameterJdbcTemplate   gdy potrzebujesz wsadu z nazwanymi parametrami albo starszego API
    //   JdbcTemplate                 stary kod, wsad z parametrami pozycyjnymi, mniej typowe operacje (CallableStatement...)
    // Wszystkie trzy to cienkie nakładki na to samo JDBC i ten sam DataSource — można je mieszać w jednej klasie.
    //
    // DOBRA PRAKTYKA: w nowym kodzie zacznij od JdbcClient, a po JdbcTemplate sięgaj tylko po to, czego JdbcClient
    //   nie ma (wsad). Dlaczego? Jeden styl w całym projekcie jest łatwiejszy do czytania i utrzymania niż mieszanka.
    //   (PYTANIE REKRUTACYJNE: "Czym jest JdbcTemplate i jakie problemy rozwiązuje?" — usuwa szablonowy kod JDBC:
    //   zarządzanie połączeniem, zamykanie zasobów i tłumaczenie wyjątków.)

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • JdbcTemplate: query(sql, rowMapper, parametry...), queryForObject(sql, Typ.class, parametry...), update, batchUpdate.
     *   • RowMapper (rs, rowNum) → obiekt: lambda, mapowanie ręczne.
     *   • queryForObject: 0 wierszy → EmptyResultDataAccessException, 2+ → IncorrectResultSizeDataAccessException.
     *   • NamedParameterJdbcTemplate: parametry nazwane z Map / SqlParameterSource.
     *   • batchUpdate: wiele zestawów parametrów naraz, wynik int[]; createBatch(lista rekordów) dla nazwanych.
     *   • JdbcClient = nowoczesna fasada (bez batch); JdbcTemplate = stary kod i wsad.
     *   • Wszystkie rzucają niesprawdzane DataAccessException, nie SQLException.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? {@code jdbcTemplate.queryForObject("SELECT name FROM customers WHERE id = ?", String.class, 99)}
     *      — klienta o id 99 nie ma.
     *   2. Co się stanie? To samo zapytanie bez WHERE (w tabeli są 3 wiersze).
     *   3. Który obiekt wybierzesz, żeby wstawić 10 000 klientów z pliku, i dlaczego nie pętlę z update()?
     *   4. ZNAJDŹ BŁĄD: {@code jdbcTemplate.query("SELECT ... WHERE id > ? AND city = ?", mapper, "Warszawa", 1)}.
     *   5. Czym różni się RowMapper od mapowania na rekord w JdbcClient?
     *   6. Co zwraca batchUpdate i ile elementów ma wynik dla trzech zestawów parametrów?
     *   7. Jak w JdbcClient odpowiednikiem queryForObject jest single(), a czym się różni optional()?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc02JdbcTemplateExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć klientów z podanego miasta posortowanych po nazwie (ORDER BY name), używając
     * {@code jdbcTemplate.query(sql, rowMapper, miasto)} i RowMappera w postaci lambdy tworzącej rekord Customer.
     */
    static List<Customer> exercise1(JdbcTemplate jdbcTemplate, String city) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): znajdź klienta po id przez {@code queryForObject} (z RowMapperem), ale gdy go nie ma,
     * zwróć Optional.empty() zamiast wyjątku. Podpowiedź: złap EmptyResultDataAccessException.
     */
    static Optional<Customer> exercise2(JdbcTemplate jdbcTemplate, long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): wstaw WSZYSTKICH podanych klientów jednym wsadem (batch) i zwróć liczbę wstawionych
     * wierszy (suma elementów tablicy int[] z batchUpdate). Pole id rekordu ignoruj — baza nada je sama.
     * Podpowiedź: namedTemplate.batchUpdate(sql z :name i :city, SqlParameterSourceUtils.createBatch(customers)).
     */
    static int exercise3(NamedParameterJdbcTemplate namedTemplate, List<Customer> customers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. EmptyResultDataAccessException — queryForObject wymaga dokładnie jednego wiersza. Sprawdzone testem 2.
     *   2. IncorrectResultSizeDataAccessException (zwróciło 3 wiersze, oczekiwano 1; to klasa NADRZĘDNA wobec
     *      EmptyResult...). Sprawdzone testem 2.
     *   3. JdbcTemplate lub NamedParameterJdbcTemplate z batchUpdate: wsad zmniejsza liczbę rozmów z bazą. Pętla z update()
     *      to 10 000 osobnych zapytań. JdbcClient nie ma operacji wsadowych.
     *   4. Kolejność parametrów jest odwrócona: pierwszy ? to id (liczba), a podano "Warszawa" (tekst); drugi ? to city,
     *      a podano 1. Naprawa: podaj 1, "Warszawa" albo użyj parametrów nazwanych.
     *   5. RowMapper mapuje się ręcznie, kolumna po kolumnie (rs.getString("name")...), a JdbcClient.query(Customer.class)
     *      dopasowuje kolumny do komponentów rekordu sam (po nazwach).
     *   6. int[] — po jednym wyniku na zestaw parametrów; dla trzech zestawów 3 elementy (w H2 każdy równy 1).
     *      Sprawdzone testem 4.
     *   7. single() zachowuje się jak queryForObject (0 wierszy → EmptyResultDataAccessException, 2+ → IncorrectResultSize...).
     *      optional() zwraca Optional.empty() dla 0 wierszy, a dla 2+ nadal rzuca IncorrectResultSizeDataAccessException.
     *      Sprawdzone testem 2.
     */
    // </editor-fold>
}

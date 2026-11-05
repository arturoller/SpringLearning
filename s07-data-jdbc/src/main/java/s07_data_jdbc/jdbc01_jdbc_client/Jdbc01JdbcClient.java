package s07_data_jdbc.jdbc01_jdbc_client;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * <pre>
 * TEMAT: JdbcClient — zapytania SQL w Springu bez żmudnego JDBC
 *        (JDBC = Java Database Connectivity, standardowe API Javy do baz SQL; client = klient;
 *         query = zapytanie; update = zmiana danych (INSERT/UPDATE/DELETE); parameter = parametr;
 *         row = wiersz; column = kolumna; mapping = mapowanie, przepisanie wiersza na obiekt)
 *
 * W SKRÓCIE:
 *   JdbcClient (Spring Framework 6.1+, jest też w Springu 7) to „płynne” API (fluent API — wywołania łączysz
 *   kropką w jedno zdanie): {@code jdbc.sql("SELECT ...").param(...).query(Product.class).list()}.
 *   W czystym JDBC sam otwierasz połączenie, przygotowujesz zapytanie, przepisujesz wiersze ResultSet na obiekty,
 *   zamykasz zasoby w try-with-resources i łapiesz SQLException. JdbcClient robi to wszystko za Ciebie,
 *   a błędy zamienia na hierarchię DataAccessException (wyjątki niezależne od rodzaju bazy, bez sprawdzanych SQLException).
 *   Boot tworzy bean JdbcClient automatycznie — wystarczy go wstrzyknąć.
 *
 * ANALOGIA: taksówka zamiast własnego samochodu.
 *   Czysty JDBC to jazda własnym autem: sam tankujesz, parkujesz, pilnujesz kluczyków. JdbcClient to taksówka:
 *   mówisz DOKĄD jedziesz (zapytanie SQL, parametry), a o paliwo, parking i drzwi dba kierowca (Spring). Trasę
 *   (SQL) nadal wyznaczasz Ty — to nie jest „magia” jak w JPA (s08).
 *
 * JAK TO DZIAŁA:
 *   1. Starter spring-boot-starter-jdbc + H2 → autokonfiguracja tworzy DataSource (pulę połączeń HikariCP),
 *      JdbcTemplate, NamedParameterJdbcTemplate i JdbcClient (por. s03_boot_basics/Boot01AutoConfiguration).
 *   2. JdbcClientAutoConfiguration buduje JdbcClient na bazie NamedParameterJdbcTemplate (odczytane z kodu Boot 4.1.1),
 *      a ten opakowuje zwykły JdbcTemplate — JdbcClient to „nowocześniejsza fasada” (nakładka) na stare, sprawdzone API.
 *   3. Przy każdym zapytaniu: pożyczenie połączenia z puli → PreparedStatement → ustawienie parametrów →
 *      wykonanie → mapowanie wierszy → oddanie połączenia do puli. Zawsze, także przy wyjątku.
 *   4. Mapowanie klasy/rekordu (query(Product.class)) bierze kolumny po nazwach i zamienia snake_case na camelCase
 *      (discount_percent → discountPercent). Rekord jest tworzony przez konstruktor.
 *   5. Błąd SQL → SQLException → Spring tłumaczy na DataAccessException (np. BadSqlGrammarException,
 *      DataIntegrityViolationException).
 *
 * SŁÓWKA:
 *   fluent API = płynne API; row mapper = obiekt zamieniający wiersz na obiekt Javy; named parameter = parametr nazwany;
 *   positional parameter = parametr pozycyjny; generated key = klucz wygenerowany przez bazę; scalar = pojedyncza
 *   wartość (np. COUNT); SQL injection = wstrzyknięcie SQL (atak); rollback = wycofanie zmian; slice = wycinek kontekstu.
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc02Connection (to samo w czystym JDBC: Connection, Statement, ResultSet),
 *             t29_jdbc_databases/Jdbc03PreparedStatement (parametry i SQL injection w czystym JDBC),
 *             s03_boot_basics/Boot01AutoConfiguration (skąd się bierze bean JdbcClient),
 *             s07_data_jdbc/Jdbc02JdbcTemplate (starsze API, na którym JdbcClient się opiera),
 *             s07_data_jdbc/Jdbc03SqlInit (skąd tabele i dane tej lekcji).
 * </pre>
 */
public final class Jdbc01JdbcClient {

    private Jdbc01JdbcClient() {
    }

    // =================================================================================================
    // 1. CZYM JEST JDBCCLIENT I SKĄD GO MAMY — oraz dlaczego testy się „nie brudzą”
    // =================================================================================================
    //
    // PRZED (czysty JDBC, kilkanaście linii na jedno zapytanie):
    //   try (Connection c = dataSource.getConnection();
    //        PreparedStatement ps = c.prepareStatement("SELECT * FROM product WHERE id = ?")) {
    //       ps.setLong(1, id);
    //       try (ResultSet rs = ps.executeQuery()) {
    //           if (rs.next()) { return new Product(rs.getLong("id"), rs.getString("name"), …); }
    //       }
    //   } catch (SQLException e) { throw new RuntimeException(e); }
    //
    // PO (JdbcClient):
    //   jdbc.sql("SELECT * FROM product WHERE id = :id").param("id", id).query(Product.class).optional();
    //
    // Dane lekcji: tabela product (id, name, category, price DECIMAL(10,2), stock, discount_percent — ostatnia
    // może być NULL). Tworzą ją skrypty jdbc01/schema.sql i jdbc01/data.sql, a włącza je profil "jdbc01"
    // (application-jdbc01.properties; szczegóły skryptów — Jdbc03SqlInit). Pięć produktów: Klawiatura, Mysz,
    // Monitor (kategoria „komputery”), Kubek, Lampka („dom”).
    //
    // TESTY: @JdbcTest (pakiet org.springframework.boot.jdbc.test.autoconfigure — w Boot 4 w jarze spring-boot-jdbc-test;
    // w Boot 2/3: org.springframework.boot.test.autoconfigure.jdbc) to „wycinek” (slice): startuje TYLKO warstwę JDBC —
    // DataSource, JdbcTemplate, JdbcClient, skrypty SQL — i nie startuje kontrolerów ani serwisów. Dlatego jest szybki.
    // Dwie ważne cechy:
    //   • DataSource jest PODMIENIANY na wbudowaną bazę o losowej nazwie (@AutoConfigureTestDatabase, domyślnie
    //     replace = NON_TEST w Boot 4 — podmienia każdą bazę, której nie dostarczył sam test, np. kontener
    //     z @ServiceConnection; s11_testing/Test06Testcontainers). Adres z application-jdbc01.properties
    //     (jdbc:h2:mem:jdbc01) jest w teście ignorowany,
    //     a skrypty schema.sql/data.sql i tak wykonają się na tej podmienionej bazie. Cel: test nigdy nie dotknie
    //     „prawdziwej” bazy z konfiguracji.
    //   • KAŻDY test działa w transakcji, którą Spring WYCOFUJE (rollback) po zakończeniu testu. Dzięki temu
    //     INSERT/UPDATE/DELETE z jednego testu nie „brudzą” danych następnego, a kolejność testów nie ma znaczenia.
    //     (@JdbcTest ma w sobie @Transactional; zmiany z DDL — CREATE/DROP TABLE — nie są wycofywane, bo baza
    //     zatwierdza je od razu.)
    // Test sekcji 1: kontekst ma JdbcClient, test trwa w transakcji, dane startowe są na miejscu, a aplikacja
    // Jdbc01Application (uruchomiona normalnie) łączy się z jdbc:h2:mem:jdbc01.
    //
    // JAK @JdbcTest ZNAJDUJE KONFIGURACJĘ? Szuka w górę pakietów klasy z @SpringBootConfiguration — czyli
    // Jdbc01Application. Dlatego lekcja ma własną klasę startową (jak w całym kursie).
    //
    // DOBRA PRAKTYKA: testuj SQL na prawdziwej bazie (tu H2), a nie na mockach JdbcClient. Dlaczego? Mock sprawdzi,
    //   że wywołałeś metodę, ale nie wykryje literówki w SQL ani błędnego mapowania kolumn — a to najczęstsze błędy.
    //
    // PYTANIE REKRUTACYJNE: czym różni się @JdbcTest od @SpringBootTest i dlaczego dane z testu nie zostają w bazie?

    // =================================================================================================
    // 2. ZAPYTANIA — list(), single(), optional(), własny RowMapper, skalary
    // =================================================================================================
    //
    //   .query(Product.class).list()     → List<Product> (pusta lista, gdy brak wierszy)
    //   .query(Product.class).single()   → dokładnie jeden wiersz, inaczej wyjątek (sekcja 5)
    //   .query(Product.class).optional() → Optional<Product> (pusty, gdy brak wiersza)
    //   .query(String.class).list()      → lista wartości z JEDNEJ kolumny (String, Integer, BigDecimal…)
    //   .query(Long.class).single()      → skalar, np. SELECT COUNT(*)
    //   .query((rs, rowNum) -> …)        → własny RowMapper: lambda dostaje ResultSet ustawiony na bieżącym wierszu
    //   .query().listOfRows()            → List<Map<String,Object>> — „na szybko”, bez typu (klucze wielkimi literami w H2)
    //
    // MAPOWANIE NA REKORD: dla query(Product.class) Spring tworzy rekord konstruktorem i dopasowuje kolumny do pól
    // po nazwach, ignorując wielkość liter i zamieniając snake_case na camelCase. Dlatego kolumna discount_percent
    // trafia do pola discountPercent bez żadnej konfiguracji. Typy są konwertowane (DECIMAL → BigDecimal).
    // Kolumny, których rekord nie ma, są pomijane; za to KAŻDE pole rekordu musi mieć kolumnę w wyniku.
    //
    // Test sekcji 2 sprawdza wszystkie warianty na pięciu produktach (ceny porównujemy przez BigDecimal, nie double).
    //
    // PUŁAPKA: brak ORDER BY. Dlaczego źle? Baza nie gwarantuje kolejności wierszy bez ORDER BY. Test może przejść
    //   dziś, a jutro (po zmianie indeksu, bazy albo wersji) zwrócić inną kolejność — błąd trudny do znalezienia.
    //
    // DOBRA PRAKTYKA: ceny trzymaj jako DECIMAL w bazie i BigDecimal w Javie, nigdy double. Dlaczego? double nie
    //   zapisuje dokładnie wartości typu 0,1 (patrz JavaLearning), a w pieniądzach grosz ma znaczenie.

    // =================================================================================================
    // 3. PARAMETRY — pozycyjne (?) i nazwane (:name), paramSource
    // =================================================================================================
    //
    //   Pozycyjne:  .sql("... WHERE category = ? AND price < ?").param("dom").param(new BigDecimal("50"))
    //               albo naraz: .params("dom", new BigDecimal("50"))  — kolejność MA znaczenie.
    //   Nazwane:    .sql("... WHERE category = :category AND price < :maxPrice")
    //               .param("maxPrice", …).param("category", …)          — kolejność nie ma znaczenia.
    //   Z obiektu:  .paramSource(product) → :name, :price… brane z akcesorów rekordu / getterów.
    //   Z mapy:     .params(Map.of("category", "dom"))
    //
    // Parametry nazwane są czytelniejsze, gdy jest ich więcej niż dwa albo ten sam występuje dwa razy.
    // NIE WOLNO mieszać stylów w jednym zapytaniu — Spring rzuca IllegalStateException
    // „Configure either named or indexed parameters, not both” (sprawdzone testem).
    //
    // PO CO PARAMETRY? Baza dostaje polecenie (plan zapytania) i wartości OSOBNO. Wartość nigdy nie jest
    // „doklejona” do tekstu SQL, więc nie może zmienić jego znaczenia (sekcja 5). Przy okazji nie musisz sam
    // zamieniać liczb, dat i BigDecimal na tekst ani cytować apostrofów.
    //
    // Test sekcji 3: pozycyjne, nazwane (w odwróconej kolejności), mieszanie stylów (wyjątek) i paramSource.

    // =================================================================================================
    // 4. ZMIANY DANYCH — update(), liczba wierszy, klucz generowany
    // =================================================================================================
    //
    // update() służy do INSERT, UPDATE i DELETE (i DDL). Zwraca LICZBĘ ZMIENIONYCH WIERSZY:
    //   • UPDATE product SET stock = 3 WHERE id = 1  → 1
    //   • to samo dla id, którego nie ma             → 0  (to NIE jest błąd! warto sprawdzić wynik)
    //   • DELETE … WHERE id = ?                       → liczba usuniętych wierszy
    //
    // KLUCZ GENEROWANY: tabela ma id BIGINT AUTO_INCREMENT, czyli numer nadaje baza. Żeby go poznać:
    //   KeyHolder kh = new GeneratedKeyHolder();
    //   jdbc.sql("INSERT …").paramSource(product).update(kh, "id");
    //   long id = kh.getKey().longValue();
    // Nazwa kolumny ("id") mówi sterownikowi, który klucz zwrócić. Metoda update(kh) bez nazwy kolumn też działa w H2,
    // ale na innych bazach (np. PostgreSQL zwraca wszystkie kolumny) bywa niejednoznaczna — podawaj nazwę.
    //
    // Test sekcji 4: INSERT przez paramSource zwraca 1, klucz generowany jest większy od pięciu istniejących,
    // UPDATE/DELETE na nieistniejącym id zwraca 0.
    //
    // PUŁAPKA: ignorowanie wyniku update(). Dlaczego źle? „UPDATE … WHERE id = ?” dla nieistniejącego id kończy
    //   się bez błędu, więc aplikacja uzna, że zmieniła stan magazynu, choć nic się nie stało. Sprawdzaj, czy
    //   liczba wierszy zgadza się z oczekiwaną (a przy braku — zgłoś błąd).
    //
    // DOBRA PRAKTYKA: po INSERT odczytuj klucz z KeyHolder zamiast pytać „SELECT MAX(id)”. Dlaczego? Między
    //   INSERT a SELECT MAX(id) inny użytkownik może dodać swój wiersz — dostaniesz cudzy numer.

    // =================================================================================================
    // 5. PUŁAPKI — single() przy 0 i 2 wierszach, SQL injection, NULL i prymityw
    // =================================================================================================
    //
    // a) single() i liczba wierszy:
    //      0 wierszy → EmptyResultDataAccessException  ("Incorrect result size: expected 1, actual 0")
    //      2 wiersze → IncorrectResultSizeDataAccessException ("... expected 1, actual 2")
    //    EmptyResultDataAccessException jest PODKLASĄ IncorrectResultSizeDataAccessException (sprawdzone testem),
    //    a obie to DataAccessException. Gdy "brak wiersza" jest normalny — użyj optional().
    //
    // b) SQL injection (wstrzyknięcie SQL) — najgroźniejszy błąd w pracy z bazą (PYTANIE REKRUTACYJNE!).
    //    ŹLE:   "SELECT * FROM product WHERE name = '" + name + "'"
    //    Użytkownik wpisuje:  x' OR '1'='1      → SQL robi się:  … WHERE name = 'x' OR '1'='1'  → zwraca WSZYSTKO.
    //    Gorzej:  x'; DELETE FROM product; --    → H2 wykonuje DWA polecenia: kasuje całą tabelę.
    //    (To nie teoria — test tej lekcji to uruchamia; kasowanie jest wycofane przez rollback testu.)
    //    DOBRZE: parametr — "… WHERE name = :name" + .param("name", name). Ten sam tekst jest wtedy zwykłą nazwą
    //    produktu, której nie ma w bazie → 0 wierszy. Test pokazuje oba warianty obok siebie.
    //
    // c) NULL w kolumnie a typ prymitywny: kolumna discount_percent bywa NULL (Klawiatura, Monitor, Lampka).
    //    Rekord z polem {@code int discountPercent} nie może przyjąć NULL → ConversionFailedException
    //    („Failed to convert from type [null] to type [int]”). Rekord z {@code Integer discountPercent} (jak Product)
    //    dostaje null. To samo dla skalarów: query(Integer.class).single() dla NULL daje TypeMismatchDataAccessException
    //    („Result value is null but no null value expected”), a optional() zwraca pusty Optional.
    //
    // PUŁAPKA: sklejanie SQL ze Stringów. Dlaczego? Wartość z zewnątrz (formularz, URL) zmienia znaczenie polecenia.
    //   Jedyną obroną jest rozdzielenie polecenia od danych — parametry. Nie „oczyszczaj” wejścia na własną rękę
    //   (usuwanie apostrofów): zawsze coś pominiesz.
    //
    // DOBRA PRAKTYKA: pola, które mogą być NULL w bazie, są w rekordzie typami obiektowymi (Integer, BigDecimal,
    //   String), a nie prymitywami. Dlaczego? Prymityw nie ma wartości „brak” — i wyjątek wyskoczy dopiero na
    //   pierwszym wierszu z NULL, często dopiero na produkcji.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • JdbcClient: jdbc.sql("…").param(…).query(Typ.class).list() / single() / optional(); update() dla zmian.
     *   • Bean JdbcClient tworzy autokonfiguracja (na NamedParameterJdbcTemplate); wstrzykujesz go konstruktorem.
     *   • Parametry: ? + param(wartość) (pozycyjne) albo :nazwa + param("nazwa", wartość); nie mieszaj stylów.
     *   • Rekord mapuje się po nazwach kolumn (snake_case → camelCase); pola, które mogą być NULL — typy obiektowe.
     *   • single(): 0 wierszy → EmptyResultDataAccessException, 2+ → IncorrectResultSizeDataAccessException.
     *   • update() zwraca liczbę wierszy; 0 nie jest błędem. Klucz generowany: GeneratedKeyHolder + update(kh, "id").
     *   • SQL injection: nigdy nie sklejaj SQL; zawsze parametry.
     *   • @JdbcTest: wycinek JDBC, wbudowana baza o losowej nazwie, każdy test w transakcji wycofywanej na końcu.
     *   • Zawsze ORDER BY; ceny jako DECIMAL/BigDecimal.
     *
     * PYTANIA KONTROLNE:
     *   1. Co za Ciebie robi JdbcClient w porównaniu z czystym JDBC (wymień co najmniej trzy rzeczy)?
     *   2. Co się stanie? {@code jdbc.sql("SELECT * FROM product WHERE id = 999").query(Product.class).single()}
     *   3. Co się stanie? To samo zapytanie, ale z {@code .optional()} zamiast {@code .single()}.
     *   4. ZNAJDŹ BŁĄD: {@code jdbc.sql("SELECT * FROM product WHERE name = '" + name + "'")} — jaki atak umożliwia
     *      ten kod i jak go naprawić?
     *   5. Co się stanie? Rekord ma pole {@code int discountPercent}, a zapytanie zwraca wiersz z NULL w tej kolumnie.
     *   6. Dlaczego dane dodane w jednym teście z @JdbcTest nie są widoczne w następnym?
     *   7. Co zwróci {@code update()} dla {@code UPDATE product SET stock = 1 WHERE id = 999} i czy poleci wyjątek?
     *   8. ZNAJDŹ BŁĄD: {@code .sql("… WHERE category = ? AND price < :max").param("dom").param("max", cena)} —
     *      co się stanie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc01JdbcClientExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć liczbę produktów w podanej kategorii (parametr pozycyjny albo nazwany).
     * Podpowiedź: {@code SELECT COUNT(*) FROM product WHERE category = :category}, {@code .query(Long.class).single()}.
     */
    static long exercise1CountInCategory(JdbcClient jdbc, String category) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć produkt o podanej nazwie albo {@code Optional.empty()}, gdy go nie ma.
     * Nie wolno rzucać wyjątku dla nieistniejącej nazwy. Podpowiedź: sekcja 2 i 5 — {@code optional()}.
     */
    static Optional<Product> exercise2FindByName(JdbcClient jdbc, String name) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwiększ stan magazynu produktu o {@code delta} sztuk (UPDATE z wyrażeniem
     * {@code stock = stock + :delta}) i zwróć liczbę zmienionych wierszy.
     * Podpowiedź: {@code update()} zwraca liczbę wierszy; dla nieistniejącego id ma wyjść 0.
     */
    static int exercise3IncreaseStock(JdbcClient jdbc, long id, int delta) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): wstaw produkt (pole {@code id} rekordu jest puste) i zwróć numer nadany przez bazę.
     * Podpowiedź: {@code paramSource(product)}, {@code GeneratedKeyHolder}, {@code update(keyHolder, "id")}.
     */
    static long exercise4InsertAndReturnId(JdbcClient jdbc, Product product) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zwróć nazwy produktów, których cena mieści się w przedziale
     * {@code [min, max]} (włącznie), posortowane rosnąco po cenie, a przy równej cenie po nazwie.
     * Podpowiedź: {@code BETWEEN :min AND :max}, {@code ORDER BY price, name}, {@code query(String.class).list()}.
     */
    static List<String> exercise5NamesInPriceRange(JdbcClient jdbc, BigDecimal min, BigDecimal max) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Pożycza i zwraca połączenie z puli, przygotowuje PreparedStatement i ustawia parametry, mapuje wiersze na
     *      obiekty, zamyka zasoby (także przy wyjątku) i tłumaczy SQLException na DataAccessException.
     *   2. EmptyResultDataAccessException („Incorrect result size: expected 1, actual 0”). Sprawdzone testem (sekcja 5).
     *   3. Zwróci Optional.empty() — bez wyjątku. Sprawdzone testem (sekcja 2).
     *   4. SQL injection: wejście {@code x' OR '1'='1} zwraca wszystkie produkty, a {@code x'; DELETE FROM product; --}
     *      kasuje tabelę. Naprawa: {@code "… WHERE name = :name"} i {@code .param("name", name)}. Sprawdzone testem.
     *   5. ConversionFailedException — „Failed to convert from type [null] to type [int]”. Pole powinno być typu
     *      Integer. Sprawdzone testem (sekcja 5).
     *   6. @JdbcTest otacza każdy test transakcją i wycofuje ją (rollback) po jego zakończeniu. Sprawdzone testem:
     *      dwa testy dodają po jednym produkcie i oba widzą 6 produktów.
     *   7. Zwróci 0 i nie rzuci wyjątku — UPDATE bez pasujących wierszy jest poprawny. Sprawdzone testem (sekcja 4).
     *   8. IllegalStateException: „Configure either named or indexed parameters, not both”. Używaj jednego stylu.
     *      Sprawdzone testem (sekcja 3).
     */
    // </editor-fold>
}

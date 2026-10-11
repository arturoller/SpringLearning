package s07_data_jdbc.jdbc01_jdbc_client;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.Optional;

/**
 * <pre>
 * TEMAT: DataSource i JdbcClient — jak Spring rozmawia z bazą danych
 *        (DataSource = źródło połączeń z bazą; connection pool = pula połączeń; JdbcClient = nowoczesna "fasada"
 *         nad JDBC, dostępna od Spring Framework 6.1; parameter = parametr zapytania; mapping = mapowanie wyniku)
 *
 * W SKRÓCIE:
 *   W czystym JDBC (t29_jdbc_databases/Jdbc03PreparedStatement) sam otwierasz Connection, tworzysz PreparedStatement,
 *   czytasz ResultSet i pilnujesz zamykania wszystkiego. Spring Boot robi to za Ciebie: gdy na classpath jest baza
 *   H2 i startery JDBC, autokonfiguracja tworzy bean DataSource (z pulą połączeń HikariCP) oraz bean JdbcClient.
 *   Ty piszesz tylko SQL i parametry: jdbcClient.sql("...").param(...).query(Product.class).list().
 *
 * ANALOGIA: taksówka zamiast własnego samochodu.
 *   PRZED: sam prowadzisz, tankujesz, parkujesz i pilnujesz kluczyków (Connection, Statement, ResultSet, close).
 *   PO: mówisz tylko "dokąd" (SQL i parametry), a kierowca (JdbcClient) bierze auto z postoju (pula połączeń),
 *   jedzie, a po kursie oddaje auto na postój. Nie ma kursu, którego zapomnisz "zamknąć".
 *
 * JAK TO DZIAŁA:
 *   1. Start aplikacji: autokonfiguracja widzi H2 na classpath i brak własnego DataSource → tworzy bazę H2 w pamięci
 *      o unikalnej nazwie (adres zaczyna się od jdbc:h2:mem:) oraz pulę HikariCP (HikariDataSource).
 *   2. Autokonfiguracja tworzy też JdbcClient (bean) oparty na tym DataSource.
 *   3. jdbcClient.sql("SELECT ... WHERE price_cents ... :max").param("max", 1000).query(Product.class).list()
 *        a. bierze połączenie z puli,
 *        b. tworzy PreparedStatement i ustawia parametry (nigdy nie skleja ich z tekstem SQL!),
 *        c. wykonuje zapytanie i zamienia każdy wiersz na rekord Product (nazwy kolumn → nazwy komponentów),
 *        d. zwraca połączenie do puli, a wyjątki SQL zamienia na niesprawdzane DataAccessException.
 *
 * SŁÓWKA:
 *   data source = źródło danych; pool = pula; query = zapytanie (odczyt); update = zmiana danych (INSERT, UPDATE,
 *   DELETE); param = parametr; named parameter = parametr nazwany (:nazwa); positional parameter = parametr
 *   pozycyjny (?); list = lista; optional = opcjonalny (0 lub 1 wynik); single = dokładnie jeden wynik; row = wiersz;
 *   column = kolumna; SQL injection = wstrzyknięcie SQL (atak); affected rows = zmienione wiersze.
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc03PreparedStatement (parametry w czystym JDBC), t29_jdbc_databases/Jdbc02Connection,
 *             s07_data_jdbc/Jdbc02JdbcTemplate (starsze, ale wciąż wszechobecne narzędzie).
 * </pre>
 */
public final class Jdbc01JdbcClient {

    private Jdbc01JdbcClient() {
    }

    // =================================================================================================
    // 1. DataSource z autokonfiguracji (Jdbc01Application + profil jdbc01)
    // =================================================================================================
    //
    // Nie napisaliśmy ani linijki konfiguracji bazy, a w kontekście są beany DataSource i JdbcClient. To robi
    // Spring Boot (klasy DataSourceAutoConfiguration i JdbcClientAutoConfiguration — autokonfiguracja):
    //   • jest H2 na classpath, a spring.datasource.url nie jest ustawione → baza wbudowana (embedded) w pamięci,
    //   • nazwa bazy jest UNIKALNA dla każdego kontekstu (spring.datasource.generate-unique-name=true to domyślne),
    //     dzięki temu dwie lekcje albo dwa konteksty testowe nie widzą swoich tabel,
    //   • pula połączeń to HikariCP (domyślny wybór Boota, gdy jest na classpath).
    // Dla prawdziwej bazy (PostgreSQL) ustawiasz spring.datasource.url, .username i .password — reszta kodu bez zmian.
    //
    // DOBRA PRAKTYKA: baza w pamięci do nauki i testów, prawdziwa baza w produkcji, ten sam kod JDBC. Dlaczego? JdbcClient
    //   pracuje na interfejsie DataSource, więc nie wie (i nie musi wiedzieć), co jest pod spodem.

    // =================================================================================================
    // 2. Parametry: pozycyjne (?) i nazwane (:nazwa)
    // =================================================================================================
    //
    // pozycyjne:  jdbcClient.sql("SELECT ... WHERE price_cents < ? AND stock > ?").param(1000).param(5)...
    //             — kolejność .param(...) musi zgadzać się z kolejnością znaków ? w SQL.
    // nazwane:    jdbcClient.sql("SELECT ... WHERE price_cents < :max AND stock > :minStock")
    //                       .param("max", 1000).param("minStock", 5)...
    //             — kolejność dowolna, a SQL czyta się jak zdanie.
    //
    // DOBRA PRAKTYKA: parametry nazwane, gdy jest ich więcej niż jeden. Dlaczego? Przy pozycyjnych łatwo zamienić
    //   kolejność dwóch liczb (obie to int) i kompilator tego nie zauważy; nazwa parametru zdradza pomyłkę od razu.

    // =================================================================================================
    // 3. Odczyt: query(...) i list() / optional() / single()
    // =================================================================================================
    //
    //   .query(Product.class).list()       wszystkie wiersze jako List (pusta lista, gdy brak wyników)
    //   .query(Product.class).optional()   0 lub 1 wiersz jako Optional (Optional.empty, gdy brak)
    //   .query(Product.class).single()     DOKŁADNIE 1 wiersz; 0 wierszy → EmptyResultDataAccessException
    //                                      (pusty wynik), 2 i więcej → IncorrectResultSizeDataAccessException
    //                                      (zły rozmiar wyniku). Sprawdzone testem sekcji 6.
    //   .query(String.class).list()        pojedyncza kolumna jako zwykły typ (String, Integer, Long...)
    //   .query((rs, rowNum) -> ...)        własna lambda mapująca wiersz (RowMapper — mapper wiersza)
    //
    // Mapowanie na rekord (record): JdbcClient dopasowuje kolumny do komponentów rekordu po nazwie, bez względu
    // na wielkość liter, a PRICE_CENTS łączy z priceCents. Rekord musi mieć konstruktor pasujący do kolumn.
    //
    // PUŁAPKA: single() tam, gdzie wiersza może nie być (np. szukanie po nazwie od użytkownika). Dlaczego? Brak
    //   wyniku to normalna sytuacja biznesowa, a single() zamienia ją w wyjątek. Używaj optional(), a o braku
    //   decyduj sam (orElseThrow z własnym, czytelnym komunikatem).

    // =================================================================================================
    // 4. Zmiany: update() zwraca liczbę zmienionych wierszy
    // =================================================================================================
    //
    //   int n = jdbcClient.sql("UPDATE products SET stock = stock - :q WHERE id = :id")
    //                     .param("q", 2).param("id", 1).update();
    // Metoda update() służy do WSZYSTKICH poleceń zmieniających dane: INSERT, UPDATE, DELETE (nazwa jest myląca —
    // nie chodzi tylko o SQL-owy UPDATE). Zwraca liczbę zmienionych wierszy; 0 oznacza "nic nie pasowało do WHERE" —
    // to NIE jest wyjątek, więc warto sprawdzać wynik, gdy zmiana musi się powieść.
    //
    // PUŁAPKA: UPDATE/DELETE bez WHERE. Dlaczego? Zmieni albo usunie WSZYSTKIE wiersze tabeli, a baza nie zapyta
    //   "czy na pewno?". Zawsze sprawdź WHERE i liczbę wierszy zwróconą przez update().

    // =================================================================================================
    // 5. SQL injection (wstrzyknięcie SQL) — dlaczego NIGDY nie sklejamy tekstu
    // =================================================================================================
    //
    // ZŁE:     jdbcClient.sql("SELECT ... FROM products WHERE name = '" + input + "'")
    // Gdy input to: x' OR '1'='1   — powstaje SQL:  ... WHERE name = 'x' OR '1'='1'   → warunek jest ZAWSZE prawdziwy
    // i zapytanie zwraca wszystkie produkty. Przy innym wejściu atakujący może usuwać tabele lub czytać cudze dane.
    // DOBRE:   jdbcClient.sql("SELECT ... WHERE name = :name").param("name", input)
    // Parametr jedzie do bazy OSOBNO od tekstu SQL, więc baza traktuje cały napis jako zwykłą wartość do porównania,
    // nigdy jako kawałek polecenia. Sprawdzone testem sekcji 5 (ten sam napis: 4 wiersze vs 0 wierszy).
    //
    // PUŁAPKA: sklejanie SQL z danymi użytkownika ("..." + input). Dlaczego? Dane zmieniają się w kod SQL; to
    //   jedna z najgroźniejszych i najczęstszych podatności w aplikacjach (PYTANIE REKRUTACYJNE: "Jak chronić
    //   się przed SQL injection?" — parametry zapytania / PreparedStatement, nigdy konkatenacja).
    // DOBRA PRAKTYKA: każda wartość spoza kodu idzie przez .param(...). Dlaczego? To jedyny sposób, który działa
    //   zawsze; "ręczne ucieczki" znaków (escaping) łatwo zrobić źle.
    // Uwaga: parametrem może być WARTOŚĆ, nie nazwa tabeli ani kolumny. Nazwy (np. do ORDER BY z wyboru użytkownika)
    // sprawdzaj z listy dozwolonych (whitelist — biała lista), nie wstawiaj ich z zewnątrz.

    // =================================================================================================
    // 6. Błędy: wyjątki DataAccessException zamiast SQLException
    // =================================================================================================
    //
    // JDBC rzuca SQLException (sprawdzany, jeden typ na wszystko). Spring tłumaczy go na hierarchię niesprawdzanych
    // wyjątków DataAccessException (wyjątek dostępu do danych): EmptyResultDataAccessException, BadSqlGrammarException
    // (błąd w SQL), DuplicateKeyException (naruszenie unikalności) itd. Szczegóły w Jdbc05Repository.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Boot sam tworzy DataSource (H2 w pamięci, unikalna nazwa, pula HikariCP) i bean JdbcClient.
     *   • jdbcClient.sql("SQL").param(...) — parametry pozycyjne (?) albo nazwane (:nazwa); NIGDY konkatenacja.
     *   • .query(Klasa.class).list() / optional() / single(); rekord mapowany po nazwach kolumn (price_cents → priceCents).
     *   • single(): 0 wierszy → EmptyResultDataAccessException, 2+ → IncorrectResultSizeDataAccessException.
     *   • .update() = INSERT/UPDATE/DELETE, zwraca liczbę zmienionych wierszy (0 to nie błąd).
     *   • SQL injection: dane użytkownika sklejone z SQL stają się kodem; parametry tego nie robią.
     *   • JDBC rzuca SQLException, Spring — niesprawdzane DataAccessException.
     *
     * PYTANIA KONTROLNE:
     *   1. Kto tworzy bean DataSource w tej lekcji i jaką pulę połączeń wybiera Boot?
     *   2. Co się stanie? {@code jdbcClient.sql("SELECT ... WHERE id = :id").param("id", 999).query(Product.class).single()}
     *      — produktu o id 999 nie ma.
     *   3. Co się stanie? Zapytanie z nazwą produktu wpisaną jako {@code x' OR '1'='1}, sklejone z SQL operatorem +
     *      (a w drugim wariancie przekazane przez .param). Ile wierszy zwróci każdy wariant?
     *   4. ZNAJDŹ BŁĄD: {@code jdbcClient.sql("DELETE FROM products").update()} wywołane po to, by usunąć produkt "Mleko".
     *   5. Czym różni się optional() od single() i kiedy wybierzesz które?
     *   6. Dlaczego baza w pamięci nie ma w tej lekcji nazwy wpisanej w url, a dwie lekcje i tak nie widzą swoich tabel?
     *   7. Co się stanie? {@code update()} dla UPDATE z WHERE, który nie pasuje do żadnego wiersza.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc01JdbcClientExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwy produktów TAŃSZYCH niż podana cena (w groszach), posortowane alfabetycznie.
     * Np. dla 1000 wynik: [Czekolada, Mleko]. Podpowiedź: parametr nazwany :max, ORDER BY name, {@code .query(String.class).list()}.
     */
    static List<String> exercise1(JdbcClient jdbcClient, int maxPriceCents) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): znajdź produkt po nazwie. Gdy go nie ma, zwróć Optional.empty() (bez wyjątku!).
     * Nazwa pochodzi od użytkownika, więc ma iść parametrem (nie sklejaj SQL). Podpowiedź: query(Product.class) + optional().
     */
    static Optional<Product> exercise2(JdbcClient jdbcClient, String name) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zrób przecenę. Obniż cenę WSZYSTKICH produktów o podany procent (liczba całkowita
     * 1-99, dzielenie całkowite: nowa cena = cena * (100 - percent) / 100) i zwróć liczbę zmienionych produktów.
     * Podpowiedź: UPDATE products SET price_cents = price_cents * (100 - :percent) / 100, a liczbę wierszy zwraca update().
     */
    static int exercise3(JdbcClient jdbcClient, int percent) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Spring Boot (DataSourceAutoConfiguration), bo na classpath jest H2, a my nie podaliśmy url. Pula to HikariCP.
     *      Sprawdzone testem 1.
     *   2. EmptyResultDataAccessException (single() wymaga dokładnie jednego wiersza). Sprawdzone testem 6.
     *   3. Sklejone: 4 wiersze (wszystkie produkty — warunek OR '1'='1' jest zawsze prawdziwy). Z .param: 0 wierszy,
     *      bo nie ma produktu o nazwie dosłownie "x' OR '1'='1". Sprawdzone testem 5.
     *   4. Brak WHERE — usunie WSZYSTKIE produkty. Poprawnie: DELETE FROM products WHERE name = :name z parametrem.
     *   5. optional() daje Optional (brak wyniku to normalna sytuacja, np. szukanie po nazwie), single() zakłada, że
     *      wynik MUSI istnieć i jest jeden (np. odczyt po kluczu, który właśnie wstawiłeś) — inaczej wyjątek.
     *   6. Boot generuje unikalną nazwę bazy dla każdego kontekstu (spring.datasource.generate-unique-name=true).
     *      Sprawdzone testem 1 (adres jdbc:h2:mem:UUID).
     *   7. Zwróci 0 i NIE rzuci wyjątku. Sprawdzone testem 4.
     */
    // </editor-fold>
}

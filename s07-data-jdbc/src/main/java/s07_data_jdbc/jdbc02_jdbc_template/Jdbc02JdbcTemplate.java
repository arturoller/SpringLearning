package s07_data_jdbc.jdbc02_jdbc_template;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <pre>
 * TEMAT: JdbcTemplate i NamedParameterJdbcTemplate — starsze API JDBC w Springu, które spotkasz w tutorialach
 *        (template = szablon; row mapper = obiekt mapujący wiersz; batch = paczka poleceń;
 *         data access exception = wyjątek dostępu do danych; translation = tłumaczenie)
 *
 * W SKRÓCIE:
 *   JdbcTemplate to „szablon” zapytań: Spring robi wszystko, co powtarzalne (połączenie, PreparedStatement,
 *   zamykanie zasobów, wyjątki), a Ty podajesz tylko SQL, parametry i sposób mapowania wiersza (RowMapper).
 *   JdbcTemplate jest starszy od JdbcClient, więc większość przykładów w sieci jest napisana właśnie nim.
 *   NamedParameterJdbcTemplate dokłada parametry nazwane (:nazwa). Od Spring Framework 6.1 nowy kod pisze się
 *   zwykle JdbcClientem (lekcja Jdbc01JdbcClient), ale trzeba umieć czytać stary.
 *
 * ANALOGIA: formularz z lukami.
 *   Urząd ma gotowy formularz (szablon): nagłówek, pieczątki, kolejność kroków są stałe. Ty wypełniasz tylko luki:
 *   jakie zapytanie, z jakimi danymi i jak zamienić odpowiedź na obiekt. Reszty nie wymyślasz za każdym razem.
 *
 * JAK TO DZIAŁA:
 *   1. Autokonfiguracja Boota tworzy beany JdbcTemplate i NamedParameterJdbcTemplate (z DataSource).
 *   2. {@code query(sql, rowMapper, parametry...)} bierze połączenie z puli, wykonuje PreparedStatement,
 *      dla każdego wiersza woła Twój RowMapper i zwalnia zasoby.
 *   3. Gdy baza zgłosi SQLException, szablon zamienia go na odpowiedni podtyp DataAccessException
 *      (np. DuplicateKeyException) — na podstawie kodu błędu bazy (SQLState / vendor code).
 *   4. JdbcClient i JdbcTemplate to to samo „pod spodem”: JdbcClient opakowuje NamedParameterJdbcTemplate.
 *
 * SŁÓWKA:
 *   template = szablon; row mapper = mapper wiersza; named = nazwany; batch = paczka; bean = ziarno;
 *   translate = tłumaczyć; constraint = ograniczenie (np. UNIQUE, NOT NULL, klucz obcy); legacy = „stary”, odziedziczony.
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc01JdbcClient (nowoczesne API),
 *             s07_data_jdbc/Jdbc05Repository (repozytorium i wyjątek unikalności),
 *             s01_core_ioc/Ioc03Stereotypes (adnotacja @Repository),
 *             t29_jdbc_databases/Jdbc03PreparedStatement (czysty JDBC),
 *             s10_transactions/Tx01Transactional (transakcje).
 * </pre>
 */
public final class Jdbc02JdbcTemplate {

    private Jdbc02JdbcTemplate() {
    }

    // =================================================================================================
    // 1. query, queryForObject, queryForList, update
    // =================================================================================================
    //
    //   jdbcTemplate.query("SELECT ... WHERE category = ?", rowMapper, category)      → List<Product>
    //   jdbcTemplate.queryForObject("SELECT COUNT(*) ...", Integer.class, 0)           → pojedyncza wartość
    //   jdbcTemplate.queryForList("SELECT name ...", String.class, category)           → List<String>
    //   jdbcTemplate.update("UPDATE ... SET stock = ? WHERE sku = ?", 5, "ELE-001")   → liczba wierszy
    //
    // PUŁAPKA: kolejność argumentów. W query(sql, rowMapper, parametry...) mapper stoi PRZED parametrami,
    //   a w starszych tutorialach spotkasz wersję query(sql, parametry[], rowMapper) (tablica przed mapperem).
    //   Dlaczego to ważne? Wersje z tablicą są przestarzałe (deprecated — oznaczone do wycofania);
    //   podawaj parametry jako zwykłe argumenty na końcu. Przy pomyłce kompilator zwykle podpowie, ale
    //   zwróć uwagę na ostrzeżenia o przestarzałych metodach.
    // Metoda queryForList(sql) bez typu zwraca listę map „KOLUMNA → wartość” (nazwy kolumn WIELKIMI literami
    // w H2: {SKU=DOM-001, STOCK=2}). Wygodna do podglądu, zła w kodzie produkcyjnym (brak typów, łatwa literówka
    // w kluczu). Test sekcji 1.

    // =================================================================================================
    // 2. RowMapper — jak zamienić wiersz na obiekt
    // =================================================================================================
    //
    //   a) własny:                lambda (rs, rowNum) -> new Product(rs.getLong("id"), ...) — pełna kontrola;
    //   b) DataClassRowMapper:    mapuje po nazwach kolumn na rekord albo klasę z konstruktorem (dobry do rekordów);
    //   c) BeanPropertyRowMapper: mapuje na klasę JavaBean (pusty konstruktor + settery) — styl sprzed rekordów.
    //
    // DOBRA PRAKTYKA: dla rekordów używaj DataClassRowMapper (albo JdbcClient z query(Klasa.class)),
    //   a ręczny RowMapper tylko gdy mapowanie jest nietypowe. Dlaczego? Mniej kodu = mniej pomyłek w nazwach
    //   kolumn, a rekord pozostaje niezmienny (bez setterów). BeanPropertyRowMapper wymaga setterów, więc
    //   zmusza do klasy zmiennej (mutable). Test sekcji 2 pokazuje DataClassRowMapper i BeanPropertyRowMapper.

    // =================================================================================================
    // 3. queryForObject — PUŁAPKA braku wyniku (i wartości NULL)
    // =================================================================================================
    //
    //   Customer c = jdbcTemplate.queryForObject("SELECT * FROM customer WHERE id = ?", mapper, 99);
    //
    // PUŁAPKA: queryForObject zakłada, że wiersz ISTNIEJE. Zero wierszy → EmptyResultDataAccessException
    //   („Incorrect result size: expected 1, actual 0”), a więcej niż jeden → IncorrectResultSizeDataAccessException.
    //   Dlaczego to częsty błąd? W tutorialach zawsze „działa”, bo id istnieje; w prawdziwej aplikacji użytkownik
    //   poda numer nieistniejącego klienta i dostaje błąd 500. Napraw: query(...) zwraca listę —
    //   sprawdź isEmpty() (albo użyj JdbcClient z optional()). Sprawdzone testem (sekcja 3).
    //
    // PUŁAPKA: kolumna z wartością NULL. queryForObject("SELECT email ...", String.class, 2) dla klienta bez adresu
    //   zwraca null, a NIE wyjątek (wiersz istnieje, pusta jest tylko wartość). Dlaczego? Dla Springa „brak
    //   wiersza” i „NULL w kolumnie” to dwie różne sytuacje. Dla typów prostych (int) NULL dałby
    //   NullPointerException przy rozpakowaniu — dlatego queryForObject zwraca typ opakowany (Integer).
    //   Sprawdzone testem (sekcja 3).

    // =================================================================================================
    // 4. NamedParameterJdbcTemplate — parametry nazwane
    // =================================================================================================
    //
    //   SqlParameterSource params = new MapSqlParameterSource("min", min).addValue("max", max);
    //   namedTemplate.query("... WHERE price BETWEEN :min AND :max", params, rowMapper);
    //   namedTemplate.update("INSERT ... VALUES (:sku, :name, ...)", new BeanPropertySqlParameterSource(product));
    //
    // MapSqlParameterSource bierze wartości z „mapy”, BeanPropertySqlParameterSource — z właściwości obiektu
    // (tu: pola rekordu). Dzięki temu INSERT nie wymaga ręcznego wypisywania pięciu .addValue(...).
    // Test sekcji 4.
    //
    // DOBRA PRAKTYKA: przy trzech i więcej parametrach używaj nazwanych. Dlaczego? Zamiana kolejności dwóch liczb
    //   w pozycyjnym zapytaniu nie wywoła błędu, tylko po cichu zapisze złe dane.

    // =================================================================================================
    // 5. batchUpdate — wiele poleceń jedną paczką
    // =================================================================================================
    //
    //   jdbcTemplate.batchUpdate("UPDATE product SET stock = ? WHERE sku = ?", List<Object[]>)   → int[]
    //   namedTemplate.batchUpdate("INSERT ... (:sku, ...)", SqlParameterSource[])                  → int[]
    //
    // Zamiast 1000 osobnych wywołań (1000 podróży do bazy) wysyłasz jedną paczkę z tym samym SQL i 1000 zestawami
    // parametrów. Zwracana tablica int[] ma jeden element na zestaw: liczbę zmienionych wierszy.
    // Przykład z testu: stany trzech SKU, z których jeden nie istnieje → w tablicy jedno 0 i dwie 1.
    //
    // PUŁAPKA: nie zakładaj, że w tablicy zawsze są dokładne liczby wierszy. Sterownik JDBC może zwrócić
    //   Statement.SUCCESS_NO_INFO (-2) zamiast liczby; H2 podaje liczby, ale inne bazy bywają
    //   mniej szczere. Dlaczego? To decyzja sterownika, nie Springa. Gdy ważna jest dokładna liczba wierszy,
    //   sprawdzaj stan bazy zapytaniem albo nie polegaj na wartościach z paczki. Sprawdzone testem (sekcja 5).

    // =================================================================================================
    // 6. HIERARCHIA WYJĄTKÓW DataAccessException — tłumaczenie błędów SQL
    // =================================================================================================
    //
    // Czysty JDBC rzuca jeden kontrolowany SQLException, z którego trzeba ręcznie odczytywać kod błędu bazy.
    // JdbcTemplate i JdbcClient SAME zamieniają go na niekontrolowany (RuntimeException) podtyp DataAccessException,
    // niezależny od bazy danych:
    //
    //   DataAccessException
    //    ├─ NonTransientDataAccessException           (powtórzenie TEGO SAMEGO polecenia nic nie da)
    //    │   ├─ DataIntegrityViolationException       (naruszone ograniczenie: NOT NULL, klucz obcy, UNIQUE...)
    //    │   │   └─ DuplicateKeyException             (naruszona unikalność, np. drugi raz ten sam numer zamówienia)
    //    │   ├─ DataRetrievalFailureException
    //    │   │   └─ IncorrectResultSizeDataAccessException
    //    │   │       └─ EmptyResultDataAccessException
    //    │   └─ InvalidDataAccessResourceUsageException
    //    │       └─ BadSqlGrammarException            (błąd w SQL: zła nazwa tabeli, literówka)
    //    └─ TransientDataAccessException              (błąd przejściowy — ponowienie może pomóc, np. timeout, deadlock)
    //
    // Zalety: kod nie zależy od H2 ani PostgreSQL; łapiesz konkretny wyjątek (DuplicateKeyException) zamiast
    // sprawdzać liczbowy kod błędu; jeden wspólny pień ułatwia obsługę błędów warstwy danych.
    // Test sekcji 6 sprawdza te typy na prawdziwych błędach H2.
    //
    // PUŁAPKA: kolejność catch. DuplicateKeyException jest PODKLASĄ DataIntegrityViolationException, więc
    //   catch (DataIntegrityViolationException e) postawiony PRZED catch (DuplicateKeyException e)
    //   połknie oba przypadki, a drugi blok nigdy się nie wykona (kompilator zgłosi tu błąd: „already been caught”).
    //   Dlaczego? Java dopasowuje blok catch od góry; szczegółowy wyjątek musi stać przed ogólnym.
    //
    // PUŁAPKA: nie porównuj TEKSTU komunikatu bazy. Komunikaty H2 zależą od języka systemu (u autora po polsku:
    //   „Naruszenie ograniczenia Klucza Głównego lub Indeksu Unikalnego”), a w innej bazie wyglądają inaczej.
    //   Dlaczego? Tekst to treść dla człowieka, nie kontrakt. Sprawdzaj TYP wyjątku.
    //
    // O adnotacji @Repository (lekcja Ioc03Stereotypes): JdbcTemplate i JdbcClient tłumaczą wyjątki same, więc do
    // samego JDBC adnotacja @Repository nie jest potrzebna do tłumaczenia. Jest ona potrzebna tam, gdzie
    // tłumaczenie robi dopiero „postprocesor” (np. kod oparty na JPA w dziale o JPA).

    // =================================================================================================
    // 7. JdbcTemplate KONTRA JdbcClient — co wybrać i jak współpracują
    // =================================================================================================
    //
    //   • Nowy kod: JdbcClient (płynne API, parametry nazwane i pozycyjne w jednym, rekordy).
    //   • Stary kod, tutoriale, batchUpdate, zaawansowane callbacki: JdbcTemplate / NamedParameterJdbcTemplate.
    //   • Oba korzystają z TEGO SAMEGO DataSource i tej samej transakcji: zmiana zrobiona JdbcTemplate jest od razu
    //     widoczna dla JdbcClient w tej samej transakcji. Można je mieszać w jednej klasie (test sekcji 7).
    //   • JdbcClient nie ma na razie odpowiednika batchUpdate — do paczek bierzesz szablon.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • JdbcTemplate: query(sql, rowMapper, parametry...), queryForObject(sql, typ, parametry...),
     *     queryForList(sql, typ, parametry...), update(sql, parametry...), batchUpdate(sql, listaTablic).
     *   • NamedParameterJdbcTemplate: :nazwa + MapSqlParameterSource / BeanPropertySqlParameterSource.
     *   • RowMapper: własny (lambda), DataClassRowMapper (rekordy), BeanPropertyRowMapper (JavaBean z setterami).
     *   • queryForObject: brak wiersza = EmptyResultDataAccessException; NULL w kolumnie = zwrócony null.
     *   • batchUpdate zwraca int[] (liczba wierszy na zestaw; sterownik może dać -2 = SUCCESS_NO_INFO).
     *   • Wyjątki: DataAccessException (niekontrolowany); DuplicateKeyException to podklasa DataIntegrityViolationException;
     *     w catch najpierw szczegółowy, potem ogólny. Nie porównuj tekstu komunikatu bazy.
     *   • Nowy kod: JdbcClient; stary kod i paczki: JdbcTemplate. Obie ścieżki dzielą DataSource i transakcję.
     *
     * PYTANIA KONTROLNE:
     *   1. Co robi za Ciebie JdbcTemplate (wymień trzy rzeczy) i co musisz podać sam?
     *   2. Co się stanie? queryForObject("SELECT * FROM customer WHERE id = ?", mapper, 99) — klienta 99 nie ma.
     *   3. Co się stanie? queryForObject("SELECT email FROM customer WHERE id = ?", String.class, 2) —
     *      Maria Nowak nie ma adresu e-mail.
     *   4. ZNAJDŹ BŁĄD: catch (DataIntegrityViolationException e) {... catch (DuplicateKeyException e) {...}}.
     *   5. Czym różni się DataClassRowMapper od BeanPropertyRowMapper i dla którego rekord Javy jest naturalnym wyborem?
     *   6. Dlaczego nie należy sprawdzać wyjątków SQL po tekście komunikatu bazy?
     *   7. ZNAJDŹ BŁĄD: ktoś woła jdbcTemplate.batchUpdate(...) i zakłada, że element tablicy równy 1 zawsze
     *      oznacza „zmieniono wiersz”. Co może pójść nie tak?
     *   8. Który wyjątek poleci przy wstawieniu zamówienia dla nieistniejącego klienta (klucz obcy)? Czy to ten sam
     *      typ co przy powtórzonym numerze zamówienia?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc02JdbcTemplateExercisesTest (tabele product, customer, shop_order z db/jdbc02)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć liczbę produktów w podanej kategorii.
     * Podpowiedź: queryForObject("SELECT COUNT(*) ...", Integer.class, kategoria).
     */
    static int exercise1(JdbcTemplate jdbcTemplate, String category) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć klienta o podanym id jako Optional; nieistniejący klient → pusty Optional,
     * BEZ wyjątku. Podpowiedź: query(sql, mapper, id) zwraca listę —
     * zamień ją na Optional.
     */
    static Optional<Customer> exercise2(JdbcTemplate jdbcTemplate, long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): ustaw stany magazynowe z mapy SKU → nowy stan JEDNĄ paczką (batch) z nazwanymi
     * parametrami i zwróć sumę zmienionych wierszy. SKU, których nie ma w bazie, po prostu nic nie zmieniają.
     * Podpowiedź: SqlParameterSource[] z MapSqlParameterSource, namedTemplate.batchUpdate(sql, tablica),
     * potem zsumuj elementy wyniku.
     */
    static int exercise3(NamedParameterJdbcTemplate namedTemplate, Map<String, Integer> newStocks) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): wstaw zamówienie (tabela shop_order, status NOWE) i zwróć tekst:
     * "OK", gdy się udało; "DUPLIKAT", gdy numer zamówienia już istnieje; "BRAK_KLIENTA", gdy klient
     * o podanym id nie istnieje (klucz obcy). Podpowiedź: dwa bloki catch we właściwej KOLEJNOŚCI
     * (DuplicateKeyException przed DataIntegrityViolationException).
     */
    static String exercise4(JdbcTemplate jdbcTemplate, String number, long customerId) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bierze połączenie z puli, przygotowuje i wykonuje PreparedStatement, zamyka zasoby, tłumaczy wyjątki SQL.
     *      Ty podajesz SQL, parametry i (dla zapytań) RowMapper.
     *   2. EmptyResultDataAccessException: „Incorrect result size: expected 1, actual 0”. Sprawdzone testem (sekcja 3).
     *   3. Zwróci null — wiersz istnieje, tylko wartość jest pusta; wyjątku nie ma. Sprawdzone testem (sekcja 3).
     *   4. DuplicateKeyException jest podklasą DataIntegrityViolationException, więc pierwszy blok łapie oba, a drugi
     *      jest nieosiągalny (kompilator zgłasza błąd). Zamień kolejność: najpierw DuplicateKeyException.
     *   5. DataClassRowMapper mapuje na rekord albo klasę z konstruktorem (bez setterów, więc obiekt niezmienny);
     *      BeanPropertyRowMapper wymaga pustego konstruktora i setterów (klasa zmienna). Dla rekordu — DataClassRowMapper.
     *   6. Komunikat zależy od bazy i języka systemu (H2 u autora mówi po polsku); to treść dla człowieka, nie kontrakt.
     *      Sprawdzaj typ wyjątku.
     *   7. Sterownik może zwrócić -2 (SUCCESS_NO_INFO): polecenie się powiodło, ale liczba wierszy jest nieznana.
     *      Sprawdzenie „== 1” uznałoby wtedy sukces za porażkę.
     *   8. DataIntegrityViolationException (naruszenie klucza obcego); powtórzony numer to jego podklasa
     *      DuplicateKeyException — typ inny, ale z tej samej rodziny. Sprawdzone testem (sekcja 6).
     */
    // </editor-fold>
}

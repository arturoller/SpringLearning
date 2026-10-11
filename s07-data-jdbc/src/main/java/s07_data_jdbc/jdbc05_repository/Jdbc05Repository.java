package s07_data_jdbc.jdbc05_repository;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.Optional;

/**
 * <pre>
 * TEMAT: Repozytorium na JdbcClient — klasa, która chowa SQL przed resztą aplikacji
 *        (repository = repozytorium, klasa dostępu do danych; DAO = Data Access Object, obiekt dostępu do danych;
 *         generated key = klucz generowany przez bazę; DataAccessException = wyjątek dostępu do danych;
 *         slice test = test "wycinka" aplikacji, np. tylko warstwa bazy)
 *
 * W SKRÓCIE:
 *   Reszta aplikacji (serwisy, kontrolery) nie powinna znać SQL. Całe SQL lądują w jednej klasie na tabelę
 *   — repozytorium (wzorzec DAO z t29_jdbc_databases/Jdbc05Dao, tym razem ze Springiem). Repozytorium ma metody
 *   o biznesowych nazwach: insert, findById, findAll, updatePrice, deleteById; w środku używa JdbcClient.
 *   Błędy bazy wychodzą jako niesprawdzane wyjątki z hierarchii DataAccessException, a brak wyniku — jako Optional.
 *
 * ANALOGIA: magazynier w hurtowni.
 *   Sprzedawca (serwis) mówi: "daj mi produkt numer 5" albo "przyjmij nowy towar". Nie wie, na którym regale leży
 *   towar ani jak działa system magazynowy (SQL). Magazynier (repozytorium) zna regały i jest jedynym, kto do nich
 *   zagląda. Gdy zmienisz system magazynowy, zmieniasz pracę magazyniera, a nie sprzedawców.
 *
 * JAK TO DZIAŁA:
 *   1. ProductRepository to bean (@Repository), dostaje JdbcClient przez konstruktor.
 *   2. insert: jdbcClient.sql("INSERT ...").param(...).update(keyHolder, "id") — baza nadaje id (AUTO_INCREMENT),
 *      a Spring odczytuje je z JDBC (getGeneratedKeys) i wkłada do KeyHolder; stąd getKeyAs(Long.class).
 *   3. Błąd bazy (np. naruszenie UNIQUE) → SQLException → JdbcClient tłumaczy go na wyjątek Springa (DuplicateKeyException).
 *   4. W teście @JdbcTest startuje TYLKO warstwę JDBC (DataSource, JdbcClient, transakcje), więc repozytorium
 *      dołączamy ręcznie przez @Import, a każdy test działa w transakcji cofanej na końcu (rollback).
 *
 * SŁÓWKA:
 *   repository = repozytorium; key holder = pojemnik na klucz; generated key = klucz generowany; unique = unikalny;
 *   constraint violation = naruszenie ograniczenia; duplicate key = zdublowany klucz; not found = nie znaleziono;
 *   slice = wycinek; import = dołączenie; rollback = wycofanie zmian; translate = tłumaczyć; unchecked = niesprawdzany.
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc05Dao (to samo w czystym JDBC), s07_data_jdbc/Jdbc01JdbcClient,
 *             s08_data_jpa/Jpa02Repositories (Spring Data JPA generuje takie repozytoria za Ciebie),
 *             s10_transactions/Tx01Transactional (transakcje w serwisach).
 * </pre>
 */
public final class Jdbc05Repository {

    private Jdbc05Repository() {
    }

    // =================================================================================================
    // 1. Repozytorium: SQL w jednym miejscu (ProductRepository)
    // =================================================================================================
    //
    // ProductRepository ma @Repository, jedno pole final JdbcClient i konstruktor — to wszystko. Metody zwracają typy
    // domenowe (Product, Optional, List), nie ResultSet ani Map. Serwis wywołuje repository.findById(5) i nie wie,
    // że pod spodem jest SQL.
    //
    // DOBRA PRAKTYKA: jedno repozytorium na tabelę (agregat), z metodami o biznesowych nazwach; parametry zawsze
    //   przez .param(...). Dlaczego? Zmiana SQL (nowa kolumna, inny indeks) dotyka jednego pliku, a nie kilkudziesięciu
    //   miejsc w aplikacji; i łatwo testować repozytorium osobno.
    // PUŁAPKA: SELECT bez ORDER BY i zakładanie kolejności wyników. Dlaczego? Baza nie gwarantuje kolejności bez ORDER BY;
    //   test przechodzi "przypadkiem", a po zmianie planu zapytania (albo na innej bazie) — nie.

    // =================================================================================================
    // 2. Klucz generowany (KeyHolder)
    // =================================================================================================
    //
    //   KeyHolder keyHolder = new GeneratedKeyHolder();
    //   jdbcClient.sql("INSERT ...").param(...).update(keyHolder, "id");
    //   long id = keyHolder.getKeyAs(Long.class);
    // Po INSERT-cie baza zna id, którego nie podałeś (AUTO_INCREMENT). update(KeyHolder, String...) prosi sterownik JDBC
    // o zwrócenie wskazanej kolumny ("id"). Dlaczego podajemy nazwę kolumny? Bez niej różne bazy zwracają różne
    // zestawy kolumn; nazwa mówi wprost, który klucz nas interesuje. Dwa wstawienia dają dwa różne, rosnące id
    // (test sekcji 2); NIE zakładaj konkretnych liczb (np. że pierwszy będzie 1) — liczniki nie cofają się przy rollbacku.
    //
    // PUŁAPKA: po INSERT-cie robić SELECT MAX(id). Dlaczego? Przy dwóch jednoczesnych wstawieniach dostaniesz cudze id —
    //   klucz zwrócony przez KeyHolder jest przypisany do TWOJEGO wstawienia.

    // =================================================================================================
    // 3. Brak wyniku: Optional albo wyjątek domenowy
    // =================================================================================================
    //
    //   findById(long)  → Optional<Product>   "może go nie być" (wyszukiwanie)
    //   getById(long)   → Product             "musi być, inaczej ProductNotFoundException" (np. przed zmianą)
    // Obie metody używają .query(...).optional(): brak wiersza to Optional.empty(), nie wyjątek JDBC.
    // update()/delete() zwracają liczbę zmienionych wierszy: porównanie z 1 mówi, czy produkt istniał.
    //
    // PUŁAPKA: single() w findById. Dlaczego? Brak wiersza daje EmptyResultDataAccessException (wyjątek techniczny),
    //   a wołający chce usłyszeć "nie ma takiego produktu" w języku domeny.

    // =================================================================================================
    // 4. Hierarchia DataAccessException
    // =================================================================================================
    //
    // Każda baza zgłasza błędy po swojemu (kody SQLState). Spring tłumaczy je na wspólną hierarchię NIESPRAWDZANYCH
    // wyjątków DataAccessException (spring-tx), więc kod nie zależy od bazy:
    //
    //   DataAccessException
    //     └─ NonTransientDataAccessException                  błąd trwały: ponawianie tego samego nic nie da
    //          ├─ InvalidDataAccessResourceUsageException     zły sposób użycia zasobu
    //          │    └─ BadSqlGrammarException                 błąd w SQL (literówka, brak tabeli)
    //          └─ DataIntegrityViolationException             naruszenie więzów (NOT NULL, UNIQUE, klucz obcy)
    //               └─ DuplicateKeyException                  dokładnie: zdublowany klucz (UNIQUE / PRIMARY KEY)
    //
    // (to wycinek hierarchii — są też wyjątki przejściowe, TransientDataAccessException, gdzie ponowienie może pomóc).
    // Test sekcji 4: zdublowana nazwa produktu → DuplicateKeyException, brak nazwy (NULL) → DataIntegrityViolationException,
    // literówka w SQL → BadSqlGrammarException, a wszystkie to RuntimeException (nie trzeba ich deklarować ani łapać).
    // Łap najwęższy wyjątek, który potrafisz obsłużyć (DuplicateKeyException → "nazwa zajęta"), resztę puść wyżej.
    //
    // PUŁAPKA: catch (Exception e) wokół wywołania repozytorium i "ciche" połknięcie. Dlaczego? Zgubisz błąd techniczny
    //   (np. awaria bazy) i udasz, że wszystko jest w porządku; łap konkretny wyjątek i tylko tam, gdzie wiesz, co zrobić.
    // PYTANIE REKRUTACYJNE: "Dlaczego Spring zamienia SQLException na DataAccessException?" — SQLException jest sprawdzany
    //   i jeden na wszystko, więc trzeba go łapać wszędzie, a przyczynę rozpoznawać po kodach zależnych od bazy;
    //   hierarchia Springa ma osobne typy na osobne przyczyny, jest niezależna od bazy i niesprawdzana.

    // =================================================================================================
    // 5. Test warstwy bazy: @JdbcTest + @Import + @Sql
    // =================================================================================================
    //
    //   @JdbcTest                       wycinek aplikacji: DataSource, JdbcClient, JdbcTemplate, transakcje (bez @Component-ów)
    //   @ActiveProfiles("jdbc05")       profil z application-jdbc05.properties (tabela products ze skryptu schematu)
    //   @Import(ProductRepository.class) dołącza nasz bean; sam @JdbcTest NIE skanuje @Repository (sprawdzone
    //                                    uruchomieniem: 0 beanów ProductRepository bez @Import)
    //   @Sql("/db/jdbc05/two-products.sql")  skrypt z danymi tylko na JEDEN test (klasa lub metoda)
    //
    // Pakiet testowy w Boot 4: org.springframework.boot.jdbc.test.autoconfigure.JdbcTest (w Boot 3 był
    // org.springframework.boot.test.autoconfigure.jdbc.JdbcTest) — import ze starych tutoriali się nie skompiluje.
    //
    // @JdbcTest jest @Transactional: każdy test biegnie w transakcji, którą Spring na końcu COFA. @Sql wykonuje się w tej
    // samej transakcji, więc dane z @Sql też znikają. Dzięki temu testy są od siebie niezależne i kolejność nie ma
    // znaczenia (test sekcji 5: jeden test dopisuje produkt, drugi widzi tylko swoje dwa).
    // Domyślnie @JdbcTest podmienia DataSource na wbudowaną bazę (AutoConfigureTestDatabase) — tu i tak mamy H2.
    //
    // PUŁAPKA: testy, które zostawiają dane w bazie (brak rollbacku). Dlaczego? Kolejny test zobaczy cudze dane i
    //   przejdzie lub padnie zależnie od kolejności uruchomienia - błąd, który znika po ponownym uruchomieniu.
    // DOBRA PRAKTYKA: dane na test przez @Sql w transakcji testu. Dlaczego? Każdy test deklaruje swój stan wyjściowy
    //   w jednym miejscu i nie psuje innych.
    //
    // Transakcje poza testami (@Transactional w serwisie, propagacja) omawia dział s10_transactions
    // (s10_transactions/Tx01Transactional); tu wystarczy wiedzieć, że repozytorium samo ich nie otwiera.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Repozytorium = jedna klasa na tabelę, @Repository, JdbcClient w konstruktorze, SQL tylko tu.
     *   • Klucz generowany: update(keyHolder, "id") + keyHolder.getKeyAs(Long.class); nie robić SELECT MAX(id).
     *   • Brak wyniku: query(...).optional() → Optional; wyjątek domenowy (ProductNotFoundException) w getById.
     *   • update()/delete() zwracają liczbę wierszy: == 1 znaczy, że rekord istniał.
     *   • Błędy: DataAccessException (niesprawdzane): DuplicateKeyException (UNIQUE), DataIntegrityViolationException
     *     (NOT NULL, FK), BadSqlGrammarException (zły SQL). Łap najwęższy możliwy.
     *   • Test: @JdbcTest (Boot 4: org.springframework.boot.jdbc.test.autoconfigure) + @Import(Repo.class) + @Sql;
     *     rollback po każdym teście.
     *   • Transakcje w serwisach: dział s10_transactions.
     *
     * PYTANIA KONTROLNE:
     *   1. Po co w repozytorium update(keyHolder, "id") zamiast SELECT MAX(id) po wstawieniu?
     *   2. Co się stanie? Wstawisz dwa produkty o tej samej nazwie do tabeli z UNIQUE na kolumnie name.
     *   3. Co się stanie? Wstawisz produkt z name = null do kolumny NOT NULL.
     *   4. ZNAJDŹ BŁĄD: test z @JdbcTest wstrzykuje ProductRepository przez @Autowired, ale klasa testu nie ma @Import.
     *   5. Czym różni się findById od getById i kiedy użyjesz której?
     *   6. Co się stanie? Test z @JdbcTest dopisuje produkt do bazy i kończy się. Czy następny test go zobaczy?
     *   7. Co się stanie? updatePrice(999, 100) dla id, którego nie ma w tabeli.
     *   8. Dlaczego DuplicateKeyException jest wyjątkiem niesprawdzanym i czym jest względem DataIntegrityViolationException?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc05RepositoryExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć, czy produkt o podanej nazwie istnieje. Podpowiedź:
     * SELECT COUNT(*) FROM products WHERE name = :name i {@code query(Integer.class).single()}; porównaj z zerem.
     */
    static boolean exercise1(JdbcClient jdbcClient, String name) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): wstaw produkt i zwróć id nadane przez bazę (klucz generowany).
     * Podpowiedź: new GeneratedKeyHolder(), update(keyHolder, "id"), keyHolder.getKeyAs(Long.class).
     */
    static long exercise2(JdbcClient jdbcClient, String name, int priceCents) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): spróbuj wstawić produkt. Zwróć true, gdy się udało, a false, gdy nazwa jest już zajęta
     * (naruszenie UNIQUE). Inne błędy mają NIE być połknięte. Podpowiedź: złap dokładnie DuplicateKeyException.
     */
    static boolean exercise3(JdbcClient jdbcClient, String name, int priceCents) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. KeyHolder zwraca klucz TWOJEGO wstawienia. SELECT MAX(id) przy dwóch jednoczesnych wstawieniach może zwrócić
     *      cudze id (wyścig, race condition), a dodatkowo to drugie zapytanie.
     *   2. Drugie wstawienie rzuci DuplicateKeyException (podklasa DataIntegrityViolationException). Sprawdzone testem 4.
     *   3. DataIntegrityViolationException (naruszenie NOT NULL; to NIE jest DuplicateKeyException). Sprawdzone testem 4.
     *   4. @JdbcTest nie skanuje @Repository/@Component, więc bean ProductRepository nie istnieje i kontekst testu nie
     *      wystartuje (brak beana do wstrzyknięcia). Dodaj @Import(ProductRepository.class). Sprawdzone uruchomieniem
     *      (0 beanów tego typu bez @Import).
     *   5. findById zwraca Optional (brak to normalna sytuacja — wyszukiwanie), getById rzuca ProductNotFoundException
     *      (produkt musi istnieć, np. przed zmianą). Sprawdzone testem 3.
     *   6. Nie. @JdbcTest jest @Transactional i cofa transakcję po każdym teście. Sprawdzone testem 5.
     *   7. Zwróci false i NIE rzuci wyjątku: UPDATE zmienił 0 wierszy. Sprawdzone testem 3.
     *   8. Bo Spring chce, by błędy bazy nie zaśmiecały sygnatur metod i nie zależały od JDBC. DuplicateKeyException to
     *      węższy przypadek DataIntegrityViolationException (naruszenie unikalności, a nie dowolnych więzów).
     *      Sprawdzone testem 4 (isInstanceOf).
     */
    // </editor-fold>
}

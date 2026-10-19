package s07_data_jdbc.jdbc03_sql_init;

/**
 * <pre>
 * TEMAT: Inicjalizacja bazy skryptami SQL — schema.sql i data.sql
 *        (initialization = inicjalizacja, przygotowanie bazy przy starcie; schema = schemat, struktura tabel;
 *         script = skrypt; mode = tryb; platform = platforma; embedded = wbudowana, działająca w procesie aplikacji)
 *
 * W SKRÓCIE:
 *   Przy starcie aplikacji Spring Boot potrafi sam wykonać skrypty SQL: najpierw schema.sql (CREATE TABLE ...),
 *   potem data.sql (INSERT ...). To najprostszy sposób, żeby baza w pamięci (H2) miała tabele i dane — idealny
 *   do nauki, prototypów i testów. Ma jednak poważne ograniczenia (skrypt biegnie od nowa przy każdym starcie,
 *   nie ma historii zmian), dlatego w prawdziwych projektach zastępuje go Flyway (lekcja Jdbc04Flyway).
 *
 * ANALOGIA: otwarcie sklepu rano.
 *   Schema.sql to ustawienie regałów (struktura), data.sql to wyłożenie towaru na półki (dane). Gdy sklep jest
 *   nowy i pusty (baza w pamięci), robisz to codziennie od zera i nic się nie dzieje. Gdy sklep już stoi i ma
 *   towar (baza trwała), „ustaw regały” drugi raz oznacza chaos: regały już są, a towar leży podwójnie.
 *
 * JAK TO DZIAŁA:
 *   1. Autokonfiguracja tworzy bean (ziarno) dataSourceScriptDatabaseInitializer — wykonawcę skryptów.
 *   2. Po utworzeniu DataSource, a PRZED beanami korzystającymi z bazy (np. JdbcClient), wykonuje:
 *      najpierw wszystkie skrypty schematu, potem wszystkie skrypty danych.
 *   3. Gdzie szuka: domyślnie schema.sql i data.sql w korzeniu classpath (oraz schema-PLATFORMA.sql / data-PLATFORMA.sql).
 *      W tej lekcji ustawiamy własne miejsca: spring.sql.init.schema-locations / data-locations
 *      (application-jdbc03.properties), bo wspólny korzeń byłby wspólny dla WSZYSTKICH lekcji modułu.
 *   4. Czy w ogóle wykonać, rozstrzyga spring.sql.init.mode: embedded (domyślnie), always, never.
 *
 * SŁÓWKA:
 *   schema = schemat; data = dane; mode = tryb; always = zawsze; never = nigdy; embedded = wbudowana;
 *   platform = platforma (rodzaj bazy: h2, postgresql...); continue = kontynuować; idempotent = idempotentny
 *   (można wykonać wiele razy z tym samym skutkiem); DDL = polecenia definiujące strukturę (CREATE, ALTER);
 *   DML = polecenia zmieniające dane (INSERT, UPDATE, DELETE); history = historia.
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc04Flyway (to, co zastępuje schema.sql w prawdziwych projektach),
 *             s07_data_jdbc/Jdbc01JdbcClient (zapytania do tak przygotowanej bazy),
 *             s03_boot_basics/Boot01AutoConfiguration (skąd się biorą beany „same z siebie”),
 *             s02_configuration/Cfg03Profiles (profil lekcji z plikiem application-jdbc03.properties).
 * </pre>
 */
public final class Jdbc03SqlInit {

    private Jdbc03SqlInit() {
    }

    // =================================================================================================
    // 1. schema.sql + data.sql — co robi Boot przy starcie
    // =================================================================================================
    //
    //   db/jdbc03/schema.sql :  CREATE TABLE employee (...)                 ← struktura (DDL)
    //   db/jdbc03/data.sql   :  INSERT INTO employee VALUES (...), ...      ← dane (DML)
    //
    // Aplikacja startuje i bez ani jednej linii Javy ma tabelę z 7 pracownikami. Test sekcji 1.
    // Pod spodem to klasa ScriptUtils ze Springa JDBC: dzieli plik na instrukcje po średniku i wykonuje po kolei.
    //
    // DOBRA PRAKTYKA: osobne pliki dla struktury (schema) i danych (data). Dlaczego? Boot gwarantuje kolejność
    //   „wszystkie schematy, potem wszystkie dane”, więc dane zawsze trafiają do istniejących już tabel,
    //   a Ty widzisz od razu, co jest strukturą, a co przykładową zawartością.

    // =================================================================================================
    // 2. KOLEJNOŚĆ SKRYPTÓW
    // =================================================================================================
    //
    //   schema-locations=schema.sql, schema-views.sql   → najpierw tabela, potem widok na tabeli (działa)
    //   schema-locations=schema-views.sql, schema.sql   → widok przed tabelą (startup się wysypuje)
    //
    // Skrypty z jednej listy wykonywane są w kolejności wypisania. Gdy kolejność jest zła, aplikacja nie startuje:
    // w łańcuchu przyczyn jest ScriptStatementFailedException: „Failed to execute SQL script statement #1 of class
    // path resource [db/jdbc03/schema-views.sql]: CREATE VIEW ...”, a na samym dole błąd bazy „Table "EMPLOYEE" not found”.
    // Test sekcji 2.
    //
    // PUŁAPKA: błąd skryptu przy starcie widać dopiero w ŚRODKU długiego łańcucha wyjątków
    //   (UnsatisfiedDependencyException → BeanCreationException → ScriptStatementFailedException → SQLException).
    //   Dlaczego tak? Skrypt wykonuje się podczas tworzenia beana, więc błąd jest opakowywany po drodze.
    //   Nie zatrzymuj się na pierwszej linii stosu — przewiń do „Caused by:” na samym dole.

    // =================================================================================================
    // 3. spring.sql.init.mode — czy w ogóle wykonywać skrypty
    // =================================================================================================
    //
    //   embedded (domyślnie)  tylko dla bazy WBUDOWANEJ (m.in. H2 w pamięci)
    //   always                zawsze, także dla bazy zewnętrznej (PostgreSQL, MySQL...)
    //   never                 nigdy
    //
    // Domyślne „embedded” to zabezpieczenie: skrypty nie dotkną przypadkiem prawdziwej bazy produkcyjnej.
    // Szczegół: H2 jest „wbudowana” dla Boota tylko wtedy, gdy działa w PAMIĘCI (adres jdbc:h2:mem:...).
    // Test sekcji 3 sprawdza to na H2 w pliku (jdbc:h2:file:...): bez zmian trybu skrypty się NIE wykonują,
    // z always — wykonują się. Przy never baza pozostaje pusta (BadSqlGrammarException przy zapytaniu).
    //
    // PUŁAPKA: przenosisz projekt z H2 w pamięci na bazę w pliku lub PostgreSQL i nagle „znikają” tabele.
    //   Dlaczego? Zmieniła się baza z „wbudowanej” na „zewnętrzną”, a domyślny tryb to embedded — skrypty
    //   po cichu przestały się wykonywać. Gdy to nie pomyłka, a potrzeba — ustaw jawnie spring.sql.init.mode=always.
    //
    // PUŁAPKA: spring.sql.init.mode=always na bazie produkcyjnej bez przemyślenia. Dlaczego? Skrypty wykonają się
    //   przy KAŻDYM starcie aplikacji; zły skrypt (np. DELETE) skasuje prawdziwe dane. Do produkcji służą migracje.

    // =================================================================================================
    // 4. BŁĘDY W SKRYPCIE I continue-on-error
    // =================================================================================================
    //
    //   spring.sql.init.continue-on-error=false (domyślnie)   błąd instrukcji = aplikacja nie startuje
    //   spring.sql.init.continue-on-error=true                błąd jest zapisany w logu, wykonywanie trwa dalej
    //
    // Test sekcji 4: skrypt data-with-error.sql ma trzy INSERT-y, a środkowy odwołuje się do nieistniejącej tabeli.
    // Domyślnie startup pada (ScriptStatementFailedException: „... statement #2 ... INSERT INTO nie_istnieje ...”).
    // Z continue-on-error=true powstają dwa wiersze (pierwszy i trzeci INSERT), a błąd ląduje tylko w logu.
    //
    // PUŁAPKA: continue-on-error=true „na wszelki wypadek”. Dlaczego? Błędy znikają w logu, a aplikacja rusza z
    //   niekompletną bazą — usterkę widzisz dopiero po godzinach, jako brakujące dane. Domyślne „fail fast” (szybkie
    //   zatrzymanie przy błędzie) jest zdrowsze. Używaj tej opcji tylko świadomie (np. skrypt celowo ignoruje „tabela już istnieje”).

    // =================================================================================================
    // 5. PLIKI DLA PLATFORMY (schema-h2.sql, schema-postgresql.sql, ...)
    // =================================================================================================
    //
    //   spring.sql.init.platform=NAZWA   (domyślnie "all")
    //
    // Gdy NIE podajesz własnych schema-locations, Boot czyta w korzeniu classpath: schema.sql oraz
    // schema-NAZWA.sql (i analogicznie data.sql oraz data-NAZWA.sql). To pozwala trzymać SQL wspólny dla wszystkich baz
    // w schema.sql, a różnice składni (np. typ SERIAL w PostgreSQL) w pliku danej platformy.
    // Boot NIE zgaduje platformy z adresu bazy — musisz ją ustawić sam. W tej lekcji używamy nazwy "jdbc03platform"
    // i pliku schema-jdbc03platform.sql (w src/main/resources), żeby nie dotknąć innych lekcji. Test sekcji 5:
    // z platform=jdbc03platform powstaje tabela platform_marker, bez niej — nie.
    // Uwaga: własne schema-locations (jak w tej lekcji) WYŁĄCZAJĄ wyszukiwanie plików platformowych.

    // =================================================================================================
    // 6. DLACZEGO TO NIE WYSTARCZA W PRODUKCJI
    // =================================================================================================
    //
    // Skrypty biegną OD NOWA przy każdym starcie. Test sekcji 6 symuluje restart aplikacji na tej samej bazie:
    //   • schema.sql z CREATE TABLE employee → drugi start pada: Table "EMPLOYEE" already exists;
    //   • schema z CREATE TABLE IF NOT EXISTS startuje, ale data.sql wstawia pracowników DRUGI raz — w bazie 14
    //     wierszy zamiast 7 (nic nie broni przed duplikatami, bo brak unikalnego klucza);
    //   • skrypt nie wie, która wersja schematu jest w bazie. Dodanie kolumny to nowy ALTER TABLE, ale
    //     jak sprawdzić, że baza produkcyjna go już dostała, a testowa jeszcze nie? Nie da się;
    //   • brak historii („kto, kiedy, co zmienił”) i brak sumy kontrolnej (nikt nie wykryje, że ktoś ręcznie
    //     przerobił już wykonany skrypt).
    // Wniosek: schema.sql/data.sql są świetne do baz wbudowanych i testów, a do baz z prawdziwymi danymi używa się
    // narzędzia migracji z historią zmian — Flyway (lekcja Jdbc04Flyway).
    //
    // DOBRA PRAKTYKA: schema.sql tylko dla baz w pamięci (nauka, szybkie testy). Dla każdej bazy, której dane mają
    //   przetrwać restart — migracje. Dlaczego? Narzędzie migracji pamięta, co już wykonało, i robi tylko nowe kroki.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Boot wykonuje przy starcie schema.sql, potem data.sql; własne miejsca: spring.sql.init.schema-locations /
     *     data-locations (listy po przecinku, wykonywane w podanej kolejności).
     *   • spring.sql.init.mode: embedded (domyślnie: tylko baza w pamięci), always, never.
     *   • Błąd instrukcji zatrzymuje start (domyślnie); continue-on-error=true tylko loguje błąd i jedzie dalej.
     *   • spring.sql.init.platform + schema-NAZWA.sql: pliki dla konkretnej bazy (działają przy domyślnych lokalizacjach).
     *   • Skrypty biegną przy każdym starcie: bez IF NOT EXISTS drugi start pada, a data.sql dubluje dane.
     *   • Prawdziwe projekty: migracje (Flyway) zamiast schema.sql.
     *
     * PYTANIA KONTROLNE:
     *   1. Który skrypt wykonuje się pierwszy: schema.sql czy data.sql? Co by się stało przy odwrotnej kolejności?
     *   2. Co się stanie? Zmieniasz adres bazy z jdbc:h2:mem:sklep na jdbc:h2:file:./sklep, nic poza tym.
     *      Czy schema.sql nadal się wykona?
     *   3. Co się stanie? Aplikacja startuje drugi raz na tej samej bazie, schema.sql zawiera
     *      CREATE TABLE employee (...) bez IF NOT EXISTS.
     *   4. ZNAJDŹ BŁĄD: schema-locations=schema-views.sql, schema.sql — dlaczego start się nie uda?
     *   5. ZNAJDŹ BŁĄD: w schema.sql jest CREATE TABLE IF NOT EXISTS, a w data.sql zwykłe INSERT-y.
     *      Aplikacja z bazą trwałą uruchomiona dziesięć razy — co zawiera tabela?
     *   6. Co robi continue-on-error=true i dlaczego nie warto włączać go „na wszelki wypadek”?
     *   7. Jakie trzy ograniczenia schema.sql sprawiają, że w produkcji wybieramy migracje?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc03SqlInitExercisesTest. Każde ćwiczenie zwraca ustawienia w formie tekstu
    // „klucz=wartość”; test uruchamia Jdbc03Application z profilem jdbc03 i tymi ustawieniami (jako argumenty
    // wiersza poleceń, z dwoma myślnikami z przodu).
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć ustawienia, po których aplikacja NIE wykona żadnego skryptu SQL
     * (tabela employee nie powstanie). Podpowiedź: spring.sql.init.mode.
     */
    static String[] exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć ustawienia, które każą załadować skrypt classpath:db/jdbc03/data-with-error.sql
     * zamiast data.sql i sprawią, że aplikacja MIMO błędu w skrypcie wystartuje (w tabeli będą 2 wiersze).
     * Podpowiedź: spring.sql.init.data-locations i spring.sql.init.continue-on-error.
     */
    static String[] exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć ustawienia, po których oprócz tabeli employee istnieje widok it_employee
     * (skrypt classpath:db/jdbc03/schema-views.sql). Uwaga na KOLEJNOŚĆ skryptów na liście.
     * Podpowiedź: spring.sql.init.schema-locations z dwiema ścieżkami po przecinku.
     */
    static String[] exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): napisz skrypt SQL (tekst), który można wykonać wiele razy z tym samym skutkiem:
     * tworzy tabelę department (id INT PRIMARY KEY, name VARCHAR(50)), jeśli jej nie ma, i wstawia dział
     * (1, 'IT'), jeśli go jeszcze nie ma. Po dwukrotnym wykonaniu w tabeli ma być dokładnie jeden wiersz.
     * Podpowiedź: CREATE TABLE IF NOT EXISTS oraz MERGE INTO ... KEY (id) VALUES (...)
     * (składnia H2) albo INSERT ... SELECT ... WHERE NOT EXISTS (...).
     */
    static String exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Najpierw schema.sql (struktura), potem data.sql. Przy odwrotnej kolejności INSERT-y trafiłyby do tabel,
     *      których jeszcze nie ma, więc start by się nie udał — Boot sam pilnuje kolejności „schematy, potem dane”.
     *      Kolejność skryptów w obrębie jednej listy jest taka, jak ją wypiszesz (test sekcji 2).
     *   2. Nie. H2 w pliku nie jest dla Boota bazą „wbudowaną” (to tylko H2 w pamięci), a domyślny tryb to embedded,
     *      więc skrypty się nie wykonają, a tabeli nie będzie. Naprawa: spring.sql.init.mode=always (test sekcji 3).
     *   3. Start się nie uda: ScriptStatementFailedException z przyczyną „Table "EMPLOYEE" already exists”
     *      (test sekcji 6).
     *   4. Widok jest tworzony PRZED tabelą employee, na której się opiera: „Table "EMPLOYEE" not found”.
     *      Skrypty wykonują się w kolejności wypisania (test sekcji 2).
     *   5. 70 wierszy (dziesięć razy po 7): CREATE TABLE IF NOT EXISTS nie przeszkadza, a INSERT-y dopisują się
     *      za każdym startem; nic nie chroni przed duplikatami (test sekcji 6 pokazuje dwa starty: 14 wierszy).
     *   6. Błąd instrukcji nie zatrzymuje startu, tylko trafia do logu; pozostałe instrukcje się wykonują.
     *      Aplikacja rusza z niekompletną bazą, a błąd zauważysz późno (test sekcji 4: 2 wiersze zamiast 3).
     *   7. Dowolne trzy z: wykonuje się przy każdym starcie (nie zna stanu bazy); brak historii zmian i wersji schematu;
     *      brak sumy kontrolnej; zmiana struktury istniejącej bazy wymaga ręcznego ALTER TABLE bez śladu, kto i kiedy.
     */
    // </editor-fold>
}

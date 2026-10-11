package s07_data_jdbc.jdbc03_sql_init;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

/**
 * <pre>
 * TEMAT: schema.sql i data.sql — skrypty SQL uruchamiane przy starcie aplikacji
 *        (schema = schemat, czyli struktura bazy: tabele i kolumny; data = dane; init = inicjalizacja;
 *         script = skrypt; mode = tryb; location = lokalizacja pliku na classpath)
 *
 * W SKRÓCIE:
 *   Spring Boot potrafi przy starcie wykonać skrypty SQL: najpierw schema.sql (tworzy tabele), potem data.sql
 *   (wstawia dane startowe). Włącza się to i dostraja właściwościami spring.sql.init.*. To najprostszy sposób, żeby
 *   mieć gotową bazę w nauce i w testach. Ale to NIE jest narzędzie do prawdziwych aplikacji — skrypty nie mają
 *   wersji ani pamięci, co już zostało wykonane. Do tego służy Flyway (następna lekcja, Jdbc04Flyway).
 *
 * ANALOGIA: karta przepisu uruchamiana przy każdym otwarciu kuchni.
 *   Kucharz zawsze czyta kartę od początku: "ustaw stoły (schema), wyłóż produkty (data)". W pustej kuchni działa
 *   świetnie. Gdy kuchnia jest już urządzona (baza istnieje), to samo polecenie "ustaw stoły" kończy się kłótnią:
 *   stoły już stoją. Karta nie wie, co już zrobiono.
 *
 * JAK TO DZIAŁA:
 *   Przy starcie kontekstu Boot (DataSourceInitializationAutoConfiguration) tworzy inicjalizator skryptów:
 *     1. sprawdza tryb: spring.sql.init.mode = embedded (DOMYŚLNIE: tylko bazy wbudowane, np. H2 w pamięci),
 *        always (zawsze, także dla PostgreSQL) albo never (nigdy),
 *     2. wykonuje skrypty z spring.sql.init.schema-locations (domyślnie schema.sql z korzenia classpath),
 *     3. POTEM wykonuje skrypty z spring.sql.init.data-locations (domyślnie data.sql z korzenia classpath),
 *     4. pierwszy błąd w skrypcie przerywa start aplikacji (chyba że continue-on-error=true).
 *   W tym module wspólny application.properties wyłącza skrypty (mode=never), a ta lekcja włącza je u siebie
 *   w profilu jdbc03 i wskazuje własne pliki w db/jdbc03 — dzięki temu lekcje nie wykonują cudzych skryptów.
 *
 * SŁÓWKA:
 *   schema = schemat; data = dane; mode = tryb; embedded = wbudowana (baza w procesie aplikacji); always = zawsze;
 *   never = nigdy; continue on error = kontynuuj mimo błędu; location = lokalizacja; classpath = ścieżka klas;
 *   migration = migracja (zmiana struktury bazy w czasie); versioning = wersjonowanie.
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc04Flyway (następca: wersjonowane migracje), s07_data_jdbc/Jdbc01JdbcClient,
 *             t29_jdbc_databases/Jdbc01SqlBasics (CREATE TABLE i INSERT od podstaw).
 * </pre>
 */
public final class Jdbc03SqlInit {

    private Jdbc03SqlInit() {
    }

    // =================================================================================================
    // 1. Kolejność: najpierw schema, potem data
    // =================================================================================================
    //
    // Nasz profil jdbc03 (application-jdbc03.properties):
    //     spring.sql.init.mode=always
    //     spring.sql.init.schema-locations=classpath:db/jdbc03/schema.sql      ← CREATE TABLE categories ...
    //     spring.sql.init.data-locations=classpath:db/jdbc03/data.sql          ← INSERT INTO categories ...
    // Po starcie tabela istnieje i ma dane. Zamiana kolejności nie jest możliwa: data zawsze idzie po schema.
    //
    // PUŁAPKA: data.sql, który wstawia dane do tabeli, której nie utworzył żaden schema.sql. Dlaczego? Aplikacja nie
    //   wystartuje: pierwsze INSERT dostaje błąd "table not found" i cały start się przerywa (ScriptStatementFailedException —
    //   "instrukcja skryptu się nie powiodła"). Sprawdzone testem sekcji 4.

    // =================================================================================================
    // 2. Tryb: spring.sql.init.mode (embedded / always / never)
    // =================================================================================================
    //
    //   embedded (DOMYŚLNY) — skrypty tylko dla baz wbudowanych (H2, HSQLDB, Derby). Dla PostgreSQL nic się nie dzieje.
    //   always              — skrypty zawsze, także dla prawdziwej bazy.
    //   never               — skrypty nigdy.
    // Sprawdzone testem sekcji 2: ten sam skrypt na H2 w pamięci — bez ustawień (embedded) tabela powstaje,
    // z mode=never nie powstaje.
    //
    // Dlaczego domyślnie "embedded"? Baza w pamięci startuje pusta przy każdym uruchomieniu, więc skrypt jest wygodny
    // i bezpieczny. Na prawdziwej bazie z danymi bezmyślne CREATE TABLE przy każdym starcie byłoby groźne.
    //
    // PUŁAPKA: ustawienie mode=always "żeby działało" na bazie produkcyjnej. Dlaczego? Skrypt wykona się przy KAŻDYM
    //   starcie aplikacji: CREATE TABLE padnie ("już istnieje"), a INSERT-y z data.sql mogą zdublować dane lub zepsuć
    //   istniejące.

    // =================================================================================================
    // 3. Własne lokalizacje: schema-locations i data-locations
    // =================================================================================================
    //
    // Domyślnie Boot szuka schema.sql i data.sql w korzeniu classpath (src/main/resources). W module z wieloma lekcjami
    // takie pliki wykonałaby KAŻDA lekcja, więc wskazujemy własne:
    //     spring.sql.init.schema-locations=classpath:db/jdbc03/schema.sql
    // Można podać kilka plików po przecinku — wykonają się w podanej kolejności.
    //
    // DOBRA PRAKTYKA: skrypty w podkatalogu o nazwie funkcji/lekcji i jawne locations, gdy w projekcie jest więcej niż
    //   jeden zestaw skryptów. Dlaczego? Domyślne nazwy "wciągają" skrypt do każdego kontekstu na classpath, także
    //   testowego, co daje zaskakujące błędy ("tabela już istnieje") daleko od ich przyczyny.

    // =================================================================================================
    // 4. Błąd w skrypcie: start aplikacji się przerywa (continue-on-error)
    // =================================================================================================
    //
    // Skrypt db/jdbc03/schema-with-error.sql ma trzy instrukcje: pierwsza i trzecia poprawne, druga celowo zepsuta
    // (zdublowana kolumna). Domyślnie błąd drugiej instrukcji kończy start aplikacji wyjątkiem BeanCreationException
    // z przyczyną ScriptStatementFailedException (a pod spodem błąd bazy H2). Z spring.sql.init.continue-on-error=true
    // błędne instrukcje są pomijane, a reszta się wykonuje — powstają tabele LEGACY_A i LEGACY_B (bez LEGACY_OOPS).
    // Sprawdzone testem sekcji 4.
    //
    // PUŁAPKA: continue-on-error=true "na stałe". Dlaczego? Aplikacja wystartuje z POŁOWICZNĄ bazą i będzie się
    //   sypać dopiero w trakcie działania, daleko od przyczyny; błąd w skrypcie powinien zatrzymać start głośno.
    //   Ta opcja bywa przydatna doraźnie (np. skrypt "posprzątaj, jeśli istnieje").

    // =================================================================================================
    // 5. Dlaczego to dobre do nauki, a złe do produkcji
    // =================================================================================================
    //
    // Skrypty z tej lekcji nie mają WERSJI ani pamięci o wykonaniu. Przy drugim uruchomieniu na tej samej bazie
    // CREATE TABLE kończy się błędem "table already exists" (sprawdzone testem sekcji 5 na bazie, która przeżywa
    // zamknięcie kontekstu). Obejścia typu CREATE TABLE IF NOT EXISTS ratują tylko tworzenie tabel — nie pomogą
    // przy zmianie istniejącej tabeli (nowa kolumna), przy poprawce danych ani w zespole, gdzie każdy ma bazę w innym stanie.
    // Rozwiązanie to narzędzie do migracji: Flyway trzyma w bazie tabelę z historią wykonanych kroków i wykonuje tylko nowe.
    //
    // PUŁAPKA: schema.sql/data.sql jako jedyny mechanizm bazy w aplikacji, która ma żyć latami. Dlaczego? Nie ma historii
    //   zmian, nie ma sposobu na bezpieczne "dodaj kolumnę" na bazie z danymi, a każdy zespół i środowisko dojdzie do
    //   innego stanu bazy. (PYTANIE REKRUTACYJNE: "Czym różni się schema.sql od Flyway?" — brak wersjonowania
    //   i historii wykonanych zmian.)

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Boot wykonuje skrypty przy starcie: najpierw schema (spring.sql.init.schema-locations), potem data.
     *   • Domyślnie: schema.sql i data.sql z korzenia classpath; tryb spring.sql.init.mode = embedded.
     *   • embedded = tylko bazy wbudowane (H2); always = zawsze; never = nigdy.
     *   • Własne pliki: spring.sql.init.schema-locations / data-locations (classpath:db/lekcja/...).
     *   • Błąd w skrypcie przerywa start (ScriptStatementFailedException); continue-on-error=true pomija błędne instrukcje.
     *   • Do nauki i testów świetne; do produkcji nie (brak wersji i historii) → Flyway.
     *   • SqlInitSupport.start(...) (mały kontekst, bez application.properties) pozwala sprawdzić ustawienia bez uruchamiania całej aplikacji.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaka jest DOMYŚLNA wartość spring.sql.init.mode i co to znaczy dla PostgreSQL?
     *   2. Co się stanie? Skonfigurujesz tylko data-locations (INSERT-y), a żaden skrypt nie utworzy tabeli.
     *   3. Co się stanie? Skrypt ma poprawne CREATE TABLE, zepsutą drugą instrukcję i poprawne trzecie CREATE TABLE,
     *      a continue-on-error jest ustawione na true. Które tabele powstaną?
     *   4. Co się stanie? Wystartujesz drugi raz aplikację ze skryptem CREATE TABLE na tej samej, trwałej bazie.
     *   5. ZNAJDŹ BŁĄD: w projekcie jest src/main/resources/schema.sql z CREATE TABLE products, a kilka lekcji
     *      w tym samym module też tworzy tabelę products. Co pójdzie nie tak i jak to naprawić?
     *   6. Dlaczego data.sql nie może wykonać się przed schema.sql?
     *   7. Dlaczego ten mechanizm nie nadaje się do aplikacji produkcyjnej i czym go zastąpić?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc03SqlInitExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): w działającej aplikacji lekcji (dane z data.sql) zwróć posortowane alfabetycznie nazwy
     * wszystkich kategorii z tabeli categories. Podpowiedź: ORDER BY name, {@code query(String.class).list()}.
     */
    static List<String> exercise1(JdbcClient jdbcClient) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): uruchom mały kontekst {@code SqlInitSupport.start(...)} z właściwościami
     * "spring.sql.init.mode=" + tryb oraz "spring.sql.init.schema-locations=classpath:db/jdbc03/schema-simple.sql",
     * a potem zwróć, czy w bazie powstała tabela GADGETS (użyj SqlInitSupport.tableNames(context)).
     * Oczekiwane: embedded → true, always → true, never → false.
     * Podpowiedź: kontekst zamknij w try-with-resources (t10_exceptions/Exceptions04TryWithResources).
     */
    static boolean exercise2(String mode) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): uruchom skrypt db/jdbc03/schema-with-error.sql (ma błąd w środku) tak, żeby kontekst
     * WYSTARTOWAŁ mimo błędu, i zwróć posortowane nazwy powstałych tabel. Oczekiwane: [LEGACY_A, LEGACY_B].
     * Podpowiedź: właściwość spring.sql.init.continue-on-error=true.
     */
    static List<String> exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. embedded: skrypty wykonują się tylko dla baz wbudowanych (H2, HSQLDB, Derby). Dla PostgreSQL nic się nie
     *      wykona, dopóki nie ustawisz mode=always. Sprawdzone w źródłach Boota 4.1.1 (SqlInitializationProperties)
     *      i testem 2.
     *   2. Aplikacja nie wystartuje: pierwsze INSERT kończy się błędem, a start przerywa wyjątek BeanCreationException
     *      z przyczyną ScriptStatementFailedException. Sprawdzone testem 4.
     *   3. Powstaną LEGACY_A i LEGACY_B; zepsuta instrukcja zostanie pominięta. Sprawdzone testem 4.
     *   4. Błąd "table already exists" (JdbcSQLSyntaxErrorException) i przerwany start. Sprawdzone testem 5.
     *   5. Każda lekcja (każdy kontekst) wykona ten sam schema.sql z korzenia classpath, więc przy kontekstach
     *      współdzielących bazę dojdzie do konfliktów, a przy osobnych bazach lekcje dostaną tabele, których nie
     *      potrzebują. Naprawa: skrypty w podkatalogu lekcji i jawne spring.sql.init.schema-locations w jej profilu,
     *      a w module domyślnie mode=never.
     *   6. Bo INSERT potrzebuje istniejącej tabeli; Boot celowo wykonuje najpierw schema-locations, potem data-locations.
     *      Sprawdzone testem 4 (same dane bez schematu → błąd startu).
     *   7. Brak wersji i historii: nie wiadomo, co już wykonano, nie da się bezpiecznie zmienić istniejącej tabeli z danymi,
     *      a środowiska rozjeżdżają się. Zamiennik: narzędzie do migracji, np. Flyway (Jdbc04Flyway).
     */
    // </editor-fold>
}

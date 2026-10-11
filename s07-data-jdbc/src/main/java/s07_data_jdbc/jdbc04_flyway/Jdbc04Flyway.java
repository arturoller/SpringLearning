package s07_data_jdbc.jdbc04_flyway;

import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.util.List;

/**
 * <pre>
 * TEMAT: Flyway — wersjonowane migracje bazy danych
 *        (migration = migracja, czyli skrypt zmieniający strukturę lub dane bazy; version = wersja;
 *         checksum = suma kontrolna pliku; repeatable = powtarzalna; schema history = historia schematu;
 *         validate = sprawdź poprawność; baseline = punkt odniesienia)
 *
 * W SKRÓCIE:
 *   Flyway to narzędzie, które pilnuje wersji Twojej bazy. Zmiany zapisujesz jako kolejne pliki
 *   V1__..., V2__..., V3__... w katalogu db/migration. Przy starcie aplikacji Flyway zagląda do tabeli
 *   flyway_schema_history, sprawdza, które pliki już wykonano, i uruchamia TYLKO nowe — po kolei, raz.
 *   Dzięki temu baza każdego programisty, serwera testowego i produkcji dochodzi do tego samego stanu.
 *   To naturalne rozwinięcie schema.sql z lekcji Jdbc03SqlInit, które nie pamiętało, co już zrobiło.
 *
 * ANALOGIA: dziennik remontów domu.
 *   Każdy remont to osobny wpis z numerem: 1. wylać fundament, 2. postawić ściany, 3. położyć dach. Dziennik mówi,
 *   co już zrobiono. Nowy majster czyta dziennik i robi tylko wpisy, których jeszcze nie ma. Nikt nie wraca do wpisu 1
 *   i nie poprawia go "wstecz" — jeśli fundament był źle, dopisuje się wpis 4: "wzmocnić fundament".
 *
 * JAK TO DZIAŁA:
 *   Start aplikacji z Flyway w classpath (FlywayAutoConfiguration — autokonfiguracja):
 *     1. Flyway łączy się z bazą i szuka tabeli flyway_schema_history; jeśli jej nie ma — tworzy ją.
 *     2. Skanuje katalog migracji (spring.flyway.locations, domyślnie classpath:db/migration) i układa pliki wg wersji.
 *     3. Porównuje z historią: dla już wykonanych sprawdza SUMĘ KONTROLNĄ pliku (validate); gdy plik zmieniono
 *        po wykonaniu — przerywa start (nigdy nie edytuj wykonanej migracji).
 *     4. Wykonuje brakujące migracje wersjonowane (V) rosnąco, zapisując każdą w historii (wersja, opis, suma kontrolna,
 *        czas, sukces).
 *     5. Na końcu wykonuje migracje powtarzalne (R), jeśli są nowe albo zmieniła się ich treść.
 *   W Spring Boot 4 Flyway wykonuje się PRZED skryptami spring.sql.init (sprawdzone testem sekcji 6).
 *
 * SŁÓWKA:
 *   versioned migration = migracja wersjonowana (V); repeatable migration = migracja powtarzalna (R);
 *   schema history table = tabela historii schematu; checksum = suma kontrolna; applied = zastosowana;
 *   pending = oczekująca; target = wersja docelowa; validate = sprawdź; repair = napraw historię;
 *   description = opis; locations = lokalizacje plików.
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc03SqlInit (poprzednik bez wersjonowania), s07_data_jdbc/Jdbc05Repository,
 *             t29_jdbc_databases/Jdbc01SqlBasics (CREATE TABLE, ALTER TABLE).
 * </pre>
 */
public final class Jdbc04Flyway {

    private Jdbc04Flyway() {
    }

    // =================================================================================================
    // 1. Migracje wersjonowane i tabela historii
    // =================================================================================================
    //
    // Pliki lekcji (src/main/resources/db/migration/jdbc04/):
    //     V1__create_products.sql        tworzy tabelę products
    //     V2__add_category.sql           dodaje kolumnę category (ALTER TABLE) — istniejące wiersze dostają wartość domyślną
    //     V3__seed_products.sql          wstawia dane startowe
    // Po starcie w bazie jest tabela flyway_schema_history z jednym wierszem na migrację: installed_rank (kolejność),
    // version, description, script, checksum, success itd. Test sekcji 1 odpytuje ją zwykłym SQL-em.
    //
    // UWAGA (H2): Flyway tworzy tę tabelę z nazwami MAŁYMI LITERAMI w cudzysłowie, a H2 domyślnie zamienia nazwy
    // bez cudzysłowu na DUŻE litery. Dlatego zapytanie musi brzmieć SELECT "version" FROM "flyway_schema_history"
    // (z cudzysłowami) — bez nich dostaniesz "Table FLYWAY_SCHEMA_HISTORY not found". Sprawdzone uruchomieniem.
    //
    // DOBRA PRAKTYKA: migracja = małe, jedno-tematyczne pliki; zmianę struktury robisz NOWYM plikiem (V4__...), nigdy
    //   edycją starego. Dlaczego? Wykonana migracja jest już w bazach innych osób i na produkcji; stary plik
    //   musi w nich dalej oznaczać dokładnie to, co wykonano.

    // =================================================================================================
    // 2. Nazwa pliku: V<wersja>__<opis>.sql
    // =================================================================================================
    //
    //     V   +   wersja (1, 2, 2.1, 20261011...)   +   DWA podkreślniki   +   opis (słowa łączone podkreślnikiem)   +   .sql
    //     V2__add_category.sql    → wersja 2, opis "add category" (podkreślniki w opisie to spacje)
    // Wersje porównuje się jako LICZBY (2 przed 10), nie jako tekst.
    //
    // PUŁAPKA: jeden podkreślnik (V2_add.sql) albo mała litera (v3__add.sql). Dlaczego? Flyway po cichu IGNORUJE pliki,
    //   które nie pasują do wzorca — migracja po prostu nie wykona się i nikt nie dostanie błędu. Sprawdzone testem
    //   sekcji 2 (z domyślnymi ustawieniami wykonały się 2 z 4 plików). Ochrona: spring.flyway.validate-migration-naming=true
    //   (w API Flyway: validateMigrationNaming) — wtedy zły plik kończy się wyjątkiem z listą błędnych nazw.

    // =================================================================================================
    // 3. Suma kontrolna: nigdy nie edytuj wykonanej migracji
    // =================================================================================================
    //
    // Przy wykonaniu migracji Flyway zapisuje w historii sumę kontrolną (checksum) pliku. Przy KAŻDYM starcie liczy ją
    // od nowa i porównuje. Gdy ktoś zmieni treść pliku, który już poszedł do bazy, suma się nie zgadza i Flyway rzuca
    // FlywayValidateException ("Migration checksum mismatch for migration version 1"). Aplikacja nie wystartuje.
    // Sprawdzone testem sekcji 3 (wersja oryginalna wykonana, potem walidacja wersji zmienionej).
    //
    // Dlaczego Flyway jest taki surowy? Edycja starego pliku NIE zmieni baz, w których migracja już się wykonała, więc
    // bazy rozjechałyby się po cichu: u Ciebie nowa kolumna jest, na produkcji nie. Lepiej zatrzymać start głośno.
    //
    // PUŁAPKA: "poprawię literówkę w V1, tylko trochę". Dlaczego? Każda zmiana pliku (nawet komentarz czy spacja) zmienia
    //   sumę kontrolną i psuje start we wszystkich bazach, gdzie V1 już wykonano. Dopisz nową migrację.
    //   Awaryjnie istnieje polecenie repair (naprawa historii), ale to ostateczność — świadomie ustawia nowe sumy.

    // =================================================================================================
    // 4. Migracje powtarzalne (R__) i idempotencja
    // =================================================================================================
    //
    // Plik R__product_names_view.sql nie ma numeru wersji. Flyway wykonuje go ZA KAŻDYM RAZEM, gdy zmieni się jego treść
    // (czyli suma kontrolna), i zawsze PO wszystkich wersjonowanych. Pasuje do rzeczy, które się "nadpisuje": widoki,
    // procedury, funkcje — stąd w nim CREATE OR REPLACE. Gdy treść się nie zmieniła, nic się nie dzieje.
    // Ponowne wywołanie migrate() na tej samej bazie bez nowych plików nie wykonuje niczego (0 migracji) — Flyway jest
    // idempotentny (powtórzenie nie zmienia wyniku). Sprawdzone testem sekcji 4.
    //
    // PUŁAPKA: wstawianie danych w migracji powtarzalnej (INSERT INTO ... w R__). Dlaczego? Skrypt wykona się od nowa po
    //   każdej zmianie pliku i zdubluje dane. Powtarzalne są do obiektów, które da się bezpiecznie nadpisać.

    // =================================================================================================
    // 5. Właściwości Boota: spring.flyway.*
    // =================================================================================================
    //
    //   spring.flyway.enabled=true|false      czy Flyway ma w ogóle działać (domyślnie true, gdy jest na classpath)
    //   spring.flyway.locations=...           gdzie szukać plików (domyślnie classpath:db/migration)
    // W tym module wspólny application.properties wyłącza Flyway (enabled=false), a lekcja włącza go w profilu jdbc04
    // i wskazuje classpath:db/migration/jdbc04. Test sekcji 5 pokazuje oba ustawienia na małym kontekście.
    //
    // DOBRA PRAKTYKA: osobny katalog migracji na moduł/aplikację i jawne locations, gdy kilka zestawów migracji leży
    //   na jednym classpath. Dlaczego? Domyślny db/migration byłby wspólny dla wszystkich i każda aplikacja
    //   wykonałaby cudze migracje.

    // =================================================================================================
    // 6. Flyway i spring.sql.init na jednej bazie — nie łącz
    // =================================================================================================
    //
    // Gdy włączone są oba mechanizmy, Boot wykonuje najpierw Flyway, a dopiero potem skrypty spring.sql.init
    // (sprawdzone testem sekcji 6: skrypt tworzący tabelę, którą stworzył już Flyway, kończy start błędem
    // "table already exists"). Dwa źródła prawdy o schemacie to prosta droga do konfliktów.
    //
    // PUŁAPKA: zostawić schema.sql/data.sql "na wszelki wypadek" obok Flyway. Dlaczego? W najlepszym razie skrypt wykona
    //   się po migracjach i zepsuje start (tabela już istnieje), w najgorszym po cichu zmieni dane poza historią Flyway.
    // DOBRA PRAKTYKA: z Flyway cały schemat i dane startowe żyją w migracjach, a spring.sql.init.mode=never. Dlaczego?
    //   Jedno źródło prawdy: historia w tabeli flyway_schema_history mówi, jak powstała baza.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Migracje: src/main/resources/db/migration/V<wersja>__<opis>.sql (dwa podkreślniki!), wykonywane rosnąco, raz.
     *   • Historia w tabeli flyway_schema_history (w H2 zapytanie z cudzysłowami: "flyway_schema_history", "version").
     *   • Źle nazwany plik (V2_x.sql, v3__x.sql) jest po cichu ignorowany; validate-migration-naming=true to wykryje.
     *   • Wykonanej migracji NIGDY nie edytuj: suma kontrolna się nie zgodzi → FlywayValidateException; dopisz nową.
     *   • R__nazwa.sql = migracja powtarzalna (widoki, procedury), wykonywana po zmianie treści, zawsze po V.
     *   • Ponowny start bez nowych plików nie wykonuje nic (idempotencja).
     *   • Boot: spring.flyway.enabled, spring.flyway.locations (domyślnie classpath:db/migration).
     *   • Nie łącz Flyway ze schema.sql/data.sql na jednej bazie; Flyway wykonuje się pierwszy.
     *
     * PYTANIA KONTROLNE:
     *   1. Jak powinien nazywać się plik migracji, która dodaje do tabeli kolumnę i ma być wersją 4? Ile podkreślników?
     *   2. Co się stanie? Plik nazwano V5_add_index.sql (jeden podkreślnik) i uruchamiasz aplikację.
     *   3. Co się stanie? Ktoś zmienił treść V1__create_products.sql po tym, jak migracja została wykonana, i uruchamiasz aplikację.
     *   4. ZNAJDŹ BŁĄD: zespół poprawił literówkę w nazwie kolumny, edytując V2__add_category.sql, "bo tak jest szybciej".
     *      Co pójdzie nie tak i jak zrobić to poprawnie?
     *   5. Czym różni się migracja V od R i do czego użyjesz której?
     *   6. Co się stanie? Wywołasz migrate() drugi raz na tej samej bazie bez żadnych nowych plików.
     *   7. Co się stanie? W projekcie jest Flyway z migracją V1 tworzącą tabelę products oraz schema.sql
     *      z CREATE TABLE products, a spring.sql.init.mode=always.
     *   8. Dlaczego zapytanie SELECT version FROM flyway_schema_history nie działa na H2 i jak je poprawić?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc04FlywayExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć opisy (kolumna "description") zastosowanych migracji WERSJONOWANYCH w kolejności
     * wykonania, czyli bez wpisu o utworzeniu tabeli historii i bez migracji powtarzalnych (ich wersja to NULL).
     * Oczekiwane: [create products, add category, seed products].
     * Podpowiedź: SELECT "description" FROM "flyway_schema_history" WHERE "version" IS NOT NULL ORDER BY "installed_rank"
     * (cudzysłowy są obowiązkowe w H2).
     */
    static List<String> exercise1(JdbcClient jdbcClient) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć posortowane alfabetycznie nazwy kolumn tabeli PRODUCTS (po migracjach V1 i V2).
     * Oczekiwane: [CATEGORY, ID, NAME, PRICE_CENTS]. Podpowiedź: widok INFORMATION_SCHEMA.COLUMNS
     * (kolumny TABLE_NAME i COLUMN_NAME); zwykłe nazwy tworzone przez nas bez cudzysłowu są w H2 zapisane DUŻYMI literami.
     */
    static List<String> exercise2(JdbcClient jdbcClient) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): na PUSTEJ bazie (parametr dataSource) uruchom Flyway programowo
     * ({@code Flyway.configure().dataSource(dataSource).locations(...).target("2").load()}), migrując z katalogu
     * classpath:db/migration/jdbc04 tylko DO WERSJI 2 (target = wersja docelowa), a następnie zwróć nazwy plików
     * zastosowanych migracji (metoda getScript z {@code flyway.info().applied()}), pomijając wpis o tabeli historii (pusta nazwa).
     * Oczekiwane: [V1__create_products.sql, V2__add_category.sql, R__product_names_view.sql] — migracja powtarzalna
     * wykonuje się też przy ustawionym target, a V3 nie.
     */
    static List<String> exercise3(DataSource dataSource) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Np. V4__add_column_to_products.sql — litera V, numer, DWA podkreślniki, opis ze słów łączonych podkreślnikami.
     *   2. Nic nie zostanie wykonane i nie będzie błędu: Flyway po cichu ignoruje pliki o złej nazwie. Z
     *      validate-migration-naming=true start kończy się wyjątkiem z listą błędnych nazw. Sprawdzone testem 2.
     *   3. Start się nie powiedzie: FlywayValidateException ("Migration checksum mismatch for migration version 1").
     *      Sprawdzone testem 3.
     *   4. Zmieniona suma kontrolna: bazy, w których V2 już poszła, odmówią startu, a te, które jeszcze nie — dostaną
     *      nową wersję, więc bazy się rozjadą. Poprawnie: nowy plik V4__rename_column.sql z ALTER TABLE ... RENAME COLUMN.
     *   5. V jest wykonywana raz, w kolejności numerów (struktura i dane, które zmieniają się w czasie). R nie ma numeru
     *      i wykonuje się ponownie po zmianie treści, zawsze po V (widoki, procedury, funkcje). Sprawdzone testem 4.
     *   6. Nie wykona niczego (0 migracji) — Flyway porównuje pliki z historią. Sprawdzone testem 4.
     *   7. Start się nie powiedzie: Flyway wykona się pierwszy i utworzy products, a potem skrypt schema.sql dostanie błąd
     *      "Table PRODUCTS already exists". Sprawdzone testem 6.
     *   8. Flyway tworzy tabelę i jej kolumny małymi literami w cudzysłowie, a H2 bez cudzysłowu szuka DUŻYCH liter.
     *      Poprawnie: SELECT "version" FROM "flyway_schema_history". Sprawdzone uruchomieniem
     *      (komunikat: Table FLYWAY_SCHEMA_HISTORY not found, candidates are flyway_schema_history).
     */
    // </editor-fold>
}

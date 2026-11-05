package s07_data_jdbc.jdbc04_flyway;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * <pre>
 * TEMAT: Flyway — wersjonowane migracje bazy danych
 *        (migration = migracja, zmiana struktury lub danych bazy zapisana w pliku; version = wersja;
 *         checksum = suma kontrolna pliku; baseline = punkt odniesienia, „bazą zaczynamy od tej wersji”;
 *         repeatable = powtarzalny; history = historia; validate = sprawdzić; clean = wyczyścić)
 *
 * W SKRÓCIE:
 *   Flyway to narzędzie, które prowadzi strukturę bazy tak, jak Git prowadzi kod: każda zmiana to osobny,
 *   NUMEROWANY plik SQL ({@code V1__create_product.sql}, {@code V2__add_stock.sql}…). Flyway zapisuje w bazie,
 *   które pliki już wykonał (tabela historii {@code flyway_schema_history}), i przy starcie wykonuje TYLKO te nowe.
 *   Dzięki temu ta sama aplikacja uruchomiona na pustej bazie, na bazie sprzed miesiąca i na bazie produkcyjnej
 *   dochodzi do tego samego schematu. Poprzednia lekcja (schema.sql) tego nie umiała: brak historii, brak wersji,
 *   brak możliwości zmiany bazy, która już istnieje i ma dane.
 *
 * ANALOGIA: dziennik remontów w bloku.
 *   Administrator prowadzi zeszyt: „1. położono rury, 2. dodano grzejniki, 3. wymieniono zawór”. Nowy wykonawca
 *   czyta zeszyt, widzi, że punkty 1–2 są zrobione, i wykonuje tylko punkt 3. Wpisów w zeszycie nie wolno
 *   poprawiać „wstecz” — jeśli się pomylono, dopisuje się NOWY punkt. Flyway pilnuje tego sumą kontrolną:
 *   zmieniony stary wpis = alarm (aplikacja się nie uruchomi).
 *
 * JAK TO DZIAŁA:
 *   1. W projekcie jest starter {@code spring-boot-starter-flyway} (w Boot 4 osobny; w Boot 3 i starszych
 *      wystarczało {@code flyway-core} — stare tutoriale mają inną zależność) i driver bazy (tu H2).
 *   2. Autokonfiguracja {@code FlywayAutoConfiguration} (pakiet org.springframework.boot.flyway.autoconfigure,
 *      jar spring-boot-flyway) tworzy bean Flyway z TYM SAMYM DataSource co reszta aplikacji oraz bean
 *      {@code flywayInitializer}, który wywołuje {@code flyway.migrate()}.
 *   3. Beany zależne od bazy (JdbcClient, JdbcTemplate, repozytoria) czekają na ten inicjalizator — w komunikacie
 *      błędu widać „Failed to initialize dependency 'flywayInitializer' of JdbcClient bean” (sprawdzone testem).
 *      Dlatego Twój kod zawsze startuje na już zmigrowanej bazie.
 *   4. {@code migrate()}: odczytuje pliki z {@code spring.flyway.locations}, porównuje z tabelą historii,
 *      sprawdza sumy kontrolne wykonanych już plików (walidacja) i wykonuje brakujące — w kolejności numerów.
 *   5. Każde wykonanie zostaje zapisane w historii: wersja, opis, suma kontrolna, czas, sukces.
 *
 * SŁÓWKA:
 *   versioned migration = migracja wersjonowana (V…, wykonywana dokładnie raz); repeatable migration = migracja
 *   powtarzalna (R…, wykonywana ponownie po każdej zmianie treści); schema history table = tabela historii schematu;
 *   pending = oczekująca (jeszcze niewykonana); applied = zastosowana (wykonana); location = lokalizacja plików;
 *   drift = „rozjazd” (baza różni się od tego, co opisują pliki).
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc03SqlInit (schema.sql — dlaczego to za mało),
 *             s07_data_jdbc/Jdbc01JdbcClient (JdbcClient i @JdbcTest, na których tu pracujemy),
 *             s03_boot_basics/Boot01AutoConfiguration (skąd się bierze bean Flyway),
 *             s03_boot_basics/Boot04Starters (starter flyway = zależności w jednym),
 *             s08_data_jpa/Jpa06FlywayWithJpa (Flyway razem z JPA: ddl-auto=validate),
 *             t29_jdbc_databases/Jdbc01SqlBasics (składnia CREATE TABLE, ALTER TABLE, INSERT).
 * </pre>
 */
public final class Jdbc04Flyway {

    private Jdbc04Flyway() {
    }

    // =================================================================================================
    // 1. MIGRACJA = WERSJONOWANY PLIK; TABELA HISTORII
    // =================================================================================================
    //
    // Pliki lekcji leżą w src/main/resources/db/migration/jdbc04/ (każda lekcja ma SWÓJ katalog — izolacja):
    //   V1__create_product.sql     tabela product (id, name, category, price)
    //   V2__add_stock.sql          ALTER TABLE: dodaje kolumnę stock
    //   V3__insert_products.sql    dane: pięć produktów sklepu
    //   R__product_overview.sql    widok product_overview (migracja powtarzalna, sekcja 4)
    //
    // NAZWA PLIKU to cały kontrakt:  V <wersja> __ <opis> .sql
    //   • duże V (lub R), numer wersji (1, 2, 10, 2.1), DWA podkreślniki, opis ze słowami po podkreślniku, .sql;
    //   • numery porównywane są LICZBOWO: V2 jest przed V10 (test), nie alfabetycznie;
    //   • podkreślniki w opisie Flyway zamienia na spacje: „add_stock” → „add stock” w kolumnie description.
    //
    // PUŁAPKA: jeden podkreślnik (V2_zle.sql). Dlaczego groźne? Flyway NIE zgłasza błędu — taki plik nie wygląda
    //   mu na migrację, więc go po cichu pomija (tylko wpis w logu na poziomie INFO). Zmiana „nie działa”, a nikt nie wie
    //   dlaczego. Test pokazuje to na żywo, a {@code validateMigrationNaming(true)} (w Boot:
    //   {@code spring.flyway.validate-migration-naming=true}) zamienia pomijanie w błąd z nazwą pliku.
    //
    // TABELA HISTORII flyway_schema_history (nazwę zmienia spring.flyway.table). Kolumny: installed_rank (kolejność
    // wykonania), version, description, type (SQL), script (nazwa pliku), checksum, installed_by, installed_on,
    // execution_time, success. W Flyway 12.4 w H2 jest jeszcze wiersz-znacznik type=TABLE o randze -1
    // („Flyway Schema History table created”) — dlatego nasz odczyt filtruje type = 'SQL'.
    //
    // PUŁAPKA: w H2 Flyway tworzy tabelę i kolumny MAŁYMI literami w cudzysłowie ("flyway_schema_history", "version"),
    //   a H2 niecytowane nazwy zamienia na WIELKIE litery. Dlatego {@code SELECT * FROM flyway_schema_history} daje
    //   BadSqlGrammarException („Table FLYWAY_SCHEMA_HISTORY not found”), a trzeba pisać
    //   {@code SELECT * FROM "flyway_schema_history"} (test). Na PostgreSQL jest odwrotnie — bez cudzysłowu działa.
    //
    // Test sekcji 1: Jdbc04FlywayJdbcTest czyta historię przez Jdbc04MigrationHistory — wiersze V1, V2, V3 (wersje
    // „1”, „2”, „3”) i R (wersja null), opisy, nazwy skryptów, success=true, sumy kontrolne niepuste.
    //
    // DOBRA PRAKTYKA: jedna migracja = jedna zmiana logiczna, z opisem mówiącym CO i PO CO (add_stock, a nie update1).
    //   Dlaczego? Za pół roku historia migracji jest jedynym opisem tego, jak baza doszła do obecnego kształtu.

    // =================================================================================================
    // 2. JAK BOOT URUCHAMIA FLYWAY — ustawienia spring.flyway.*
    // =================================================================================================
    //
    // Kolejność startu (sprawdzona testem): Flyway migruje bazę → dopiero potem powstają beany używające DataSource.
    // Jdbc04StartupReport czyta w konstruktorze tabelę product i znajduje w niej 5 wierszy z migracji V3.
    //
    // Ustawienia (nazwy z metadanych Boot 4.1.1, jar spring-boot-flyway):
    //   spring.flyway.enabled              true (domyślnie). W TYM MODULE wspólny application.properties ustawia false,
    //                                      żeby Flyway nie ruszał baz innych lekcji; lekcja włącza go w swoim profilu.
    //   spring.flyway.locations            gdzie szukać plików; domyślnie classpath:db/migration (odczytane z klasy
    //                                      FlywayProperties). U nas: classpath:db/migration/jdbc04.
    //   spring.flyway.baseline-on-migrate  false (domyślnie) — patrz niżej.
    //   spring.flyway.clean-disabled       true (domyślnie) — blokada kasowania bazy (sekcja 5).
    //   spring.flyway.validate-on-migrate  true (domyślnie) — przed migracją sprawdza sumy kontrolne (sekcja 3).
    //   spring.flyway.fail-on-missing-locations  false (domyślnie) — patrz PUŁAPKA poniżej.
    //   spring.flyway.table                nazwa tabeli historii (domyślnie flyway_schema_history).
    //
    // H2 W FLYWAY 12: NIE potrzeba żadnego dodatkowego modułu. Od Flyway 10 obsługa części baz (np. PostgreSQL,
    // SQL Server, Oracle) leży w osobnych zależnościach flyway-database-…; wsparcie dla H2 jest w flyway-core, więc
    // nasz pom.xml ma tylko starter i H2 — i migracje działają. Ostrzeżenie w logu „H2 … newer than the version
    // Flyway has been verified with” oznacza tylko, że H2 2.4 jest nowsza od wersji, na której Flyway testowano.
    //
    // BASELINE — wdrożenie Flyway do bazy, która już istnieje. Pusta baza: Flyway tworzy tabelę historii i działa.
    // Baza z tabelami, ale BEZ historii: migrate() odmawia („Found non-empty schema(s) … but no schema history
    // table… baselineOnMigrate”) — Flyway nie wie, co z tymi tabelami zrobić. spring.flyway.baseline-on-migrate=true
    // + baseline-version=1 mówią: „traktuj stan obecny jako wersję 1, plik V1 pomiń, wykonaj V2 i dalej” (test:
    // stany BASELINE i BASELINE_IGNORED w info()).
    //
    // PUŁAPKA: literówka w locations ({@code classpath:db/migration/jdbc4}). Dlaczego groźne? Przy domyślnym
    //   fail-on-missing-locations=false Flyway nie znajduje żadnych plików, ale NIE zgłasza błędu: aplikacja startuje
    //   bez tabel i wywala się dopiero na pierwszym zapytaniu (BadSqlGrammarException). Test pokazuje to na żywo;
    //   fail-on-missing-locations=true zamienia to w błąd przy starcie („Unable to resolve location …”).
    //
    // DOBRA PRAKTYKA: w prawdziwym projekcie włącz spring.flyway.fail-on-missing-locations=true. Dlaczego? Brak
    //   katalogu z migracjami to prawie zawsze błąd konfiguracji, a nie zamiar — lepiej, żeby aplikacja nie wystartowała.

    // =================================================================================================
    // 3. MIGRACJI, KTÓRA JUŻ POSZŁA, NIGDY SIĘ NIE EDYTUJE — suma kontrolna
    // =================================================================================================
    //
    // Po wykonaniu pliku Flyway zapisuje w historii jego sumę kontrolną (checksum — liczba policzona z treści).
    // Przy każdym kolejnym starcie liczy ją od nowa dla plików, które już poszły, i porównuje.
    // Zmieniony stary plik → inna suma → FlywayValidateException:
    //     Validate failed: Migrations have failed validation
    //     Migration checksum mismatch for migration version 1
    //     -> Applied to database : (suma zapisana w historii)
    //     -> Resolved locally    : (suma policzona z pliku teraz)
    //     Either revert the changes to the migration, or run repair to update the schema history.
    // (typ: org.flywaydb.core.api.exception.FlywayValidateException, podklasa FlywayException; treść komunikatu sprawdzona testem).
    // W aplikacji Boot wyjątek wychodzi z beana flywayInitializer i aplikacja NIE STARTUJE (BeanCreationException;
    // test) — świadomie, bo baza i kod rozjechałyby się po cichu.
    //
    // DLACZEGO TAK SUROWO? Stary plik poszedł już na bazy kolegów, testową i produkcyjną. Zmiana jego treści nie
    // zmieni tamtych baz — powstałyby dwie różne „wersje 1” i nikt nie wiedziałby, która jest prawdziwa.
    //
    // JAK NAPRAWIĆ? Dwie drogi: (1) pomyłka w SQL poszła już na bazy → dopisz NOWĄ migrację (V2__…), która to
    // koryguje; stary plik zostaje nietknięty (test: walidacja przechodzi, wykonuje się tylko V2). (2) Edycja była
    // przypadkowa → cofnij ją (git). Komenda repair przepisuje sumy kontrolne w historii — to narzędzie awaryjne,
    // gdy WIESZ, że bazy i plik są i tak zgodne; nie jest sposobem na „ciche” poprawianie starych plików.
    //
    // PYTANIE REKRUTACYJNE: dlaczego nie wolno zmieniać migracji, która została już wdrożona, i co zrobić zamiast tego?
    //
    // PUŁAPKA: edycja starej migracji „tylko u siebie, bo baza lokalna i tak jest wyczyszczona”. Dlaczego źle? U Ciebie
    //   zadziała, a u kolegi i na serwerze CI (bazy z historią) aplikacja nie wystartuje — błąd wychodzi dopiero po
    //   wdrożeniu. Reguła „stare pliki są tylko do odczytu” musi obowiązywać zawsze.

    // =================================================================================================
    // 4. MIGRACJA Z DANYMI I MIGRACJE POWTARZALNE (R__)
    // =================================================================================================
    //
    // MIGRACJA Z DANYMI (V3__insert_products.sql): zwykła migracja wersjonowana zawierająca INSERT. Nadaje się do
    // danych referencyjnych (słowniki, kategorie, role) i startowych. Zasada ta sama: wykona się raz i jej treści
    // nie zmieniamy. Dane testowe/przykładowe trzymaj osobno (@Sql w testach, Jdbc03SqlInit) — na produkcję
    // nie powinny trafiać „losowe” produkty.
    //
    // MIGRACJA POWTARZALNA (R__product_overview.sql): brak numeru wersji. Flyway wykonuje ją
    //   • za pierwszym razem,
    //   • ponownie za każdym razem, gdy zmieni się jej TREŚĆ (suma kontrolna), i nie ma to nic wspólnego z błędem walidacji,
    //   • ZAWSZE po wszystkich migracjach wersjonowanych (nawet gdy nazwa pliku „stoi wcześniej”) — test.
    // Dlatego pasuje do rzeczy, które da się bezpiecznie nadpisać: widoków (CREATE OR REPLACE VIEW), procedur,
    // funkcji. Nie pasuje do CREATE TABLE ani INSERT-ów (powtórne wykonanie by je zdublowało lub zepsuło).
    // W historii każde wykonanie to osobny wiersz z tą samą nazwą skryptu i wersją null (test).
    //
    // DOBRA PRAKTYKA: widoki i procedury trzymaj w R__, a nie w V__. Dlaczego? Poprawka widoku to wtedy edycja jednego
    //   pliku (z historią w Git), a nie nowy plik V przy każdej zmianie.

    // =================================================================================================
    // 5. FLYWAY W TESTACH I NA PRODUKCJI
    // =================================================================================================
    //
    // @JdbcTest ma w swoim wycinku autokonfigurację Flyway (zweryfikowane w jarze spring-boot-flyway, plik
    // AutoConfigureDataSourceInitialization.imports) — więc przy włączonym spring.flyway.enabled migracje wykonują
    // się na bazie podmienionej przez test (losowa nazwa; Jdbc04FlywayJdbcTest). Zalety:
    //   • test działa na PRAWDZIWYM schematem z migracji, nie na jego ręcznej kopii — literówka w V2 wyjdzie w testach;
    //   • migracje wykonują się raz na kontekst testowy, a dane z testu wycofuje rollback (Jdbc01JdbcClient, sekcja 1).
    //     Dwa testy dodające ten sam produkt oba widzą 6 wierszy.
    // Każda lekcja ma własną lokalizację (db/migration/jdbc04, w następnej jdbc05) i własną bazę — dlatego migracje
    // jednej lekcji nie wpadają do drugiej.
    //
    // OD TEJ LEKCJI wszystkie działy z bazą prowadzą schemat Flywayem. W s08 (JPA) Hibernate nie będzie tworzył tabel
    // ({@code spring.jpa.hibernate.ddl-auto=validate} tylko sprawdzi, czy encje pasują do schematu z migracji) —
    // s08_data_jpa/Jpa06FlywayWithJpa.
    //
    // PUŁAPKA: clean. Polecenie {@code flyway.clean()} kasuje WSZYSTKIE obiekty w schemacie (tabele, widoki, dane).
    //   Dlaczego Boot domyślnie je blokuje (spring.flyway.clean-disabled=true)? Bo pomyłka na produkcji = utrata
    //   bazy. Bez odblokowania clean() rzuca FlywayException „Unable to execute clean as it has been disabled”;
    //   po {@code cleanDisabled(false)} kasuje bazę, a Flyway znów widzi V1 jako oczekującą (oba testy). Odblokowuj
    //   to wyłącznie w lokalnych/testowych profilach, NIGDY w produkcyjnym. Uwaga dla czytelników starszych
    //   tutoriali: ustawienie clean-on-validation-error („przy błędzie walidacji skasuj bazę”) nie istnieje już
    //   w Flyway 12 (brak w metadanych Boot 4.1.1 i w API) — i dobrze.
    //
    // DOBRA PRAKTYKA: na produkcji zostaw clean-disabled=true, a zmiany robisz wyłącznie nowymi migracjami do przodu
    //   (nie „cofaniem”). Dlaczego? Migracja wykonana na bazie z danymi nie ma bezpiecznego cofnięcia — ALTER TABLE DROP
    //   COLUMN gubi dane. Błąd naprawia się następną migracją.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Migracja = plik V<numer>__<opis>.sql (DWA podkreślniki); wykonuje się raz, w kolejności numerów (liczbowo).
     *   • Historia w tabeli "flyway_schema_history" (w H2: małe litery w cudzysłowie). Wiersze: wersja, opis, checksum, success.
     *   • Boot 4: spring-boot-starter-flyway; FlywayAutoConfiguration + flywayInitializer migrują PRZED beanami z bazą.
     *   • H2 nie potrzebuje modułu flyway-database-…; spring.flyway.locations wskazuje katalog z plikami.
     *   • Stara migracja zmieniona po wdrożeniu → FlywayValidateException (checksum mismatch). Zmiana = NOWA migracja.
     *   • R__opis.sql = migracja powtarzalna (wykonuje się po zmianie treści i po wszystkich V); dobre na widoki.
     *   • Baza istniejąca bez historii: baseline-on-migrate=true + baseline-version. Literówka w locations: cicha,
     *     chyba że fail-on-missing-locations=true. Plik V2_zle.sql (jeden podkreślnik) jest po cichu pomijany.
     *   • clean-disabled=true na produkcji; clean-on-validation-error nie istnieje już we Flyway 12.
     *   • @JdbcTest uruchamia migracje: testujesz na prawdziwym schemacie; dane z testu wycofuje rollback.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym migracja Flyway różni się od schema.sql z poprzedniej lekcji (podaj co najmniej trzy różnice)?
     *   2. Co się stanie? Po wdrożeniu poprawiasz literówkę w pliku V1__create_product.sql i uruchamiasz aplikację.
     *   3. Co się stanie? Nazywasz nowy plik V4_add_discount.sql (jeden podkreślnik).
     *   4. ZNAJDŹ BŁĄD: {@code jdbc.sql("SELECT * FROM flyway_schema_history")} na H2 — dlaczego to nie działa
     *      i jak to naprawić?
     *   5. Co się stanie? W {@code spring.flyway.locations} jest literówka, a fail-on-missing-locations ma wartość domyślną.
     *   6. Czym różni się migracja V__ od R__ i kiedy wybierzesz R__?
     *   7. Co się stanie? Wywołujesz {@code flyway.clean()} na Flyway skonfigurowanym przez Boot bez dodatkowych ustawień.
     *   8. Co się stanie? Podłączasz Flyway do istniejącej bazy z tabelami, ale bez tabeli historii.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc04FlywayExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj nazwę pliku migracji z numeru wersji i opisu: spacje w opisie zamień na
     * pojedyncze podkreślniki, opis zapisz małymi literami. Np. (5, "Add discount percent") →
     * {@code "V5__add_discount_percent.sql"}.
     * Podpowiedź: sekcja 1 — {@code "V" + wersja + "__" + opis + ".sql"} (DWA podkreślniki po wersji).
     */
    static String exercise1MigrationFileName(int version, String description) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć numer wersji OSTATNIEJ pomyślnie wykonanej migracji wersjonowanej (np. "3"),
     * odczytany z tabeli historii. Migracji powtarzalnych (wersja {@code null}) nie liczymy.
     * Podpowiedź: sekcja 1 — nazwy w cudzysłowie ({@code "flyway_schema_history"}, {@code "version"}),
     * {@code ORDER BY "installed_rank" DESC LIMIT 1}. Dlaczego nie {@code MAX("version")}? Wersja jest tekstem,
     * a w tekście "9" jest „większe” od "10".
     */
    static String exercise2LastVersion(JdbcClient jdbc) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć treść migracji V4, która dodaje do tabeli product kolumnę
     * {@code discount_percent} typu INT, mogącą mieć NULL (istniejące produkty nie mają zniżki).
     * Podpowiedź: sekcja 1 i plik V2__add_stock.sql tej lekcji — {@code ALTER TABLE … ADD COLUMN …}. Test zapisze Twój SQL jako plik
     * V4 obok migracji lekcji i uruchomi Flyway.
     */
    static String exercise3AddDiscountSql() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć treść migracji POWTARZALNEJ tworzącej widok {@code expensive_product}
     * (kolumny name i price) z produktów kosztujących co najmniej 100. Widok ma dać się utworzyć wielokrotnie.
     * Podpowiedź: sekcja 4 — {@code CREATE OR REPLACE VIEW … AS SELECT …}. Test zapisze Twój SQL jako plik
     * R__expensive_product.sql i uruchomi Flyway dwa razy: drugi raz nie ma nic do wykonania.
     */
    static String exercise4ExpensiveProductViewSql() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Migracje mają numery wersji i historię w bazie (wiadomo, co już wykonano), wykonują się tylko raz
     *      i tylko brakujące, mogą ZMIENIAĆ istniejącą bazę z danymi (ALTER TABLE), a zmiana wdrożonego pliku jest
     *      wykrywana sumą kontrolną. schema.sql wykonuje się za każdym razem od zera, nie zna wersji i nie
     *      potrafi dostosować bazy, która już istnieje.
     *   2. Aplikacja się nie uruchomi: FlywayValidateException „Migration checksum mismatch for migration version 1”
     *      (w Boot: BeanCreationException z beana flywayInitializer). Sprawdzone testem (sekcja 3).
     *   3. Flyway nie rzuci błędu — plik zostanie po cichu pominięty (wpis INFO w logu), więc kolumna nie powstanie.
     *      Dopiero validateMigrationNaming(true) zgłosi „Invalid SQL filenames found”. Sprawdzone testem (sekcja 1).
     *   4. H2 zamienia niecytowaną nazwę na wielkie litery (FLYWAY_SCHEMA_HISTORY), a Flyway utworzył tabelę małymi
     *      literami w cudzysłowie — BadSqlGrammarException „Table … not found”. Naprawa: {@code FROM "flyway_schema_history"}.
     *      Sprawdzone testem (sekcja 1).
     *   5. Aplikacja wystartuje bez błędu, ale bez żadnych migracji (info().all() jest puste), więc bez tabel;
     *      błąd BadSqlGrammarException pojawi się dopiero przy pierwszym zapytaniu. Z fail-on-missing-locations=true
     *      start kończy się błędem „Unable to resolve location …”. Sprawdzone testem (sekcja 2).
     *   6. V__ wykonuje się raz i nie wolno jej zmieniać; R__ wykonuje się ponownie po każdej zmianie treści
     *      i zawsze po wszystkich V__. R__ wybierasz dla widoków, funkcji i procedur (CREATE OR REPLACE).
     *      Sprawdzone testem (sekcja 4).
     *   7. FlywayException „Unable to execute clean as it has been disabled with the 'flyway.cleanDisabled' property”
     *      (Boot domyślnie ustawia clean-disabled=true). Sprawdzone testem (sekcje 2 i 5).
     *   8. migrate() odmawia: FlywayException „Found non-empty schema(s) … but no schema history table. Use baseline()
     *      or set baselineOnMigrate to true to initialize the schema history table.” Rozwiązanie: baseline-on-migrate
     *      + baseline-version. Sprawdzone testem (sekcja 2).
     */
    // </editor-fold>
}

package s08_data_jpa.jpa06_flyway_with_jpa;

import javax.sql.DataSource;
import java.util.List;

/**
 * <pre>
 * TEMAT: Flyway + JPA — Flyway tworzy i zmienia schemat, Hibernate ({@code ddl-auto=validate}) tylko go sprawdza
 *        (migration = migracja, skrypt zmiany schematu; schema = schemat — tabele, kolumny, ograniczenia;
 *         validate = sprawdź; ddl-auto = automatyczny DDL Hibernate; DDL — Data Definition Language, polecenia
 *         CREATE/ALTER/DROP)
 *
 * W SKRÓCIE:
 *   Schemat bazy to kod — ma historię, przegląd w code review i jest taki sam na każdym środowisku. Dlatego zmiany
 *   schematu piszemy jako migracje Flyway (V1, V2, V3… — s07_data_jdbc/Jdbc04Flyway), a Hibernate ustawiamy na
 *   {@code validate}: przy starcie porównuje encje z tabelami i jeśli czegoś brakuje albo typ się nie zgadza —
 *   aplikacja NIE startuje. Nowe pole w encji = nowa migracja. {@code update} (Hibernate sam zmienia schemat) wygląda
 *   wygodnie, ale na produkcji jest groźny — w Hibernate 7 potrafi nawet po cichu zmienić typ kolumny.
 *
 * ANALOGIA: architekt i inspektor budowlany.
 *   Flyway to architekt z dziennikiem budowy: każda zmiana (nowe okno, ściana) to wpis z numerem, nikt nie przerabia
 *   starych wpisów. Hibernate z validate to inspektor: przychodzi przed otwarciem budynku i sprawdza z planem (encje),
 *   czy wszystko jest. Hibernate z update to inspektor, który zamiast zgłosić brak drzwi, sam wybija dziurę w ścianie —
 *   bez wpisu w dzienniku, a gdy mu się nie uda, tylko coś zanotuje i otworzy budynek.
 *
 * JAK TO DZIAŁA:
 *   1. Start aplikacji: Boot tworzy bean {@code flyway} i {@code flywayInitializer} (FlywayMigrationInitializer),
 *      który wykonuje brakujące migracje i zapisuje je w tabeli flyway_schema_history.
 *   2. Bean {@code entityManagerFactory} ma dopisaną zależność (depends-on) od {@code flywayInitializer} — Boot
 *      gwarantuje, że Hibernate wystartuje DOPIERO po migracjach (sprawdzone testem).
 *   3. Hibernate z {@code hibernate.hbm2ddl.auto=validate} czyta metadane bazy (tabele, kolumny, typy) i porównuje
 *      z mapowaniem każdej encji. Rozjazd → SchemaManagementException → EntityManagerFactory nie powstaje →
 *      aplikacja nie startuje.
 *   4. Validate sprawdza tylko to, co jest w encjach: nadmiarowe kolumny i tabele w bazie mu nie przeszkadzają.
 *
 * SŁÓWKA:
 *   migration = migracja; versioned = wersjonowana (V1__…); target = docelowa wersja; pending = oczekująca
 *   (jeszcze niewykonana); schema history = historia schematu; drift = rozjazd (dryf) schematu; missing column =
 *   brakująca kolumna; wrong column type = zły typ kolumny; depends on = zależy od; script = skrypt;
 *   create-drop = utwórz i usuń; update = zaktualizuj.
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc04Flyway (Flyway od podstaw: nazwy plików, historia, checksum),
 *             s08_data_jpa/Jpa01Entities (create-drop i domyślne ddl-auto w Boot 4),
 *             s08_data_jpa/Jpa02Repositories (pierwsza lekcja z Flyway + validate),
 *             s11_testing/Test06Testcontainers (testy na prawdziwym PostgreSQL zamiast H2).
 * </pre>
 */
public final class Jpa06FlywayWithJpa {

    private Jpa06FlywayWithJpa() {
    }

    // =================================================================================================
    // 1. PODZIAŁ RÓL — Flyway pisze schemat, Hibernate go sprawdza; kolejność startu
    // =================================================================================================
    //
    //   application-jpa06.properties:
    //     spring.flyway.enabled=true
    //     spring.flyway.locations=classpath:db/migration/jpa06      ← V1 tabela, V2 dane, V3 nowa kolumna
    //     spring.jpa.hibernate.ddl-auto=validate                     ← Hibernate tylko sprawdza
    //
    // Sprawdzone testem (Jpa06FlywayWithJpaTest):
    //   • flyway_schema_history ma wersje 1, 2, 3 (success = true);
    //   • EntityManagerFactory dostał {@code hibernate.hbm2ddl.auto = validate};
    //   • definicja beana entityManagerFactory ma depends-on: {@code flyway, flywayInitializer, …} — to Boot dopisuje
    //     tę zależność, gdy na classpath są i Flyway, i JPA. Dlatego encje nigdy nie są sprawdzane na pustej bazie.
    //
    // Dlaczego tak, a nie Hibernate „sam”? Migracje:
    //   • mają HISTORIĘ (kto, kiedy, co zmienił — w git i w flyway_schema_history);
    //   • przechodzą code review jak każdy kod (DDL widać w pull requeście);
    //   • dają TEN SAM schemat na laptopie, w testach i na produkcji (te same pliki w tej samej kolejności);
    //   • umieją przenosić DANE (UPDATE w migracji), czego generator Hibernate nie zrobi nigdy.
    // Domyślne ddl-auto w Boot 4 (sprawdzone w Jpa01): baza wbudowana bez Flyway → create-drop; z Flyway → Boot nic nie
    // ustawia (none). Mimo to wpisujemy {@code validate} jawnie — bez tego nikt by nie sprawdził zgodności encji.
    //
    // DOBRA PRAKTYKA: {@code ddl-auto=validate} wszędzie tam, gdzie schemat tworzy Flyway (testy, staging, produkcja).
    //   Dlaczego? Rozjazd encji i schematu wychodzi przy STARCIE (w CI, przed wdrożeniem), a nie przy pierwszym
    //   zapytaniu klienta.
    //   PYTANIE REKRUTACYJNE: „Jak zarządzasz schematem bazy w aplikacji Spring Boot + JPA?”

    // =================================================================================================
    // 2. ROZJAZD encji i schematu — aplikacja nie startuje (SchemaManagementException)
    // =================================================================================================
    //
    // Brakująca kolumna (test: migracje tylko do V2 — {@code --spring.flyway.target=2} — a encja ma pole weightGrams):
    //   BeanCreationException: Error creating bean with name 'entityManagerFactory' …
    //     ← PersistenceException: Unable to build Hibernate SessionFactory …
    //       ← org.hibernate.tool.schema.spi.SchemaManagementException:
    //         „Schema validation: missing column [weight_grams] in table [product]”
    //
    // Zły typ kolumny (test: encja WrongTypeProduct w pakiecie testowym jpa06_pitfalls, pole {@code String stock}
    // na kolumnie INTEGER):
    //   SchemaManagementException: „Schema validation: wrong column type encountered in column [stock] in table
    //   [product]; found [integer (Types#INTEGER)], but expecting [varchar(255) (Types#VARCHAR)]”
    //
    // Komunikat mówi dokładnie: która tabela, która kolumna, co jest w bazie, czego oczekuje encja. Validate jest wyrozumiały tam, gdzie typy są „zgodne w Javie”: VARCHAR dla
    // {@code @Enumerated(STRING)}, NUMERIC(10,2) dla BigDecimal (Jpa01, Jpa02).
    //
    // Dlaczego błędna encja jest w osobnym pakiecie TESTOWYM? Klasa startowa lekcji skanuje encje ze swojego pakietu —
    // WrongTypeProduct w jpa06_flyway_with_jpa zepsułby start każdego testu tej lekcji.
    //
    // PUŁAPKA: dodanie pola do encji bez migracji. Dlaczego groźne? Lokalnie z create-drop wszystko działa (Hibernate
    //   sam doda kolumnę), a na środowisku z Flyway + validate aplikacja nie wstanie. Zasada: pole w encji i migracja
    //   w TYM SAMYM commicie — i test @DataJpaTest, który uruchamia validate (każdy test lekcji robi to przy starcie).

    // =================================================================================================
    // 3. Wartości ddl-auto i dlaczego update jest groźny na produkcji
    // =================================================================================================
    //
    //   wartość        co robi Hibernate przy starcie (i przy zamknięciu)          gdzie
    //   none           nic                                                        produkcja (gdy nie chcesz nawet sprawdzać)
    //   validate       porównuje encje ze schematem; rozjazd = nie startuje        produkcja, testy — z Flyway
    //   update         dodaje brakujące tabele/kolumny, zmienia typy; nie usuwa    NIGDY na produkcji; ewentualnie szkic lokalnie
    //   create         DROP + CREATE przy starcie (dane giną przy każdym starcie)  prototyp
    //   create-drop    jak create + DROP przy zamknięciu                           nauka, prototyp (Jpa01), domyślne dla H2 bez Flyway
    //
    // Co robi update w Hibernate 7.4 (sprawdzone testami Jpa06StartupTest):
    //   • ZMIENIA TYP kolumny bez pytania: dla WrongTypeProduct wysłał
    //     {@code alter table if exists product alter column stock set data type varchar(255)} — liczby w stanie magazynu
    //     stały się tekstem. (Starsze tutoriale piszą „update nie zmienia typów” — w Hibernate 7 to nieprawda.)
    //   • NIE USUWA kolumn ani tabel, których nie ma w encji (name, price… zostały, choć WrongTypeProduct ich nie zna).
    //   • Nieudany DDL to tylko OSTRZEŻENIE w logu: {@code alter table … add column weight_grams integer not null} padł
    //     na tabeli z danymi („NULL not allowed for column”), a aplikacja i tak wystartowała — bez kolumny.
    //     Błąd wyszedł dopiero przy pierwszym zapytaniu: InvalidDataAccessResourceUsageException
    //     „Column "P1_0.WEIGHT_GRAMS" not found”.
    //
    // PUŁAPKA: ddl-auto=update na produkcji. Dlaczego?
    //   • brak historii — nikt nie wie, kiedy i dlaczego schemat się zmienił; nie da się tego przejrzeć w code review;
    //   • różne środowiska dostają różne schematy (zależnie od tego, które wersje encji na nich startowały);
    //   • potrafi zmienić typ kolumny z danymi (patrz wyżej), a usuniętych pól nie sprząta — śmieci rosną;
    //   • błędy DDL połyka — awaria przesuwa się ze startu na pierwsze zapytanie klienta;
    //   • nie przeniesie danych (np. podział kolumny „imię i nazwisko” na dwie) — to umie tylko migracja.
    //   PYTANIE REKRUTACYJNE: „Dlaczego nie używa się spring.jpa.hibernate.ddl-auto=update na produkcji?”
    //
    // DOBRA PRAKTYKA: nie mieszaj Flyway z update/create. Dlaczego? Dwóch „właścicieli” schematu: Hibernate zmieni
    //   tabelę bez wpisu w flyway_schema_history, a następna migracja Flyway trafi na schemat, którego się nie
    //   spodziewa (np. kolumna już istnieje → migracja pada).

    // =================================================================================================
    // 4. NOWA KOLUMNA = nowa migracja + pole w encji (scenariusz V1 → V2 → V3)
    // =================================================================================================
    //
    //   V3__add_product_weight.sql:
    //     ALTER TABLE product ADD COLUMN weight_grams INTEGER DEFAULT 0 NOT NULL;   -- DEFAULT wypełnia stare wiersze
    //     UPDATE product SET weight_grams = 450 WHERE name = 'Klawiatura';         -- dane w migracji
    //   Product.java:
    //     private int weightGrams;   // kolumna weight_grams (nazwa z pola, strategia snake_case Springa)
    //
    // Sprawdzone testem (Jpa06MigrationVersionsTest — samo API Flyway, bez Springa):
    //   • {@code Flyway.configure()…target("2").load().migrate()} → tabela bez weight_grams, 5 wierszy;
    //   • {@code target("latest")} → wykonała się TYLKO V3 (migrationsExecuted = 1, wersja 3), istniejące wiersze mają
    //     450/90/320 z UPDATE albo 0 z DEFAULT;
    //   • kolejne migrate na aktualnej bazie: 0 migracji, {@code info().pending()} puste.
    //
    // Dlaczego DEFAULT 0? {@code ADD COLUMN … NOT NULL} bez wartości domyślnej na tabeli z danymi pada (te same
    // wiersze musiałyby dostać NULL — właśnie tak padł ALTER z update w sekcji 3). Bezpieczny wzór dla dużych tabel:
    // dodaj kolumnę jako NULL → uzupełnij dane → dopiero potem ustaw NOT NULL (trzy kroki, czasem trzy migracje).
    //
    // PUŁAPKA: edycja wykonanej już migracji (np. dopisanie kolumny do V1). Dlaczego? Flyway pamięta sumę kontrolną
    //   (checksum) każdego pliku — przy następnym starcie zgłosi „Migration checksum mismatch” (s07_data_jdbc/Jdbc04Flyway),
    //   a bazy, które już wykonały V1, i tak kolumny nie dostaną. Zmiana = zawsze NOWY plik.
    //
    // DOBRA PRAKTYKA: każda migracja małą, jednoznaczną zmianą z opisową nazwą (V3__add_product_weight.sql).
    //   Dlaczego? Łatwo ją przejrzeć, a w razie błędu wiadomo, który krok go wprowadził.

    // =================================================================================================
    // 5. Szkic pierwszej migracji z Hibernate (schema-generation.scripts) — pomoc, nie automat; H2 ≠ produkcja
    // =================================================================================================
    //
    // Standard JPA pozwala zapisać DDL, który Hibernate WYGENEROWAŁBY, do pliku — bez zmieniania bazy:
    //   spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create
    //   spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=build/create.sql
    // Działa razem z {@code ddl-auto=validate} (sprawdzone testem: plik w @TempDir, aplikacja wystartowała). Wynik dla H2:
    //   {@code create table product (price numeric(10,2) not null, stock integer not null, weight_grams integer not null,
    //   created_at timestamp(6) with time zone not null, id bigint generated by default as identity, name varchar(100)
    //   not null, category enum ('COMPUTERS','GARDEN','HOME','OFFICE') not null, primary key (id));}
    //
    // Dlaczego tylko SZKIC (porównaj z naszą V1 — test to sprawdza)?
    //   • Hibernate wybrał dla H2 typ {@code enum (…)} — my mamy VARCHAR(20), przenośny i łatwy do rozszerzenia;
    //   • brak naszego ograniczenia UNIQUE (uk_product_name) — Hibernate wie tylko to, co w adnotacjach;
    //   • kolejność kolumn jest „Hibernate'owa” (nie taka, jak zaprojektowaliśmy); brak DEFAULT, indeksów, danych;
    //   • DDL jest dla dialektu bazy, na której wystartowałeś (tu H2) — dla PostgreSQL wygeneruj go na PostgreSQL.
    // Użyj go raz, na początku projektu: wygeneruj, przeczytaj, popraw ręcznie i zapisz jako V1__….sql. Dalej — tylko
    // ręczne migracje.
    //
    // PUŁAPKA: testy tylko na H2. Dlaczego? H2 udaje inne bazy, ale nie jest nimi: inne typy (enum, timestamp), inne
    //   funkcje, inna obsługa ALTER. Migracja, która przechodzi na H2, może paść na PostgreSQL. Dlatego w
    //   s11_testing/Test06Testcontainers uruchamiamy migracje i validate na prawdziwym PostgreSQL w kontenerze.
    //
    // DOBRA PRAKTYKA: plik ze skryptem generuj do katalogu builda (target/, build/), nie do src/main/resources.
    //   Dlaczego? Flyway uznałby go za migrację, a to ma być tylko materiał do przeczytania.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Flyway = właściciel schematu (V1, V2… w db/migration/…), Hibernate = kontroler ({@code ddl-auto=validate}).
     *   • Kolejność startu: flywayInitializer → entityManagerFactory (depends-on dopisuje Boot).
     *   • Rozjazd → SchemaManagementException: „missing column […] in table […]”, „wrong column type … found […],
     *     but expecting […]” — aplikacja nie startuje.
     *   • ddl-auto: none / validate (produkcja z Flyway) / update (nigdy na produkcji) / create / create-drop (nauka).
     *   • update w Hibernate 7: dodaje, ZMIENIA typy, nie usuwa, błędy DDL tylko loguje — brak historii.
     *   • Nowa kolumna = nowy plik migracji (DEFAULT dla NOT NULL na istniejących danych) + pole w encji, jeden commit.
     *   • Nigdy nie edytuj wykonanej migracji (checksum).
     *   • scripts.action=create + create-target → szkic DDL do przeczytania, nie gotowa migracja.
     *   • H2 ≠ produkcyjna baza → Testcontainers (s11).
     *
     * PYTANIA KONTROLNE:
     *   1. Kto w tej lekcji tworzy tabele, a kto je tylko sprawdza? Skąd wiadomo, że migracje wykonają się przed
     *      sprawdzeniem?
     *   2. Co się stanie? Do encji dodajesz pole {@code private String color;}, ale zapominasz o migracji. Aplikacja
     *      ma {@code ddl-auto=validate}.
     *   3. ZNAJDŹ BŁĄD: {@code ALTER TABLE product ADD COLUMN weight_grams INTEGER NOT NULL;} w migracji na tabeli,
     *      która ma już wiersze.
     *   4. Co się stanie? Pole {@code private String stock;} w encji, a kolumna stock w bazie to INTEGER — przy
     *      {@code validate}, a co przy {@code update}?
     *   5. Podaj trzy powody, dla których {@code ddl-auto=update} nie nadaje się na produkcję.
     *   6. ZNAJDŹ BŁĄD: „Dopisałem kolumnę do V1__create_product.sql, bo to przecież ta sama tabela.”
     *   7. Do czego służy {@code jakarta.persistence.schema-generation.scripts.*} i dlaczego wyniku nie wklejasz bez
     *      czytania?
     *   8. Dlaczego zielone testy na H2 nie gwarantują, że migracje zadziałają na PostgreSQL?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — uzupełnij metody poniżej (testy: Jpa06FlywayWithJpaExercisesTest). Każdy test dostaje własną
    // bazę H2, na której migracje lekcji wykonuje samo API Flyway (bez Springa).
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj nazwę pliku migracji wersjonowanej Flyway z numeru i opisu (słowa oddzielone
     * spacjami): {@code (4, "add supplier column")} → {@code "V4__add_supplier_column.sql"}.
     * Podpowiedź: sekcja 4 i s07_data_jdbc/Jdbc04Flyway — litera V, wersja, DWA podkreślniki, opis z podkreślnikami
     * zamiast spacji, rozszerzenie .sql.
     */
    static String exercise1MigrationFileName(int version, String description) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć nazwy kolumn tabeli (małymi literami) w kolejności z bazy — tak, jak sprawdzisz,
     * co naprawdę zrobiła migracja. Dla {@code "PRODUCT"} po V3: id, name, category, price, stock, created_at,
     * weight_grams.
     * Podpowiedź: {@code JdbcClient.create(dataSource)} (s07_data_jdbc/Jdbc01JdbcClient), tabela
     * {@code information_schema.columns}, kolumny {@code column_name}, {@code table_name}, {@code ordinal_position}.
     */
    static List<String> exercise2Columns(DataSource dataSource, String table) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): napisz treść migracji V4 jako listę poleceń SQL: dodaj kolumnę {@code supplier}
     * (dostawca) VARCHAR(100), która MOŻE być pusta, i ustaw w niej {@code 'Nowak'} wszystkim produktom kategorii
     * COMPUTERS. Test wykona polecenia po kolei na bazie po V3.
     * Podpowiedź: sekcja 4 — {@code ALTER TABLE … ADD COLUMN …}, potem {@code UPDATE … WHERE category = …}.
     * Dlaczego kolumna NULL? Bo pozostałe produkty dostawcy nie mają.
     */
    static List<String> exercise3SupplierMigration() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć wersje migracji z {@code classpath:db/migration/jpa06}, których baza jeszcze
     * NIE wykonała (np. {@code ["3"]} dla bazy zmigrowanej do V2). Tak można przed wdrożeniem sprawdzić, co się wykona.
     * Podpowiedź: {@code Flyway.configure().dataSource(dataSource).locations(…).load().info().pending()} — tablica
     * {@code MigrationInfo}; wersję daje {@code getVersion().getVersion()}.
     */
    static List<String> exercise4PendingVersions(DataSource dataSource) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Tabele tworzy Flyway (migracje V1–V3), Hibernate z {@code ddl-auto=validate} je tylko sprawdza. Kolejność
     *      gwarantuje Boot: bean entityManagerFactory ma depends-on {@code flywayInitializer} (sprawdzone testem).
     *   2. Aplikacja nie wystartuje: BeanCreationException (entityManagerFactory) ← SchemaManagementException
     *      „Schema validation: missing column [color] in table [product]”. Sprawdzone analogicznym testem
     *      (weight_grams przy migracjach do V2). Naprawa: migracja V4 z ALTER TABLE … ADD COLUMN color.
     *   3. NOT NULL bez DEFAULT na tabeli z wierszami — istniejące wiersze musiałyby dostać NULL, więc polecenie pada
     *      („NULL not allowed for column” — ten sam błąd widać w teście update z sekcji 3). Poprawka: {@code DEFAULT 0}
     *      albo trzy kroki: kolumna NULL → UPDATE danych → SET NOT NULL.
     *   4. validate: aplikacja nie startuje — „wrong column type encountered in column [stock] … found [integer …],
     *      but expecting [varchar(255) …]”. update (Hibernate 7.4): aplikacja startuje, a Hibernate po cichu zmienia typ
     *      kolumny na varchar(255) ({@code alter column stock set data type varchar(255)}). Oba sprawdzone testem.
     *   5. Brak historii i code review; różne schematy na różnych środowiskach; potrafi zmienić typ kolumny z danymi,
     *      a nie usuwa starych kolumn; błędy DDL tylko loguje (awaria przy pierwszym zapytaniu); nie przenosi danych.
     *   6. Wykonanej migracji nie wolno zmieniać: Flyway wykryje inną sumę kontrolną (checksum mismatch) i nie
     *      wystartuje, a bazy, które już wykonały V1, kolumny i tak nie dostaną. Trzeba nowej migracji (V4__…).
     *   7. Zapisuje do pliku DDL, który Hibernate wygenerowałby z encji — szkic pierwszej migracji. Trzeba go przeczytać,
     *      bo ma typy specyficzne dla bazy (enum w H2), nie zna ograniczeń spoza adnotacji (UNIQUE z V1), nie ma
     *      DEFAULT, indeksów ani danych (sprawdzone testem — porównanie z naszą V1).
     *   8. H2 to inna baza: inne typy, funkcje i zachowanie ALTER; „tryb zgodności” nie jest PostgreSQL-em. Migracje
     *      i validate trzeba uruchomić na tej samej bazie co produkcja — Testcontainers (s11_testing/Test06Testcontainers).
     */
    // </editor-fold>
}

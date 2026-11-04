package s07_data_jdbc.jdbc03_sql_init;

import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.List;

/**
 * <pre>
 * TEMAT: Skrypty SQL przy starcie aplikacji — schema.sql, data.sql i spring.sql.init.*
 *        (initialization = inicjalizacja, przygotowanie bazy; schema = schemat, czyli struktura tabel (DDL);
 *         data = dane (DML); script = skrypt; mode = tryb; platform = platforma, rodzaj bazy)
 *
 * W SKRÓCIE:
 *   Boot potrafi przy starcie wykonać skrypty SQL: najpierw skrypty SCHEMATU (CREATE TABLE…), potem skrypty
 *   DANYCH (INSERT…). Sterują tym ustawienia {@code spring.sql.init.*}. Domyślnie skrypty wykonują się
 *   tylko na bazie WBUDOWANEJ (H2 w pamięci) — na prawdziwej bazie (PostgreSQL) Boot ich NIE tknie,
 *   i to jest celowe zabezpieczenie. To wygodne do nauki i prototypów, ale w prawdziwych projektach
 *   strukturę bazy prowadzi się migracjami (Flyway — następna lekcja), bo skrypty nie mają historii ani wersji.
 *
 * ANALOGIA: przygotowanie sali przed wykładem.
 *   Najpierw ktoś ustawia krzesła i stoły (schemat), dopiero potem kładzie na nich materiały (dane) — odwrotnie
 *   się nie da. Woźny robi to sam tylko w Twojej własnej sali ćwiczeń (baza wbudowana). W sali konferencyjnej
 *   hotelu (prawdziwa baza) bez wyraźnego polecenia „ustaw sale” nie ruszy niczego — i słusznie, bo mogą tam
 *   już stać cudze rzeczy.
 *
 * JAK TO DZIAŁA:
 *   1. Autokonfiguracja bazy tworzy inicjalizator skryptów (w Boot 4: pakiet org.springframework.boot.sql.init,
 *      jar spring-boot-sql; ustawienia czyta SqlInitializationProperties) — uruchamia się PRZED beanami,
 *      które używają DataSource (np. JdbcClient).
 *   2. Sprawdza tryb {@code spring.sql.init.mode}: always / embedded / never.
 *   3. Wykonuje skrypty z {@code schema-locations}, a potem z {@code data-locations}. Kolejność ustawień w pliku
 *      nie ma znaczenia — schemat zawsze wygrywa kolejnością wykonania.
 *   4. Błąd w skrypcie → wyjątek i aplikacja się nie uruchamia (chyba że continue-on-error=true).
 *   5. W testach adnotacja @Sql robi to samo dla pojedynczego testu (pod spodem ResourceDatabasePopulator).
 *
 * SŁÓWKA:
 *   DDL = Data Definition Language (CREATE/ALTER/DROP — struktura); DML = Data Manipulation Language
 *   (INSERT/UPDATE/DELETE — dane); embedded database = baza wbudowana (w tym samym procesie, np. H2 w pamięci);
 *   idempotent = idempotentny (wielokrotne wykonanie daje ten sam skutek co jednokrotne); populator = „wypełniacz”.
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc01JdbcClient (korzysta z tych skryptów, a testy się wycofują — rollback),
 *             s07_data_jdbc/Jdbc04Flyway (migracje zamiast schema.sql),
 *             s03_boot_basics/Boot01AutoConfiguration (skąd się bierze autokonfiguracja inicjalizatora),
 *             t29_jdbc_databases/Jdbc01SqlBasics (składnia SQL: CREATE TABLE, INSERT).
 * </pre>
 */
public final class Jdbc03SqlInit {

    private Jdbc03SqlInit() {
    }

    // =================================================================================================
    // 1. USTAWIENIA spring.sql.init.* — mode, schema-locations, data-locations, continue-on-error, platform
    // =================================================================================================
    //
    // Nazwy i wartości odczytane z metadanych Boot 4.1.1 (jar spring-boot-sql):
    //   spring.sql.init.mode               always | embedded (DOMYŚLNIE) | never
    //   spring.sql.init.schema-locations   lista skryptów schematu, np. classpath:jdbc03/schema.sql
    //   spring.sql.init.data-locations     lista skryptów danych
    //   spring.sql.init.continue-on-error  false (DOMYŚLNIE) — true: błędne polecenie jest pomijane, reszta leci dalej
    //   spring.sql.init.platform           domyślnie "all"; używane w DOMYŚLNYCH lokalizacjach (patrz niżej)
    //   spring.sql.init.encoding / separator / username / password — kodowanie plików, separator poleceń (;),
    //                                      osobny użytkownik do skryptów
    //
    // DOMYŚLNE LOKALIZACJE (gdy nie ustawisz schema-/data-locations): w KORZENIU classpath (src/main/resources)
    //   schema.sql i schema-${platform}.sql, oraz data.sql i data-${platform}.sql.
    // W tym kursie lekcje dzielą jeden classpath, więc NIE kładziemy schema.sql w korzeniu (uruchomiłby się
    // w każdej lekcji) — każda lekcja wskazuje własne pliki: classpath:jdbc03/schema.sql. W prawdziwym projekcie
    // wystarczy plik schema.sql w src/main/resources i nic więcej.
    //
    // PLATFORM: ustawienie spring.sql.init.platform=xyz dokłada do domyślnych lokalizacji pliki schema-xyz.sql
    // i data-xyz.sql (z korzenia classpath). Dzięki temu jeden projekt może mieć osobne skrypty dla H2 i dla innej
    // bazy. Działa TYLKO dla domyślnych lokalizacji — w jawnie podanych schema-locations znacznik ${platform} nie
    // jest podstawiany (test pokazuje, że wtedy Boot szuka dosłownego pliku i go nie znajduje). Nasz test używa
    // platformy "jdbc03platform" i plików schema-jdbc03platform.sql / data-jdbc03platform.sql w korzeniu resources.
    //
    // Test sekcji 1: ten sam plik ustawień — zwykły start tworzy tabelę i wstawia 3 produkty; mode=never nie robi
    // nic (brak tabeli); start bez skryptów w ogóle kończy się błędem „Table PRODUCT not found”;
    // platforma wczytuje pliki z korzenia; spring.sql.init.enabled=false NIE wyłącza skryptów w Boot 4.1.1
    // (wyłącza je mode=never). To ustawienie jest przestarzałe (deprecated — oznaczone do usunięcia) od Boot 2.5.1;
    // metadane Boota podają jako następcę spring.sql.init.mode. Spotkasz je w starych tutorialach — nie używaj go.
    //
    // PUŁAPKA: zawsze podawaj schema-locations/data-locations z prefiksem classpath: (albo file:). Dlaczego?
    //   Bez prefiksu ścieżka jest interpretowana względem kontekstu aplikacji i łatwo o plik, którego nie znajdziesz.
    //   Brak pliku pod jawnie podaną lokalizacją to błąd „No schema scripts found at location …” przy starcie
    //   (żeby literówka nie przeszła cicho). Lokalizacja z prefiksem optional: jest pomijana, gdy pliku nie ma.
    //
    // DOBRA PRAKTYKA: nie zmieniaj niczego w wspólnych ustawieniach „na próbę” — trzymaj ustawienia lekcji w osobnym
    //   pliku (application-jdbc03.properties). Dlaczego? Wspólny application.properties działa na WSZYSTKIE lekcje modułu.

    // =================================================================================================
    // 2. KOLEJNOŚĆ — schemat przed danymi
    // =================================================================================================
    //
    // Skrypty z schema-locations wykonują się ZAWSZE przed skryptami z data-locations. Powód oczywisty:
    // INSERT do tabeli, której jeszcze nie ma, się nie uda. Test: ustawiamy tylko data-locations (bez schematu)
    // → start aplikacji kończy się błędem; w jego przyczynie jest ScriptStatementFailedException
    // („Table "PRODUCT" not found”).
    //
    // Zapowiedź s08: gdy tabele tworzy Hibernate (JPA), skrypty data.sql domyślnie wykonują się PRZED Hibernate —
    // INSERT do jeszcze nie istniejących tabel. Jest na to osobne ustawienie opóźniające skrypty (spring.jpa…,
    // dokładną nazwę w Boot 4 sprawdzimy w s08_data_jpa). Pamiętaj tylko: kolejność skryptów to sprawa
    // inicjalizatora, a kolejność względem Hibernate — osobny temat.
    //
    // PUŁAPKA: INSERT bez jawnych id w data.sql, jeśli te same dane wstawiasz kilka razy (np. skrypt + @Sql).
    //   Dlaczego? Id nadaje baza, więc każde wykonanie dodaje kolejne wiersze (duplikaty biznesowe). Pilnuj,
    //   które skrypty wykonują się dla danego testu.

    // =================================================================================================
    // 3. @Sql W TESTACH — dane testowe per test
    // =================================================================================================
    //
    // Zamiast zmieniać wspólne data.sql, każdy test deklaruje, jakich danych potrzebuje:
    //   @Sql("/jdbc03/extra-products.sql")                          → skrypt z classpath (src/test/resources)
    //   @Sql(statements = "DELETE FROM product")                    → polecenia wpisane w adnotacji
    //   @Sql(scripts = "…", executionPhase = AFTER_TEST_METHOD)     → PO teście (domyślnie BEFORE_TEST_METHOD)
    //   @SqlGroup({@Sql(…), @Sql(…)}) albo dwa @Sql obok siebie     → kilka skryptów (@Sql jest powtarzalna)
    //   @SqlConfig(errorMode = CONTINUE_ON_ERROR, transactionMode = ISOLATED)  → ustawienia wykonania
    // Adnotację można położyć na klasie (dla wszystkich testów) albo na metodzie; adnotacja na metodzie ZASTĘPUJE
    // tę z klasy (domyślny tryb scalania — zmienisz go przez @SqlMergeMode).
    //
    // GDZIE LEŻĄ DANE? W src/test/resources — widoczne tylko w testach. Ścieżka zaczynająca się od / jest od korzenia
    // classpath; bez / — względem pakietu klasy testowej.
    //
    // CZY TO SIĘ „BRUDZI”? W @JdbcTest (każdy test w transakcji, którą Spring wycofuje) @Sql wykonuje się W TEJ
    // SAMEJ transakcji (transactionMode = INFERRED), więc po teście znika razem z resztą. Dlatego następny test
    // widzi znowu tylko dane z data.sql. (Z transactionMode = ISOLATED skrypt zatwierdza się osobno i zostaje
    // w bazie — używaj ostrożnie.) Test sekcji 3 sprawdza: dane z @Sql są widoczne w teście, a w następnym
    // znika; executionPhase = AFTER_TEST_METHOD wykonuje się dopiero po metodzie (sprawdza to nasz własny
    // nasłuchiwacz testów).
    //
    // POD SPODEM @Sql używa ResourceDatabasePopulator — tej samej klasy, której możesz użyć w kodzie:
    //   new ResourceDatabasePopulator(new ClassPathResource("jdbc03/extra-products.sql")).execute(dataSource);
    // (To samo robi Boot przy spring.sql.init.*. Ćwiczenie 4 korzysta z populatora.)
    //
    // DOBRA PRAKTYKA: dane specyficzne dla testu trzymaj przy teście (@Sql), a wspólny data.sql ogranicz do minimum,
    //   którego wszystkie testy faktycznie potrzebują. Dlaczego? Gdy test potrzebuje 3 konkretnych klientów,
    //   czytelnik testu chce widzieć, kim są — nie szukać ich w pliku ładowanym dla całej aplikacji.

    // =================================================================================================
    // 4. PUŁAPKI — mode=embedded na prawdziwej bazie i skrypt bez IF NOT EXISTS
    // =================================================================================================
    //
    // a) mode=embedded (DOMYŚLNY) sprawdza, czy baza jest WBUDOWANA. Test używa H2 w TRYBIE PLIKOWYM
    //    (jdbc:h2:file:…) — dla Boota to już nie jest baza wbudowana (wbudowana = H2/HSQL/Derby w pamięci), więc
    //    zachowuje się jak PostgreSQL na serwerze: skrypty NIE wykonują się, a pierwsze zapytanie kończy się
    //    „Table PRODUCT not found”. Dlaczego to celowe? Gdyby Boot po cichu uruchamiał CREATE TABLE/INSERT na
    //    produkcyjnej bazie przy każdym starcie, mógłby dublować dane albo nadpisać cudzą strukturę. Chcesz wykonać
    //    skrypty także tam? Świadomie ustaw mode=always — ale zobacz punkt b).
    //
    // b) mode=always na TRWAŁEJ bazie + skrypt bez IF NOT EXISTS: pierwszy start działa, DRUGI wywala się na
    //    „Table PRODUCT already exists” (ScriptStatementFailedException), bo tabela została w pliku bazy po poprzednim
    //    uruchomieniu. Test uruchamia aplikację dwa razy na tym samym pliku. Poprawka: CREATE TABLE IF NOT EXISTS
    //    (schema-safe.sql) — wtedy kolejne starty przechodzą. Dane (INSERT) nadal się dublują, więc idempotentne
    //    dane wymagają dodatkowej pracy (MERGE/UPSERT) — kolejny powód, żeby przejść na migracje.
    //
    // c) continue-on-error=true: błędne polecenie (np. INSERT łamiący klucz główny) jest pominięte, a reszta
    //    skryptu wykonana. Test: data-with-error.sql ma 3 INSERT-y, środkowy się nie uda → bez continue aplikacja
    //    nie startuje, z continue powstają 2 produkty. UWAGA: to maskuje prawdziwe błędy — używaj tylko do
    //    skryptów, które świadomie mogą się częściowo nie udać.
    //
    // PUŁAPKA: spring.sql.init.mode=always „na chwilę” w pliku używanym na produkcji. Dlaczego? Zostanie na zawsze,
    //   a każdy restart uruchomi skrypty na prawdziwych danych (błąd „already exists” w najlepszym razie, zdublowane
    //   albo skasowane dane w najgorszym).
    //
    // DOBRA PRAKTYKA: skrypt DDL pisz tak, żeby dało się go uruchomić drugi raz (IF NOT EXISTS), nawet jeśli dziś
    //   działa tylko na bazie w pamięci. Dlaczego? Wystarczy zmiana adresu bazy w ustawieniach i założenie
    //   „zawsze startuję od pustej bazy” przestaje być prawdą.
    //
    // PYTANIE REKRUTACYJNE: dlaczego Spring Boot domyślnie nie wykonuje schema.sql na bazie PostgreSQL?

    // =================================================================================================
    // 5. DLACZEGO W PRAWDZIWYCH PROJEKTACH schema.sql ZASTĘPUJĄ MIGRACJE
    // =================================================================================================
    //
    // schema.sql to jeden plik „stan końcowy”. Brakuje w nim wszystkiego, co potrzebne po pierwszym wdrożeniu:
    //   • BRAK HISTORII: nie wiesz, co zmieniono, kiedy i dlaczego (poza historią gita);
    //   • BRAK WERSJI: baza nie „wie”, na jakim etapie jest — więc nie wiadomo, co jeszcze trzeba wykonać;
    //   • NIE ZMIENISZ ISTNIEJĄCEJ BAZY: CREATE TABLE nie doda kolumny do tabeli, która już ma dane
    //     (potrzebny ALTER TABLE — wykonany raz i tylko raz, na każdej kopii bazy: dev, test, produkcja);
    //   • BRAK KONTROLI: nikt nie wykryje, że ktoś ręcznie zmienił skrypt już wdrożony.
    // Migracje (Flyway) rozwiązują to wszystko: numerowane pliki V1__…, V2__…, tabela z historią i sumy kontrolne.
    // Do nauki, prototypów i testów skrypty startowe wystarczą; od następnej lekcji (Jdbc04Flyway) i we wszystkich
    // kolejnych działach z bazą używamy Flyway.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Skrypty startowe: schema-locations (DDL) → data-locations (DML); domyślnie schema.sql/data.sql w korzeniu classpath.
     *   • spring.sql.init.mode: embedded (domyślnie: tylko baza wbudowana), always, never.
     *   • continue-on-error=true pomija błędne polecenia; platform dokłada schema-${platform}.sql (domyślne lokalizacje).
     *   • Błąd w skrypcie = aplikacja się nie uruchomi (ScriptStatementFailedException w przyczynie).
     *   • @Sql (scripts / statements / executionPhase / @SqlConfig / @SqlGroup) — dane per test, w transakcji testu.
     *   • ResourceDatabasePopulator — to samo w kodzie.
     *   • mode=always na trwałej bazie → skrypty bez IF NOT EXISTS wywalą drugi start.
     *   • Prawdziwe projekty: migracje Flyway (historia, wersje, zmiana istniejącej bazy).
     *
     * PYTANIA KONTROLNE:
     *   1. Który skrypt wykonuje się pierwszy — schema czy data — i co się stanie, gdy podasz tylko data-locations?
     *   2. Co się stanie? Aplikacja łączy się z bazą H2 w trybie plikowym (jdbc:h2:file:…), a tryb skryptów to domyślny
     *      embedded. Czy tabela z schema.sql powstanie?
     *   3. Dlaczego Boot domyślnie nie wykonuje skryptów na prawdziwej bazie PostgreSQL?
     *   4. Co się stanie? mode=always, trwała baza i schema.sql z {@code CREATE TABLE product …} (bez IF NOT EXISTS) —
     *      aplikacja startuje dwa razy z rzędu.
     *   5. ZNAJDŹ BŁĄD: {@code spring.sql.init.schema-locations=classpath:db/schema-@@platform@@.sql} —
     *      programista liczył, że znacznik platformy zostanie podstawiony. Co się stanie?
     *   6. Co się stanie? W skrypcie danych środkowy z trzech INSERT-ów łamie klucz główny; ustawiono
     *      continue-on-error=true. Ile wierszy trafi do tabeli?
     *   7. Czy dane wstawione przez {@code @Sql} w teście z {@code @JdbcTest} widzi następny test?
     *   8. Wymień trzy powody, dla których w prawdziwym projekcie migracje wygrywają ze schema.sql.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc03SqlInitExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć JEDNO ustawienie w postaci {@code "klucz=wartość"}, dzięki któremu skrypty schematu
     * i danych wykonają się także na bazie plikowej (niewbudowanej). Test uruchamia aplikację na pliku, wskazuje
     * skrypty jdbc03/schema.sql i jdbc03/data.sql, i oczekuje 3 produktów.
     * Podpowiedź: sekcja 1 i 4a — tryb inicjalizacji.
     */
    static String exercise1ModeProperty() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć wartość ustawienia {@code spring.sql.init.schema-locations} (samą wartość, bez klucza),
     * dzięki której aplikacja da się uruchomić DWA razy z rzędu na tym samym pliku bazy (mode=always).
     * Podpowiedź: w katalogu jdbc03 leży gotowy skrypt z IF NOT EXISTS; wartość zaczyna się od {@code classpath:}.
     */
    static String exercise2RepeatableSchemaLocation() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć DWA ustawienia ({@code "klucz=wartość"}), dzięki którym aplikacja uruchomi się mimo
     * błędnego skryptu danych {@code classpath:jdbc03/data-with-error.sql} i będzie miała w tabeli 2 produkty.
     * Podpowiedź: nadpisz data-locations i włącz pomijanie błędów (sekcja 4c).
     */
    static List<String> exercise3ContinueOnErrorProperties() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć tekst skryptu SQL, który po wykonaniu przez {@link ResourceDatabasePopulator}
     * (tak działa @Sql) doda produkty 'Zeszyt' (kategoria 'dom', cena 4.50, stan 40) i 'Teczka' (kategoria 'dom',
     * cena 7.00, stan 10) oraz zmieni stan produktu 'Lampka' na 7. Polecenia oddziel średnikami.
     * Podpowiedź: dwa INSERT-y i jeden UPDATE w jednym Stringu (blok tekstowy {@code """ … """}).
     */
    static String exercise4Script() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): zwróć JEDNO ustawienie w postaci {@code "klucz=wartość"}, dzięki któremu aplikacja
     * (bez profilu jdbc03, na świeżej bazie H2 w pamięci, z domyślnym trybem embedded) wczyta z KORZENIA classpath
     * pliki schema-jdbc03platform.sql i data-jdbc03platform.sql i będzie miała w tabeli 1 produkt.
     * Podpowiedź: sekcja 1 — ustawienie platformy; wartość to część nazwy pliku między {@code schema-} a {@code .sql}.
     */
    static String exercise5PlatformProperty() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Najpierw schema, potem data. Przy samych data-locations INSERT trafia na pustą bazę i aplikacja się nie
     *      uruchamia — w przyczynie jest ScriptStatementFailedException („Table PRODUCT not found”). Sprawdzone testem.
     *   2. Nie. Baza plikowa nie jest dla Boota „wbudowana”, więc w trybie embedded skrypty się nie wykonują;
     *      pierwsze zapytanie kończy się „Table PRODUCT not found”. Sprawdzone testem (sekcja 4a).
     *   3. To zabezpieczenie: na prawdziwej bazie mogą być cudze dane i struktura, a skrypt wykonywany przy każdym
     *      starcie mógłby je zdublować albo zniszczyć. Skrypty mają się wykonać tylko tam, gdzie baza jest „jednorazowa”.
     *   4. Pierwszy start działa. Drugi kończy się błędem „Table PRODUCT already exists” (ScriptStatementFailedException),
     *      bo tabela została w pliku bazy. Rozwiązanie: CREATE TABLE IF NOT EXISTS. Sprawdzone testem (sekcja 4b).
     *   5. Znacznik @@platform@@ w jawnej lokalizacji nie jest podstawiany: Boot szuka dosłownego pliku i startuje
     *      z błędem „No schema scripts found at location …”. Platforma działa tylko w domyślnych lokalizacjach
     *      (schema-${platform}.sql w korzeniu classpath). Sprawdzone testem (sekcja 1).
     *   6. Dwa — błędny INSERT zostaje pominięty, pozostałe się wykonują. Sprawdzone testem (sekcja 4c).
     *   7. Nie. @Sql w @JdbcTest wykonuje się w transakcji testu, którą Spring wycofuje, więc kolejny test widzi
     *      tylko dane startowe. Sprawdzone testem (sekcja 3).
     *   8. Brak historii zmian, brak wersji bazy (nie wiadomo, co jeszcze wykonać) i niemożność bezpiecznej zmiany
     *      istniejącej bazy z danymi (CREATE TABLE nie doda kolumny; potrzebny ALTER wykonany raz); także brak
     *      wykrywania ręcznej zmiany wdrożonego skryptu (sumy kontrolne we Flyway).
     */
    // </editor-fold>
}

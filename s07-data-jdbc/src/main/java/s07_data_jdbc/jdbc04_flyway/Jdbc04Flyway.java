package s07_data_jdbc.jdbc04_flyway;

/**
 * <pre>
 * TEMAT: Flyway — wersjonowane migracje bazy danych
 *        (migration = migracja, krok zmiany schematu; version = wersja; checksum = suma kontrolna;
 *         repeatable = powtarzalna; history = historia; validate = sprawdzić poprawność; clean = wyczyścić)
 *
 * W SKRÓCIE:
 *   Flyway to „system kontroli wersji dla bazy danych”. Każdą zmianę schematu piszesz jako osobny plik SQL
 *   z numerem wersji (V1__..., V2__..., V3__...). Przy starcie aplikacji Flyway sprawdza, które pliki baza już
 *   dostała (pamięta to w tabeli flyway_schema_history), i wykonuje tylko NOWE — po kolei. Dzięki temu baza testowa,
 *   produkcyjna i komputer kolegi dochodzą do tego samego schematu, a każda zmiana ma ślad: kto, kiedy, co.
 *   Od tej lekcji wszystkie działy z bazą w tym kursie używają Flyway.
 *
 * ANALOGIA: dziennik budowy.
 *   Dom stawia się etapami: fundamenty (V1), ściany (V2), dach (V3). Każdy wpis w dzienniku jest podpisany i
 *   datowany. Nie wolno wyrywać kartek ani poprawiać starych wpisów — gdy trzeba zmienić ścianę, dopisujesz NOWY
 *   wpis („V4: przesunięcie ściany”). Kierownik budowy (Flyway) sprawdza przy każdej wizycie, czy stare wpisy
 *   się nie zmieniły, i dopiero potem wykonuje nowe prace.
 *
 * JAK TO DZIAŁA:
 *   1. Starter spring-boot-starter-flyway ciągnie bibliotekę Flyway i autokonfigurację (moduł spring-boot-flyway).
 *   2. Przy starcie, po utworzeniu DataSource, a PRZED beanami korzystającymi z bazy (np. JdbcClient), bean
 *      flywayInitializer woła Flyway.migrate().
 *   3. Flyway skanuje katalog migracji (spring.flyway.locations; domyślnie classpath:db/migration), czyta nazwy
 *      plików i liczy dla każdego sumę kontrolną.
 *   4. Porównuje z tabelą flyway_schema_history: czy wykonane migracje mają te same sumy kontrolne? (jeśli nie —
 *      błąd walidacji i start się nie udaje). Które migracje są nowe? Wykonuje je po kolei i dopisuje do historii.
 *   5. Flyway obsługuje H2 w samej bibliotece flyway-core (osobny moduł niepotrzebny). Flyway 12.4 ostrzega w logu,
 *      że H2 2.4.240 jest NOWSZE niż najnowsze zweryfikowane przez niego (2.3.232) — to tylko ostrzeżenie, wszystko działa.
 *
 * SŁÓWKA:
 *   migration = migracja; version = wersja; description = opis; checksum = suma kontrolna; repeatable = powtarzalna;
 *   validate = zwalidować; repair = naprawić (historię); baseline = punkt odniesienia dla istniejącej bazy;
 *   clean = wyczyścić całą bazę; out of order = poza kolejnością; location = lokalizacja (katalog).
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc03SqlInit (schema.sql — to, co migracje zastępują),
 *             s07_data_jdbc/Jdbc05Repository (repozytorium na tak przygotowanej bazie),
 *             s03_boot_basics/Boot04Starters (nazwy starterów Boota 4),
 *             s02_configuration/Cfg03Profiles (profil lekcji z application-jdbc04.properties).
 * </pre>
 */
public final class Jdbc04Flyway {

    private Jdbc04Flyway() {
    }

    // =================================================================================================
    // 1. MIGRACJE WERSJONOWANE: V1__create_product.sql, V2__..., V3__...
    // =================================================================================================
    //
    //   src/main/resources/db/migration/jdbc04/
    //       V1__create_product.sql        tabela product
    //       V2__insert_products.sql       dane startowe
    //       V3__add_stock_column.sql      nowa kolumna stock
    //       R__expensive_products_view.sql  widok (migracja powtarzalna — sekcja 5)
    //
    // Po starcie baza ma tabelę, dane, kolumnę i widok — bez schema.sql i bez linii Javy. Test sekcji 1.
    // Tabela flyway_schema_history wygląda tak (z uruchomienia, H2 + Flyway 12.4):
    //   -1 | null | << Flyway Schema History table created >> | TABLE | true     ← wpis o utworzeniu samej tabeli historii
    //    1 | 1    | create product        | SQL | true
    //    2 | 2    | insert products       | SQL | true
    //    3 | 3    | add stock column      | SQL | true
    //    4 | null | expensive products view | SQL | true                       ← migracja powtarzalna: wersja NULL
    // (kolumny: installed_rank | version | description | type | success; w tabeli są też m.in. script, checksum,
    // installed_by, installed_on, execution_time).
    //
    // Reguły nazw (WAŻNE — Flyway jest drobiazgowy):
    //   V<wersja>__<opis>.sql   wielka litera V, wersja (liczby rozdzielone podkreślnikiem lub kropką: 1_1 = 1.1),
    //                           DWA podkreślniki, opis ze spacjami zapisanymi jako podkreślniki, rozszerzenie .sql
    //   R__<opis>.sql           migracja powtarzalna (bez wersji)
    // Wersje porównywane są LICZBOWO, nie tekstowo: V2 < V9 < V10 (test sekcji 4).
    //
    // DOBRA PRAKTYKA: jedna migracja = jedna zmiana, opis mówi co robi („add_stock_column”, nie „zmiany”). Dlaczego?
    //   Historia ma być czytelna: po roku ktoś (Ty) musi zrozumieć, skąd w bazie wzięła się dana kolumna.

    // =================================================================================================
    // 2. FLYWAY WYKONUJE TYLKO NOWE MIGRACJE
    // =================================================================================================
    //
    // Drugi start aplikacji na tej samej bazie nic nie wykonuje (historia pokazuje, że V1–V3 są zrobione).
    // Dodajesz V4 → następny start wykona tylko V4 i dopisze rank 5 do historii (rank 4 zajęła migracja powtarzalna).
    // Porównaj z lekcją Jdbc03SqlInit: tam drugi start kończył się błędem „tabela już istnieje” albo zdublowanymi
    // danymi. Test sekcji 2.
    //
    // DOBRA PRAKTYKA: migracje małe — jeden krok, jedna zmiana. Dlaczego? Flyway próbuje uruchomić migrację w
    //   transakcji, ale niektóre bazy zatwierdzają zmiany strukturalne (DDL) automatycznie (zależy od bazy), więc nie
    //   zawsze da się „wycofać” migrację, która padła w środku. Gdy połowa dużej migracji wykona się, a druga nie,
    //   naprawa bywa ręczna; mała migracja ogranicza to ryzyko.

    // =================================================================================================
    // 3. SUMA KONTROLNA — NIGDY NIE EDYTUJ WYKONANEJ MIGRACJI
    // =================================================================================================
    //
    // Flyway zapisuje sumę kontrolną każdego pliku w momencie wykonania. Przy KAŻDYM starcie przelicza ją ponownie.
    // Zmieniłeś choćby jeden znak w już wykonanym V1 → start się nie udaje (FlywayValidateException):
    //
    //   Validate failed: Migrations have failed validation
    //   Migration checksum mismatch for migration version 1
    //   -> Applied to database : 859439963
    //   -> Resolved locally    : -840434892
    //   Either revert the changes to the migration, or run repair to update the schema history.
    //
    // (liczby zależą od treści pliku). Test sekcji 3 robi dokładnie to na kopii migracji w katalogu tymczasowym.
    //
    // PUŁAPKA: „drobna poprawka” w istniejącej migracji (literówka w komentarzu, zmiana białych znaków!).
    //   Dlaczego to groźne? Baza produkcyjna ma stare sumy, więc po wdrożeniu aplikacja NIE WSTANIE, a Ty będziesz
    //   naprawiać historię na produkcji (polecenie repair). Jedyna właściwa droga: dopisać NOWĄ migrację (V4...).
    //   Uwaga dla Windows: końce linii (CRLF/LF, np. po git autocrlf) NIE zmieniają sumy (Flyway liczy ją linia po linii, bez
    //   znaków końca linii) — sprawdzone testem sekcji 3. Ale każda zmiana TREŚCI linii już tak, także spacja na końcu linii.

    // =================================================================================================
    // 4. NAZWY PLIKÓW: BŁĘDNE SĄ POMIJANE W CISZY
    // =================================================================================================
    //
    // Pliki, których nazwa nie pasuje do reguł (V1_create.sql — jedno podkreślenie; v2__lower.sql — mała litera),
    // Flyway po prostu IGNORUJE. W teście sekcji 4: w katalogu leżą trzy pliki, a wykonuje się tylko poprawny V3.
    // Ustawienie spring.flyway.validate-migration-naming=true zmienia to w błąd startu:
    //   „Invalid SQL filenames found: Invalid versioned migration name format: V1_create.sql
    //    (could not recognise version number 1_create) / Unrecognised migration name format: v2__lower.sql”.
    //
    // PUŁAPKA: literówka w nazwie pliku („V2_insert_products.sql”) i „nic się nie dzieje”: aplikacja startuje, a
    //   tabeli lub danych brak. Dlaczego? Flyway nie rozpoznaje pliku jako migracji, więc go pomija bez błędu.
    //   Włącz validate-migration-naming=true — wtedy literówka od razu wyjdzie na jaw.
    //
    // PUŁAPKA: ta sama cisza dotyczy złej ścieżki. Wskazanie nieistniejącego katalogu w spring.flyway.locations
    //   (literówka) kończy się startem bez żadnej migracji, bo domyślnie fail-on-missing-locations=false. Sprawdzone
    //   testem (sekcja 4): historia zawiera wtedy tylko wpis o utworzeniu tabeli. Dlaczego to ważne? Zamiast błędu
    //   dostajesz pustą bazę. Ustaw fail-on-missing-locations=true, a taka literówka zatrzyma start.

    // =================================================================================================
    // 5. MIGRACJE POWTARZALNE (R__) I KOLEJNOŚĆ POZA NUMERACJĄ (out-of-order)
    // =================================================================================================
    //
    // R__expensive_products_view.sql nie ma wersji. Flyway wykonuje ją PO wszystkich wersjonowanych i ponownie za
    // każdym razem, gdy zmieni się jej treść (suma kontrolna). Idealne do rzeczy, które nadpisujesz w całości:
    // widoki, procedury, funkcje (CREATE OR REPLACE ...). Zmiana progu z 1000 na 2000 w teście sekcji 5 dopisuje
    // nowy wiersz w historii (rank 5) — i tym razem edycja jest dozwolona.
    //
    // Out-of-order: do zespołowej pracy. Baza ma już V4, a kolega dopiero dołączył do repozytorium migrację V3_5
    // (numer niższy niż już wykonane). Domyślnie Flyway zgłasza błąd:
    //   „Detected resolved migration not applied to database: 3.5. To ignore this migration, set
    //    -ignoreMigrationPatterns='*:ignored'. To allow executing this migration, set -outOfOrder=true.”
    // Z spring.flyway.out-of-order=true wykona ją mimo to (rank 6 w historii). Test sekcji 6.
    //
    // DOBRA PRAKTYKA: out-of-order to wyjątek, nie reguła. Dlaczego? Kolejność wersji ma znaczenie dla schematu;
    //   przy równoległej pracy ustalcie numerację (np. przez znacznik czasu w wersji) zamiast włączać wyjątek na stałe.

    // =================================================================================================
    // 6. clean — NARZĘDZIE, KTÓRE KASUJE WSZYSTKO
    // =================================================================================================
    //
    // Flyway ma polecenie clean: usuwa WSZYSTKIE obiekty ze schematu (tabele, widoki, dane). Domyślnie jest
    // WYŁĄCZONE (spring.flyway.clean-disabled=true), a wywołanie flyway.clean() kończy się wyjątkiem:
    //   FlywayException: Unable to execute clean as it has been disabled with the 'flyway.cleanDisabled' property.
    // Test sekcji 7. Włączasz je tylko świadomie, w środowisku testowym.
    //
    // PUŁAPKA: włączenie clean-disabled=false „bo tak wygodniej w testach” w pliku wspólnym dla wszystkich środowisk.
    //   Dlaczego? Wystarczy jedno pomyłkowe wywołanie (albo zły profil) i produkcyjna baza jest pusta. Domyślne
    //   wyłączenie to zabezpieczenie projektantów Flyway — nie ruszaj go w konfiguracji, która może trafić na produkcję.

    // =================================================================================================
    // 7. BŁĄD W SKRYPCIE MIGRACJI
    // =================================================================================================
    //
    // Błąd SQL w migracji zatrzymuje start: FlywayMigrateException: „Failed to execute script V2__bad.sql” +
    // numer linii, SQLState i komunikat bazy. Migracja z błędem NIE trafia do historii jako udana. Zanim baza
    // wyrówna się z kodem, napraw plik (lub dopisz nową migrację, zależnie od tego, czy trafiła już na inne środowiska).
    // Test sekcji 8.
    //
    // O baselinie (nie ćwiczymy tutaj): gdy wdrażasz Flyway do ISTNIEJĄCEJ bazy z danymi, ustawienie
    // spring.flyway.baseline-on-migrate=true każe Flyway uznać obecny stan za punkt wyjścia (wersja bazowa) zamiast
    // zgłaszać błąd „niepusty schemat bez historii”.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Starter spring-boot-starter-flyway; migracje w classpath:db/migration (domyślnie), w kursie: db/migration/<lekcja>
     *     przez spring.flyway.locations.
     *   • Nazwy: V<wersja>__<opis>.sql (dwa podkreślniki!), R__<opis>.sql (powtarzalne). Złe nazwy są ignorowane w ciszy,
     *     chyba że spring.flyway.validate-migration-naming=true.
     *   • Historia w flyway_schema_history: wersja, opis, typ, suma kontrolna, sukces. Wykonane migracje nie zmieniają się.
     *   • Zmiana wykonanej migracji → FlywayValidateException „Migration checksum mismatch” — dopisz NOWĄ migrację.
     *   • Migracja powtarzalna (R__) wykonuje się ponownie przy każdej zmianie treści (widoki, procedury).
     *   • out-of-order=true pozwala wykonać migrację o numerze niższym niż już wykonane (wyjątek, nie reguła).
     *   • clean domyślnie wyłączony (clean-disabled=true) — kasuje wszystko, nie włączaj na produkcji.
     *   • Flyway startuje PRZED beanami używającymi bazy; błąd migracji = aplikacja nie wstaje.
     *
     * PYTANIA KONTROLNE:
     *   1. Po co Flyway zapisuje sumę kontrolną każdej wykonanej migracji i kiedy ją sprawdza?
     *   2. Co się stanie? Edytujesz wykonaną już migrację V1 (dopisujesz komentarz) i uruchamiasz aplikację ponownie.
     *   3. Co się stanie? Plik nazywa się V2_insert_products.sql (jedno podkreślenie).
     *   4. ZNAJDŹ BŁĄD: chcesz dodać kolumnę do tabeli utworzonej w V1 — „poprawiasz” plik V1, bo „tak jest
     *      czyściej”. Co powinieneś zrobić zamiast tego i dlaczego?
     *   5. Czym różni się migracja V od R i do czego służy R?
     *   6. W jakiej kolejności wykonają się V10__a.sql, V2__b.sql i V9__c.sql?
     *   7. Co się stanie? Wywołujesz flyway.clean() na świeżo skonfigurowanej aplikacji. Jak to zmienić
     *      i dlaczego domyślnie tak nie jest?
     *   8. ZNAJDŹ BŁĄD: kolega dodał V3_5, a baza ma już V4 — aplikacja nie startuje i pada „Detected resolved
     *      migration not applied to database: 3.5”. Co oznacza ten komunikat i jakie masz opcje?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc04FlywayExercisesTest (migracje lekcji w katalogu tymczasowym, baza H2)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): podaj NAZWĘ PLIKU migracji, która jako wersja 4 tworzy tabelę klientów
     * (opis: „create customer”). Test zapisze w katalogu migracji plik o tej nazwie z poleceniem CREATE TABLE
     * i sprawdzi, że Flyway go rozpoznał (wersja 4, opis „create customer”).
     * Podpowiedź: V + wersja + DWA podkreślniki + opis ze spacjami zapisanymi jako podkreślniki + .sql.
     */
    static String exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): napisz TREŚĆ migracji V4, która dodaje do tabeli product kolumnę
     * description VARCHAR(200) (może być pusta). Nie zmieniaj istniejących migracji — to nowy plik.
     * Test zapisze Twój tekst jako V4__add_description.sql i sprawdzi, że zapytanie o kolumnę działa.
     */
    static String exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć ustawienie (tekst „klucz=wartość”), które pozwoli wykonać migrację V3_5 dodaną
     * już PO wykonaniu V4 (numer niższy niż wykonane). Test uruchomi aplikację z tym ustawieniem
     * (jako argumentem z dwoma myślnikami) i sprawdzi, że wersja 3.5 trafiła do historii.
     */
    static String exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): napisz nową treść migracji POWTARZALNEJ R__expensive_products_view.sql, tak aby widok
     * expensive_product zawierał produkty z ceną co najmniej 300 (zamiast 1000). Kolumny widoku jak dotąd:
     * id, sku, name, price. Test podmieni plik w katalogu migracji i uruchomi aplikację ponownie —
     * ten sam plik powinien zostać wykonany jeszcze raz (zmieniła się jego suma kontrolna).
     * Podpowiedź: CREATE OR REPLACE VIEW ... AS SELECT ...
     */
    static String exercise4() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Żeby wykryć, że wykonana już migracja została zmieniona (a więc baza i kod mogą się rozjechać).
     *      Sprawdza ją przy każdym starcie, przed wykonaniem nowych migracji.
     *   2. Start się nie uda: FlywayValidateException „Migration checksum mismatch for migration version 1”
     *      (suma w bazie różni się od sumy pliku). Sprawdzone testem (sekcja 3).
     *   3. Nic — Flyway nie rozpoznaje pliku jako migracji, pomija go w ciszy, a tabeli/danych brak. Ustaw
     *      validate-migration-naming=true, a pomyłka stanie się błędem. Sprawdzone testem (sekcja 4).
     *   4. Dopisać NOWĄ migrację (np. V4__add_column.sql z ALTER TABLE). V1 jest zapisana w historii z sumą kontrolną
     *      — jej edycja zablokuje start wszędzie tam, gdzie V1 już się wykonała (np. na produkcji).
     *   5. V ma wersję i wykonuje się dokładnie raz; R nie ma wersji i wykonuje się ponownie za każdym razem, gdy zmieni
     *      się jej treść — po wszystkich V. Służy do widoków, procedur, funkcji.
     *   6. V2, V9, V10 — wersje porównywane liczbowo, nie alfabetycznie. Sprawdzone testem (sekcja 4).
     *   7. FlywayException: „Unable to execute clean as it has been disabled with the 'flyway.cleanDisabled' property.”
     *      Zmiana: spring.flyway.clean-disabled=false. Domyślnie wyłączone, bo clean kasuje całą bazę — jedna pomyłka
     *      na produkcji niszczy dane. Sprawdzone testem (sekcja 7).
     *   8. Baza wykonała już wyższą wersję (4), a w katalogu pojawiła się niższa (3.5), której baza nie zna.
     *      Opcje: spring.flyway.out-of-order=true (wykonaj mimo to), ignore-migration-patterns (pomiń ją) albo
     *      — najlepiej — przenumerować migrację na wyższą wersję, jeśli nie trafiła jeszcze na inne środowiska.
     *      Sprawdzone testem (sekcja 6).
     */
    // </editor-fold>
}

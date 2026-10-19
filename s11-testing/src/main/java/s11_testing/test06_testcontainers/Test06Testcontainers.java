package s11_testing.test06_testcontainers;

/**
 * <pre>
 * TEMAT: Testcontainers — prawdziwy PostgreSQL w kontenerze Dockera na czas testu, @ServiceConnection
 *        (container = kontener, odizolowany proces z własnym systemem plików, uruchamiany z obrazu; image = obraz,
 *         „szablon” kontenera, np. postgres:18-alpine; service connection = połączenie z usługą, które Boot tworzy
 *         sam na podstawie kontenera)
 *
 * W SKRÓCIE:
 *   H2 jest szybkie i wygodne, ale to INNA baza niż produkcyjny PostgreSQL. Część błędów widać tylko na prawdziwej
 *   bazie. Testcontainers uruchamia na czas testu prawdziwy PostgreSQL w kontenerze Dockera (losowy port, świeża baza),
 *   a {@code @ServiceConnection} sprawia, że Boot sam podłącza do niego DataSource — bez żadnego spring.datasource.url.
 *   Po testach kontener znika. Bez Dockera testy są POMIJANE ({@code @Testcontainers(disabledWithoutDocker = true)}),
 *   więc budowanie przechodzi u każdego.
 *
 * ANALOGIA: jazda próbna prawdziwym samochodem zamiast symulatora.
 *   Symulator (H2) wystarcza do nauki znaków i skrętów — szybki, zawsze pod ręką. Ale to, jak auto zachowuje się na
 *   mokrej nawierzchni, sprawdzisz tylko prawdziwym samochodem (PostgreSQL). Testcontainers to wypożyczalnia pod domem:
 *   na każdą jazdę próbną dostajesz czyste auto, a po jeździe ktoś je zabiera. Gdy wypożyczalnia jest zamknięta
 *   (brak Dockera), jazdę próbną się odwołuje, a nie oblewa.
 *
 * JAK TO DZIAŁA:
 *   1. {@code @Testcontainers} (rozszerzenie JUnit) znajduje pola {@code @Container}: statyczne uruchamia raz przed
 *      testami klasy i zatrzymuje po nich; niestatyczne — osobno dla każdej metody (wolno!).
 *   2. Kontener startuje z obrazu (pierwszy raz Docker go pobiera), wystawia port 5432 na LOSOWYM porcie hosta;
 *      Testcontainers czeka, aż baza przyjmuje połączenia.
 *   3. {@code @ServiceConnection} (Boot, moduł spring-boot-testcontainers): z kontenera powstaje bean
 *      JdbcConnectionDetails (adres, użytkownik, hasło), a autokonfiguracja DataSource używa go ZAMIAST spring.datasource.*.
 *   4. Pomocniczy kontener Ryuk (testcontainers/ryuk) sprząta kontenery, nawet gdy JVM zostanie zabita.
 *   5. Testcontainers 2.0: klasa {@code org.testcontainers.postgresql.PostgreSQLContainer} (nowy pakiet, bez parametru
 *      typu), artefakty {@code testcontainers-postgresql} i {@code testcontainers-junit-jupiter}. Stara klasa
 *      {@code org.testcontainers.containers.PostgreSQLContainer<SELF>} nadal jest w jarze, ale jest @Deprecated
 *      (sprawdzone w jarze testcontainers-postgresql 2.0.5).
 *
 * SŁÓWKA:
 *   container = kontener; image = obraz; tag = znacznik wersji obrazu; port mapping = mapowanie portów;
 *   service connection = połączenie z usługą; connection details = dane połączenia; disabled = wyłączony (pominięty);
 *   available = dostępny; reaper = „sprzątacz” (Ryuk); abort = przerwać; unsupported = nieobsługiwany.
 *
 * ZOBACZ TEŻ: s11_testing/Test04DataJpaTest (podmiana bazy w wycinku, NON_TEST), s11_testing/Test05TestConfiguration
 *             (@DynamicPropertySource), s11_testing/Test07DockerCompose (baza z Docker Compose przy uruchamianiu),
 *             s07_data_jdbc/Jdbc04Flyway (Flyway), s10_transactions/Tx01Transactional (transakcje),
 *             t29_jdbc_databases/Jdbc04Transactions (transakcje w czystym JDBC).
 * </pre>
 */
public final class Test06Testcontainers {

    private Test06Testcontainers() {
    }

    // =================================================================================================
    // 1. KONTENER + @ServiceConnection
    // =================================================================================================
    //
    //   @Testcontainers(disabledWithoutDocker = true)   // org.testcontainers.junit.jupiter
    //   @SpringBootTest @ActiveProfiles("test06")
    //   class ... {
    //       @Container @ServiceConnection
    //       static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");
    //   }
    //
    // Nie piszesz spring.datasource.url ani hasła — Boot bierze je z kontenera. Test sekcji 1
    // (Test06TestcontainersPostgresTest) sprawdza: wersja bazy zaczyna się od „PostgreSQL 18” (u autora: 18.6), adres
    // to jdbc:postgresql://localhost:<losowy port>/test, schemat i 7 klientów wczytane skryptami lekcji
    // (spring.sql.init.mode=always — PostgreSQL nie jest bazą wbudowaną, więc domyślny tryb by ich nie uruchomił).
    // Starszy sposób (przed Boot 3.1, w wielu tutorialach): @DynamicPropertySource z postgres::getJdbcUrl,
    // getUsername, getPassword — działa, ale @ServiceConnection robi to samo krócej.
    //
    // DOBRA PRAKTYKA: obraz z jawnym tagiem ("postgres:18-alpine"), nigdy "latest". Dlaczego? „latest” zmienia się
    //   bez Twojej wiedzy — jutro test dostanie inną główną wersję bazy i może zacząć padać (albo przechodzić) z powodów,
    //   które nie mają nic wspólnego z Twoim kodem. Najlepiej ta sama wersja co na produkcji.

    // =================================================================================================
    // 2. CYKL ŻYCIA KONTENERA
    // =================================================================================================
    //
    //   static @Container   → jeden kontener na klasę testową (start przed pierwszym testem, stop po ostatnim)
    //   @Container (pole zwykłe) → nowy kontener dla KAŻDEJ metody testowej — kilka sekund na test
    //   kontener jako @Bean (sekcja 5) → żyje tyle, co kontekst Springa (współdzielony przez pamięć podręczną)
    //
    // Test sekcji 2: wszystkie testy klasy widzą ten sam kontener (ten sam port w adresie połączenia).
    //
    // PUŁAPKA: dane z poprzedniego testu w tym samym kontenerze. Dlaczego? Statyczny kontener jest WSPÓLNY dla
    //   wszystkich testów klasy — to, co jeden zatwierdzi (commit), widzi następny. Tak jak przy H2: transakcja testu
    //   z wycofaniem (@DataJpaTest, @JdbcTest) albo sprzątanie po sobie.

    // =================================================================================================
    // 3. H2 A PostgreSQL — BŁĄD, KTÓREGO TEST NA H2 NIE WYKRYJE
    // =================================================================================================
    //
    // CustomerImporter importuje listę klientów w JEDNEJ transakcji: duplikat e-maila łapie (DuplicateKeyException)
    // i „jedzie dalej”. Ten sam test:
    //   • H2 (Test06TestcontainersH2Test): wynik ImportResult(1, 1) — duplikat pominięty, reszta zapisana. Zielono.
    //   • PostgreSQL (Test06TestcontainersPostgresTest): po pierwszym błędzie PostgreSQL odrzuca KAŻDE kolejne
    //     polecenie w tej transakcji. Następny INSERT kończy się: UncategorizedSQLException „… SQL state [25P02];
    //     error code [0]; ERROR: current transaction is aborted, commands ignored until end of transaction block”,
    //     a przyczyną jest pierwotny błąd „duplicate key value violates unique constraint "customer_email_key"”.
    //     Cała transakcja zostaje wycofana — nikt nie jest dodany. (Komunikaty z prawdziwego uruchomienia.)
    // Na produkcji ten import padałby przy pierwszym duplikacie, choć wszystkie testy na H2 były zielone.
    //
    // PUŁAPKA: „mój test na H2 przechodzi, więc SQL jest dobry”. Dlaczego to złudzenie? H2 tylko NAŚLADUJE inne bazy:
    //   inaczej obsługuje błędy w transakcji, typy, funkcje, sortowanie tekstów. Uwaga, nie każda różnica jest tam,
    //   gdzie się jej spodziewasz — porównanie BOOLEAN z liczbą (vip = 1) odrzucają OBIE bazy (H2 2.4: „Values of types
    //   "BOOLEAN" and "INTEGER" are not comparable”; sprawdzone uruchomieniem). Pewność daje tylko test na tej samej
    //   bazie co produkcja.
    //
    // DOBRA PRAKTYKA: logikę bazodanową (zapytania, transakcje, ograniczenia) testuj na tej samej bazie co produkcja;
    //   H2 zostaw do szybkich testów wycinków, w których baza jest tylko tłem. Poprawiony import: ĆWICZENIE 2.

    // =================================================================================================
    // 4. PUŁAPKA: FLYWAY NA PostgreSQL BEZ MODUŁU BAZY
    // =================================================================================================
    //
    // Od Flyway 10 obsługa większości baz (także PostgreSQL) leży w OSOBNYCH modułach. spring-boot-starter-flyway
    // daje tylko flyway-core (obsługuje m.in. H2 — stąd wcześniejsze lekcje działały). Z PostgreSQL start kończy się
    // błędem (Test06TestcontainersFlywayTest, komunikat z uruchomienia):
    //   org.flywaydb.core.api.FlywayException: Unsupported Database: PostgreSQL 18.6
    // Rozwiązanie w prawdziwym projekcie — dopisać zależność (wersję daje BOM Spring Boota):
    //   <dependency>
    //       <groupId>org.flywaydb</groupId>
    //       <artifactId>flyway-database-postgresql</artifactId>
    //   </dependency>
    // Moduł s11-testing celowo jej nie ma — dlatego lekcja tworzy schemat skryptami spring.sql.init. Spring Initializr
    // dodaje ten moduł sam, gdy wybierzesz Flyway + PostgreSQL.

    // =================================================================================================
    // 5. KONTENER JAKO BEAN, WYCINKI I URUCHAMIANIE APLIKACJI Z KONTENERAMI
    // =================================================================================================
    //
    //   @TestConfiguration(proxyBeanMethods = false)
    //   public class Test06Containers {
    //       @Bean @ServiceConnection PostgreSQLContainer postgres() { return new PostgreSQLContainer("postgres:18-alpine"); }
    //   }
    //
    // Wycinek: @JdbcTest + @Import(Test06Containers.class) (Test06TestcontainersSliceTest) — domyślne
    // replace = NON_TEST NIE podmienia bazy z @ServiceConnection, więc wycinek naprawdę działa na PostgreSQL
    // (patrz s11_testing/Test04DataJpaTest, sekcja 5). Kontener-bean żyje tyle, co kontekst — klasy testowe z tą samą
    // konfiguracją dzielą jeden kontener (pamięć podręczna kontekstów).
    //
    // Aplikacja lokalnie z prawdziwą bazą (klasa Test06DevApplication w src/test/java):
    //   SpringApplication.from(Test06Application::main).with(Test06Containers.class).withAdditionalProfiles("test06").run(args);
    // Sprawdzone uruchomieniem: GET /api/database → {"database":"PostgreSQL 18.6"}, GET /api/customers → 7 klientów.
    //
    // PUŁAPKA: SpringApplication.from(...) ustawia dodatkowe profile od nowa — profil dodany w main aplikacji przez
    //   setAdditionalProfiles("test06") przepada (log: „No active profile set, falling back to 1 default profile:
    //   "default"” — sprawdzone uruchomieniem). Dlaczego to groźne? Aplikacja wstaje bez ustawień profilu (tu: bez
    //   schematu i danych). Profil podaj w withAdditionalProfiles(...).

    // =================================================================================================
    // 6. BEZ DOCKERA TESTY SĄ POMIJANE, NIE CZERWONE
    // =================================================================================================
    //
    // @Testcontainers(disabledWithoutDocker = true) to warunek JUnit: gdy Testcontainers nie znajdzie Dockera, cała
    // klasa jest POMINIĘTA z powodem „disabledWithoutDocker is true and Docker is not available” (sprawdzone
    // uruchomieniem w JVM, która nie widziała Dockera). Klasy z kontenerem-beanem (bez pól @Container) też mają tę
    // adnotację — tylko jako warunek. Dzięki temu ./mvnw test przechodzi u każdego, a z Dockerem testuje więcej.
    // Inna możliwość: @EnabledIfDockerAvailable (też org.testcontainers.junit.jupiter) — sam warunek, bez zarządzania
    // polami @Container.
    //
    // PUŁAPKA: test z kontenerem BEZ tego warunku. Dlaczego? Na komputerze bez Dockera (albo na serwerze CI bez
    //   Dockera) kończy się błędem „Could not find a valid Docker environment” — i blokuje całe budowanie.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Testcontainers(disabledWithoutDocker = true) + @Container @ServiceConnection
     *     static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");
     *   • Testcontainers 2.0: org.testcontainers.postgresql.PostgreSQLContainer (stara klasa w .containers — @Deprecated);
     *     artefakty testcontainers-postgresql, testcontainers-junit-jupiter.
     *   • @ServiceConnection = Boot sam robi DataSource z kontenera (bez spring.datasource.*).
     *   • static @Container = jeden na klasę; kontener jako @Bean w @TestConfiguration = jeden na kontekst.
     *   • Wycinki (Boot 4, replace = NON_TEST) NIE podmieniają bazy z @ServiceConnection.
     *   • PostgreSQL przerywa transakcję po błędzie (SQLState 25P02) — H2 nie. Testuj na bazie z produkcji.
     *   • Flyway + PostgreSQL wymaga flyway-database-postgresql (inaczej „Unsupported Database: PostgreSQL …”).
     *   • Aplikacja lokalnie: SpringApplication.from(App::main).with(Kontenery.class).withAdditionalProfiles(...).run(args).
     *   • Obraz zawsze z jawnym tagiem.
     *
     * PYTANIA KONTROLNE:
     *   1. Po co Testcontainers, skoro mamy H2 w pamięci?
     *   2. Co robi @ServiceConnection i czego dzięki niemu nie musisz pisać?
     *   3. Co się stanie? Import klientów w jednej transakcji łapie DuplicateKeyException i wstawia kolejnych klientów —
     *      raz na H2, raz na PostgreSQL.
     *   4. Co się stanie? Do modułu ze starterem Flyway i PostgreSQL w kontenerze włączasz spring.flyway.enabled=true,
     *      nie dodając żadnej innej zależności.
     *   5. Czym różni się static @Container od niestatycznego? Który wybierzesz i dlaczego?
     *   6. ZNAJDŹ BŁĄD: new PostgreSQLContainer("postgres:latest") w teście, który od wczoraj „sam z siebie” pada.
     *   7. ZNAJDŹ BŁĄD: klasa testowa ma @Container, ale nie ma @Testcontainers(disabledWithoutDocker = true). Kolega bez
     *      Dockera zgłasza, że ./mvnw test jest czerwony.
     *   8. Jak uruchomić całą aplikację lokalnie z PostgreSQL w kontenerze i dlaczego taka klasa leży w src/test?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Test06TestcontainersExercisesTest (klasy z kontenerami bez Dockera są pomijane)
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): w klasie Exercise1 utwórz kontener PostgreSQL z obrazu Test06Containers.POSTGRES_IMAGE
    //   (pole static z @Container i @ServiceConnection już jest). Podpowiedź: sekcja 1.
    //
    // ĆWICZENIE 2 (średnie): dokończ SafeCustomerImporter.importAll — import pomijający duplikaty e-maili, który działa
    //   na H2 I na PostgreSQL (testy sprawdzą oba). Podpowiedź: nie prowokuj błędu UNIQUE — najpierw sprawdź
    //   SELECT COUNT(*) ... WHERE email = :email. Klient bez e-maila (null) zawsze jest zapisywany.
    //
    // ĆWICZENIE 3 (trudniejsze): w klasie Exercise3 (@JdbcTest) dołącz konfigurację z kontenerem-beanem i repozytorium
    //   klientów tak, żeby wycinek działał na PostgreSQL. Podpowiedź: sekcja 5, @Import({..., ...}).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. H2 to inna baza — naśladuje PostgreSQL tylko częściowo. Część błędów (obsługa błędów w transakcji, typy,
     *      funkcje, sortowanie) widać dopiero na prawdziwej bazie. Testcontainers daje ją na czas testu: świeżą,
     *      w znanej wersji, bez ręcznej instalacji. Sprawdzone testami sekcji 3.
     *   2. Tworzy z kontenera dane połączenia (JdbcConnectionDetails), z których Boot buduje DataSource. Nie piszesz
     *      spring.datasource.url/username/password ani metody @DynamicPropertySource. Sprawdzone testem sekcji 1.
     *   3. H2: duplikat pominięty, reszta zapisana — ImportResult(1, 1). PostgreSQL: kolejny INSERT po błędzie kończy się
     *      UncategorizedSQLException „SQL state [25P02] … current transaction is aborted, commands ignored until end of
     *      transaction block”, a cała transakcja jest wycofana. Sprawdzone testami sekcji 3.
     *   4. Kontekst nie wstaje: FlywayException „Unsupported Database: PostgreSQL 18.6” (u autora) — obsługa PostgreSQL
     *      jest w osobnym module flyway-database-postgresql. Sprawdzone testem sekcji 4.
     *   5. Statyczny — jeden kontener na wszystkie testy klasy (szybko, ale dane mogą przechodzić między testami);
     *      niestatyczny — nowy kontener dla każdej metody (pełna izolacja, ale kilka sekund na test). Zwykle static
     *      + transakcja z wycofaniem albo sprzątanie po teście.
     *   6. „latest” to ruchomy znacznik — wczoraj wskazywał inną wersję bazy niż dziś. Podaj jawny tag (najlepiej
     *      wersję z produkcji), np. postgres:18-alpine.
     *   7. Bez warunku Testcontainers próbuje znaleźć Dockera i rzuca „Could not find a valid Docker environment” —
     *      test jest czerwony. Z disabledWithoutDocker = true klasa jest pominięta. Sprawdzone uruchomieniem bez Dockera.
     *   8. Klasą z main w src/test/java: SpringApplication.from(Test06Application::main).with(Test06Containers.class)
     *      .withAdditionalProfiles("test06").run(args). Leży w src/test, bo używa zależności testowych (Testcontainers),
     *      których nie ma w jarze produkcyjnym.
     */
    // </editor-fold>
}

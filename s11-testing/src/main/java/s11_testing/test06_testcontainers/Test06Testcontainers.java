package s11_testing.test06_testcontainers;

/**
 * <pre>
 * TEMAT: Testcontainers — testy na prawdziwym PostgreSQL w kontenerze Dockera: @Testcontainers, @Container,
 *        @ServiceConnection, @DynamicPropertySource, kontener współdzielony, Ryuk i aplikacja uruchamiana lokalnie
 *        z kontenerem (SpringApplication.from)
 *        (container = kontener — odizolowany proces z własnym systemem plików, uruchomiony z obrazu;
 *         image = obraz, „szablon” kontenera, np. postgres:17-alpine; service connection = połączenie z usługą)
 *
 * W SKRÓCIE:
 *   H2 jest szybka i wygodna, ale to INNA baza niż PostgreSQL na produkcji. Zapytania natywne, typy, funkcje i nawet
 *   zachowanie transakcji różnią się — test zielony na H2 nie dowodzi, że kod zadziała na PostgreSQL (i odwrotnie).
 *   Testcontainers (biblioteka Java) uruchamia na czas testów PRAWDZIWY PostgreSQL w kontenerze Dockera, a Spring Boot
 *   ({@code @ServiceConnection}) sam łączy z nim aplikację. Bez Dockera takie testy są POMIJANE, więc build zostaje
 *   zielony u każdego.
 *
 * ANALOGIA: jazda próbna.
 *   Symulator jazdy (H2) świetnie uczy zasad i jest tani — możesz w nim jeździć setki razy dziennie. Ale egzamin zdajesz
 *   prawdziwym samochodem (PostgreSQL), z innym sprzęgłem i innymi lusterkami. Testcontainers to wypożyczalnia, która
 *   na każdą lekcję podstawia prawdziwy samochód pod drzwi, a po lekcji go odbiera (Ryuk pilnuje, żeby żaden nie
 *   został na parkingu). Robisz więc dużo jazd na symulatorze i kilka w prawdziwym aucie.
 *
 * JAK TO DZIAŁA:
 *   1. {@code @Testcontainers} (rozszerzenie JUnit 5/6) szuka w klasie pól {@code @Container}: pole statyczne = jeden
 *      kontener na klasę (start przed pierwszym testem, stop po ostatnim), pole zwykłe = kontener na każdy test.
 *      {@code disabledWithoutDocker = true} — gdy Docker nie odpowiada, cała klasa jest POMIJANA.
 *   2. Testcontainers łączy się z Dockerem, pobiera obraz (jeśli go nie ma lokalnie), uruchamia kontener, wystawia port
 *      bazy na LOSOWYM porcie komputera (np. 32771) i czeka, aż baza przyjmuje połączenia.
 *   3. {@code @ServiceConnection} na polu (albo metodzie {@code @Bean}) z kontenerem: Boot tworzy z niego bean
 *      {@code JdbcConnectionDetails} (adres, login, hasło). DataSource, Flyway i JPA biorą dane z niego — wartości
 *      {@code spring.datasource.*} z plików są pomijane.
 *   4. Flyway lekcji (profil test06) wykonuje migracje na PostgreSQL, Hibernate ({@code ddl-auto=validate}) sprawdza
 *      encje. Test działa dokładnie na takim schemacie, jak produkcja.
 *   5. Ryuk (kontener {@code testcontainers/ryuk}) startuje razem z pierwszym kontenerem i usuwa WSZYSTKIE kontenery
 *      tej sesji testów, gdy JVM się zakończy — nawet gdy proces testów zostanie zabity.
 *
 * SŁÓWKA:
 *   Docker = program uruchamiający kontenery; tag = etykieta wersji obrazu (17-alpine); port mapping = przekierowanie
 *   portu kontenera na port komputera; native query = zapytanie natywne (w SQL danej bazy); dialect = dialekt (odmiana
 *   SQL konkretnej bazy); upsert = „wstaw albo pomiń/zaktualizuj”; reaper = żniwiarz, sprzątacz (Ryuk);
 *   singleton container = jeden kontener na wszystkie testy; skipped = pominięty.
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc04Flyway, s08_data_jpa/Jpa04CustomQueries (zapytania natywne),
 *             s08_data_jpa/Jpa06FlywayWithJpa (ddl-auto=validate), s10_transactions/Tx01Transactional,
 *             s11_testing/Test01TestSlices (pamięć podręczna kontekstów), s11_testing/Test04DataJpaTest
 *             (@AutoConfigureTestDatabase), s11_testing/Test05TestConfiguration (@DynamicPropertySource, @Import),
 *             s11_testing/Test07DockerCompose (baza z compose.yaml przy lokalnym uruchamianiu).
 * </pre>
 */
public final class Test06Testcontainers {

    private Test06Testcontainers() {
    }

    // =================================================================================================
    // 1. DLACZEGO H2 ≠ POSTGRESQL (Test06H2DifferencesTest — H2, Test06ServiceConnectionTest — PostgreSQL)
    // =================================================================================================
    //
    // ProductRepository ma trzy zapytania NATYWNE. Hibernate nie tłumaczy ich na dialekt — idą do bazy tak, jak je
    // napisano. Te same metody na dwóch bazach (wyniki sprawdzone testami):
    //
    //   zapytanie                                              H2 2.4                PostgreSQL 17
    //   ---------------------------------------------------------------------------------------------------
    //   INSERT … ON CONFLICT (name) DO NOTHING  (insertIfAbsent)  błąd składni          działa (1 albo 0)
    //   SELECT TOP 3 * … (findTop3MostExpensiveH2Style)           działa                błąd składni
    //   SELECT … LIMIT :n  (findMostExpensive)                    działa                działa
    //   kolejne zapytanie w transakcji po błędzie SQL             działa                „current transaction is aborted”
    //
    // Najgroźniejszy jest wiersz 2: test na H2 jest ZIELONY, a na produkcji zapytanie pada. Wiersz 4 to różnica
    // w zachowaniu, nie w składni: PostgreSQL po każdym błędzie w transakcji odrzuca wszystkie następne polecenia aż do
    // ROLLBACK (test postgresAbortsTransactionAfterError), a H2 pozwala pracować dalej (h2KeepsTransactionAfterError).
    // Kod, który „łapie wyjątek i próbuje dalej w tej samej transakcji”, działa więc na H2, a na PostgreSQL — nie.
    //
    // Inne różnice sprawdzone ręcznie (H2 2.4 kontra PostgreSQL 17): typ {@code jsonb} i operator {@code ->>},
    // {@code DISTINCT ON}, {@code INTERVAL '1 day'} — tylko PostgreSQL. Tryb zgodności H2 ({@code MODE=PostgreSQL})
    // NIE pomaga: {@code ON CONFLICT DO NOTHING}, {@code jsonb} i {@code DISTINCT ON} nadal są błędami.
    //
    // PUŁAPKA: „mamy testy repozytoriów, więc SQL jest sprawdzony”. Dlaczego to złudzenie? Jeśli testy biegną na H2,
    //   sprawdzają SQL w dialekcie H2. Zapytania natywne, migracje z typami specyficznymi dla PostgreSQL i zachowanie
    //   transakcji trzeba sprawdzić na PostgreSQL.
    //
    // DOBRA PRAKTYKA: dwie warstwy testów bazy — szybkie {@code @DataJpaTest} na H2 dla logiki zapytań JPQL i kilka
    //   klas na PostgreSQL z Testcontainers dla SQL natywnego, migracji i ograniczeń bazy. Dlaczego? H2 daje szybkość
    //   (bez Dockera), kontener — pewność. Projekty, które mają tylko PostgreSQL, często rezygnują z H2 całkowicie.

    // =================================================================================================
    // 2. @Testcontainers + @Container + @ServiceConnection (Test06ServiceConnectionTest) I STARY SPOSÓB
    //    @DynamicPropertySource (Test06DynamicPropertySourceTest)
    // =================================================================================================
    //
    //   @DataJpaTest(showSql = false)
    //   @ActiveProfiles("test06")
    //   @Testcontainers(disabledWithoutDocker = true)             // org.testcontainers.junit.jupiter
    //   class Test06ServiceConnectionTest {
    //       @Container                                            // org.testcontainers.junit.jupiter
    //       @ServiceConnection                                    // org.springframework.boot.testcontainers.service.connection
    //       static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    //   }
    //
    // Co sprawdza test: adres JDBC to {@code jdbc:postgresql://…} kontenera, baza to „PostgreSQL” w wersji 17,
    // Flyway wykonał V1 i V2, a w bazie są 4 produkty z migracji.
    //
    // ZMIANY W TESTCONTAINERS 2.x (w poradnikach spotkasz starą wersję 1.x):
    //   • klasa {@code org.testcontainers.postgresql.PostgreSQLContainer} BEZ parametru typu; stara
    //     {@code org.testcontainers.containers.PostgreSQLContainer<?>} jeszcze istnieje, ale jest przestarzała (deprecated),
    //   • artefakty Maven mają przedrostek: {@code testcontainers-postgresql}, {@code testcontainers-junit-jupiter}
    //     (dawniej {@code postgresql}, {@code junit-jupiter}). Wersje pilnuje BOM Boota — w pom.xml modułu bez numerów.
    //
    // Nie ma tu {@code @AutoConfigureTestDatabase(replace = NONE)} — i nie jest potrzebne. Domyślna wartość w Boot 4 to
    // {@code replace = NON_TEST}: wycinek podmienia bazę na wbudowaną TYLKO wtedy, gdy połączenie NIE pochodzi z testu.
    // Baza z {@code @ServiceConnection} (albo z {@code @DynamicPropertySource}) pochodzi z testu, więc zostaje
    // (sprawdzone dla obu). W starszych wersjach Boota domyślne było ANY i bez {@code replace = NONE} test po cichu
    // działał na H2 — dlatego stare poradniki zawsze dopisują NONE.
    //
    // STARY SPOSÓB (Boot przed 3.1, często w poradnikach) — Test06DynamicPropertySourceTest:
    //   {@code @Container static PostgreSQLContainer postgres = ...;}   (bez @ServiceConnection)
    //   {@code @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {}
    //   {@code     registry.add("spring.datasource.url", postgres::getJdbcUrl);}
    //   {@code     registry.add("spring.datasource.username", postgres::getUsername);}
    //   {@code     registry.add("spring.datasource.password", postgres::getPassword);  }}
    // Działa, ale: łatwo zapomnieć o jednej właściwości, a dla każdej usługi (Redis, Kafka, RabbitMQ…) trzeba znać nazwy
    // jej właściwości. {@code @ServiceConnection} robi to za Ciebie dla wszystkich usług, które Boot zna.
    //
    // DOBRA PRAKTYKA: obraz z konkretną wersją ({@code postgres:17-alpine}), taką jak na produkcji, nigdy
    //   {@code postgres:latest}. Dlaczego? „latest” zmienia się bez ostrzeżenia — test, który wczoraj przechodził, dziś
    //   pada (albo przechodzi na wersji, której produkcja nie ma). Wariant {@code -alpine} jest mniejszy, więc szybciej
    //   się pobiera.
    //
    // PUŁAPKA: brak {@code disabledWithoutDocker = true}. Dlaczego? Bez Dockera (laptop bez Docker Desktop, serwer CI bez
    //   Dockera) test nie jest pomijany, tylko pada z „Could not find a valid Docker environment” — cały build jest
    //   czerwony z powodu środowiska, a nie kodu.

    // =================================================================================================
    // 3. FLYWAY NA POSTGRESQL, @DataJpaTest I @SpringBootTest (Test06ServiceConnectionTest, Test06SpringBootPostgresTest)
    // =================================================================================================
    //
    // Migracje lekcji ({@code db/migration/test06}) są napisane w SQL zgodnym z OBIEMA bazami:
    // {@code BIGINT GENERATED BY DEFAULT AS IDENTITY}, {@code NUMERIC(10, 2)}, {@code VARCHAR}, {@code UNIQUE}. Dlatego
    // wystarcza jedna lokalizacja. Gdy migracje muszą użyć typów tylko z PostgreSQL ({@code jsonb}, {@code uuid},
    // {@code text[]}), rozdziel je: wspólne w {@code db/migration/test06/common}, specyficzne w
    // {@code db/migration/test06/postgresql}, a {@code spring.flyway.locations} wskazuje wspólną + właściwą dla bazy
    // (Flyway rozumie też symbol {@code {vendor}} w ścieżce). Uwaga: do PostgreSQL Flyway 10+ potrzebuje modułu
    // {@code flyway-database-postgresql} (jest w pom.xml modułu).
    //
    // Test06SpringBootPostgresTest — CAŁA aplikacja na PostgreSQL:
    //   {@code @SpringBootTest(webEnvironment = RANDOM_PORT)} + {@code @Import(PostgresTestcontainersConfig.class)},
    //   {@code @LocalServerPort} + {@code RestClient}: POST „Ciastka” → 201, ten sam POST drugi raz → 409 Conflict
    //   z ProblemDetail (zapytanie ON CONFLICT na PostgreSQL), GET /top?n=2 → Czajnik, Kawa.
    //   Sprzątanie w {@code @BeforeEach} (usuwamy produkty spoza migracji) — w {@code @SpringBootTest} nie ma
    //   automatycznego rollbacku: żądanie HTTP przechodzi przez prawdziwy serwer w INNYM wątku i zatwierdza swoje
    //   transakcje (s10_transactions/Tx01Transactional).
    //
    // PUŁAPKA: dane zostawione w bazie kontenera przez test {@code @SpringBootTest}. Dlaczego to problem? Kontener żyje
    //   tyle, co kontekst (albo cały przebieg testów — sekcja 4), więc inne testy widzą te wiersze i liczą źle. Sprzątaj
    //   w {@code @BeforeEach}/{@code @AfterEach} albo dawaj danym testu unikalne nazwy.

    // =================================================================================================
    // 4. KONTENER WSPÓŁDZIELONY: SZYBKOŚĆ (PostgresTestcontainersConfig, SharedPostgres,
    //    Test06SharedContainerJpaTest, Test06SharedContainerAppTest) I RYUK
    // =================================================================================================
    //
    // Każdy start PostgreSQL w kontenerze to 1–3 sekundy. Pole {@code @Container static} w KAŻDEJ klasie = osobny
    // kontener dla każdej klasy. Trzy sposoby, żeby było ich mniej:
    //
    //   a) Kontener jako bean — {@code @TestConfiguration} z metodą {@code @Bean @ServiceConnection PostgreSQLContainer}
    //      (PostgresTestcontainersConfig) i {@code @Import} w testach. Spring uruchamia kontener razem z kontekstem
    //      i zatrzymuje przy jego zamknięciu. Klasy o tej samej konfiguracji dzielą kontekst z pamięci podręcznej
    //      (Test01TestSlices, sekcja 4), a więc i kontener. Ten sam plik wykorzystasz w TestTest06Application (sekcja 5).
    //   b) {@code @ImportTestcontainers(SharedPostgres.class)} (org.springframework.boot.testcontainers.context) —
    //      Boot bierze kontenery z pól STATYCZNYCH wskazanej klasy/interfejsu, rejestruje je jako beany i uruchamia.
    //      Pole statyczne = jeden obiekt na całą JVM, więc RÓŻNE konfiguracje ({@code @DataJpaTest} w
    //      Test06SharedContainerJpaTest i {@code @SpringBootTest} w Test06SharedContainerAppTest) mają różne konteksty,
    //      ale TEN SAM kontener — oba testy widzą identyczny adres JDBC.
    //   c) Klasyczny wzorzec „abstract base” ze starych poradników: klasa bazowa z polem statycznym i blokiem
    //      {@code static { POSTGRES.start(); }}, po której dziedziczą testy. Działa, ale wiąże testy dziedziczeniem;
    //      w Boot 3.1+ wygodniejsze są a) i b).
    //
    // PUŁAPKA: dane w kontenerze współdzielonym. Dlaczego? Wszystkie klasy z SharedPostgres piszą do JEDNEJ bazy. Testy
    //   {@code @DataJpaTest} są bezpieczne (rollback), ale {@code @SpringBootTest} zatwierdza zmiany — sprzątaj po sobie
    //   i nie zakładaj dokładnej liczby wierszy, jeśli inne klasy mogą coś dodać.
    //
    // RYUK — kto sprząta kontenery? Testcontainers uruchamia pomocniczy kontener {@code testcontainers/ryuk}. Ryuk
    // trzyma połączenie z procesem testów; gdy połączenie zniknie (koniec JVM, zabicie procesu, awaria IntelliJ),
    // usuwa wszystkie kontenery, sieci i wolumeny tej sesji. Dzięki temu na komputerze nie zostają dziesiątki
    // „osieroconych” baz.
    //
    // PUŁAPKA: wyłączanie Ryuka ({@code TESTCONTAINERS_RYUK_DISABLED=true}) „bo coś nie działa”. Dlaczego nie? Każdy
    //   przerwany przebieg testów zostawia działające kontenery, które zjadają pamięć i porty. Wyłącza się go tylko
    //   tam, gdzie środowisko samo sprząta (np. jednorazowa maszyna CI) — nigdy w kodzie ani w konfiguracji projektu.
    //
    // DOBRA PRAKTYKA: kontener współdzielony (a albo b) zamiast {@code @Container} w każdej klasie. Dlaczego? 20 klas
    //   testowych × 2 s to 40 s samego startowania baz; jeden kontener to 2 s.

    // =================================================================================================
    // 5. APLIKACJA URUCHAMIANA LOKALNIE Z KONTENEREM (TestTest06Application w src/test/java)
    // =================================================================================================
    //
    //   {@code public static void main(String[] args) {}
    //   {@code     SpringApplication.from(Test06Application::main)      // uruchom zwykły main aplikacji}
    //   {@code             .with(PostgresTestcontainersConfig.class)    // i dołóż konfigurację z kontenerem}
    //   {@code             .withAdditionalProfiles("test06")}
    //   {@code             .run(args);}
    //   {@code }}
    //
    // Po co? Programista uruchamia aplikację na swoim komputerze z PRAWDZIWYM PostgreSQL bez instalowania bazy — kontener
    // startuje z aplikacją i znika po jej zatrzymaniu. Klasa leży w src/test/java, bo używa bibliotek testowych
    // (Testcontainers nie trafia do paczki produkcyjnej). Nazwa „Test” + nazwa klasy aplikacji to konwencja Boota.
    //
    // Jak uruchomić:
    //   • IntelliJ: ▶ obok {@code main} w TestTest06Application (Docker musi działać),
    //   • wiersz poleceń: {@code ./mvnw -pl s11-testing spring-boot:test-run
    //     -Dspring-boot.run.main-class=s11_testing.test06_testcontainers.TestTest06Application}
    //     (cel {@code test-run} widzi klasy testowe, zwykłe {@code spring-boot:run} — nie).
    //   Zapytania: {@code s11-testing/http/test06_testcontainers.http}. Ta sama aplikacja uruchomiona zwykłym
    //   Test06Application działa na H2 — i POST kończy się błędem 500 (ON CONFLICT). Sprawdzone ręcznie oba warianty.
    //
    // PUŁAPKA: {@code SpringApplication.from(...)} gubi profil ustawiony w {@code main} przez
    //   {@code app.setAdditionalProfiles("test06")}. Dlaczego? Mechanizm {@code from} wstawia do uruchamianej aplikacji
    //   SWOJĄ listę dodatkowych profili (pustą), zastępując tę z {@code main}. Aplikacja wstała z profilem „default”,
    //   Flyway był wyłączony i pierwszy GET skończył się 500: {@code relation "product" does not exist} (sprawdzone
    //   ręcznie). Dlatego w TestTest06Application jest {@code .withAdditionalProfiles("test06")}.
    //
    // CO, GDY DOCKER NIE DZIAŁA? Klasy z {@code @Testcontainers(disabledWithoutDocker = true)} są POMIJANE
    // (IntelliJ oznacza je jako pominięte, Maven liczy je w „Skipped”), a reszta testów przechodzi — build zielony
    // (sprawdzone ręcznie: przy niedostępnym Dockerze wszystkie testy z kontenerem tej lekcji były pominięte, a testy
    // na H2 przeszły). TestTest06Application bez Dockera nie wstanie: „Could not find a valid Docker environment”.
    // Ciekawostka: Testcontainers zapamiętuje w pliku {@code ~/.testcontainers.properties}, jak ostatnio znalazł Dockera
    // (np. przez gniazdo {@code /var/run/docker.sock}), i próbuje tego sposobu najpierw.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • H2 ≠ PostgreSQL: zapytania natywne, typy (jsonb), składnia (ON CONFLICT, TOP, DISTINCT ON), transakcja po błędzie.
     *     MODE=PostgreSQL w H2 tego nie naprawia.
     *   • Klasa z kontenerem: {@code @Testcontainers(disabledWithoutDocker = true)}; pole
     *     {@code @Container @ServiceConnection static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");}
     *   • Testcontainers 2.x: {@code org.testcontainers.postgresql.PostgreSQLContainer} (bez {@code <?>}).
     *   • {@code @ServiceConnection} zamiast {@code @DynamicPropertySource} z {@code spring.datasource.*}.
     *   • {@code @DataJpaTest} z bazą z testu: domyślne {@code replace = NON_TEST} jej nie podmienia (NONE niepotrzebne).
     *   • Współdzielenie: {@code @TestConfiguration} z {@code @Bean @ServiceConnection} + {@code @Import} (kontener na
     *     kontekst) albo {@code @ImportTestcontainers(Klasa.class)} z polem statycznym (kontener na całą JVM).
     *   • Ryuk sprząta kontenery po zakończeniu JVM — nie wyłączaj go w projekcie.
     *   • Lokalnie: {@code SpringApplication.from(App::main).with(Config.class).withAdditionalProfiles(...).run(args)} w
     *     src/test/java; {@code ./mvnw spring-boot:test-run}.
     *   • Konkretny tag obrazu (17-alpine), nigdy latest.
     *
     * PYTANIA KONTROLNE:
     *   1. Podaj dwa przykłady zapytań, które zachowują się inaczej na H2 i na PostgreSQL. Który kierunek różnicy jest
     *      groźniejszy i dlaczego?
     *   2. Co się stanie? W teście {@code @DataJpaTest} na PostgreSQL wywołujesz zapytanie z błędem składni, łapiesz
     *      wyjątek i w tym samym teście wywołujesz poprawne zapytanie. A na H2?
     *   3. Co robi {@code @ServiceConnection} i jakie właściwości zastępuje?
     *   4. Co się stanie, gdy uruchomisz {@code ./mvnw test} na komputerze bez Dockera? A gdy usuniesz
     *      {@code disabledWithoutDocker = true}?
     *   5. ZNAJDŹ BŁĄD: test ma {@code @DataJpaTest} i {@code @Container @ServiceConnection static PostgreSQLContainer}.
     *      Kolega dopisuje „dla pewności” {@code @AutoConfigureTestDatabase(replace = ANY)}. Na jakiej bazie biegnie teraz test?
     *   6. Czym różni się kontener z pola {@code @Container static} od kontenera z {@code @ImportTestcontainers} z polem
     *      statycznym w osobnym interfejsie, gdy masz dwie klasy testowe o RÓŻNEJ konfiguracji?
     *   7. Czym jest Ryuk i dlaczego nie wyłączamy go w projekcie?
     *   8. ZNAJDŹ BŁĄD: TestTest06Application to {@code SpringApplication.from(Test06Application::main)
     *      .with(PostgresTestcontainersConfig.class).run(args)}, a {@code main} aplikacji ustawia profil test06. Aplikacja
     *      wstaje, ale GET kończy się 500 „relation "product" does not exist”. Dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — piszesz TESTY (Docker musi działać). Szkielet: Test06TestcontainersExercisesTest (@Tag("cwiczenie")),
    // rozwiązania: s11_testing.solutions.test06_testcontainers.Test06TestcontainersSolutionsTest (@Tag("wzorzec"))
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): {@code @DataJpaTest} ze wspólnym kontenerem ({@code @ImportTestcontainers(SharedPostgres.class)})
    //   — sprawdź, że {@code SELECT version()} zaczyna się od „PostgreSQL 17”. Podpowiedź: sekcja 4 b), JdbcClient.
    //
    // ĆWICZENIE 2 (łatwe): na PostgreSQL {@code insertIfAbsent("Herbata", …)} zwraca 0, {@code insertIfAbsent("Sok", …)}
    //   zwraca 1, a produktów jest 5. Podpowiedź: sekcja 1, Herbata jest w migracji V2.
    //
    // ĆWICZENIE 3 (średnie): pokaż, że na PostgreSQL {@code findTop3MostExpensiveH2Style} rzuca
    //   InvalidDataAccessResourceUsageException, a {@code findMostExpensive(3)} zwraca Czajnik, Kawa, Kubek.
    //   Podpowiedź: dwa OSOBNE testy — sekcja 1, wiersz 4 tabeli.
    //
    // ĆWICZENIE 4 (średnie): stary sposób — {@code @Container} BEZ {@code @ServiceConnection} i trzy właściwości
    //   {@code spring.datasource.*} w {@code @DynamicPropertySource}; w bazie są 4 produkty. Podpowiedź: sekcja 2.
    //
    // ĆWICZENIE 5 (trudniejsze): test od końca do końca: {@code @SpringBootTest(webEnvironment = RANDOM_PORT)} na PostgreSQL
    //   — POST „Sok” → 201, drugi raz → 409, GET /test06/products/top?n=1 → Czajnik. Podpowiedź: sekcja 3; w pakiecie
    //   rozwiązań {@code @Import} musi dołączyć też ProductService i ProductController (aplikacja rozwiązań ich nie skanuje).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. {@code INSERT … ON CONFLICT DO NOTHING} działa tylko na PostgreSQL (H2: błąd składni); {@code SELECT TOP 3}
     *      działa tylko na H2 (PostgreSQL: błąd składni); także jsonb, DISTINCT ON, INTERVAL '1 day'. Groźniejszy jest
     *      kierunek „działa na H2, nie działa na PostgreSQL” — test jest zielony, a produkcja pada. Sprawdzone:
     *      Test06H2DifferencesTest i Test06ServiceConnectionTest.
     *   2. PostgreSQL po błędzie przerywa transakcję: drugie (poprawne) zapytanie też pada z „current transaction is
     *      aborted, commands ignored until end of transaction block”. Na H2 drugie zapytanie działa normalnie.
     *      Sprawdzone: postgresAbortsTransactionAfterError i h2KeepsTransactionAfterError.
     *   3. Tworzy z kontenera bean z danymi połączenia ({@code JdbcConnectionDetails}): adres JDBC, użytkownika, hasło —
     *      zamiast {@code spring.datasource.url/username/password}; korzystają z niego DataSource i Flyway.
     *      Sprawdzone: Test06ServiceConnectionTest (adres = {@code postgres.getJdbcUrl()}).
     *   4. Klasy z {@code disabledWithoutDocker = true} są pominięte (Skipped), reszta przechodzi — build zielony
     *      (sprawdzone ręcznie z niedostępnym Dockerem). Bez tej flagi testy z kontenerem padają z „Could not find a valid
     *      Docker environment” i build jest czerwony.
     *   5. Na DWÓCH bazach naraz — i dlatego nie wstaje. {@code replace = ANY} każe ZAWSZE podmienić DataSource na H2, ale
     *      Flyway ma z {@code @ServiceConnection} WŁASNE dane połączenia z kontenerem: migracje trafiają do PostgreSQL
     *      (4 produkty), a Hibernate sprawdza pustą H2 — „Schema validation: missing table [product]”. Sprawdzone
     *      ręcznie (z {@code ddl-auto=none}: DataSource to {@code jdbc:h2:mem:…}, a w kontenerze są 4 produkty). Domyślne
     *      NON_TEST zostawia PostgreSQL dla wszystkich.
     *   6. {@code @Container static} — kontener należy do klasy: każda klasa ma swój (start/stop razem z klasą).
     *      {@code @ImportTestcontainers} z polem statycznym w interfejsie — jeden obiekt na JVM, więc obie klasy (różne
     *      konteksty) używają TEGO SAMEGO kontenera. Sprawdzone: Test06SharedContainerJpaTest i Test06SharedContainerAppTest.
     *   7. Pomocniczy kontener Testcontainers, który po zakończeniu (albo zabiciu) JVM usuwa wszystkie kontenery sesji.
     *      Wyłączony zostawiałby po przerwanych testach działające kontenery, które zajmują pamięć i porty.
     *   8. {@code from(...)} zastępuje dodatkowe profile ustawione w {@code main} swoją (pustą) listą — aplikacja ma
     *      profil „default”, Flyway jest wyłączony (wspólny application.properties), tabel nie ma. Naprawa:
     *      {@code .withAdditionalProfiles("test06")}. Sprawdzone ręcznie (sekcja 5).
     */
    // </editor-fold>
}

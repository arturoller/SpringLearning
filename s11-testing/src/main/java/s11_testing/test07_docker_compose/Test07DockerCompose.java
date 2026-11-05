package s11_testing.test07_docker_compose;

/**
 * <pre>
 * TEMAT: Docker Compose w Spring Boot — aplikacja uruchamiana lokalnie razem z bazą z pliku compose.yaml:
 *        spring-boot-docker-compose, ServiceConnection bez spring.datasource.url, cykl życia, skip.in-tests,
 *        test z prawdziwym compose i porównanie z Testcontainers
 *        (compose = „składanie” — plik opisujący kilka usług w kontenerach; service = usługa, np. baza;
 *         lifecycle = cykl życia; working directory = katalog roboczy)
 *
 * W SKRÓCIE:
 *   Aplikacja potrzebuje bazy także wtedy, gdy uruchamiasz ją u siebie (▶ w IntelliJ), a nie tylko w testach.
 *   Zamiast instalować PostgreSQL, opisujesz go w pliku compose ({@code compose-test07.yaml}). Moduł
 *   {@code spring-boot-docker-compose} przy starcie aplikacji sam wykonuje {@code docker compose up}, czeka, aż baza
 *   będzie gotowa, odczytuje z kontenera adres, port, login i hasło i łączy z nią aplikację — w properties nie ma
 *   {@code spring.datasource.url}. Przy zamknięciu aplikacji zatrzymuje kontenery. W testach domyślnie NIE działa —
 *   tam rządzi Testcontainers (Test06Testcontainers).
 *
 * ANALOGIA: stały catering na biurowe spotkania.
 *   Testcontainers to zamawianie pizzy na każde spotkanie osobno: przyjeżdża świeża, po spotkaniu pudełka znikają
 *   (testy). Docker Compose to umowa z kuchnią w budynku: kiedy przychodzisz do pracy (start aplikacji), kuchnia się
 *   otwiera według stałego menu z kartki (compose.yaml), a gdy wychodzisz — zamyka, ale zapasy w lodówce zostają do
 *   jutra (stop zamiast down: dane w bazie przetrwają).
 *
 * JAK TO DZIAŁA:
 *   1. Przy starcie aplikacji DockerComposeListener (z {@code spring-boot-docker-compose}) sprawdza
 *      {@code spring.docker.compose.enabled}; szuka pliku: {@code spring.docker.compose.file} albo w katalogu
 *      roboczym {@code compose.yaml}, {@code compose.yml}, {@code docker-compose.yaml}, {@code docker-compose.yml}.
 *   2. Jeśli usługi nie działają, wykonuje {@code docker compose up --no-color --detach --wait} (--wait czeka na
 *      healthcheck z pliku), a potem sam sprawdza, czy porty przyjmują połączenia (readiness = gotowość).
 *   3. Dla każdej usługi rozpoznaje jej rodzaj po NAZWIE OBRAZU ({@code postgres:…} → PostgreSQL) i tworzy bean
 *      {@code JdbcConnectionDetails}: host, wylosowany port komputera, {@code POSTGRES_DB}, {@code POSTGRES_USER},
 *      {@code POSTGRES_PASSWORD}. DataSource i Flyway biorą dane z niego.
 *   4. Przy zamknięciu aplikacji (lifecycle-management = start-and-stop) wykonuje {@code docker compose stop}
 *      (albo {@code down}, jeśli tak ustawisz {@code stop.command}).
 *   5. Gdy działa w teście (JUnit na stosie wywołań) — nic nie robi ({@code skip.in-tests=true}).
 *
 * SŁÓWKA:
 *   up = uruchom (utwórz i wystartuj kontenery); down = zatrzymaj i USUŃ kontenery (i sieć); stop = tylko zatrzymaj;
 *   healthcheck = sprawdzanie zdrowia (czy usługa działa); readiness = gotowość; detach = odłącz (praca w tle);
 *   skip = pomiń; listener = słuchacz zdarzeń; environment variable = zmienna środowiskowa.
 *
 * ZOBACZ TEŻ: s02_configuration/Cfg03Profiles (profile), s03_boot_basics/Boot03SpringApplication (start aplikacji,
 *             właściwości domyślne), s07_data_jdbc/Jdbc04Flyway, s11_testing/Test05TestConfiguration
 *             (@DynamicPropertySource), s11_testing/Test06Testcontainers (Testcontainers, @ServiceConnection).
 * </pre>
 */
public final class Test07DockerCompose {

    private Test07DockerCompose() {
    }

    // =================================================================================================
    // 1. PLIK compose-test07.yaml I GDZIE BOOT GO SZUKA (Test07MissingFileTest, ComposeFile)
    // =================================================================================================
    //
    // Plik leży w katalogu MODUŁU: {@code s11-testing/compose-test07.yaml}. Najważniejsze linie:
    //   {@code image: postgres:17-alpine}                 — ten sam obraz co w testach (Test06), stała wersja,
    //   {@code environment: POSTGRES_DB / POSTGRES_USER / POSTGRES_PASSWORD} — z nich Boot bierze dane logowania,
    //   {@code ports: - "5432"}                            — port kontenera na LOSOWYM porcie komputera,
    //   {@code healthcheck: pg_isready …}                  — {@code up --wait} czeka, aż baza przyjmuje połączenia.
    //
    // Gdzie Boot szuka pliku? {@code spring.docker.compose.file} to {@code java.io.File}, więc ścieżka WZGLĘDNA liczy
    // się od KATALOGU ROBOCZEGO procesu (bez ustawienia — Boot szuka {@code compose.yaml} itp. w tym katalogu).
    // A katalog roboczy zależy od sposobu uruchomienia:
    //   • {@code ./mvnw -pl s11-testing spring-boot:run} oraz testy w Maven — katalog modułu (s11-testing),
    //   • IntelliJ — to, co jest w Run Configuration → Working directory: katalog modułu ({@code $MODULE_WORKING_DIR$})
    //     albo katalog główny projektu (SpringLearning).
    // Dlatego Test07Application nie wpisuje ścieżki na sztywno, tylko pyta {@code ComposeFile.locate()}: najpierw
    // {@code compose-test07.yaml}, potem {@code s11-testing/compose-test07.yaml} — i ustawia wynik jako właściwość
    // domyślną ({@code app.setDefaultProperties(...)}). Sprawdzone ręcznie oba katalogi: w logu za każdym razem
    // „Using Docker Compose file /…/s11-testing/compose-test07.yaml”.
    //
    // PUŁAPKA: ścieżka, która działa „u mnie”. Dlaczego? {@code spring.docker.compose.file=compose-test07.yaml} działa
    //   z ./mvnw, a w IntelliJ uruchomionym z katalogu projektu pliku „nie ma”. Aplikacja nie wstaje z błędem
    //   {@code 'files' content [...] must exist} — Boot sprawdza plik, ZANIM zawoła Dockera (Test07MissingFileTest
    //   biegnie więc także bez Dockera). Rozwiązania: ścieżka liczona w kodzie (jak tu), ścieżka bezwzględna w zmiennej
    //   środowiskowej albo plik {@code compose.yaml} w katalogu, z którego zawsze uruchamiasz.
    //
    // DOBRA PRAKTYKA: w prawdziwym projekcie (jedna aplikacja w repozytorium) trzymaj {@code compose.yaml} w katalogu
    //   głównym obok pom.xml i nie ustawiaj {@code spring.docker.compose.file} wcale. Dlaczego? Boot znajdzie go sam,
    //   a IntelliJ i Maven uruchamiają z tego samego katalogu. U nas modułów jest wiele i każda lekcja ma własny plik —
    //   stąd dodatkowa właściwość.

    // =================================================================================================
    // 2. CO ROBI BOOT: ServiceConnection BEZ spring.datasource.url, CYKL ŻYCIA, skip.in-tests
    //    (Test07DockerComposeTest, Test07SkipInTestsTest)
    // =================================================================================================
    //
    // W application-test07.properties nie ma ani adresu bazy, ani hasła. Test07DockerComposeTest sprawdza, że:
    //   • {@code spring.datasource.url} jest PUSTE, a mimo to DataSource łączy się z {@code jdbc:postgresql://…/sklep},
    //   • bean {@code JdbcConnectionDetails} pochodzi z Docker Compose (nazwa klasy zawiera „DockerCompose”), a
    //     użytkownik to „sklep” z {@code POSTGRES_USER},
    //   • Flyway lekcji wykonał migracje na tej bazie (3 produkty).
    // To ten sam mechanizm co {@code @ServiceConnection} w Testcontainers (Test06Testcontainers, sekcja 2): dane
    // połączenia przychodzą z kontenera, nie z plików properties.
    //
    // Właściwości {@code spring.docker.compose.*} (sprawdzone w metadanych Boot 4.1.1):
    //   enabled                  true            — czy w ogóle działać (u nas wspólny application.properties: false!)
    //   file                     (brak)          — plik(i) compose; bez niego szukanie w katalogu roboczym
    //   lifecycle-management     start-and-stop  — start-and-stop: up przy starcie, stop przy końcu;
    //                                              start-only: tylko up (kontenery działają dalej po zamknięciu);
    //                                              none: niczego nie uruchamia, tylko łączy się z działającymi
    //   start.skip               if-running      — nie uruchamiaj, jeśli usługi już działają
    //   stop.command             stop            — stop (zostawia kontener z danymi) albo down (usuwa kontener)
    //   skip.in-tests            true            — w testach nic nie rób
    //   readiness.timeout        2m              — jak długo czekać na gotowość usług
    //
    // Wspólny application.properties modułu ustawia {@code spring.docker.compose.enabled=false}. Dlaczego? Moduł jest
    // wspólny dla siedmiu lekcji, a {@code spring-boot-docker-compose} działa przy KAŻDYM starcie aplikacji: bez pliku
    // compose każda inna lekcja (Test01Application…) kończyła start błędem „No Docker Compose file found”.
    // Profil test07 włącza go z powrotem.
    //
    // PUŁAPKA: {@code skip.in-tests}. Profil test07 włącza compose, a Test07SkipInTestsTest i tak działa na H2!
    //   Dlaczego? W testach Boot domyślnie pomija Docker Compose. Adresu bazy nikt nie podał, więc Boot tworzy
    //   wbudowaną H2 (jest na classpath), migracje są zgodne z H2 — i test jest zielony, choć nie dotknął PostgreSQL.
    //   Sprawdzone: adres zaczyna się od {@code jdbc:h2:mem:}. Pomijanie działa nawet przy ręcznym
    //   {@code SpringApplicationBuilder.run()} w teście (Boot patrzy, czy JUnit jest na stosie wywołań).
    //
    // DOBRA PRAKTYKA: {@code ports: - "5432"} (losowy port komputera) zamiast {@code "5432:5432"}. Dlaczego? Port 5432
    //   bywa zajęty (zainstalowany PostgreSQL, inna aplikacja z compose) — z losowym portem nie ma konfliktu, a Boot
    //   i tak odczyta prawdziwy port z kontenera.
    //
    // PUŁAPKA: oczekiwanie czystej bazy po restarcie przy domyślnym {@code stop.command=stop}. Dlaczego? stop zatrzymuje
    //   kontener, ale go nie usuwa — przy następnym {@code up} wraca ta sama baza z danymi z poprzedniej sesji (po
    //   zatrzymaniu aplikacji {@code docker ps -a} pokazuje kontener „Exited”). Chcesz czystą bazę przy każdym starcie —
    //   ustaw {@code stop.command=down} albo usuń ręcznie: {@code docker compose -f compose-test07.yaml down}.

    // =================================================================================================
    // 3. TEST Z PRAWDZIWYM COMPOSE (Test07DockerComposeTest)
    // =================================================================================================
    //
    //   {@code @SpringBootTest(webEnvironment = RANDOM_PORT, properties = {}
    //   {@code         "spring.docker.compose.skip.in-tests=false",     // w tym teście NIE pomijaj compose}
    //   {@code         "spring.docker.compose.stop.command=down" })      // po teście usuń kontener}
    //   {@code @ActiveProfiles("test07")}
    //   {@code @EnabledIfDockerAvailable}                                 // org.testcontainers.junit.jupiter
    //   {@code class Test07DockerComposeTest {}
    //   {@code     @DynamicPropertySource static void composeFile(DynamicPropertyRegistry r) {}
    //   {@code         r.add("spring.docker.compose.file", ComposeFile::locate); } }}
    //
    // Warunek „Docker dostępny”: test z compose nie ma pól {@code @Container}, ale potrzebuje Dockera. Niezawodnie
    // zrobi to {@code @EnabledIfDockerAvailable} z Testcontainers (sprawdza Dockera tak samo jak
    // {@code @Testcontainers(disabledWithoutDocker = true)}, przez {@code DockerClientFactory.instance().isDockerAvailable()})
    // — bez Dockera klasa jest POMIJANA (sprawdzone: 2 testy „skipped”, build zielony). Własny warunek JUnit
    // ({@code ExecutionCondition}) piszesz w ćwiczeniu 5 — np. gdy chcesz sprawdzić także polecenie {@code docker compose}.
    //
    // Test pyta prawdziwy serwer (RestClient + {@code @LocalServerPort}): GET /test07/database zwraca bazę „sklep”,
    // użytkownika „sklep” i wersję 17.x, GET /test07/products — trzy produkty z migracji.
    //
    // Kiedy taki test ma sens? Rzadko: gdy compose.yaml jest częścią projektu i chcesz mieć dowód, że aplikacja wstaje
    // z TYM plikiem (np. po zmianie wersji obrazu). Do zwykłych testów integracyjnych używaj Testcontainers — szybciej,
    // bez zależności od pliku i katalogu roboczego.
    //
    // PUŁAPKA: kilka klas testowych z compose. Dlaczego uważać? Wszystkie używają TEGO SAMEGO projektu compose
    //   (nazwa {@code springlearning-test07} z pliku) — drugi kontekst zastaje działające usługi ({@code start.skip=if-running})
    //   i dzieli z pierwszym bazę i dane. Testy powinny wtedy tylko czytać albo po sobie sprzątać.

    // =================================================================================================
    // 4. DOCKER COMPOSE KONTRA TESTCONTAINERS — OBA W JEDNYM PROJEKCIE
    // =================================================================================================
    //
    //                          Docker Compose (spring-boot-docker-compose)   Testcontainers
    //   ---------------------------------------------------------------------------------------------------
    //   do czego                lokalne uruchamianie aplikacji (▶ main)      testy (i TestTestNNApplication)
    //   opis usług              plik compose.yaml (YAML)                    kod Java (new PostgreSQLContainer(...))
    //   w testach               domyślnie POMIJANE (skip.in-tests)          tak — po to istnieje
    //   połączenie              automatyczne (ServiceConnection z compose)  @ServiceConnection
    //   port                    losowy ("5432") lub stały ("5432:5432")     zawsze losowy
    //   po zakończeniu          stop (dane zostają) lub down                kontener usunięty (Ryuk)
    //   zależność Maven         spring-boot-docker-compose (optional)       spring-boot-testcontainers + moduły (test)
    //   bez Dockera             aplikacja nie wstaje                        testy pominięte (disabledWithoutDocker)
    //
    // Oba razem: typowy projekt ma compose.yaml do codziennej pracy (baza, kolejka, Redis — wszystko jednym poleceniem)
    // i Testcontainers w testach. Nie przeszkadzają sobie, bo compose w testach domyślnie milczy. Alternatywą dla
    // compose przy lokalnym uruchamianiu jest TestTest06Application (Test06Testcontainers, sekcja 5) — wtedy jedno
    // narzędzie wystarcza do obu celów, ale baza jest czysta przy każdym starcie.
    //
    // DOBRA PRAKTYKA: ta sama wersja obrazu w compose.yaml, w testach i na produkcji ({@code postgres:17-alpine}).
    //   Dlaczego? Wtedy „u mnie działa”, „testy przechodzą” i „produkcja działa” znaczą to samo.

    // =================================================================================================
    // 5. URUCHOMIENIE APLIKACJI LEKCJI Z BAZĄ Z COMPOSE (http/test07_docker_compose.http)
    // =================================================================================================
    //
    //   1) Uruchom Docker (Docker Desktop) — bez niego aplikacja nie wstanie.
    //   2) IntelliJ: ▶ obok {@code main} w Test07Application; wiersz poleceń (katalog główny projektu):
    //      {@code ./mvnw -pl s11-testing spring-boot:run -Dspring-boot.run.main-class=s11_testing.test07_docker_compose.Test07Application}
    //   3) W logu: „Using Docker Compose file …/s11-testing/compose-test07.yaml”, „Container springlearning-test07-postgres-1
    //      Started”, „… Healthy”, potem „Tomcat started on port 8080”.
    //   4) Zapytania z {@code s11-testing/http/test07_docker_compose.http}: /test07/database pokazuje bazę „sklep”
    //      (wersja 17.x), /test07/products — Czajnik, Herbata, Kawa (sprawdzone ręcznie).
    //   5) Po zatrzymaniu aplikacji: {@code docker ps -a} pokazuje zatrzymany kontener; sprzątanie:
    //      {@code docker compose -f compose-test07.yaml down} (w katalogu s11-testing).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code spring-boot-docker-compose} (optional) + compose.yaml → przy starcie aplikacji {@code docker compose up},
     *     połączenie z bazą BEZ {@code spring.datasource.url} (rodzaj usługi po nazwie obrazu, dane z POSTGRES_*).
     *   • Plik: {@code spring.docker.compose.file} albo compose.yaml w KATALOGU ROBOCZYM; ścieżka względna zależy od
     *     sposobu uruchomienia (IntelliJ vs ./mvnw). Brak pliku → „'files' content [...] must exist”.
     *   • {@code lifecycle-management}: start-and-stop (domyślne) / start-only / none; {@code stop.command}: stop
     *     (dane zostają) / down (kontener usunięty).
     *   • W testach pomijane ({@code skip.in-tests=true}) — test z profilem compose po cichu działa na H2!
     *     Test z compose: {@code skip.in-tests=false} + {@code @EnabledIfDockerAvailable}.
     *   • {@code ports: - "5432"} = losowy port komputera, bez konfliktów.
     *   • Compose do lokalnej pracy, Testcontainers do testów; ta sama wersja obrazu wszędzie.
     *
     * PYTANIA KONTROLNE:
     *   1. Skąd aplikacja lekcji zna adres, login i hasło bazy, skoro w application-test07.properties ich nie ma?
     *   2. Co się stanie? Ustawiasz {@code spring.docker.compose.file=compose-test07.yaml} i uruchamiasz aplikację
     *      z IntelliJ, którego katalogiem roboczym jest katalog główny projektu (SpringLearning).
     *   3. Co się stanie? Test {@code @SpringBootTest} z {@code @ActiveProfiles("test07")} (compose włączony w profilu)
     *      sprawdza tylko {@code repository.count() == 3}. Z jaką bazą się łączy i czy przejdzie?
     *   4. Czym różnią się {@code lifecycle-management} start-and-stop, start-only i none?
     *   5. ZNAJDŹ BŁĄD: programista po każdym restarcie aplikacji oczekuje czystej bazy, a widzi dane wpisane wczoraj.
     *      Konfiguracja: domyślna. Co zmienić?
     *   6. Po co w module jest {@code spring.docker.compose.enabled=false} we wspólnym application.properties?
     *   7. ZNAJDŹ BŁĄD: compose.yaml ma {@code ports: - "5432:5432"}, a aplikacja u kolegi nie wstaje, bo „port is
     *      already allocated”. Jak to poprawić, nie zmieniając konfiguracji Springa?
     *   8. Kiedy wybierzesz Docker Compose, a kiedy Testcontainers?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — piszesz TESTY. Szkielet: Test07DockerComposeExercisesTest (@Tag("cwiczenie")),
    // rozwiązania: s11_testing.solutions.test07_docker_compose.Test07DockerComposeSolutionsTest (@Tag("wzorzec"))
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): bez Springa i bez Dockera sprawdź, że {@code ComposeFile.locate()} wskazuje istniejący plik
    //   i że jest w nim {@code image: postgres:17-alpine}. Podpowiedź: sekcja 1; {@code Files.readString(Path)}.
    //
    // ĆWICZENIE 2 (łatwe): udowodnij pułapkę {@code skip.in-tests}: z profilem test07 (bez {@code skip.in-tests=false})
    //   adres bazy zaczyna się od {@code jdbc:h2:}. Podpowiedź: sekcja 2; nie potrzebujesz Dockera.
    //
    // ĆWICZENIE 3 (średnie): test z prawdziwym compose — {@code SELECT current_user} zwraca „sklep”. Podpowiedź:
    //   sekcja 3 (dwie właściwości, {@code @DynamicPropertySource}, {@code @EnabledIfDockerAvailable}), JdbcClient.
    //
    // ĆWICZENIE 4 (średnie): z compose i prawdziwym serwerem: GET /test07/products/99 → 404 z ProblemDetail „Nie ma
    //   takiego produktu”. Podpowiedź: RANDOM_PORT, {@code @LocalServerPort}, RestClient rzuca
    //   {@code HttpClientErrorException.NotFound}.
    //
    // ĆWICZENIE 5 (trudniejsze): napisz własny warunek JUnit {@code ExecutionCondition}: test biegnie tylko wtedy, gdy
    //   polecenie {@code docker compose version} kończy się kodem 0; użyj go przez {@code @ExtendWith} na teście, który
    //   sprawdza, że wynik polecenia zawiera „Docker Compose”. Podpowiedź: {@code ProcessBuilder},
    //   {@code ConditionEvaluationResult.enabled/disabled}, brak programu {@code docker} → IOException.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Z pliku compose: Boot uruchamia usługę, rozpoznaje PostgreSQL po obrazie i tworzy bean
     *      {@code JdbcConnectionDetails} z hosta, wylosowanego portu oraz zmiennych POSTGRES_DB/USER/PASSWORD.
     *      Sprawdzone: Test07DockerComposeTest ({@code spring.datasource.url} puste, klasa z „DockerCompose”).
     *   2. Ścieżka względna liczy się od katalogu roboczego (SpringLearning), a tam pliku nie ma — aplikacja nie wstaje:
     *      „'files' content [compose-test07.yaml] must exist” (Boot sprawdza plik, zanim zawoła Dockera). Sprawdzone:
     *      Test07MissingFileTest. Dlatego Test07Application używa {@code ComposeFile.locate()}.
     *   3. Compose w testach jest pomijany, więc test łączy się z wbudowaną H2; migracje są zgodne z H2, więc
     *      {@code count() == 3} PRZEJDZIE — choć PostgreSQL z compose nie był użyty. Sprawdzone: Test07SkipInTestsTest.
     *   4. start-and-stop: {@code up} przy starcie i stop/down przy zamknięciu aplikacji; start-only: tylko {@code up},
     *      kontenery działają dalej po zamknięciu (szybszy kolejny start); none: Boot niczego nie uruchamia ani nie
     *      zatrzymuje — tylko łączy się z usługami, które już działają.
     *   5. Domyślne {@code stop.command=stop} tylko zatrzymuje kontener — przy następnym {@code up} wraca ta sama baza
     *      z danymi. Zmień na {@code spring.docker.compose.stop.command=down} (kontener usuwany) albo sprzątaj ręcznie
     *      {@code docker compose down}. Sprawdzone ręcznie: po zatrzymaniu aplikacji kontener ma status „Exited”.
     *   6. {@code spring-boot-docker-compose} jest na classpath całego modułu i działa przy każdym starcie aplikacji;
     *      bez pliku compose wszystkie pozostałe lekcje kończyłyby start błędem „No Docker Compose file found”.
     *      Wyłączamy go wspólnie i włączamy tylko w profilu test07.
     *   7. Stały port 5432 na komputerze jest już zajęty (np. przez zainstalowany PostgreSQL). Wystarczy
     *      {@code ports: - "5432"} — Docker wybierze wolny port, a Boot sam go odczyta z kontenera.
     *   8. Docker Compose — do lokalnego uruchamiania aplikacji z bazą i innymi usługami (jeden plik dla całego zespołu).
     *      Testcontainers — do testów: kontener definiowany w kodzie, losowe porty, sprzątanie przez Ryuka, pomijanie
     *      bez Dockera. Lokalnie można też użyć TestTestNNApplication z Testcontainers.
     */
    // </editor-fold>
}

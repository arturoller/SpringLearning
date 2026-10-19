package s11_testing.test07_docker_compose;

import java.util.Map;

/**
 * <pre>
 * TEMAT: Docker Compose w Spring Boot — usługi (np. baza) uruchamiane razem z aplikacją; Compose a Testcontainers
 *        (Docker Compose = narzędzie, które z jednego pliku compose.yaml uruchamia kilka kontenerów naraz;
 *         service = usługa, jeden kontener w pliku Compose; lifecycle = cykl życia; readiness = gotowość usługi)
 *
 * W SKRÓCIE:
 *   Do uruchomienia aplikacji lokalnie potrzebujesz bazy (a czasem kolejki, cache…). Zamiast instalować PostgreSQL
 *   na komputerze, opisujesz go w pliku compose.yaml. Z zależnością spring-boot-docker-compose Boot przy starcie
 *   aplikacji sam wykonuje „docker compose up”, czeka, aż baza będzie gotowa, i SAM tworzy połączenie (DataSource) —
 *   bez spring.datasource.url. Przy zamknięciu aplikacji zatrzymuje usługi. W testach Boot to wszystko POMIJA
 *   (domyślnie) — do testów służy Testcontainers (s11_testing/Test06Testcontainers).
 *
 * ANALOGIA: lista zakupów przed gotowaniem.
 *   compose.yaml to lista „co musi stać na blacie, zanim zacznę gotować” (baza, kolejka). Boot to pomocnik kuchenny:
 *   gdy wchodzisz do kuchni (start aplikacji), wykłada wszystko z listy i czeka, aż piekarnik się nagrzeje (readiness);
 *   gdy wychodzisz (zamknięcie), sprząta. Na egzaminie kulinarnym (test) pomocnik nie pomaga — tam każdy dostaje
 *   własny, świeży zestaw od organizatora (Testcontainers).
 *
 * JAK TO DZIAŁA:
 *   1. Zależność spring-boot-docker-compose (w pom.xml: scope runtime, optional — projekty zależne od tego modułu jej
 *      nie dziedziczą). W tym module obsługa jest WYŁĄCZONA w application.properties
 *      (spring.docker.compose.enabled=false), a włącza ją tylko profil test07 — inaczej każda lekcja próbowałaby
 *      uruchamiać Compose.
 *   2. Przy starcie: plik z spring.docker.compose.file albo (bez tej właściwości) compose.yaml / compose.yml /
 *      docker-compose.yaml / docker-compose.yml z katalogu roboczego → „docker compose up” → czekanie na gotowość
 *      (readiness) → dla znanych obrazów (postgres, mysql, redis, …) bean z danymi połączenia (ConnectionDetails)
 *      zbudowany z obrazu, portu i zmiennych POSTGRES_USER / POSTGRES_PASSWORD / POSTGRES_DB.
 *   3. Przy zamknięciu (lifecycle-management=start-and-stop): polecenie stop (domyślnie „docker compose stop”;
 *      w tej lekcji „down --volumes”).
 *   4. W testach (JUnit na ścieżce klas i na stosie wywołań) obsługa jest pomijana: spring.docker.compose.skip.in-tests=true.
 *
 * SŁÓWKA:
 *   compose = złożyć (tu: narzędzie Docker Compose); service = usługa; up / down = uruchom / usuń; stop = zatrzymaj;
 *   lifecycle management = zarządzanie cyklem życia; readiness = gotowość; healthy = zdrowy (gotowy);
 *   working directory = katalog roboczy; skip = pominąć; volume = wolumen (miejsce na dane kontenera).
 *
 * ZOBACZ TEŻ: s11_testing/Test06Testcontainers (kontenery w testach, @ServiceConnection),
 *             s02_configuration/Cfg03Profiles (profile), s07_data_jdbc/Jdbc03SqlInit (skrypty schema.sql/data.sql),
 *             s03_boot_basics/Boot01AutoConfiguration (autokonfiguracja).
 * </pre>
 */
public final class Test07DockerCompose {

    private Test07DockerCompose() {
    }

    // =================================================================================================
    // 1. PLIK compose.yaml LEKCJI I START APLIKACJI
    // =================================================================================================
    //
    //   s11-testing/compose/test07/compose.yaml:
    //     services:
    //       postgres:
    //         image: 'postgres:18-alpine'
    //         environment: { POSTGRES_DB: 'sklep', POSTGRES_USER: 'sklep', POSTGRES_PASSWORD: '…' }
    //         ports: [ '5432' ]           # tylko port kontenera — host dostaje wolny port
    //
    // Uruchomienie Test07Application (sprawdzone na porcie 8091, log skrócony):
    //   Using Docker Compose file E:\java\SpringLearning\s11-testing\compose\test07\compose.yaml
    //   Network test07_default  Created
    //   Container test07-postgres-1  Started
    //   Container test07-postgres-1  Healthy
    //   Tomcat started on port …
    // GET /api/database → {"database":"PostgreSQL 18.6"}; GET /api/products → 5 produktów z db/test07/data.sql.
    // Nazwa projektu Compose (test07) pochodzi od katalogu pliku; port hosta był losowy (u autora 53913).
    //
    // DOBRA PRAKTYKA: w compose.yaml podawaj tylko port kontenera ('5432'), nie '5432:5432'. Dlaczego? Boot i tak
    //   odczyta prawdziwy port z Dockera, a stały port hosta zderzy się z inną bazą na Twoim komputerze (albo z kolegą
    //   z zespołu, który ma już PostgreSQL na 5432) — i aplikacja nie wstanie.

    // =================================================================================================
    // 2. SKĄD BOOT BIERZE PLIK: KATALOG ROBOCZY ALBO spring.docker.compose.file
    // =================================================================================================
    //
    // Bez właściwości Boot szuka compose.yaml (i trzech innych standardowych nazw) w KATALOGU ROBOCZYM aplikacji.
    // Ten moduł ma kilka lekcji, więc plik lekcji leży w compose/test07, a profil wskazuje go wprost:
    //   spring.docker.compose.file=compose/test07/compose.yaml      (ścieżka względem katalogu roboczego)
    // Test sekcji 2 (Test07DockerComposeFileTest): w katalogu modułu standardowego pliku nie ma, w compose/test07 — jest.
    //
    // PUŁAPKA: inny katalog roboczy (np. uruchomienie z katalogu głównego repozytorium). Dlaczego? Ścieżka względna
    //   compose/test07/compose.yaml wskazuje wtedy w złe miejsce i Boot nie znajdzie pliku. Katalogiem roboczym ma być
    //   katalog modułu s11-testing — tak uruchamia ./mvnw -pl s11-testing spring-boot:run i tak domyślnie ustawia
    //   IntelliJ (Run Configuration → Working directory).
    //
    // Co się stanie, gdy obsługa Compose jest włączona, a pliku NIE MA? Aplikacja nie wstaje (sprawdzone uruchomieniem
    // Test01Application z --spring.docker.compose.enabled=true):
    //   java.lang.IllegalStateException: No Docker Compose file found in directory 'E:\java\SpringLearning\s11-testing\.'
    // Dlatego w tym module obsługa jest domyślnie wyłączona — inne lekcje nie mają pliku Compose.

    // =================================================================================================
    // 3. CYKL ŻYCIA I POZOSTAŁE USTAWIENIA
    // =================================================================================================
    //
    // Wartości domyślne (sprawdzone testem Test07DockerComposeDefaultsTest na prawdziwej klasie DockerComposeProperties):
    //   spring.docker.compose.enabled=true                 spring.docker.compose.lifecycle-management=start-and-stop
    //   spring.docker.compose.start.command=up             spring.docker.compose.start.skip=if-running
    //   spring.docker.compose.stop.command=stop            spring.docker.compose.stop.timeout=10s
    //   spring.docker.compose.skip.in-tests=true           spring.docker.compose.readiness.wait=always (timeout 2m)
    // lifecycle-management: none (nic nie rób — usługi uruchamiasz sam), start-only (tylko start; kontenery zostają po
    // zamknięciu — szybszy kolejny start), start-and-stop. start.skip=if-running: gdy usługi już działają (np. uruchomione
    // ręcznie „docker compose up”), Boot ich nie startuje drugi raz, tylko się podłącza.
    //
    // W tej lekcji stop.command=down + stop.arguments=--volumes: po zamknięciu kontener jest USUWANY, więc każde
    // uruchomienie ma świeżą bazę, a schema.sql może zawsze robić CREATE TABLE. Sprawdzone uruchomieniem: po
    // normalnym zakończeniu aplikacji kontenera test07-postgres-1 już nie ma.
    //
    // PUŁAPKA: zabity proces aplikacji. Dlaczego? Polecenie stop/down wykonuje się przy NORMALNYM zamknięciu kontekstu.
    //   Gdy proces zostanie zabity (np. Stop-Process -Force), Boot nie zdąży — kontener działa dalej (sprawdzone
    //   uruchomieniem). Sprzątasz wtedy sam: docker compose -f compose/test07/compose.yaml down --volumes.
    //   (Testcontainers ma na to Ryuka — Compose w Boot nie ma.)
    //
    // PUŁAPKA: Docker wyłączony. Dlaczego? Aplikacja z włączonym Compose przy starcie woła polecenie docker i bez
    //   działającego Dockera NIE WSTAJE: „Application run failed”, a u autora (Docker niedostępny — zmienna DOCKER_HOST
    //   wskazywała donikąd) ProcessExitException: 'docker version --format {{.Client.Version}}' failed with exit code 1.
    //   Najpierw uruchom Docker Desktop.

    // =================================================================================================
    // 4. W TESTACH COMPOSE JEST POMIJANY
    // =================================================================================================
    //
    // Test z @ActiveProfiles("test07") ma obsługę Compose WŁĄCZONĄ, a mimo to Boot jej nie uruchamia
    // (skip.in-tests=true): rozpoznaje test po JUnit na ścieżce klas i klasach org.junit.platform. /
    // org.springframework.boot.test. na stosie wywołań. Baza w teście to wtedy H2 wbudowana (moduł ma H2) — ten sam
    // schemat i dane z db/test07. Test sekcji 4: Test07DockerComposeSkipInTestsTest (przechodzi także bez Dockera).
    //
    // PUŁAPKA: spring.docker.compose.skip.in-tests=false „żeby testy miały prawdziwą bazę”. Dlaczego to zły pomysł?
    //   Wszystkie testy dzielą jedną bazę z Compose (dane przeciekają między uruchomieniami), testy wymagają Dockera
    //   i nie da się ich pominąć warunkiem jak przy Testcontainers. Do testów: Testcontainers (Test06).

    // =================================================================================================
    // 5. DOCKER COMPOSE CZY TESTCONTAINERS — KIEDY CO
    // =================================================================================================
    //
    //                     Docker Compose (spring-boot-docker-compose)    Testcontainers (+ @ServiceConnection)
    //   do czego          uruchamianie aplikacji lokalnie (praca)         testy automatyczne
    //   opis usług        plik compose.yaml (wspólny dla zespołu)         kod Javy (klasa testowa / @TestConfiguration)
    //   kiedy startuje    przy starcie aplikacji                          przy starcie testów / kontekstu testowego
    //   sprzątanie        stop/down przy normalnym zamknięciu              zawsze (także po zabiciu JVM — Ryuk)
    //   w testach         domyślnie POMIJANY                               do tego służy
    //   bez Dockera       aplikacja nie wstaje (sprawdzone, sekcja 3)    testy pominięte (disabledWithoutDocker)
    //
    // Można łączyć: compose.yaml do codziennej pracy, Testcontainers w testach, a do uruchamiania aplikacji z kontenerami
    // bez pliku Compose — SpringApplication.from(...).with(...) (Test06DevApplication w s11_testing/Test06Testcontainers).
    //
    // DOBRA PRAKTYKA: w compose.yaml i w testach używaj TEGO SAMEGO obrazu z jawnym tagiem co na produkcji.
    //   Dlaczego? Wtedy „u mnie działa” znaczy to samo lokalnie, w testach i na serwerze.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Zależność spring-boot-docker-compose (runtime, optional) + compose.yaml → Boot przy starcie robi „up”
     *     i sam tworzy połączenie z usługą (bez spring.datasource.url).
     *   • Plik: compose.yaml (lub .yml, docker-compose.*) w katalogu roboczym albo spring.docker.compose.file.
     *   • spring.docker.compose.enabled / lifecycle-management (none | start-only | start-and-stop)
     *     / start.command (up | start) / stop.command (stop | down) / skip.in-tests (domyślnie true).
     *   • Port w compose.yaml: tylko port kontenera ('5432'); obraz z jawnym tagiem.
     *   • W testach Compose jest pomijany — do testów Testcontainers.
     *   • Zabity proces = kontener zostaje → docker compose -f … down.
     *
     * PYTANIA KONTROLNE:
     *   1. Co robi Spring Boot przy starcie aplikacji, gdy na ścieżce klas jest spring-boot-docker-compose, a obok leży
     *      compose.yaml z PostgreSQL?
     *   2. Co się stanie? Test ma @ActiveProfiles("test07") (Compose włączony w profilu), a Docker jest wyłączony.
     *   3. Dlaczego w tym module spring.docker.compose.enabled=false jest we wspólnym application.properties?
     *   4. ZNAJDŹ BŁĄD: w compose.yaml jest ports: - '5432:5432', a aplikacja kolegi nie wstaje, bo „port jest zajęty”.
     *   5. Co się stanie z kontenerem, gdy zamkniesz aplikację normalnie, a co — gdy jej proces zostanie zabity?
     *   6. Czym różni się stop.command=stop od down? Kiedy który?
     *   7. Kiedy Docker Compose, a kiedy Testcontainers?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Test07DockerComposeExercisesTest (żadne nie uruchamia Dockera)
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): w teście ustaw w obiekcie DockerComposeProperties cykl życia „tylko start” i brak czekania
    //   na gotowość usług. Podpowiedź: setLifecycleManagement(...), getReadiness().setWait(...).

    /**
     * ĆWICZENIE 2 (średnie): zwróć mapę właściwości (klucz → wartość, dokładnie jak w application.properties), która:
     * włącza obsługę Compose, wskazuje plik compose/test07/compose.yaml, przy zamknięciu USUWA kontenery i NIE pomija
     * Compose w testach. Test powiąże ją z klasą DockerComposeProperties tym samym mechanizmem co Boot.
     * Podpowiedź: Map.of("spring.docker.compose.enabled", "true", ...) — sekcje 2 i 3.
     */
    public static Map<String, String> exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // ĆWICZENIE 3 (trudniejsze): popraw plik s11-testing/compose/exercise07/services.yaml tak, żeby Boot znalazł go bez
    //   spring.docker.compose.file, obraz miał jawny tag (np. postgres:18-alpine), a port nie był przypięty do portu
    //   hosta. Podpowiedź: sekcje 1 i 2; zmień też NAZWĘ pliku; port zapisz w apostrofach: '5432'.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Wykonuje „docker compose up” dla pliku, czeka na gotowość usług (readiness), a z obrazu postgres, jego portu
     *      i zmiennych POSTGRES_* tworzy dane połączenia — DataSource łączy się z kontenerem bez spring.datasource.url.
     *      Sprawdzone uruchomieniem (log „Container test07-postgres-1 Healthy”, /api/database → PostgreSQL 18.6).
     *   2. Nic złego — w testach obsługa Compose jest pomijana (skip.in-tests=true), Docker nie jest potrzebny, a baza
     *      to H2 wbudowana. Sprawdzone testem sekcji 4.
     *   3. Startery i zależności są wspólne dla wszystkich lekcji modułu — bez wyłączenia każda aplikacja lekcji
     *      próbowałaby uruchamiać Compose i nie wstawałaby: „No Docker Compose file found in directory …” (sprawdzone
     *      uruchomieniem), a bez Dockera — błąd polecenia docker. Włącza ją tylko profil test07.
     *   4. Stały port hosta 5432 jest zajęty przez inną bazę na komputerze kolegi. Wystarczy '5432' — Docker wybierze
     *      wolny port hosta, a Boot go odczyta.
     *   5. Normalne zamknięcie: Boot wykonuje polecenie stop (w tej lekcji „down --volumes” — kontener usunięty).
     *      Zabity proces: Boot nie zdąży, kontener działa dalej — trzeba go usunąć ręcznie (docker compose … down).
     *      Oba przypadki sprawdzone uruchomieniem.
     *   6. stop zatrzymuje kontenery, ale ich nie usuwa — kolejny start jest szybszy, dane zostają. down usuwa kontenery
     *      (z --volumes także dane) — każdy start od czystej bazy. stop do codziennej pracy z danymi, down — gdy chcesz
     *      powtarzalnego startu.
     *   7. Compose — uruchamianie aplikacji lokalnie, plik wspólny dla zespołu. Testcontainers — testy automatyczne:
     *      kontener na czas testów, sprzątanie gwarantowane, pomijanie bez Dockera.
     */
    // </editor-fold>
}

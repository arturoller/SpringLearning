package s19_deploy.dep02_docker;

import java.util.List;

/**
 * <pre>
 * TEMAT: Aplikacja w kontenerze — Dockerfile wielostopniowy, warstwy, użytkownik nie-root, pamięć JVM i Buildpacks
 *        (container = kontener; image = obraz; layer = warstwa; multi-stage = wielostopniowy; build = budowanie;
 *        buildpack = „pakiet budujący”, gotowy przepis na obraz bez pisania Dockerfile)
 *
 * W SKRÓCIE:
 *   Obraz Dockera to „zamrożone” środowisko: system, Java i Twoja aplikacja w jednym pakiecie, który działa tak samo
 *   na laptopie, serwerze i w chmurze. Są dwie drogi do obrazu ze Spring Bootem: (1) własny Dockerfile (pełna
 *   kontrola, trzeba go umieć napisać) i (2) {@code ./mvnw spring-boot:build-image} (Buildpacks — obraz powstaje
 *   z JAR-a bez Dockerfile, ale z mniejszą kontrolą). Lekcja pokazuje obie i uczciwie je porównuje.
 *   Dockerfile i .dockerignore tej lekcji leżą w katalogu modułu: s19-deploy/Dockerfile, s19-deploy/.dockerignore.
 *   Testy NIE uruchamiają Dockera (działają w każdym środowisku) — czytają Dockerfile jako tekst i sprawdzają zasady.
 *
 * ANALOGIA: food truck kontra gotowy zestaw w pudełku.
 *   Dockerfile to Twój własny przepis: wybierasz kuchnię (obraz bazowy), układasz składniki warstwami i ustalasz, kto
 *   stoi przy kasie (użytkownik). Buildpacks to franczyza: dostajesz sprawdzone pudełko od dostawcy — wygodnie, ale
 *   menu (wersja Javy, ustawienia pamięci) układa za Ciebie ktoś inny.
 *
 * JAK TO DZIAŁA:
 *   Dockerfile wielostopniowy z tego modułu (skrót; pełny tekst w pliku):
 *     FROM eclipse-temurin:25-jdk AS extract       ← etap 1: JDK, tylko do rozpakowania JAR-a na warstwy
 *     COPY target/s19-deploy-1.0-SNAPSHOT.jar app.jar
 *     RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted
 *     FROM eclipse-temurin:25-jre                  ← etap 2: samo JRE — to jest obraz końcowy
 *     RUN groupadd ... ; useradd ...             ← użytkownik bez uprawnień administratora
 *     COPY --from=extract /build/extracted/dependencies/ ./     ← warstwy: od najrzadziej do najczęściej zmienianej
 *     USER app
 *     ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
 *   Etap 1 znika z obrazu końcowego (zostaje tylko etap ostatni) — w obrazie nie ma JDK ani kopii grubego JAR-a.
 *
 * SŁÓWKA:
 *   Dockerfile = przepis na obraz; stage = etap; base image = obraz bazowy; tag = etykieta wersji obrazu (25-jre);
 *   registry = rejestr obrazów; context = kontekst budowania (katalog wysyłany do Dockera); cache = bufor;
 *   root = administrator systemu; heap = sterta (pamięć na obiekty); OOMKilled = zabity za przekroczenie pamięci;
 *   signal SIGTERM = sygnał „proszę się zakończyć”; PID 1 = proces nr 1 w kontenerze;
 *   builder = konstruktor (obraz, który buduje obrazy); SBOM = lista składników obrazu.
 *
 * ZOBACZ TEŻ: s19_deploy/Dep01ExecutableJar (JAR, warstwy, tryb tools), s19_deploy/Dep04StartupAndShutdown
 *             (łagodne zamykanie i sondy), s02_configuration/Cfg05Environment (ustawienia ze zmiennych środowiska).
 * </pre>
 */
public final class Dep02Docker {

    private Dep02Docker() {
    }

    // =================================================================================================
    // 1. BUDOWANIE I URUCHAMIANIE — polecenia sprawdzone przez autora (Docker 28, Windows)
    // =================================================================================================
    //
    //     ./mvnw -pl s19-deploy package -DskipTests            ← najpierw JAR (Dockerfile kopiuje go z target/)
    //     docker build -t s19-shop-deploy:1.0 s19-deploy       ← ostatni argument = KONTEKST budowania (katalog modułu)
    //     docker run --rm -p 8080:8080 s19-shop-deploy:1.0
    //     curl http://localhost:8080/actuator/health
    //
    // Prawdziwe wyniki (obraz zbudowany w ok. 24 s z pustym buforem warstw, ale z już pobranym obrazem bazowym):
    //     {"groups":["liveness","readiness"],"status":"UP"}                  ← odpowiedź na /actuator/health
    //     Started Dep01Application in 6.415 seconds (process running for 7.292)   ← start w kontenerze z limitem 512 MB
    //     rozmiar obrazu: 360 MB  (obraz bazowy eclipse-temurin:25-jre = 337 MB, aplikacja dokłada 23 MB)
    //     warstwy aplikacji:  21,8 MB (dependencies) · 402 kB (spring-boot-loader) · 46 kB (application)
    //
    // Dlaczego Dockerfile NIE buduje JAR-a sam (nie ma w nim ./mvnw)? Ten projekt jest wielomodułowy: Maven potrzebuje
    // parent pom.xml z korzenia repozytorium, więc kontekstem musiałoby być całe repozytorium. Wybraliśmy prostszą wersję
    // — JAR powstaje przed docker build (lokalnie albo w CI), a etap 1 tylko go rozpakowuje. W projekcie z jednym modułem
    // można dodać do etapu 1 polecenie RUN ./mvnw package i skopiować do niego źródła.
    //
    // PUŁAPKA: obraz bazowy bez wersji (FROM eclipse-temurin) albo z :latest. Dlaczego? „latest” to po prostu najnowszy
    //   obraz w chwili budowania — jutro ten sam Dockerfile da inną wersję Javy, a build, który wczoraj działał, dziś
    //   nie jest powtarzalny. Zawsze podawaj jawny tag, u nas {@code 25-jre} (Java 25 LTS, tylko JRE).
    //   (DOBRA PRAKTYKA na produkcji: dodatkowo przypinaj skrót obrazu, np. @sha256:… — wtedy nie zmieni się nawet tag.)
    //
    // Test lekcji (sekcja 1) czyta prawdziwy Dockerfile i sprawdza: dwa etapy, jawne tagi obrazów bazowych, JRE w końcowym.

    // =================================================================================================
    // 2. WARSTWY I BUFOR (cache) — kolejność instrukcji COPY ma znaczenie
    // =================================================================================================
    //
    // Każda instrukcja Dockerfile tworzy warstwę. Docker ponownie używa warstwy z bufora, dopóki ona i WSZYSTKIE
    // wcześniejsze są bez zmian. Dlatego w Dockerfile kopiujemy od rzeczy najrzadziej zmienianych:
    //     1. dependencies           (biblioteki — 21,8 MB, zmieniają się przy aktualizacji wersji)
    //     2. spring-boot-loader     (402 kB)
    //     3. snapshot-dependencies  (u nas pusta)
    //     4. application            (Twój kod — 46 kB, zmienia się przy każdym commicie)
    // Po poprawce w kodzie Docker przebudowuje tylko ostatnią warstwę, a do rejestru obrazów wysyłasz 46 kB, nie 22 MB.
    // Warstwy pochodzą z trybu -Djarmode=tools extract --layers (lekcja Dep01ExecutableJar, sekcja 4).
    //
    // PRZED (jeden gruby JAR w jednej warstwie):
    //     COPY target/app.jar app.jar          ← każda zmiana kodu = nowa warstwa 22 MB
    // PO (warstwy):
    //     COPY --from=extract /build/extracted/dependencies/ ./   ← z bufora, jeśli biblioteki bez zmian
    //     COPY --from=extract /build/extracted/application/ ./    ← tylko to się przebudowuje
    //
    // DOBRA PRAKTYKA: wielostopniowość (multi-stage). Dlaczego? Obraz końcowy zawiera tylko to, co potrzebne do
    //   UŻYWANIA aplikacji (JRE + warstwy). JDK, kompilator i narzędzia budowania zostają w etapie 1 i nie trafiają
    //   do produkcji — obraz jest mniejszy, a jego powierzchnia ataku (liczba programów, które można wykorzystać) mniejsza.

    // =================================================================================================
    // 3. UŻYTKOWNIK NIE-ROOT
    // =================================================================================================
    //
    // Domyślnie proces w kontenerze działa jako root (administrator). Dockerfile tej lekcji tworzy użytkownika „app”
    // i przełącza się na niego instrukcją USER. Sprawdzone w działającym kontenerze:
    //     docker exec s19agent-app id      →   uid=999(app) gid=999(app) groups=999(app)
    //
    // PUŁAPKA: brak instrukcji USER. Dlaczego to groźne? Kontener działa jako root, więc włamanie przez lukę
    //   w aplikacji daje atakującemu uprawnienia roota w kontenerze (a przy błędnej konfiguracji także poza nim).
    //   Aplikacja webowa nie potrzebuje roota — port 8080 jest powyżej 1024, więc wolno go otworzyć zwykłemu użytkownikowi.
    //
    // Test lekcji (sekcja 3) sprawdza, że ostatnia instrukcja USER w Dockerfile to nie root.

    // =================================================================================================
    // 4. PAMIĘĆ JVM W KONTENERZE — -XX:MaxRAMPercentage
    // =================================================================================================
    //
    // JVM wykrywa limit pamięci kontenera (docker run -m 512m albo limits.memory w Kubernetesie) i sama dobiera
    // maksymalną stertę (heap = obszar pamięci na obiekty). Domyślnie to tylko 25% limitu. Prawdziwy wynik w kontenerze
    // z limitem 512 MB (java -XX:+PrintFlagsFinal -version, pole MaxHeapSize):
    //     domyślnie:                    134217728 bajtów  = 128 MB (25%)
    //     z -XX:MaxRAMPercentage=75.0:  402653184 bajtów  = 384 MB (75%)
    // W Dockerfile ustawiamy to zmienną środowiska JAVA_TOOL_OPTIONS (JVM sama ją czyta i pisze w logu
    // „Picked up JAVA_TOOL_OPTIONS: -XX:MaxRAMPercentage=75.0”).
    //
    // Dlaczego procent, a nie sztywne -Xmx384m? Ten sam obraz uruchomisz z limitem 512 MB w teście i 2 GB na produkcji —
    // procent dostosuje się sam, a sztywne -Xmx trzeba byłoby zmieniać przy każdym limicie.
    //
    // PUŁAPKA: ustawienie MaxRAMPercentage na 90–100%. Dlaczego? Poza stertą JVM potrzebuje pamięci na metaspace (klasy),
    //   stosy wątków, bufory bezpośrednie i kod JIT. Gdy suma przekroczy limit kontenera, system zabija proces
    //   (OOMKilled, kod wyjścia 137) — bez żadnego wyjątku w logu aplikacji. 75% to bezpieczny punkt wyjścia.
    //
    // PUŁAPKA: kontener bez limitu pamięci. Dlaczego? JVM liczy procent z pamięci CAŁEGO komputera (hosta), więc na dużej
    //   maszynie sterta rośnie ponad to, co kontener „powinien” zużywać. Zawsze ustawiaj limit (docker run -m, Kubernetes).

    // =================================================================================================
    // 5. ENTRYPOINT W FORMIE „exec” I ŁAGODNE ZATRZYMANIE
    // =================================================================================================
    //
    //     ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]     ← forma exec (tablica JSON)
    //     ENTRYPOINT java org.springframework.boot.loader.launch.JarLauncher            ← forma shell (zwykły tekst)
    //
    // docker stop wysyła proces nr 1 sygnał SIGTERM (prośba o zakończenie); Spring odpowiada łagodnym zamknięciem
    // (Dep04StartupAndShutdown). Prawdziwy log po docker stop (forma exec — Java jest procesem nr 1, „PID 1”):
    //     GracefulShutdown : Commencing graceful shutdown. Waiting for active requests to complete
    //     GracefulShutdown : Graceful shutdown complete
    //
    // PUŁAPKA: forma shell. Dlaczego? Procesem nr 1 zostaje /bin/sh -c "java …", a Java jest jego dzieckiem (sprawdzone:
    //   ps w kontenerze pokazał „1 /bin/sh -c java org.springframework…” i „7 java org.springframework…”). Powłoka nie
    //   przekazuje SIGTERM dalej, więc docker stop czekał pełne 10 s, po czym zabił kontener (kod wyjścia 137) —
    //   w logu aplikacji NIE było żadnej wiadomości o zamykaniu: żądania w toku zostały przerwane.
    //   Zawsze pisz ENTRYPOINT w formie tablicy.
    //
    // Test lekcji (sekcja 5) sprawdza, że ENTRYPOINT w Dockerfile ma formę exec.
    //
    // O HEALTHCHECK: obraz eclipse-temurin:25-jre nie ma ani curl, ani wget (sprawdzone: which zwraca błąd), więc typowy
    // HEALTHCHECK z curl by nie zadziałał. W Kubernetesie i tak używa się sond liveness/readiness (Dep04).

    // =================================================================================================
    // 6. .dockerignore — co NIE jedzie do Dockera
    // =================================================================================================
    //
    // Argument „s19-deploy” w docker build to kontekst: Docker pakuje cały katalog i wysyła go demonowi budującemu.
    // Plik .dockerignore (obok Dockerfile) mówi, czego nie wysyłać. Nasz ma dwie linie:
    //     *                              ← ignoruj wszystko
    //     !target/s19-deploy-*.jar       ← wyjątek: gotowy JAR
    // Dzięki temu do kontekstu nie trafiają źródła, .git, pliki IDE ani przypadkowe sekrety (.env, klucze).
    //
    // DOBRA PRAKTYKA: zaczynaj od „ignoruj wszystko” i dopuszczaj wyjątki (lista dozwolonych). Dlaczego? Lista zakazów
    //   zawsze o czymś zapomni (nowy plik z hasłem trafi do obrazu), a lista dozwolonych zawiera tylko to, co świadomie dodałeś.

    // =================================================================================================
    // 7. BUILDPACKS — ./mvnw spring-boot:build-image
    // =================================================================================================
    //
    //     ./mvnw -pl s19-deploy spring-boot:build-image -DskipTests -Dspring-boot.build-image.imageName=s19-shop-buildpack:1.0
    //
    // Plugin spring-boot-maven-plugin (ten sam co w Dep01) nie potrzebuje Dockerfile: sam używa „konstruktora”
    // Paketo. Prawdziwy przebieg (Docker 28, Boot 4.1.1):
    //     Pulling builder image 'docker.io/paketobuildpacks/builder-noble-java-tiny:latest'     ← 870 MB, jednorazowo
    //     Pulling run image 'docker.io/paketobuildpacks/ubuntu-noble-run-tiny:0.0.138'
    //     Using Java version 25 extracted from MANIFEST.MF                                      ← wersja z JAR-a (Build-Jdk-Spec)
    //     BellSoft Liberica JRE 25.0.4: Contributing to layer
    //     Successfully built image 'docker.io/library/s19-shop-buildpack:1.0'                   ← łącznie 1 min 13 s (z pobraniem)
    // Wynik: obraz 341 MB, użytkownik 1002:1001 (nie root), uruchamiany przez /cnb/process/web.
    // Przy starcie „kalkulator pamięci” Buildpacka sam policzył ustawienia JVM dla limitu 768 MB:
    //     Calculated JVM Memory Configuration: -XX:MaxDirectMemorySize=10M -Xmx195045K -XX:MaxMetaspaceSize=79386K
    //     -XX:ReservedCodeCacheSize=240M -Xss1M (Total Memory: 768M, Thread Count: 250, Loaded Class Count: 11602 …)
    // Aplikacja wystartowała (Started Dep01Application in 3.043 seconds) i odpowiedziała na /actuator/health.
    //
    // PORÓWNANIE:
    //   • Dockerfile — pełna kontrola (obraz bazowy, użytkownik, flagi JVM, dodatkowe pliki), wymaga znajomości Dockera,
    //     własności: Ty odpowiadasz za aktualizacje obrazu bazowego.
    //   • Buildpacks — zero plików do utrzymania, domyślnie nie-root, kalkulator pamięci, regularnie aktualizowany
    //     system w obrazie (przebudowa daje świeże poprawki bezpieczeństwa), SBOM (lista składników). Wady: pierwszy
    //     build ciągnie ~900 MB, wersję Javy wybiera Buildpack (zmienna BP_JVM_VERSION), trudniej o niestandardowe
    //     rzeczy, a sam build wymaga działającego demona Dockera.
    //   Reguła kciuka: zaczynaj od Buildpacks (szybki start), przejdź na Dockerfile, gdy potrzebujesz kontroli.
    //
    // UWAGA: Buildpacks wybrały Javę 25, bo JAR zbudowano JDK 25 (wpis Build-Jdk-Spec w manifeście), mimo że kod
    // kompiluje się do poziomu 21 (java.version=21). Inną wersję wybierzesz zmienną BP_JVM_VERSION (jest na liście zmiennych, którą wypisuje
    // Buildpack, z wartością domyślną 21) w konfiguracji pluginu (image → env) — tego wariantu w lekcji nie uruchamialiśmy.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Dockerfile wielostopniowy: etap 1 (JDK) rozpakowuje JAR na warstwy, etap 2 (JRE) to obraz końcowy.
     *   • Obraz bazowy zawsze z jawnym tagiem (25-jre), nigdy bez tagu ani :latest.
     *   • Warstwy: dependencies → loader → snapshot-dependencies → application (od najrzadziej zmienianej).
     *   • USER app (nie root); ENTRYPOINT w formie tablicy, żeby Java była procesem nr 1 i dostawała SIGTERM.
     *   • Pamięć: limit kontenera + -XX:MaxRAMPercentage=75.0 (domyślnie JVM bierze tylko 25%).
     *   • .dockerignore: „*” i wyjątki. Kontekst budowania = katalog z Dockerfile.
     *   • Buildpacks: ./mvnw spring-boot:build-image — bez Dockerfile, mniej kontroli, pierwszy build długi.
     *
     * PYTANIA KONTROLNE:
     *   1. Po co w Dockerfile dwa etapy FROM i co z pierwszego trafia do obrazu końcowego?
     *   2. Co się stanie? Zmienisz jedną linię w klasie kontrolera i przebudujesz obraz — które warstwy Docker weźmie
     *      z bufora, a która zostanie zbudowana od nowa?
     *   3. ZNAJDŹ BŁĄD: {@code FROM eclipse-temurin:latest} jako obraz bazowy produkcyjnego Dockerfile.
     *   4. ZNAJDŹ BŁĄD: {@code ENTRYPOINT java org.springframework.boot.loader.launch.JarLauncher} — docker stop
     *      trwa 10 sekund, a w logu nie ma wiadomości o łagodnym zamykaniu. Dlaczego?
     *   5. Co się stanie? Kontener z limitem 512 MB uruchamiasz bez żadnych flag JVM — ile (w przybliżeniu) wyniesie
     *      maksymalna sterta?
     *   6. Dlaczego nie ustawiać MaxRAMPercentage na 100?
     *   7. Co daje .dockerignore z linią „*” i wyjątkiem dla JAR-a?
     *   8. Kiedy wybrać Buildpacks, a kiedy własny Dockerfile?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Dep02DockerExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): czy obraz bazowy jest „przypięty”, czyli ma jawny tag inny niż {@code latest}?
     * Przykłady: "eclipse-temurin:25-jre" → true; "eclipse-temurin" → false; "eclipse-temurin:latest" → false.
     * UWAGA: adres rejestru może zawierać port, np. "localhost:5000/app" — tu dwukropek NIE oznacza tagu
     * (tag jest dopiero po ostatnim ukośniku), więc to false, a "localhost:5000/app:1.2" → true.
     * Podpowiedź: lastIndexOf('/') i lastIndexOf(':').
     */
    static boolean exercise1(String image) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): wypisz obrazy bazowe z tekstu Dockerfile, w kolejności występowania: argument instrukcji
     * FROM, bez opcji (np. {@code --platform=linux/amd64}) i bez {@code AS nazwa}. Linie komentarzy (#) pomiń,
     * wielkość liter w słowie FROM nie ma znaczenia.
     * Przykład: "FROM --platform=linux/amd64 eclipse-temurin:25-jdk AS build" → ["eclipse-temurin:25-jdk"].
     * Podpowiedź: split("\\R") po liniach, strip(), split("\\s+").
     */
    static List<String> exercise2(String dockerfile) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): prosty „linter” Dockerfile. Zwróć listę problemów (w kolejności wykrycia; pusta
     * lista = brak uwag), każdy jako jeden z napisów:
     *   "NIEPRZYPIETY_OBRAZ:" + obraz   — obraz bazowy bez jawnego tagu lub z :latest (ćwiczenie 1); FROM, który
     *                                     odwołuje się do nazwy wcześniejszego etapu (AS nazwa), NIE jest obrazem bazowym;
     *   "BRAK_USER_NIE_ROOT"            — brak instrukcji USER albo ostatnia to root (także "0");
     *   "ENTRYPOINT_W_FORMIE_SHELL"     — ENTRYPOINT, którego argument nie zaczyna się od "[".
     * Podpowiedź: zbieraj nazwy etapów (AS) w zbiorze i porównuj z argumentem kolejnych FROM.
     */
    static List<String> exercise3(String dockerfile) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Etap 1 (JDK) rozpakowuje JAR na warstwy, etap 2 (JRE) jest obrazem końcowym. Do obrazu końcowego trafiają
     *      tylko pliki skopiowane jawnie instrukcją COPY --from=extract (warstwy aplikacji); JDK i gruby JAR zostają
     *      w etapie 1 i nie są częścią obrazu.
     *   2. Z bufora: dependencies, spring-boot-loader i snapshot-dependencies (bez zmian). Od nowa: tylko „application”
     *      (u nas 46 kB). Warunek: instrukcje COPY są w kolejności od najrzadziej zmienianej.
     *   3. Tag „latest” zmienia się w czasie: ten sam Dockerfile zbudowany jutro może dać inną wersję Javy, więc build
     *      nie jest powtarzalny. Należy użyć jawnego tagu, np. eclipse-temurin:25-jre.
     *   4. Forma shell: procesem nr 1 jest /bin/sh, który nie przekazuje SIGTERM do Javy. docker stop czeka do końca
     *      limitu (10 s), po czym zabija kontener (kod 137) — bez łagodnego zamknięcia. Sprawdzone uruchomieniem.
     *      Poprawnie: ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"].
     *   5. Około 128 MB (134217728 bajtów) — 25% limitu. Sprawdzone: java -XX:+PrintFlagsFinal w kontenerze -m 512m.
     *   6. Poza stertą JVM zużywa pamięć na metaspace, stosy wątków, bufory bezpośrednie i kod JIT. Przy 100% suma
     *      przekroczy limit i system zabije kontener (OOMKilled) bez wyjątku w logu.
     *   7. Do kontekstu budowania trafia tylko gotowy JAR — mniej danych do wysłania demonowi Dockera i żadnych
     *      przypadkowych plików (źródła, .git, sekrety z .env) w obrazie.
     *   8. Buildpacks: szybki start, brak plików do utrzymania, domyślne dobre ustawienia (nie-root, kalkulator pamięci).
     *      Dockerfile: gdy potrzebujesz kontroli (własny obraz bazowy, specjalne flagi JVM, dodatkowe pliki, własne
     *      warstwy) albo nie masz demona Dockera w miejscu budowania, ale masz inne narzędzie do budowania obrazów.
     */
    // </editor-fold>
}

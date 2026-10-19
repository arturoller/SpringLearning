package s19_deploy.dep01_executable_jar;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Wykonywalny JAR („gruby JAR”) — jak Spring Boot pakuje aplikację do jednego pliku i jak ją uruchomić
 *        (executable = wykonywalny; JAR = Java ARchive, archiwum Javy, czyli plik ZIP z klasami; repackage = przepakuj;
 *        layer = warstwa; launcher = program uruchamiający)
 *
 * W SKRÓCIE:
 *   Zwykły JAR z Mavena zawiera tylko TWOJE klasy — do uruchomienia brakuje Springa, Tomcata, Jacksona... Boot dokłada
 *   w fazie package cel spring-boot:repackage, który wkłada do JAR-a wszystkie biblioteki i mały program startowy
 *   (JarLauncher). Wynik uruchamiasz jednym poleceniem: {@code java -jar plik.jar}. Na serwerze nie potrzebujesz
 *   Mavena, kodu źródłowego ani instalowania Tomcata — tylko Javy.
 *
 * ANALOGIA: gotowy zestaw obiadowy z pudełka zamiast zakupów.
 *   Zwykły JAR to sam przepis (Twoje klasy) — musisz sam kupić składniki (biblioteki) i dobrać garnek (serwer).
 *   Gruby JAR to pudełko „wszystko w środku”: składniki są poukładane w przegródkach (BOOT-INF/lib), a instrukcja
 *   na wierzchu (JarLauncher) mówi, od czego zacząć. Warstwy to osobne pojemniki w pudełku: składniki, które
 *   rzadko się zmieniają (biblioteki), leżą osobno od przepisu, który zmieniasz codziennie (Twój kod).
 *
 * JAK TO DZIAŁA:
 *   1. ./mvnw -pl s19-deploy package        → faza package: najpierw maven-jar-plugin robi zwykły JAR,
 *      potem spring-boot-maven-plugin (cel repackage) PRZEPAKOWUJE go: zwykły JAR zostaje jako *.jar.original,
 *      a w jego miejsce powstaje JAR wykonywalny.
 *   2. Wnętrze (prawdziwy wynik jar tf — pokazane skrótowo, szczegóły w sekcji 2):
 *        META-INF/MANIFEST.MF                  ← Main-Class: org.springframework.boot.loader.launch.JarLauncher
 *        BOOT-INF/classes/…                    ← TWOJE klasy i zasoby (application.properties)
 *        BOOT-INF/lib/…                        ← 42 biblioteki (spring-*, tomcat-embed-*, logback-*…) jako osobne JAR-y
 *        org/springframework/boot/loader/…     ← klasy programu startowego
 *   3. java -jar app.jar → JVM czyta Main-Class z manifestu → JarLauncher buduje ścieżkę klas z BOOT-INF/classes
 *      i BOOT-INF/lib (JAR w JAR-ze, bez rozpakowywania na dysk) → woła Start-Class (Twoją klasę z main) → Spring startuje.
 *
 * SŁÓWKA:
 *   fat jar / uber jar = gruby JAR (z bibliotekami w środku); manifest = plik opisujący JAR (Main-Class, Start-Class…);
 *   Main-Class = klasa, którą JVM uruchamia jako pierwszą; Start-Class = Twoja klasa z metodą main;
 *   classpath = ścieżka klas (gdzie JVM szuka klas); extract = wypakuj; layer = warstwa (grupa plików, którą Docker
 *   zapisuje i buforuje osobno); snapshot = migawka (wersja w budowie, np. 1.0-SNAPSHOT);
 *   command-line argument = argument z linii poleceń; property = właściwość (ustawienie).
 *
 * ZOBACZ TEŻ: s00_start/Start03FirstApplication (uruchamianie aplikacji w IntelliJ), s02_configuration/Cfg01Properties
 *             (priorytet źródeł ustawień: argumenty wygrywają z plikiem), s19_deploy/Dep02Docker (JAR w obrazie).
 * </pre>
 */
public final class Dep01ExecutableJar {

    private Dep01ExecutableJar() {
    }

    // =================================================================================================
    // 1. BUDOWANIE — plugin w pom.xml i polecenie package
    // =================================================================================================
    //
    // Moduł s19-deploy/pom.xml ma na końcu sekcję build z pluginem (plugin = wtyczka Mavena):
    //
    //     <plugin>
    //         <groupId>org.springframework.boot</groupId>
    //         <artifactId>spring-boot-maven-plugin</artifactId>          ← wersję zna spring-boot-starter-parent
    //         <configuration>
    //             <mainClass>s19_deploy.dep01_executable_jar.Dep01Application</mainClass>
    //         </configuration>
    //     </plugin>
    //
    // Polecenie:  ./mvnw -pl s19-deploy package -DskipTests      (-pl = tylko ten moduł, -DskipTests = bez testów)
    // Wynik w s19-deploy/target:
    //     s19-deploy-1.0-SNAPSHOT.jar            22 012 899 bajtów (≈ 21 MB)  ← JAR wykonywalny
    //     s19-deploy-1.0-SNAPSHOT.jar.original        27 998 bajtów           ← zwykły JAR: tylko 22 klasy tego modułu
    // Różnica 21 MB kontra 28 KB to właśnie biblioteki (Spring, Tomcat, Jackson, Logback, Micrometer…).
    // (Te 22 klasy to cały moduł kursu, także rozwiązania ćwiczeń z pakietów solutions — leżą w src/main. W prawdziwym
    // projekcie JAR zawiera tylko kod produkcyjny, a testy i przykłady z src/test do JAR-a nie trafiają.)
    //
    // DLACZEGO mainClass? W tym module jest kilka klas @SpringBootApplication (po jednej na lekcję). Plugin szuka klasy
    // z main, a gdy znajdzie więcej niż jedną, nie wie, którą wybrać — zgłasza błąd. Ustawienie mainClass rozstrzyga to
    // wprost. Prawdziwy komunikat bez mainClass: „Unable to find a single main class from the following candidates
    // [s19_deploy.dep01_executable_jar.Dep01Application, s19_deploy.dep03_production_config.Dep03Application, …]”.
    // W zwykłym projekcie z jedną aplikacją ten wpis jest zbędny (plugin sam znajdzie jedyną klasę z main).
    //
    // PUŁAPKA: jeśli w pom.xml nie ma spring-boot-maven-plugin, package daje ZWYKŁY JAR (≈ 28 KB). Dlaczego to częsty błąd?
    //   Polecenie java -jar kończy się wtedy komunikatem „no main manifest attribute” albo NoClassDefFoundError
    //   (brak klas Springa) — a przecież build przeszedł na zielono. Projekt ze Spring Initializr ma plugin domyślnie.
    //
    // Test lekcji (sekcja 1) czyta pom.xml i sprawdza, że plugin jest, a mainClass wskazuje istniejącą klasę z main.

    // =================================================================================================
    // 2. WNĘTRZE JAR-a — prawdziwy wynik polecenia  jar tf s19-deploy-1.0-SNAPSHOT.jar
    // =================================================================================================
    //
    // (jar tf = „table, file”: wypisz zawartość pliku JAR; skrócone o biblioteki i klasy loadera; wpisów jest razem 206)
    //
    //     META-INF/MANIFEST.MF
    //     org/springframework/boot/loader/launch/JarLauncher.class           ← program startowy (Boot 3.2+: pakiet .launch)
    //     org/springframework/boot/loader/launch/PropertiesLauncher.class    ← wariant z zewnętrznym classpath
    //     BOOT-INF/classes/s19_deploy/dep01_executable_jar/Dep01Application.class
    //     BOOT-INF/classes/s19_deploy/dep01_executable_jar/ShopInfoController.class
    //     BOOT-INF/lib/logback-classic-1.5.38.jar
    //     BOOT-INF/lib/spring-boot-autoconfigure-4.1.1.jar
    //     BOOT-INF/lib/spring-webmvc-7.0.9.jar … (razem 42 biblioteki)
    //     BOOT-INF/classpath.idx                                             ← kolejność bibliotek na ścieżce klas
    //     BOOT-INF/layers.idx                                                ← podział na warstwy (sekcja 4)
    //     META-INF/maven/springlearning/s19-deploy/pom.xml
    //
    // Prawdziwy manifest (java -jar czyta go jako pierwszy):
    //
    //     Main-Class: org.springframework.boot.loader.launch.JarLauncher
    //     Start-Class: s19_deploy.dep01_executable_jar.Dep01Application
    //     Spring-Boot-Version: 4.1.1
    //     Spring-Boot-Classes: BOOT-INF/classes/
    //     Spring-Boot-Lib: BOOT-INF/lib/
    //     Spring-Boot-Classpath-Index: BOOT-INF/classpath.idx
    //     Spring-Boot-Layers-Index: BOOT-INF/layers.idx
    //
    // Uwaga na stare tutoriale: w Boot 2.x launcher nazywał się org.springframework.boot.loader.JarLauncher (bez .launch).
    // W Boot 3.2 i nowszych (także 4.1) jest org.springframework.boot.loader.launch.JarLauncher. Sprawdzone na prawdziwym JAR-ze.
    //
    // DLACZEGO JAR w JAR-ze, a nie „uber-jar” z rozpakowanymi klasami? Narzędzia typu maven-shade-plugin rozpakowują
    // wszystkie biblioteki i sklejają ich klasy w jedno drzewo. Wady: (1) pliki o tych samych nazwach z różnych bibliotek
    // (META-INF/spring.factories, pliki licencji, usługi META-INF/services) nadpisują się i aplikacja zachowuje się
    // dziwnie — trzeba ręcznie je scalać; (2) nie widać, które biblioteki są w środku i w jakich wersjach; (3) traci się
    // podpisy cyfrowe bibliotek. Boot zostawia każdą bibliotekę jako osobny, nietknięty JAR w BOOT-INF/lib, a własny
    // ładowacz klas (LaunchedClassLoader) czyta je prosto z archiwum.

    // =================================================================================================
    // 3. URUCHAMIANIE: java -jar, argumenty i właściwości z linii poleceń
    // =================================================================================================
    //
    //     java -jar s19-deploy/target/s19-deploy-1.0-SNAPSHOT.jar
    //     java -jar s19-deploy/target/s19-deploy-1.0-SNAPSHOT.jar --server.port=8099 --shop.name=Moj_sklep
    //     java -Dserver.port=8099 -jar s19-deploy/target/s19-deploy-1.0-SNAPSHOT.jar        ← właściwość systemowa JVM
    //
    // Argumenty PO nazwie JAR-a (--klucz=wartość) trafiają do main(String[] args) i do SpringApplication.run — Spring
    // robi z nich właściwości o NAJWYŻSZYM priorytecie (wygrywają z application.properties, zmiennymi środowiska i -D).
    // Prawdziwy wynik uruchomienia z --server.port=8099 --shop.name=Test:
    //
    //     Tomcat initialized with port 8099 (http)
    //     Started Dep01Application in 3.995 seconds (process running for 4.551)
    //     GET /api/shop → {"greeting":"Witamy","name":"Test"}
    //
    // PUŁAPKA: opcje JVM (-D…, -Xmx…, -XX:…) piszesz PRZED -jar, a argumenty aplikacji (--klucz=wartość) PO nazwie JAR-a.
    //   Dlaczego? Wszystko po nazwie JAR-a JVM oddaje aplikacji jako args i nie interpretuje. Zapis
    //   {@code java -jar app.jar -Xmx512m} nie ustawi pamięci — aplikacja dostanie tylko dziwny argument.
    //
    // DOBRA PRAKTYKA: jeden JAR na wszystkie środowiska (test, produkcja), a różnice w konfiguracji podawaj z zewnątrz
    //   (argumenty, zmienne środowiska — lekcja Dep03ProductionConfig). Dlaczego? To, co przetestowałeś, jest bajt w bajt
    //   tym, co działa na produkcji; przebudowa JAR-a pod każde środowisko wprowadza ryzyko różnic.
    //
    // Test lekcji (sekcja 3) uruchamia prawdziwą aplikację przez SpringApplication.run z argumentami tak, jak robi to
    // JarLauncher, i sprawdza, że argument wygrywa z wartością domyślną.

    // =================================================================================================
    // 4. WARSTWY (layers) i tryb jarmode=tools
    // =================================================================================================
    //
    // Boot zapisuje w JAR-ze plik BOOT-INF/layers.idx — podział zawartości na warstwy (od najrzadziej do najczęściej
    // zmienianej). Prawdziwa zawartość (spring-boot-maven-plugin 4.1.1, ustawienia domyślne — warstwy są włączone
    // od Boot 2.4 i nie trzeba nic konfigurować):
    //
    //     - "dependencies":                 ← biblioteki z wydań (BOOT-INF/lib/*.jar) — zmieniają się rzadko
    //       - "BOOT-INF/lib/"
    //     - "spring-boot-loader":           ← klasy programu startowego — prawie nigdy
    //       - "org/"
    //     - "snapshot-dependencies":        ← biblioteki w wersji SNAPSHOT (u nas pusta)
    //     - "application":                  ← TWÓJ kod — zmienia się przy każdym commicie
    //       - "BOOT-INF/classes/"
    //       - "BOOT-INF/classpath.idx"
    //       - "BOOT-INF/layers.idx"
    //       - "META-INF/"
    //
    // Po co? Obraz Dockera składa się z warstw i buforuje je (cache): zmienia się tylko to, co się zmieniło. Gdy
    // poprawisz jedną linię kodu, przebudowuje się warstwa „application” (u nas 46 kB), a „dependencies” (21,8 MB) bierze
    // się z bufora. W obrazie z całym grubym JAR-em każda poprawka oznacza wysłanie 21 MB od nowa (Dep02Docker).
    //
    // WAŻNE (zmiana względem starszych tutoriali): dawny tryb  -Djarmode=layertools  został w Boot 4.1 USUNIĘTY. Sprawdzone
    // uruchomieniem:  java -Djarmode=layertools -jar app.jar list  → „Error: Unsupported jarmode 'layertools'”.
    // Zastąpił go tryb  -Djarmode=tools  (jarmode = tryb pracy JAR-a; tools = narzędzia), który wypisuje polecenia:
    //
    //     java -Djarmode=tools -jar s19-deploy-1.0-SNAPSHOT.jar help
    //       extract      Extract the contents from the jar
    //       list-layers  List layers from the jar that can be extracted
    //       help         Help about any command
    //
    //     java -Djarmode=tools -jar s19-deploy-1.0-SNAPSHOT.jar list-layers
    //       dependencies
    //       spring-boot-loader
    //       snapshot-dependencies
    //       application
    //
    // Polecenie extract ma dwa tryby (oba sprawdzone uruchomieniem):
    //   (a) zwykłe:  java -Djarmode=tools -jar app.jar extract
    //       → katalog app/ z lekkim app.jar (30261 bajtów, w manifeście Class-Path: lib/logback-classic-1.5.38.jar …)
    //         i katalogiem lib/ z bibliotekami. Uruchamiasz:  java -jar app/s19-deploy-1.0-SNAPSHOT.jar
    //         (bez JarLaunchera, więc nieco szybszy start).
    //   (b) warstwami:  java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted
    //       → katalogi extracted/dependencies, extracted/spring-boot-loader, extracted/snapshot-dependencies,
    //         extracted/application. Aplikację uruchamia się po złożeniu warstw w jeden katalog:
    //         java org.springframework.boot.loader.launch.JarLauncher     (tak robi Dockerfile w Dep02Docker).
    //
    // PUŁAPKA: kopiowanie rozpakowanego JAR-a przez  java -jar  do obrazu Dockera „jakoś działa” — ale wtedy cały
    //   plik 21 MB leży w jednej warstwie. Dlaczego to problem? Każda zmiana kodu unieważnia cały bufor, więc każda
    //   przebudowa obrazu i każde wdrożenie wysyła do rejestru 21 MB zamiast 46 kB.
    //
    // Test lekcji (sekcja 4) pokazuje regułę przydziału do warstw na prawdziwych ścieżkach (klasa JarLayers).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Budowa: ./mvnw -pl s19-deploy package → spring-boot-maven-plugin (repackage) robi JAR wykonywalny; zwykły JAR
     *     zostaje jako *.jar.original. Wiele klas z main w module → ustaw mainClass.
     *   • Wnętrze: BOOT-INF/classes (Twój kod), BOOT-INF/lib (biblioteki jako osobne JAR-y), klasy loadera w org/…,
     *     manifest z Main-Class: org.springframework.boot.loader.launch.JarLauncher i Start-Class (Twoja klasa).
     *   • Uruchomienie: java -jar app.jar [--klucz=wartość]; opcje JVM PRZED -jar, argumenty aplikacji PO nazwie JAR-a.
     *   • Argumenty z linii poleceń mają najwyższy priorytet wśród ustawień.
     *   • Warstwy (layers.idx): dependencies, spring-boot-loader, snapshot-dependencies, application.
     *   • W Boot 4.1: -Djarmode=tools (extract, list-layers, help); dawny layertools nie istnieje.
     *   • Nie rozpakowuj bibliotek do jednego drzewa klas (shade/uber): konflikty plików i brak kontroli nad wersjami.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się plik .jar od pliku .jar.original w katalogu target po poleceniu package?
     *   2. Co się stanie? W pom.xml modułu usuniesz spring-boot-maven-plugin, zbudujesz projekt i uruchomisz
     *      {@code java -jar target/s19-deploy-1.0-SNAPSHOT.jar}.
     *   3. ZNAJDŹ BŁĄD: {@code java -jar app.jar -Dserver.port=9000} — aplikacja nadal startuje na porcie 8080. Dlaczego?
     *   4. Który plik z manifestu mówi JVM, od czego zacząć, a który — jarLauncherowi, którą klasę z main wywołać?
     *   5. Do której warstwy trafi plik BOOT-INF/lib/logback-core-1.5.38.jar, a do której
     *      BOOT-INF/lib/s19-modul-1.0-SNAPSHOT.jar i BOOT-INF/classes/app.properties?
     *   6. Co się stanie? Uruchomisz {@code java -Djarmode=layertools -jar app.jar list} w Boot 4.1.
     *   7. Po co dzielić JAR na warstwy, skoro i tak ostatecznie liczy się ta sama aplikacja?
     *   8. Dlaczego Boot nie rozpakowuje bibliotek do jednego drzewa klas, jak narzędzie shade?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Dep01ExecutableJarExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj polecenie uruchomienia JAR-a. Opcje JVM (-D…) idą PRZED {@code -jar}, a argumenty
     * aplikacji ({@code --klucz=wartość}) PO nazwie JAR-a; w obu grupach porządek alfabetyczny kluczy.
     * Przykład: ("app.jar", jvm = {"file.encoding": "UTF-8"}, app = {"server.port": "8099", "shop.name": "Test"})
     * → "java -Dfile.encoding=UTF-8 -jar app.jar --server.port=8099 --shop.name=Test". Puste mapy = brak opcji.
     * Podpowiedź: TreeMap albo sorted() na entrySet; StringBuilder albo String.join.
     */
    static String exercise1(String jarName, Map<String, String> jvmProperties, Map<String, String> appArguments) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): policz biblioteki w wykonywalnym JAR-ze — wpisy z listy {@code jar tf}, które leżą w
     * {@code BOOT-INF/lib/} i kończą się na {@code .jar} (sam katalog {@code BOOT-INF/lib/} się nie liczy).
     * Podpowiedź: strumień z filter(); startsWith / endsWith.
     */
    static long exercise2(List<String> jarEntries) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): przydziel wpis JAR-a do warstwy: "spring-boot-loader" (ścieżka zaczyna się od
     * {@code org/springframework/boot/loader/}), "snapshot-dependencies" (BOOT-INF/lib/…SNAPSHOT.jar), "dependencies"
     * (pozostałe BOOT-INF/lib/…), w każdym innym przypadku "application". Kolejność sprawdzania ma znaczenie!
     * Podpowiedź: instrukcja if po kolei, najpierw loader, potem lib (a w nim SNAPSHOT).
     */
    static String exercise3(String entryPath) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. .jar to JAR wykonywalny (22 012 899 bajtów — z bibliotekami w BOOT-INF/lib i JarLauncherem), a .jar.original
     *      to zwykły JAR sprzed przepakowania (27 998 bajtów — tylko Twoje klasy i zasoby). Sprawdzone na prawdziwej budowie.
     *   2. Powstanie zwykły JAR, a java -jar zakończy się błędem („no main manifest attribute, in …jar” — w manifeście
     *      nie ma Main-Class). Sam build przejdzie na zielono, bo brak pluginu nie jest błędem Mavena.
     *   3. Opcje -D… trzeba podać PRZED -jar. Po nazwie JAR-a trafiają do aplikacji jako zwykły argument
     *      „-Dserver.port=9000”, którego Spring nie traktuje jak ustawienia (ustawienie ma postać --klucz=wartość).
     *      Poprawnie: {@code java -Dserver.port=9000 -jar app.jar} albo {@code java -jar app.jar --server.port=9000}.
     *   4. Main-Class mówi JVM: „uruchom JarLauncher”; Start-Class mówi JarLauncherowi: „a teraz wywołaj main z Twojej
     *      klasy aplikacji”. Sprawdzone w prawdziwym manifeście.
     *   5. logback-core → "dependencies"; s19-modul-1.0-SNAPSHOT.jar → "snapshot-dependencies"; BOOT-INF/classes/app.properties
     *      → "application". (Reguła jest w klasie JarLayers; sprawdzone testem sekcji 4.)
     *   6. Błąd: „Error: Unsupported jarmode 'layertools'” — ten tryb usunięto, używa się -Djarmode=tools z poleceniem
     *      extract. Sprawdzone uruchomieniem na JAR-ze zbudowanym w Boot 4.1.1.
     *   7. Dla bufora warstw Dockera: biblioteki (21,8 MB) zmieniają się rzadko, a Twój kod (46 kB) przy każdym commicie.
     *      Przy podziale na warstwy przebudowa i wysłanie obrazu dotyczy tylko małej warstwy z kodem.
     *   8. Bo skleiłby pliki o tych samych nazwach z różnych bibliotek (META-INF/spring.factories, META-INF/services/…),
     *      nadpisując jedne drugimi, i ukryłby wersje bibliotek. Boot zostawia każdą bibliotekę jako osobny, nietknięty JAR.
     */
    // </editor-fold>
}

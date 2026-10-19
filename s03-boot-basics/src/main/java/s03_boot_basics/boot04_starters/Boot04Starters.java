package s03_boot_basics.boot04_starters;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Startery Spring Boota 4, BOM i wersje bibliotek
 *        (starter = zestaw startowy zależności; BOM — bill of materials = „spis części” z wersjami;
 *         dependency = zależność; transitive = przechodnia; module = moduł; deprecated = przestarzały)
 *
 * W SKRÓCIE:
 *   Starter to artefakt Mavena BEZ kodu — jego jedynym zadaniem jest dociągnąć właściwe biblioteki i moduł Boota
 *   z autokonfiguracją. Dodajesz jeden wpis w pom.xml (spring-boot-starter-jackson), a dostajesz Jacksona 3, moduł
 *   spring-boot-jackson z JacksonAutoConfiguration i rdzeń Boota. Wersji nie piszesz: wszystkie pochodzą z BOM-u
 *   spring-boot-dependencies, dzięki czemu biblioteki pasują do siebie. W Boot 4 startery nazywają się jak technologia
 *   (spring-boot-starter-webmvc, -aspectj…) i mają osobne startery testowe (…-test).
 *
 * ANALOGIA: zestaw „kuchnia włoska” w sklepie.
 *   Kupujesz jedno opakowanie (starter), a w środku są makaron, sos i przyprawy (biblioteki) dobrane tak, żeby
 *   do siebie pasowały (BOM), plus karteczka z przepisem (moduł z autokonfiguracją). Samo pudełko nic nie gotuje —
 *   liczy się zawartość. Kto dorzuci przyprawę z innego zestawu (inną wersję biblioteki), ryzykuje, że danie nie wyjdzie.
 *
 * JAK TO DZIAŁA:
 *   1. pom.xml modułu: spring-boot-starter-jackson (bez wersji).
 *   2. Maven czyta pom startera → zależności przechodnie: spring-boot-starter, spring-boot-jackson, jackson-databind…
 *   3. Wersje: parent pom kursu → spring-boot-starter-parent 4.1.1 → spring-boot-dependencies 4.1.1 (BOM):
 *      tam są <jackson-bom.version>3.1.5, <spring-framework.version>7.0.9, <mockito.version>5.23.0 itd.
 *   4. Na ścieżce klas ląduje jar spring-boot-jackson z plikiem AutoConfiguration.imports → Boot widzi
 *      JacksonAutoConfiguration (lekcja Boot01) → w kontekście jest bean JsonMapper.
 *   Wniosek: dodanie/usunięcie startera zmienia ścieżkę klas, a ścieżka klas zmienia to, co Boot konfiguruje.
 *
 * SŁÓWKA:
 *   dependency management = zarządzanie zależnościami; version = wersja; scope = zakres (compile, test…);
 *   optional = opcjonalna (nie przechodzi dalej); exclusion = wykluczenie; tree = drzewo; classic = klasyczny;
 *   developer tools = narzędzia programisty; restart = ponowne uruchomienie; in favor of = na rzecz.
 *
 * ZOBACZ TEŻ: s00_start/Start02MultiModule (dlaczego kurs ma moduł na dział),
 *             s03_boot_basics/Boot01AutoConfiguration (jak moduł z autokonfiguracją zmienia kontekst),
 *             s03_boot_basics/Boot02WritingAutoConfiguration (starter własnej biblioteki),
 *             s19_deploy (spakowany JAR, w którym nie ma DevTools),
 *             t30_build_modules/Build01MavenBasics (Maven od podstaw),
 *             t30_build_modules/Build02JarClasspath (jar i ścieżka klas).
 * </pre>
 */
public final class Boot04Starters {

    private Boot04Starters() {
    }

    // =================================================================================================
    // 1. STARTER = ARTEFAKT BEZ KODU
    // =================================================================================================
    //
    // Jar spring-boot-starter-jackson-4.1.1.jar zawiera tylko META-INF/MANIFEST.MF, LICENSE.txt i NOTICE.txt —
    // ani jednej klasy (test sekcji 1). Cała „treść” startera to jego pom:
    //     spring-boot-starter-jackson  →  spring-boot-starter (rdzeń: autokonfiguracja, logowanie, YAML)
    //                                  →  spring-boot-jackson (moduł: JacksonAutoConfiguration + plik .imports)
    //                                        →  tools.jackson.core:jackson-databind 3.1.5
    // Kod autokonfiguracji jest w module spring-boot-jackson (test sekcji 1 sprawdza, z którego jaru pochodzi klasa
    // JacksonAutoConfiguration).
    //
    // DOBRA PRAKTYKA: dodawaj starter, a nie pojedyncze biblioteki (np. sam jackson-databind).
    //   Dlaczego? Sama biblioteka nie przyniesie modułu z autokonfiguracją — Boot nie utworzy beana JsonMapper
    //   i nie zadziałają ustawienia spring.jackson.*. Starter daje komplet: bibliotekę + jej „instrukcję montażu”.

    // =================================================================================================
    // 2. NAZWY STARTERÓW W BOOT 4 (tylko opis — bez testu)
    // =================================================================================================
    //
    // Boot 4 ma trzy rodzaje artefaktów (sprawdzone w spring-boot-dependencies-4.1.1.pom):
    //   spring-boot-<technologia>                moduł: kod + autokonfiguracja (spring-boot-jackson, spring-boot-webmvc)
    //   spring-boot-starter-<technologia>        starter do aplikacji (spring-boot-starter-jackson)
    //   spring-boot-starter-<technologia>-test   starter do testów tej technologii (spring-boot-starter-webmvc-test)
    //
    // ZMIANY NAZW względem Boota 3 (stare nazwy spotkasz w tutorialach):
    //   spring-boot-starter-web                         → spring-boot-starter-webmvc   (stary: „deprecated in favor of
    //                                                                                   spring-boot-starter-webmvc”)
    //   spring-boot-starter-web-services                → spring-boot-starter-webservices
    //   spring-boot-starter-oauth2-client               → spring-boot-starter-security-oauth2-client
    //   spring-boot-starter-oauth2-resource-server      → spring-boot-starter-security-oauth2-resource-server
    //   spring-boot-starter-oauth2-authorization-server → spring-boot-starter-security-oauth2-authorization-server
    //   spring-boot-starter-aop                         → spring-boot-starter-aspectj  (starej nazwy w BOM 4.1.1 NIE MA)
    // Przestarzałe nazwy nadal działają (są w BOM-ie), ale opis ich pom.xml kieruje do nowych. Nazwa usunięta
    // (-aop) kończy się błędem Mavena: „'dependencies.dependency.version' for
    // org.springframework.boot:spring-boot-starter-aop:jar is missing.” (sprawdzone uruchomieniem).
    //
    // Startery „-classic” do migracji z Boota 3:
    //   spring-boot-starter-classic → spring-boot-autoconfigure-classic → spring-boot-autoconfigure-classic-modules,
    //   który dołącza 93 moduły spring-boot-<technologia>, każdy z wykluczeniem *:* (sam kod autokonfiguracji, bez
    //   bibliotek). Efekt jak w Boocie 3: wszystkie autokonfiguracje są kandydatami, a włączają się te, których
    //   biblioteki dodałeś sam. Do testów jest analogiczny spring-boot-starter-test-classic.
    //   Dlaczego kurs ich NIE używa? Chcemy widzieć, który starter przynosi którą autokonfigurację (krótki raport
    //   warunków — Boot01), i od razu uczyć docelowego sposobu. „Classic” to most na czas migracji, nie cel.

    // =================================================================================================
    // 3. BOM — DLACZEGO W pom.xml MODUŁÓW NIE MA WERSJI
    // =================================================================================================
    //
    //   pom.xml kursu (parent)  →  spring-boot-starter-parent 4.1.1  →  spring-boot-dependencies 4.1.1 (BOM)
    // spring-boot-dependencies ma sekcję <dependencyManagement> z wersjami setek bibliotek, a każda wersja to
    // właściwość, np. (fragmenty pliku spring-boot-dependencies-4.1.1.pom):
    //   <spring-framework.version>7.0.9</spring-framework.version>
    //   <jackson-bom.version>3.1.5</jackson-bom.version>
    //   <mockito.version>5.23.0</mockito.version>
    //   <junit-jupiter.version>6.0.3</junit-jupiter.version>
    // Test sekcji 3 sprawdza w działającej aplikacji: Boot 4.1.1, Spring Framework 7.x, Jackson 3.x.
    //
    // Nadpisanie wersji (gdy MUSISZ, np. poprawka bezpieczeństwa wyszła przed nowym Bootem): we własnym pom.xml
    //   <properties><mockito.version>5.23.1</mockito.version></properties>
    // Działa, bo dziedziczysz po spring-boot-starter-parent. (Uwaga w tym kursie: parent pom używa ${mockito.version}
    // także w argLine agenta Mockito — zmiana wersji zmienia też ścieżkę do jaru agenta.) Jeśli projekt nie dziedziczy
    // po starter-parent, tylko importuje BOM (<scope>import</scope>), właściwości nie nadpiszesz — trzeba wpisać
    // zależność z wersją w <dependencyManagement> PRZED importem BOM-u.
    //
    // PUŁAPKA: wpisana na sztywno wersja jednej biblioteki z „rodziny”, np. spring-context z <version>6.2.0</version>.
    //   Dlaczego to groźne? Maven użyje 6.2.0 tylko dla tego jednego artefaktu, a reszta Springa zostanie w 7.0.9.
    //   Mieszanka wersji kompiluje się, ale w czasie działania kończy się błędami typu NoSuchMethodError
    //   albo ClassNotFoundException — w miejscach pozornie niezwiązanych ze zmianą.
    //
    // DOBRA PRAKTYKA: nie wpisuj wersji przy bibliotekach z BOM-u; aktualizuj całość, podnosząc wersję Boota
    //   w parent pom. Dlaczego? Autorzy Boota testują razem dokładnie ten zestaw wersji.

    // =================================================================================================
    // 4. STARTER ZMIENIA ŚCIEŻKĘ KLAS, A ŚCIEŻKA KLAS — KONFIGURACJĘ (dlaczego moduł na dział)
    // =================================================================================================
    //
    // Moduł s03-boot-basics ma tylko spring-boot-starter i spring-boot-starter-jackson. Test sekcji 4:
    //   • ClassUtils.isPresent("tools.jackson.databind.json.JsonMapper") → true;
    //     ClassUtils.isPresent("org.springframework.web.servlet.DispatcherServlet") → false;
    //   • z listy TechnologyCheck.KNOWN obecne są tylko: JSON (Jackson 3), Logowanie (Logback), YAML;
    //   • w kontekście Boot04Application jest bean "jacksonJsonMapper", a nie ma żadnego beana serwera WWW.
    // Tak samo działa @ConditionalOnClass: pyta o jedną charakterystyczną klasę (TechnologyCheck to jego „ręczna” wersja).
    //
    // Powtórka z s00_start/Start02MultiModule: kurs ma OSOBNY moduł Mavena na każdy dział właśnie dlatego.
    // Starter spring-boot-starter-security dodany do jednego wspólnego modułu włączyłby autokonfigurację
    // bezpieczeństwa we WSZYSTKICH lekcjach — nagle każdy punkt końcowy (endpoint) wymagałby logowania.
    // Osobny moduł = osobna ścieżka klas = dział ma tylko swoje startery.

    // =================================================================================================
    // 5. CO NAPRAWDĘ JEST NA ŚCIEŻCE KLAS — dependency:tree i okno Maven (tylko opis — bez testu)
    // =================================================================================================
    //
    //   ./mvnw -pl s03-boot-basics dependency:tree          (Windows: mvnw.cmd -pl s03-boot-basics dependency:tree)
    //
    // Fragment prawdziwego wyniku (uruchomione dla tego modułu):
    //   [INFO] springlearning:s03-boot-basics:jar:1.0-SNAPSHOT
    //   [INFO] +- org.springframework.boot:spring-boot-starter:jar:4.1.1:compile
    //   [INFO] |  +- org.springframework.boot:spring-boot-starter-logging:jar:4.1.1:compile
    //   [INFO] |  |  +- ch.qos.logback:logback-classic:jar:1.5.38:compile
    //   [INFO] |  +- org.springframework.boot:spring-boot-autoconfigure:jar:4.1.1:compile
    //   [INFO] |  |  \- org.springframework.boot:spring-boot:jar:4.1.1:compile
    //   [INFO] |  |     \- org.springframework:spring-context:jar:7.0.9:compile
    //   [INFO] |  +- jakarta.annotation:jakarta.annotation-api:jar:3.0.0:compile
    //   [INFO] |  \- org.yaml:snakeyaml:jar:2.6:compile
    //   [INFO] +- org.springframework.boot:spring-boot-starter-jackson:jar:4.1.1:compile
    //   [INFO] |  \- org.springframework.boot:spring-boot-jackson:jar:4.1.1:compile
    //   [INFO] |     \- tools.jackson.core:jackson-databind:jar:3.1.5:compile
    //   [INFO] |        +- com.fasterxml.jackson.core:jackson-annotations:jar:2.21:compile
    //   [INFO] |        \- tools.jackson.core:jackson-core:jar:3.1.5:compile
    //   [INFO] \- org.springframework.boot:spring-boot-starter-test:jar:4.1.1:test
    //   …
    // Jak czytać: wcięcie = „kto kogo dociągnął”; ostatnie pole = zakres (compile — w aplikacji, test — tylko
    // w testach). Ciekawostka: jackson-annotations ma wersję 2.21 i stary pakiet com.fasterxml — Jackson 3 celowo
    // korzysta z adnotacji z linii 2.x, żeby @JsonProperty itp. działały w obu wersjach.
    //
    // W IntelliJ: okno Maven (pasek po prawej) → s03-boot-basics → Dependencies — to samo drzewo do rozwijania.
    // IntelliJ ma też analizator zależności (Dependency Analyzer), który pokazuje konflikty wersji.
    //
    // DOBRA PRAKTYKA: gdy „klasa nie istnieje” albo „Boot nie konfiguruje X”, zacznij od dependency:tree.
    //   Dlaczego? To jedyna pewna odpowiedź na pytanie, co JEST na ścieżce klas — a od tego zależą warunki
    //   @ConditionalOnClass (lekcja Boot01).

    // =================================================================================================
    // 6. DEVTOOLS — automatyczny restart w czasie pisania kodu (tylko opis — bez testu)
    // =================================================================================================
    //
    // spring-boot-devtools (narzędzia programisty) to NIE starter, ale moduł dodawany tak:
    //   <dependency>
    //       <groupId>org.springframework.boot</groupId>
    //       <artifactId>spring-boot-devtools</artifactId>
    //       <optional>true</optional>      ← nie przechodzi dalej do projektów, które zależą od Twojego
    //   </dependency>
    // (W modułach kursu go NIE dodajemy — tylko opis.)
    // Co daje: po zmianie skompilowanych klas aplikacja sama się restartuje — szybciej niż ręczny stop/start, bo
    // biblioteki (jary) zostają w jednym ładowaczu klas, a przeładowywany jest tylko drugi, z Twoimi klasami.
    // Kiedy NIE działa (sprawdzone w jarach 4.1.1):
    //   • w testach — DevTools wykrywa na stosie wywołań klasy JUnit i testów Boota i się nie włącza;
    //   • w spakowanym JAR (java -jar) — restart działa tylko przy starcie z IDE/Mavena (zwykły ładowacz klas
    //     aplikacji), a plugin spring-boot-maven-plugin domyślnie w ogóle nie pakuje DevTools (excludeDevtools=true).
    // W IntelliJ restart wymaga skompilowania zmian: Build → Build Project (Ctrl+F9) albo automatyczne budowanie
    // (Settings → Build, Execution, Deployment → Compiler → Build project automatically).
    //
    // PUŁAPKA: „zmieniłem kod, a DevTools nic nie zrobił”.
    //   Dlaczego? DevTools obserwuje skompilowane klasy (target/classes), nie pliki .java. Bez budowania projektu
    //   w IntelliJ nic się w target/classes nie zmienia, więc nie ma czego restartować.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Starter = artefakt bez kodu (pusty jar + pom z zależnościami). Kod i autokonfiguracja są w module
     *     spring-boot-<technologia>.
     *   • Boot 4: spring-boot-<tech> (moduł), spring-boot-starter-<tech>, spring-boot-starter-<tech>-test.
     *   • Nowe nazwy: -web → -webmvc, -web-services → -webservices, -oauth2-* → -security-oauth2-*,
     *     -aop → -aspectj (starej nazwy brak). -classic = wszystkie autokonfiguracje jak w Boot 3 (na czas migracji).
     *   • Wersje: parent → spring-boot-starter-parent → spring-boot-dependencies (BOM). W pom.xml modułów bez wersji.
     *   • Nadpisanie wersji: właściwość, np. <mockito.version>; nigdy pojedynczej biblioteki z „rodziny”.
     *   • Starter → ścieżka klas → @ConditionalOnClass → beany. Dlatego kurs ma moduł na dział.
     *   • ./mvnw -pl <moduł> dependency:tree albo okno Maven w IntelliJ — co NAPRAWDĘ jest na ścieżce klas.
     *   • DevTools: <optional>true</optional>, restart po zmianie klas, wyłączony w testach i w spakowanym JAR;
     *     w IntelliJ trzeba zbudować projekt.
     *
     * PYTANIA KONTROLNE:
     *   1. Co jest w jarze spring-boot-starter-jackson-4.1.1.jar? Gdzie leży kod JacksonAutoConfiguration?
     *   2. Dlaczego przy spring-boot-starter-jackson w pom.xml modułu nie ma <version>? Skąd Maven ją bierze?
     *   3. Co się stanie? W pom.xml modułu dla Boota 4.1.1 wpisujesz spring-boot-starter-aop (bez wersji).
     *   4. ZNAJDŹ BŁĄD: ktoś „naprawił” błąd, wpisując przy spring-context <version>6.2.0</version>; reszta Springa
     *      pochodzi z BOM-u Boota 4.1.1. Kompiluje się, a aplikacja sypie się NoSuchMethodError. Dlaczego?
     *   5. Dodajesz zależność jackson-databind (bez startera). Czy w kontekście pojawi się bean JsonMapper od Boota?
     *   6. Co się stanie, gdy do modułu s03-boot-basics dodasz spring-boot-starter-webmvc? Wymień dwie zmiany
     *      w działaniu aplikacji lekcji.
     *   7. Po co jest spring-boot-starter-classic i dlaczego kurs go nie używa?
     *   8. ZNAJDŹ BŁĄD: „Dodałem DevTools, zmieniam kod w IntelliJ, a aplikacja się nie restartuje.” Co sprawdzisz?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Boot04StartersExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): z podanej listy pełnych nazw klas zwróć te, które SĄ na ścieżce klas tego modułu,
     * posortowane alfabetycznie (bez powtórzeń).
     * Podpowiedź: {@code ClassUtils.isPresent(nazwa, null)} (null = domyślny ładowacz klas), potem
     * {@code stream().filter(...).distinct().sorted().toList()}.
     */
    static List<String> exercise1(List<String> classNames) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): dopasuj starter Boota 4 do potrzeby. Zwróć mapę „potrzeba → nazwa startera” jako
     * {@code TreeMap} (klucze posortowane), z DOKŁADNIE takimi kluczami:
     * <pre>
     *   "AOP (aspekty)", "JSON", "REST API (Spring MVC)", "baza danych przez JPA", "migracje bazy (Flyway)",
     *   "testy kontrolerów MVC", "walidacja danych"
     * </pre>
     * Podpowiedź: sekcja 2 lekcji (nazwy z Boota 4!) i CLAUDE.md, sekcja 3. Testowy starter MVC kończy się na -test.
     */
    static Map<String, String> exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): przegląd pom.xml po migracji z Boota 3. Dostajesz listę nazw starterów; zwróć
     * {@code TreeMap} „stara nazwa → nazwa w Boot 4” TYLKO dla tych, które trzeba zmienić (przestarzałe albo
     * usunięte — tabela w sekcji 2). Pozostałe pomiń.
     * Uwaga: {@code spring-boot-starter-webmvc} jest poprawny — nie myl go ze {@code spring-boot-starter-web}.
     * Podpowiedź: stała mapa zmian + porównanie CAŁYCH nazw ({@code equals}/{@code get}), nie {@code contains}.
     */
    static Map<String, String> exercise3(List<String> starters) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Tylko META-INF/MANIFEST.MF, LICENSE.txt i NOTICE.txt — żadnej klasy. JacksonAutoConfiguration leży w module
     *      spring-boot-jackson-4.1.1.jar (razem z plikiem .imports). Sprawdzone testem (sekcja 1).
     *   2. Wersję podaje BOM: parent pom kursu dziedziczy po spring-boot-starter-parent 4.1.1, a ten po
     *      spring-boot-dependencies 4.1.1, który ma <dependencyManagement> z wersjami.
     *   3. Maven nie zbuduje projektu — w BOM-ie 4.1.1 nie ma artefaktu spring-boot-starter-aop, więc nie ma skąd
     *      wziąć wersji: „'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-aop:jar
     *      is missing.” (sprawdzone uruchomieniem Mavena na próbnym pom.xml). Nowa nazwa: spring-boot-starter-aspectj.
     *   4. Mieszanka wersji: spring-context 6.2.0 obok spring-core/spring-beans 7.0.9. Klasy jednej wersji wołają
     *      metody, których w drugiej nie ma — błąd wychodzi dopiero w czasie działania. Usuń <version> i pozwól BOM-owi
     *      dobrać wersję.
     *   5. Nie. Sama biblioteka nie przynosi modułu spring-boot-jackson, a w nim jest JacksonAutoConfiguration
     *      i jej plik .imports. Boot nie ma czego załadować — bean JsonMapper nie powstanie.
     *   6. (1) Typ aplikacji zmieni się z NONE na SERVLET — przy starcie każdej aplikacji lekcji ruszy wbudowany Tomcat.
     *      (2) Dojdą kandydaci z modułów webowych (np. WebMvcAutoConfiguration, DispatcherServletAutoConfiguration),
     *      a JacksonAutoConfiguration.JsonProblemDetailsConfiguration zacznie pasować (pojawi się klasa ProblemDetail).
     *      Sprawdzone uruchomieniem z classpathem startera spring-boot-starter-webmvc. Testy lekcji s03, które
     *      oczekują braku weba, przestałyby przechodzić — dlatego web ma własny moduł (s04_web_rest).
     *   7. To most na czas migracji z Boota 3: dołącza kod WSZYSTKICH modułów autokonfiguracji (bez bibliotek), więc
     *      aplikacja zachowuje się jak w Boot 3. Kurs uczy docelowego sposobu (starter na technologię) i chce, żeby
     *      raport warunków pokazywał tylko to, co dział naprawdę używa.
     *   8. Czy projekt jest budowany po zmianie (Build → Build Project / automatyczne budowanie) — DevTools patrzy na
     *      target/classes. Czy aplikacja jest uruchomiona z IDE (nie jako spakowany JAR) i czy to nie test.
     */
    // </editor-fold>
}

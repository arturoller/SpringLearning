package s03_boot_basics.boot04_starters;

import java.util.List;

/**
 * <pre>
 * TEMAT: Startery i zarządzanie wersjami — skąd biorą się biblioteki na classpath
 *        (starter = starter, zestaw zależności pod jedną nazwą; BOM, bill of materials = spis części z wersjami;
 *         dependency management = zarządzanie zależnościami; managed version = wersja zarządzana)
 *
 * W SKRÓCIE:
 *   Starter to prawie pusty jar: w środku nie ma ani jednej klasy, tylko pom.xml z listą zależności. Dodajesz JEDNĄ
 *   zależność (spring-boot-starter), a Maven dociąga kilkanaście bibliotek: Spring (kontener), Boot z autokonfiguracją,
 *   Logback (logowanie), SnakeYAML (pliki .yaml), jakarta.annotation. Numerów wersji nie piszesz — podaje je BOM
 *   spring-boot-dependencies, który dziedziczysz przez spring-boot-starter-parent. W Boot 4 startery są drobniejsze
 *   (zmodularyzowane): osobny starter na technologię i osobny starter testowy.
 *
 * ANALOGIA: zestaw z meblami „kuchnia w paczce”.
 *   Zamawiasz jeden kod („kuchnia komplet” — starter), a przychodzą szafki, blat i zlew (biblioteki). Katalog sklepu
 *   (BOM) mówi, który model szafki pasuje do którego blatu (zgodne wersje). Możesz zamówić inny blat (nadpisać
 *   wersję), ale wtedy sam odpowiadasz za to, czy pasuje.
 *
 * JAK TO DZIAŁA:
 *   s03-boot-basics/pom.xml:  spring-boot-starter (BEZ wersji)
 *     ↓ wersja z dependencyManagement w spring-boot-dependencies-4.1.1.pom (rodzic rodzica naszego pom.xml)
 *   spring-boot-starter-4.1.1.pom → zależności:
 *     spring-boot-starter-logging → logback-classic 1.5.38, log4j-to-slf4j, jul-to-slf4j
 *     spring-boot-autoconfigure → spring-boot → spring-context 7.0.9 → spring-aop, spring-beans, spring-expression,
 *                                                                     micrometer-observation
 *     jakarta.annotation-api 3.0.0, snakeyaml 2.6
 *   Podgląd: ./mvnw -pl s03-boot-basics dependency:tree
 *
 * SŁÓWKA:
 *   transitive dependency = zależność przechodnia (zależność Twojej zależności); scope = zasięg (compile — zawsze,
 *   test — tylko w testach, runtime — tylko przy uruchomieniu, provided — dostarczy środowisko); optional = opcjonalna
 *   (nie przechodzi dalej); deprecated = przestarzały; relocation = przeniesienie; override = nadpisanie;
 *   code source = źródło kodu (jar albo katalog, z którego załadowano klasę); tree = drzewo.
 *
 * ZOBACZ TEŻ: s00_start/Start02MultiModule (moduły, classpath, pierwsze startery), s03_boot_basics/Boot01AutoConfiguration
 *             (jak klasy z classpath włączają autokonfigurację), s03_boot_basics/Boot02WritingAutoConfiguration,
 *             t30_build_modules/Build01MavenBasics (Maven), t30_build_modules/Build02JarClasspath (jar i classpath),
 *             t30_build_modules/Build03Modules (moduły).
 * </pre>
 */
public final class Boot04Starters {

    private Boot04Starters() {
    }

    // =================================================================================================
    // 1. STARTER = pom.xml z zależnościami, prawie bez kodu
    // =================================================================================================
    //
    // Zajrzyj do jara spring-boot-starter-4.1.1.jar (IntelliJ: External Libraries). W środku są tylko
    // META-INF/MANIFEST.MF, LICENSE.txt i NOTICE.txt — zero klas. Cała „treść” startera to jego pom.xml:
    //   <artifactId>spring-boot-starter</artifactId>
    //   <description>Core starter, including auto-configuration support, logging and YAML</description>
    //   <dependencies> spring-boot-starter-logging, spring-boot-autoconfigure, jakarta.annotation-api, snakeyaml
    // Test sekcji 1 otwiera ten jar z classpath i liczy w nim pliki .class: wychodzi 0.
    //
    // Po co więc startery? Żeby nie dobierać ręcznie kilkunastu bibliotek i ich zgodnych wersji. „Chcę aplikację
    // webową” = jeden starter spring-boot-starter-webmvc, a nie: Spring MVC + Tomcat + Jackson + konwertery + ...
    //
    // DOBRA PRAKTYKA: zależności dodawaj starterami, a nie pojedynczymi bibliotekami Springa. Dlaczego? Starter
    //   przynosi też moduł z autokonfiguracją danej technologii (w Boot 4 np. spring-boot-jackson) — sama biblioteka
    //   (np. jackson-databind) da Ci klasy, ale Boot nie skonfiguruje dla niej beanów.

    // =================================================================================================
    // 2. CO PRZYNOSI spring-boot-starter (sprawdzone na classpath tego modułu)
    // =================================================================================================
    //
    //   org.springframework.context.ApplicationContext          spring-context (kontener IoC)
    //   org.springframework.boot.autoconfigure.SpringBootApplication   spring-boot-autoconfigure
    //   ch.qos.logback.classic.Logger                           logback-classic (domyślne logowanie)
    //   org.slf4j.Logger                                        slf4j-api (wspólne API logowania)
    //   org.yaml.snakeyaml.Yaml                                 snakeyaml (obsługa application.yaml)
    //   jakarta.annotation.PostConstruct                        jakarta.annotation-api ({@code @PostConstruct}, {@code @PreDestroy})
    //   io.micrometer.observation.Observation                   micrometer-observation (przyszło ze spring-context 7!)
    // Ostatni wiersz to typowa zależność przechodnia: nikt jej nie dodał wprost, a jest — bo potrzebuje jej Spring.

    // =================================================================================================
    // 3. CZEGO spring-boot-starter NIE przynosi
    // =================================================================================================
    //
    //   org.apache.catalina.startup.Tomcat                      serwer Tomcat     → spring-boot-starter-tomcat (w -webmvc)
    //   org.springframework.web.servlet.DispatcherServlet       Spring MVC        → spring-boot-starter-webmvc
    //   tools.jackson.databind.ObjectMapper                     Jackson 3 (JSON)  → spring-boot-starter-jackson
    //   org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration   → moduł spring-boot-jackson
    //   org.springframework.jdbc.core.JdbcTemplate              JDBC              → spring-boot-starter-jdbc
    // Dlatego Boot01Application nie ma serwera WWW ani beanów JSON: nie ma klas → warunki {@code @ConditionalOnClass}
    // niespełnione → brak beanów (Boot01AutoConfiguration, sekcja 2).
    //
    // NOWOŚĆ W BOOT 4: Jackson 3 ma nowy groupId tools.jackson i nowe pakiety (tools.jackson.databind zamiast
    //   com.fasterxml.jackson.databind). Stare importy z tutoriali do Boot 3 nie skompilują się z Jacksonem 3.
    //   (Adnotacje, np. {@code @JsonProperty}, zostały w com.fasterxml.jackson.annotation.)
    //
    // PUŁAPKA: ClassUtils.isPresent w TEŚCIE widzi classpath testów — czyli także zależności o zasięgu test
    //   (JUnit, AssertJ, Mockito ze spring-boot-starter-test). Dlaczego to ważne? Aplikacja uruchomiona przez main tych
    //   klas NIE ma. Sprawdzając „czy biblioteka jest w aplikacji”, patrz na zasięg w dependency:tree.

    // =================================================================================================
    // 4. BOOT 4 — startery zmodularyzowane
    // =================================================================================================
    //
    // Spring Boot 4 podzielił duży jar spring-boot-autoconfigure na moduły technologii: spring-boot-jackson,
    // spring-boot-webmvc, spring-boot-tomcat, spring-boot-security ... Każdy ma swoje klasy, swoją autokonfigurację
    // (własny plik .imports) i swój starter. Skutki dla Ciebie:
    //   • spring-boot-starter-webmvc — aplikacja webowa (Spring MVC + Tomcat + Jackson). Dawny spring-boot-starter-web
    //     nadal istnieje w 4.1.1, ale jego opis mówi „deprecated in favor of spring-boot-starter-webmvc” (przestarzały).
    //   • testy mają WŁASNE startery: spring-boot-starter-webmvc-test ({@code @WebMvcTest}, MockMvc),
    //     spring-boot-starter-data-jpa-test ({@code @DataJpaTest}) itd. Sam spring-boot-starter-test daje tylko JUnit,
    //     AssertJ, Mockito, Spring Test i ogólne narzędzia Boota (np. ApplicationContextRunner).
    //   • dla migracji starych projektów są „klasyczne” startery: spring-boot-starter-classic i
    //     spring-boot-starter-test-classic (opis: „full auto-configuration support”) — przynoszą wszystkie moduły
    //     autokonfiguracji naraz, jak w Boot 3.
    //
    // DOBRA PRAKTYKA: w nowym projekcie używaj starterów zmodularyzowanych, nie „classic”. Dlaczego? Mniejszy classpath
    //   = mniej autokonfiguracji do oceny przy starcie, szybszy start i mniej niespodzianek (sekcja 3 lekcji Boot02).

    // =================================================================================================
    // 5. BOM — skąd są wersje (spring-boot-starter-parent → spring-boot-dependencies)
    // =================================================================================================
    //
    // Nasz pom.xml → rodzic spring-learning → rodzic spring-boot-starter-parent 4.1.1 → rodzic
    // spring-boot-dependencies 4.1.1. Ten ostatni to BOM: sekcja <dependencyManagement> z wersjami ~setek bibliotek
    // oraz <properties> z numerami, np.:
    //   <logback.version>1.5.38</logback.version>        <snakeyaml.version>2.6</snakeyaml.version>
    //   <tomcat.version>11.0.24</tomcat.version>         <junit-jupiter.version>6.0.3</junit-jupiter.version>
    //   <jackson-bom.version>3.1.5</jackson-bom.version> (Jackson 3, tools.jackson)
    //   <jackson-2-bom.version>2.21.5</jackson-2-bom.version> (Jackson 2, com.fasterxml.jackson — dla starszych bibliotek)
    // starter-parent dodaje do tego ustawienia pluginów (kompilator z -parameters, spring-boot-maven-plugin, kodowanie).
    // Test sekcji 5 odczytuje wersje Boota i Springa w czasie działania: SpringBootVersion.getVersion() = 4.1.1,
    // SpringVersion.getVersion() = 7.0.9.
    //
    // Nie możesz dziedziczyć po starter-parent (firma ma własnego rodzica)? Wtedy importujesz sam BOM:
    //   <dependencyManagement><dependencies><dependency>
    //     <groupId>org.springframework.boot</groupId><artifactId>spring-boot-dependencies</artifactId>
    //     <version>4.1.1</version><type>pom</type><scope>import</scope>
    //   </dependency></dependencies></dependencyManagement>

    // =================================================================================================
    // 6. NADPISANIE WERSJI ZARZĄDZANEJ — możliwe, ale rzadko rozsądne
    // =================================================================================================
    //
    // Przy dziedziczeniu po spring-boot-starter-parent wersję zmienia się WŁAŚCIWOŚCIĄ w swoim pom.xml:
    //   <properties>
    //     <jackson-bom.version>3.1.6</jackson-bom.version>   ← przykład: nowsza poprawka Jacksona 3
    //   </properties>
    // Maven podstawi ją wszędzie, gdzie BOM używa ${jackson-bom.version}, więc zmieni się CAŁY zestaw modułów Jacksona
    // naraz (to jest zaleta właściwości: nie da się zmienić jednego modułu i zapomnieć o reszcie).
    //   Uwaga: działa to tylko przy dziedziczeniu po starter-parent. Przy imporcie BOM-u (sekcja 5) właściwości z Twojego
    //   pom.xml nie wpływają na zaimportowany BOM — trzeba wtedy zaimportować BOM danej biblioteki PRZED
    //   spring-boot-dependencies.
    //
    // PUŁAPKA: nadpisanie wersji „bo jest nowsza”. Dlaczego groźne? Spring Boot 4.1.1 jest testowany z DOKŁADNIE
    //   tymi wersjami z BOM-u. Inna wersja (szczególnie inna główna, np. Jackson 2 zamiast 3 albo Hibernate 6 zamiast 7)
    //   może się skompilować, a wywrócić w czasie działania (NoSuchMethodError, ClassNotFoundException).
    // DOBRA PRAKTYKA: zamiast nadpisywać wersje, podnieś wersję Spring Boota (np. 4.1.1 → 4.1.2). Dlaczego? Nowa
    //   wersja Boota przynosi spójny zestaw poprawek wszystkich bibliotek. Nadpisuj tylko w wyjątkowych sytuacjach
    //   (pilna poprawka bezpieczeństwa), z komentarzem „dlaczego” i z planem usunięcia.

    // =================================================================================================
    // 7. JAK SPRAWDZIĆ, CO JEST NA CLASSPATH
    // =================================================================================================
    //
    //   ./mvnw -pl s03-boot-basics dependency:tree                         całe drzewo (z zasięgami)
    //   ./mvnw -pl s03-boot-basics dependency:tree -Dincludes=org.yaml     tylko pasujące (skąd przyszło snakeyaml?)
    //   IntelliJ: okno Maven → moduł → Dependencies; albo External Libraries w drzewie projektu
    //   w kodzie: ClassUtils.isPresent("pełna.Nazwa", classLoader)        jest / nie ma (ClasspathInspector.isPresent)
    //   w kodzie: Klasa.class.getProtectionDomain().getCodeSource().getLocation()   z którego jara? (ćwiczenie 2)
    // Test sekcji 7 sprawdza, że ApplicationContext pochodzi z jara spring-context-7.0.9.jar.
    //
    // DOBRA PRAKTYKA: zanim dodasz zależność „bo czegoś brakuje”, sprawdź dependency:tree. Dlaczego? Często biblioteka
    //   już jest (przechodnio), tylko w innej wersji albo zasięgu — druga deklaracja tylko zaciemni sprawę.

    /** Fragment prawdziwego wyniku {@code ./mvnw -pl s03-boot-basics dependency:tree} (do ćwiczenia 3). */
    public static final String DEPENDENCY_TREE_SAMPLE = """
            springlearning:s03-boot-basics:jar:1.0-SNAPSHOT
            +- org.springframework.boot:spring-boot-starter:jar:4.1.1:compile
            |  +- org.springframework.boot:spring-boot-starter-logging:jar:4.1.1:compile
            |  |  +- ch.qos.logback:logback-classic:jar:1.5.38:compile
            |  |  |  \\- ch.qos.logback:logback-core:jar:1.5.38:compile
            |  +- jakarta.annotation:jakarta.annotation-api:jar:3.0.0:compile
            |  \\- org.yaml:snakeyaml:jar:2.6:compile
            \\- org.springframework.boot:spring-boot-starter-test:jar:4.1.1:test
               +- org.assertj:assertj-core:jar:3.27.7:test
               +- org.junit.jupiter:junit-jupiter:jar:6.0.3:test
               \\- org.mockito:mockito-core:jar:5.23.0:test
            """;

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Starter = pom.xml z zależnościami (spring-boot-starter-4.1.1.jar ma 0 klas).
     *   • spring-boot-starter: spring-context, spring-boot(-autoconfigure), logback, slf4j, snakeyaml, jakarta.annotation.
     *     NIE ma: Tomcata, Spring MVC, Jacksona, JDBC.
     *   • Boot 4: startery per technologia (spring-boot-starter-webmvc; -web przestarzały), osobne startery testowe
     *     (-webmvc-test, -data-jpa-test), moduły autokonfiguracji (spring-boot-jackson...), „classic” do migracji.
     *   • Wersje: BOM spring-boot-dependencies (przez spring-boot-starter-parent) — w module bez numerów wersji.
     *   • Nadpisanie: właściwość, np. {@code <jackson-bom.version>} — tylko w wyjątkowych sytuacjach; lepiej podnieś Boota.
     *   • Classpath: dependency:tree (-Dincludes=...), ClassUtils.isPresent, getCodeSource().getLocation().
     *
     * PYTANIA KONTROLNE:
     *   1. PYTANIE REKRUTACYJNE: czym jest starter Spring Boota i co zyskujesz, używając go zamiast pojedynczych bibliotek?
     *   2. Co się stanie? W tym module wywołasz ClasspathInspector.isPresent("org.apache.catalina.startup.Tomcat").
     *   3. Co się stanie? W tym module wywołasz ClasspathInspector.isPresent("io.micrometer.observation.Observation"),
     *      choć nikt nie dodał Micrometera do pom.xml.
     *   4. ZNAJDŹ BŁĄD: w module Boot 4 ktoś dodał {@code <artifactId>jackson-databind</artifactId>} z groupId
     *      com.fasterxml.jackson.core i dziwi się, że Boot nie tworzy beana do obsługi JSON. Co jest nie tak (dwie rzeczy)?
     *   5. ZNAJDŹ BŁĄD: {@code <jackson-bom.version>2.21.5</jackson-bom.version>} w pom.xml projektu Boot 4.1.1, „żeby mieć
     *      sprawdzonego Jacksona 2”. Dlaczego to zły pomysł?
     *   6. Skąd moduł s03-boot-basics wie, że ma użyć logback-classic w wersji 1.5.38?
     *   7. Test z ClassUtils.isPresent("org.assertj.core.api.Assertions") zwraca true. Czy to znaczy, że AssertJ jest
     *      w aplikacji uruchomionej przez main?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Boot04StartersExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): z podanej listy pełnych nazw klas zwróć te, które SĄ na classpath, posortowane.
     * Podpowiedź: ClasspathInspector.isPresent, stream().filter(...).sorted().toList().
     */
    static List<String> exercise1(List<String> classNames) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć NAZWĘ PLIKU jara, z którego załadowano podaną klasę, np. dla ApplicationContext —
     * „spring-context-7.0.9.jar”. Podpowiedź: type.getProtectionDomain().getCodeSource().getLocation() daje URL;
     * zamień go na ścieżkę (Path.of(url.toURI())) i weź getFileName().toString(). Wyjątek URISyntaxException opakuj
     * w IllegalStateException.
     */
    static String exercise2(Class<?> type) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): z tekstu dependency:tree (np. DEPENDENCY_TREE_SAMPLE) zwróć posortowaną listę
     * „artifactId:wersja” wszystkich zależności o podanym zasięgu (scope). Linia zależności po obcięciu znaków drzewa
     * ({@code |}, {@code +-}, {@code \-}, spacje) ma postać {@code groupId:artifactId:typ:wersja:zasięg}; pierwszą linię
     * (sam projekt, 4 części) pomiń. Przykład dla „test”: [assertj-core:3.27.7, junit-jupiter:6.0.3, mockito-core:5.23.0,
     * spring-boot-starter-test:4.1.1].
     * Podpowiedź: text.lines(); znaki drzewa obetniesz przez replaceFirst("^[^a-z]+", "") (groupId zaczyna się małą
     * literą, a znaki drzewa nie są literami); potem split(":") i parts.length == 5.
     */
    static List<String> exercise3(String dependencyTree, String scope) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Starter to artefakt Mavena z samym pom.xml (bez kodu), który zbiera zgodny zestaw bibliotek dla jednej
     *      technologii — razem z modułem autokonfiguracji. Zyskujesz: jedną zależność zamiast kilkunastu, zgodne wersje
     *      (z BOM-u) i beany skonfigurowane przez Boota.
     *   2. Zwróci false — Tomcat przychodzi dopiero ze spring-boot-starter-tomcat (część spring-boot-starter-webmvc).
     *      Sprawdzone testem w Boot04StartersTest (sekcja 3).
     *   3. Zwróci true — micrometer-observation jest zależnością przechodnią spring-context 7.0.9. Sprawdzone testem
     *      (sekcja 2) i w dependency:tree.
     *   4. (a) W Boot 4 domyślny jest Jackson 3: groupId tools.jackson.core, pakiety tools.jackson.* — com.fasterxml to
     *      Jackson 2. (b) Sama biblioteka nie przynosi autokonfiguracji: JacksonAutoConfiguration leży w module
     *      spring-boot-jackson, który dodaje starter spring-boot-starter-jackson (albo spring-boot-starter-webmvc).
     *      Właściwą zależnością jest więc starter, nie goła biblioteka.
     *   5. jackson-bom.version w Boot 4 steruje Jacksonem 3 (BOM tools.jackson:jackson-bom). Wpisanie 2.21.5 każe Mavenowi
     *      szukać tools.jackson:jackson-bom:2.21.5 — takiej wersji Jacksona 3 nie ma, więc budowanie się wywróci (a gdyby
     *      była — wersja niezgodna z Bootem). Do wersji Jacksona 2 służy osobna właściwość jackson-2-bom.version, ale
     *      i tak: zamiast nadpisywać, podnieś wersję Boota.
     *   6. Z BOM-u: s03-boot-basics dziedziczy po spring-learning → spring-boot-starter-parent 4.1.1 →
     *      spring-boot-dependencies 4.1.1, gdzie jest {@code <logback.version>1.5.38</logback.version>} i wpis
     *      w dependencyManagement. Widać to w dependency:tree (ch.qos.logback:logback-classic:jar:1.5.38:compile).
     *   7. Nie. Test widzi classpath testów, a AssertJ ma zasięg test (przychodzi ze spring-boot-starter-test). Aplikacja
     *      uruchomiona przez main go nie ma. Sprawdzone testem (sekcja 3) i w dependency:tree (assertj-core:...:test).
     */
    // </editor-fold>
}

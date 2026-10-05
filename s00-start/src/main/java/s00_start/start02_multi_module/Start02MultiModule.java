package s00_start.start02_multi_module;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Projekt wielomodułowy Mavena — rodzic, moduły, classpath i startery
 *        (multi-module project = projekt wielomodułowy; parent = rodzic; module = moduł; classpath = ścieżka klas;
 *         starter = starter, gotowy zestaw zależności pod jedną nazwą)
 *
 * W SKRÓCIE:
 *   Kurs to JEDEN projekt Mavena złożony z wielu MODUŁÓW: jeden dział = jeden moduł (s00-start, s01-core-ioc, ...).
 *   Główny pom.xml (rodzic) trzyma wspólne ustawienia i wersje, a każdy moduł ma własny pom.xml z zależnościami TYLKO
 *   swojego działu. Dlaczego tak? Spring Boot sam konfiguruje aplikację na podstawie tego, co znajdzie na classpath.
 *   Gdyby wszystkie działy były w jednym module, Spring Security z działu 12 „zamknąłby na hasło” lekcje z działu 4.
 *
 * ANALOGIA: blok mieszkalny.
 *   Budynek (rodzic) ma wspólny dach, instalację i regulamin (wersje bibliotek, kodowanie, wersja Javy). Każde
 *   mieszkanie (moduł) ma własne meble (zależności). Alarm zamontowany w mieszkaniu nr 12 nie dzwoni w mieszkaniu nr 4.
 *
 * JAK TO DZIAŁA:
 *   pom.xml (rodzic)            packaging = pom, parent = spring-boot-starter-parent 4.1.1, lista modules
 *   └── s00-start/pom.xml       parent = rodzic kursu; zależności: spring-boot-starter-webmvc (+ testowe)
 *         ↓ Maven buduje classpath modułu: Spring MVC, Tomcat, Jackson... ale NIE Security, NIE JPA
 *         ↓ Spring Boot przy starcie sprawdza classpath: „jest DispatcherServlet → skonfiguruję aplikację webową”
 *   kontekst aplikacji          beany dodane automatycznie: dispatcherServlet, serwer Tomcat, konwertery JSON...
 *
 * SŁÓWKA:
 *   dependency = zależność (biblioteka potrzebna projektowi); scope = zasięg zależności (np. test — tylko w testach);
 *   artifact = artefakt (plik biblioteki w repozytorium Mavena); BOM (bill of materials) = spis wersji bibliotek;
 *   auto-configuration = autokonfiguracja; application context = kontekst aplikacji (kontener z beanami);
 *   bean = ziarno, czyli obiekt zarządzany przez Springa; wrapper = opakowanie (Maven Wrapper — skrypt mvnw).
 *
 * ZOBACZ TEŻ: t30_build_modules/Build01MavenBasics (Maven od podstaw), t30_build_modules/Build02JarClasspath (classpath),
 *             s00_start/Start03FirstApplication (pierwsza aplikacja), s00_start/Start01HowToUse (nazwy modułów).
 * </pre>
 */
public final class Start02MultiModule {

    private Start02MultiModule() {
    }

    // =================================================================================================
    // 1. RODZIC I MODUŁY
    // =================================================================================================
    //
    // Główny pom.xml w korzeniu repozytorium:
    //   <parent> spring-boot-starter-parent 4.1.1 </parent>   ← wersje setek bibliotek „w pakiecie”
    //   <packaging>pom</packaging>                            ← rodzic nie ma kodu, tylko ustawienia
    //   <modules> <module>s00-start</module> ... </modules>   ← lista działów
    //   <java.version>21</java.version>                       ← jedna wersja Javy dla całego kursu
    //
    // Pom modułu (s00-start/pom.xml) wskazuje rodzica i dodaje swoje zależności BEZ numerów wersji — wersje przychodzą
    // od rodzica. Dzięki temu cały kurs używa dokładnie tej samej wersji Springa.
    //
    // Skąd się bierze taki pom.xml? Zwykły (jednomodułowy) projekt Spring Boot generuje Spring Initializr
    // (initializr = inicjalizator, generator szkieletu projektu): https://start.spring.io albo w IntelliJ File → New →
    // Project → Spring Boot. Wybierasz Maven, Javę, wersję Boota i startery (Dependencies), a dostajesz ZIP z pom.xml,
    // klasą @SpringBootApplication, application.properties i pustym testem. Nasz kurs to kilka takich projektów pod
    // jednym rodzicem — układ katalogów (src/main/java, src/test/java) jest dokładnie taki sam jak z Initializra.
    //
    // DOBRA PRAKTYKA: nie wpisuj wersji przy zależnościach Springa w modułach. Dlaczego? Biblioteki Springa muszą do siebie
    //   pasować; wersja wpisana ręcznie w jednym miejscu łatwo rozjedzie się z resztą i da dziwne błędy w czasie działania.

    // =================================================================================================
    // 2. CLASSPATH MODUŁU — tylko jego zależności
    // =================================================================================================
    //
    // Classpath (ścieżka klas) to lista bibliotek, z których JVM ładuje klasy. Każdy moduł ma WŁASNY classpath.
    // W s00-start jest Spring MVC, ale nie ma Spring Security ani Spring Data JPA — sprawdza to test sekcji 2
    // (metoda isOnClasspath poniżej, zbudowana na ClassUtils.isPresent ze Springa).
    //
    // PUŁAPKA: „dodam zależność w module s12-security, a użyję jej w s04-web-rest”. Nie zadziała — dlaczego? Moduł widzi
    //   tylko swoje zależności (i te od rodzica). To celowe: izolacja działów.

    /** Czy klasa o podanej pełnej nazwie jest na classpath tego modułu? Nie ładuje klasy do użytku — tylko sprawdza. */
    static boolean isOnClasspath(String fullyQualifiedClassName) {
        return org.springframework.util.ClassUtils.isPresent(fullyQualifiedClassName, Start02MultiModule.class.getClassLoader());
    }

    // =================================================================================================
    // 3. STARTERY I AUTOKONFIGURACJA
    // =================================================================================================
    //
    // Starter to jedna zależność, która przyciąga cały zestaw bibliotek. spring-boot-starter-webmvc przyciąga Spring MVC,
    // serwer Tomcat i Jackson (JSON). Spring Boot przy starcie patrzy na classpath i dokłada beany: skoro jest Spring MVC,
    // tworzy bean dispatcherServlet (centralny „rozdzielacz” zapytań HTTP). Skoro NIE ma sterownika bazy ani JDBC,
    // NIE tworzy beana DataSource (źródła danych). Test sekcji 3 sprawdza oba fakty na pustej aplikacji Start02Application.
    //
    // PUŁAPKA: w Spring Boot 4 startery zostały podzielone na mniejsze moduły i część zmieniła nazwy — dawny
    //   spring-boot-starter-web jest przestarzały (deprecated) na rzecz spring-boot-starter-webmvc, a narzędzia testów
    //   webowych (MockMvc, @WebMvcTest) NIE są już w spring-boot-starter-test — trzeba dodać spring-boot-starter-webmvc-test.
    //   Dlaczego to ważne? W internecie jest mnóstwo tutoriali do Boot 3 — ich pom.xml i importy nie zawsze zadziałają.

    /** Startery Boot 4 używane w kursie: krótka nazwa technologii → artefakt startera. */
    static final Map<String, String> STARTERS = Map.of(
            "web", "spring-boot-starter-webmvc",
            "validation", "spring-boot-starter-validation",
            "jpa", "spring-boot-starter-data-jpa",
            "security", "spring-boot-starter-security",
            "actuator", "spring-boot-starter-actuator");

    // =================================================================================================
    // 4. MAVEN WRAPPER — budowanie bez instalowania Mavena
    // =================================================================================================
    //
    // W korzeniu repozytorium są skrypty mvnw (Linux/macOS) i mvnw.cmd (Windows) oraz katalog .mvn/wrapper.
    // Przy pierwszym uruchomieniu wrapper pobiera właściwą wersję Mavena i potem jej używa. Polecenia:
    //   ./mvnw test                          wszystkie działy
    //   ./mvnw -pl s00-start test            jeden dział (-pl = project list, lista projektów/modułów)
    //   mvnw.cmd -pl s00-start test          to samo w Windows (PowerShell: .\mvnw.cmd ...)
    //
    // DOBRA PRAKTYKA: używaj mvnw zamiast „gołego” mvn. Dlaczego? Każdy (Ty, kolega, serwer CI) buduje tą samą wersją
    //   Mavena zapisaną w .mvn/wrapper/maven-wrapper.properties — koniec z „u mnie działa”.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Rodzic (pom.xml w korzeniu): packaging pom, parent spring-boot-starter-parent, lista modules, wspólne properties.
     *   • Moduł = dział: własny pom.xml z zależnościami tylko tego działu, BEZ numerów wersji (dziedziczy od rodzica).
     *   • Classpath modułu = jego zależności + zależności rodzica. Moduły nie widzą nawzajem swoich bibliotek.
     *   • Starter = zestaw bibliotek; Boot 4: spring-boot-starter-webmvc (dawniej -web), testy: -webmvc-test.
     *   • Autokonfiguracja: Boot tworzy beany zależnie od classpath (jest Spring MVC → dispatcherServlet).
     *   • ./mvnw -pl s00-start test — testy jednego działu; mvnw.cmd w Windows.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego kurs ma osobny moduł na każdy dział, zamiast jednego dużego projektu?
     *   2. Co się stanie? W module s00-start wywołasz isOnClasspath("org.springframework.data.jpa.repository.JpaRepository").
     *   3. Co się stanie? Dopiszesz w s00-start/pom.xml zależność spring-boot-starter-security i uruchomisz lekcję Start03.
     *   4. ZNAJDŹ BŁĄD: moduł ma zależności spring-boot-starter-webmvc i spring-boot-starter-test, ale test z adnotacją
     *      {@code @WebMvcTest} się nie kompiluje (kompilator nie zna tej adnotacji). Czego brakuje w Boot 4?
     *   5. Skąd moduł bierze wersję spring-boot-starter-webmvc, skoro w jego pom.xml jej nie ma?
     *   6. Czym różni się ./mvnw test od ./mvnw -pl s00-start test?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Start02MultiModuleExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwę artefaktu startera dla krótkiej nazwy technologii z mapy STARTERS,
     * a dla nieznanej nazwy rzuć IllegalArgumentException z komunikatem "Nieznana technologia: " + nazwa.
     * Przykład: "web" → "spring-boot-starter-webmvc". Podpowiedź: STARTERS.get(...) zwraca null, gdy klucza brak.
     */
    static String exercise1(String technology) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć nazwę zależności testowej odpowiadającej starterowi — Boot 4 dokleja "-test".
     * Przykład: "spring-boot-starter-webmvc" → "spring-boot-starter-webmvc-test". Gdy nazwa nie zaczyna się od
     * "spring-boot-starter-", rzuć IllegalArgumentException. Podpowiedź: String.startsWith.
     */
    static String exercise2(String starterArtifact) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): z listy pełnych nazw klas zwróć tylko te, których NIE ma na classpath tego modułu,
     * posortowane alfabetycznie. Podpowiedź: stream + filter(nazwa -> !isOnClasspath(nazwa)) + sorted() + toList()
     * (t16_streams/Streams03FilterMap).
     */
    static List<String> exercise3(List<String> classNames) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo startery zmieniają autokonfigurację CAŁEJ aplikacji. W jednym module Security z działu 12 zabezpieczyłby
     *      hasłem wszystkie wcześniejsze lekcje webowe, a JPA wymagałoby bazy danych od lekcji, które jej nie używają.
     *      Osobne moduły = osobne classpath = każdy dział ma tylko swoje biblioteki.
     *   2. Zwróci false — w s00-start nie ma Spring Data JPA (sprawdzone w Start02MultiModuleTest).
     *   3. Spring Security trafi na classpath, a autokonfiguracja zabezpieczy WSZYSTKIE adresy: zapytanie do /api/hello
     *      dostanie odpowiedź 401 (Unauthorized — nieautoryzowany), a w logu pojawi się wygenerowane hasło. Dokładnie
     *      ten problem rozwiązuje podział na moduły. Sprawdzone uruchomieniem (na kopii modułu z dodanym starterem).
     *   4. Zależności spring-boot-starter-webmvc-test (scope test). W Boot 4 spring-boot-starter-test zawiera tylko
     *      ogólne narzędzia (JUnit, AssertJ, Mockito, Spring Test); @WebMvcTest i @AutoConfigureMockMvc są w module
     *      spring-boot-webmvc-test, w pakiecie org.springframework.boot.webmvc.test.autoconfigure (w Boot 3 były gdzie
     *      indziej — stare importy z tutoriali też trzeba poprawić).
     *   5. Od rodzica: główny pom.xml dziedziczy po spring-boot-starter-parent, który zawiera BOM z wersjami wszystkich
     *      bibliotek Springa. Moduł dziedziczy po głównym pom.xml.
     *   6. ./mvnw test buduje i testuje wszystkie moduły z listy modules; -pl s00-start ogranicza pracę do jednego modułu
     *      (szybciej, a błąd w innym dziale nie przeszkadza).
     */
    // </editor-fold>
}

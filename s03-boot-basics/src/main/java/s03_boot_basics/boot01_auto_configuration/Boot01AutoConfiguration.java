package s03_boot_basics.boot01_auto_configuration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

/**
 * <pre>
 * TEMAT: Autokonfiguracja Spring Boota — skąd biorą się beany, których nie napisałeś
 *        (auto-configuration = autokonfiguracja; condition = warunek; back off = wycofać się;
 *         condition evaluation report = raport oceny warunków; classpath = ścieżka klas)
 *
 * W SKRÓCIE:
 *   Dołączasz starter (tu: spring-boot-starter-jackson), nie piszesz żadnego @Bean, a serwis dostaje gotowy
 *   JsonMapper. Zrobiła to autokonfiguracja: zwykłe klasy konfiguracji przygotowane przez autorów Boota, obłożone
 *   warunkami @ConditionalOn… Boot sprawdza warunki — czy biblioteka jest na ścieżce klas? czy nie zdefiniowałeś
 *   własnego beana? czy ustawienie jest włączone? — i tworzy tylko to, co pasuje. Twój bean wygrywa:
 *   autokonfiguracja się „wycofuje”. Nie ma tu magii — raport warunków mówi, co Boot utworzył, czego nie i DLACZEGO.
 *
 * ANALOGIA: hotel i lista przygotowania pokoju.
 *   Recepcja (Boot) szykuje pokój według listy: „jeśli gość przyjechał z dzieckiem — wstaw łóżeczko”, „jeśli gość
 *   przywiózł własną poduszkę — nie kładź hotelowej”, „minibar tylko na życzenie”. Lista z odhaczonymi punktami
 *   (raport warunków) leży w recepcji — zawsze możesz zapytać, dlaczego coś jest albo czego brakuje.
 *   Możesz też z góry powiedzieć „bez minibaru, proszę” (exclude).
 *
 * JAK TO DZIAŁA:
 *   1. @SpringBootApplication zawiera @EnableAutoConfiguration („włącz autokonfigurację”).
 *   2. Boot czyta z każdego jaru plik META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 *      — listę kandydatów (klas autokonfiguracji).
 *   3. Kandydaci są przetwarzani PO Twoich klasach (@Component, @Configuration, @Bean) — dlatego warunek
 *      „nie ma beana” widzi już Twoje beany.
 *   4. Każda klasa i każda metoda @Bean autokonfiguracji ma warunki. Boot je ocenia i zapisuje wynik
 *      w raporcie (ConditionEvaluationReport).
 *   5. Warunki spełnione → bean powstaje. Niespełnione → Boot go pomija i zapisuje powód.
 *
 * SŁÓWKA:
 *   enable = włącz; conditional on = warunkowo od; missing = brakujący; class = klasa; bean = ziarno (obiekt
 *   zarządzany przez Springa); match = dopasowanie; positive/negative matches = warunki spełnione/niespełnione;
 *   exclude = wyklucz; candidates = kandydaci; primary = główny (wybierany, gdy pasuje kilka beanów);
 *   imports = importy (lista do załadowania); filter = filtr; unconditional = bezwarunkowy.
 *
 * ZOBACZ TEŻ: s02_configuration/Cfg05Environment (@ConditionalOnProperty na Twoich beanach),
 *             s01_core_ioc/Ioc05MultipleCandidates (@Primary — kto wygrywa przy dwóch kandydatach),
 *             s03_boot_basics/Boot02WritingAutoConfiguration (piszesz własną autokonfigurację),
 *             s00_start/Start08Debugging (czytanie błędów startu),
 *             s15_actuator_logging (raport warunków przez /actuator/conditions),
 *             t19_annotations_reflection/Annotations05MiniFramework (jak framework czyta adnotacje).
 * </pre>
 */
public final class Boot01AutoConfiguration {

    private Boot01AutoConfiguration() {
    }

    // =================================================================================================
    // 1. @SpringBootApplication — TRZY ADNOTACJE W JEDNEJ
    // =================================================================================================
    //
    // @SpringBootApplication to adnotacja złożona (composed annotation). Nad Boot01Application tak naprawdę stoją:
    //
    //   @SpringBootConfiguration   „to jest GŁÓWNA konfiguracja aplikacji” — odmiana @Configuration; @SpringBootTest
    //                              szuka właśnie jej, idąc w górę drzewa pakietów od pakietu testu
    //   @EnableAutoConfiguration   „włącz autokonfigurację” — temat tej lekcji
    //   @ComponentScan(excludeFilters = { TypeExcludeFilter, AutoConfigurationExcludeFilter })
    //                              „skanuj mój pakiet i podpakiety w poszukiwaniu @Component” — z dwoma filtrami:
    //        • TypeExcludeFilter — pozwala innym beanom wykluczać klasy ze skanowania (korzystają z tego testy
    //          Boota, żeby nie wciągać do aplikacji klas testowych);
    //        • AutoConfigurationExcludeFilter — NIE skanuje klas autokonfiguracji (z adnotacją @AutoConfiguration
    //          albo wpisanych do pliku .imports). Autokonfiguracja ma wejść „swoimi drzwiami” — lekcja Boot02.
    //
    // Test sekcji 1 odczytuje te adnotacje z Boot01Application.
    // PYTANIE REKRUTACYJNE: z czego składa się @SpringBootApplication? (odpowiedź: powyżej — trzy adnotacje).
    //
    // DOBRA PRAKTYKA: klasę z @SpringBootApplication kładź w pakiecie NAD resztą kodu aplikacji.
    //   Dlaczego? @ComponentScan skanuje pakiet tej klasy i jego podpakiety — klasy „obok” albo „wyżej” nie zostaną
    //   znalezione. (W tym kursie każda lekcja ma własną klasę startową w swoim podpakiecie — sekcja 2 w CLAUDE.md.)

    // =================================================================================================
    // 2. SKĄD BOOT WIE, CO MOŻE SKONFIGUROWAĆ — pliki AutoConfiguration.imports
    // =================================================================================================
    //
    // Boot szuka w KAŻDYM jarze na ścieżce klas pliku:
    //     META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    // Jedna linia = pełna nazwa jednej klasy autokonfiguracji. W tym module (pliki sprawdza test sekcji 2,
    // liczby klas — odczytane z jarów):
    //     spring-boot-autoconfigure-4.1.1.jar   12 klas „ogólnych” (pula wątków, harmonogram zadań, JMX, AOP…)
    //     spring-boot-jackson-4.1.1.jar          1 klasa: org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
    //     (plus plik z lekcji Boot02 w samym module — o nim w następnej lekcji)
    // Żadnej autokonfiguracji webowej — jar z nią (spring-boot-webmvc) nie jest dołączony. To nie warunek ją
    // wyłączył: jej po prostu nie ma na liście kandydatów.
    //
    // ZMIANA W BOOT 4 (uwaga na starsze tutoriale): w Bootcie 2 i 3 prawie wszystkie autokonfiguracje (web, JPA,
    // Jackson…) leżały w JEDNYM dużym jarze spring-boot-autoconfigure. Boot 4 podzielił je na moduły
    // spring-boot-<technologia> (spring-boot-jackson, spring-boot-webmvc, spring-boot-data-jpa…) — każdy ma własne
    // autokonfiguracje i własny plik .imports, a starter dociąga odpowiedni moduł. Zmieniły się też pakiety:
    //     Boot 3:  org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
    //     Boot 4:  org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
    //
    // HISTORIA: w Bootcie 2 listę autokonfiguracji wpisywano do META-INF/spring.factories (pod kluczem
    //   org.springframework.boot.autoconfigure.EnableAutoConfiguration). Boot 2.7 wprowadził plik .imports
    //   i adnotację @AutoConfiguration, a od Boota 3.0 autokonfiguracji NIE rejestruje się już w spring.factories.
    //   Sam plik spring.factories nadal istnieje — dla innych rozszerzeń (np. słuchaczy zdarzeń, analizatorów błędów).
    //
    // Kolejność: autokonfiguracje ładuje AutoConfigurationImportSelector — „odroczony” selektor importów
    // (DeferredImportSelector), który działa dopiero PO przetworzeniu wszystkich Twoich konfiguracji. Dzięki temu
    // warunek @ConditionalOnMissingBean (sekcja 3) zna już Twoje beany.

    // =================================================================================================
    // 3. „WYCOFANIE SIĘ” — @ConditionalOnMissingBean: Twój bean wygrywa
    // =================================================================================================
    //
    // Uproszczony szkielet JacksonAutoConfiguration z Boota 4.1.1 (adnotacje odczytane z jaru):
    //
    //   @AutoConfiguration
    //   @ConditionalOnClass(JsonMapper.class)                   ← tylko gdy Jackson 3 jest na ścieżce klas
    //   public final class JacksonAutoConfiguration {
    //
    //       @Bean @Primary @ConditionalOnMissingBean            ← tylko gdy NIE MA żadnego beana typu JsonMapper
    //       JsonMapper jacksonJsonMapper(JsonMapper.Builder builder) {
    //           return builder.build();
    //       }
    //
    //       @Bean @Scope("prototype") @ConditionalOnMissingBean  ← budowniczy z ustawieniami spring.jackson.*;
    //       JsonMapper.Builder jsonMapperBuilder(…) { … }         nowy obiekt przy każdym wstrzyknięciu
    //   }
    //
    // Wynik (test sekcji 3):
    //   • bez Twojego beana: w kontekście jest JEDEN JsonMapper o nazwie "jacksonJsonMapper" (oznaczony @Primary)
    //     — i to on trafia do ProductJsonService;
    //   • z Twoim @Bean JsonMapper shopMapper(): nadal JEDEN JsonMapper — Twój. Beana "jacksonJsonMapper" nie ma,
    //     bo warunek @ConditionalOnMissingBean znalazł Twój bean i metoda Boota została pominięta.
    // Jackson 3: pakiety tools.jackson.* (np. tools.jackson.databind.json.JsonMapper), ale adnotacje (@JsonProperty…)
    // zostały w com.fasterxml.jackson.annotation. Rekord Product jest zapisywany w kolejności pól:
    //     {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99}
    //
    // PYTANIE REKRUTACYJNE: jak nadpisać bean z autokonfiguracji? Zdefiniuj własny bean TEGO typu, którego pilnuje
    //   @ConditionalOnMissingBean (typ sprawdzisz w raporcie — sekcja 5).
    //
    // PUŁAPKA: własny JsonMapper zbudowany od zera (JsonMapper.builder().build()) ignoruje wszystkie ustawienia
    //   spring.jackson.* (test sekcji 3: indent-output=true nie działa na taki mapper).
    //   Dlaczego? Ustawienia z pliku trafiają do budowniczego Boota (jsonMapperBuilder), a Twój mapper go nie użył.
    //
    // DOBRA PRAKTYKA: zanim napiszesz własny bean, spróbuj po kolei: (1) właściwość spring.jackson.*,
    //   (2) bean JsonMapperBuilderCustomizer (dostosowanie budowniczego Boota), (3) dopiero na końcu własny JsonMapper
    //   — najlepiej zbudowany ze wstrzykniętego JsonMapper.Builder. Dlaczego? Każdy krok dalej wyłącza więcej
    //   gotowych ustawień Boota, które potem trzeba utrzymywać samemu.

    // =================================================================================================
    // 4. INNE WARUNKI — klasa, ustawienie, typ aplikacji
    // =================================================================================================
    //
    //   @ConditionalOnClass(X.class)          klasa X JEST na ścieżce klas (czyli dołączono bibliotekę / starter)
    //   @ConditionalOnMissingClass("a.b.X")   klasy NIE MA
    //   @ConditionalOnBean / @ConditionalOnMissingBean   jest / nie ma beana danego typu (albo nazwy)
    //   @ConditionalOnProperty(...)           ustawienie ma określoną wartość (s02_configuration/Cfg05Environment)
    //   @ConditionalOnBooleanProperty(...)    ustawienie typu prawda/fałsz — nowość Boota 3.5
    //   @ConditionalOnWebApplication          aplikacja jest webowa (@ConditionalOnNotWebApplication — odwrotnie)
    //   @ConditionalOnResource(...)           na ścieżce klas jest plik (np. META-INF/build-info.properties)
    //
    // Prawdziwe przykłady z raportu TEGO modułu:
    //   • JmxAutoConfiguration: @ConditionalOnBooleanProperty (spring.jmx.enabled=true) — JMX (zdalne zarządzanie
    //     przez JVM) jest domyślnie wyłączony, włączasz go ustawieniem;
    //   • AopAutoConfiguration.ClassProxyingConfiguration: @ConditionalOnMissingClass — „biblioteki AspectJ nie ma”,
    //     więc Boot włącza wariant konfiguracji pośredników (proxy) działający bez niej;
    //   • ProjectInfoAutoConfiguration#buildProperties: @ConditionalOnResource — „nie ma pliku build-info”.
    //
    // Test sekcji 4 sprawdza trzy warunki:
    //   • właściwość spring.jackson.serialization.indent-output=true → mapper Boota robi wcięcia (JSON w wielu liniach);
    //   • FilteredClassLoader (ładowacz klas z testów Boota, który udaje brak klasy) ukrywa JsonMapper →
    //     @ConditionalOnClass nie pasuje → brak beana JsonMapper;
    //   • WebHomePage ma @Component i @ConditionalOnWebApplication — aplikacja nie jest webowa, więc beana nie ma.
    //     Warunki Boota działają także na TWOICH klasach, nie tylko w autokonfiguracjach.
    //
    // DOBRA PRAKTYKA: @ConditionalOnClass(X.class) stawiaj nad KLASĄ konfiguracji (w autokonfiguracji — nad osobną,
    //   zagnieżdżoną klasą @Configuration), a nie nad metodą @Bean zwracającą X. Dlaczego? Adnotację nad klasą
    //   Spring czyta z pliku .class, zanim klasa zostanie załadowana, więc brak X niczego nie psuje. Zanim zadziała
    //   warunek na METODZIE, JVM ładuje już klasę konfiguracji z jej metodami — gdy X brakuje, kończy się to błędem
    //   (NoClassDefFoundError). Tak opisuje to dokumentacja adnotacji @ConditionalOnClass.

    // =================================================================================================
    // 5. RAPORT WARUNKÓW — odpowiedź na „dlaczego?”
    // =================================================================================================
    //
    // Jak go zobaczyć:
    //   • argument programu --debug albo debug=true w application.properties → przy starcie w logu (poziom DEBUG)
    //     pojawia się blok:
    //         ============================
    //         CONDITIONS EVALUATION REPORT
    //         ============================
    //         Positive matches:      (warunki spełnione — to Boot UTWORZYŁ)
    //            JacksonAutoConfiguration matched:
    //               - @ConditionalOnClass found required class 'tools.jackson.databind.json.JsonMapper' (OnClassCondition)
    //         Negative matches:      (warunki niespełnione — tego NIE ma, z powodem)
    //            JacksonAutoConfiguration.JsonProblemDetailsConfiguration:
    //               Did not match:
    //                  - @ConditionalOnClass did not find required class 'org.springframework.http.ProblemDetail' (OnClassCondition)
    //         Exclusions:            (wyłączone ręcznie — sekcja 6)
    //         Unconditional classes: (autokonfiguracje bez warunków)
    //   • w kodzie/teście: ConditionEvaluationReport.get(beanFactory).getConditionAndOutcomesBySource() — mapa
    //     „źródło → wyniki”, gdzie źródło to "pełna.nazwa.Klasy", "pełna.nazwa.Klasy#metodaBean" albo
    //     "pełna.nazwa.Klasy$Zagnieżdżona" (test sekcji 5);
    //   • w działającej aplikacji webowej: /actuator/conditions (dział s15_actuator_logging).
    // --debug włącza DEBUG tylko dla wybranych loggerów Boota (m.in. tego od raportu) — nie dla Twojego kodu.
    // Gdy start się NIE uda, Boot przypomina w logu: „Error starting ApplicationContext. To display the condition
    // evaluation report re-run your application with 'debug' enabled.”
    //
    // PUŁAPKA: „Boot coś zrobił magicznie” albo „Boot nie zrobił, choć powinien” — i zgadywanie.
    //   Dlaczego to błąd? Raport zawsze mówi, który warunek zdecydował, z dokładną nazwą klasy, beana czy ustawienia.
    //   Zamiast zgadywać: --debug i szukaj nazwy klasy w Positive/Negative matches.
    //   Ciekawostka z testu sekcji 5: w @SpringBootTest Boot sam dopisuje spring.jmx.enabled=false, więc dla JMX
    //   raport mówi tam „found different value”, a przy zwykłym starcie — „did not find property”.

    // =================================================================================================
    // 6. WYŁĄCZANIE AUTOKONFIGURACJI — exclude
    // =================================================================================================
    //
    //   @SpringBootApplication(exclude = JacksonAutoConfiguration.class)        w kodzie (kompilator sprawdzi nazwę)
    //   @SpringBootApplication(excludeName = "org.springframework...Klasa")     po nazwie (gdy klasy może nie być)
    //   spring.autoconfigure.exclude=org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
    //                                                                           w pliku albo argumencie — bez kompilacji
    // Wyłączone klasy raport wypisuje w sekcji „Exclusions”. Test sekcji 6 pokazuje oba sposoby.
    //
    // PUŁAPKA: wyłączasz autokonfigurację, a Twój kod potrzebuje jej beana. Start się nie uda:
    //       APPLICATION FAILED TO START
    //       Description:
    //       Parameter 0 of constructor in s03_boot_basics.boot01_auto_configuration.ProductJsonService required
    //       a bean of type 'tools.jackson.databind.json.JsonMapper' that could not be found.
    //   Dlaczego? exclude usuwa WSZYSTKIE beany tej autokonfiguracji, także te, z których korzystasz.
    //
    // PUŁAPKA: nazwa klasy, której nie ma na ścieżce klas (literówka albo stara nazwa z Boota 3, np.
    //   org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration) — Boot ją PO CICHU pomija:
    //   brak błędu, brak ostrzeżenia, a Jackson działa dalej (test sekcji 6).
    //   Dlaczego? Boot pozwala wykluczać klasy, których może nie być (np. z opcjonalnych jarów), więc nieznanej nazwy
    //   nie traktuje jak błędu. Po migracji z Boota 3 przejrzyj więc wszystkie exclude!
    //
    // PUŁAPKA: nazwa klasy, która ISTNIEJE, ale nie jest autokonfiguracją (np. typ beana zamiast klasy
    //   autokonfiguracji) — błąd startu:
    //       java.lang.IllegalStateException: The following classes could not be excluded because they are not
    //       auto-configuration classes:
    //           - tools.jackson.databind.json.JsonMapper
    //   Dlaczego? Wykluczyć można tylko kandydata z listy .imports — Boot ostrzega, że pomyliłeś klasy.
    //
    // DOBRA PRAKTYKA: wyłączaj przez exclude = Klasa.class (kompilator wyłapie literówkę), a po zmianie sprawdź
    //   w raporcie sekcję „Exclusions”. Dlaczego? Tekstowa nazwa z literówką to „wyłączenie”, które nic nie robi.

    // =================================================================================================
    // 7. PUŁAPKA: WŁASNY BEAN INNEGO TYPU NIE WYŁĄCZA AUTOKONFIGURACJI
    // =================================================================================================
    //
    // Chcesz własnego mappera i piszesz @Bean ObjectMapper myObjectMapper() (ObjectMapper to klasa bazowa JsonMapper).
    // Boot pilnuje jednak typu JsonMapper (@ConditionalOnMissingBean na metodzie zwracającej JsonMapper), a Twój bean
    // jest typu ObjectMapper — warunek nie znajduje JsonMappera, więc Boot tworzy SWÓJ. Wynik (test sekcji 7):
    //   • dwa beany typu ObjectMapper: "myObjectMapper" i "jacksonJsonMapper";
    //   • tam, gdzie wstrzykujesz ObjectMapper, trafia mapper BOOTA, bo ma @Primary. Twój leży nieużywany.
    // PUŁAPKA: „moje ustawienia mappera nie działają” — bez żadnego błędu.
    //   Dlaczego? O wycofaniu decyduje TYP z warunku, nie Twoja intencja. Sprawdź typ w raporcie:
    //   „@ConditionalOnMissingBean (types: tools.jackson.databind.json.JsonMapper; …)”.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @SpringBootApplication = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
     *     (filtry: TypeExcludeFilter, AutoConfigurationExcludeFilter).
     *   • Lista kandydatów: META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports w każdym
     *     jarze. Boot 4: wiele modułów spring-boot-<technologia>; nowe pakiety (…boot.jackson.autoconfigure…).
     *   • Autokonfiguracje są przetwarzane PO Twoich konfiguracjach → @ConditionalOnMissingBean widzi Twoje beany.
     *   • Twój bean tego samego typu wygrywa (Boot się wycofuje). Bean innego typu — NIE (dwa beany, Boot ma @Primary).
     *   • Warunki: OnClass / OnMissingClass, OnBean / OnMissingBean, OnProperty, OnBooleanProperty (3.5+),
     *     OnWebApplication, OnResource. Działają też na Twoich klasach.
     *   • Raport: --debug / debug=true (Positive / Negative matches, Exclusions, Unconditional classes),
     *     w teście ConditionEvaluationReport.get(beanFactory).
     *   • Wyłączanie: exclude = Klasa.class, excludeName = "…", spring.autoconfigure.exclude=… .
     *     Nieznana nazwa — cisza; istniejąca klasa, która nie jest autokonfiguracją — IllegalStateException.
     *   • Kolejność prób: właściwość spring.jackson.* → JsonMapperBuilderCustomizer → własny JsonMapper.
     *
     * PYTANIA KONTROLNE:
     *   1. Z jakich trzech adnotacji składa się @SpringBootApplication i co robi każda z nich?
     *   2. Skąd Boot 4 bierze listę autokonfiguracji do sprawdzenia? Czym to się różni od Boota 2?
     *   3. Co się stanie? Dopisujesz @Bean JsonMapper shopMapper(). Ile beanów typu JsonMapper będzie w kontekście
     *      i czy istnieje bean "jacksonJsonMapper"?
     *   4. Co się stanie? Dopisujesz @Bean ObjectMapper myObjectMapper() i wstrzykujesz ObjectMapper do serwisu.
     *      Który obiekt dostanie serwis?
     *   5. ZNAJDŹ BŁĄD: po migracji z Boota 3 w application.properties zostało
     *      spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
     *      — a JsonMapper od Boota nadal jest w kontekście. Dlaczego i dlaczego nikt nie zgłosił błędu?
     *   6. Co się stanie? Uruchamiasz Boot01Application z argumentem
     *      --spring.autoconfigure.exclude=org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
     *   7. WebHomePage ma @Component i leży w skanowanym pakiecie. Dlaczego nie ma takiego beana? Jak to sprawdzisz?
     *   8. Spodziewałeś się beana z autokonfiguracji, a go nie ma. Jakie są Twoje pierwsze dwa kroki?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Boot01AutoConfigurationExercisesTest
    // =================================================================================================

    /**
     * Klient sklepu do ćwiczenia 1. Maria Nowak (id 2) nie podała e-maila, więc {@code email} to {@code null}.
     *
     * @param id    numer klienta
     * @param name  imię i nazwisko
     * @param email adres e-mail albo {@code null}
     */
    public record CustomerJson(long id, String name, String email) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć JEDNO ustawienie w postaci {@code "klucz=wartość"}, po którym mapper Boota pomija
     * pola z wartością {@code null}. Klient {@code new CustomerJson(2, "Maria Nowak", null)} ma się zapisać jako
     * {@code {"id":2,"name":"Maria Nowak"}} (bez pola email).
     * Podpowiedź: rodzina ustawień {@code spring.jackson.*}; szukaj „default property inclusion” (domyślne
     * dołączanie pól), wartość {@code non_null}. Pełną listę kluczy spring.jackson.* znajdziesz w pliku
     * {@code META-INF/spring-configuration-metadata.json} w jarze spring-boot-jackson.
     */
    static String exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): aplikacja ćwiczenia ma działać BEZ autokonfiguracji Jacksona.
     * (a) Dopisz wyłączenie w adnotacji nad klasą {@link Exercise2Application} (atrybut {@code exclude}).
     * (b) Uzupełnij {@link #exercise2Exclusions}: zwróć listę wykluczonych autokonfiguracji odczytaną z raportu
     * warunków uruchomionej aplikacji, posortowaną alfabetycznie.
     * Podpowiedź: {@code ConditionEvaluationReport.get(context.getBeanFactory()).getExclusions()}, potem
     * {@code stream().sorted().toList()}.
     */
    @EnableAutoConfiguration   // TODO: wyłącz tu autokonfigurację Jacksona
    static class Exercise2Application {
    }

    /** Część (b) ćwiczenia 2. */
    static List<String> exercise2Exclusions(ConfigurableApplicationContext context) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /*
     * ĆWICZENIE 3 (trudniejsze): tym razem piszesz TEST. W Boot01AutoConfigurationExercisesTest uzupełnij metodę
     * exercise3() tak, żeby udowodniła wycofanie się autokonfiguracji na DWA sposoby:
     *   (a) ApplicationContextRunner z AutoConfigurations.of(JacksonAutoConfiguration.class) i Twoją konfiguracją
     *       z beanem JsonMapper o nazwie "shopMapper": w kontekście jest dokładnie jeden JsonMapper — Twój;
     *   (b) w raporcie warunków TEGO kontekstu źródło
     *       "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration#jacksonJsonMapper"
     *       NIE pasuje, a komunikat zawiera "found beans of type 'tools.jackson.databind.json.JsonMapper' shopMapper".
     * Podpowiedź: wzoruj się na teście sekcji 3 i 5 (Boot01AutoConfigurationTest); raport weźmiesz przez
     * ConditionEvaluationReport.get(ctx.getBeanFactory()) — działa także w kontekście z ApplicationContextRunner.
     */

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @SpringBootConfiguration (główna konfiguracja — szuka jej @SpringBootTest), @EnableAutoConfiguration
     *      (włącza autokonfigurację), @ComponentScan (skanuje pakiet klasy i podpakiety; z filtrami
     *      TypeExcludeFilter i AutoConfigurationExcludeFilter). Sprawdzone testem (sekcja 1).
     *   2. Z plików META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports we wszystkich
     *      jarach na ścieżce klas — w Boot 4 jest ich wiele, bo każdy moduł technologii ma swój. W Bootcie 2 lista
     *      była w META-INF/spring.factories (klucz …EnableAutoConfiguration); od Boota 3.0 tam już nie działa.
     *   3. Jeden JsonMapper — Twój "shopMapper". Beana "jacksonJsonMapper" nie ma: @ConditionalOnMissingBean znalazł
     *      Twój bean i Boot pominął swoją metodę. Sprawdzone testem (sekcja 3).
     *   4. Mapper BOOTA ("jacksonJsonMapper"). Twój bean jest typu ObjectMapper, a warunek pilnuje JsonMapper, więc Boot
     *      utworzył swój mapper; są dwa ObjectMappery, a wygrywa ten z @Primary. Sprawdzone testem (sekcja 7).
     *   5. To nazwa z Boota 3 — w Boot 4 klasa jest w pakiecie org.springframework.boot.jackson.autoconfigure.
     *      Klasy o starej nazwie nie ma na ścieżce klas, a nieznane nazwy w exclude Boot po cichu pomija.
     *      Sprawdzone testem (sekcja 6). Napraw: nowa nazwa albo exclude = JacksonAutoConfiguration.class.
     *   6. Start się nie uda: ProductJsonService potrzebuje JsonMappera, a jedyne źródło tego beana zostało wyłączone.
     *      Wyjątek UnsatisfiedDependencyException („No qualifying bean of type 'tools.jackson.databind.json.JsonMapper'
     *      available”), a w logu „APPLICATION FAILED TO START” z opisem: „Parameter 0 of constructor in
     *      …ProductJsonService required a bean of type 'tools.jackson.databind.json.JsonMapper' that could not be
     *      found.” Sprawdzone testem (sekcja 6).
     *   7. Ma też @ConditionalOnWebApplication, a aplikacja nie jest webowa (brak klas serwletów i WebFluxa).
     *      Raport warunków: „@ConditionalOnWebApplication (required) did not find servlet web application classes
     *      and did not find reactive web application classes”. Sprawdzone testem (sekcje 4 i 5).
     *   8. (1) Uruchom z --debug i znajdź klasę autokonfiguracji w „Negative matches” — przeczytaj powód.
     *      (2) Jeśli jej nie ma ani w Positive, ani w Negative — nie ma jej na liście kandydatów: brakuje startera
     *      (modułu spring-boot-<technologia>) albo jest w „Exclusions”.
     */
    // </editor-fold>
}

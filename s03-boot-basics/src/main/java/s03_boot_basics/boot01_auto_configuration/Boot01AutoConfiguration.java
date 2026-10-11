package s03_boot_basics.boot01_auto_configuration;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

/**
 * <pre>
 * TEMAT: Autokonfiguracja — jak Spring Boot sam dokłada beany i kiedy się wycofuje
 *        (auto-configuration = autokonfiguracja; condition = warunek; back-off = wycofanie się;
 *         condition evaluation report = raport oceny warunków; exclude = wyklucz)
 *
 * W SKRÓCIE:
 *   Boot01Application nie definiuje ŻADNEGO beana, a jej kontekst ma ich około 50. Skąd? Z autokonfiguracji.
 *   Adnotacja {@code @SpringBootApplication} zawiera {@code @EnableAutoConfiguration} (włącz autokonfigurację).
 *   Ta czyta z plików na classpath listę KANDYDATÓW — zwykłych klas konfiguracyjnych z metodami {@code @Bean}.
 *   Każdy kandydat ma WARUNKI: „jest taka klasa na classpath?”, „brakuje takiego beana?”, „ustawiono taką
 *   właściwość?”. Kandydat, którego warunki są spełnione, dokłada swoje beany; reszta jest pomijana.
 *   Najważniejsza zasada: Twój bean wygrywa — autokonfiguracja wycofuje się, gdy zdefiniujesz własny.
 *
 * ANALOGIA: hotel „all inclusive”.
 *   Recepcja (autokonfiguracja) ma listę usług (kandydaci). Każda ma regułę: „ręczniki — tylko jeśli gość nie
 *   przywiózł swoich” (brak beana), „basen — tylko jeśli hotel ma basen” (klasa na classpath), „śniadanie do
 *   pokoju — tylko jeśli zaznaczysz w formularzu” (właściwość). Przywozisz własne ręczniki? Hotel nie kładzie swoich.
 *
 * JAK TO DZIAŁA:
 *   {@code @SpringBootApplication} = {@code @SpringBootConfiguration} + {@code @EnableAutoConfiguration} + {@code @ComponentScan}
 *     1. najpierw TWOJE beany: skanowanie pakietu klasy startowej (i klasy, które wskażesz)
 *     2. potem autokonfiguracja (celowo PÓŹNIEJ — dzięki temu warunek „brak beana” widzi Twoje beany):
 *        a) kandydaci z META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 *           w każdym jarze na classpath (w Boot 4.1.1 spring-boot-autoconfigure ma ich 12; jary technologii,
 *           np. spring-boot-jackson, spring-boot-webmvc, mają własne pliki .imports)
 *        b) usunięcie wykluczonych (exclude w adnotacji, właściwość spring.autoconfigure.exclude)
 *        c) ocena warunków {@code @ConditionalOn...} każdego kandydata, a potem każdej jego metody {@code @Bean}
 *        d) zapis wyników do raportu ConditionEvaluationReport (pokazuje go flaga --debug)
 *
 * SŁÓWKA:
 *   enable = włącz; candidate = kandydat; imports = importy (tu: plik z listą klas); conditional on class = pod
 *   warunkiem, że jest klasa; on missing bean = gdy brakuje beana; on property = gdy jest właściwość; having value =
 *   mająca wartość; match if missing = pasuje, gdy brak (właściwości); positive/negative match = warunek spełniony/
 *   niespełniony; executor = wykonawca zadań; runner = uruchamiacz (tu: ApplicationContextRunner, mały kontekst do testów).
 *
 * ZOBACZ TEŻ: s00_start/Start02MultiModule (pierwsze spotkanie ze starterami i autokonfiguracją),
 *             s01_core_ioc/Ioc04BeanMethods (metody {@code @Bean} — z nich składa się każda autokonfiguracja),
 *             s02_configuration/Cfg01Properties (właściwości, które sterują warunkami),
 *             s03_boot_basics/Boot02WritingAutoConfiguration (własna autokonfiguracja),
 *             t19_annotations_reflection/Annotations05MiniFramework (mini-framework czytający adnotacje — ta sama idea).
 * </pre>
 */
public final class Boot01AutoConfiguration {

    private Boot01AutoConfiguration() {
    }

    // =================================================================================================
    // 1. SKĄD SĄ BEANY, KTÓRYCH NIE NAPISAŁEŚ — {@code @EnableAutoConfiguration} i plik .imports
    // =================================================================================================
    //
    // Boot01Application jest pusta, a w jej kontekście jest m.in. bean applicationTaskExecutor (wykonawca zadań
    // asynchronicznych, typ ThreadPoolTaskExecutor — pula wątków). Utworzyła go klasa
    // org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration — kandydat z pliku
    //   META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    // w jarze spring-boot-autoconfigure-4.1.1.jar. Ten plik to zwykła lista pełnych nazw klas, jedna w linii.
    // Sprawdzisz go sam: w IntelliJ rozwiń External Libraries → spring-boot-autoconfigure → META-INF → spring.
    //
    // Pod spodem: {@code @EnableAutoConfiguration} importuje AutoConfigurationImportSelector (selektor importów), a ten
    // woła ImportCandidates.load(AutoConfiguration.class, classLoader) — wczytuje pliki .imports ze WSZYSTKICH jarów.
    // Test sekcji 1 robi to samo i sprawdza, że TaskExecutionAutoConfiguration jest na liście.
    //
    // PUŁAPKA: stare tutoriale (Boot 2.x) rejestrują autokonfigurację w META-INF/spring.factories pod kluczem
    //   EnableAutoConfiguration. Od Boot 3.0 Spring Boot tego klucza NIE czyta. Dlaczego to ważne? Autokonfiguracja
    //   zapisana „po staremu” po prostu się nie włączy — bez żadnego błędu.
    //
    // NOWOŚĆ W BOOT 4: autokonfiguracje zostały rozdzielone na moduły technologii. Np. JacksonAutoConfiguration nie
    //   leży już w spring-boot-autoconfigure, tylko w jarze spring-boot-jackson (pakiet
    //   org.springframework.boot.jackson.autoconfigure). Dlatego w naszym module — bez Jacksona — tej klasy wcale nie ma.

    // =================================================================================================
    // 2. WARUNEK „JEST KLASA” — {@code @ConditionalOnClass}
    // =================================================================================================
    //
    //   {@code @ConditionalOnClass(ThreadPoolTaskExecutor.class)}     ← nagłówek TaskExecutionAutoConfiguration
    //   {@code @AutoConfiguration}
    //   public final class TaskExecutionAutoConfiguration { ... }
    //
    // ThreadPoolTaskExecutor pochodzi ze spring-context, który mamy — warunek spełniony. A np.
    // AopAutoConfiguration.AspectJAutoProxyingConfiguration wymaga klasy org.aspectj.weaver.Advice z biblioteki
    // AspectJ, której tu NIE ma — więc ta część konfiguracji jest pominięta. Tak Boot „reaguje na classpath”:
    // dodajesz starter → pojawiają się klasy → spełniają się warunki → pojawiają się beany.
    //
    // Test sekcji 2 ukrywa klasę ThreadPoolTaskExecutor (FilteredClassLoader — filtrujący ładowacz klas, narzędzie
    // testowe Boota) i pokazuje, że wtedy applicationTaskExecutor nie powstaje.
    //
    // Ciekawostka: warunek czyta nazwę klasy z bajtkodu adnotacji, a nie ładuje jej. Dlatego brak klasy nie kończy się
    //   błędem ClassNotFoundException (nie znaleziono klasy), tylko spokojnym „warunek niespełniony”.

    // =================================================================================================
    // 3. WARUNEK „BRAK BEANA” — {@code @ConditionalOnMissingBean}, czyli Twój bean wygrywa
    // =================================================================================================
    //
    // Najważniejszy warunek autokonfiguracji. Przykład z Boota (uproszczony):
    //   {@code @Bean}
    //   {@code @ConditionalOnMissingBean}                 ← „utwórz tylko, gdy nikt inny nie dał takiego beana”
    //   ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder(...) { ... }
    // a applicationTaskExecutor powstaje tylko, gdy w kontekście NIE ma żadnego beana typu java.util.concurrent.Executor.
    //
    // Boot01OwnExecutorConfig definiuje własny Executor (shopExecutor). Test sekcji 3: z tą konfiguracją
    // applicationTaskExecutor znika — autokonfiguracja się wycofała (back-off).
    //
    // PRZED: chcę inny executor → szukam, jak „wyłączyć” Boota, kopiuję jego kod.
    // PO: definiuję swój bean, a Boot sam ustępuje miejsca.
    //
    // DOBRA PRAKTYKA: zanim wyłączysz całą autokonfigurację, zdefiniuj własny bean albo zmień właściwość
    //   (spring.task.execution.pool.max-size itd.). Dlaczego? Wyłączenie zabiera też inne, potrzebne beany tej
    //   autokonfiguracji; własny bean podmienia dokładnie jeden element.
    //
    // PUŁAPKA: warunek patrzy na TYP. Zdefiniujesz dowolny Executor (nawet do zupełnie innego celu), a Boot uzna, że
    //   „executor już jest”, i nie utworzy applicationTaskExecutor. Dlaczego to groźne? Coś, co dotąd działało
    //   (np. metody {@code @Async} — asynchroniczne), nagle użyje Twojego executora albo innego zastępczego.

    // =================================================================================================
    // 4. WARUNEK „JEST WŁAŚCIWOŚĆ” — {@code @ConditionalOnProperty} i {@code @ConditionalOnBooleanProperty}
    // =================================================================================================
    //
    //   {@code @ConditionalOnProperty(name = "shop.cache.enabled", havingValue = "true", matchIfMissing = false)}
    //     name           — nazwa właściwości
    //     havingValue    — wymagana wartość (bez niej: dowolna wartość różna od „false”)
    //     matchIfMissing — co, gdy właściwości w ogóle nie ma (domyślnie false = warunek niespełniony)
    //
    // Od Boot 3.5 jest też wygodniejszy {@code @ConditionalOnBooleanProperty} — dla przełączników true/false. Używa go
    // JmxAutoConfiguration: {@code @ConditionalOnBooleanProperty("spring.jmx.enabled")}. Domyślnie właściwości nie ma,
    // więc JMX (Java Management Extensions — zdalne zarządzanie aplikacją) jest WYŁĄCZONY. Test sekcji 4: bez
    // właściwości nie ma beana mbeanExporter, a z spring.jmx.enabled=true — jest.
    //
    // DOBRA PRAKTYKA: właściwość, która coś WŁĄCZA, nazywaj ...enabled i ustaw jasno matchIfMissing. Dlaczego?
    //   Czytelnik konfiguracji od razu wie, jaki jest stan domyślny, bez zaglądania do kodu.

    // =================================================================================================
    // 5. RAPORT WARUNKÓW — dlaczego bean powstał (albo nie)
    // =================================================================================================
    //
    // Każda ocena warunku trafia do ConditionEvaluationReport. Jak go zobaczyć?
    //   • argument --debug przy uruchomieniu (albo debug=true w application.properties) → w logu sekcja
    //     „CONDITIONS EVALUATION REPORT” z listami Positive matches (spełnione) i Negative matches (niespełnione)
    //     oraz Exclusions (wykluczone) i Unconditional classes (klasy bez warunków)
    //   • w kodzie/teście: ConditionEvaluationReport.get(context.getBeanFactory())
    //       .getConditionAndOutcomesBySource()   → mapa: źródło → wyniki warunków
    //   Kluczem jest nazwa klasy (TaskExecutionAutoConfiguration), klasy zagnieżdżonej (z $) albo metody (z #),
    //   np. ...TaskExecutorConfigurations$TaskExecutorConfiguration#applicationTaskExecutor.
    //
    // W naszym module (sprawdzone testem sekcji 5): TaskExecutionAutoConfiguration — spełniona,
    // JmxAutoConfiguration i MessageSourceAutoConfiguration — niespełnione (brak spring.jmx.enabled; brak pliku
    // messages.properties).
    //
    // DOBRA PRAKTYKA: „bean nie powstał” → najpierw --debug i szukaj nazwy w Negative matches. Dlaczego? Raport mówi
    //   wprost, który warunek zawiódł („did not find property 'enabled'”, „did not find required class ...”), zamiast
    //   zgadywania.

    // =================================================================================================
    // 6. WYKLUCZANIE AUTOKONFIGURACJI — exclude
    // =================================================================================================
    //
    // Gdy autokonfiguracja w ogóle nie jest Ci potrzebna albo przeszkadza:
    //   {@code @SpringBootApplication(exclude = TaskExecutionAutoConfiguration.class)}            — w kodzie
    //   {@code @SpringBootApplication(excludeName = "org...task.TaskExecutionAutoConfiguration")} — po nazwie (klasy może nie być)
    //   spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
    //                                                                                             — we właściwościach
    // Właściwość jest wygodna, bo można jej użyć tylko w jednym profilu albo w jednym teście. Test sekcji 6 używa właśnie
    // jej (exclude w adnotacji pokazujemy tylko tutaj — druga klasa {@code @SpringBootApplication} w tym pakiecie
    // zostałaby zeskanowana przez Boot01Application).
    //
    // PUŁAPKA: literówka we właściwości spring.autoconfigure.exclude jest CICHO ignorowana. Dlaczego? Boot sprawdza
    //   tylko klasy, które ISTNIEJĄ — nieistniejącej nazwy nie ma jak zweryfikować. Wynik: autokonfiguracja dalej działa,
    //   a Ty myślisz, że ją wyłączyłeś (sprawdzone testem sekcji 6).
    // PUŁAPKA: wykluczenie klasy, która istnieje, ale NIE jest autokonfiguracją (np. Twojej {@code @Configuration}),
    //   kończy się błędem startu IllegalStateException: „The following classes could not be excluded because they are
    //   not auto-configuration classes”. Dlaczego? exclude działa tylko na listę kandydatów z plików .imports; swoją
    //   konfigurację po prostu usuń albo obejmij warunkiem/profilem.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code @SpringBootApplication} zawiera {@code @EnableAutoConfiguration}; kandydaci są w plikach
     *     META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports w jarach.
     *   • Autokonfiguracja działa PO Twoich beanach, więc warunki „brak beana” widzą Twoje definicje.
     *   • {@code @ConditionalOnClass} — jest klasa na classpath; {@code @ConditionalOnMissingBean} — nie ma beana (Twój wygrywa);
     *     {@code @ConditionalOnProperty} / {@code @ConditionalOnBooleanProperty} — jest właściwość o wartości.
     *   • Raport: --debug albo ConditionEvaluationReport.get(beanFactory); Positive/Negative matches.
     *   • Wykluczanie: exclude / excludeName w adnotacji, spring.autoconfigure.exclude we właściwościach.
     *   • Literówka w exclude = cisza; wykluczenie nie-autokonfiguracji = IllegalStateException.
     *   • Boot 4: autokonfiguracje rozdzielone na moduły technologii (spring-boot-jackson, spring-boot-webmvc...).
     *
     * PYTANIA KONTROLNE:
     *   1. PYTANIE REKRUTACYJNE: jak działa autokonfiguracja Spring Boota? Skąd Boot wie, jakie klasy rozważyć?
     *   2. Co się stanie? Do kontekstu Boot01Application dodasz własny bean typu Executor (jak w Boot01OwnExecutorConfig).
     *      Czy w kontekście będzie bean applicationTaskExecutor?
     *   3. Co się stanie? Uruchomisz aplikację z właściwością spring.jmx.enabled=true. Czy pojawi się bean mbeanExporter?
     *   4. ZNAJDŹ BŁĄD: {@code spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfigration}
     *      — aplikacja startuje bez błędu, ale applicationTaskExecutor nadal jest. Dlaczego?
     *   5. Co się stanie? We właściwości spring.autoconfigure.exclude wpiszesz nazwę klasy java.lang.String.
     *   6. Bean, którego się spodziewasz, nie powstał. Jak sprawdzić dlaczego, bez czytania kodu Boota?
     *   7. Dlaczego autokonfiguracje używają {@code @ConditionalOnMissingBean} i dlaczego są przetwarzane po Twoich beanach?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Boot01AutoConfigurationExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć posortowane nazwy beanów typu java.util.concurrent.Executor z podanego kontekstu.
     * W kontekście Boot01Application powinna to być jedna nazwa — ta od autokonfiguracji.
     * Podpowiedź: context.getBeanNamesForType(Executor.class), Arrays.stream(...).sorted().toList().
     */
    static List<String> exercise1(ApplicationContext context) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): z raportu warunków zwróć PROSTE nazwy (bez pakietu) klas autokonfiguracji najwyższego
     * poziomu, których wszystkie warunki są spełnione — posortowane alfabetycznie.
     * „Najwyższego poziomu” = klucz bez znaków '$' (klasa zagnieżdżona) i '#' (metoda).
     * Podpowiedź: report.getConditionAndOutcomesBySource().entrySet(), entry.getValue().isFullMatch(),
     * nazwa.substring(nazwa.lastIndexOf('.') + 1).
     */
    static List<String> exercise2(ConditionEvaluationReport report) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): uruchom i zwróć kontekst aplikacji Boot01Application, w którym:
     * (a) nie ma serwera WWW (WebApplicationType.NONE — typ aplikacji: bez weba),
     * (b) JMX jest włączony właściwością,
     * (c) obok Boot01Application zarejestrowana jest konfiguracja użytkownika Boot01OwnExecutorConfig.
     * Test sprawdzi, że jest bean mbeanExporter, jest shopExecutor i NIE ma applicationTaskExecutor (a potem zamknie kontekst).
     * Podpowiedź: new SpringApplicationBuilder(Boot01Application.class, Boot01OwnExecutorConfig.class)
     * .web(...).properties("...=true").run() — builder (budowniczy) poznasz dokładniej w Boot03SpringApplication.
     */
    static ConfigurableApplicationContext exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. {@code @SpringBootApplication} zawiera {@code @EnableAutoConfiguration}. Ta wczytuje listę kandydatów z plików
     *      META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports we wszystkich jarach na
     *      classpath, odrzuca wykluczone i ocenia warunki {@code @ConditionalOn...} każdego kandydata (klasa na
     *      classpath, brak beana, właściwość). Spełnione kandydaty dokładają swoje beany. Dzieje się to PO
     *      zarejestrowaniu Twoich beanów, więc Twoje definicje mają pierwszeństwo.
     *   2. Nie. TaskExecutorConfiguration ma warunek „brak beana typu Executor”, więc się wycofuje — w kontekście jest
     *      tylko shopExecutor. Sprawdzone testem w Boot01AutoConfigurationTest (sekcja 3).
     *   3. Tak — warunek {@code @ConditionalOnBooleanProperty("spring.jmx.enabled")} jest spełniony i JmxAutoConfiguration
     *      tworzy mbeanExporter (oraz mbeanServer). Bez właściwości beana nie ma. Sprawdzone testem (sekcja 4).
     *   4. Literówka: „Configration” zamiast „Configuration”. Klasa o takiej nazwie nie istnieje, a Boot weryfikuje tylko
     *      istniejące klasy — nieistniejąca nazwa jest cicho pomijana. Sprawdzone testem (sekcja 6).
     *   5. Aplikacja się nie uruchomi: IllegalStateException „The following classes could not be excluded because they
     *      are not auto-configuration classes: java.lang.String”. Sprawdzone testem (sekcja 6).
     *   6. Uruchomić z --debug (albo debug=true) i znaleźć klasę w „Negative matches” raportu warunków — przy niej jest
     *      opis niespełnionego warunku. W teście: ConditionEvaluationReport.get(context.getBeanFactory()).
     *   7. Żeby użytkownik mógł podmienić dowolny element, po prostu definiując własny bean (Twój bean wygrywa).
     *      Działa to tylko dlatego, że autokonfiguracja jest przetwarzana na końcu — w chwili oceny warunku Twoje
     *      definicje beanów są już znane.
     */
    // </editor-fold>
}

package s03_boot_basics.boot01_auto_configuration;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;

import javax.sql.DataSource;
import java.util.List;

/**
 * <pre>
 * TEMAT: Autokonfiguracja Spring Boota — skąd biorą się beany, których nikt nie napisał
 *        (auto-configuration = autokonfiguracja, czyli konfiguracja dopisywana automatycznie przez Boota;
 *         condition = warunek; back-off = ustępowanie, wycofanie się; exclude = wyłącz, wyklucz)
 *
 * W SKRÓCIE:
 *   Boot ma w swoich jarach setki gotowych klas konfiguracji (z metodami @Bean). Przy starcie przegląda je
 *   i włącza tylko te, których WARUNKI są spełnione: „czy na classpath jest klasa X?”, „czy użytkownik nie
 *   zdefiniował już własnego beana?”, „czy ustawienie Y ma wartość Z?”. Dlatego wystarczy dodać do pom.xml
 *   starter JDBC i bazę H2, a w kontekście pojawiają się DataSource, JdbcTemplate i JdbcClient.
 *   Twoje beany ZAWSZE mają pierwszeństwo — autokonfiguracja ustępuje (back-off), gdy widzi Twój bean.
 *   Każdą decyzję Boota można podejrzeć (raport warunków) i każdą autokonfigurację można wyłączyć (exclude).
 *
 * ANALOGIA: hotel z „pakietem startowym” w pokoju.
 *   Recepcja (Boot) przygotowuje pokój według prostych reguł: „gość ma dziecko → wstaw łóżeczko”, „gość przywiózł
 *   własną poduszkę → nie kładź hotelowej”, „gość zaznaczył w formularzu: bez śniadania → nie dostarczaj”.
 *   Gość niczego nie musi zamawiać, ale może: wtedy jego wybór wygrywa. A kierownik hotelu ma zeszyt (raport),
 *   w którym zapisano przy każdej regule, czy zadziałała i dlaczego.
 *
 * JAK TO DZIAŁA:
 *   1. @SpringBootApplication zawiera @EnableAutoConfiguration (enable = włącz).
 *   2. @EnableAutoConfiguration czyta ze WSZYSTKICH jarów na classpath pliki
 *      META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports — listy klas autokonfiguracji.
 *      (Jar spring-boot-jdbc 4.1.1 wymienia w nim np. DataSourceAutoConfiguration i JdbcTemplateAutoConfiguration.)
 *   3. Boot układa je w kolejności (after/before) i ładuje PO Twoich beanach z @ComponentScan i Twoich @Bean.
 *   4. Dla każdej klasy (i każdej metody @Bean w niej) sprawdza warunki @ConditionalOn…; spełnione → bean powstaje.
 *   5. Wynik każdego warunku trafia do ConditionEvaluationReport (raportu oceny warunków).
 *
 * SŁÓWKA:
 *   auto-configuration = autokonfiguracja; conditional on class = warunkowo od obecności klasy;
 *   conditional on missing bean = warunkowo od BRAKU beana; conditional on bean = warunkowo od obecności beana;
 *   conditional on property = warunkowo od ustawienia; single candidate = jedyny kandydat; positive/negative match =
 *   dopasowanie pozytywne/negatywne (warunek spełniony/niespełniony); exclusion = wyłączenie; classpath = ścieżka klas;
 *   data source = źródło danych (połączeń z bazą); connection pool = pula połączeń; driver = sterownik.
 *
 * ZOBACZ TEŻ: s01_core_ioc/Ioc04BeanMethods (metody @Bean — autokonfiguracja to zwykłe klasy z @Bean),
 *             s02_configuration/Cfg05Environment (@ConditionalOnProperty od strony użytkownika),
 *             s03_boot_basics/Boot02WritingAutoConfiguration (piszemy własną autokonfigurację),
 *             t19_annotations_reflection/Annotations05MiniFramework (jak framework czyta adnotacje).
 * </pre>
 */
public final class Boot01AutoConfiguration {

    private Boot01AutoConfiguration() {
    }

    // =================================================================================================
    // 1. CZYM JEST AUTOKONFIGURACJA — @SpringBootApplication rozłożone na części
    // =================================================================================================
    //
    //   @SpringBootApplication  =  @SpringBootConfiguration   → „to jest klasa konfiguracji aplikacji” (@Configuration)
    //                            + @EnableAutoConfiguration   → „dołóż autokonfiguracje z jarów”
    //                            + @ComponentScan             → „przeszukaj MÓJ pakiet i podpakiety (@Component…)”
    //
    // Boot01Application jest pusta, a test sekcji 1 (@SpringBootTest) znajduje w kontekście:
    //   • DataSource  — pula połączeń HikariCP (HikariDataSource) do bazy H2 w pamięci,
    //                   utworzona przez DataSourceAutoConfiguration;
    //   • JdbcTemplate — „szablon JDBC” (JdbcTemplateAutoConfiguration);
    //   • JdbcClient  — nowsze, płynne API do zapytań (JdbcClientAutoConfiguration).
    // Zapytania SELECT 1 i SELECT H2VERSION() działają, choć nikt nie podał adresu bazy.
    // Skąd adres? Gdy nie ustawisz spring.datasource.url, a na classpath jest baza wbudowana (H2, HSQLDB, Derby),
    // Boot tworzy bazę w pamięci o wygenerowanej, unikalnej nazwie (jdbc:h2:mem:…losowy-identyfikator…).
    //
    // Skąd Boot wie, które klasy przejrzeć? Z pliku …AutoConfiguration.imports w każdym jarze (patrz JAK TO DZIAŁA).
    //
    // UWAGA — BOOT 4 vs STARE TUTORIALE: w Boot 4 autokonfiguracje mieszkają w modułach technologii.
    //   Boot 4:      org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration   (jar spring-boot-jdbc)
    //   Boot 2 i 3:  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration   (jeden wielki jar
    //                spring-boot-autoconfigure)
    // W Internecie zobaczysz głównie starą nazwę — w Boot 4 się nie skompiluje. Jar spring-boot-autoconfigure nadal
    // istnieje, ale zawiera już tylko mechanizm (adnotacje @ConditionalOn…, @AutoConfiguration) i ogólne rzeczy.
    //
    // PYTANIE REKRUTACYJNE: z jakich adnotacji składa się @SpringBootApplication i co robi każda z nich?

    // =================================================================================================
    // 2. WARUNKI — @ConditionalOnClass, @ConditionalOnBean, @ConditionalOnProperty
    // =================================================================================================
    //
    // Najważniejsze warunki (pakiet org.springframework.boot.autoconfigure.condition — nie zmienił się w Boot 4):
    //   @ConditionalOnClass(X.class)        → klasa X jest na classpath     („jest starter/biblioteka?”)
    //   @ConditionalOnMissingBean           → NIE MA jeszcze beana tego typu („użytkownik nie dał własnego?”) — sekcja 3
    //   @ConditionalOnBean / @ConditionalOnSingleCandidate(DataSource.class)
    //                                       → JEST bean (dokładnie jeden kandydat) („mam z czym pracować?”)
    //   @ConditionalOnProperty(name = …, havingValue = …, matchIfMissing = …)
    //                                       → ustawienie ma daną wartość (jak w Cfg05Environment)
    //
    // Prawdziwe warunki z Boot 4.1.1 (odczytane z jarów):
    //   DataSourceAutoConfiguration      @ConditionalOnClass({DataSource, EmbeddedDatabaseType})
    //     └─ konfiguracja puli           @ConditionalOnMissingBean(DataSource) + „jest jakaś pula na classpath
    //                                     (np. HikariCP) albo ustawione spring.datasource.type”
    //         └─ wariant Hikari          @ConditionalOnProperty(name = "spring.datasource.type",
    //                                     havingValue = "com.zaxxer.hikari.HikariDataSource", matchIfMissing = true)
    //     └─ konfiguracja bazy wbudowanej → gdy NIE MA puli, ale jest H2/HSQLDB/Derby
    //   JdbcTemplateAutoConfiguration    @ConditionalOnClass({DataSource, JdbcTemplate})
    //                                    + @ConditionalOnSingleCandidate(DataSource) + after = DataSourceAutoConfiguration
    //
    // NARZĘDZIE TESTÓW DZIAŁU — ApplicationContextRunner (uruchamiacz kontekstu,
    //   pakiet org.springframework.boot.test.context.runner, jar spring-boot-test):
    //     new ApplicationContextRunner()
    //         .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, …)) // które autokonfiguracje
    //         .withUserConfiguration(MojaKonfiguracja.class)    // Twoje klasy @Configuration (ładowane PRZED autokonf.)
    //         .withBean(Typ.class, () -> obiekt)                 // pojedynczy Twój bean
    //         .withPropertyValues("klucz=wartość")              // ustawienia
    //         .withClassLoader(new FilteredClassLoader("org.h2")) // udaj, że pakietu/klasy NIE MA na classpath
    //         .run(context -> assertThat(context).hasSingleBean(DataSource.class));
    //   Każde run(...) buduje NOWY, mały kontekst i zamyka go po teście. Runner jest niezmienny (immutable):
    //   withXxx() zwraca NOWY runner, więc wspólną bazę możesz rozbudowywać w każdym teście inaczej.
    //   AutoConfigurations.of(...) sam ustawia kolejność według after/before — kolejność argumentów nie ma znaczenia.
    //   Jeśli kontekst się nie uruchomił, run nie rzuca wyjątku: sprawdzasz assertThat(context).hasFailed().
    //
    // Test sekcji 2 sprawdza (uruchomieniem):
    //   a) wszystko na classpath           → DataSource = HikariDataSource, jest JdbcTemplate;
    //   b) ukryte H2 i HikariCP             → brak DataSource i brak JdbcTemplate (kontekst startuje bez błędu);
    //   c) ukryta TYLKO H2 (Hikari zostaje) → kontekst się NIE uruchamia: „Failed to determine a suitable driver class”;
    //   d) samo JdbcTemplateAutoConfiguration, bez DataSource → brak JdbcTemplate (warunek „jest DataSource”);
    //   e) spring.datasource.type=…DriverManagerDataSource → @ConditionalOnProperty wyłącza wariant Hikari.
    //
    // PUŁAPKA: (przypadek c) „dodałem starter JDBC i aplikacja nie startuje”. Dlaczego? Starter przynosi pulę HikariCP,
    //   więc Boot chce zbudować pulę — ale bez bazy wbudowanej na classpath i bez spring.datasource.url nie wie, z czym
    //   się łączyć. Rozwiązanie: podaj spring.datasource.url (prawdziwa baza) albo dodaj H2 (baza w pamięci).
    //
    // DOBRA PRAKTYKA: zanim zaczniesz „walczyć” z Bootem, sprawdź, jaki warunek stoi za danym beanem (sekcja 4).
    //   Dlaczego? Zachowanie Boota nie jest magią — to zwykłe if-y zapisane adnotacjami; raport mówi, który if zadziałał.

    // =================================================================================================
    // 3. BACK-OFF (USTĘPOWANIE) — Twój bean wygrywa
    // =================================================================================================
    //
    //   @Configuration
    //   class ShopDataSourceConfiguration {                       // konfiguracja UŻYTKOWNIKA
    //       @Bean DataSource shopDataSource() { return new DriverManagerDataSource("jdbc:h2:mem:boot01-shop", …); }
    //   }
    //
    // Boot przetwarza Twoje beany PRZED autokonfiguracjami. Gdy dochodzi do puli z DataSourceAutoConfiguration,
    // warunek @ConditionalOnMissingBean(DataSource) widzi shopDataSource → NIE spełniony → Boot NIE tworzy swojego.
    // Test sekcji 3: w kontekście jest DOKŁADNIE jeden DataSource, nazywa się shopDataSource (nie ma beana „dataSource”
    // z Boota), łączy się z jdbc:h2:mem:boot01-shop, a JdbcTemplate z autokonfiguracji używa właśnie jego.
    //   Zwróć uwagę: Boot ustąpił TYLKO z DataSource. JdbcTemplate nadal dostałeś za darmo — autokonfiguracje są
    //   drobne i niezależne, więc podmieniasz dokładnie jeden element, resztę Boot dokłada dalej.
    //
    // PUŁAPKA: „mój bean nie działa, bo Boot ma swój i go nadpisuje”. Jest ODWROTNIE: to Boot ustępuje Tobie.
    //   Dlaczego tak to zaprojektowano? Autokonfiguracja ma być rozsądnym domyślnym, a nie przymusem. Jeśli Twój bean
    //   „nie działa”, sprawdź raczej: czy jest w skanowanym pakiecie? czy nie masz DWÓCH beanów tego typu
    //   (wtedy JdbcTemplateAutoConfiguration z @ConditionalOnSingleCandidate po cichu się wycofa)?
    //
    // DOBRA PRAKTYKA: podmieniaj bean tylko wtedy, gdy nie wystarczą ustawienia (spring.datasource.url,
    //   spring.datasource.hikari.maximum-pool-size…). Dlaczego? Własny bean wyłącza też wszystkie ustawienia, które
    //   Boot by do niego zastosował — od tej chwili całą konfigurację tego obiektu utrzymujesz sam.
    //
    // PYTANIE REKRUTACYJNE: co się dzieje z autokonfiguracją DataSource, gdy zdefiniujesz własny bean DataSource?

    // =================================================================================================
    // 4. RAPORT WARUNKÓW — CONDITIONS EVALUATION REPORT
    // =================================================================================================
    //
    // Jak włączyć w logach (dowolny sposób):
    //   • argument programu:  --debug            (IntelliJ: Run → Edit Configurations → Program arguments)
    //   • ustawienie:         debug=true          (w application.properties — tylko na chwilę!)
    //   • ./mvnw -pl s03-boot-basics spring-boot:run -Dspring-boot.run.main-class=…Boot01Application
    //                                               -Dspring-boot.run.arguments=--debug
    // Gdy aplikacja NIE wystartuje, Boot sam podpowiada w logu: „To display the condition evaluation report re-run
    // your application with 'debug' enabled.”
    //
    // Jak czytać raport (fragment, skrócony):
    //   ============================
    //   CONDITIONS EVALUATION REPORT
    //   ============================
    //   Positive matches:                                   ← autokonfiguracje / metody, które ZADZIAŁAŁY
    //      DataSourceAutoConfiguration matched:
    //         - @ConditionalOnClass found required classes 'javax.sql.DataSource', … (OnClassCondition)
    //   Negative matches:                                   ← NIE zadziałały — z powodem
    //      JndiDataSourceAutoConfiguration:
    //         Did not match:
    //            - @ConditionalOnProperty (spring.datasource.jndi-name) did not find property 'spring.datasource.jndi-name'
    //   Exclusions:                                         ← wyłączone ręcznie (sekcja 5)
    //   Unconditional classes:                              ← klasy bez warunków (włączane zawsze)
    // Szukaj w nim nazwy technologii (Ctrl+F „DataSource”) i czytaj linię „Did not match” — to odpowiedź na „czemu
    // Boot nie utworzył beana X?”.
    //
    // W kodzie / testach: ConditionEvaluationReport.get(context.getBeanFactory()).getConditionAndOutcomesBySource()
    //   → mapa: nazwa klasy (albo klasa$Zagnieżdżona, albo klasa#metoda) → wyniki warunków; isFullMatch() = wszystkie
    //   warunki spełnione. getExclusions() → lista wyłączonych autokonfiguracji.
    // Test sekcji 4: DataSourceAutoConfiguration ma pełne dopasowanie, JndiDataSourceAutoConfiguration nie
    //   (warunek spring.datasource.jndi-name — nikt go nie ustawił).
    //
    // PUŁAPKA: debug=true zostawione w pliku na produkcji. Dlaczego źle? Raport ma setki linii przy KAŻDYM starcie
    //   i zaśmieca logi. Uwaga: debug=true włącza raport i więcej logów Boota, ale NIE przełącza całej aplikacji
    //   w poziom DEBUG (to robi logging.level.root=debug).

    // =================================================================================================
    // 5. WYŁĄCZANIE AUTOKONFIGURACJI — exclude
    // =================================================================================================
    //
    //   W kodzie:     @SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
    //                 (gdy klasy nie ma na classpath w czasie kompilacji: excludeName = "pełna.nazwa.Klasy")
    //   W ustawieniu: spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
    //                 (PEŁNA nazwa klasy; kilka — po przecinku). Działa bez zmiany kodu, np. tylko w jednym profilu.
    // Test sekcji 5:
    //   a) klasa z @EnableAutoConfiguration(exclude = …) (ten sam atrybut co w @SpringBootApplication) → brak
    //      DataSource i brak JdbcTemplate (bo zabrakło DataSource); raport wymienia klasę w Exclusions;
    //   b) Boot01Application uruchomiona z spring.autoconfigure.exclude=… → brak DataSource i JdbcTemplate;
    //   c) ApplicationContextRunner z AutoConfigurations.of(…) IGNORUJE spring.autoconfigure.exclude — tam po prostu
    //      nie podajesz klasy, której nie chcesz.
    //
    // PUŁAPKA: stara nazwa klasy w spring.autoconfigure.exclude (org.springframework.boot.autoconfigure.jdbc.…
    //   z tutoriala dla Boot 3). Dlaczego groźne? W pliku ustawień nikt nie sprawdza nazwy przy kompilacji — wpisujesz
    //   klasę, której w Boot 4 nie ma. Atrybut exclude w adnotacji jest bezpieczniejszy: zła klasa = błąd kompilacji.
    //
    // DOBRA PRAKTYKA: wyłączaj autokonfigurację, gdy jej NIE CHCESZ w ogóle (np. aplikacja ma starter JDBC tylko
    //   dla biblioteki, ale sama nie łączy się z bazą). Gdy chcesz ją TYLKO ZMIENIĆ — użyj ustawień albo własnego beana
    //   (sekcja 3). Dlaczego? exclude wyłącza całą klasę razem ze wszystkimi jej beanami, a od tego bywa zależna reszta
    //   (tu: JdbcTemplate, JdbcClient, menedżer transakcji).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @SpringBootApplication = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan.
     *   • Lista autokonfiguracji: META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports w jarach.
     *   • Boot 4: org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration (jar spring-boot-jdbc);
     *     Boot 2/3: org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.
     *   • Warunki: @ConditionalOnClass, @ConditionalOnMissingBean, @ConditionalOnBean/@ConditionalOnSingleCandidate,
     *     @ConditionalOnProperty.
     *   • Twoje beany są przetwarzane PRZED autokonfiguracją → @ConditionalOnMissingBean → Boot ustępuje (back-off).
     *   • Raport: --debug / debug=true → CONDITIONS EVALUATION REPORT (Positive / Negative matches, Exclusions);
     *     w kodzie ConditionEvaluationReport.get(beanFactory).
     *   • Wyłączanie: @SpringBootApplication(exclude = …) albo spring.autoconfigure.exclude=pełna.nazwa.Klasy.
     *   • Testy: ApplicationContextRunner + AutoConfigurations.of(…), withUserConfiguration, withBean,
     *     withPropertyValues, withClassLoader(new FilteredClassLoader(…)), assertThat(context).hasSingleBean/hasFailed.
     *
     * PYTANIA KONTROLNE:
     *   1. Z jakich trzech adnotacji składa się @SpringBootApplication i co robi każda z nich?
     *   2. Skąd Boot wie, jakie klasy autokonfiguracji istnieją w jarach na classpath?
     *   3. Co się stanie? Projekt ma spring-boot-starter-jdbc, ale NIE ma H2 i nie ustawiono spring.datasource.url.
     *   4. Co się stanie? Definiujesz własny {@code @Bean DataSource shopDataSource()}. Ile beanów DataSource będzie
     *      w kontekście i którego użyje JdbcTemplate?
     *   5. ZNAJDŹ BŁĄD: w application.properties wpisano
     *      {@code spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration},
     *      a DataSource w Boot 4 dalej powstaje. Dlaczego?
     *   6. Co się stanie? W teście ApplicationContextRunner z {@code AutoConfigurations.of(DataSourceAutoConfiguration.class)}
     *      dostaje {@code withPropertyValues("spring.autoconfigure.exclude=…DataSourceAutoConfiguration")}.
     *   7. Jak sprawdzisz, dlaczego Boot NIE utworzył jakiegoś beana?
     *   8. Kiedy lepiej użyć exclude, a kiedy własnego beana albo ustawień?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Boot01AutoConfigurationExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć JEDNO ustawienie w postaci {@code "klucz=wartość"}, które wyłącza autokonfigurację
     * DataSource w Boot 4. Test uruchamia z nim Boot01Application i sprawdza, że nie ma ani DataSource, ani JdbcTemplate.
     * Podpowiedź: sekcja 5; pełna nazwa klasy — {@code DataSourceAutoConfiguration.class.getName()}.
     */
    static String exercise1ExcludeProperty() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć własny DataSource łączący się z bazą {@code jdbc:h2:mem:boot01-cw2}
     * (użytkownik {@code sa}, puste hasło). Test rejestruje go jako bean obok autokonfiguracji JDBC i sprawdza, że Boot
     * ustąpił: jest jeden DataSource, ma ten adres, a JdbcTemplate używa właśnie jego.
     * Podpowiedź: {@code new DriverManagerDataSource(url, user, password)} (org.springframework.jdbc.datasource).
     */
    static DataSource exercise2OwnDataSource() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć {@code true}, gdy w raporcie autokonfiguracja o podanej pełnej nazwie klasy ma
     * WSZYSTKIE warunki spełnione; {@code false}, gdy nie ma pełnego dopasowania albo w ogóle nie ma jej w raporcie.
     * Podpowiedź: {@code report.getConditionAndOutcomesBySource().get(nazwa)} może zwrócić null; potem isFullMatch().
     */
    static boolean exercise3Matched(ConditionEvaluationReport report, String autoConfigurationClassName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć posortowaną listę PROSTYCH nazw (bez pakietu) klas autokonfiguracji z pakietu
     * {@code org.springframework.boot.jdbc.autoconfigure} (bez podpakietów), które NIE zadziałały (Negative matches).
     * Bierz tylko nazwy kończące się na {@code AutoConfiguration} (raport zawiera też zwykłe klasy konfiguracji
     * importowane przez autokonfiguracje, np. DataSourceJmxConfiguration). Pomiń wpisy klas zagnieżdżonych
     * (z {@code $}) i metod (z {@code #}). W aplikacji lekcji wynik to dwie klasy — sprawdź, które i dlaczego.
     * Podpowiedź: przejdź po {@code getConditionAndOutcomesBySource().entrySet()}, filtruj klucz: zaczyna się od
     * {@code "org.springframework.boot.jdbc.autoconfigure."}, reszta nie zawiera {@code .}, {@code $} ani {@code #};
     * wartość {@code !isFullMatch()}; potem {@code substring}, {@code endsWith} i {@code sorted()}.
     */
    static List<String> exercise4NegativeJdbcMatches(ConditionEvaluationReport report) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @SpringBootConfiguration (klasa jest konfiguracją aplikacji — odmiana @Configuration, po której testy
     *      @SpringBootTest znajdują aplikację), @EnableAutoConfiguration (dołącza autokonfiguracje z jarów),
     *      @ComponentScan (skanuje pakiet tej klasy i podpakiety). Sprawdzone testem (sekcja 1).
     *   2. Z plików META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports w każdym jarze —
     *      jedna pełna nazwa klasy na linię.
     *   3. Aplikacja się nie uruchomi: starter przyniósł pulę HikariCP, więc Boot próbuje ją zbudować, ale nie zna
     *      adresu bazy — „Failed to determine a suitable driver class”. Sprawdzone testem (sekcja 2, przypadek c).
     *   4. Dokładnie jeden — Twój shopDataSource; Boot ustępuje dzięki @ConditionalOnMissingBean, a JdbcTemplate
     *      z autokonfiguracji dostaje właśnie Twój DataSource. Sprawdzone testem (sekcja 3).
     *   5. To nazwa klasy z Boot 2/3. W Boot 4 klasa leży w org.springframework.boot.jdbc.autoconfigure, więc wpis
     *      wskazuje klasę, której nie ma — prawdziwa DataSourceAutoConfiguration działa dalej. Popraw pakiet albo użyj
     *      atrybutu exclude w adnotacji (błąd wyjdzie przy kompilacji).
     *   6. DataSource i tak powstanie: runner rejestruje klasy podane w AutoConfigurations.of bezpośrednio, z pominięciem
     *      mechanizmu, który czyta spring.autoconfigure.exclude. Sprawdzone testem (sekcja 5, przypadek c).
     *   7. Uruchomić z --debug (albo debug=true) i w CONDITIONS EVALUATION REPORT znaleźć autokonfigurację w Negative
     *      matches — linia „Did not match” podaje powód. W teście: ConditionEvaluationReport.get(beanFactory).
     *   8. exclude — gdy danej funkcji nie chcesz wcale. Ustawienia — gdy wystarczy zmienić parametry. Własny bean —
     *      gdy ustawień nie wystarcza; Boot wtedy ustąpi tylko w tym jednym miejscu, a resztę skonfiguruje dalej.
     */
    // </editor-fold>
}

package s00_start.start08_debugging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.util.PlaceholderResolutionException;
import s00_start.start08_debugging.DebugShop.StackSnapshot;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Start08Debugging. Dlaczego nie @SpringBootTest? Tu psujemy kontekst CELOWO i oglądamy błędy —
 * potrzebujemy pełnej kontroli: ApplicationContextRunner (uruchamia kontekst z podanych klas i pozwala zbadać
 * niepowodzenie), SpringApplicationBuilder (pełny start jak w prawdziwej aplikacji, z wypisaniem błędu) i
 * TestContextManager (ten sam mechanizm, którego używa @SpringBootTest, uruchomiony ręcznie).
 */
@ExtendWith(OutputCaptureExtension.class)
class Start08DebuggingTest {

    /** Nazwa pakietu lekcji — przydaje się w komunikatach, które Spring pisze z pełnymi nazwami klas. */
    private static final String PKG = "s00_start.start08_debugging";

    /** Pomocnik: uruchamia aplikację Boota bez serwera WWW, bez banera, z podanymi klasami konfiguracji. */
    private static SpringApplicationBuilder app(Class<?> config, WebApplicationType type) {
        return new SpringApplicationBuilder(config).web(type).bannerMode(Banner.Mode.OFF);
    }

    /** Pomocnik: kolejne uruchomienie Boota przywraca domyślne poziomy logowania (żeby DEBUG z jednego testu nie zalewał następnych). */
    private static void resetLogging() {
        app(BrokenConfigs.StackProbe.class, WebApplicationType.NONE).run().close();
    }

    /** Pomocnik: łańcuch przyczyn od wyjątku zewnętrznego do pierwotnego. */
    private static List<Throwable> chain(Throwable failure) {
        List<Throwable> all = new ArrayList<>();
        for (Throwable t = failure; t != null; t = t.getCause()) {
            all.add(t);
        }
        return all;
    }

    // ----------------------------------------------------------------------------------------------
    // 1. Czytanie błędu startu
    // ----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("1. Brak beana: wyjątek zewnętrzny to UnsatisfiedDependencyException, PIERWOTNA przyczyna to NoSuchBeanDefinitionException (ostatnia w łańcuchu)")
    void missingBean() {
        new ApplicationContextRunner().withUserConfiguration(BrokenConfigs.MissingBean.class).run(context -> {
            assertThat(context).hasFailed();
            Throwable failure = context.getStartupFailure();
            assertThat(failure).isInstanceOf(UnsatisfiedDependencyException.class);
            assertThat(chain(failure)).last().isInstanceOf(NoSuchBeanDefinitionException.class);
            assertThat(NestedExceptionUtils.getMostSpecificCause(failure)).isInstanceOf(NoSuchBeanDefinitionException.class);
            assertThat(failure.getMessage()).contains("Unsatisfied dependency expressed through method 'priceReport' parameter 0");
        });
    }

    @Test
    @DisplayName("1. Dwóch kandydatów: pierwotna przyczyna to NoUniqueBeanDefinitionException, w komunikacie jest lista kandydatów")
    void twoCandidates() {
        new ApplicationContextRunner().withUserConfiguration(BrokenConfigs.TwoCandidates.class).run(context -> {
            assertThat(context).hasFailed();
            Throwable root = NestedExceptionUtils.getMostSpecificCause(context.getStartupFailure());
            assertThat(root).isInstanceOf(NoUniqueBeanDefinitionException.class);
            assertThat(root.getMessage()).contains("expected single matching bean but found 2: emailNotifier,smsNotifier");
        });
    }

    @Test
    @DisplayName("1. Brak właściwości: BeanCreationException opakowuje PlaceholderResolutionException z nazwą brakującego klucza")
    void missingProperty() {
        new ApplicationContextRunner().withUserConfiguration(BrokenConfigs.MissingProperty.class).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).isInstanceOf(BeanCreationException.class);
            Throwable root = NestedExceptionUtils.getMostSpecificCause(context.getStartupFailure());
            assertThat(root).isInstanceOf(PlaceholderResolutionException.class);
            assertThat(root.getMessage()).contains("Could not resolve placeholder 'shop.currency'");
        });
    }

    @Test
    @DisplayName("1. Pełny start aplikacji z brakującym beanem wypisuje blok APPLICATION FAILED TO START z sekcjami Description i Action")
    void applicationFailedToStart(CapturedOutput output) {
        assertThatThrownBy(() -> app(BrokenConfigs.MissingBean.class, WebApplicationType.NONE).run())
                .isInstanceOf(UnsatisfiedDependencyException.class);

        String text = output.getAll();
        assertThat(text).contains("APPLICATION FAILED TO START")
                .contains("Description:")
                .contains("Parameter 0 of method priceReport in " + PKG + ".BrokenConfigs$MissingBean required a bean of type '"
                        + PKG + ".DebugShop$PriceSource' that could not be found.")
                .contains("Action:")
                .contains("Consider defining a bean of type '" + PKG + ".DebugShop$PriceSource' in your configuration.");
    }

    @Test
    @DisplayName("1. Dwóch kandydatów: analizator błędu radzi @Primary, @Qualifier albo przyjęcie wielu beanów")
    void twoCandidatesAnalysis(CapturedOutput output) {
        assertThatThrownBy(() -> app(BrokenConfigs.TwoCandidates.class, WebApplicationType.NONE).run())
                .isInstanceOf(UnsatisfiedDependencyException.class);

        assertThat(output.getAll())
                .contains("APPLICATION FAILED TO START")
                .contains("required a single bean, but 2 were found:")
                .contains("Consider marking one of the beans as @Primary, updating the consumer to accept multiple beans, "
                        + "or using @Qualifier to identify the bean that should be consumed");
    }

    @Test
    @DisplayName("1. Gdy żaden analizator nie pasuje (brak właściwości), Boot loguje „Application run failed” i pełny stos zamiast bloku Description/Action")
    void noAnalyzerMatches(CapturedOutput output) {
        assertThatThrownBy(() -> app(BrokenConfigs.MissingProperty.class, WebApplicationType.NONE).run())
                .isInstanceOf(BeanCreationException.class);

        assertThat(output.getAll()).contains("Application run failed")
                .doesNotContain("APPLICATION FAILED TO START");
    }

    // ----------------------------------------------------------------------------------------------
    // 2. Błędy kontekstu w testach
    // ----------------------------------------------------------------------------------------------

    /**
     * Klasa-ofiara: test Springa ze zepsutą konfiguracją. Nie ma żadnej metody @Test, więc JUnit jej nie uruchamia sam —
     * robimy to ręcznie przez TestContextManager, żeby zobaczyć komunikaty, które normalnie widzisz w czerwonym teście.
     */
    @SpringBootTest(classes = BrokenConfigs.MissingBean.class)
    static class BrokenContextHolder {
    }

    @Test
    @DisplayName("2. Pierwsza próba: „Failed to load ApplicationContext” z pierwotną przyczyną; druga: „failure threshold (1) exceeded” bez nowej przyczyny")
    void contextFailureThreshold() {
        TestContextManager manager = new TestContextManager(BrokenContextHolder.class);

        Throwable first = catchContextFailure(manager);
        assertThat(first).isInstanceOf(IllegalStateException.class);
        assertThat(first.getMessage()).startsWith("Failed to load ApplicationContext for [");
        assertThat(NestedExceptionUtils.getMostSpecificCause(first)).isInstanceOf(NoSuchBeanDefinitionException.class);

        Throwable second = catchContextFailure(manager);
        assertThat(second).isInstanceOf(IllegalStateException.class);
        assertThat(second.getMessage()).startsWith("ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context for [");
        assertThat(second.getCause()).as("drugi komunikat nie ma pierwotnej przyczyny — jest tylko w pierwszym").isNull();
    }

    private static Throwable catchContextFailure(TestContextManager manager) {
        return catchThrowable(() -> manager.getTestContext().getApplicationContext());
    }

    private static Throwable catchThrowable(Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            return t;
        }
        throw new AssertionError("Oczekiwano błędu ładowania kontekstu, a kontekst się uruchomił");
    }

    // ----------------------------------------------------------------------------------------------
    // 3. Debugger i ramki stosu
    // ----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("3. Spring opakowuje wyjątek: na wierzchu BeanCreationException, w środku BeanInstantiationException, na dnie nasz IllegalStateException")
    void wrappedException() {
        new ApplicationContextRunner().withUserConfiguration(BrokenConfigs.ThrowingBean.class).run(context -> {
            assertThat(context).hasFailed();
            List<Class<?>> types = chain(context.getStartupFailure()).stream().<Class<?>>map(Throwable::getClass).toList();
            assertThat(types).first().isEqualTo(BeanCreationException.class);
            assertThat(types).last().isEqualTo(IllegalStateException.class);
            assertThat(types).hasSizeGreaterThanOrEqualTo(3);
            assertThat(context.getStartupFailure().getMessage())
                    .contains("Error creating bean with name 'stockSnapshot'")
                    .contains("Factory method 'stockSnapshot' threw exception with message: Magazyn niedostępny");
            assertThat(NestedExceptionUtils.getMostSpecificCause(context.getStartupFailure()))
                    .hasMessage("Magazyn niedostępny");
        });
    }

    @Test
    @DisplayName("3. W ramkach stosu widać klasy proxy ($$SpringCGLIB$$): konfiguracja jest podklasą wygenerowaną przez Springa")
    void cglibFramesInStack() {
        new ApplicationContextRunner().withUserConfiguration(BrokenConfigs.StackProbe.class).run(context -> {
            assertThat(context).hasNotFailed();
            StackSnapshot snapshot = context.getBean(StackSnapshot.class);
            assertThat(snapshot.frames()).anyMatch(frame -> frame.contains("$$SpringCGLIB$$"));
            // ramki kodu Springa to większość stosu — Twój kod jest wąskim skrawkiem (patrz też: Step Over / Force Step Into)
            assertThat(snapshot.frames()).anyMatch(frame -> frame.startsWith("org.springframework.beans.factory.support."));
            assertThat(context.getBean(BrokenConfigs.StackProbe.class).getClass().getName()).contains("$$SpringCGLIB$$");
        });
    }

    // ----------------------------------------------------------------------------------------------
    // 4. Logi
    // ----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("4. logging.level.<pakiet>=DEBUG pokazuje wpisy DEBUG tego pakietu; domyślnie (INFO) ich nie ma")
    void packageLogLevel(CapturedOutput output) {
        app(BrokenConfigs.LoggingConfig.class, WebApplicationType.NONE).run().close();
        assertThat(output.getAll()).contains("[Start08] wpis INFO")
                .doesNotContain("[Start08] wpis DEBUG");

        app(BrokenConfigs.LoggingConfig.class, WebApplicationType.NONE)
                .properties("logging.level." + PKG + "=DEBUG").run().close();
        assertThat(output.getAll()).contains("DEBUG").contains("[Start08] wpis DEBUG: tworzę bean diagnosticMarker");
        resetLogging();
    }

    @Test
    @DisplayName("4. Argument --debug (albo debug=true) włącza raport warunków autokonfiguracji; bez niego raportu nie ma")
    void conditionsReport(CapturedOutput output) {
        app(BrokenConfigs.WithAutoConfiguration.class, WebApplicationType.NONE).run().close();
        assertThat(output.getAll()).doesNotContain("CONDITIONS EVALUATION REPORT");

        app(BrokenConfigs.WithAutoConfiguration.class, WebApplicationType.NONE).run("--debug").close();
        assertThat(output.getAll()).contains("CONDITIONS EVALUATION REPORT")
                .contains("Positive matches:")
                .contains("Negative matches:");
        resetLogging();
    }

    // ----------------------------------------------------------------------------------------------
    // 5. Błąd środowiska a błąd kodu
    // ----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("5. Zajęty port to błąd ŚRODOWISKA: Boot pisze „Port N was already in use” (PortInUseException w łańcuchu), a na dnie leży BindException z systemu")
    void portAlreadyInUse(CapturedOutput output) throws Exception {
        try (ServerSocket occupied = new ServerSocket(0)) {   // 0 = system wybiera wolny port; trzymamy go zajętego
            int port = occupied.getLocalPort();

            assertThatThrownBy(() -> app(BrokenConfigs.WithAutoConfiguration.class, WebApplicationType.SERVLET)
                    .properties("server.port=" + port).run())
                    .isInstanceOf(ApplicationContextException.class)
                    .satisfies(e -> {
                        assertThat(chain(e)).anyMatch(t -> t instanceof PortInUseException);
                        assertThat(NestedExceptionUtils.getMostSpecificCause(e)).isInstanceOf(java.net.BindException.class);
                    });

            assertThat(output.getAll()).contains("Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.")
                    .contains("APPLICATION FAILED TO START")
                    .contains("Web server failed to start. Port " + port + " was already in use.")
                    .contains("Identify and stop the process that's listening on port " + port
                            + " or configure this application to listen on another port.");
            assertThat(ErrorCatalog.diagnose(output.getAll())).get()
                    .extracting(ErrorCatalog.Diagnosis::kind).isEqualTo(ErrorCatalog.Kind.ENVIRONMENT);
        }
    }

    @Test
    @DisplayName("5. Katalog błędów odróżnia błędy środowiska (JDK, agent Mockito, port) od błędów kodu (brak beana, dwóch kandydatów)")
    void errorCatalog() {
        assertThat(ErrorCatalog.diagnose("error: release version 21 not supported").orElseThrow().kind())
                .isEqualTo(ErrorCatalog.Kind.ENVIRONMENT);
        assertThat(ErrorCatalog.diagnose("Could not self-attach to current VM using external process").orElseThrow().kind())
                .isEqualTo(ErrorCatalog.Kind.ENVIRONMENT);
        assertThat(ErrorCatalog.diagnose("Port 8080 was already in use.").orElseThrow().kind())
                .isEqualTo(ErrorCatalog.Kind.ENVIRONMENT);
        assertThat(ErrorCatalog.diagnose("required a bean of type 'x.Y' that could not be found").orElseThrow().kind())
                .isEqualTo(ErrorCatalog.Kind.CODE);
        assertThat(ErrorCatalog.diagnose("required a single bean, but 2 were found").orElseThrow().kind())
                .isEqualTo(ErrorCatalog.Kind.CODE);
        assertThat(ErrorCatalog.diagnose("Unable to find a @SpringBootConfiguration by searching packages upwards").orElseThrow().kind())
                .isEqualTo(ErrorCatalog.Kind.CODE);
        assertThat(ErrorCatalog.diagnose("coś zupełnie nieznanego")).isEmpty();
    }

    @Test
    @DisplayName("5. Ten test w ogóle działa, więc JDK ma co najmniej wersję 21 (wymóg kursu)")
    void javaVersion() {
        assertThat(Runtime.version().feature()).as("kurs wymaga Javy 21 lub nowszej").isGreaterThanOrEqualTo(21);
    }
}

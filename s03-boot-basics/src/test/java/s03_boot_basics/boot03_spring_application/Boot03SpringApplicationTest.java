package s03_boot_basics.boot03_spring_application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.Banner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Boot03SpringApplication — jeden test na sekcję.
 * <p>
 * Tu NIE używamy {@code @SpringBootTest}: sam jest „opakowaniem” na SpringApplication, a my chcemy właśnie
 * SpringApplication wołać ręcznie (ustawiać typ aplikacji, odbiorniki zdarzeń, argumenty). Każdy kontekst zamykamy
 * w try-with-resources. Aplikacje „zepsute” celowo (brak zależności) to klasy wewnętrzne testu — skan komponentów
 * aplikacji ich nie widzi, więc nie psują sekcji, w których uruchamiamy Boot03Application.
 */
class Boot03SpringApplicationTest {

    private static final String[] SHOP_ARGS = {"--shop.mode=demo", "zeszyt", "--x=1", "--x=2"};

    /** Typ, którego nikt nie zarejestrował jako bean — wywoła błąd „brak zależności”. */
    static class MissingDependency {
    }

    /** Konfiguracja z błędem: metoda @Bean prosi o beana, którego nie ma. */
    @Configuration(proxyBeanMethods = false)
    static class BrokenConfiguration {
        @Bean
        String shopName(MissingDependency missing) {
            return "Sklep " + missing;
        }
    }

    /** Generator kodu wyjścia jako bean (sekcja 6). */
    @Configuration(proxyBeanMethods = false)
    static class ExitCodeConfiguration {
        @Bean
        ExitCodeGenerator shopExitCode() {
            return () -> 7;
        }
    }

    /** Bean z metodami @EventListener — słyszy tylko to, co leci PO utworzeniu beanów (sekcja 4). */
    static class BeanListener {
        final List<String> heard = new ArrayList<>();

        @EventListener
        void onStarting(ApplicationStartingEvent event) {
            heard.add("Starting");
        }

        @EventListener
        void onPrepared(ApplicationPreparedEvent event) {
            heard.add("Prepared");
        }

        @EventListener
        void onStarted(ApplicationStartedEvent event) {
            heard.add("Started");
        }

        @EventListener
        void onReady(ApplicationReadyEvent event) {
            heard.add("Ready");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class BeanListenerConfiguration {
        @Bean
        BeanListener beanListener() {
            return new BeanListener();
        }
    }

    private static SpringApplication shopApplication(Class<?>... extraSources) {
        Class<?>[] sources = new Class<?>[extraSources.length + 1];
        sources[0] = Boot03Application.class;
        System.arraycopy(extraSources, 0, sources, 1, extraSources.length);
        SpringApplication application = new SpringApplication(sources);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application;
    }

    /** Odbiornik zapisujący nazwy zdarzeń; wpisy ShopStartupLog (zwykłe teksty) dopisuje jako „[runner] …”. */
    private static ApplicationListener<ApplicationEvent> recorder(List<String> sink) {
        return event -> {
            if (event instanceof PayloadApplicationEvent<?> payload && payload.getPayload() instanceof String text) {
                sink.add("[runner] " + text);
            } else if (event.getClass().getPackageName().startsWith("org.springframework.boot.context.event")
                    || event instanceof ContextRefreshedEvent) {
                sink.add(event.getClass().getSimpleName());
            }
        };
    }

    @Test
    @DisplayName("1. new SpringApplication + settery: typ aplikacji, profil i ustawienie domyślne (argument je przesłania)")
    void section1_springApplicationStepByStep() {
        SpringApplication application = new SpringApplication(Boot03Application.class);
        // w tym module nie ma serwera WWW na classpath, więc Boot sam wybiera NONE (więcej: Boot04Starters)
        assertThat(application.getWebApplicationType()).isEqualTo(WebApplicationType.NONE);
        application.setWebApplicationType(WebApplicationType.NONE); // jawnie — tu się to ustawia
        application.setDefaultProperties(Map.of("shop.mode", "default"));
        application.setAdditionalProfiles("demo");

        // bez argumentu wygrywa wartość domyślna
        try (ConfigurableApplicationContext context = application.run()) {
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("demo");
            assertThat(context.getEnvironment().getProperty("shop.mode")).isEqualTo("default");
            assertThat(context.isActive()).isTrue();
        }

        // argument programu ma wyższy priorytet niż setDefaultProperties — i trafia do Environment jako ustawienie
        ConfigurableApplicationContext closed;
        try (ConfigurableApplicationContext context = application.run("--shop.mode=demo")) {
            assertThat(context.getEnvironment().getProperty("shop.mode")).isEqualTo("demo");
            closed = context;
        }
        assertThat(closed.isActive()).as("try-with-resources zamknął kontekst").isFalse();
    }

    @Test
    @DisplayName("2. SpringApplicationBuilder daje ten sam efekt płynnym łańcuchem wywołań")
    void section2_builder() {
        int before = LazyReportGenerator.createdCount();
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Boot03Application.class)
                .web(WebApplicationType.NONE)
                .properties("shop.mode=builder")
                .profiles("demo")
                .bannerMode(Banner.Mode.OFF)
                .lazyInitialization(true)
                .run()) {
            assertThat(context.getEnvironment().getProperty("shop.mode")).isEqualTo("builder");
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("demo");
            // lazyInitialization(true) działa jak spring.main.lazy-initialization=true: beany powstają dopiero przy użyciu
            assertThat(LazyReportGenerator.createdCount()).as("leniwy bean jeszcze nie powstał").isEqualTo(before);
        }
    }

    @Test
    @DisplayName("3. CommandLineRunner dostaje surowe argumenty, ApplicationRunner — rozdzielone na opcje i zwykłe; @Order ustala kolejność")
    void section3_runners() {
        try (ConfigurableApplicationContext context = shopApplication().run(SHOP_ARGS)) {
            assertThat(context.getBean(ShopStartupLog.class).entries()).containsExactly(
                    "CommandLineRunner: [--shop.mode=demo, zeszyt, --x=1, --x=2]",
                    "ApplicationRunner: opcje=[shop.mode, x], zwykłe=[zeszyt]");
        }
    }

    @Test
    @DisplayName("4. Zdarzenia startu: kolejność od Starting do Ready, a bean z @EventListener nie słyszy wczesnych zdarzeń")
    void section4_events() {
        List<String> events = new ArrayList<>();
        SpringApplication application = shopApplication(BeanListenerConfiguration.class);
        application.addListeners(recorder(events)); // jedyny sposób na złapanie zdarzenia Starting

        try (ConfigurableApplicationContext context = application.run(SHOP_ARGS)) {
            assertThat(events).containsExactly(
                    "ApplicationStartingEvent",
                    "ApplicationEnvironmentPreparedEvent",
                    "ApplicationContextInitializedEvent",
                    "ApplicationPreparedEvent",
                    "ContextRefreshedEvent",
                    "ApplicationStartedEvent",
                    "[runner] CommandLineRunner: [--shop.mode=demo, zeszyt, --x=1, --x=2]",
                    "[runner] ApplicationRunner: opcje=[shop.mode, x], zwykłe=[zeszyt]",
                    "ApplicationReadyEvent");

            // PUŁAPKA: @EventListener w beanie słyszy dopiero to, co leci po powstaniu beana
            assertThat(context.getBean(BeanListener.class).heard).containsExactly("Started", "Ready");
        }
    }

    @Test
    @DisplayName("5. Leniwa inicjalizacja: bean powstaje dopiero przy pierwszym użyciu, a błąd konfiguracji wychodzi późno")
    void section5_lazyInitialization() {
        // tryb gorliwy (domyślny) — bean powstaje przy starcie
        int beforeEager = LazyReportGenerator.createdCount();
        try (ConfigurableApplicationContext context = shopApplication().run()) {
            assertThat(LazyReportGenerator.createdCount() - beforeEager).isEqualTo(1);
        }

        // tryb leniwy — bean powstaje przy pierwszym getBean, drugi getBean zwraca tego samego singletona
        SpringApplication lazy = shopApplication();
        lazy.setLazyInitialization(true);
        int beforeLazy = LazyReportGenerator.createdCount();
        try (ConfigurableApplicationContext context = lazy.run()) {
            assertThat(LazyReportGenerator.createdCount() - beforeLazy).isZero();
            LazyReportGenerator first = context.getBean(LazyReportGenerator.class);
            assertThat(LazyReportGenerator.createdCount() - beforeLazy).isEqualTo(1);
            assertThat(context.getBean(LazyReportGenerator.class)).isSameAs(first);
            assertThat(LazyReportGenerator.createdCount() - beforeLazy).isEqualTo(1);
        }

        // PUŁAPKA: wadliwy bean — gorliwy start pada od razu...
        assertThatThrownBy(() -> new SpringApplication(BrokenConfiguration.class) {{
            setWebApplicationType(WebApplicationType.NONE);
        }}.run()).isInstanceOf(UnsatisfiedDependencyException.class);

        // ...a leniwy startuje bez błędu i wywala się dopiero przy pierwszym użyciu beana
        SpringApplication lazyBroken = new SpringApplication(BrokenConfiguration.class);
        lazyBroken.setWebApplicationType(WebApplicationType.NONE);
        lazyBroken.setLazyInitialization(true);
        try (ConfigurableApplicationContext context = lazyBroken.run()) {
            assertThat(context.isActive()).isTrue();
            assertThatThrownBy(() -> context.getBean("shopName")).isInstanceOf(UnsatisfiedDependencyException.class);
        }
    }

    @Test
    @DisplayName("6. Kod wyjścia z ExitCodeGenerator (zero jest pomijane, argument wyprzedza bean) oraz nieudany start: wyjątek i ApplicationFailedEvent")
    void section6_exitCodesAndFailure() {
        // kod wyjścia z beana ExitCodeGenerator; exit zamyka kontekst, ale NIE kończy procesu
        ConfigurableApplicationContext withBean = shopApplication(ExitCodeConfiguration.class).run();
        assertThat(SpringApplication.exit(withBean)).isEqualTo(7);
        assertThat(withBean.isActive()).isFalse();

        // generator zwracający 0 jest pomijany — liczy się pierwszy niezerowy; generator z argumentu wyprzedza beany
        ConfigurableApplicationContext withZero = shopApplication(ExitCodeConfiguration.class).run();
        assertThat(SpringApplication.exit(withZero, () -> 0)).isEqualTo(7);
        ConfigurableApplicationContext withArgument = shopApplication(ExitCodeConfiguration.class).run();
        assertThat(SpringApplication.exit(withArgument, () -> 9)).isEqualTo(9);

        ConfigurableApplicationContext withoutGenerators = shopApplication().run();
        assertThat(SpringApplication.exit(withoutGenerators)).isZero();

        // nieudany start: run() rzuca wyjątek, a odbiornik dostaje ApplicationFailedEvent z TYM SAMYM wyjątkiem
        List<ApplicationEvent> events = new ArrayList<>();
        SpringApplication broken = new SpringApplication(BrokenConfiguration.class);
        broken.setWebApplicationType(WebApplicationType.NONE);
        broken.addListeners(events::add);

        assertThatThrownBy(broken::run)
                .isInstanceOf(UnsatisfiedDependencyException.class)
                .hasMessageContaining("MissingDependency")
                .satisfies(thrown -> assertThat(events).filteredOn(ApplicationFailedEvent.class::isInstance)
                        .singleElement()
                        .satisfies(event -> assertThat(((ApplicationFailedEvent) event).getException()).isSameAs(thrown)));
        assertThat(events).noneMatch(event -> event instanceof ApplicationStartedEvent || event instanceof ApplicationReadyEvent);
    }
}

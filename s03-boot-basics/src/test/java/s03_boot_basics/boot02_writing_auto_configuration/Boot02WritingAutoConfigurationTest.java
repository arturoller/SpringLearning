package s03_boot_basics.boot02_writing_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import s03_boot_basics.boot01_auto_configuration.Boot01Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Boot02WritingAutoConfiguration.
 * {@code @SpringBootTest} + profil boot02 — prawdziwa aplikacja lekcji (sekcje 1–2); SpringApplicationBuilder — aplikacja
 * INNEJ lekcji (sekcja 3); ApplicationContextRunner — szybkie przypadki warunków (sekcja 4).
 */
@SpringBootTest
@ActiveProfiles("boot02")
class Boot02WritingAutoConfigurationTest {

    private static final String OUR_AUTO_CONFIGURATION = Boot02GreetingAutoConfiguration.class.getName();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Boot02GreetingAutoConfiguration.class));

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("1. Z profilem boot02 autokonfiguracja tworzy domyślny GreetingService z nazwą sklepu z właściwości")
    void autoConfigurationCreatesDefaultBean() {
        GreetingService greetingService = context.getBean(GreetingService.class);
        assertThat(greetingService).isInstanceOf(DefaultGreetingService.class);
        assertThat(greetingService.greet("Ania")).isEqualTo("Witaj, Ania! Zapraszamy do sklepu Sklep pod Sprezyna.");
        assertThat(context.getBean(Boot02GreetingProperties.class).enabled()).isTrue();
    }

    @Test
    @DisplayName("2. Plik .imports rejestruje naszą klasę; skanowanie jej nie dubluje (bean ma nazwę autokonfiguracji)")
    void importsFileRegistersAutoConfiguration() {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates())
                .contains(OUR_AUTO_CONFIGURATION);
        // autokonfiguracje dostają jako nazwę beana PEŁNĄ nazwę klasy; ze skanowania byłoby "boot02GreetingAutoConfiguration"
        assertThat(context.getBeanNamesForType(Boot02GreetingAutoConfiguration.class))
                .containsExactly(OUR_AUTO_CONFIGURATION);
    }

    @Test
    @DisplayName("3. Bezpiecznik: w aplikacji innej lekcji (Boot01) bean jest tylko po ustawieniu właściwości (pytanie 2)")
    void guardKeepsOtherLessonsClean() {
        try (var boot01 = new SpringApplicationBuilder(Boot01Application.class).web(WebApplicationType.NONE).run()) {
            assertThat(boot01.getBeanNamesForType(GreetingService.class)).isEmpty();
            var outcomes = ConditionEvaluationReport.get(boot01.getBeanFactory()).getConditionAndOutcomesBySource();
            assertThat(outcomes.get(OUR_AUTO_CONFIGURATION).isFullMatch()).as("Negative match w raporcie").isFalse();
        }
        try (var boot01 = new SpringApplicationBuilder(Boot01Application.class).web(WebApplicationType.NONE)
                .properties("boot02.greeting.enabled=true").run()) {
            assertThat(boot01.getBean(GreetingService.class).greet("Ola"))
                    .isEqualTo("Witaj, Ola! Zapraszamy do sklepu Sklep Kursanta.");
        }
    }

    @Test
    @DisplayName("4. Runner: bean domyślny / bean użytkownika wygrywa / wyłączone bez właściwości (pytanie 3)")
    void testingWithApplicationContextRunner() {
        runner.withPropertyValues("boot02.greeting.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(GreetingService.class)
                        .getBean(GreetingService.class).isInstanceOf(DefaultGreetingService.class));

        runner.withPropertyValues("boot02.greeting.enabled=true")
                .withUserConfiguration(Boot02CustomGreetingConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(GreetingService.class)
                            .hasBean("vipGreetingService")
                            .doesNotHaveBean("greetingService");
                    assertThat(ctx.getBean(GreetingService.class).greet("Ola")).startsWith("Witamy ponownie, Ola!");
                });

        runner.run(ctx -> assertThat(ctx).hasNotFailed().doesNotHaveBean(GreetingService.class));
        runner.withPropertyValues("boot02.greeting.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(GreetingService.class));
    }

    /** Odbiorca wstrzykujący GreetingService przez konstruktor — do pokazania błędu z pytania 5. */
    record GreetingPrinter(GreetingService service) {
    }

    /** Ten sam odbiorca, ale nazwa parametru = nazwa jednego z beanów ("greetingService"). */
    record NamedGreetingPrinter(GreetingService greetingService) {
    }

    @Test
    @DisplayName("4. Bez @ConditionalOnMissingBean: dwa beany i błąd startu NoUniqueBeanDefinitionException (pytanie 5)")
    void missingBackOffCausesTwoBeans() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Boot02NoBackOffAutoConfiguration.class))
                .withUserConfiguration(Boot02CustomGreetingConfig.class)
                .withBean(GreetingPrinter.class)
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure().rootCause().isInstanceOf(NoUniqueBeanDefinitionException.class)
                        .hasMessageContaining("expected single matching bean but found 2"));

        // ciekawostka: gdy nazwa parametru konstruktora jest równa nazwie beana, Spring wybiera ten bean po nazwie
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Boot02NoBackOffAutoConfiguration.class))
                .withUserConfiguration(Boot02CustomGreetingConfig.class)
                .withBean(NamedGreetingPrinter.class)
                .run(ctx -> assertThat(ctx).hasNotFailed().getBeans(GreetingService.class).hasSize(2));
    }
}

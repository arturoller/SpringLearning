package s01_core_ioc.ioc04_bean_methods;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Ioc04BeanMethods. {@code @SpringBootTest} startuje kontekst Ioc04Application (sekcje 1–2),
 * {@code ApplicationContextRunner} (mały, szybki kontekst budowany w teście) pokazuje tryb pełny i lekki,
 * a zwykłe testy jednostkowe — generator z zatrzymanym zegarem.
 */
@SpringBootTest
class Ioc04BeanMethodsTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-11T10:00:00Z"), ZoneId.of("UTC"));

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("1. Nazwa beana z metody @Bean to nazwa metody, a @Bean(\"nazwa\") ją zmienia")
    void beanNames() {
        assertThat(context.getBeanNamesForType(Clock.class)).containsExactly("clock");
        assertThat(context.getBeanNamesForType(NumberFormat.class)).containsExactly("plnFormat");
        assertThat(context.containsBean("currencyFormat")).isFalse();
        String formatted = context.getBean("plnFormat", NumberFormat.class).format(12.5)
                .replace(Character.toString(160), " ");   // twarda spacja -> zwykła, dla czytelności asercji
        assertThat(formatted).isEqualTo("12,50 zł");
    }

    @Test
    @DisplayName("2. Zależność metody @Bean (parametr Clock) została uzupełniona beanem")
    void methodParameterIsInjected() {
        InvoiceNumberGenerator generator = context.getBean(InvoiceNumberGenerator.class);
        assertThat(generator.next()).matches("FV/\\d{4}/\\d{2}/0001");
    }

    @Test
    @DisplayName("3. Clock i NumberFormat to klasy abstrakcyjne bez publicznego konstruktora: bean daje tylko metoda @Bean")
    void clockNeedsBeanMethod() {
        assertThat(Modifier.isAbstract(Clock.class.getModifiers())).isTrue();
        assertThat(Clock.class.getConstructors()).isEmpty();          // brak publicznych konstruktorów
        assertThat(Modifier.isAbstract(NumberFormat.class.getModifiers())).isTrue();
        assertThat(NumberFormat.class.getConstructors()).isEmpty();

        new ApplicationContextRunner().withUserConfiguration(ShopConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(Clock.class);
            assertThat(ctx).hasSingleBean(NumberFormat.class);
        });
    }

    @Test
    @DisplayName("4. Wstrzyknięty Clock: generator z zatrzymanym zegarem daje powtarzalne numery faktur")
    void fixedClockGivesRepeatableNumbers() {
        InvoiceNumberGenerator generator = new InvoiceNumberGenerator(FIXED);
        assertThat(generator.next()).isEqualTo("FV/2026/10/0001");
        assertThat(generator.next()).isEqualTo("FV/2026/10/0002");
        Clock november = Clock.fixed(Instant.parse("2026-11-02T08:00:00Z"), ZoneId.of("UTC"));
        assertThat(new InvoiceNumberGenerator(november).next()).isEqualTo("FV/2026/11/0001");
    }

    @Test
    @DisplayName("5. Tryb pełny (proxy): wywołanie metody @Bean zwraca TEGO SAMEGO singletona")
    void fullModeReturnsSameSingleton() {
        new ApplicationContextRunner().withUserConfiguration(FullModeConfig.class).run(ctx -> {
            PriceCalculator calculator = ctx.getBean("fullCalculator", PriceCalculator.class);
            assertThat(calculator.taxTable()).isSameAs(ctx.getBean("fullTaxTable", TaxTable.class));
            assertThat(ctx.getBean(FullModeConfig.class).getClass().getName()).contains("$$SpringCGLIB$$");
        });
    }

    @Test
    @DisplayName("5. Tryb lekki: wywołanie metody @Bean tworzy NOWY obiekt, a parametr daje prawdziwego beana")
    void liteModeCreatesNewObject() {
        new ApplicationContextRunner().withUserConfiguration(LiteModeConfig.class).run(ctx -> {
            TaxTable bean = ctx.getBean("liteTaxTable", TaxTable.class);
            assertThat(ctx.getBean("liteCalculator", PriceCalculator.class).taxTable()).isNotSameAs(bean);
            assertThat(ctx.getBean("liteCalculatorFromParameter", PriceCalculator.class).taxTable()).isSameAs(bean);
            assertThat(ctx.getBean(LiteModeConfig.class).getClass()).isEqualTo(LiteModeConfig.class);
        });
    }

    @Test
    @DisplayName("5. Dwa beany TaxTable w aplikacji: @Qualifier wskazuje właściwy, sama nazwa parametru jest krucha")
    void twoTaxTablesNeedQualifier() {
        // aplikacja lekcji: kalkulator z @Qualifier("liteTaxTable") dostał właściwego beana
        assertThat(context.getBeanNamesForType(TaxTable.class)).containsExactlyInAnyOrder("fullTaxTable", "liteTaxTable");
        assertThat(context.getBean("liteCalculatorFromParameter", PriceCalculator.class).taxTable())
                .isSameAs(context.getBean("liteTaxTable"));

        // bez @Qualifier: parametr nazwany jak bean (liteTaxTable) działa tylko dzięki nazwie...
        new ApplicationContextRunner()
                .withUserConfiguration(FullModeConfig.class, LiteModeConfig.class, ParameterNamedLikeBeanConfig.class)
                .run(ctx -> assertThat(ctx.getBean("byParameterName", PriceCalculator.class).taxTable())
                        .isSameAs(ctx.getBean("liteTaxTable")));
        // ...a po zmianie nazwy parametru na taxTable aplikacja nie startuje
        new ApplicationContextRunner()
                .withUserConfiguration(FullModeConfig.class, LiteModeConfig.class, RenamedParameterConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).rootCause()
                            .isInstanceOf(NoUniqueBeanDefinitionException.class)
                            .hasMessageContaining("expected single matching bean but found 2: fullTaxTable,liteTaxTable");
                });
    }

    @Test
    @DisplayName("5. Pytanie kontrolne 4: clock() na beanie ShopConfig (proxy) zwraca singletona z kontenera")
    void callingBeanMethodOnProxyReturnsSingleton() {
        ShopConfig config = context.getBean(ShopConfig.class);
        assertThat(config.clock()).isSameAs(context.getBean(Clock.class));
    }

    @Test
    @DisplayName("5. Zwykła klasa z metodą @Bean (bez @Configuration) też działa — w trybie lekkim")
    void plainClassWithBeanMethodIsLite() {
        new ApplicationContextRunner().withUserConfiguration(PlainConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(TaxTable.class);
            assertThat(ctx.getBean(PlainConfig.class).getClass()).isEqualTo(PlainConfig.class);
        });
    }

    /** Zwykła klasa BEZ @Configuration (więc nie jest skanowana przez Ioc04Application). */
    static class PlainConfig {
        @Bean
        TaxTable plainTaxTable() {
            return new TaxTable();
        }
    }

    /** Bez @Qualifier: parametr nazwany tak samo jak bean "liteTaxTable" — dopasowanie po nazwie parametru. */
    static class ParameterNamedLikeBeanConfig {
        @Bean
        PriceCalculator byParameterName(TaxTable liteTaxTable) {
            return new PriceCalculator(liteTaxTable);
        }
    }

    /** Bez @Qualifier i z inną nazwą parametru — przy dwóch beanach TaxTable kontener nie ma jak wybrać. */
    static class RenamedParameterConfig {
        @Bean
        PriceCalculator renamedParameter(TaxTable taxTable) {
            return new PriceCalculator(taxTable);
        }
    }
}

package s02_configuration.cfg05_environment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Cfg05Environment. Zmiennych środowiska systemu NIE ustawiamy naprawdę (testy zależałyby od komputera):
 * podstawiamy sztuczne źródło {@link SystemEnvironmentPropertySource} o nazwie "systemEnvironment" — w zwykłym
 * środowisku Springa albo w całej aplikacji.
 */
@SpringBootTest
@ActiveProfiles("cfg05")
class Cfg05EnvironmentTest {

    @Autowired
    Environment environment;

    @Autowired
    DiscountService discounts;

    @Autowired
    Checkout checkout;

    /** Ustawienia do wiązania Binderem; nie jest beanem. Pole discountPercent ↔ klucz discount-percent. */
    record ShopSettings(int discountPercent, String label, List<String> days) {
    }

    /** Wariant PUŁAPKI: warunek havingValue="false" bez matchIfMissing. */
    @ConditionalOnProperty(prefix = "cfg05.feature", name = "new-checkout", havingValue = "false")
    static class StrictLegacyCheckout implements Checkout {
        @Override
        public String version() {
            return "stara";
        }
    }

    /** Klasa, która wymaga kasy w konstruktorze. */
    static class NeedsCheckout {
        NeedsCheckout(Checkout checkout) {
        }
    }

    /** Flaga logiczna nowszym zapisem (Boot 3.5+): bean istnieje, gdy cfg05.feature.gift-wrap=true. Nie jest skanowana. */
    @ConditionalOnBooleanProperty("cfg05.feature.gift-wrap")
    static class GiftWrap {
    }

    /**
     * Środowisko, w którym prawdziwe zmienne systemu zastępujemy mapą z testu. Ważne: źródło dostaje nazwę
     * "systemEnvironment" (stała SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME) — tylko wtedy Boot stosuje do niego
     * reguły nazw zmiennych środowiska, tak jak do prawdziwych zmiennych systemu (PUŁAPKA z sekcji 3).
     */
    private static StandardEnvironment osEnvironment(Map<String, Object> variables) {
        StandardEnvironment env = new StandardEnvironment();
        String name = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
        env.getPropertySources().replace(name, new SystemEnvironmentPropertySource(name, variables));
        return env;
    }

    /** Wiąże klucz cfg05.shop.discount-percent tak, jak robi to @ConfigurationProperties; brak wartości = null. */
    private static Integer bindPercent(StandardEnvironment env) {
        return new Binder(ConfigurationPropertySources.get(env))
                .bind("cfg05.shop.discount-percent", Integer.class).orElse(null);
    }

    /** Startuje lekcję (profil cfg05) z podstawionymi zmiennymi systemu — resztę (argumenty, JVM, pliki) Boot dokłada sam. */
    private static ConfigurableApplicationContext startWithFakeOsEnvironment(Map<String, Object> variables, String... args) {
        return new SpringApplicationBuilder(Cfg05Application.class)
                .environment(osEnvironment(variables))
                .profiles("cfg05")
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Test
    @DisplayName("1. Environment: getProperty (z typem i wartością domyślną), getRequiredProperty i profile")
    void environmentApi() {
        assertThat(environment.getProperty("cfg05.shop.discount-percent")).isEqualTo("10");
        assertThat(environment.getProperty("cfg05.shop.discount-percent", Integer.class)).isEqualTo(10);
        assertThat(environment.getProperty("cfg05.brak")).isNull();
        assertThat(environment.getProperty("cfg05.brak", "domyślna")).isEqualTo("domyślna");
        assertThat(environment.containsProperty("cfg05.shop.discount-percent")).isTrue();

        assertThatThrownBy(() -> environment.getRequiredProperty("cfg05.brak"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cfg05.brak");

        assertThat(environment.getActiveProfiles()).containsExactly("cfg05");
        assertThat(environment.acceptsProfiles(Profiles.of("cfg05"))).isTrue();
        assertThat(environment.acceptsProfiles(Profiles.of("cfg05-weekend"))).isFalse();
        assertThat(environment.acceptsProfiles(Profiles.of("cfg05-weekend", "cfg05"))).isTrue();   // LUB

        // usługa korzysta z tego samego Environment
        assertThat(discounts.percent()).isEqualTo(10);
        assertThat(discounts.label()).isEqualTo("Sklep");
        assertThat(discounts.mode()).isEqualTo("ZWYKŁY");
        assertThat(discounts.apply(new BigDecimal("99.99"))).isEqualByComparingTo("89.99");
    }

    @Test
    @DisplayName("2. @ConditionalOnProperty: flaga przełącza kasę (matchIfMissing, havingValue); @ConditionalOnBooleanProperty")
    void featureFlag() {
        // pełna aplikacja: plik mówi new-checkout=false → stara kasa
        assertThat(checkout.version()).isEqualTo("stara");

        // ta sama aplikacja z flagą włączoną argumentem
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Cfg05Application.class)
                .profiles("cfg05").web(WebApplicationType.NONE).run("--cfg05.feature.new-checkout=true")) {
            assertThat(context.getBean(Checkout.class).version()).isEqualTo("nowa");
            assertThat(context.getBeansOfType(LegacyCheckout.class)).isEmpty();
        }

        // lekki kontekst: brak klucza, "true", "false"
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(NewCheckout.class, LegacyCheckout.class);
        runner.run(context -> assertThat(context.getBean(Checkout.class).version()).isEqualTo("stara"));   // matchIfMissing
        runner.withPropertyValues("cfg05.feature.new-checkout=true")
                .run(context -> assertThat(context.getBean(Checkout.class).version()).isEqualTo("nowa"));
        runner.withPropertyValues("cfg05.feature.new-checkout=false")
                .run(context -> assertThat(context.getBean(Checkout.class).version()).isEqualTo("stara"));

        // PUŁAPKA: „domyślna” kasa bez matchIfMissing — bez flagi nie ma żadnej kasy i wstrzyknięcie Checkout się nie uda
        new ApplicationContextRunner().withUserConfiguration(NewCheckout.class, StrictLegacyCheckout.class)
                .run(context -> assertThat(context).doesNotHaveBean(Checkout.class));
        new ApplicationContextRunner().withUserConfiguration(NewCheckout.class, StrictLegacyCheckout.class, NeedsCheckout.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
                });

        // bez havingValue: klucz musi istnieć i nie może być "false"
        ApplicationContextRunner loyalty = new ApplicationContextRunner().withUserConfiguration(LoyaltyProgram.class);
        loyalty.run(context -> assertThat(context).doesNotHaveBean(LoyaltyProgram.class));
        loyalty.withPropertyValues("cfg05.feature.loyalty=false")
                .run(context -> assertThat(context).doesNotHaveBean(LoyaltyProgram.class));
        loyalty.withPropertyValues("cfg05.feature.loyalty=true")
                .run(context -> assertThat(context).hasSingleBean(LoyaltyProgram.class));
        loyalty.withPropertyValues("cfg05.feature.loyalty=tak")
                .run(context -> assertThat(context).hasSingleBean(LoyaltyProgram.class));

        // nowszy zapis dla flag prawda/fałsz: @ConditionalOnBooleanProperty — wartość porównywana z "true"
        ApplicationContextRunner giftWrap = new ApplicationContextRunner().withUserConfiguration(GiftWrap.class);
        giftWrap.run(context -> assertThat(context).doesNotHaveBean(GiftWrap.class));          // brak klucza
        giftWrap.withPropertyValues("cfg05.feature.gift-wrap=true")
                .run(context -> assertThat(context).hasSingleBean(GiftWrap.class));
        giftWrap.withPropertyValues("cfg05.feature.gift-wrap=TRUE")
                .run(context -> assertThat(context).hasSingleBean(GiftWrap.class));            // wielkość liter bez znaczenia
        giftWrap.withPropertyValues("cfg05.feature.gift-wrap=tak")
                .run(context -> assertThat(context).doesNotHaveBean(GiftWrap.class));          // inaczej niż LoyaltyProgram
    }

    @Test
    @DisplayName("3. Zmienne środowiska CFG05_SHOP_DISCOUNTPERCENT (z dokumentacji) i CFG05_SHOP_DISCOUNT_PERCENT pasują do klucza discount-percent")
    void environmentVariableNaming() {
        // 1) wiązanie (tak jak przy @ConfigurationProperties): działają OBA zapisy nazwy zmiennej
        assertThat(bindPercent(osEnvironment(Map.of("CFG05_SHOP_DISCOUNTPERCENT", "25")))).isEqualTo(25);   // myślnik usunięty
        assertThat(bindPercent(osEnvironment(Map.of("CFG05_SHOP_DISCOUNT_PERCENT", "15")))).isEqualTo(15);  // myślnik → _

        // 2) wiązanie do rekordu (relaxed binding): pole discountPercent ↔ klucz discount-percent ↔ zmienna DISCOUNTPERCENT
        StandardEnvironment env = osEnvironment(Map.of("CFG05_SHOP_DISCOUNTPERCENT", "25", "CFG05_SHOP_LABEL", "Z ENV"));
        ShopSettings settings = new Binder(ConfigurationPropertySources.get(env))
                .bind("cfg05.shop", Bindable.of(ShopSettings.class)).get();
        assertThat(settings.discountPercent()).isEqualTo(25);
        assertThat(settings.label()).isEqualTo("Z ENV");

        // 3) Environment z regułami Boota (attach = podłącz; każda aplikacja Boota robi to sama): getProperty też tłumaczy
        ConfigurationPropertySources.attach(env);
        assertThat(env.getProperty("cfg05.shop.discount-percent")).isEqualTo("25");
        assertThat(env.getProperty("CFG05_SHOP_LABEL")).isEqualTo("Z ENV");   // dokładna nazwa zmiennej też działa

        // 4) PUŁAPKA: źródło o innej nazwie niż "systemEnvironment" — Boot nie stosuje reguł nazw zmiennych środowiska
        StandardEnvironment otherName = osEnvironment(Map.of());   // prawdziwe zmienne komputera usunięte
        otherName.getPropertySources().addFirst(new SystemEnvironmentPropertySource("sztuczne-env",
                Map.of("CFG05_SHOP_DISCOUNTPERCENT", "25")));
        assertThat(bindPercent(otherName)).isNull();   // zapis z dokumentacji NIE znaleziony — inaczej niż w prawdziwym systemie
        StandardEnvironment otherNameUnderscore = osEnvironment(Map.of());
        otherNameUnderscore.getPropertySources().addFirst(new SystemEnvironmentPropertySource("sztuczne-env",
                Map.of("CFG05_SHOP_DISCOUNT_PERCENT", "15")));
        assertThat(bindPercent(otherNameUnderscore)).isEqualTo(15);   // znaleziony prostszym dopasowaniem samego Springa

        // 5) lista w zmiennej środowiska: wartości po przecinku
        assertThat(new Binder(ConfigurationPropertySources.get(osEnvironment(Map.of("CFG05_SHOP_DAYS", "pon,wt"))))
                .bind("cfg05.shop.days", Bindable.listOf(String.class)).get()).containsExactly("pon", "wt");
    }

    @Test
    @DisplayName("4. Priorytet: plik < zmienna środowiska < właściwość JVM < argument (zmienne środowiska podstawione w teście)")
    void precedenceWithEnvironmentVariables() {
        Map<String, Object> os = Map.of("CFG05_SHOP_DISCOUNT_PERCENT", "15");
        // plik: 10; zmienna środowiska: 15 → wygrywa zmienna
        try (ConfigurableApplicationContext context = startWithFakeOsEnvironment(os)) {
            assertThat(context.getBean(DiscountService.class).percent()).isEqualTo(15);
        }
        // zapis z dokumentacji (myślnik usunięty) działa tak samo: plik 10, zmienna CFG05_SHOP_DISCOUNTPERCENT=25 → 25
        try (ConfigurableApplicationContext context =
                     startWithFakeOsEnvironment(Map.of("CFG05_SHOP_DISCOUNTPERCENT", "25"))) {
            assertThat(context.getBean(DiscountService.class).percent()).isEqualTo(25);
        }
        // argument: 20 → wygrywa argument
        try (ConfigurableApplicationContext context = startWithFakeOsEnvironment(os, "--cfg05.shop.discount-percent=20")) {
            assertThat(context.getBean(DiscountService.class).percent()).isEqualTo(20);
        }
        // właściwość JVM: 17 → wygrywa ze zmienną środowiska
        System.setProperty("cfg05.shop.discount-percent", "17");
        try (ConfigurableApplicationContext context = startWithFakeOsEnvironment(os)) {
            assertThat(context.getBean(DiscountService.class).percent()).isEqualTo(17);
        } finally {
            System.clearProperty("cfg05.shop.discount-percent");
        }
    }

    @Test
    @DisplayName("5. Profil sprawdzany w kodzie: acceptsProfiles widzi aktywny profil cfg05-weekend")
    void profilesInCode() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Cfg05Application.class)
                .profiles("cfg05", "cfg05-weekend").web(WebApplicationType.NONE).run()) {
            assertThat(context.getBean(DiscountService.class).mode()).isEqualTo("WEEKEND");
        }
    }
}

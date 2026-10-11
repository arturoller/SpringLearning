package s02_configuration.cfg05_environment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
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
 * podstawiamy sztuczne źródło {@link SystemEnvironmentPropertySource} albo własne środowisko, którego
 * "zmienne systemowe" są podane w teście.
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

    /** Środowisko z podstawionymi „zmiennymi systemowymi” — resztę (argumenty, JVM, pliki) Boot dokłada sam. */
    static class FakeOsEnvironment extends StandardEnvironment {
        private static final Map<String, Object> OS = Map.of("CFG05_SHOP_DISCOUNT_PERCENT", "15");

        @Override
        public Map<String, Object> getSystemEnvironment() {
            return OS;
        }
    }

    private static ConfigurableApplicationContext startWithFakeOsEnvironment(String... args) {
        return new SpringApplicationBuilder(Cfg05Application.class)
                .environment(new FakeOsEnvironment())
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
    @DisplayName("2. @ConditionalOnProperty: flaga przełącza kasę (matchIfMissing, havingValue)")
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
                .run(context -> assertThat(context).hasFailed());

        // bez havingValue: klucz musi istnieć i nie może być "false"
        ApplicationContextRunner loyalty = new ApplicationContextRunner().withUserConfiguration(LoyaltyProgram.class);
        loyalty.run(context -> assertThat(context).doesNotHaveBean(LoyaltyProgram.class));
        loyalty.withPropertyValues("cfg05.feature.loyalty=false")
                .run(context -> assertThat(context).doesNotHaveBean(LoyaltyProgram.class));
        loyalty.withPropertyValues("cfg05.feature.loyalty=true")
                .run(context -> assertThat(context).hasSingleBean(LoyaltyProgram.class));
        loyalty.withPropertyValues("cfg05.feature.loyalty=tak")
                .run(context -> assertThat(context).hasSingleBean(LoyaltyProgram.class));
    }

    @Test
    @DisplayName("3. Zmienna środowiska CFG05_SHOP_DISCOUNT_PERCENT odpowiada kluczowi cfg05.shop.discount-percent")
    void environmentVariableNaming() {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new SystemEnvironmentPropertySource("sztuczne-env", Map.of(
                "CFG05_SHOP_DISCOUNT_PERCENT", "15",
                "CFG05_SHOP_LABEL", "Z ENV")));

        // 1) samo Environment: kropki i myślniki z klucza zamienia na _ i szuka WIELKIMI literami
        assertThat(env.getProperty("cfg05.shop.discount-percent")).isEqualTo("15");
        assertThat(env.getProperty("cfg05.shop.label")).isEqualTo("Z ENV");
        // 2) odwrotnie nie działa: nazwa ze zmiennej nie jest kluczem z kropkami
        assertThat(env.getProperty("CFG05_SHOP_LABEL")).isEqualTo("Z ENV");   // tak, bo to dokładna nazwa zmiennej

        // 3) wiązanie do rekordu (relaxed binding): pole discountPercent ↔ klucz discount-percent ↔ zmienna DISCOUNT_PERCENT
        Binder binder = new Binder(ConfigurationPropertySources.get(env));
        ShopSettings fromUnderscore = binder.bind("cfg05.shop", Bindable.of(ShopSettings.class)).get();
        assertThat(fromUnderscore.discountPercent()).isEqualTo(15);
        assertThat(fromUnderscore.label()).isEqualTo("Z ENV");

        // 4) PUŁAPKA: nazwa BEZ podkreślnika w środku (DISCOUNTPERCENT) NIE pasuje do klucza discount-percent
        StandardEnvironment glued = new StandardEnvironment();
        glued.getPropertySources().addLast(new SystemEnvironmentPropertySource("sztuczne-env",
                Map.of("CFG05_SHOP_DISCOUNTPERCENT", "25")));
        assertThat(new Binder(ConfigurationPropertySources.get(glued))
                .bind("cfg05.shop.discount-percent", Integer.class).isBound()).isFalse();

        // 5) lista w zmiennej środowiska: wartości po przecinku
        StandardEnvironment list = new StandardEnvironment();
        list.getPropertySources().addLast(new SystemEnvironmentPropertySource("sztuczne-env",
                Map.of("CFG05_SHOP_DAYS", "pon,wt")));
        assertThat(new Binder(ConfigurationPropertySources.get(list))
                .bind("cfg05.shop.days", Bindable.listOf(String.class)).get()).containsExactly("pon", "wt");
    }

    @Test
    @DisplayName("4. Priorytet: plik < zmienna środowiska < właściwość JVM < argument (zmienne środowiska podstawione w teście)")
    void precedenceWithEnvironmentVariables() {
        // plik: 10; zmienna środowiska: 15 → wygrywa zmienna
        try (ConfigurableApplicationContext context = startWithFakeOsEnvironment()) {
            assertThat(context.getBean(DiscountService.class).percent()).isEqualTo(15);
        }
        // argument: 20 → wygrywa argument
        try (ConfigurableApplicationContext context = startWithFakeOsEnvironment("--cfg05.shop.discount-percent=20")) {
            assertThat(context.getBean(DiscountService.class).percent()).isEqualTo(20);
        }
        // właściwość JVM: 17 → wygrywa z zmienną środowiska
        System.setProperty("cfg05.shop.discount-percent", "17");
        try (ConfigurableApplicationContext context = startWithFakeOsEnvironment()) {
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

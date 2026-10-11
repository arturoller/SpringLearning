package s05_validation.val05_service_validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.validation.FieldError;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Val05ServiceValidation: {@code ApplicationContextRunner} — mały kontekst budowany w teście z podanymi
 * ustawieniami. Dlaczego nie {@code @SpringBootTest}? Chcemy zbadać kontekst, który CELOWO nie wystartuje; runner
 * zamiast wyjątku w teście oddaje błąd startu w {@code context.getStartupFailure()}.
 * Klasy konfiguracyjne poniżej NIE mają stereotypu (@Configuration), więc skaner Val05Application ich nie widzi.
 */
class Val05ServiceValidationPropertiesTest {

    @EnableConfigurationProperties(ShopProperties.class)
    static class ShopConfig {
    }

    @EnableConfigurationProperties({ShopPropertiesLoose.class, ShopPropertiesUnchecked.class})
    static class LooseConfig {
    }

    private final ApplicationContextRunner shopRunner = new ApplicationContextRunner().withUserConfiguration(ShopConfig.class);

    @Test
    @DisplayName("4. Poprawne ustawienia: kontekst startuje, a rekord dostaje wartości z konfiguracji")
    void validProperties() {
        shopRunner.withPropertyValues("val05.shop.name=Sklep", "val05.shop.max-items=10", "val05.shop.support.email=pomoc@sklep.pl")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ShopProperties properties = context.getBean(ShopProperties.class);
                    assertThat(properties.name()).isEqualTo("Sklep");
                    assertThat(properties.maxItems()).isEqualTo(10);
                    assertThat(properties.support().email()).isEqualTo("pomoc@sklep.pl");
                });
    }

    @Test
    @DisplayName("4. Błędne ustawienia: kontekst się nie uruchamia (BindValidationException z listą pól)")
    void invalidPropertiesStopStartup() {
        shopRunner.withPropertyValues("val05.shop.name=", "val05.shop.max-items=0", "val05.shop.support.email=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure).isInstanceOf(ConfigurationPropertiesBindException.class);
                    assertThat(failure.getCause()).isInstanceOf(BindException.class);
                    Throwable root = failure.getCause().getCause();
                    assertThat(root).isInstanceOf(BindValidationException.class);

                    List<String> fields = ((BindValidationException) root).getValidationErrors().getAllErrors().stream()
                            .map(error -> ((FieldError) error).getField()).sorted().toList();
                    assertThat(fields).containsExactly("maxItems", "name", "support.email");
                });
    }

    @Test
    @DisplayName("4. Pułapki: bez @Validated na klasie albo bez @Valid przy polu zagnieżdżonym błędne ustawienia przechodzą")
    void missingValidatedOrValid() {
        new ApplicationContextRunner().withUserConfiguration(LooseConfig.class)
                .withPropertyValues("val05.loose.name=Sklep", "val05.loose.support.email=",   // pusty e-mail w zagnieżdżonym polu
                        "val05.unchecked.name=")                                              // pusta nazwa bez @Validated
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ShopPropertiesLoose.class).support().email()).isEmpty();
                    assertThat(context.getBean(ShopPropertiesUnchecked.class).name()).isEmpty();
                });
    }

    @Test
    @DisplayName("4. @Valid pomija null: brakujące zagnieżdżone ustawienia przechodzą, jeśli nie ma @NotNull")
    void nestedNullIsSkipped() {
        shopRunner.withPropertyValues("val05.shop.name=Sklep", "val05.shop.max-items=10")   // brak val05.shop.support.*
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ShopProperties.class).support()).isNull();
                });
    }
}

package s05_validation.val05_service_validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.validation.FieldError;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Val05ServiceValidation. {@code ApplicationContextRunner} startuje malutki kontekst i pozwala sprawdzić,
 * że aplikacja NIE wystartuje ze złą konfiguracją ({@code hasFailed()}) — bez uruchamiania całej aplikacji.
 */
class Val05ConfigurationPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(ShopPropertiesConfig.class);

    private static List<String> failedFields(Throwable failure) {
        Throwable cause = failure;
        while (cause != null && !(cause instanceof BindValidationException)) {
            cause = cause.getCause();
        }
        assertThat(cause).as("w łańcuchu przyczyn jest BindValidationException").isNotNull();
        return ((BindValidationException) cause).getValidationErrors().getAllErrors().stream()
                .filter(FieldError.class::isInstance).map(e -> ((FieldError) e).getField()).sorted().toList();
    }

    @Test
    @DisplayName("Sekcja 4: poprawne ustawienia — kontekst startuje, a właściwości są powiązane")
    void validProperties() {
        runner.withPropertyValues("val05.shop.name=Sklep", "val05.shop.max-items-per-order=10",
                        "val05.shop.delivery.city=Łódź", "val05.shop.delivery.free-from-amount=200")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ShopProperties.class).name()).isEqualTo("Sklep");
                });
    }

    @Test
    @DisplayName("Sekcja 4: złe ustawienia — aplikacja nie startuje (ConfigurationPropertiesBindException → BindValidationException)")
    void invalidProperties() {
        runner.withPropertyValues("val05.shop.name=", "val05.shop.max-items-per-order=0", "val05.shop.delivery.city=Łódź")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(org.springframework.boot.context.properties.ConfigurationPropertiesBindException.class);
                    assertThat(failedFields(context.getStartupFailure())).containsExactly("maxItemsPerOrder", "name");
                });
    }

    @Test
    @DisplayName("Sekcja 4: brak właściwości też łamie ograniczenia (null dla @NotBlank, 0 dla int z @Min(1))")
    void missingProperties() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(failedFields(context.getStartupFailure())).containsExactly("maxItemsPerOrder", "name");
        });
    }

    @Test
    @DisplayName("Sekcja 4: ustawienia zagnieżdżone są sprawdzane dzięki @Valid na polu (ścieżka delivery.city)")
    void nestedProperties() {
        runner.withPropertyValues("val05.shop.name=Sklep", "val05.shop.max-items-per-order=10", "val05.shop.delivery.city=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(failedFields(context.getStartupFailure())).containsExactly("delivery.city");
                });
    }

    @Test
    @DisplayName("Sekcja 4: bez @Validated na klasie ustawień ograniczenia są ignorowane — kontekst startuje mimo błędu")
    void withoutValidatedNothingHappens() {
        new ApplicationContextRunner().withUserConfiguration(LaxShopProperties.Config.class)
                .withPropertyValues("val05.lax.name=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(LaxShopProperties.class).name()).isEmpty();
                });
    }
}

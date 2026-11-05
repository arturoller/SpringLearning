package s05_validation.val04_custom_validators;

import jakarta.validation.constraints.Size;

/** Formularze do sekcji 5 (komunikaty z parametrami i z Expression Language). */
public final class MessageForms {

    private MessageForms() {
    }

    /** {@code {min}} w komunikacie z pliku + polskie litery. */
    public record ShortName(@Size(min = 3, message = "{val04.name.short}") String name) {
    }

    /** {@code ${validatedValue}} w komunikacie z pliku. */
    public record EchoSku(@Sku String sku) {
    }

    /** Wywołanie METODY w wyrażeniu EL — w Hibernate Validator 9 domyślnie niedozwolone. */
    public record MethodCallInMessage(
            @Size(min = 5, message = "Podano ${validatedValue.length()} znaków, a minimum to {min}") String text) {
    }

    /** Dostęp do właściwości w EL ({@code ${validatedValue}}) działa, formatowanie liczb — {@code formatter}. */
    public record Formatted(
            @jakarta.validation.constraints.DecimalMin(value = "10",
                    message = "Cena ${formatter.format('%1$.2f', validatedValue)} jest za niska (min {value})")
            java.math.BigDecimal price) {
    }

    /** Surowy znak ${...} zamaskowany ukośnikiem — dosłowny tekst. */
    public record Escaped(@Size(min = 3, message = "Wpisz \\${wartość} (min {min})") String text) {
    }
}

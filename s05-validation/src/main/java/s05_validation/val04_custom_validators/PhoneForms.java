package s05_validation.val04_custom_validators;

/** Dwa formularze z ograniczeniami złożonymi: z jednym naruszeniem i z naruszeniami składników. */
public final class PhoneForms {

    private PhoneForms() {
    }

    public record Quiet(@PhoneNumber String phone) {
    }

    public record Loud(@PhoneNumberLoud String phone) {
    }
}

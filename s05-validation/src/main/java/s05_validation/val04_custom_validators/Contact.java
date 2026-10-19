package s05_validation.val04_custom_validators;

/** Contact = dane kontaktowe klienta (np. Jan Kowalski) — rekord do ćwiczenia 1 lekcji Val04CustomValidators. */
public record Contact(String name, @Phone String phone) {
}

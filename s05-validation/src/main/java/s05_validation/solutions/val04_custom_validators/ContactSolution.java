package s05_validation.solutions.val04_custom_validators;

/** Rekord z adnotacją z rozwiązania ćwiczenia 1 (odpowiednik Contact). */
public record ContactSolution(String name, @PhoneSolution String phone) {
}

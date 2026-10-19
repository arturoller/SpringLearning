package s22_extras.ext01_spring_ai;

import java.util.Locale;

/**
 * Wydźwięk opinii klienta (sentiment = wydźwięk, nastawienie). Odpowiedź modelu NIGDY nie jest przyjmowana „na słowo”:
 * {@link #fromModelAnswer(String)} przepuszcza tylko znane wartości, a wszystko inne zamienia na {@link #NIEZNANA}.
 */
public enum ReviewSentiment {
    POZYTYWNA, NEGATYWNA, NEUTRALNA, NIEZNANA;

    /**
     * Zamienia surową odpowiedź modelu na wartość z BIAŁEJ LISTY (whitelist = lista dozwolonych wartości).
     * Model może odpowiedzieć „Pozytywna.”, „  NEGATYWNA ” albo coś zupełnie innego (np. po prompt injection) —
     * tolerujemy wielkość liter, spacje i kropkę na końcu, resztę odrzucamy.
     */
    public static ReviewSentiment fromModelAnswer(String answer) {
        if (answer == null) {
            return NIEZNANA;
        }
        String normalized = answer.strip().toUpperCase(Locale.ROOT);
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return switch (normalized) {
            case "POZYTYWNA" -> POZYTYWNA;
            case "NEGATYWNA" -> NEGATYWNA;
            case "NEUTRALNA" -> NEUTRALNA;
            default -> NIEZNANA;
        };
    }
}

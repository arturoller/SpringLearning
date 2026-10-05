package s00_start.solutions.start06_exercises;

/** Rozwiązania wzorcowe ćwiczeń 1–2 lekcji Start06Exercises — zajrzyj dopiero po własnej próbie! */
public final class Start06ExercisesSolutions {

    private Start06ExercisesSolutions() {
    }

    static int solution1(int priceGrosze, int percent) {
        solution2(percent);
        return (priceGrosze * (100 - percent) + 50) / 100;
    }

    static void solution2(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Rabat musi być w zakresie 0..100: " + percent);
        }
    }
}

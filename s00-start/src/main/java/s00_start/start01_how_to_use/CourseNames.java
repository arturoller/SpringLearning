package s00_start.start01_how_to_use;

import java.time.LocalDate;
import java.util.List;

/**
 * CourseNames = nazwy w kursie. Zamienia nazwy między modułem (z myślnikami), pakietem (z podkreślnikami),
 * klasą lekcji i jej testem, a także wylicza daty powtórek. To zwykła klasa Javy — bez Springa — używana w lekcji
 * {@link Start01HowToUse} do pokazania konwencji nazewnictwa kursu.
 */
public final class CourseNames {

    /** Odstępy powtórek w dniach (spaced repetition — powtórki rozłożone w czasie). */
    static final List<Integer> REVIEW_GAPS_DAYS = List.of(1, 3, 7, 14, 30);

    private CourseNames() {
        // klasa narzędziowa (utility class) — same metody statyczne, nie tworzymy obiektów
    }

    /**
     * Nazwa modułu → nazwa pakietu działu: myślniki zamieniamy na podkreślniki.
     * Przykład: "s04-web-rest" → "s04_web_rest". Java nie pozwala na myślnik w nazwie pakietu.
     */
    static String sectionPackageOf(String moduleName) {
        return moduleName.replace('-', '_');
    }

    /** Numer działu z nazwy modułu albo pakietu: "s04-web-rest" → 4, "s16_cache_scheduling_async" → 16. */
    static int sectionNumberOf(String moduleOrPackage) {
        return Integer.parseInt(moduleOrPackage.substring(1, 3));
    }

    /** Klasa lekcji → klasa jej testu: "Start01HowToUse" → "Start01HowToUseTest". */
    static String testClassOf(String lessonClass) {
        return lessonClass + "Test";
    }

    /** Daty powtórek dla materiału nauczonego danego dnia: +1, +3, +7, +14 i +30 dni. */
    static List<LocalDate> reviewDates(LocalDate learnedOn) {
        return REVIEW_GAPS_DAYS.stream()
                .map(learnedOn::plusDays)
                .toList();
    }
}

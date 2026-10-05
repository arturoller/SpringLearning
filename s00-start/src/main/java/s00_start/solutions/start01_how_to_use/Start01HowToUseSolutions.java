package s00_start.solutions.start01_how_to_use;

import java.time.LocalDate;
import java.util.List;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Start01HowToUse — zajrzyj dopiero po własnej próbie!
 * Pakiet solutions jest RODZEŃSTWEM lekcji (nie jej podpakietem), żeby klasa startowa lekcji nigdy go nie skanowała.
 */
public final class Start01HowToUseSolutions {

    private static final List<Integer> REVIEW_GAPS_DAYS = List.of(1, 3, 7, 14, 30);

    private Start01HowToUseSolutions() {
    }

    static String solution1(String sectionPackage) {
        return sectionPackage.replace('_', '-');
    }

    static String solution2(String lessonClass) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lessonClass.length(); i++) {
            char c = lessonClass.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    static boolean solution3(LocalDate learnedOn, LocalDate day) {
        return REVIEW_GAPS_DAYS.stream().map(learnedOn::plusDays).toList().contains(day);
    }

    static String solution4(String moduleName, String lessonClass) {
        String sectionPackage = moduleName.replace('-', '_');
        return moduleName + "/src/test/java/" + sectionPackage + "/" + solution2(lessonClass) + "/" + lessonClass + "Test.java";
    }
}

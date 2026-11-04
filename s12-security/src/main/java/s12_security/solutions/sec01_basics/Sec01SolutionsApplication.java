package s12_security.solutions.sec01_basics;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Konfiguracja rozwiązań lekcji Sec01Basics. Leży w pakiecie solutions.sec01_basics (rodzeństwo lekcji), więc skanuje tylko
 * rozwiązania, a nie szkielet ćwiczeń ani konfigurację lekcji. Skanowanie znajduje {@link ExerciseSecurityConfig}
 * (tu już z {@code @Configuration}) i {@link ExerciseController}.
 */
@SpringBootApplication
public class Sec01SolutionsApplication {
}

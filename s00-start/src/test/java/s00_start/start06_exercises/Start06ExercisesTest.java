package s00_start.start06_exercises;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import s00_start.solutions.start06_exercises.SolutionDiscountController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Start06Exercises — sprawdzają sam MECHANIZM ćwiczeń (nie Twoje rozwiązania):
 * etykiety klas testowych i to, że klasa startowa lekcji nie widzi rozwiązań.
 */
@SpringBootTest
class Start06ExercisesTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("2. Klasa testów ćwiczeń ma etykietę „cwiczenie” — dlatego ./mvnw test ją pomija")
    void exerciseTestsAreTagged() {
        // getAnnotation = pobierz adnotację klasy (refleksja, t19_annotations_reflection/Annotations03ReflectionBasics)
        Tag tag = Start06ExercisesExercisesTest.class.getAnnotation(Tag.class);
        assertThat(tag.value()).isEqualTo("cwiczenie");
    }

    @Test
    @DisplayName("3. Klasa testów rozwiązań ma etykietę „wzorzec” — biegnie przy każdym ./mvnw test")
    void solutionTestsAreTagged() throws ClassNotFoundException {
        Class<?> solutionsTest = Class.forName("s00_start.solutions.start06_exercises.Start06ExercisesSolutionsTest");
        assertThat(solutionsTest.getAnnotation(Tag.class).value()).isEqualTo("wzorzec");
    }

    @Test
    @DisplayName("3. Aplikacja lekcji ma szkielet DiscountController, ale NIE widzi kontrolera z rozwiązań")
    void solutionsAreNotScanned() {
        assertThat(context.getBeanNamesForType(DiscountController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(SolutionDiscountController.class)).isEmpty();
    }
}

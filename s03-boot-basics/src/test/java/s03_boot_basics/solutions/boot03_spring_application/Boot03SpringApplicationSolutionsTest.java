package s03_boot_basics.solutions.boot03_spring_application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import s03_boot_basics.boot03_spring_application.Boot03Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Boot03SpringApplicationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Bez {@code @SpringBootTest}: aplikację uruchamiamy jawnie (Boot03Application), jak w lekcji.
 */
@Tag("wzorzec")
class Boot03SpringApplicationSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: wartość opcji --import.file albo „brak”")
    void solution1() {
        assertThat(Boot03SpringApplicationSolutions.solution1(new DefaultApplicationArguments("--import.file=ceny.csv", "pilne")))
                .isEqualTo("ceny.csv");
        assertThat(Boot03SpringApplicationSolutions.solution1(new DefaultApplicationArguments("pilne"))).isEqualTo("brak");
    }

    @Test
    @DisplayName("Wzorzec 2: właściwość domyślna i jej nadpisanie argumentem")
    void solution2() {
        assertThat(Boot03SpringApplicationSolutions.solution2()).isEqualTo("Sklep Ćwiczeniowy");
        assertThat(Boot03SpringApplicationSolutions.solution2("--boot03.shop-name=Inny")).isEqualTo("Inny");
    }

    @Test
    @DisplayName("Wzorzec 3: kod wyjścia 2 dla słowa „blad”, 0 bez niego")
    void solution3() {
        // każde uruchomienie = NOWY builder (drugie run() na tym samym builderze zwraca stary kontekst)
        assertThat(SpringApplication.exit(builder().run("blad"))).isEqualTo(2);
        assertThat(SpringApplication.exit(builder().run("ok"))).isZero();
    }

    private static SpringApplicationBuilder builder() {
        return new SpringApplicationBuilder(Boot03Application.class, Boot03ExerciseExitConfigSolution.class).web(WebApplicationType.NONE);
    }
}

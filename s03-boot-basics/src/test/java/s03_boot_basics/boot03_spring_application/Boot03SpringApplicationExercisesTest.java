package s03_boot_basics.boot03_spring_application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Boot03SpringApplication. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Boot03SpringApplicationExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: wartość opcji --import.file albo „brak”")
    void exercise1() {
        assertThat(Boot03SpringApplication.exercise1(new DefaultApplicationArguments("--import.file=ceny.csv", "pilne")))
                .isEqualTo("ceny.csv");
        assertThat(Boot03SpringApplication.exercise1(new DefaultApplicationArguments("pilne"))).isEqualTo("brak");
    }

    @Test
    @DisplayName("Ćwiczenie 2: właściwość domyślna i jej nadpisanie argumentem")
    void exercise2() {
        assertThat(Boot03SpringApplication.exercise2()).isEqualTo("Sklep Ćwiczeniowy");
        assertThat(Boot03SpringApplication.exercise2("--boot03.shop-name=Inny")).isEqualTo("Inny");
    }

    @Test
    @DisplayName("Ćwiczenie 3: kod wyjścia 2 dla słowa „blad”, 0 bez niego")
    void exercise3() {
        // każde uruchomienie = NOWY builder (drugie run() na tym samym builderze zwraca stary kontekst)
        assertThat(SpringApplication.exit(builder().run("blad"))).isEqualTo(2);
        assertThat(SpringApplication.exit(builder().run("ok"))).isZero();
    }

    private static SpringApplicationBuilder builder() {
        return new SpringApplicationBuilder(Boot03Application.class, Boot03ExerciseExitConfig.class).web(WebApplicationType.NONE);
    }
}

package s01_core_ioc.ioc07_lifecycle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Ioc07Lifecycle. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Zwykłe testy jednostkowe: nie potrzebują @SpringBootTest, wystarczą małe konteksty.
 */
@Tag("cwiczenie")
class Ioc07LifecycleExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: @PostConstruct wypełnia cache trzema produktami")
    void exercise1() {
        try (var context = new AnnotationConfigApplicationContext(Ioc07ExerciseCache.class)) {
            assertThat(context.getBean(Ioc07ExerciseCache.class).size()).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("Ćwiczenie 2: @PreDestroy czyści cache i ustawia closed po zamknięciu kontekstu")
    void exercise2() {
        var context = new AnnotationConfigApplicationContext(Ioc07ExerciseSessionCache.class);
        Ioc07ExerciseSessionCache cache = context.getBean(Ioc07ExerciseSessionCache.class);
        assertThat(cache.size()).isEqualTo(3);
        assertThat(cache.isClosed()).isFalse();

        context.close();

        assertThat(cache.size()).isZero();
        assertThat(cache.isClosed()).isTrue();
    }

    @Test
    @DisplayName("Ćwiczenie 3: dziennik zdarzeń po zamknięciu kontekstu")
    void exercise3() {
        assertThat(Ioc07Lifecycle.exercise3())
                .containsExactly("konstruktor", "setter", "@PostConstruct", "@PreDestroy");
    }
}

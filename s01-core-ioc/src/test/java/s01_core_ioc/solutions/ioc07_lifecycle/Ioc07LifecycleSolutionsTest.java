package s01_core_ioc.solutions.ioc07_lifecycle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Ioc07LifecycleExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Ioc07LifecycleSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: @PostConstruct wypełnia cache trzema produktami")
    void solution1() {
        try (var context = new AnnotationConfigApplicationContext(Ioc07ExerciseCacheSolution.class)) {
            assertThat(context.getBean(Ioc07ExerciseCacheSolution.class).size()).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("Wzorzec 2: @PreDestroy czyści cache i ustawia closed po zamknięciu kontekstu")
    void solution2() {
        var context = new AnnotationConfigApplicationContext(Ioc07ExerciseSessionCacheSolution.class);
        Ioc07ExerciseSessionCacheSolution cache = context.getBean(Ioc07ExerciseSessionCacheSolution.class);
        assertThat(cache.size()).isEqualTo(3);

        context.close();

        assertThat(cache.size()).isZero();
        assertThat(cache.isClosed()).isTrue();
    }

    @Test
    @DisplayName("Wzorzec 3: dziennik zdarzeń po zamknięciu kontekstu")
    void solution3() {
        assertThat(Ioc07LifecycleSolutions.solution3())
                .containsExactly("konstruktor", "setter", "@PostConstruct", "@PreDestroy");
    }
}

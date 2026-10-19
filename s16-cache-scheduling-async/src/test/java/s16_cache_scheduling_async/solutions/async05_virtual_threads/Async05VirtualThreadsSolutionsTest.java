package s16_cache_scheduling_async.solutions.async05_virtual_threads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Async05VirtualThreadsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Async05VirtualThreadsSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: zadanie na wątku wirtualnym widzi isVirtual() == true")
    void solution1() throws Exception {
        assertThat(Async05VirtualThreadsSolutions.solution1()).isTrue();
    }

    @Test
    @DisplayName("Wzorzec 2: kwadraty liczb liczone na osobnych wątkach wirtualnych")
    void solution2() throws Exception {
        assertThat(Async05VirtualThreadsSolutions.solution2(6)).containsExactly(0, 1, 4, 9, 16, 25);
        assertThat(Async05VirtualThreadsSolutions.solution2(0)).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 3: ograniczenie współbieżności semaforem")
    void solution3() throws Exception {
        AtomicInteger executed = new AtomicInteger();

        int max = Async05VirtualThreadsSolutions.solution3(200, 5, () -> {
            executed.incrementAndGet();
            Thread.yield();
        });

        assertThat(executed).hasValue(200);
        assertThat(max).isBetween(1, 5);
    }
}

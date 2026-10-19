package s16_cache_scheduling_async.async05_virtual_threads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Async05VirtualThreads. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Async05VirtualThreadsExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: zadanie na wątku wirtualnym widzi isVirtual() == true")
    void exercise1() throws Exception {
        assertThat(Async05VirtualThreads.exercise1()).isTrue();
        assertThat(Thread.currentThread().isVirtual()).as("sam test działa na wątku platformowym").isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 2: kwadraty liczb liczone na osobnych wątkach wirtualnych")
    void exercise2() throws Exception {
        assertThat(Async05VirtualThreads.exercise2(6)).containsExactly(0, 1, 4, 9, 16, 25);
        assertThat(Async05VirtualThreads.exercise2(0)).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: ograniczenie współbieżności semaforem")
    void exercise3() throws Exception {
        AtomicInteger executed = new AtomicInteger();

        int max = Async05VirtualThreads.exercise3(200, 5, () -> {
            executed.incrementAndGet();
            Thread.yield();
        });

        assertThat(executed).as("wszystkie zadania się wykonały").hasValue(200);
        assertThat(max).isBetween(1, 5);
    }
}

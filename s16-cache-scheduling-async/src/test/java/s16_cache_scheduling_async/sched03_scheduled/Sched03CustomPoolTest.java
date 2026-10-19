package s16_cache_scheduling_async.sched03_scheduled;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

/** Osobna klasa testowa, bo zmienia ustawienia kontekstu: spring.task.scheduling.*. */
@SpringBootTest(properties = {
        "spring.task.scheduling.pool.size=4",
        "spring.task.scheduling.thread-name-prefix=harmonogram-"
})
class Sched03CustomPoolTest {

    @Autowired
    ThreadPoolTaskScheduler taskScheduler;

    @Test
    @DisplayName("11. spring.task.scheduling.* zmienia rozmiar puli i prefiks nazw wątków")
    void poolSettings() {
        assertThat(taskScheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(4);
        assertThat(taskScheduler.getThreadNamePrefix()).isEqualTo("harmonogram-");
    }
}

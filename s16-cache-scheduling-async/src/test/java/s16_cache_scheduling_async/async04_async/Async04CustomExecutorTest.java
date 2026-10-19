package s16_cache_scheduling_async.async04_async;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Osobna klasa testowa: własny bean wykonawcy zmienia zachowanie autokonfiguracji Boota. */
@SpringBootTest
class Async04CustomExecutorTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class OwnExecutorConfig {
        @Bean
        ThreadPoolTaskExecutor reportExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setThreadNamePrefix("raport-");
            executor.setCorePoolSize(2);
            return executor;
        }
    }

    @Autowired
    ReportService reports;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("12. PUŁAPKA: własny bean wykonawcy wyłącza applicationTaskExecutor Boota (i jego spring.task.execution.*)")
    void ownExecutorReplacesBootsExecutor() throws Exception {
        assertThat(context.containsBean("applicationTaskExecutor")).isFalse();
        assertThat(reports.buildReport("x").get(10, TimeUnit.SECONDS)).startsWith("raport-");
    }
}

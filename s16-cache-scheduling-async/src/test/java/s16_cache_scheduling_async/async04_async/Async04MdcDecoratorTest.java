package s16_cache_scheduling_async.async04_async;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ta sama aplikacja co w Async04AsyncTest, ale z dodatkowym beanem TaskDecorator. @TestConfiguration zagnieżdżona w
 * klasie testu nie jest skanowana przez inne testy — działa tylko tutaj.
 */
@SpringBootTest
class Async04MdcDecoratorTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class DecoratorConfig {
        @Bean
        TaskDecorator mdcTaskDecorator() {
            return new MdcTaskDecorator();
        }
    }

    @Autowired
    ReportService reports;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("10. Bean TaskDecorator kopiuje MDC do wątku puli — Boot nakłada go na applicationTaskExecutor")
    void decoratorCopiesMdc() throws Exception {
        MDC.put("requestId", "REQ-123");

        assertThat(reports.readRequestId().get(10, TimeUnit.SECONDS)).isEqualTo("REQ-123");
    }

    @Test
    @DisplayName("11. Dekorator nie zostawia śladu w wątku puli — kolejne zadanie bez MDC widzi puste")
    void decoratorDoesNotLeak() throws Exception {
        MDC.put("requestId", "REQ-1");
        reports.readRequestId().get(10, TimeUnit.SECONDS);

        MDC.clear();
        assertThat(reports.readRequestId().get(10, TimeUnit.SECONDS)).isEqualTo("null");
    }
}

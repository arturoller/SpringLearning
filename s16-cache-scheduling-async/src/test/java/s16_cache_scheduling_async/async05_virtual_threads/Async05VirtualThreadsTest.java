package s16_cache_scheduling_async.async05_virtual_threads;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Async05VirtualThreads — wątki wirtualne WŁĄCZONE. Prawdziwy serwer na losowym porcie (RANDOM_PORT):
 * MockMvc wykonuje kontroler w wątku testu, więc nie pokazałby wątku Tomcata.
 * Ustawienia pool.* podajemy celowo — przy wątkach wirtualnych są ignorowane (sprawdzamy to w teście 3).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.threads.virtual.enabled=true",
        "spring.task.execution.pool.core-size=2",
        "spring.task.scheduling.pool.size=4"
})
class Async05VirtualThreadsTest {

    @LocalServerPort
    int port;

    @Autowired
    ApplicationContext context;

    RestClient client;

    @BeforeEach
    void createClient() {
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    @DisplayName("1. Kontroler wykonuje się na wątku WIRTUALNYM serwera Tomcat")
    void controllerRunsOnVirtualThread() {
        ThreadInfo info = client.get().uri("/api/thread").retrieve().body(ThreadInfo.class);

        assertThat(info.virtual()).isTrue();
        assertThat(info.name()).startsWith("tomcat-handler-");
        assertThat(context.containsBean("tomcatVirtualThreadsProtocolHandlerCustomizer")).isTrue();
    }

    @Test
    @DisplayName("2. Metoda @Async też jedzie na wątku wirtualnym: wykonawcą jest SimpleAsyncTaskExecutor")
    void asyncMethodRunsOnVirtualThread() {
        ThreadInfo info = client.get().uri("/api/async-thread").retrieve().body(ThreadInfo.class);

        assertThat(info.virtual()).isTrue();
        assertThat(info.name()).startsWith("task-");
        assertThat(context.getBean("applicationTaskExecutor")).isInstanceOf(SimpleAsyncTaskExecutor.class);
    }

    @Test
    @DisplayName("3. Harmonogram to SimpleAsyncTaskScheduler (nie pula), a pool.size jest ignorowane")
    void schedulerIsNotAPool() {
        assertThat(context.getBean("taskScheduler")).isInstanceOf(SimpleAsyncTaskScheduler.class);
        // spring.task.execution.pool.core-size=2 i spring.task.scheduling.pool.size=4 nie mają tu czego ustawiać:
        // nie ma puli (ThreadPoolTaskExecutor/ThreadPoolTaskScheduler), każde zadanie dostaje nowy wątek wirtualny.
        assertThat(context.getBeanNamesForType(org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor.class)).isEmpty();
        assertThat(context.getBeanNamesForType(org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler.class)).isEmpty();
    }
}

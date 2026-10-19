package s16_cache_scheduling_async.async05_virtual_threads;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/** Wątki wirtualne WYŁĄCZONE (domyślnie w Boot 4.1) — punkt odniesienia dla Async05VirtualThreadsTest. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.threads.virtual.enabled=false")
class Async05PlatformThreadsTest {

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
    @DisplayName("4. Bez wątków wirtualnych: kontroler jedzie na wątku platformowym Tomcata z jego puli")
    void controllerRunsOnPlatformThread() {
        ThreadInfo info = client.get().uri("/api/thread").retrieve().body(ThreadInfo.class);

        assertThat(info.virtual()).isFalse();
        assertThat(info.name()).startsWith("http-nio-");
        assertThat(context.containsBean("tomcatVirtualThreadsProtocolHandlerCustomizer")).isFalse();
    }

    @Test
    @DisplayName("5. Bez wątków wirtualnych: pule Boota (ThreadPoolTaskExecutor i ThreadPoolTaskScheduler)")
    void poolsAreUsed() {
        ThreadInfo info = client.get().uri("/api/async-thread").retrieve().body(ThreadInfo.class);

        assertThat(info.virtual()).isFalse();
        assertThat(info.name()).startsWith("task-");
        assertThat(context.getBean("applicationTaskExecutor")).isInstanceOf(ThreadPoolTaskExecutor.class);
        assertThat(context.getBean("taskScheduler")).isInstanceOf(ThreadPoolTaskScheduler.class);
    }
}

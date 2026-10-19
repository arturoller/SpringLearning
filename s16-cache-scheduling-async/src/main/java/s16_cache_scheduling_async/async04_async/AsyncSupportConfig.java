package s16_cache_scheduling_async.async04_async;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

/**
 * AsyncConfigurer (konfigurator metod asynchronicznych) pozwala podmienić wykonawcę i obsługę wyjątków dla @Async.
 * Tu podajemy TYLKO obsługę wyjątków; wykonawcy nie ruszamy (getAsyncExecutor zwraca domyślnie null), więc zostaje
 * wykonawca Boota (applicationTaskExecutor).
 */
@Configuration(proxyBeanMethods = false)
class AsyncSupportConfig implements AsyncConfigurer {

    private final CollectingExceptionHandler handler;

    AsyncSupportConfig(CollectingExceptionHandler handler) {
        this.handler = handler;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return handler;
    }
}

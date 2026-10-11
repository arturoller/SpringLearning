package s01_core_ioc.ioc07_lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * Małe konfiguracje i klasy do demonstracji w testach lekcji Ioc07Lifecycle. Żadna z nich nie ma
 * {@code @Configuration} ani {@code @Component} — dzięki temu skanowanie aplikacji lekcji ich nie widzi,
 * a test dodaje do kontekstu tylko tę, której potrzebuje.
 */
public final class Ioc07DemoConfigs {

    private Ioc07DemoConfigs() {
    }

    /** Zwykła klasa z publiczną metodą close() — NIE implementuje AutoCloseable. */
    public static class CloseResource {
        private final EventLog log;

        public CloseResource(EventLog log) {
            this.log = log;
        }

        public void close() {
            log.add("close");
        }
    }

    /** Zwykła klasa z publiczną metodą shutdown() (shutdown = wyłączenie). */
    public static class ShutdownResource {
        private final EventLog log;

        public ShutdownResource(EventLog log) {
            this.log = log;
        }

        public void shutdown() {
            log.add("shutdown");
        }
    }

    /** Bean prototype z @PostConstruct i @PreDestroy. */
    public static class TempBasket {
        private final EventLog log;

        public TempBasket(EventLog log) {
            this.log = log;
        }

        @PostConstruct
        void init() {
            log.add("prototype @PostConstruct");
        }

        @PreDestroy
        void cleanup() {
            log.add("prototype @PreDestroy");
        }
    }

    /** Bean, którego @PostConstruct rzuca wyjątek. */
    public static class FailingBean {
        @PostConstruct
        void init() {
            throw new IllegalStateException("magazyn niedostępny");
        }
    }

    /** initMethod i destroyMethod podane po nazwie metody. */
    public static class CallbacksConfig {
        @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
        AllCallbacksBean allCallbacksBean(EventLog log) {
            return new AllCallbacksBean(log);
        }
    }

    /** Bez destroyMethod: Spring sam wykrywa publiczne close() i shutdown() (destroyMethod wnioskowane). */
    public static class InferredDestroyConfig {
        @Bean
        CloseResource closeResource(EventLog log) {
            return new CloseResource(log);
        }

        @Bean
        ShutdownResource shutdownResource(EventLog log) {
            return new ShutdownResource(log);
        }
    }

    /** destroyMethod = "" wyłącza wnioskowanie — close() nie zostanie wywołane. */
    public static class DisabledDestroyConfig {
        @Bean(destroyMethod = "")
        CloseResource closeResource(EventLog log) {
            return new CloseResource(log);
        }
    }

    /** Bean prototype zdefiniowany metodą @Bean. */
    public static class PrototypeConfig {
        @Bean
        @Scope("prototype")
        TempBasket tempBasket(EventLog log) {
            return new TempBasket(log);
        }
    }
}

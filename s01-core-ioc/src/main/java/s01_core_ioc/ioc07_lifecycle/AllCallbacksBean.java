package s01_core_ioc.ioc07_lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean z WSZYSTKIMI sposobami zaczepienia się w cyklu życia naraz — po to, by test pokazał ich kolejność.
 * {@code InitializingBean} (inicjalizowany bean) ma metodę afterPropertiesSet (po ustawieniu właściwości),
 * {@code DisposableBean} (bean jednorazowy) ma metodę destroy (zniszcz). Do tego metody wskazane po nazwie w
 * {@code @Bean(initMethod = ..., destroyMethod = ...)} (init = start, destroy = zniszczenie).
 * Klasa nie ma {@code @Component} — rejestruje ją Ioc07DemoConfigs.CallbacksConfig w teście.
 */
public class AllCallbacksBean implements InitializingBean, DisposableBean {

    private final EventLog log;

    public AllCallbacksBean(EventLog log) {
        this.log = log;
    }

    @PostConstruct
    void postConstruct() {
        log.add("@PostConstruct");
    }

    @Override
    public void afterPropertiesSet() {
        log.add("afterPropertiesSet");
    }

    void customInit() {
        log.add("initMethod");
    }

    @PreDestroy
    void preDestroy() {
        log.add("@PreDestroy");
    }

    @Override
    public void destroy() {
        log.add("destroy");
    }

    void customDestroy() {
        log.add("destroyMethod");
    }
}

package s01_core_ioc.ioc07_lifecycle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Ioc07Lifecycle. Kolejność zdarzeń sprawdzamy na małych kontekstach tworzonych ręcznie
 * (AnnotationConfigApplicationContext), bo tylko wtedy sami decydujemy, kiedy wywołać {@code close()}.
 * {@code @SpringBootTest} służy do sprawdzenia, że prawdziwa aplikacja lekcji ma beany gotowe do pracy.
 */
@SpringBootTest
class Ioc07LifecycleTest {

    @Autowired
    WarehouseConnection warehouse;

    @Autowired
    ProductCache productCache;

    @Test
    @DisplayName("1. Kolejność: konstruktor, setter, @PostConstruct, a @PreDestroy dopiero po zamknięciu kontekstu")
    void orderOfEvents() {
        var context = new AnnotationConfigApplicationContext(EventLog.class, ProductCatalog.class,
                WarehouseConnection.class);
        EventLog log = context.getBean(EventLog.class);

        assertThat(log.events()).containsExactly("konstruktor", "setter", "@PostConstruct");

        context.close();

        assertThat(log.events()).containsExactly("konstruktor", "setter", "@PostConstruct", "@PreDestroy");
    }

    @Test
    @DisplayName("2. W konstruktorze zależność z settera jest jeszcze null, w @PostConstruct już jest")
    void constructorSeesNoSetterDependencies() {
        assertThat(warehouse.catalogWasNullInConstructor()).isTrue();
        assertThat(warehouse.catalogReadyInPostConstruct()).isTrue();
        assertThat(warehouse.isConnected()).isTrue();
    }

    @Test
    @DisplayName("3. Praca w konstruktorze na polu wstrzykniętym później kończy się NullPointerException")
    void brokenCacheFailsInConstructor() {
        assertThatThrownBy(() -> new AnnotationConfigApplicationContext(ProductCatalog.class,
                BrokenProductCache.class))
                .isInstanceOf(BeanCreationException.class)
                .hasRootCauseInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("3. To samo ładowanie w @PostConstruct działa: cache jest wypełniony po starcie")
    void cacheWarmedUpInPostConstruct() {
        assertThat(productCache.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("4. Kolejność wszystkich mechanizmów: @PostConstruct, afterPropertiesSet, initMethod; przy zamknięciu @PreDestroy, destroy, destroyMethod")
    void allCallbacksOrder() {
        var context = new AnnotationConfigApplicationContext(EventLog.class, Ioc07DemoConfigs.CallbacksConfig.class);
        EventLog log = context.getBean(EventLog.class);
        context.close();

        assertThat(log.events()).containsExactly("@PostConstruct", "afterPropertiesSet", "initMethod",
                "@PreDestroy", "destroy", "destroyMethod");
    }

    @Test
    @DisplayName("4. Metoda @Bean: Spring sam wywołuje publiczne close() i shutdown(), a destroyMethod=\"\" to wyłącza")
    void inferredDestroyMethod() {
        var inferred = new AnnotationConfigApplicationContext(EventLog.class, Ioc07DemoConfigs.InferredDestroyConfig.class);
        EventLog inferredLog = inferred.getBean(EventLog.class);
        assertThat(inferredLog.events()).isEmpty();
        inferred.close();
        assertThat(inferredLog.events()).containsExactlyInAnyOrder("close", "shutdown");

        var disabled = new AnnotationConfigApplicationContext(EventLog.class, Ioc07DemoConfigs.DisabledDestroyConfig.class);
        EventLog disabledLog = disabled.getBean(EventLog.class);
        disabled.close();
        assertThat(disabledLog.events()).isEmpty();
    }

    @Test
    @DisplayName("5. Prototype: @PostConstruct jest wywoływany przy każdym pobraniu, @PreDestroy nigdy")
    void prototypeIsNotDestroyed() {
        var context = new AnnotationConfigApplicationContext(EventLog.class, Ioc07DemoConfigs.PrototypeConfig.class);
        EventLog log = context.getBean(EventLog.class);
        context.getBean(Ioc07DemoConfigs.TempBasket.class);
        context.getBean(Ioc07DemoConfigs.TempBasket.class);
        context.close();

        assertThat(log.events()).containsExactly("prototype @PostConstruct", "prototype @PostConstruct");
    }

    @Test
    @DisplayName("6. Wyjątek w @PostConstruct przerywa start kontekstu (BeanCreationException)")
    void failingPostConstructStopsStartup() {
        assertThatThrownBy(() -> new AnnotationConfigApplicationContext(Ioc07DemoConfigs.FailingBean.class))
                .isInstanceOf(BeanCreationException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }
}

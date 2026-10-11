package s01_core_ioc.ioc01_container;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Ioc01Container. {@code @SpringBootTest} startuje kontekst Ioc01Application (wstrzykiwany do pola),
 * a sekcje 2–3 tworzą obiekty ręcznie albo przez „goły” kontener Springa.
 */
@SpringBootTest
class Ioc01ContainerTest {

    @Autowired
    ApplicationContext bootContext;

    @Test
    @DisplayName("1. PRZED: ręczny kalkulator liczy poprawnie, choć sam tworzy swój cennik")
    void manualCalculator() {
        assertThat(new ManualOrderCalculator().total(Map.of("Kawa", 2))).isEqualTo(5998);
    }

    @Test
    @DisplayName("2. PO: zależność podana przez konstruktor — bez żadnego Springa")
    void constructorInjectionByHand() {
        OrderCalculator calculator = new OrderCalculator(new PriceList());
        assertThat(calculator.total(Map.of("Kawa", 2, "Herbata", 1))).isEqualTo(7597);
    }

    @Test
    @DisplayName("3. Goły kontener Springa tworzy i łączy podane klasy")
    void plainSpringContainer() {
        try (var context = new AnnotationConfigApplicationContext(PriceList.class, OrderCalculator.class)) {
            OrderCalculator calculator = context.getBean(OrderCalculator.class);
            assertThat(calculator.total(Map.of("Czekolada", 3))).isEqualTo(2697);
        }
    }

    @Test
    @DisplayName("3. Bez zarejestrowanego PriceList kontener nie wystartuje (pytanie kontrolne 4)")
    void missingDependency() {
        assertThatThrownBy(() -> new AnnotationConfigApplicationContext(OrderCalculator.class))
                .isInstanceOf(UnsatisfiedDependencyException.class);
    }

    @Test
    @DisplayName("4. Kontekst Boota ma nasze beany i dużo więcej beanów niż goły kontener (autokonfiguracja)")
    void bootContextHasMoreBeans() {
        assertThat(bootContext.getBean(OrderCalculator.class)).isNotNull();
        try (var plain = new AnnotationConfigApplicationContext(PriceList.class, OrderCalculator.class)) {
            assertThat(bootContext.getBeanDefinitionCount()).isGreaterThan(plain.getBeanDefinitionCount());
        }
    }

    @Test
    @DisplayName("4. Klasa bez adnotacji nie jest beanem: NoSuchBeanDefinitionException (pytanie kontrolne 2)")
    void classWithoutAnnotationIsNotABean() {
        assertThatThrownBy(() -> bootContext.getBean(ManualOrderCalculator.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    @DisplayName("5. getBean po nazwie: domyślna nazwa to nazwa klasy z małej litery")
    void getBeanByName() {
        assertThat(bootContext.getBean("orderCalculator", OrderCalculator.class))
                .isSameAs(bootContext.getBean(OrderCalculator.class));
        assertThat(bootContext.getBeanNamesForType(PriceList.class)).containsExactly("priceList");
    }
}

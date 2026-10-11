package s01_core_ioc.solutions.ioc06_scopes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import s01_core_ioc.ioc06_scopes.Ioc06Application;
import s01_core_ioc.ioc06_scopes.ShoppingCart;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Ioc06ScopesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma własnej klasy startowej, więc wskazujemy ją wprost: {@code classes = Ioc06Application.class}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Ioc06Application.class)
class Ioc06ScopesSolutionsTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Wzorzec 1: trzy getBean(ShoppingCart) to trzy różne obiekty")
    void solution1() {
        assertThat(Ioc06ScopesSolutions.solution1(context)).isEqualTo(3);
    }

    @Test
    @DisplayName("Wzorzec 2: nowy koszyk dla każdego zamówienia")
    void solution2() {
        List<List<String>> orders = List.of(List.of("Kawa", "Herbata"), List.of("Czekolada"), List.of());
        assertThat(Ioc06ScopesSolutions.solution2(context.getBeanProvider(ShoppingCart.class), orders))
                .containsExactly(2, 1, 0);
    }

    @Test
    @DisplayName("Wzorzec 3: @Scope(prototype) na metodzie @Bean daje różne obiekty")
    void solution3() {
        new ApplicationContextRunner()
                .withUserConfiguration(Ioc06Exercise3ConfigSolution.class)
                .run(ctx -> assertThat(ctx.getBean(ShoppingCart.class)).isNotSameAs(ctx.getBean(ShoppingCart.class)));
    }
}

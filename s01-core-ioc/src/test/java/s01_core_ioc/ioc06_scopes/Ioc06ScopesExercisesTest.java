package s01_core_ioc.ioc06_scopes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Ioc06Scopes. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
class Ioc06ScopesExercisesTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Ćwiczenie 1: trzy getBean(ShoppingCart) to trzy różne obiekty")
    void exercise1() {
        assertThat(Ioc06Scopes.exercise1(context)).isEqualTo(3);
    }

    @Test
    @DisplayName("Ćwiczenie 2: nowy koszyk dla każdego zamówienia")
    void exercise2() {
        List<List<String>> orders = List.of(List.of("Kawa", "Herbata"), List.of("Czekolada"), List.of());
        assertThat(Ioc06Scopes.exercise2(context.getBeanProvider(ShoppingCart.class), orders))
                .containsExactly(2, 1, 0);
    }

    @Test
    @DisplayName("Ćwiczenie 3: bean z Ioc06Exercise3Config ma zasięg prototype")
    void exercise3() {
        new ApplicationContextRunner()
                .withUserConfiguration(Ioc06Exercise3Config.class)
                .run(ctx -> assertThat(ctx.getBean(ShoppingCart.class)).isNotSameAs(ctx.getBean(ShoppingCart.class)));
    }
}

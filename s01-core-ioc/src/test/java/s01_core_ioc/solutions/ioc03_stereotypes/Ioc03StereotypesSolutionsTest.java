package s01_core_ioc.solutions.ioc03_stereotypes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import s01_core_ioc.ioc03_stereotypes.CatalogService;
import s01_core_ioc.ioc03_stereotypes.Ioc03Application;
import s01_core_ioc.ioc03_stereotypes.LoyaltyDiscount;
import s01_core_ioc.ioc03_stereotypes.NotAComponent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Ioc03StereotypesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Kontekst: {@code classes = Ioc03Application.class} (pakiet solutions nie ma własnej klasy startowej).
 */
@Tag("wzorzec")
@SpringBootTest(classes = Ioc03Application.class)
class Ioc03StereotypesSolutionsTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Wzorzec 1: domyślna nazwa beana")
    void solution1() {
        assertThat(Ioc03StereotypesSolutions.solution1("CatalogService")).isEqualTo("catalogService");
        assertThat(Ioc03StereotypesSolutions.solution1("URLShortener")).isEqualTo("URLShortener");
        assertThat(Ioc03StereotypesSolutions.solution1("X")).isEqualTo("x");
        assertThat(Ioc03StereotypesSolutions.solution1("Ab")).isEqualTo("ab");
        assertThat(Ioc03StereotypesSolutions.solution1("")).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 2: czy klasa jest komponentem")
    void solution2() {
        assertThat(Ioc03StereotypesSolutions.solution2(CatalogService.class)).isTrue();
        assertThat(Ioc03StereotypesSolutions.solution2(LoyaltyDiscount.class)).isTrue();
        assertThat(Ioc03StereotypesSolutions.solution2(NotAComponent.class)).isFalse();
        assertThat(Ioc03StereotypesSolutions.solution2(String.class)).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 3: posortowane nazwy beanów z @Service")
    void solution3() {
        assertThat(Ioc03StereotypesSolutions.solution3(context)).containsExactly("catalogService", "shippingService");
    }
}

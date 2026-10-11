package s01_core_ioc.ioc03_stereotypes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Ioc03Stereotypes. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
class Ioc03StereotypesExercisesTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Ćwiczenie 1: domyślna nazwa beana")
    void exercise1() {
        assertThat(Ioc03Stereotypes.exercise1("CatalogService")).isEqualTo("catalogService");
        assertThat(Ioc03Stereotypes.exercise1("URLShortener")).isEqualTo("URLShortener");
        assertThat(Ioc03Stereotypes.exercise1("X")).isEqualTo("x");
        assertThat(Ioc03Stereotypes.exercise1("Ab")).isEqualTo("ab");
        assertThat(Ioc03Stereotypes.exercise1("")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 2: czy klasa jest komponentem")
    void exercise2() {
        assertThat(Ioc03Stereotypes.exercise2(CatalogService.class)).isTrue();
        assertThat(Ioc03Stereotypes.exercise2(LoyaltyDiscount.class)).isTrue();
        assertThat(Ioc03Stereotypes.exercise2(NotAComponent.class)).isFalse();
        assertThat(Ioc03Stereotypes.exercise2(String.class)).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 3: posortowane nazwy beanów z @Service")
    void exercise3() {
        assertThat(Ioc03Stereotypes.exercise3(context)).containsExactly("catalogService", "shippingService");
    }
}

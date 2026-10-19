package s20_modulith.mod01_modules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import s20_modulith.mod01_broken.cycle.CycleApplication;
import s20_modulith.mod01_broken.internal_access.InternalAccessApplication;
import s20_modulith.mod01_modules.customers.CustomerView;
import s20_modulith.mod01_modules.orders.internal.OrderNumbers;
import s20_modulith.mod01_modules.products.ProductCatalog;
import s20_modulith.mod01_modules.products.internal.ProductStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Mod01Modules. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Mod01ModulesExercisesTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(Mod01Application.class);

    @Test
    @DisplayName("Ćwiczenie 1: nazwy modułów alfabetycznie")
    void exercise1() {
        assertThat(Mod01Modules.exercise1(MODULES)).containsExactly("customers", "orders", "products");
        assertThat(Mod01Modules.exercise1(Mod01ModulesTest.broken(CycleApplication.class)))
                .containsExactly("orders", "products");
    }

    @Test
    @DisplayName("Ćwiczenie 2: poprawność architektury bez wyjątku")
    void exercise2() {
        assertThat(Mod01Modules.exercise2(MODULES)).isTrue();
        assertThat(Mod01Modules.exercise2(Mod01ModulesTest.broken(InternalAccessApplication.class))).isFalse();
        assertThat(Mod01Modules.exercise2(Mod01ModulesTest.broken(CycleApplication.class))).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 3: czy typ należy do API swojego modułu")
    void exercise3() {
        assertThat(Mod01Modules.exercise3(MODULES, ProductCatalog.class)).isTrue();
        assertThat(Mod01Modules.exercise3(MODULES, CustomerView.class)).isTrue();
        assertThat(Mod01Modules.exercise3(MODULES, ProductStore.class)).isFalse();
        assertThat(Mod01Modules.exercise3(MODULES, OrderNumbers.class)).isFalse();
        assertThat(Mod01Modules.exercise3(MODULES, String.class)).isFalse();      // spoza modułów
    }

    @Test
    @DisplayName("Ćwiczenie 4: bezpośrednie zależności modułu")
    void exercise4() {
        assertThat(Mod01Modules.exercise4(MODULES, "orders")).containsExactly("customers", "products");
        assertThat(Mod01Modules.exercise4(MODULES, "products")).isEmpty();
        assertThatThrownBy(() -> Mod01Modules.exercise4(MODULES, "payments"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nie ma modułu: payments");
    }
}

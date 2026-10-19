package s20_modulith.solutions.mod01_modules;

import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import s20_modulith.mod01_broken.cycle.CycleApplication;
import s20_modulith.mod01_broken.internal_access.InternalAccessApplication;
import s20_modulith.mod01_modules.Mod01Application;
import s20_modulith.mod01_modules.customers.CustomerView;
import s20_modulith.mod01_modules.orders.internal.OrderNumbers;
import s20_modulith.mod01_modules.products.ProductCatalog;
import s20_modulith.mod01_modules.products.internal.ProductStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Mod01ModulesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Mod01ModulesSolutionsTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(Mod01Application.class);

    private static ApplicationModules broken(Class<?> application) {
        return ApplicationModules.of(application, new ImportOption.OnlyIncludeTests());
    }

    @Test
    @DisplayName("Wzorzec 1: nazwy modułów alfabetycznie")
    void solution1() {
        assertThat(Mod01ModulesSolutions.solution1(MODULES)).containsExactly("customers", "orders", "products");
        assertThat(Mod01ModulesSolutions.solution1(broken(CycleApplication.class))).containsExactly("orders", "products");
    }

    @Test
    @DisplayName("Wzorzec 2: poprawność architektury bez wyjątku")
    void solution2() {
        assertThat(Mod01ModulesSolutions.solution2(MODULES)).isTrue();
        assertThat(Mod01ModulesSolutions.solution2(broken(InternalAccessApplication.class))).isFalse();
        assertThat(Mod01ModulesSolutions.solution2(broken(CycleApplication.class))).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 3: czy typ należy do API swojego modułu")
    void solution3() {
        assertThat(Mod01ModulesSolutions.solution3(MODULES, ProductCatalog.class)).isTrue();
        assertThat(Mod01ModulesSolutions.solution3(MODULES, CustomerView.class)).isTrue();
        assertThat(Mod01ModulesSolutions.solution3(MODULES, ProductStore.class)).isFalse();
        assertThat(Mod01ModulesSolutions.solution3(MODULES, OrderNumbers.class)).isFalse();
        assertThat(Mod01ModulesSolutions.solution3(MODULES, String.class)).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 4: bezpośrednie zależności modułu")
    void solution4() {
        assertThat(Mod01ModulesSolutions.solution4(MODULES, "orders")).containsExactly("customers", "products");
        assertThat(Mod01ModulesSolutions.solution4(MODULES, "products")).isEmpty();
        assertThatThrownBy(() -> Mod01ModulesSolutions.solution4(MODULES, "payments"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nie ma modułu: payments");
    }
}

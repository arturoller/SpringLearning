package s20_modulith.solutions.mod03_documentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import s20_modulith.mod03_documentation.Mod03Application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Mod03DocumentationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Mod03DocumentationSolutionsTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(Mod03Application.class);

    @Test
    @DisplayName("Wzorzec 1: sam diagram wszystkich modułów")
    void solution1() throws IOException {
        Mod03DocumentationSolutions.solution1(MODULES, "target/mod03-solutions/ex1");

        Path folder = Path.of("target", "mod03-solutions", "ex1");
        assertThat(Files.readString(folder.resolve("components.puml"))).contains("\"uses\"", "\"listens to\"");
        assertThat(folder.resolve("module-orders.adoc")).doesNotExist();
    }

    @Test
    @DisplayName("Wzorzec 2: nazwane interfejsy modułu")
    void solution2() {
        assertThat(Mod03DocumentationSolutions.solution2(MODULES, "orders")).containsExactly("events");
        assertThat(Mod03DocumentationSolutions.solution2(MODULES, "products")).isEmpty();
        assertThatThrownBy(() -> Mod03DocumentationSolutions.solution2(MODULES, "payments"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Nie ma modułu: payments");
    }

    @Test
    @DisplayName("Wzorzec 3: kanwy z wnętrzem modułów")
    void solution3() throws IOException {
        Mod03DocumentationSolutions.solution3(MODULES, "target/mod03-solutions/ex3");

        Path folder = Path.of("target", "mod03-solutions", "ex3");
        assertThat(Files.readString(folder.resolve("module-orders.adoc"))).contains("OrderStore");
        assertThat(Files.readString(folder.resolve("module-inventory.adoc"))).contains("InventoryEventHandler");
    }

    @Test
    @DisplayName("Wzorzec 4: mapa zależności modułów")
    void solution4() {
        assertThat(Mod03DocumentationSolutions.solution4(MODULES)).containsExactly(
                Map.entry("inventory", List.of("orders")),
                Map.entry("orders", List.of("products")),
                Map.entry("products", List.of()));
    }
}

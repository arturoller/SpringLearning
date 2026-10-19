package s20_modulith.mod03_documentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Mod03Documentation. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Mod03DocumentationExercisesTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(Mod03Application.class);

    @Test
    @DisplayName("Ćwiczenie 1: sam diagram wszystkich modułów")
    void exercise1() throws IOException {
        Mod03Documentation.exercise1(MODULES, "target/mod03-exercises/ex1");

        Path folder = Path.of("target", "mod03-exercises", "ex1");
        assertThat(Files.readString(folder.resolve("components.puml"))).contains("\"uses\"", "\"listens to\"");
        assertThat(folder.resolve("module-orders.adoc")).doesNotExist();              // tylko diagram całości
    }

    @Test
    @DisplayName("Ćwiczenie 2: nazwane interfejsy modułu")
    void exercise2() {
        assertThat(Mod03Documentation.exercise2(MODULES, "orders")).containsExactly("events");
        assertThat(Mod03Documentation.exercise2(MODULES, "products")).isEmpty();
        assertThatThrownBy(() -> Mod03Documentation.exercise2(MODULES, "payments"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Nie ma modułu: payments");
    }

    @Test
    @DisplayName("Ćwiczenie 3: kanwy z wnętrzem modułów")
    void exercise3() throws IOException {
        Mod03Documentation.exercise3(MODULES, "target/mod03-exercises/ex3");

        Path folder = Path.of("target", "mod03-exercises", "ex3");
        assertThat(Files.readString(folder.resolve("module-orders.adoc"))).contains("OrderStore");
        assertThat(Files.readString(folder.resolve("module-inventory.adoc"))).contains("InventoryEventHandler");
    }

    @Test
    @DisplayName("Ćwiczenie 4: mapa zależności modułów")
    void exercise4() {
        assertThat(Mod03Documentation.exercise4(MODULES)).containsExactly(
                Map.entry("inventory", List.of("orders")),
                Map.entry("orders", List.of("products")),
                Map.entry("products", List.of()));
    }
}

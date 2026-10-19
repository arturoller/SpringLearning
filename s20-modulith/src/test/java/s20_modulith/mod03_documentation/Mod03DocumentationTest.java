package s20_modulith.mod03_documentation;

import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.NamedInterface;
import org.springframework.modulith.docs.Documenter;
import org.springframework.modulith.docs.Documenter.CanvasOptions;
import org.springframework.modulith.docs.Documenter.DiagramOptions;
import org.springframework.modulith.docs.Documenter.Options;
import s20_modulith.mod03_broken.named_interface.NamedInterfaceApplication;
import s20_modulith.mod03_documentation.orders.OrderManagement;
import s20_modulith.mod03_documentation.orders.events.OrderPlaced;
import s20_modulith.mod03_documentation.orders.internal.OrderStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Testy lekcji Mod03Documentation — „wynik” sekcji 1–3.
 *
 * <p>Wybór rodzaju testu: zwykły test jednostkowy — Documenter i analiza modułów czytają skompilowane klasy, kontekst
 * Springa nie jest potrzebny. Ścieżki są względne: Maven uruchamia testy w katalogu modułu (s20-modulith), więc
 * „target/…” to s20-modulith/target/…. Każdy test pisze do własnego folderu, bo Documenter czyści folder wynikowy.</p>
 */
class Mod03DocumentationTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(Mod03Application.class);

    @Test
    @DisplayName("1. writeDocumentation(): diagramy C4 i kanwy w target/spring-modulith-docs, strzałki uses i listens to")
    void section1() throws IOException {
        new Documenter(MODULES).writeDocumentation();

        Path docs = Path.of("target", "spring-modulith-docs");
        assertThat(docs.resolve("components.puml")).exists();
        assertThat(docs.resolve("all-docs.adoc")).exists();
        for (String module : new String[]{"orders", "products", "inventory"}) {
            assertThat(docs.resolve("module-" + module + ".puml")).exists();
            assertThat(docs.resolve("module-" + module + ".adoc")).exists();
        }
        assertThat(Path.of("target", "classes", "META-INF", "spring-modulith", "application-modules.json")).exists();

        assertThat(Files.readString(docs.resolve("components.puml")))
                .contains("@startuml", "!include <C4/C4_Component>")
                .contains("\"Produkty\"", "\"Zamówienia\"", "\"Magazyn\"")
                .contains("Rel(Mod03Application.Mod03Application.Zamówienia, Mod03Application.Mod03Application.Produkty, \"uses\"")
                .contains("Rel(Mod03Application.Mod03Application.Magazyn, Mod03Application.Mod03Application.Zamówienia, \"listens to\"");
        // diagram jednego modułu: moduł i to, od czego zależy — bez modułów, które zależą od niego
        assertThat(Files.readString(docs.resolve("module-orders.puml")))
                .contains("\"Zamówienia\"", "\"Produkty\"").doesNotContain("\"Magazyn\"");
        assertThat(Files.readString(docs.resolve("all-docs.adoc")))
                .contains("plantuml::components.puml[,,format=svg]", "include::module-orders.adoc[]");

        // styl UML zamiast C4
        new Documenter(MODULES, Options.defaults().withOutputFolder("target/mod03-uml"))
                .writeModulesAsPlantUml(DiagramOptions.defaults().withStyle(DiagramOptions.DiagramStyle.UML));
        assertThat(Files.readString(Path.of("target", "mod03-uml", "components.puml")))
                .contains("componentStyle uml1", ": \"uses\"").doesNotContain("!include <C4");
    }

    @Test
    @DisplayName("1b. PUŁAPKA: Documenter czyści folder wynikowy przed zapisem; withoutClean() zostawia stare pliki")
    void outputFolderIsWiped() throws IOException {
        Path folder = Path.of("target", "mod03-clean-demo");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("moj-plik.txt"), "ważne notatki");

        new Documenter(MODULES, Options.defaults().withOutputFolder(folder.toString())).writeModulesAsPlantUml();
        assertThat(folder.resolve("components.puml")).exists();
        assertThat(folder.resolve("moj-plik.txt")).doesNotExist();                    // skasowany!

        Files.writeString(folder.resolve("moj-plik.txt"), "ważne notatki");
        new Documenter(MODULES, Options.defaults().withOutputFolder(folder.toString()).withoutClean())
                .writeModulesAsPlantUml();
        assertThat(folder.resolve("moj-plik.txt")).exists();
    }

    @Test
    @DisplayName("2. Kanwa modułu: beany API, Bean references, Events listened to; wnętrze dopiero z revealInternals()")
    void section2() throws IOException {
        new Documenter(MODULES, Options.defaults().withOutputFolder("target/mod03-canvases")).writeModuleCanvases();
        Path canvases = Path.of("target", "mod03-canvases");

        assertThat(Files.readString(canvases.resolve("module-inventory.adoc")))
                .contains("|Base package", "|`s20_modulith.mod03_documentation.inventory`")
                .contains("* `s.m.i.InventoryManagement`")
                .contains("|Events listened to", "|* `s.m.o.events.OrderPlaced` (async)")
                .doesNotContain("InventoryEventHandler");                              // internal — ukryty
        assertThat(Files.readString(canvases.resolve("module-orders.adoc")))
                .contains("|Bean references", "|* `s.m.p.ProductCatalog` (in Produkty)")
                .doesNotContain("Published events", "OrderStore");

        // publikowane zdarzenia: tylko typy jMolecules — zwykły rekord OrderPlaced się nie liczy
        assertThat(MODULES.getModuleByName("orders").orElseThrow().getPublishedEvents()).isEmpty();

        new Documenter(MODULES, Options.defaults().withOutputFolder("target/mod03-internals"))
                .writeModuleCanvases(CanvasOptions.defaults().revealInternals());
        assertThat(Files.readString(Path.of("target", "mod03-internals", "module-orders.adoc")))
                .contains("_Others_", "OrderStore");
    }

    @Test
    @DisplayName("3. @NamedInterface: podpakiet events jest drugim interfejsem modułu; magazyn może używać tylko jego")
    void section3() {
        ApplicationModule orders = MODULES.getModuleByName("orders").orElseThrow();
        assertThat(orders.getNamedInterfaces().stream().map(NamedInterface::getName))
                .containsExactlyInAnyOrder("<<UNNAMED>>", "events");
        assertThat(orders.isExposed(OrderManagement.class)).isTrue();                  // interfejs nienazwany
        assertThat(orders.isExposed(OrderPlaced.class)).isTrue();                      // interfejs „events”
        assertThat(orders.isExposed(OrderStore.class)).isFalse();                      // internal
        assertThat(MODULES.toString()).contains(
                "+ NamedInterface: name=<<UNNAMED>>, types=[ s.m.o.OrderManagement ]",
                "+ NamedInterface: name=events, types=[ s.m.o.e.OrderPlaced ]");

        assertThatCode(MODULES::verify).doesNotThrowAnyException();

        ApplicationModules broken = ApplicationModules.of(NamedInterfaceApplication.class, new ImportOption.OnlyIncludeTests());
        assertThat(broken.detectViolations().getMessages()).containsExactly(
                "Module 'inventory' depends on module 'orders' via "
                        + "s20_modulith.mod03_broken.named_interface.inventory.InventoryEventHandler -> "
                        + "s20_modulith.mod03_broken.named_interface.orders.OrderManagement. Allowed targets: orders :: events.");
    }
}

package s20_modulith.mod01_modules;

import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;
import s20_modulith.mod01_broken.cycle.CycleApplication;
import s20_modulith.mod01_broken.field_injection.FieldInjectionApplication;
import s20_modulith.mod01_broken.internal_access.InternalAccessApplication;
import s20_modulith.mod01_broken.not_allowed.NotAllowedApplication;
import s20_modulith.mod01_modules.customers.CustomerView;
import s20_modulith.mod01_modules.orders.OrderService;
import s20_modulith.mod01_modules.orders.internal.OrderNumbers;
import s20_modulith.mod01_modules.products.ProductCatalog;
import s20_modulith.mod01_modules.products.ProductView;
import s20_modulith.mod01_modules.products.internal.ProductStore;
import s20_modulith.solutions.mod01_modules.Mod01ModulesSolutions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Mod01Modules — „wynik” sekcji 1–6.
 *
 * <p>Wybór rodzaju testu: analiza modułów ({@code ApplicationModules}) czyta skompilowane klasy i NIE potrzebuje
 * kontekstu Springa — to zwykły test jednostkowy (szybki). Kontekst startujemy tylko tam, gdzie trzeba pokazać,
 * że zła aplikacja mimo wszystko działa (sekcje 3 i 4). Złe aplikacje leżą w {@code s20_modulith.mod01_broken}
 * (src/test/java), dlatego analizujemy je z {@code ImportOption.OnlyIncludeTests}.</p>
 */
class Mod01ModulesTest {

    /** Moduły dobrej aplikacji lekcji (domyślna analiza: tylko klasy z src/main, bez testów). */
    private static final ApplicationModules MODULES = ApplicationModules.of(Mod01Application.class);

    /**
     * Moduły celowo złej aplikacji z src/test/java. Każde wywołanie tworzy NOWĄ opcję importu, więc Modulith liczy
     * świeży obiekt ApplicationModules (pamięć podręczna rozróżnia opcje) — testy nie wpływają na siebie.
     */
    static ApplicationModules broken(Class<?> application) {
        return ApplicationModules.of(application, new ImportOption.OnlyIncludeTests());
    }

    private static List<String> names(ApplicationModules modules) {
        return modules.stream().map(module -> module.getIdentifier().toString()).sorted().toList();
    }

    @Test
    @DisplayName("1. Moduł = bezpośredni podpakiet klasy startowej; internal i solutions nie są modułami")
    void section1() {
        assertThat(names(MODULES)).containsExactly("customers", "orders", "products");

        ApplicationModule orders = MODULES.getModuleByName("orders").orElseThrow();
        assertThat(orders.getDisplayName()).isEqualTo("Zamówienia");                 // z @ApplicationModule
        assertThat(orders.getBasePackage().getName()).isEqualTo("s20_modulith.mod01_modules.orders");
        // podpakiet orders.internal należy do modułu orders
        assertThat(MODULES.getModuleByType(OrderNumbers.class)).contains(orders);

        // klasa startowa leży w pakiecie głównym: specjalny moduł „root”, nie zwykły moduł
        ApplicationModule root = MODULES.getModuleByType(Mod01Application.class).orElseThrow();
        assertThat(root.isRootModule()).isTrue();
        assertThat(root.getIdentifier().toString()).isEqualTo("root:s20_modulith.mod01_modules");

        // rozwiązania ćwiczeń są poza pakietem głównym — Modulith ich nie widzi
        assertThat(MODULES.contains(Mod01ModulesSolutions.class)).isFalse();
        assertThat(MODULES.getModuleByType(Mod01ModulesSolutions.class)).isEmpty();

        // opis modułów (System.out.println(modules)) — fragment prawdziwego wyniku
        assertThat(MODULES.toString())
                .contains("# Zamówienia", "> Logical name: orders", "> Base package: s20_modulith.mod01_modules.orders");
    }

    @Test
    @DisplayName("2. API modułu = typy w pakiecie bazowym; klasy z internal są wewnętrzne, choć public")
    void section2() {
        ApplicationModule products = MODULES.getModuleByName("products").orElseThrow();
        assertThat(products.isExposed(ProductCatalog.class)).isTrue();
        assertThat(products.isExposed(ProductView.class)).isTrue();
        assertThat(products.isExposed(ProductStore.class)).isFalse();                 // products.internal

        assertThat(MODULES.getModuleByName("orders").orElseThrow().isExposed(OrderNumbers.class)).isFalse();
        assertThat(MODULES.getModuleByName("customers").orElseThrow().isExposed(CustomerView.class)).isTrue();

        // w opisie: „+” = wystawiony (API), „o” = wewnętrzny
        assertThat(MODULES.toString()).contains("+ ….ProductCatalog", "o ….internal.ProductStore",
                "+ ….OrderService", "o ….internal.OrderNumbers");
    }

    @Test
    @DisplayName("3. verify(): dobra aplikacja przechodzi; sięganie do internal innego modułu kończy się Violations")
    void section3() {
        assertThatCode(MODULES::verify).doesNotThrowAnyException();

        // zła aplikacja kompiluje się i DZIAŁA — Spring nie pilnuje granic modułów
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(InternalAccessApplication.class)
                .web(WebApplicationType.NONE).run()) {
            assertThat(context.getBean(s20_modulith.mod01_broken.internal_access.orders.OrderService.class)
                    .canOrder("ELE-001", 2)).isTrue();
        }

        ApplicationModules broken = broken(InternalAccessApplication.class);
        assertThatThrownBy(broken::verify)
                .isInstanceOf(Violations.class)
                .hasMessageContaining("- Module 'orders' depends on non-exposed type "
                        + "s20_modulith.mod01_broken.internal_access.products.internal.ProductStore within module 'products'!")
                .hasMessageContaining("OrderService declares constructor OrderService(ProductStore)");
        // cztery pozycje: deklaracja konstruktora, parametr konstruktora, typ pola, wywołanie metody
        assertThat(broken.detectViolations().getMessages()).hasSize(4);

        // PUŁAPKA: verify() działa raz na obiekt — drugie wywołanie nic nie robi; detectViolations() liczy zawsze
        assertThatCode(broken::verify).doesNotThrowAnyException();
        assertThat(broken.detectViolations().hasViolations()).isTrue();

        // PUŁAPKA: domyślnie Modulith pomija klasy testowe — zła aplikacja z src/test/java „nie ma klas”
        assertThatThrownBy(() -> ApplicationModules.of(InternalAccessApplication.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No classes found in packages [s20_modulith.mod01_broken.internal_access]!");
    }

    @Test
    @DisplayName("4. Cykl orders ↔ products: aplikacja startuje, ale verify() zgłasza cykl między modułami")
    void section4() {
        // beany nie tworzą cyklu, więc Spring startuje bez błędu
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(CycleApplication.class)
                .web(WebApplicationType.NONE).run()) {
            assertThat(context.getBean(s20_modulith.mod01_broken.cycle.products.ProductCatalog.class).bestsellers())
                    .containsExactly("ELE-001", "KSI-001");
        }

        List<String> messages = broken(CycleApplication.class).detectViolations().getMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst())
                .startsWith("Cycle detected: Slice ")
                .contains("Slice orders", "Slice products", "Dependencies of Slice orders", "Dependencies of Slice products")
                .contains("has parameter of type <s20_modulith.mod01_broken.cycle.orders.OrderHistory>");
    }

    @Test
    @DisplayName("5. allowedDependencies: dobra aplikacja ma tylko zadeklarowane zależności, zła łamie listę")
    void section5() {
        assertThat(dependencies("orders")).containsExactly("customers", "products");
        assertThat(dependencies("products")).isEmpty();                                // allowedDependencies = {}
        assertThat(dependencies("customers")).isEmpty();

        assertThat(broken(NotAllowedApplication.class).detectViolations().getMessages()).containsExactly(
                "Module 'orders' depends on module 'customers' via "
                        + "s20_modulith.mod01_broken.not_allowed.orders.OrderService -> "
                        + "s20_modulith.mod01_broken.not_allowed.customers.CustomerDirectory. Allowed targets: products.");
    }

    @Test
    @DisplayName("6. verify() zgłasza wstrzykiwanie przez pole, nawet gdy sama zależność jest dozwolona")
    void section6() {
        assertThat(broken(FieldInjectionApplication.class).detectViolations().getMessages()).containsExactly(
                "Module Orders uses field injection in "
                        + "s20_modulith.mod01_broken.field_injection.orders.OrderService.catalog. "
                        + "Prefer constructor injection instead!");
    }

    @Test
    @DisplayName("Dobra aplikacja nie ma żadnych naruszeń (detectViolations zwraca pustą listę)")
    void goodApplicationHasNoViolations() {
        assertThat(MODULES.detectViolations().getMessages()).isEmpty();
        assertThat(MODULES.getModuleByType(OrderService.class)).map(m -> m.getIdentifier().toString()).contains("orders");
    }

    private static List<String> dependencies(String moduleName) {
        return MODULES.getModuleByName(moduleName).orElseThrow()
                .getDirectDependencies(MODULES).uniqueModules()
                .map(module -> module.getIdentifier().toString()).sorted().toList();
    }
}

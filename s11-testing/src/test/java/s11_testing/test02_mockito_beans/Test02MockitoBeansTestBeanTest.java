package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;

/**
 * Sekcja 4 lekcji Test02MockitoBeans: {@code @TestBean} — podmiana beana na obiekt zwrócony przez STATYCZNĄ metodę
 * fabryczną w klasie testowej. Bez Mockito: zwykły obiekt (tu lambda), który zachowuje się zawsze tak samo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class Test02MockitoBeansTestBeanTest {

    @TestBean   // test bean = bean testowy; Spring szuka statycznej metody bez parametrów o NAZWIE POLA: catalog()
    ProductCatalog catalog;

    @MockitoBean
    OrderNotifier notifier;

    @Autowired
    OrderService service;

    /** Metoda fabryczna dla {@code @TestBean ProductCatalog catalog}: każdy SKU kosztuje 1 zł (100 gr). */
    static ProductCatalog catalog() {
        return sku -> new Product(sku, "Produkt testowy " + sku, 100);
    }

    @Test
    @DisplayName("4. @TestBean: katalog z metody fabrycznej (każdy SKU po 100 gr), żadnej atrapy Mockito")
    void testBeanFromFactoryMethod() {
        assertThat(service.placeOrder("jan@example.com", "ABC-123", 3).totalGrosze()).isEqualTo(300);
        assertThat(mockingDetails(catalog).isMock()).isFalse();
        assertThat(catalog.getBySku("XYZ-000").name()).isEqualTo("Produkt testowy XYZ-000");
    }
}

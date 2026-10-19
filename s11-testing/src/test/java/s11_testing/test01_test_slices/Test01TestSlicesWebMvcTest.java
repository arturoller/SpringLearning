package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockingDetails;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 2 lekcji Test01TestSlices: {@code @WebMvcTest} — wycinek webowy (web slice). Jest kontroler i infrastruktura
 * MVC (DispatcherServlet, konwertery JSON, MockMvc), NIE MA serwisów, repozytoriów, DataSource (źródła danych) ani JPA.
 * Dowodzimy tego, zaglądając do kontekstu.
 */
@WebMvcTest(ProductController.class)   // tylko ten kontroler (bez atrybutu — wszystkie kontrolery z pakietu)
class Test01TestSlicesWebMvcTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService service;   // serwisu w wycinku nie ma — kontroler dostaje atrapę (mock)

    @Test
    @DisplayName("2. @WebMvcTest: kontroler jest, repozytorium i DataSource NIE MA, serwis to atrapa")
    void webSliceContents() {
        assertThat(context.getBeanNamesForType(ProductController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(ProductRepository.class)).isEmpty();
        assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
        assertThat(mockingDetails(context.getBean(ProductService.class)).isMock()).isTrue();
    }

    @Test
    @DisplayName("2. @WebMvcTest: zapytanie przechodzi przez prawdziwy kontroler i JSON, dane daje atrapa")
    void controllerWithMockService() throws Exception {
        given(service.byId(1L)).willReturn(new ProductDto(1L, "Kawa ziarnista 1kg", 6499, "64.99"));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kawa ziarnista 1kg"))
                .andExpect(jsonPath("$.priceZl").value("64.99"));
    }

    /** Ta sama konfiguracja, ale BEZ atrapy serwisu. Klasa-nośnik adnotacji — nie ma metod testowych. */
    @WebMvcTest(ProductController.class)
    static class WithoutServiceMock {
    }

    @Test
    @DisplayName("2. PUŁAPKA: @WebMvcTest bez @MockitoBean dla serwisu → kontekst nie wstaje (brak beana ProductService)")
    void missingMockFailsContext() {
        // TestContextManager (zarządca kontekstu testu) robi to samo, co JUnit + SpringExtension przed zwykłym testem.
        assertThatThrownBy(() -> new TestContextManager(WithoutServiceMock.class).getTestContext().getApplicationContext())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Failed to load ApplicationContext")
                .rootCause()
                .isInstanceOf(NoSuchBeanDefinitionException.class)
                .hasMessage("No qualifying bean of type 's11_testing.test01_test_slices.ProductService' available: "
                        + "expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}");
    }
}

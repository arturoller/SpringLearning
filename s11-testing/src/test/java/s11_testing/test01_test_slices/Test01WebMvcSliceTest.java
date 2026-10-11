package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockingDetails;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 2 lekcji Test01TestSlices: {@code @WebMvcTest} — wycinek webowy. Jest kontroler i infrastruktura MVC,
 * NIE MA serwisów, repozytoriów, DataSource (źródła danych) ani JPA. Dowodzimy tego, zaglądając do kontekstu.
 */
@WebMvcTest(ProductController.class)
class Test01WebMvcSliceTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService service;   // serwisu w wycinku nie ma — kontroler dostaje atrapę

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
        given(service.byId(1L)).willReturn(new ProductDto(1L, "Kawa", 2999, "29.99"));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kawa"))
                .andExpect(jsonPath("$.priceZl").value("29.99"));
    }
}

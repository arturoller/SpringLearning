package s09_jpa_relations.rel04_lazy_loading;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletException;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test sekcji 5 lekcji Rel04LazyLoading z WYŁĄCZONYM open-in-view ({@code spring.jpa.open-in-view=false}) — osobna
 * klasa, bo inna wartość właściwości to inny kontekst Springa. Ten sam kontroler co w Rel04LazyLoadingTest.
 */
@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc
@ActiveProfiles("rel04")
class Rel04OpenInViewOffTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationContext context;

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("5. open-in-view=false: brak OpenEntityManagerInViewInterceptor w kontekście")
    void noInterceptor() {
        assertThat(context.getBeansOfType(OpenEntityManagerInViewInterceptor.class)).isEmpty();
    }

    @Test
    @DisplayName("5. open-in-view=false: kontroler czytający leniwe pole po transakcji → LazyInitializationException")
    void lazyFieldInControllerFails() {
        assertThatThrownBy(() -> mvc.perform(get("/api/rel04/orders/ZAM-001")))
                .isInstanceOf(ServletException.class)
                .hasMessageStartingWith("Request processing failed: org.hibernate.LazyInitializationException: "
                        + "Could not initialize proxy [s09_jpa_relations.rel04_lazy_loading.Customer#")
                .hasRootCauseInstanceOf(LazyInitializationException.class);
    }

    @Test
    @DisplayName("5. open-in-view=false: połączenie wraca do puli zaraz po transakcji serwisu (busyConnections = 0)")
    void connectionReturnedAfterTransaction() throws Exception {
        mvc.perform(get("/api/rel04/orders/ZAM-001/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busyConnections").value(0));
    }

    @Test
    @DisplayName("5. Pula HikariCP ma domyślnie 10 połączeń — tyle równoczesnych żądań może je trzymać przy open-in-view")
    void defaultPoolSize() throws SQLException {
        assertThat(dataSource.unwrap(HikariDataSource.class).getMaximumPoolSize()).isEqualTo(10);
    }
}

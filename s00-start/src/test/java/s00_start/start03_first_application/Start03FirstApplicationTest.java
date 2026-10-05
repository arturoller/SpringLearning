package s00_start.start03_first_application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import s00_start.start04_testing_intro.PriceCalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Start03FirstApplication.
 * {@code @SpringBootTest} uruchamia kontekst Start03Application (bez prawdziwego serwera — w środowisku „na niby”).
 * {@code @AutoConfigureMockMvc} dodaje MockMvc: narzędzie, które wysyła zapytania HTTP do kontrolerów bez sieci i portu.
 * Szczegółowo o obu — w lekcji Start04TestingIntro.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Start03FirstApplicationTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. Skanowanie pakietu znalazło serwis i kontroler — oba są beanami")
    void componentScanFindsBeans() {
        assertThat(context.getBean(GreetingService.class)).isNotNull();
        assertThat(context.getBean(GreetingController.class)).isNotNull();
    }

    @Test
    @DisplayName("2. Spring tworzy JEDEN obiekt serwisu (singleton) i zawsze zwraca ten sam")
    void serviceIsSingleton() {
        assertThat(context.getBean(GreetingService.class)).isSameAs(context.getBean(GreetingService.class));
    }

    @Test
    @DisplayName("2. Beanów innej lekcji (PriceCalculator ze Start04) NIE ma w tym kontekście — izolacja lekcji")
    void otherLessonsAreNotScanned() {
        assertThat(context.getBeanNamesForType(PriceCalculator.class)).isEmpty();
    }

    @Test
    @DisplayName("3. GET /api/hello?name=Ala zwraca 200 i JSON z powitaniem")
    void helloWithName() throws Exception {
        mockMvc.perform(get("/api/hello").param("name", "Ala"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cześć, Ala!"));
    }

    @Test
    @DisplayName("4. Bez parametru name działa wartość domyślna „świecie”")
    void helloDefault() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cześć, świecie!"));
    }

    @Test
    @DisplayName("4. Pusty parametr name= też dostaje wartość domyślną (pytanie kontrolne 3)")
    void helloEmptyParameter() throws Exception {
        mockMvc.perform(get("/api/hello").param("name", ""))
                .andExpect(jsonPath("$.message").value("Cześć, świecie!"));
    }

    @Test
    @DisplayName("3. Nieznany adres zwraca 404 Not Found")
    void unknownAddress() throws Exception {
        mockMvc.perform(get("/api/nie-ma-takiego-adresu")).andExpect(status().isNotFound());
    }
}

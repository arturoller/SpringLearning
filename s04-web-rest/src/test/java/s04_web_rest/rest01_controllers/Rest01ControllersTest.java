package s04_web_rest.rest01_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest01Controllers. {@code @WebMvcTest} (test warstwy webowej) — wycinek kontekstu: tylko wskazane
 * kontrolery + infrastruktura MVC (DispatcherServlet, mapowania, konwertery), bez serwera. Wskazujemy kontrolery
 * lekcji wprost, żeby szkielet ćwiczeń (Rest01ExerciseController) nie wpływał na testy lekcji.
 */
@WebMvcTest(controllers = {
        ProductController.class,
        GreetingControllers.RestGreetingController.class,
        GreetingControllers.ClassicGreetingController.class,
        GreetingControllers.ViewNameController.class})
class Rest01ControllersTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("1. @RestController i @Controller + @ResponseBody dają TĘ SAMĄ odpowiedź: tekst w ciele")
    void restControllerEqualsControllerPlusResponseBody() throws Exception {
        mockMvc.perform(get("/api/greeting/rest"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Witaj w sklepie"));

        mockMvc.perform(get("/api/greeting/classic"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Witaj w sklepie"));
    }

    @Test
    @DisplayName("1. PUŁAPKA: sam @Controller zwraca nazwę widoku — MockMvc pokazuje forward pod „witaj” i PUSTE ciało")
    void viewNameInsteadOfBody() throws Exception {
        mockMvc.perform(get("/api/greeting/view"))
                .andExpect(status().isOk())              // MockMvc nie wykonuje forwardu, więc widać 200…
                .andExpect(forwardedUrl("witaj"))        // …ale zapytanie zostało przekazane pod „witaj”
                .andExpect(content().string(""));        // tekstu „witaj” w ciele NIE ma
    }

    @Test
    @DisplayName("2. GET /api/products → 200 i tablica JSON posortowana po sku")
    void getAll() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].sku").value("ELE-001"))
                .andExpect(jsonPath("$[0].price").value(5499.99));
    }

    @Test
    @DisplayName("2. GET /api/products/{sku} → jeden produkt; nieznany sku → 404 z ResponseStatusException")
    void getOne() throws Exception {
        mockMvc.perform(get("/api/products/SPO-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kawa ziarnista 1kg"));

        mockMvc.perform(get("/api/products/XXX-999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("2. POST, PUT, PATCH i DELETE trafiają do właściwych metod kontrolera")
    void otherHttpMethods() throws Exception {
        // POST — dodanie (sku unikalny dla tego testu, żeby testy nie zależały od siebie)
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"ODZ-001","name":"Kurtka zimowa","category":"ODZIEZ","price":459.00,"stock":12}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("ODZ-001"));

        // PUT — zastąpienie całego produktu
        mockMvc.perform(put("/api/products/ODZ-001").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"ODZ-001","name":"Kurtka zimowa XL","category":"ODZIEZ","price":479.00,"stock":10}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kurtka zimowa XL"));

        // PATCH — zmiana tylko stanu magazynu
        mockMvc.perform(patch("/api/products/ODZ-001/stock").param("delta", "-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(7))
                .andExpect(jsonPath("$.name").value("Kurtka zimowa XL"));

        // DELETE — metoda void: 200 i puste ciało
        mockMvc.perform(delete("/api/products/ODZ-001"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/products/ODZ-001"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("3. Pod maską: RequestMappingHandlerMapping wybiera ProductController.one, a JSON pisze JacksonJsonHttpMessageConverter")
    void underTheHood() throws Exception {
        RequestMappingHandlerMapping mapping = context.getBean(RequestMappingHandlerMapping.class);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products/ELE-001");
        HandlerExecutionChain chain = mapping.getHandler(request);   // „recepcjonistka sprawdza spis pokoi”

        assertThat(chain).isNotNull();
        HandlerMethod handler = (HandlerMethod) chain.getHandler();
        assertThat(handler.getBeanType()).isEqualTo(ProductController.class);
        assertThat(handler.getMethod().getName()).isEqualTo("one");

        RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);
        assertThat(adapter.getMessageConverters())
                .as("Spring Framework 7 + Boot 4: JSON przez Jacksona 3")
                .hasAtLeastOneElementOfType(JacksonJsonHttpMessageConverter.class);
    }

    @Test
    @DisplayName("4. Zła metoda HTTP → 405 Method Not Allowed i nagłówek Allow z dozwolonymi metodami (pytanie 3)")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(post("/api/greeting/rest"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(HttpRequestMethodNotSupportedException.class)
                        .hasMessage("Request method 'POST' is not supported"));

        mockMvc.perform(post("/api/products/ELE-001"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Allow"))
                        .contains("GET", "PUT", "DELETE")
                        .doesNotContain("POST"));
    }

    @Test
    @DisplayName("4. Nieznany adres → 404; Spring szukał jeszcze pliku statycznego (NoResourceFoundException)")
    void unknownAddress() throws Exception {
        mockMvc.perform(get("/api/nothing"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(NoResourceFoundException.class)
                        .hasMessageContaining("No static resource api/nothing"));
    }

    @Test
    @DisplayName("5. Kontroler jest singletonem: jeden obiekt, stan widoczny dla kolejnych zapytań")
    void controllerIsSingletonWithSharedState() throws Exception {
        assertThat(context.getBean(ProductController.class)).isSameAs(context.getBean(ProductController.class));

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"DOM-002","name":"Lampka biurkowa","category":"DOM","price":129.00,"stock":18}
                        """))
                .andExpect(status().isOk());

        // „inny klient” — kolejne zapytanie trafia do TEGO SAMEGO obiektu kontrolera, więc widzi dodany produkt
        mockMvc.perform(get("/api/products/DOM-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lampka biurkowa"));
    }

    @Test
    @DisplayName("6. Zapowiedź: MockMvcTester — ten sam test w stylu AssertJ (więcej w s11_testing)")
    void mockMvcTesterPreview() {
        MockMvcTester tester = MockMvcTester.create(mockMvc);   // tester = sprawdzacz; owija zwykłe MockMvc

        assertThat(tester.get().uri("/api/products/KSI-001"))
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson().extractingPath("$.name").isEqualTo("Czysty kod");
    }
}

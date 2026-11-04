package s04_web_rest.rest01_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

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
 * Testy lekcji Rest01Controllers. Wybór wycinka: {@code @WebMvcTest} — startuje TYLKO warstwę web (kontrolery,
 * konwertery, DispatcherServlet), bez serwera Tomcat i bez reszty aplikacji; zapytania wysyłamy przez MockMvc
 * (atrapa serwera). To najwęższy wycinek, który potrafi odpowiedzieć na pytanie „co zwróci kontroler?”.
 */
@WebMvcTest
class Rest01ControllersTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RequestMappingHandlerMapping handlerMapping;

    @Autowired
    RequestMappingHandlerAdapter handlerAdapter;

    @Test
    @DisplayName("1. @RestController = @Controller + @ResponseBody, a zwykły @Controller zwraca nazwę widoku")
    void restControllerIsControllerPlusResponseBody() throws Exception {
        assertThat(AnnotatedElementUtils.hasAnnotation(HelloController.class, Controller.class)).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(HelloController.class, ResponseBody.class)).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(ClassicController.class, ResponseBody.class)).isFalse();

        mockMvc.perform(get("/rest01/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Witaj w sklepie: Sklep DP21!"));

        // @Controller + @ResponseBody na metodzie: wynik ten sam co w @RestController
        mockMvc.perform(get("/rest01/classic-body"))
                .andExpect(status().isOk())
                .andExpect(content().string("to jest treść odpowiedzi"));

        // @Controller bez @ResponseBody: napis to NAZWA WIDOKU — ciało jest puste, a MockMvc pokazuje przekierowanie
        // wewnętrzne (forward) do widoku. Na prawdziwym serwerze takiego widoku nie ma, więc klient nie dostanie tego napisu.
        mockMvc.perform(get("/rest01/classic-view"))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andExpect(forwardedUrl("nazwa-widoku"));
    }

    @Test
    @DisplayName("2. Ta sama ścieżka, różne metody HTTP → różne metody Javy")
    void samePathDifferentHttpMethods() throws Exception {
        mockMvc.perform(get("/rest01/orders")).andExpect(content().string("GET: lista zamówień"));
        mockMvc.perform(post("/rest01/orders")).andExpect(content().string("POST: nowe zamówienie"));
        mockMvc.perform(put("/rest01/orders")).andExpect(content().string("PUT: podmiana zamówienia w całości"));
        mockMvc.perform(patch("/rest01/orders")).andExpect(content().string("PATCH: zmiana części zamówienia"));
        mockMvc.perform(delete("/rest01/orders")).andExpect(content().string("DELETE: usunięcie zamówienia"));
        // zapis klasyczny @RequestMapping(method = GET) działa tak samo jak @GetMapping
        mockMvc.perform(get("/rest01/orders/legacy")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. Zła metoda HTTP → 405 z nagłówkiem Allow, nieznana ścieżka → 404")
    void wrongMethodAnd404() throws Exception {
        // /rest01/hello zna tylko GET: POST dostaje 405 Method Not Allowed, a serwer podpowiada w Allow, co wolno
        mockMvc.perform(post("/rest01/hello"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"));

        // ścieżki, której nikt nie zmapował, nie ma → 404 Not Found
        mockMvc.perform(get("/rest01/nie-ma-takiej")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("3. String daje text/plain, a rekord daje application/json")
    void messageConvertersChooseContentType() throws Exception {
        mockMvc.perform(get("/rest01/hello"))
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"));

        mockMvc.perform(get("/rest01/hello-json"))
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(content().json("{\"message\":\"Witaj w sklepie: Sklep DP21!\",\"version\":1}"));
    }

    @Test
    @DisplayName("3. Pod spodem: HandlerMapping zna metodę, a adapter ma konwertery String i JSON (Jackson 3)")
    void underTheHood() {
        // HandlerMapping (mapowanie żądań na metody): ścieżka → metoda Javy. Klucz to opis „GET [/rest01/hello]”.
        assertThat(handlerMapping.getHandlerMethods().keySet())
                .anySatisfy(info -> assertThat(info.toString()).contains("GET [/rest01/hello]"));

        // HttpMessageConverter (konwerter wiadomości): obiekt Javy ↔ ciało HTTP. W Framework 7 konwerter JSON
        // oparty o Jackson 3 to JacksonJsonHttpMessageConverter (w starych tutorialach: MappingJackson2HttpMessageConverter).
        assertThat(handlerAdapter.getMessageConverters())
                .anyMatch(StringHttpMessageConverter.class::isInstance)
                .anyMatch(JacksonJsonHttpMessageConverter.class::isInstance);
    }

    @Test
    @DisplayName("4. produces: nagłówek Accept wybiera metodę (CSV albo JSON), nieznany format → 406")
    void producesAndAccept() throws Exception {
        mockMvc.perform(get("/rest01/report").accept("text/csv"))
                .andExpect(status().isOk())
                .andExpect(content().string("id;name\n1;Kawa\n2;Herbata\n"));

        mockMvc.perform(get("/rest01/report").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kawa"));

        // Klient prosi o XML, którego żadna metoda nie umie oddać → 406 Not Acceptable
        // (serwer wylicza w nagłówku Accept odpowiedzi, co UMIE oddać)
        mockMvc.perform(get("/rest01/report").accept("application/xml"))
                .andExpect(status().isNotAcceptable())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Accept"))
                        .contains("application/json", "text/csv")); // kolejność na liście nie jest gwarantowana
    }

    @Test
    @DisplayName("4. consumes: zły Content-Type zapytania → 415")
    void consumesAndContentType() throws Exception {
        mockMvc.perform(post("/rest01/report/import").contentType("application/json").content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("przyjęto JSON"));

        // Wysyłamy zwykły tekst, a metoda przyjmuje tylko JSON → 415 Unsupported Media Type
        mockMvc.perform(post("/rest01/report/import").contentType("text/plain").content("x"))
                .andExpect(status().isUnsupportedMediaType());

        // Brak Content-Type to też 415
        mockMvc.perform(post("/rest01/report/import"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("5. PUŁAPKA: końcowy ukośnik — /hello/ NIE pasuje do /hello (404)")
    void trailingSlashDoesNotMatch() throws Exception {
        mockMvc.perform(get("/rest01/hello")).andExpect(status().isOk());
        mockMvc.perform(get("/rest01/hello/")).andExpect(status().isNotFound());
        // rozszerzenie w ścieżce też już nie pasuje (kiedyś /hello.json oznaczało „daj JSON”)
        mockMvc.perform(get("/rest01/hello.json")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5. PUŁAPKA: String przy Accept: application/json dostaje nagłówek JSON, choć to nie jest JSON")
    void stringWithJsonAcceptIsNotJson() throws Exception {
        mockMvc.perform(get("/rest01/hello").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(content().string("Witaj w sklepie: Sklep DP21!")); // goły tekst, bez cudzysłowów JSON
    }

    @Test
    @DisplayName("5. DOBRA PRAKTYKA: ta sama ścieżka z różnymi produces NIE jest niejednoznaczna")
    void differentProducesIsNotAmbiguous() {
        assertThat(handlerMapping.getHandlerMethods().keySet().stream()
                .filter(info -> info.toString().contains("[/rest01/report]"))
                .count()).isEqualTo(2);
    }
}

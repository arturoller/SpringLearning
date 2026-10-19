package s15_actuator_logging.obs01_actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Obs01Actuator. Dobór narzędzia: punkty końcowe Actuatora to zwykłe adresy HTTP obsługiwane przez
 * DispatcherServlet, więc wystarcza MockMvc (bez sieci, szybko) — z jednym wyjątkiem: osobny port zarządzania
 * wymaga prawdziwego serwera (RANDOM_PORT). Każda klasa {@code @Nested} ma własne ustawienia {@code management.*}
 * w {@code properties} — dzięki temu widać, która właściwość co zmienia. Testy używają prawdziwej klasy
 * startowej lekcji (Obs01Application) bez profilu "obs01", czyli CZYSTYCH ustawień domyślnych Boota.
 */
class Obs01ActuatorTest {

    // =================================================================================================
    // 1. Domyślnie przez HTTP jest wystawione tylko /actuator/health
    // =================================================================================================

    @Nested   // nested = zagnieżdżona (osobny kontekst z własnymi ustawieniami)
    @SpringBootTest
    @AutoConfigureMockMvc
    @DisplayName("1. Ustawienia domyślne: przez HTTP widać tylko health")
    class Defaults {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("health odpowiada 200 i statusem UP, ale bez szczegółów (show-details=never)")
        void healthIsExposedWithoutDetails() throws Exception {
            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist());
        }

        @Test
        @DisplayName("info, env, metrics, loggers, beans, conditions — nie są wystawione: 404")
        void othersAreNotExposed() throws Exception {
            for (String endpoint : new String[]{"info", "env", "metrics", "loggers", "beans", "conditions"}) {
                mvc.perform(get("/actuator/" + endpoint)).andExpect(status().isNotFound());
            }
        }

        @Test
        @DisplayName("strona /actuator (discovery) wymienia tylko self i health")
        void discoveryListsOnlyHealth() throws Exception {
            mvc.perform(get("/actuator"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links", hasKey("health")))
                    .andExpect(jsonPath("$._links", not(hasKey("env"))))
                    .andExpect(jsonPath("$._links", not(hasKey("metrics"))));
        }
    }

    // =================================================================================================
    // 2. Wystawianie punktów końcowych: management.endpoints.web.exposure.include
    // =================================================================================================

    @Nested
    @SpringBootTest(properties = "management.endpoints.web.exposure.include=health,info,metrics,loggers,env,beans,conditions")
    @AutoConfigureMockMvc
    @DisplayName("2. exposure.include wystawia wskazane punkty końcowe")
    class Exposed {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("wymienione punkty końcowe odpowiadają 200")
        void listedEndpointsAnswer() throws Exception {
            for (String endpoint : new String[]{"health", "info", "metrics", "loggers", "env", "beans", "conditions"}) {
                mvc.perform(get("/actuator/" + endpoint)).andExpect(status().isOk());
            }
        }

        @Test
        @DisplayName("beans pokazuje beany aplikacji, a conditions — raport autokonfiguracji")
        void beansAndConditions() throws Exception {
            mvc.perform(get("/actuator/beans"))
                    .andExpect(jsonPath("$.contexts.application.beans.productController.type")
                            .value(ProductController.class.getName()));
            mvc.perform(get("/actuator/conditions"))
                    .andExpect(jsonPath("$.contexts.application.positiveMatches").isMap())
                    .andExpect(jsonPath("$.contexts.application.negativeMatches").isMap());
        }

        @Test
        @DisplayName("niewymieniony punkt końcowy (configprops) nadal daje 404")
        void notListedStaysHidden() throws Exception {
            mvc.perform(get("/actuator/configprops")).andExpect(status().isNotFound());
        }
    }

    // =================================================================================================
    // 3. /actuator/info — dane aplikacji
    // =================================================================================================

    @Nested
    @SpringBootTest(properties = {
            "management.endpoints.web.exposure.include=info",
            "management.info.env.enabled=true",
            "info.app.name=Sklep SpringLearning",
            "info.app.version=1.0-nauka"})
    @AutoConfigureMockMvc
    @DisplayName("3. info z właściwości info.* (po włączeniu management.info.env.enabled)")
    class InfoWithData {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("właściwości info.* trafiają do odpowiedzi")
        void infoShowsProperties() throws Exception {
            mvc.perform(get("/actuator/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app.name").value("Sklep SpringLearning"))
                    .andExpect(jsonPath("$.app.version").value("1.0-nauka"));
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "management.endpoints.web.exposure.include=info",
            "info.app.name=Sklep SpringLearning"})
    @AutoConfigureMockMvc
    @DisplayName("3. PUŁAPKA: info.* bez management.info.env.enabled=true daje pustą odpowiedź")
    class InfoWithoutEnvContributor {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("wystawiony info odpowiada 200, ale ciało to {} — dane nie są pokazywane")
        void infoIsEmpty() throws Exception {
            mvc.perform(get("/actuator/info"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{}", true));
        }
    }

    // =================================================================================================
    // 4. env — wartości domyślnie zamaskowane
    // =================================================================================================

    @Nested
    @SpringBootTest(properties = {
            "management.endpoints.web.exposure.include=env",
            "sklep.klucz-testowy=NIE-PRAWDZIWY-KLUCZ"})
    @AutoConfigureMockMvc
    @DisplayName("4. env domyślnie maskuje wartości (show-values=never)")
    class EnvMasked {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("wartość właściwości to ****** — widać nazwę i źródło, nie wartość")
        void valuesAreMasked() throws Exception {
            mvc.perform(get("/actuator/env/sklep.klucz-testowy"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.property.value").value("******"));
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "management.endpoints.web.exposure.include=env",
            "management.endpoint.env.show-values=always",
            "sklep.klucz-testowy=NIE-PRAWDZIWY-KLUCZ"})
    @AutoConfigureMockMvc
    @DisplayName("4. PUŁAPKA: show-values=always ujawnia wartości każdemu, kto trafi na adres")
    class EnvRevealed {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("wartość widać wprost w odpowiedzi")
        void valuesAreVisible() throws Exception {
            mvc.perform(get("/actuator/env/sklep.klucz-testowy"))
                    .andExpect(jsonPath("$.property.value").value("NIE-PRAWDZIWY-KLUCZ"));
        }
    }

    // =================================================================================================
    // 5. include=* i dostęp (access) — czego NIE wystawia gwiazdka
    // =================================================================================================

    @Nested
    @SpringBootTest(properties = "management.endpoints.web.exposure.include=*")
    @AutoConfigureMockMvc
    @DisplayName("5. include=* wystawia prawie wszystko, ale nie heapdump i shutdown")
    class Wildcard {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("env, threaddump, mappings i configprops są wystawione; shutdown i heapdump nie")
        void wildcardExposesMostEndpoints() throws Exception {
            mvc.perform(get("/actuator"))
                    .andExpect(jsonPath("$._links", hasKey("env")))
                    .andExpect(jsonPath("$._links", hasKey("threaddump")))
                    .andExpect(jsonPath("$._links", hasKey("mappings")))
                    .andExpect(jsonPath("$._links", hasKey("configprops")))
                    .andExpect(jsonPath("$._links", not(hasKey("shutdown"))))
                    .andExpect(jsonPath("$._links", not(hasKey("heapdump"))));
            mvc.perform(get("/actuator/heapdump")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "management.endpoints.web.exposure.include=env",
            "management.endpoints.web.exposure.exclude=env"})
    @AutoConfigureMockMvc
    @DisplayName("5. exclude ma pierwszeństwo przed include (ten sam punkt na obu listach)")
    class ExcludeWins {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("env jest na obu listach, więc GET /actuator/env daje 404")
        void excludeWins() throws Exception {
            mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "management.endpoints.web.exposure.include=*",
            "management.endpoints.web.exposure.exclude=env,threaddump",
            "management.endpoint.heapdump.access=unrestricted"})
    @AutoConfigureMockMvc
    @DisplayName("5. exclude wyłącza z wystawienia, a access=unrestricted włącza heapdump świadomie")
    class ExcludeAndAccess {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("exclude ma pierwszeństwo przed include; heapdump pojawia się dopiero po jawnym access")
        void excludeBeatsIncludeAndHeapdumpNeedsAccess() throws Exception {
            mvc.perform(get("/actuator"))
                    .andExpect(jsonPath("$._links", not(hasKey("env"))))
                    .andExpect(jsonPath("$._links", not(hasKey("threaddump"))))
                    .andExpect(jsonPath("$._links", hasKey("heapdump")))
                    .andExpect(jsonPath("$._links", hasKey("mappings")));
        }
    }

    // =================================================================================================
    // 6. Osobny port zarządzania
    // =================================================================================================

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
    @DisplayName("6. management.server.port — Actuator na osobnym porcie")
    class SeparateManagementPort {

        @LocalServerPort
        int appPort;           // port aplikacji (ProductController)

        @LocalManagementPort
        int managementPort;    // port zarządzania (Actuator); 0 w ustawieniu = losowy wolny

        private int statusOf(int port, String path) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        }

        @Test
        @DisplayName("health jest tylko na porcie zarządzania, a API sklepu tylko na porcie aplikacji")
        void actuatorLivesOnItsOwnPort() throws Exception {
            assertThat(appPort).isNotEqualTo(managementPort);
            assertThat(statusOf(managementPort, "/actuator/health")).isEqualTo(200);
            assertThat(statusOf(appPort, "/actuator/health")).isEqualTo(404);
            assertThat(statusOf(appPort, "/api/products")).isEqualTo(200);
            assertThat(statusOf(managementPort, "/api/products")).isEqualTo(404);
        }
    }
}

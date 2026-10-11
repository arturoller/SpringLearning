package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Test01TestSlices: {@code @SpringBootTest(webEnvironment = ...)} — środowisko webowe testu
 * (web environment). MOCK (domyślne) — kontekst webowy bez serwera; RANDOM_PORT — prawdziwy Tomcat na losowym
 * porcie; NONE — kontekst zwykły, bez warstwy web. DEFINED_PORT (port z konfiguracji, zwykle 8080) celowo
 * pomijamy: w testach nigdy nie używamy stałego portu.
 */
class Test01WebEnvironmentTest {

    @Nested
    @SpringBootTest   // = webEnvironment = WebEnvironment.MOCK
    class Mock {

        @Autowired
        ApplicationContext context;

        @Test
        @DisplayName("5. MOCK: kontekst webowy (WebApplicationContext), ale serwer WWW nie startuje")
        void mock() {
            assertThat(context).isInstanceOf(WebApplicationContext.class);
            assertThat(context).isNotInstanceOf(WebServerApplicationContext.class);
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    class RandomPort {

        @LocalServerPort   // local server port = port lokalnego serwera, wylosowany przy starcie
        int port;

        @Autowired
        ApplicationContext context;

        @Test
        @DisplayName("5. RANDOM_PORT: prawdziwy serwer na losowym wolnym porcie")
        void randomPort() {
            assertThat(port).isPositive();
            assertThat(context).isInstanceOf(WebServerApplicationContext.class);
            assertThat(((WebServerApplicationContext) context).getWebServer().getPort()).isEqualTo(port);
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.NONE)
    class None {

        @Autowired
        ApplicationContext context;

        @Test
        @DisplayName("5. NONE: zwykły kontekst bez warstwy web — szybszy, gdy testujesz tylko serwisy")
        void none() {
            assertThat(context).isNotInstanceOf(WebApplicationContext.class);
            assertThat(context.getBeanNamesForType(ProductService.class)).hasSize(1);
        }
    }
}

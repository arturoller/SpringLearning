package s12_security.sec01_basics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test PUŁAPKI z sekcji 1 lekcji Sec01Basics: drugi starter (oauth2-resource-server) wyłącza wygenerowanego
 * użytkownika. Osobna klasa, bo przechwycone wyjście ({@code CapturedOutput}) zbiera log ze WSZYSTKICH testów klasy —
 * tu nie może się w nim znaleźć hasło z innego testu.
 */
@ExtendWith(OutputCaptureExtension.class)
class Sec01BasicsResourceServerPitfallTest {

    @Test
    @DisplayName("1. PUŁAPKA: ze starterem oauth2-resource-server na classpath Boot NIE tworzy użytkownika „user” ani hasła")
    void noGeneratedUserWhenResourceServerIsOnClasspath(CapturedOutput output) throws Exception {
        // ta sama aplikacja co w Sec01BasicsStarterOnlyTest, ale BEZ spring.security.user.name
        try (ConfigurableApplicationContext context = new SpringApplication(Sec01StarterOnlyApplication.class)
                .run("--server.port=0")) {
            assertThat(context.getBeanNamesForType(UserDetailsService.class)).as("brak beana z użytkownikami").isEmpty();
            assertThat(output.getAll()).doesNotContain("Using generated security password");

            // nie ma żadnego użytkownika, więc każda próba logowania kończy się 401
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/orders"))
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("user:cokolwiek".getBytes()))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(401);
        }
    }
}

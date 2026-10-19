package s22_extras.ext05_webflux_overview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Klasa startowa lekcji Ext05WebFluxOverview. W module są i MVC, i WebFlux — Boot wybrałby MVC (Tomcat), dlatego
 * typ aplikacji ustawiamy jawnie na REACTIVE (serwer Netty). ▶ przy main, potem s22-extras/http/ext05_webflux_overview.http.
 */
@SpringBootApplication
public class Ext05Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Ext05Application.class);
        application.setWebApplicationType(WebApplicationType.REACTIVE);   // to samo: spring.main.web-application-type=reactive
        application.run(args);
    }

    /**
     * Sekcja 3: styl FUNKCYJNY WebFlux (functional endpoints) — trasa jako bean zamiast adnotacji. Ten sam serwer,
     * ta sama obsługa; wybór stylu to kwestia gustu i zespołu.
     */
    @Bean
    RouterFunction<ServerResponse> helloRoute() {
        return RouterFunctions.route()
                .GET("/api/reactive/hello", request -> ServerResponse.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .bodyValue("Cześć z WebFlux!"))
                .build();
    }

    /**
     * IZOLACJA: Spring Security (ze startera resource-server w module) w aplikacji REAKTYWNEJ to inny łańcuch —
     * SecurityWebFilterChain i ServerHttpSecurity zamiast SecurityFilterChain i HttpSecurity. Bez tego beana każde
     * zapytanie dostałoby 401. Ta lekcja nie uczy zabezpieczeń — otwieramy wszystko.
     *
     * <p>Łańcuch jest też BEZSTANOWY (bez sesji): domyślny sięga po WebSession przy każdym zapytaniu, a sesja w pamięci
     * (InMemoryWebSessionStore) powstaje na puli boundedElastic i wraca na pulę parallel — kontroler działałby wtedy
     * na wątkach parallel-N zamiast na wątkach Netty reactor-http-nio-N (sprawdzone uruchomieniem).</p>
     */
    @Bean
    SecurityWebFilterChain ext05OpenChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())   // nie zapisuj w sesji
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))  // nie pamiętaj żądań
                .build();
    }
}

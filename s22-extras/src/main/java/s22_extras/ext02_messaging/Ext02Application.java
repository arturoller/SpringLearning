package s22_extras.ext02_messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Klasa startowa lekcji Ext02Messaging. Włącza profil ext02 (application-ext02.properties: broker localhost:9092,
 * JSON przez Jackson 3).
 *
 * <p>Najpierw uruchom Kafkę: {@code docker compose -f s22-extras/compose/ext02/compose.yaml up -d}, potem ▶ przy main
 * i zapytania z s22-extras/http/ext02_messaging.http.</p>
 */
@SpringBootApplication
public class Ext02Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Ext02Application.class);
        application.setAdditionalProfiles("ext02");
        application.run(args);
    }

    /**
     * Temat z 3 partycjami. Bean NewTopic sprawia, że KafkaAdmin przy starcie łączy się z brokerem i zakłada temat
     * (jeśli go nie ma). Bez tego beana temat powstałby automatycznie przy pierwszym wysłaniu — z domyślną liczbą
     * partycji brokera (zwykle 1).
     */
    @Bean
    NewTopic ordersTopic() {
        return TopicBuilder.name(OrderEventPublisher.TOPIC)
                .partitions(3)
                .replicas(1)   // replika = kopia partycji na innym brokerze; lokalnie mamy jeden broker
                .build();
    }

    /** IZOLACJA: starter oauth2-resource-server jest w module — ta lekcja nie uczy zabezpieczeń, więc otwieramy wszystko. */
    @Bean
    SecurityFilterChain ext02OpenChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }
}

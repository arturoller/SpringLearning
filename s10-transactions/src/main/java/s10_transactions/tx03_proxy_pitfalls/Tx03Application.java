package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Tx03ProxyPitfalls (bez weba). Profil {@code tx03} wczytuje {@code application-tx03.properties}.
 * Beany, które CELOWO psują start aplikacji (klasa final z {@code @Transactional}), leżą w testach w osobnym pakiecie
 * {@code s10_transactions.tx03_pitfalls} — poza zasięgiem skanowania tej klasy.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Tx03Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Tx03Application.class);
        app.setAdditionalProfiles("tx03"); // profile = profil ustawień
        app.run(args).close();
    }
}

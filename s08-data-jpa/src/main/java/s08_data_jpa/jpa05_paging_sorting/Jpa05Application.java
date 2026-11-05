package s08_data_jpa.jpa05_paging_sorting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jpa05PagingSorting (bez weba). Z jej pakietu Boot skanuje encje ({@code @Entity})
 * i repozytoria Spring Data. Profil {@code jpa05} wczytuje {@code application-jpa05.properties}: własną bazę H2,
 * Flyway z {@code db/migration/jpa05} i {@code ddl-auto=validate}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Jpa05Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jpa05Application.class);
        app.setAdditionalProfiles("jpa05"); // profile = profil ustawień
        app.run(args).close();
    }
}

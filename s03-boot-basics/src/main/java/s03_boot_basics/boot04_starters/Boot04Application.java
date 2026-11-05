package s03_boot_basics.boot04_starters;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Boot04Starters. Pusta: wszystko, co aplikacja dostaje (DataSource, JdbcTemplate…),
 * pochodzi ze starterów z pliku {@code pom.xml} modułu i ich autokonfiguracji.
 */
@SpringBootApplication
public class Boot04Application {

    public static void main(String[] args) {
        SpringApplication.run(Boot04Application.class, args);
    }
}

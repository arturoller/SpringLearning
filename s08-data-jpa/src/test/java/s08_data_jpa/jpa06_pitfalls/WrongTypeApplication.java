package s08_data_jpa.jpa06_pitfalls;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Osobna, celowo wadliwa aplikacja do sekcji 2 i 3 lekcji Jpa06FlywayWithJpa. Leży w pakiecie TESTOWYM
 * jpa06_pitfalls, POZA jpa06_flyway_with_jpa — inaczej klasa startowa lekcji zeskanowałaby błędną encję i żaden
 * test lekcji by nie wystartował. Encja: WrongTypeProduct z tego pakietu; schemat: migracje lekcji (profil jpa06).
 */
@SpringBootApplication
public class WrongTypeApplication {
}

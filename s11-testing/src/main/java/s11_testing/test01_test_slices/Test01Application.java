package s11_testing.test01_test_slices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Test01TestSlices. Przede wszystkim to KONFIGURACJA dla testów: testy tej lekcji (wycinki
 * i {@code @SpringBootTest}) znajdują ją same — szukają klasy z {@code @SpringBootConfiguration} w pakiecie testu
 * i wyżej. Baza: H2 w pamięci; tabele tworzy Hibernate z encji (Flyway jest w tym module wyłączony, a dla bazy
 * wbudowanej Boot domyślnie ustawia {@code spring.jpa.hibernate.ddl-auto=create-drop}). Możesz ją uruchomić (▶),
 * ale baza jest pusta — „wynikiem” tej lekcji są testy, nie ręczne zapytania.
 */
@SpringBootApplication
public class Test01Application {

    public static void main(String[] args) {
        SpringApplication.run(Test01Application.class, args);
    }
}

package s08_data_jpa.jpa02_repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jpa02Repositories. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@DataJpaTest
@ActiveProfiles("jpa02")
class Jpa02RepositoriesExercisesTest {

    @Autowired
    CustomerRepository customers;

    @Test
    @DisplayName("Ćwiczenie 1: rejestracja klienta zwraca id")
    void exercise1() {
        Long id = Jpa02Repositories.exercise1(customers, "nowy@sklep.pl", "Nowy");

        assertThat(id).isNotNull();
        assertThat(customers.findById(id)).map(Customer::getEmail).contains("nowy@sklep.pl");
    }

    @Test
    @DisplayName("Ćwiczenie 2: imię klienta albo „nieznany klient”")
    void exercise2() {
        Long id = customers.save(new Customer("ewa@sklep.pl", "Ewa")).getId();

        assertThat(Jpa02Repositories.exercise2(customers, id)).isEqualTo("Ewa");
        assertThat(Jpa02Repositories.exercise2(customers, -1L)).isEqualTo("nieznany klient");
    }

    @Test
    @DisplayName("Ćwiczenie 3: usunięcie nieaktywnych klientów")
    void exercise3() {
        customers.save(new Customer("a@sklep.pl", "A"));
        Customer b = new Customer("b@sklep.pl", "B");
        b.setActive(false);
        Customer c = new Customer("c@sklep.pl", "C");
        c.setActive(false);
        customers.save(b);
        customers.save(c);

        assertThat(Jpa02Repositories.exercise3(customers)).isEqualTo(2);
        assertThat(customers.findAll()).extracting(Customer::getName).containsExactly("A");
    }
}

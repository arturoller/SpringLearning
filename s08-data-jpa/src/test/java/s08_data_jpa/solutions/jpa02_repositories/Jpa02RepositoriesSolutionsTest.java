package s08_data_jpa.solutions.jpa02_repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import s08_data_jpa.jpa02_repositories.Customer;
import s08_data_jpa.jpa02_repositories.CustomerRepository;
import s08_data_jpa.jpa02_repositories.Jpa02Application;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jpa02RepositoriesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@DataJpaTest
@ContextConfiguration(classes = Jpa02Application.class)
@ActiveProfiles("jpa02")
class Jpa02RepositoriesSolutionsTest {

    @Autowired
    CustomerRepository customers;

    @Test
    @DisplayName("Wzorzec 1: rejestracja klienta zwraca id")
    void solution1() {
        Long id = Jpa02RepositoriesSolutions.solution1(customers, "nowy@sklep.pl", "Nowy");

        assertThat(id).isNotNull();
        assertThat(customers.findById(id)).map(Customer::getEmail).contains("nowy@sklep.pl");
    }

    @Test
    @DisplayName("Wzorzec 2: imię klienta albo „nieznany klient”")
    void solution2() {
        Long id = customers.save(new Customer("ewa@sklep.pl", "Ewa")).getId();

        assertThat(Jpa02RepositoriesSolutions.solution2(customers, id)).isEqualTo("Ewa");
        assertThat(Jpa02RepositoriesSolutions.solution2(customers, -1L)).isEqualTo("nieznany klient");
    }

    @Test
    @DisplayName("Wzorzec 3: usunięcie nieaktywnych klientów")
    void solution3() {
        customers.save(new Customer("a@sklep.pl", "A"));
        Customer b = new Customer("b@sklep.pl", "B");
        b.setActive(false);
        Customer c = new Customer("c@sklep.pl", "C");
        c.setActive(false);
        customers.save(b);
        customers.save(c);

        assertThat(Jpa02RepositoriesSolutions.solution3(customers)).isEqualTo(2);
        assertThat(customers.findAll()).extracting(Customer::getName).containsExactly("A");
    }
}

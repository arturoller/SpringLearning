package s08_data_jpa.solutions.jpa02_repositories;

import s08_data_jpa.jpa02_repositories.Customer;
import s08_data_jpa.jpa02_repositories.CustomerRepository;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jpa02Repositories — zajrzyj dopiero po własnej próbie! */
public final class Jpa02RepositoriesSolutions {

    private Jpa02RepositoriesSolutions() {
    }

    static Long solution1(CustomerRepository customers, String email, String name) {
        return customers.save(new Customer(email, name)).getId();   // nowy obiekt → persist → id już ustawione
    }

    static String solution2(CustomerRepository customers, Long id) {
        return customers.findById(id)
                .map(Customer::getName)
                .orElse("nieznany klient");
    }

    static int solution3(CustomerRepository customers) {
        List<Customer> inactive = customers.findAll().stream()
                .filter(customer -> !customer.isActive())
                .toList();
        customers.deleteAll(inactive);
        return inactive.size();
        // W Jpa03QueryMethods zrobisz to lepiej: findByActiveFalse() albo deleteByActiveFalse() — filtr w SQL,
        // a nie w pamięci.
    }
}

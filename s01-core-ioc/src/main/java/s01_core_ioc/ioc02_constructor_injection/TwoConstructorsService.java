package s01_core_ioc.ioc02_constructor_injection;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Serwis z DWOMA konstruktorami i adnotacją {@code @Autowired} na jednym z nich — Spring wie, którego użyć.
 * Celowo BEZ {@code @Component}: lekcja rejestruje tę klasę ręcznie w teście (żeby aplikacja lekcji zawsze startowała).
 */
public class TwoConstructorsService {

    private final CustomerRepository repository;
    private final String prefix;

    @Autowired // „użyj tego konstruktora”
    public TwoConstructorsService(CustomerRepository repository) {
        this(repository, "Klient");
    }

    public TwoConstructorsService(CustomerRepository repository, String prefix) {
        this.repository = repository;
        this.prefix = prefix;
    }

    public String describe(long id) {
        return prefix + " " + repository.findById(id).map(Customer::lastName).orElse("?");
    }
}

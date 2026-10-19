package s01_core_ioc.ioc02_constructor_injection;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Serwis z DWOMA konstruktorami i adnotacją {@code @Autowired} na jednym z nich — Spring wie, którego użyć.
 * Ta klasa jest POPRAWNA — z {@code @Component} też by działała. Nie ma go tylko dlatego, że test sekcji 4 rejestruje
 * ją ręcznie, obok błędnych przykładów (AmbiguousConstructorsService, NoArgConstructorTrapService), żeby porównać
 * wszystkie trzy przypadki w małych kontekstach.
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

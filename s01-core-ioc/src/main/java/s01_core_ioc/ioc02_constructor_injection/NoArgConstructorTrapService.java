package s01_core_ioc.ioc02_constructor_injection;

/**
 * Demonstracja pułapki z sekcji 4: dwa konstruktory, żaden z {@code @Autowired}, a jeden z nich jest BEZ parametrów.
 * Spring nie zgłasza błędu — po cichu wybiera konstruktor bez parametrów, więc repozytorium NIE zostaje wstrzyknięte
 * (pole zostaje null). Celowo BEZ {@code @Component}: test rejestruje tę klasę ręcznie.
 */
public class NoArgConstructorTrapService {

    private final CustomerRepository repository;

    /** Konstruktor dodany „na potrzeby testu” — to właśnie jego wybierze Spring. */
    public NoArgConstructorTrapService() {
        this.repository = null;
    }

    public NoArgConstructorTrapService(CustomerRepository repository) {
        this.repository = repository;
    }

    /** Czy repozytorium zostało podane? (w kontekście Springa: false) */
    public boolean hasRepository() {
        return repository != null;
    }
}

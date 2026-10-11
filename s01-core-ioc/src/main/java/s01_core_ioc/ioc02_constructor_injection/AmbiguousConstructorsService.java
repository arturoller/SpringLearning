package s01_core_ioc.ioc02_constructor_injection;

/**
 * Dwa konstruktory, żaden bez parametrów i żaden z {@code @Autowired} — Spring nie ma jak wybrać.
 * Celowo BEZ {@code @Component}: błąd pokazujemy w teście, na ręcznie zbudowanym kontekście.
 */
public class AmbiguousConstructorsService {

    public AmbiguousConstructorsService(CustomerRepository repository) {
    }

    public AmbiguousConstructorsService(CustomerRepository repository, String prefix) {
    }
}

package s03_boot_basics.boot02_writing_auto_configuration;

/**
 * GreetingService = usługa powitań. Interfejs, który „biblioteka” (nasza autokonfiguracja) dostarcza z domyślną
 * implementacją, a użytkownik może podmienić własną — wystarczy, że zdefiniuje bean tego typu.
 */
public interface GreetingService {

    /** Powitanie klienta sklepu, np. „Witaj, Ania! Zapraszamy do sklepu Sklep Kursanta.”. */
    String greet(String customer);
}

package s03_boot_basics.boot02_writing_auto_configuration;

/**
 * DefaultGreetingService = domyślna usługa powitań. Celowo BEZ {@code @Component} (komponent): beanem staje się
 * tylko przez metodę {@code @Bean} w Boot02GreetingAutoConfiguration — i tylko wtedy, gdy warunki są spełnione.
 */
public class DefaultGreetingService implements GreetingService {

    private final String shopName;

    public DefaultGreetingService(String shopName) {
        this.shopName = shopName;
    }

    @Override
    public String greet(String customer) {
        return "Witaj, " + customer + "! Zapraszamy do sklepu " + shopName + ".";
    }
}

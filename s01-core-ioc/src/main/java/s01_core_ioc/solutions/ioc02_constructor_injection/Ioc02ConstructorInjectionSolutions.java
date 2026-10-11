package s01_core_ioc.solutions.ioc02_constructor_injection;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import s01_core_ioc.ioc02_constructor_injection.Customer;
import s01_core_ioc.ioc02_constructor_injection.CustomerRepository;
import s01_core_ioc.ioc02_constructor_injection.CustomerService;
import s01_core_ioc.ioc02_constructor_injection.InMemoryCustomerRepository;

import java.util.Optional;

/** Rozwiązania wzorcowe ćwiczeń lekcji Ioc02ConstructorInjection — zajrzyj dopiero po własnej próbie! */
public final class Ioc02ConstructorInjectionSolutions {

    private Ioc02ConstructorInjectionSolutions() {
    }

    static String solution1(long id) {
        return new CustomerService(new InMemoryCustomerRepository()).displayName(id);
    }

    static String solution2() {
        CustomerRepository stub = id -> Optional.of(new Customer(id, "Test", "Testowy"));
        return new CustomerService(stub).displayName(42);
    }

    static String solution3(long id) {
        try (var context = new AnnotationConfigApplicationContext(
                InMemoryCustomerRepository.class, CustomerService.class)) {
            return context.getBean(CustomerService.class).displayName(id);
        }
    }
}

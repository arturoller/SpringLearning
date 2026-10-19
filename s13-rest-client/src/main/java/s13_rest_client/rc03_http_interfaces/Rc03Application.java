package s13_rest_client.rc03_http_interfaces;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Klasa startowa lekcji Rc03HttpInterfaces. Włącza profil {@code rc03} (application-rc03.properties: adres
 * i nagłówek domyślny GRUPY klientów „dostawca”).
 *
 * <p>{@code @ImportHttpServices} (importuj usługi HTTP, Spring Framework 7) rejestruje interfejs SupplierApi w grupie
 * „dostawca”: Spring tworzy dla niej RestClient i proxy, a Boot 4 konfiguruje go z właściwości
 * {@code spring.http.serviceclient.dostawca.*}. Bean SupplierApi wstrzykujesz jak każdy inny.</p>
 */
@SpringBootApplication
@ImportHttpServices(group = "dostawca", types = SupplierApi.class)
public class Rc03Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Rc03Application.class)
                .profiles("rc03")
                .run(args);
        CheapestOfferService service = context.getBean(CheapestOfferService.class);
        System.out.println("Najtańsza dostępna elektronika u dostawcy: " + service.cheapestAvailable("ELEKTRONIKA"));
    }
}

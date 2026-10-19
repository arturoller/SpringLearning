package s13_rest_client.rc01_rest_client;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Klasa startowa lekcji Rc01RestClient. Włącza profil {@code rc01} (plik application-rc01.properties).
 * Po starcie aplikacja SAMA pyta „dostawcę” przez RestClient i wypisuje wynik w konsoli, a potem działa dalej —
 * możesz wysyłać zapytania z pliku s13-rest-client/http/rc01_rest_client.http.
 */
@SpringBootApplication
public class Rc01Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Rc01Application.class)
                .profiles("rc01")
                .run(args);
        SupplierClient client = context.getBean(SupplierClient.class);
        System.out.println("Sklep zapytał dostawcę przez RestClient: " + client.offer("ELE-001"));
    }
}

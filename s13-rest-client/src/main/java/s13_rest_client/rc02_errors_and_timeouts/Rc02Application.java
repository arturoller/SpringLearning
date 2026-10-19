package s13_rest_client.rc02_errors_and_timeouts;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Rc02ErrorsAndTimeouts. Włącza profil {@code rc02} (plik application-rc02.properties:
 * adres dostawcy i limity czasu klientów HTTP). Po starcie wysyłaj zapytania z pliku
 * s13-rest-client/http/rc02_errors_and_timeouts.http i patrz w konsolę — SupplierCallLoggingInterceptor loguje
 * każde wywołanie dostawcy.
 */
@SpringBootApplication
public class Rc02Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Rc02Application.class).profiles("rc02").run(args);
    }
}

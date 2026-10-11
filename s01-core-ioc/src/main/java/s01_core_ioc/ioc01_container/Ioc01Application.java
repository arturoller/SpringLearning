package s01_core_ioc.ioc01_container;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Ioc01Container. To aplikacja BEZ serwera WWW (moduł nie ma startera webmvc): uruchomiona,
 * zbuduje kontekst, wypisze log „Started Ioc01Application” i od razu się zakończy — nie ma na co czekać.
 */
@SpringBootApplication
public class Ioc01Application {

    public static void main(String[] args) {
        SpringApplication.run(Ioc01Application.class, args);
    }
}

package s01_core_ioc.ioc03_stereotypes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Klasa startowa lekcji Ioc03Stereotypes. Aplikacja bez serwera WWW: zbuduje kontekst i od razu się zakończy. */
@SpringBootApplication
public class Ioc03Application {

    public static void main(String[] args) {
        SpringApplication.run(Ioc03Application.class, args);
    }
}

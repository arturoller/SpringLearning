package s01_core_ioc.ioc06_scopes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Ioc06Scopes. Aplikacja bez serwera WWW: zbuduje kontekst, wypisze log i zakończy się.
 * Skanuje tylko ten pakiet (ioc06_scopes). Klasa Ioc06Exercise3Config nie ma adnotacji, więc nie jest skanowana.
 */
@SpringBootApplication
public class Ioc06Application {

    public static void main(String[] args) {
        SpringApplication.run(Ioc06Application.class, args);
    }
}

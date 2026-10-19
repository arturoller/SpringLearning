package s19_deploy.dep04_startup_and_shutdown;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Klasa startowa lekcji Dep04StartupAndShutdown. Uruchom ▶ i spróbuj zatrzymać aplikację (Ctrl+C w terminalu). */
@SpringBootApplication
public class Dep04Application {

    public static void main(String[] args) {
        SpringApplication.run(Dep04Application.class, args);
    }
}

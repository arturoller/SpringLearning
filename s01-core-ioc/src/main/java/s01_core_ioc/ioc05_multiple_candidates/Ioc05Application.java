package s01_core_ioc.ioc05_multiple_candidates;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Ioc05MultipleCandidates (aplikacja bez serwera WWW). Skanuje swój pakiet: trzy wysyłacze
 * i trzy usługi. Przykłady BŁĘDÓW (demonstracje i ćwiczenia) to klasy bez adnotacji — rejestrują je dopiero testy.
 */
@SpringBootApplication
public class Ioc05Application {

    public static void main(String[] args) {
        SpringApplication.run(Ioc05Application.class, args);
    }
}

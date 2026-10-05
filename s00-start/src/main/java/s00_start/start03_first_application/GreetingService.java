package s00_start.start03_first_application;

import org.springframework.stereotype.Service;

/**
 * GreetingService = serwis powitań. {@code @Service} (serwis) oznacza klasę z logiką biznesową — Spring znajdzie ją
 * przy skanowaniu pakietu i utworzy z niej JEDEN bean, który wstrzyknie wszędzie, gdzie jest potrzebny.
 */
@Service
public class GreetingService {

    /** greet = przywitaj. Logika jest tutaj, a nie w kontrolerze — kontroler tylko tłumaczy HTTP na wywołanie metody. */
    public String greet(String name) {
        return "Cześć, " + name + "!";
    }
}

package s20_modulith.mod03_documentation;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Klasa startowa lekcji Mod03Documentation — korzeń aplikacji z modułami {@code products}, {@code orders}
 * (z nazwanym interfejsem {@code events}) i {@code inventory}. Lekcja nie potrzebuje uruchamiać Springa: analiza
 * modułów i dokumentacja powstają z samych skompilowanych klas.
 * Uruchomienie (▶ przy main): wypisuje opis modułów — to samo, co potem trafia na diagramy i kanwy.
 */
@SpringBootApplication
public class Mod03Application {

    public static void main(String[] args) {
        System.out.println(ApplicationModules.of(Mod03Application.class));
    }
}

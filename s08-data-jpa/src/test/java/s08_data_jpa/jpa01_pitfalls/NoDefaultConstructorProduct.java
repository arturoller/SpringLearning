package s08_data_jpa.jpa01_pitfalls;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Pułapka a) z sekcji 5 lekcji Jpa01Entities: encja BEZ konstruktora bez argumentów. Nie rób tak. */
@Entity
public class NoDefaultConstructorProduct {

    @Id
    private Long id;

    private String name;

    public NoDefaultConstructorProduct(Long id, String name) { // jedyny konstruktor — z argumentami
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

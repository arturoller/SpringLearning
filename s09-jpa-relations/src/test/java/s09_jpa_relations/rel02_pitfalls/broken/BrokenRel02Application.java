package s09_jpa_relations.rel02_pitfalls.broken;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import s09_jpa_relations.rel02_one_to_many.Product;

/**
 * Osobna aplikacja z CELOWO złymi encjami do PUŁAPEK lekcji Rel02OneToMany (toString w pętli, CascadeType.REMOVE
 * na @ManyToOne). Leży w pakiecie TESTOWYM rel02_pitfalls, POZA rel02_one_to_many — inaczej klasa startowa lekcji
 * zeskanowałaby te encje razem z poprawnymi. Encje: z tego pakietu + Product lekcji ({@code @EntityScan}).
 * Mapują te same tabele co lekcja (profil rel02, migracje rel02, ddl-auto=validate).
 */
@SpringBootApplication
@EntityScan(basePackageClasses = {BrokenRel02Application.class, Product.class})
public class BrokenRel02Application {
}

package s09_jpa_relations.rel03_pitfalls;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import s09_jpa_relations.rel03_many_to_many.Tag;

/**
 * Osobna aplikacja z CELOWO złymi encjami do PUŁAPEK lekcji Rel03ManyToMany ({@code List} przy @ManyToMany,
 * kaskada na @ManyToMany). Leży w pakiecie TESTOWYM rel03_pitfalls, POZA rel03_many_to_many — klasa startowa lekcji
 * ich nie widzi. Encje: z tego pakietu + encje lekcji (Tag i reszta, {@code @EntityScan}). Mapują te same tabele co
 * lekcja (profil rel03, migracje rel03, ddl-auto=validate).
 */
@SpringBootApplication
@EntityScan(basePackageClasses = {BrokenRel03Application.class, Tag.class})
public class BrokenRel03Application {
}

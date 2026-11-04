package s09_jpa_relations.rel02_pitfalls.nomappedby;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import s09_jpa_relations.rel02_one_to_many.OrderLine;

/**
 * Celowo zepsuta aplikacja do pytania 3 lekcji Rel02OneToMany: lista pozycji BEZ {@code mappedBy}. Encje: z tego
 * pakietu + encje lekcji (OrderLine i Product). Schemat z migracji rel02, {@code ddl-auto=validate}.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = {NoMappedByApplication.class, OrderLine.class})
public class NoMappedByApplication {
}

package s09_jpa_relations.rel02_pitfalls.jointable;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Osobna aplikacja do sekcji 2 lekcji Rel02OneToMany: jednokierunkowe {@code @OneToMany}. Tu schemat tworzy
 * SAM Hibernate ({@code ddl-auto=create-drop}, Flyway wyłączony), żeby pokazać, JAKICH tabel chce dla takiego
 * mapowania. Leży w pakiecie testowym obok lekcji (nie w niej), bo inaczej lekcja z ddl-auto=validate by nie wstała.
 */
@SpringBootApplication
public class JoinTableApplication {
}

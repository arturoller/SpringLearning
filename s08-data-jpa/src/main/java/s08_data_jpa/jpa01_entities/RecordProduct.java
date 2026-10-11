package s08_data_jpa.jpa01_entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * RecordProduct — ZŁY PRZYKŁAD: rekord (record) jako encja. Kompiluje się i aplikacja startuje, zapis (persist) też
 * działa. Ale rekord nie ma konstruktora bez argumentów, więc ODCZYT z bazy kończy się wyjątkiem
 * {@code org.hibernate.InstantiationException: No default constructor for entity} — sekcja 5 lekcji i jej test.
 * Rekordy świetnie nadają się za to na DTO i projekcje (lekcja Jpa04CustomQueries).
 */
@Entity
@Table(name = "record_products")
public record RecordProduct(@Id Long id, String name) {
}

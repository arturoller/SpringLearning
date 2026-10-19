package s08_data_jpa.jpa01_entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PRZYKŁAD BŁĘDNY (do sekcji 1 lekcji) — rekord (record) jako encja.
 *
 * <p>Kompiluje się i — co zaskakuje — aplikacja STARTUJE: Hibernate 7.4 wypisuje tylko komunikat INFO
 * {@code HHH000182: No default (no-argument) constructor for class [...RecordProduct]} i tworzy tabelę.
 * Zapis (INSERT) też działa. Wybucha dopiero PIERWSZY ODCZYT z bazy:
 * {@code org.hibernate.InstantiationException: No default constructor for entity '...RecordProduct'}
 * (instantiation = tworzenie obiektu; no default constructor = brak konstruktora domyślnego, bez argumentów).
 * Sprawdzone testem sekcji 1.</p>
 *
 * <p>Rekord nie nadaje się na encję, bo: nie ma konstruktora bez argumentów, jego pola są final (Hibernate nie
 * wpisze wartości po utworzeniu obiektu, a my nie zmienimy ceny przez setter), a sama klasa jest final
 * (Hibernate nie zbuduje podklasy-pośrednika do leniwego ładowania). Rekordy świetnie za to nadają się na
 * DTO i projekcje — wyniki zapytań tylko do odczytu (lekcja s08_data_jpa/Jpa04CustomQueries).</p>
 */
@Entity
@Table(name = "record_products")
public record RecordProduct(@Id String sku, String name) {
}

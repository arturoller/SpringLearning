package s09_jpa_relations.rel01_many_to_one;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Klient sklepu — strona „jeden” relacji klient 1—N zamówienie. Celowo NIE ma tu listy zamówień: relacja jest
 * jednokierunkowa (unidirectional), zna ją tylko {@link ShopOrder}. Tabela {@code customer} nie ma żadnej kolumny
 * o zamówieniach — klucz obcy siedzi w tabeli {@code orders}. Lista zamówień w kliencie to temat Rel02.
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    protected Customer() { // dla Hibernate
    }

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    /** equals po id z obsługą null, stały hashCode — dlaczego tak: s08_data_jpa/Jpa01Entities, sekcja 5. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Customer other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Customer.class.hashCode();
    }

    @Override
    public String toString() {
        return "Customer[id=" + id + ", name=" + name + "]";
    }
}

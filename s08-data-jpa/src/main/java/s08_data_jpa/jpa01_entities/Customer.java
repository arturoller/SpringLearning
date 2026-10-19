package s08_data_jpa.jpa01_entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Klient sklepu — encja z kluczem {@code GenerationType.IDENTITY} (identity = tożsamość: kolumna, której wartość
 * nadaje SAMA BAZA przy wstawianiu wiersza, jak AUTO_INCREMENT w MySQL). Porównanie z sekwencją: sekcja 2 lekcji.
 *
 * <p>Klient nie ma naturalnego klucza biznesowego (e-mail bywa pusty i można go zmienić), więc equals/hashCode
 * opiera się na id — ale w BEZPIECZNEJ wersji (patrz komentarz przy equals).</p>
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // strategy = strategia; baza nadaje id przy INSERT
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String city;

    private String email;                 // może być null (np. Maria Nowak nie podała e-maila); domyślnie varchar(255)

    private boolean vip;

    protected Customer() {
    }

    public Customer(String name, String city, String email, boolean vip) {
        this.name = name;
        this.city = city;
        this.email = email;
        this.vip = vip;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getEmail() {
        return email;
    }

    public boolean isVip() {
        return vip;
    }

    /**
     * Bezpieczna równość po id: dwie encje są równe tylko wtedy, gdy OBIE mają już id i jest ono takie samo.
     * Nowe (niezapisane) obiekty są równe wyłącznie same sobie — nie „sklejają się” w HashSet jak w {@link Employee}.
     */
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

    /**
     * Stały hashCode (taki sam dla wszystkich klientów): nie zmienia się w chwili, gdy Hibernate nada id.
     * Koszt: wszystkie obiekty trafiają do jednego „kubełka” HashSet — przy kilkuset encjach w jednym zbiorze
     * nie ma to znaczenia. Wzór ten polecają m.in. autorzy Hibernate.
     */
    @Override
    public int hashCode() {
        return Customer.class.hashCode();
    }

    @Override
    public String toString() {
        return "Customer[id=" + id + ", name=" + name + "]";
    }
}

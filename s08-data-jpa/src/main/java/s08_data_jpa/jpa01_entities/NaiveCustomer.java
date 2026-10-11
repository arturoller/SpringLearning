package s08_data_jpa.jpa01_entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * NaiveCustomer = klient z NAIWNYM equals/hashCode (naive = naiwny) — przykład PUŁAPKI z sekcji 6 lekcji.
 * hashCode liczony z id, a id nadaje baza dopiero przy zapisie. Obiekt dodany do HashSet PRZED zapisem „gubi się”
 * w zbiorze PO zapisie, bo zmienił mu się hashCode. Poprawna wersja: Customer.
 */
@Entity
@Table(name = "naive_customers")
public class NaiveCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    protected NaiveCustomer() {
    }

    public NaiveCustomer(String email) {
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NaiveCustomer other && Objects.equals(id, other.id);   // ŹLE: dwa nowe obiekty są „równe”
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);                                               // ŹLE: zmienia się po zapisie
    }
}

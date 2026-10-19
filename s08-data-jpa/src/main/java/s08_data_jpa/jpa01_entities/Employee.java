package s08_data_jpa.jpa01_entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * PRZYKŁAD BŁĘDNY (do sekcji 7 lekcji) — NIE kopiuj tego equals/hashCode do swoich encji.
 *
 * <p>Pracownik ma equals/hashCode wygenerowane „z automatu” z pola id, które nadaje baza danych.
 * Skutki (sprawdzone testem):</p>
 * <ul>
 *   <li>dwaj RÓŻNI nowi pracownicy (id == null) są sobie „równi” — w HashSet zostaje tylko jeden;</li>
 *   <li>pracownik dodany do HashSet PRZED zapisem „znika” z niego PO zapisie: hashCode zmienił się z
 *       {@code Objects.hashCode(null)} na hash nadanego id, więc HashSet szuka go w innym kubełku.</li>
 * </ul>
 * Poprawne wzorce: {@link Product} (klucz biznesowy) i {@link Customer} (bezpieczne id + stały hashCode).
 */
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String department;

    private int salary;                    // pensja w złotych (liczba całkowita — wystarczy do przykładów)

    protected Employee() {
    }

    public Employee(String name, String department, int salary) {
        this.name = name;
        this.department = department;
        this.salary = salary;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public int getSalary() {
        return salary;
    }

    // BŁĄD: równość i hash z pola, które zmienia się z null na liczbę w chwili zapisu do bazy.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Employee other)) {
            return false;
        }
        return Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

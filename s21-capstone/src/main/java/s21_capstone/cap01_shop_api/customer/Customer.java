package s21_capstone.cap01_shop_api.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Encja klienta. Celowo BEZ listy zamówień: relacja jest jednokierunkowa (zamówienie → klient). Lista „zamówienia
 * klienta” to zapytanie, a nie pole encji — nie ma cyklu klient ↔ zamówienie i nie ma pokusy ładowania wszystkich
 * zamówień przy każdym kliencie (s09_jpa_relations/Rel02OneToMany).
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(unique = true, length = 100)
    private String email;   // może być null — nie każdy klient podał email

    @Column(nullable = false)
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
}

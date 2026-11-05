package s09_jpa_relations.rel02_pitfalls.nomappedby;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import s09_jpa_relations.rel02_one_to_many.OrderLine;

import java.util.ArrayList;
import java.util.List;

/** ZŁA encja: {@code @OneToMany} bez mappedBy — Hibernate uzna listę za osobną relację z tabelą łączącą. */
@Entity
@Table(name = "orders")
public class NoMappedByOrder {

    @Id
    private Long id;

    @Column(name = "order_number")
    private String orderNumber;

    @OneToMany // brak mappedBy = "order"!
    private List<OrderLine> lines = new ArrayList<>();

    protected NoMappedByOrder() {
    }
}

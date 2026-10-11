package s08_data_jpa.zz_scratch;
import jakarta.persistence.*;
@Entity
public class AutoEnt {
    @Id @GeneratedValue Long id;
    String unitPrice;
    protected AutoEnt() {}
}

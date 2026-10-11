package s08_data_jpa.zz_scratch;
import jakarta.persistence.*;
@Entity
public record RecEnt(@Id Long id, String name) {}

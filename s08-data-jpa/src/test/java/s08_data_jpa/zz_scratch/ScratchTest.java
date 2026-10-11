package s08_data_jpa.zz_scratch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
@DataJpaTest
class ScratchTest {
    @Autowired EntityManagerFactory emf;
    @Test void t() {
        emf.getProperties().forEach((k,v) -> { if (k.toString().contains("naming")) System.out.println("XXX " + k + "=" + v); });
    }
}

package s08_data_jpa.zz_scratch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
@DataJpaTest
class ScratchTest {
    @Autowired TestEntityManager em;
    @Test void t() {
        RecEnt n = em.persistAndFlush(new RecEnt(1L, "a"));
        em.clear();
        try { System.out.println("XXX found " + em.find(RecEnt.class, 1L)); }
        catch (Exception e) { System.out.println("XXX " + e.getClass().getName() + ": " + e.getMessage()); }
    }
}

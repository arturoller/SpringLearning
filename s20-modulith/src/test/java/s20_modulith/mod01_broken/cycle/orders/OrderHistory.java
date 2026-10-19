package s20_modulith.mod01_broken.cycle.orders;

import org.springframework.stereotype.Component;

import java.util.List;

/** Historia zamówień modułu orders (zła aplikacja z lekcji Mod01Modules). */
@Component
public class OrderHistory {

    public List<String> mostOrderedSkus() {
        return List.of("ELE-001", "KSI-001");
    }
}

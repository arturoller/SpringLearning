package s20_modulith.mod03_broken.named_interface.orders;

import org.springframework.stereotype.Service;

/** Interfejs nienazwany modułu orders (zła aplikacja z lekcji Mod03Documentation). */
@Service
public class OrderManagement {

    public int countOrders() {
        return 3;
    }
}

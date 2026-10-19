package s20_modulith.mod01_broken.not_allowed.customers;

import org.springframework.stereotype.Service;

/** API modułu customers (zła aplikacja z lekcji Mod01Modules). */
@Service
public class CustomerDirectory {

    public boolean isVip(long customerId) {
        return customerId == 1 || customerId == 4;
    }
}

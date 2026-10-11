package s01_core_ioc.ioc02_constructor_injection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * SetterInjectedCustomerService = wstrzykiwanie przez settera (metodę ustawiającą). Sensowne dla zależności
 * OPCJONALNYCH: jest wartość domyślna (null), a brak beana nie przewraca aplikacji.
 */
@Service
public class SetterInjectedCustomerService {

    private final CustomerService customers;
    private VipPolicy vipPolicy; // opcjonalna: może zostać null

    public SetterInjectedCustomerService(CustomerService customers) {
        this.customers = customers;
    }

    @Autowired(required = false) // required = wymagane; false → brak beana VipPolicy nie jest błędem
    public void setVipPolicy(VipPolicy vipPolicy) {
        this.vipPolicy = vipPolicy;
    }

    /** Nazwa klienta, z gwiazdką dla VIP-a, jeśli zasada VIP jest dostępna. */
    public String label(long id) {
        String name = customers.displayName(id);
        return vipPolicy != null && vipPolicy.isVip(id) ? "★ " + name : name;
    }
}

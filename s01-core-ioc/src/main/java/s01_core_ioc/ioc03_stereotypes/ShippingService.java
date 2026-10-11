package s01_core_ioc.ioc03_stereotypes;

import org.springframework.stereotype.Service;

/** Serwis wysyłki — drugi {@code @Service} w lekcji (potrzebny w ćwiczeniu 3). Nazwa beana: "shippingService". */
@Service
public class ShippingService {

    public int shippingCostGrosze(int weightKg) {
        return 999 + weightKg * 100;
    }
}

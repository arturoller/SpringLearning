package s01_core_ioc.ioc03_stereotypes;

/** Rabat lojalnościowy — bean dzięki WŁASNEJ adnotacji {@code @ShopComponent}. Nazwa: "loyaltyDiscount". */
@ShopComponent
public class LoyaltyDiscount {

    public int percentFor(int completedOrders) {
        return Math.min(completedOrders, 10);
    }
}

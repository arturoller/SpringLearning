package s01_core_ioc.ioc02_constructor_injection;

/**
 * VipPolicy = zasada rozpoznawania klientów VIP. W tej lekcji NIE MA żadnego beana tego typu — to zależność
 * opcjonalna (patrz SetterInjectedCustomerService).
 */
@FunctionalInterface
public interface VipPolicy {

    boolean isVip(long customerId);
}

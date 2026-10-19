package s20_modulith.mod01_modules.customers;

/**
 * Widok klienta udostępniany innym modułom (API modułu {@code customers}).
 *
 * @param id   identyfikator klienta
 * @param name imię i nazwisko
 * @param city miasto
 * @param vip  czy klient ma status VIP
 */
public record CustomerView(long id, String name, String city, boolean vip) {
}

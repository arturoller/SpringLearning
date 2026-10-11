package s01_core_ioc.ioc02_constructor_injection;

/**
 * Customer = klient sklepu. To {@code record} (rekord — zwięzła klasa na dane, niezmienna), więc nie ma tu
 * ani setterów, ani zmiennych pól.
 */
public record Customer(long id, String firstName, String lastName) {
}

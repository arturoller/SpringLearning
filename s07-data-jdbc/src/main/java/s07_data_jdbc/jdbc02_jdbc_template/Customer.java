package s07_data_jdbc.jdbc02_jdbc_template;

/**
 * Klient sklepu — niezmienny rekord (record).
 *
 * @param id   klucz główny
 * @param name imię i nazwisko
 * @param city miasto
 */
public record Customer(long id, String name, String city) {
}

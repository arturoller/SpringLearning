package s07_data_jdbc.jdbc02_jdbc_template;

/** Klient sklepu. {@code email} bywa {@code null} (Maria Nowak i Ola Pawlak nie podały adresu). */
public record Customer(long id, String name, String city, String email, boolean vip) {
}

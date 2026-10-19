package s08_data_jpa.jpa01_entities;

import java.math.BigDecimal;

/**
 * Zamówienie sklepu — SZKIELET do ćwiczeń 1 i 2 lekcji Jpa01Entities (opis ćwiczeń: na końcu klasy lekcji).
 * Na razie to zwykła klasa Javy: Hibernate jej nie zna, więc zapis kończy się błędem „nie jest encją”.
 *
 * <p>Docelowo: encja w tabeli {@code orders}; klucz główny = numer zamówienia nadawany przez sklep
 * (np. {@code "ZAM-001"}, BEZ {@code @GeneratedValue}); status zapisany jako tekst; kwota jako numeric(10,2).</p>
 */
// TODO ĆWICZENIE 1: adnotacje encji i tabeli (podpowiedź: ORDER to słowo zastrzeżone SQL — tabela "orders")
public class ShopOrder {

    // TODO ĆWICZENIE 1: klucz główny (numer nadaje sklep, nie baza)
    private String number;

    // TODO ĆWICZENIE 1: zapis jako nazwa stałej (tekst), nie numer pozycji
    private OrderStatus status;

    // TODO ĆWICZENIE 1: numeric(10,2), NOT NULL
    private BigDecimal total;

    protected ShopOrder() {
    }

    public ShopOrder(String number, OrderStatus status, BigDecimal total) {
        this.number = number;
        this.status = status;
        this.total = total;
    }

    public String getNumber() {
        return number;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    // TODO ĆWICZENIE 2: equals i hashCode po kluczu biznesowym (numer zamówienia)
}

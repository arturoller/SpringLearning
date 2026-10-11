package s04_web_rest.rest04_json;

/**
 * ShelfLabel = etykieta na półkę — ZWYKŁA KLASA z getterami (nie rekord), do sekcji 4 lekcji Rest04Json.
 * Gettery są celowo w kolejności price, name, barcode. Klasa NIE MA konstruktora z parametrami (tylko domyślny
 * i metodę fabryczną of), więc żadna właściwość nie jest „parametrem konstruktora” — Jackson 3 sortuje wtedy
 * wszystkie alfabetycznie: barcode, name, price.
 */
public class ShelfLabel {

    private int price;
    private String name;
    private String barcode;

    /** Metoda fabryczna (factory method = metoda tworząca obiekt). Jackson jej nie używa — to nie konstruktor. */
    public static ShelfLabel of(int price, String name, String barcode) {
        ShelfLabel label = new ShelfLabel();
        label.price = price;
        label.name = name;
        label.barcode = barcode;
        return label;
    }

    public int getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }

    public String getBarcode() {
        return barcode;
    }
}

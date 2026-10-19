package s11_testing.test02_mockito_beans;

/**
 * OrderConfirmation = potwierdzenie zamówienia: kto zamówił, co, ile sztuk i za ile (w groszach).
 * Ten obiekt serwis przekazuje do {@link OrderNotifier} — w testach „łapiemy” go {@code ArgumentCaptor}.
 */
public record OrderConfirmation(String customerEmail, String sku, int quantity, long totalGrosze) {
}

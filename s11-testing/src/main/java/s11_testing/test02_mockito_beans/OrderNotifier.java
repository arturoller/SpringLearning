package s11_testing.test02_mockito_beans;

/**
 * OrderNotifier = powiadamiacz o zamówieniu (interfejs). W prawdziwej aplikacji wysłałby e-mail — to EFEKT
 * UBOCZNY, którego w testach NIE chcemy (nikt nie powinien dostać maila z testu). Dlatego w testach to atrapa.
 */
public interface OrderNotifier {

    void orderPlaced(OrderConfirmation confirmation);
}

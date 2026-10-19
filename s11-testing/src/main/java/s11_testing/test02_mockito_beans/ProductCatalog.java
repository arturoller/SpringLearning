package s11_testing.test02_mockito_beans;

/**
 * ProductCatalog = katalog produktów (interfejs). Prawdziwa implementacja: {@link InMemoryProductCatalog}.
 * W testach zastępujemy go atrapą (mock), szpiegiem (spy) albo własnym obiektem ({@code @TestBean}).
 */
public interface ProductCatalog {

    /** Produkt o podanym SKU; brak → {@link java.util.NoSuchElementException} „Nieznany produkt: …”. */
    Product getBySku(String sku);
}

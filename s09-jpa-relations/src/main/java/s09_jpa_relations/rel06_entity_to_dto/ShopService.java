package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Serwis sklepu. {@code @Transactional(readOnly = true)} — każda metoda w transakcji tylko do odczytu.
 * Dwie metody „ZŁE” (zwracają encje — używa ich EntityController) i dwie „DOBRE” (mapują na DTO wewnątrz
 * transakcji — używa ich DtoController).
 */
@Service
@Transactional(readOnly = true)
public class ShopService {

    private final ProductRepository products;
    private final CustomerRepository customers;
    private final CustomerMapper customerMapper;

    public ShopService(ProductRepository products, CustomerRepository customers, CustomerMapper customerMapper) {
        this.products = products;
        this.customers = customers;
        this.customerMapper = customerMapper;
    }

    // ---- ŹLE: encja wychodzi poza transakcję -------------------------------------------------------------

    public Product productEntity(String sku) {
        return products.findBySku(sku).orElseThrow(() -> new NoSuchElementException("Brak produktu " + sku));
    }

    public Customer customerEntity(Long id) {
        return customers.findById(id).orElseThrow(() -> new NoSuchElementException("Brak klienta " + id));
    }

    // ---- DOBRZE: DTO zbudowane w transakcji ----------------------------------------------------------------

    public ProductDto product(String sku) {
        return ProductDto.from(productEntity(sku));
    }

    public CustomerDto customer(Long id) {
        Customer customer = customers.findWithOrdersById(id)                       // 1 zapytanie z join fetch
                .orElseThrow(() -> new NoSuchElementException("Brak klienta " + id));
        return customerMapper.toDto(customer);                                     // sesja otwarta — bezpiecznie
    }
}

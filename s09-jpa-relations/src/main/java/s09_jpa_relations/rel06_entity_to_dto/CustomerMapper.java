package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * CustomerMapper = maper (tłumacz) encja → DTO jako OSOBNA klasa (bean Springa). Drugi sposób mapowania obok
 * metody {@code ProductDto.from}: przydaje się, gdy mapowanie jest większe, ma zależności albo chcemy, żeby DTO
 * nic nie wiedziało o encjach.
 *
 * <p>UWAGA: {@code toDto} czyta leniwą kolekcję {@code customer.getOrders()} — wołaj go WEWNĄTRZ transakcji
 * (w serwisie), na encji wczytanej z odpowiednim join fetch.</p>
 */
@Component
public class CustomerMapper {

    public CustomerDto toDto(Customer customer) {
        return new CustomerDto(
                customer.getName(),
                customer.getCity(),
                customer.getEmail(),
                customer.getOrders().stream()
                        .sorted(Comparator.comparing(ShopOrder::getNumber))   // stała kolejność w odpowiedzi
                        .map(order -> new CustomerDto.OrderDto(order.getNumber(), order.getStatus()))
                        .toList());
    }
}

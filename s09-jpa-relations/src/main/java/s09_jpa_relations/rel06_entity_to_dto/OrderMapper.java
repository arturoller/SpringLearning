package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.stereotype.Component;

/**
 * Maper (mapper — „tłumacz” encja → DTO) zamówień — styl 2 z sekcji 2: osobna klasa zamiast metody {@code from}
 * w DTO. Zwykły bean bez dostępu do bazy: dostaje encję, oddaje rekord. Woła się go W TRANSAKCJI serwisu — wtedy
 * leniwe relacje mogą się doczytać (choć dobrze napisany serwis wczytuje je wcześniej jednym zapytaniem).
 */
@Component // component = komponent; bean (ziarno — obiekt zarządzany przez Springa)
public class OrderMapper {

    public OrderDetailsDto toDetails(ShopOrder order) {
        return new OrderDetailsDto(
                order.getOrderNumber(),
                order.getCustomer().getName(),
                order.getStatus(),
                order.getNote(),
                order.getLines().stream().map(OrderLineDto::from).toList(),
                order.total());
    }
}

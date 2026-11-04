package s09_jpa_relations.rel06_entity_to_dto;

/**
 * DTO (data transfer object — obiekt do przenoszenia danych) klienta: TYLKO to, co wolno pokazać na zewnątrz.
 * Rekord (record) jest niezmienny i nie ma relacji — nic się w nim nie doczyta leniwie i nie zapętli.
 * Mapowanie ręczne, metodą statyczną {@code from} w samym DTO (styl 1 z sekcji 2).
 */
public record CustomerDto(Long id, String name, String email) {

    public static CustomerDto from(Customer customer) {
        return new CustomerDto(customer.getId(), customer.getName(), customer.getEmail());
    }
}

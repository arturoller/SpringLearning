package s21_capstone.cap01_shop_api.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Strona wyników w odpowiedzi API — NASZ rekord zamiast obiektu {@code Page} ze Spring Data.
 *
 * <p>Dlaczego nie zwracać {@code Page} wprost? Jego JSON to szczegół biblioteki (pola pageable, sort, empty...), który
 * zmieniał się między wersjami — Spring Data sam ostrzega w logu, że serializowanie {@code PageImpl} „tak jak jest”
 * nie jest wspierane. Własny rekord to stała umowa z klientem: zawartość + numer strony + rozmiar + liczby.</p>
 *
 * @param content       elementy tej strony
 * @param page          numer strony (od ZERA)
 * @param size          rozmiar strony
 * @param totalElements ile elementów pasuje w sumie
 * @param totalPages    ile jest stron
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}

package s21_capstone.cap01_shop_api.discount;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium kodów rabatowych (klucz = kod). Gotowe — używasz go w ĆWICZENIU 1. */
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, String> {
}

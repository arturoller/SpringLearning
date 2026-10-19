package s08_data_jpa.jpa02_repositories;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium kodów rabatowych — klucz typu String nadawany ręcznie (patrz DiscountCode). */
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, String> {
}

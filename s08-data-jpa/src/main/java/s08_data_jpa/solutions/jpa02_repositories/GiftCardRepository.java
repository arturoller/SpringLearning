package s08_data_jpa.solutions.jpa02_repositories;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium kart z rozwiązania ćwiczenia 3. */
public interface GiftCardRepository extends JpaRepository<GiftCard, String> {
}

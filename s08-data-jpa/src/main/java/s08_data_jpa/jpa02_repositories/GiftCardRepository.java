package s08_data_jpa.jpa02_repositories;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium kart podarunkowych (ćwiczenie 3 lekcji Jpa02Repositories). */
public interface GiftCardRepository extends JpaRepository<GiftCard, String> {
}

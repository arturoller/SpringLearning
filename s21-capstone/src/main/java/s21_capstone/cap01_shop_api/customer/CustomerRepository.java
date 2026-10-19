package s21_capstone.cap01_shop_api.customer;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium klientów. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmail(String email);
}

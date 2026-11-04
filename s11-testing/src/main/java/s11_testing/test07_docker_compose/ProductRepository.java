package s11_testing.test07_docker_compose;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repozytorium produktów lekcji Test07DockerCompose. */
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByNameAsc();
}

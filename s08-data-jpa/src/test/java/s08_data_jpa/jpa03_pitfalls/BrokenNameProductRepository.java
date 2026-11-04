package s08_data_jpa.jpa03_pitfalls;

import org.springframework.data.jpa.repository.JpaRepository;
import s08_data_jpa.jpa03_query_methods.Product;

import java.util.List;

/** Sekcja 3 lekcji Jpa03QueryMethods: repozytorium z literówką w nazwie metody (nmae zamiast name). */
public interface BrokenNameProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNmae(String name); // literówka: encja Product nie ma pola „nmae”
}

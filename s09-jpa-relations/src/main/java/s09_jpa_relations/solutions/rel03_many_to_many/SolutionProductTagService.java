package s09_jpa_relations.solutions.rel03_many_to_many;

import org.springframework.stereotype.Service;
import s09_jpa_relations.rel03_many_to_many.Product;
import s09_jpa_relations.rel03_many_to_many.Tag;
import s09_jpa_relations.rel03_many_to_many.TagRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rel03ManyToMany (ProductTagExercises) — zajrzyj dopiero po własnej próbie! */
@Service
public class SolutionProductTagService {

    private final SolutionProductRepository products;
    private final TagRepository tags;
    private final SolutionProductSupplierRepository productSuppliers;

    public SolutionProductTagService(SolutionProductRepository products, TagRepository tags,
                                     SolutionProductSupplierRepository productSuppliers) {
        this.products = products;
        this.tags = tags;
        this.productSuppliers = productSuppliers;
    }

    public void tagProduct(Long productId, String tagName) {
        Product product = products.findById(productId).orElseThrow();
        Tag tag = tags.findByName(tagName).orElseGet(() -> tags.save(new Tag(tagName))); // nowy tag zapisany świadomie
        product.addTag(tag); // Set: drugi raz ten sam tag nic nie zmienia
    }

    public void untagProduct(Long productId, String tagName) {
        Product product = products.findById(productId).orElseThrow();
        tags.findByName(tagName).ifPresent(product::removeTag);
    }

    public List<String> productNamesWithAnyTag(Collection<String> tagNames) {
        return products.findNamesWithAnyTag(tagNames);
    }

    public List<String> productNamesWithAllTags(Collection<String> tagNames) {
        Set<String> distinctNames = Set.copyOf(tagNames); // powtórzona nazwa w argumencie nie może zawyżyć licznika
        return products.findNamesWithAllTags(distinctNames, distinctNames.size());
    }

    public Optional<String> cheapestSupplierName(Long productId) {
        return productSuppliers.findFirstByProductIdOrderByPurchasePriceAsc(productId)
                .map(link -> link.getSupplier().getName()); // leniwy dostawca — SELECT przy getName()
    }
}

package s07_data_jdbc.jdbc05_repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 6 lekcji Jdbc05Repository. Tu potrzebny jest PEŁNY kontekst aplikacji ({@code @SpringBootTest}), bo chodzi
 * o postprocesor, który Boot rejestruje tylko w pełnej aplikacji, a nie w wycinku {@code @JdbcTest}.
 */
@SpringBootTest
@ActiveProfiles("jdbc05")
class Jdbc05RepositoryFullContextTest {

    @Autowired
    private ProductRepository repository;

    @Test
    @DisplayName("6. W pełnej aplikacji bean z @Repository jest proxy (CGLIB) tłumaczącym wyjątki trwałości")
    void section6() {
        assertThat(AopUtils.isAopProxy(repository)).isTrue();
        assertThat(AopUtils.isCglibProxy(repository)).isTrue();
        assertThat(AopUtils.getTargetClass(repository)).isEqualTo(ProductRepository.class);
        assertThat(repository.count()).isEqualTo(14);   // i działa normalnie
    }
}

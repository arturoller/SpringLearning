package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Test01TestSlices: mniejsze wycinki. {@code @JsonTest} — tylko Jackson i testery JSON;
 * {@code @JdbcTest} — tylko DataSource, JdbcClient/JdbcTemplate i transakcje, BEZ JPA.
 * {@code @Nested} (zagnieżdżona klasa testowa) — każda ma własną adnotację, więc własny wycinek i własny kontekst.
 */
class Test01TestSlicesJsonJdbcTest {

    @Nested
    @JsonTest
    class JsonSlice {

        @Autowired
        JacksonTester<ProductDto> json;   // JacksonTester = tester JSON-a; Boot wstrzykuje go w @JsonTest

        @Autowired
        ApplicationContext context;

        @Test
        @DisplayName("4. @JsonTest: rekord → JSON i z powrotem; serwisów i repozytoriów nie ma")
        void jsonSlice() throws Exception {
            var dto = new ProductDto(6L, "Czekolada gorzka", 749, "7.49");

            assertThat(json.write(dto)).extractingJsonPathStringValue("$.priceZl").isEqualTo("7.49");
            assertThat(json.parseObject("""
                    {"id":6,"name":"Czekolada gorzka","priceGrosze":749,"priceZl":"7.49"}
                    """)).isEqualTo(dto);
            assertThat(context.getBeanNamesForType(ProductService.class)).isEmpty();
            assertThat(context.getBeanNamesForType(ProductRepository.class)).isEmpty();
        }
    }

    @Nested
    @JdbcTest
    class JdbcSlice {

        @Autowired
        JdbcClient jdbc;

        @Autowired
        ApplicationContext context;

        @Test
        @DisplayName("4. @JdbcTest: JdbcClient jest, ale JPA nie startuje — nie ma repozytorium ani tabeli PRODUCT")
        void jdbcSlice() {
            assertThat(context.getBeanNamesForType(ProductRepository.class)).isEmpty();
            Integer tables = jdbc.sql("select count(*) from information_schema.tables where table_name = 'PRODUCT'")
                    .query(Integer.class)
                    .single();
            assertThat(tables).as("Hibernate nie działa w @JdbcTest, więc nie utworzył tabeli z encji").isZero();
        }
    }
}

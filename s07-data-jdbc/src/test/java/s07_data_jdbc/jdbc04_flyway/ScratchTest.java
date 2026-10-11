package s07_data_jdbc.jdbc04_flyway;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("jdbc04")
class ScratchTest {
    @Autowired JdbcClient c;

    @Test
    void x() {
        System.out.println("S1 " + c.sql("SELECT * FROM \"flyway_schema_history\" ORDER BY \"installed_rank\"").query().listOfRows());
        System.out.println("S2 " + c.sql("SELECT * FROM product_names").query().listOfRows());
        var ds = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        Flyway.configure().dataSource(ds).locations("classpath:db/demo/original").load().migrate();
        try { Flyway.configure().dataSource(ds).locations("classpath:db/demo/edited").load().validate(); } catch (Exception e) { System.out.println("S3 " + e.getClass().getName() + " :: " + e.getMessage()); }
        try { Flyway.configure().dataSource(ds).locations("classpath:db/demo/edited").load().migrate(); } catch (Exception e) { System.out.println("S4 " + e.getClass().getName()); }
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, DataSourceInitializationAutoConfiguration.class, FlywayAutoConfiguration.class))
            .withPropertyValues("spring.flyway.locations=classpath:db/migration/jdbc04", "spring.sql.init.mode=always", "spring.sql.init.schema-locations=classpath:db/demo/conflict/schema.sql")
            .run(ctx -> { Throwable t = ctx.getStartupFailure(); System.out.println("S5 " + t); while (t != null && t.getCause()!=null) t = t.getCause(); System.out.println("   root " + (t==null?null:t.getMessage())); });
    }
}

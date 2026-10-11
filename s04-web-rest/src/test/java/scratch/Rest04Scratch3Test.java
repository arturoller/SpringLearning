package scratch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import s04_web_rest.rest04_json.Rest04Application;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(properties = "spring.jackson.use-jackson2-defaults=true")
@ContextConfiguration(classes = Rest04Application.class)
class Rest04Scratch3Test {
    @Autowired JsonMapper m;
    public static class Pojo { public int zeta = 1; public String alpha = "a"; }
    @Test void run() throws Exception {
        System.out.println("SCRATCH3 j2defaults DATES_TS=" + m.isEnabled(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS) + " FAIL_UNKNOWN=" + m.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) + " NULL_PRIM=" + m.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES) + " SORT=" + m.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY) + " pojo=" + m.writeValueAsString(new Pojo()) + " date=" + m.writeValueAsString(java.time.LocalDate.of(2026,10,1)));
    }
}

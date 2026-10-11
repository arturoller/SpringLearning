package scratch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import s04_web_rest.rest04_json.Rest04Application;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(properties = {"spring.jackson.datatype.datetime.write-dates-as-timestamps=true",
        "spring.jackson.deserialization.fail-on-unknown-properties=true",
        "spring.jackson.default-property-inclusion=non_null"})
@ContextConfiguration(classes = Rest04Application.class)
class Rest04Scratch2Test {
    @Autowired MockMvc mvc; @Autowired JsonMapper m;
    public static class Pojo { public int zeta = 1; public String alpha = "a"; public int getMid() { return 2; } }
    record WithIgnore(String name, @JsonIgnore int secret) {}
    record Q(String name, Integer qty) {}
    @Test void run() throws Exception {
        System.out.println("SCRATCH2 pojo plain " + JsonMapper.builder().build().writeValueAsString(new Pojo()));
        System.out.println("SCRATCH2 props DATES_TS=" + m.isEnabled(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS) + " FAIL_UNKNOWN=" + m.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        System.out.println("SCRATCH2 catalog " + mvc.perform(get("/api/catalog/1")).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println("SCRATCH2 q null " + m.writeValueAsString(new Q("a", null)));
        try { System.out.println(m.readValue("{\"name\":\"a\",\"qty\":1,\"x\":2}", Q.class)); } catch (Exception e) { System.out.println("SCRATCH2 unknown EX " + e.getClass().getName() + " : " + e.getMessage().lines().findFirst().get()); }
        JsonMapper plain = JsonMapper.builder().build();
        System.out.println("SCRATCH2 ignore ser " + plain.writeValueAsString(new WithIgnore("a", 5)));
        try { System.out.println(plain.readValue("{\"name\":\"a\"}", WithIgnore.class)); } catch (Exception e) { System.out.println("SCRATCH2 ignore read EX " + e.getClass().getName() + " : " + e.getMessage().lines().findFirst().get()); }
        try { System.out.println(plain.readValue("{\"name\":\"a\",\"secret\":5}", WithIgnore.class)); } catch (Exception e) { System.out.println("SCRATCH2 ignore read2 EX " + e.getClass().getName() + " : " + e.getMessage().lines().findFirst().get()); }
    }
}

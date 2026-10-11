package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(properties = {"err04.custom-advice=${A:true}", "spring.mvc.problemdetails.enabled=${B:false}"})
class Probe {
    @Autowired MockMvc mvc;
    @Test void probe() throws Exception {
        String[][] reqs = {{"GET","/api/products/999"},{"GET","/api/search"},{"POST","/api/search"},{"GET","/api/products/1/stock"},{"GET","/api/limits"},{"GET","/api/products/abc"}};
        for (String[] r : reqs) {
            try {
                var res = mvc.perform(request(org.springframework.http.HttpMethod.valueOf(r[0]), r[1])).andReturn().getResponse();
                System.out.println("PROBE " + r[1] + " -> " + res.getStatus() + " " + res.getContentType() + " " + res.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception e) { System.out.println("PROBE " + r[1] + " THROWS " + e.getClass().getSimpleName()); }
        }
    }
}

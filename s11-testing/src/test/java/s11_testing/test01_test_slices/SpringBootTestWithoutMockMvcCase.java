package s11_testing.test01_test_slices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Pomocnicza klasa BEZ metod testowych: {@code @SpringBootTest} bez {@code @AutoConfigureMockMvc}, a prosi o MockMvc. */
@SpringBootTest
@ActiveProfiles("test01")
class SpringBootTestWithoutMockMvcCase {

    @Autowired
    MockMvc mockMvc;
}

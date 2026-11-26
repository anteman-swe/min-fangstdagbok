package se.fiskeexperten;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostGisTestConfig.class)
class FiskeExpertenApplicationTests {

    @Test
    void contextLoads() {
    }
}

package org.example.fiskeexperten;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

// PostGisTestConfig startar en PostGIS i Docker åt testet (kräver att Docker körs)
@SpringBootTest
@Import(PostGisTestConfig.class)
class FiskeexpertenApplicationTests {

    @Test
    void contextLoads() {
    }

}

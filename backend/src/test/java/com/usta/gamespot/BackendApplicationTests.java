package com.usta.gamespot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = "app.cli.registration.enabled=false")
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}

package io.github.zemiles.hanjari;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import io.github.zemiles.hanjari.support.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class HanjariBatchApplicationTests {

	@Test
	void contextLoads() {
	}

}

package com.carrefour.kata.bidanddrive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BidanddriveApplicationTests {

	@Test
	void contextLoads() {
	}

}

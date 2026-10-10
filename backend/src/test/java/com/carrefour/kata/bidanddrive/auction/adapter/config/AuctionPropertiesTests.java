package com.carrefour.kata.bidanddrive.auction.adapter.config;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionWindow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
		"bidanddrive.auction.min-duration=PT5M",
		"bidanddrive.auction.max-duration=P7D" })
class AuctionPropertiesTests {

	private final AuctionWindow window;

	@Autowired
	AuctionPropertiesTests(AuctionWindow window) {
		this.window = window;
	}

	@Test
	void window_bounds_are_read_from_configuration() {
		assertThat(window).isEqualTo(new AuctionWindow(Duration.ofMinutes(5), Duration.ofDays(7)));
	}

}

package com.carrefour.kata.bidanddrive.auction.adapter.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventsPropertiesTests {

	@Test
	void production_defaults_are_a_25_second_heartbeat_and_a_30_minute_emitter_timeout() {
		EventsProperties properties = bind(Map.of());

		assertThat(properties.heartbeat()).isEqualTo(Duration.ofSeconds(25));
		assertThat(properties.emitterTimeout()).isEqualTo(Duration.ofMinutes(30));
	}

	@Test
	void both_can_be_overridden() {
		EventsProperties properties = bind(Map.of(
				"bidanddrive.events.heartbeat", "PT0.05S",
				"bidanddrive.events.emitter-timeout", "PT1M"));

		assertThat(properties.heartbeat()).isEqualTo(Duration.ofMillis(50));
		assertThat(properties.emitterTimeout()).isEqualTo(Duration.ofMinutes(1));
	}

	private static EventsProperties bind(Map<String, String> values) {
		return new Binder(new MapConfigurationPropertySource(values))
				.bindOrCreate("bidanddrive.events", EventsProperties.class);
	}

}

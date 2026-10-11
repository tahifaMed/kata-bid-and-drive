package com.carrefour.kata.bidanddrive.auction.adapter.config;

import com.carrefour.kata.bidanddrive.auction.adapter.out.events.AuctionEventBroker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Live auction events over SSE (spec 009, ADR 0015). One heartbeat sweep keeps every open stream alive
 * and finds clients that left, since a closed connection only shows when a write fails.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(EventsProperties.class)
class EventsConfiguration implements SchedulingConfigurer {

	private final EventsProperties properties;

	EventsConfiguration(EventsProperties properties) {
		this.properties = properties;
	}

	@Bean
	AuctionEventBroker auctionEventBroker() {
		return new AuctionEventBroker();
	}

	@Bean
	EventStreamsShutdown eventStreamsShutdown(AuctionEventBroker broker) {
		return new EventStreamsShutdown(broker);
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar registrar) {
		AuctionEventBroker broker = auctionEventBroker();
		registrar.addFixedRateTask(broker::heartbeat, properties.heartbeat());
	}

}

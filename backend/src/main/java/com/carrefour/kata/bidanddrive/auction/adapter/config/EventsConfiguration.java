package com.carrefour.kata.bidanddrive.auction.adapter.config;

import com.carrefour.kata.bidanddrive.auction.adapter.out.events.AuctionEventBroker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Live auction events over SSE (spec 009, ADR 0015). */
@Configuration
class EventsConfiguration {

	@Bean
	AuctionEventBroker auctionEventBroker() {
		return new AuctionEventBroker();
	}

}

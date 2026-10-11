package com.carrefour.kata.bidanddrive.auction.adapter.out.events;

import com.carrefour.kata.bidanddrive.auction.application.AuctionEventPublisher;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Hands the event to Spring, so {@link AuctionEventRelay} sees it only after the transaction commits. */
@Component
class SpringAuctionEventPublisher implements AuctionEventPublisher {

	private final ApplicationEventPublisher publisher;

	SpringAuctionEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void publish(AuctionEvent event) {
		publisher.publishEvent(event);
	}

}

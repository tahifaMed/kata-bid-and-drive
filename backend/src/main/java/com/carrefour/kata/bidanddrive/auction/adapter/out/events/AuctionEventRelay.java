package com.carrefour.kata.bidanddrive.auction.adapter.out.events;

import com.carrefour.kata.bidanddrive.auction.domain.AuctionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends committed auction events to the live subscribers (ADR 0002). Runs on the committing thread
 * after commit, so nothing may escape: the change is already stored and the caller must see its success.
 */
@Component
class AuctionEventRelay {

	private static final Logger log = LoggerFactory.getLogger(AuctionEventRelay.class);

	private final AuctionEventBroker broker;
	private final JsonMapper json;

	AuctionEventRelay(AuctionEventBroker broker, JsonMapper json) {
		this.broker = broker;
		this.json = json;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void relay(AuctionEvent event) {
		AuctionEventMessage message = AuctionEventMessage.of(event);
		try {
			broker.broadcast(message.auctionId(), message.type(), json.writeValueAsString(message));
		} catch (JacksonException failure) {
			log.error("Could not send a {} event for auction {}", message.type(), message.auctionId(), failure);
		}
	}

}

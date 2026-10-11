package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.AuctionEvent;

/**
 * Port for auction events. Call it inside the transaction that made the change: the adapter sends the event
 * only after that transaction commits, and drops it on rollback (ADR 0015).
 */
public interface AuctionEventPublisher {

	void publish(AuctionEvent event);

}

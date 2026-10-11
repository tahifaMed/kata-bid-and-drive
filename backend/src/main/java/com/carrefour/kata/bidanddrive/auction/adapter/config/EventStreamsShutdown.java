package com.carrefour.kata.bidanddrive.auction.adapter.config;

import com.carrefour.kata.bidanddrive.auction.adapter.out.events.AuctionEventBroker;
import org.springframework.context.SmartLifecycle;

/**
 * Ends the open event streams when the application stops. It stops before the web server's graceful
 * shutdown (a higher phase stops first), which would otherwise wait its full timeout for them.
 */
class EventStreamsShutdown implements SmartLifecycle {

	private final AuctionEventBroker broker;
	private volatile boolean running;

	EventStreamsShutdown(AuctionEventBroker broker) {
		this.broker = broker;
	}

	@Override
	public void start() {
		running = true;
	}

	@Override
	public void stop() {
		broker.closeAll();
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public int getPhase() {
		return SmartLifecycle.DEFAULT_PHASE;
	}

}

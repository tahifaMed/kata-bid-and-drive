package com.carrefour.kata.bidanddrive.auction.adapter.config;

import com.carrefour.kata.bidanddrive.auction.adapter.out.events.AuctionEventBroker;
import com.carrefour.kata.bidanddrive.auction.adapter.out.events.EventSink;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.context.WebServerGracefulShutdownLifecycle;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring stops and restarts a context's lifecycle beans without closing it: the test framework pauses
 * cached contexts between test classes. A restarted context must stream events again.
 */
class EventStreamsShutdownTests {

	private final AuctionEventBroker broker = new AuctionEventBroker();
	private final EventStreamsShutdown lifecycle = new EventStreamsShutdown(broker);

	@Test
	void stopping_ends_the_open_streams() {
		lifecycle.start();
		broker.subscribe(5, new QuietSink());

		lifecycle.stop();

		assertThat(broker.subscriberCount(5)).isZero();
	}

	@Test
	void a_restart_accepts_streams_again() {
		lifecycle.start();
		lifecycle.stop();

		lifecycle.start();
		broker.subscribe(5, new QuietSink());

		assertThat(broker.subscriberCount(5)).isEqualTo(1);
	}

	@Test
	void stops_before_the_web_server_drains_its_requests() {
		assertThat(lifecycle.getPhase()).isGreaterThan(WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE);
	}

	private static final class QuietSink implements EventSink {

		@Override
		public void send(String name, String data) {
		}

		@Override
		public void comment(String text) {
		}

		@Override
		public void complete() {
		}

		@Override
		public void completeWithError(Throwable cause) {
		}

		@Override
		public void onCompletion(Runnable callback) {
		}

		@Override
		public void onTimeout(Runnable callback) {
		}

		@Override
		public void onError(Consumer<Throwable> callback) {
		}

	}

}

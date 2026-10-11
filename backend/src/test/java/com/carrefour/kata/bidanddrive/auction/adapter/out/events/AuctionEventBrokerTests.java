package com.carrefour.kata.bidanddrive.auction.adapter.out.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AuctionEventBrokerTests {

	private final AuctionEventBroker broker = new AuctionEventBroker();

	@Test
	void counts_subscribers_per_auction() {
		broker.subscribe(5, new FakeSink());
		broker.subscribe(5, new FakeSink());
		broker.subscribe(6, new FakeSink());

		assertThat(broker.subscriberCount(5)).isEqualTo(2);
		assertThat(broker.subscriberCount(6)).isEqualTo(1);
		assertThat(broker.subscriberCount(7)).isZero();
	}

	@Test
	void sends_an_event_only_to_the_subscribers_of_its_auction() {
		FakeSink onFive = new FakeSink();
		FakeSink onSix = new FakeSink();
		broker.subscribe(5, onFive);
		broker.subscribe(6, onSix);

		broker.broadcast(5, "bid-placed", "{\"auctionId\":5}");

		assertThat(onFive.events).containsExactly("bid-placed {\"auctionId\":5}");
		assertThat(onSix.events).isEmpty();
	}

	@Test
	void unsubscribing_stops_the_events() {
		FakeSink sink = new FakeSink();
		broker.subscribe(5, sink);

		broker.unsubscribe(5, sink);
		broker.broadcast(5, "bid-placed", "{}");

		assertThat(broker.subscriberCount(5)).isZero();
		assertThat(sink.events).isEmpty();
	}

	@Test
	void a_completed_sink_is_removed() {
		FakeSink sink = new FakeSink();
		broker.subscribe(5, sink);

		sink.completion.run();
		broker.broadcast(5, "bid-placed", "{}");

		assertThat(broker.subscriberCount(5)).isZero();
		assertThat(sink.events).isEmpty();
	}

	@Test
	void a_timed_out_sink_is_removed() {
		FakeSink sink = new FakeSink();
		broker.subscribe(5, sink);

		sink.timeout.run();

		assertThat(broker.subscriberCount(5)).isZero();
	}

	@Test
	void a_sink_that_reports_an_error_is_removed() {
		FakeSink sink = new FakeSink();
		broker.subscribe(5, sink);

		sink.error.accept(new IOException("broken pipe"));

		assertThat(broker.subscriberCount(5)).isZero();
	}

	@Test
	void a_failing_sink_is_removed_and_the_others_still_get_the_event() {
		FakeSink broken = new FakeSink();
		broken.failure = new IllegalStateException("already completed");
		FakeSink healthy = new FakeSink();
		broker.subscribe(5, broken);
		broker.subscribe(5, healthy);

		assertThatNoException().isThrownBy(() -> broker.broadcast(5, "bid-placed", "{}"));

		assertThat(healthy.events).containsExactly("bid-placed {}");
		assertThat(broken.completedWithError).isSameAs(broken.failure);
		assertThat(broker.subscriberCount(5)).isEqualTo(1);
	}

	@Test
	void a_sink_that_cannot_be_closed_does_not_break_the_broadcast() {
		FakeSink broken = new FakeSink();
		broken.failure = new IOException("connection reset");
		broken.closeFailure = new IllegalStateException("already completed");
		broker.subscribe(5, broken);

		assertThatNoException().isThrownBy(() -> broker.broadcast(5, "bid-placed", "{}"));

		assertThat(broker.subscriberCount(5)).isZero();
	}

	@Test
	void the_heartbeat_reaches_every_auction() {
		FakeSink onFive = new FakeSink();
		FakeSink onSix = new FakeSink();
		broker.subscribe(5, onFive);
		broker.subscribe(6, onSix);

		broker.heartbeat();

		assertThat(onFive.comments).containsExactly("keepalive");
		assertThat(onSix.comments).containsExactly("keepalive");
	}

	@Test
	void a_heartbeat_to_a_gone_client_removes_it() {
		FakeSink gone = new FakeSink();
		gone.failure = new IOException("connection reset");
		broker.subscribe(5, gone);

		broker.heartbeat();

		assertThat(broker.subscriberCount(5)).isZero();
	}

}

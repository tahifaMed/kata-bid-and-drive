package com.carrefour.kata.bidanddrive.auction.adapter.out.events;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	void a_new_subscriber_gets_a_connected_comment_so_the_stream_opens_at_once() {
		FakeSink sink = new FakeSink();

		broker.subscribe(5, sink);

		assertThat(sink.comments).containsExactly("connected");
	}

	@Test
	void a_subscriber_that_fails_on_connect_is_not_kept() {
		FakeSink gone = new FakeSink();
		gone.failure = new IOException("connection reset");

		broker.subscribe(5, gone);

		assertThat(broker.subscriberCount(5)).isZero();
	}

	@RepeatedTest(20)
	void a_subscribe_racing_the_last_unsubscribe_is_never_lost() throws Exception {
		try (ExecutorService threads = Executors.newFixedThreadPool(2)) {
			for (int round = 0; round < 500; round++) {
				AuctionEventBroker fresh = new AuctionEventBroker();
				FakeSink leaving = new FakeSink();
				FakeSink joining = new FakeSink();
				fresh.subscribe(5, leaving);
				CyclicBarrier start = new CyclicBarrier(2);

				Future<?> unsubscribe = threads.submit(() -> {
					start.await();
					fresh.unsubscribe(5, leaving);
					return null;
				});
				Future<?> subscribe = threads.submit(() -> {
					start.await();
					fresh.subscribe(5, joining);
					return null;
				});
				unsubscribe.get();
				subscribe.get();

				assertThat(fresh.subscriberCount(5)).as("round %d", round).isEqualTo(1);
			}
		}
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
		FakeSink healthy = new FakeSink();
		broker.subscribe(5, broken);
		broker.subscribe(5, healthy);
		broken.failure = new IllegalStateException("already completed");

		assertThatNoException().isThrownBy(() -> broker.broadcast(5, "bid-placed", "{}"));

		assertThat(healthy.events).containsExactly("bid-placed {}");
		assertThat(broken.completedWithError).isSameAs(broken.failure);
		assertThat(broker.subscriberCount(5)).isEqualTo(1);
	}

	@Test
	void a_sink_that_cannot_be_closed_does_not_break_the_broadcast() {
		FakeSink broken = new FakeSink();
		broker.subscribe(5, broken);
		broken.failure = new IOException("connection reset");
		broken.closeFailure = new IllegalStateException("already completed");

		assertThatNoException().isThrownBy(() -> broker.broadcast(5, "bid-placed", "{}"));

		assertThat(broker.subscriberCount(5)).isZero();
	}

	@Test
	void closing_completes_every_stream_and_forgets_it() {
		FakeSink onFive = new FakeSink();
		FakeSink onSix = new FakeSink();
		broker.subscribe(5, onFive);
		broker.subscribe(6, onSix);

		broker.closeAll();

		assertThat(onFive.completed).isTrue();
		assertThat(onSix.completed).isTrue();
		assertThat(broker.subscriberCount(5)).isZero();
		assertThat(broker.subscriberCount(6)).isZero();
	}

	@Test
	void a_stream_opened_after_closing_is_ended_at_once() {
		broker.closeAll();
		FakeSink late = new FakeSink();

		broker.subscribe(5, late);

		assertThat(late.completed).isTrue();
		assertThat(broker.subscriberCount(5)).isZero();
	}

	@Test
	void reopening_accepts_streams_again() {
		broker.closeAll();
		broker.open();
		FakeSink sink = new FakeSink();

		broker.subscribe(5, sink);

		assertThat(sink.completed).isFalse();
		assertThat(broker.subscriberCount(5)).isEqualTo(1);
	}

	@Test
	void closing_a_stream_that_is_already_closed_does_not_stop_the_others() {
		FakeSink closed = new FakeSink();
		FakeSink open = new FakeSink();
		broker.subscribe(5, closed);
		broker.subscribe(5, open);
		closed.closeFailure = new IllegalStateException("already completed");

		assertThatNoException().isThrownBy(broker::closeAll);

		assertThat(open.completed).isTrue();
	}

	@Test
	void the_heartbeat_reaches_every_auction() {
		FakeSink onFive = new FakeSink();
		FakeSink onSix = new FakeSink();
		broker.subscribe(5, onFive);
		broker.subscribe(6, onSix);

		broker.heartbeat();

		assertThat(onFive.comments).containsExactly("connected", "keepalive");
		assertThat(onSix.comments).containsExactly("connected", "keepalive");
	}

	@Test
	void a_heartbeat_to_a_gone_client_removes_it() {
		FakeSink gone = new FakeSink();
		broker.subscribe(5, gone);
		gone.failure = new IOException("connection reset");

		broker.heartbeat();

		assertThat(broker.subscriberCount(5)).isZero();
	}

}

package com.carrefour.kata.bidanddrive.auction.adapter.out.events;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
import com.carrefour.kata.bidanddrive.auction.application.AuctionNotFoundException;
import com.carrefour.kata.bidanddrive.auction.application.AuctionRepository;
import com.carrefour.kata.bidanddrive.auction.application.InvalidBidException;
import com.carrefour.kata.bidanddrive.auction.application.PlaceBid;
import com.carrefour.kata.bidanddrive.auction.application.RawAmount;
import com.carrefour.kata.bidanddrive.auction.domain.AlreadyHighestBidderException;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionClosedException;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidBelowMinimumException;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.auction.domain.SellerCannotBidException;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The bid use case, its transaction and the after-commit listener, against a real database (spec 009).
 * A fake sink stands in for a client. Delivery is synchronous, so an event is in the sink when the
 * transaction returns. Not transactional: each bid commits on its own, so the test cleans up.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BidEventsAfterCommitTests {

	private static final UserId SELLER = new UserId(1);
	private static final UserId BOB = new UserId(7101);
	private static final UserId CHLOE = new UserId(7102);
	private static final Instant ENDS_AT = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS);

	@Autowired
	private PlaceBid placeBid;

	@Autowired
	private AuctionEventBroker broker;

	@Autowired
	private AuctionRepository auctions;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private JsonMapper json;

	private final FakeSink sink = new FakeSink();
	private long auctionId;
	private long otherAuctionId;

	@BeforeEach
	void create_bidders_and_auctions() {
		jdbc.update("INSERT INTO app_user (id, display_name) VALUES (?, 'Bob'), (?, 'Chloe') ON CONFLICT (id) DO NOTHING",
				BOB.value(), CHLOE.value());
		auctionId = save(ENDS_AT);
		otherAuctionId = save(ENDS_AT);
		broker.subscribe(auctionId, sink);
	}

	@AfterEach
	void delete_what_the_test_created() {
		broker.unsubscribe(auctionId, sink);
		for (long id : List.of(auctionId, otherAuctionId)) {
			jdbc.update("DELETE FROM bid WHERE auction_id = ?", id);
			jdbc.update("DELETE FROM car WHERE auction_id = ?", id);
			jdbc.update("DELETE FROM auction WHERE id = ?", id);
		}
		jdbc.update("DELETE FROM app_user WHERE id IN (?, ?)", BOB.value(), CHLOE.value());
	}

	@Test
	void a_committed_bid_sends_bid_placed_with_the_auction_state_after_the_bid() {
		Bid bid = placeBid.execute(auctionId, BOB, new RawAmount("1200.00"));

		assertThat(sink.events).hasSize(1);
		assertThat(sink.events.getFirst()).startsWith("bid-placed ");
		JsonNode event = envelope(sink.events.getFirst());
		assertThat(event.get("type").asString()).isEqualTo("bid-placed");
		assertThat(event.get("auctionId").asLong()).isEqualTo(auctionId);
		assertThat(Instant.parse(event.get("occurredAt").asString())).isEqualTo(bid.placedAt());
		JsonNode data = event.get("data");
		assertThat(data.get("bidId").asLong()).isEqualTo(bid.id());
		assertThat(data.get("amount").asString()).isEqualTo("1200.00");
		assertThat(data.get("bidderId").asLong()).isEqualTo(BOB.value());
		assertThat(data.get("bidderName").asString()).isEqualTo("B***");
		assertThat(data.get("highestBidderId").asLong()).isEqualTo(BOB.value());
		assertThat(data.get("currentBid").asString()).isEqualTo("1200.00");
		assertThat(data.get("minNextBid").asString()).isEqualTo("1300.00");
		assertThat(data.get("bidCount").asInt()).isEqualTo(1);
		assertThat(Instant.parse(data.get("endsAt").asString())).isEqualTo(ENDS_AT);
		assertThat(data.get("extended").asBoolean()).isFalse();
	}

	@Test
	void a_bid_whose_transaction_rolls_back_after_publishing_sends_nothing() {
		transaction().executeWithoutResult(status -> {
			placeBid.execute(auctionId, BOB, new RawAmount("1200.00"));
			status.setRollbackOnly();
		});

		assertThat(sink.events).isEmpty();
		assertOnlyEventIsMarker();
	}

	@Test
	void a_bid_that_loses_a_version_conflict_at_commit_sends_nothing() {
		assertThatThrownBy(() -> transaction().executeWithoutResult(status -> {
			placeBid.execute(auctionId, BOB, new RawAmount("1000.00"));
			CompletableFuture.runAsync(() -> placeBid.execute(auctionId, CHLOE, new RawAmount("1000.00")))
					.orTimeout(10, TimeUnit.SECONDS)
					.join();
		})).isInstanceOf(OptimisticLockingFailureException.class);

		assertThat(sink.events).hasSize(1);
		assertThat(bidderOf(sink.events.getFirst())).isEqualTo(CHLOE.value());
	}

	@Test
	void a_rejected_bid_sends_nothing() {
		long endedAuctionId = save(Instant.now().minus(Duration.ofHours(1)));
		FakeSink endedSink = new FakeSink();
		broker.subscribe(endedAuctionId, endedSink);
		placeBid.execute(auctionId, BOB, new RawAmount("1000.00"));
		sink.events.clear();

		try {
			assertThatThrownBy(() -> placeBid.execute(auctionId, CHLOE, new RawAmount("abc")))
					.isInstanceOf(InvalidBidException.class);
			assertThatThrownBy(() -> placeBid.execute(auctionId, SELLER, new RawAmount("5000.00")))
					.isInstanceOf(SellerCannotBidException.class);
			assertThatThrownBy(() -> placeBid.execute(Long.MAX_VALUE, CHLOE, new RawAmount("5000.00")))
					.isInstanceOf(AuctionNotFoundException.class);
			assertThatThrownBy(() -> placeBid.execute(auctionId, BOB, new RawAmount("5000.00")))
					.isInstanceOf(AlreadyHighestBidderException.class);
			assertThatThrownBy(() -> placeBid.execute(auctionId, CHLOE, new RawAmount("1000.00")))
					.isInstanceOf(BidBelowMinimumException.class);
			assertThatThrownBy(() -> placeBid.execute(endedAuctionId, CHLOE, new RawAmount("5000.00")))
					.isInstanceOf(AuctionClosedException.class);

			assertThat(endedSink.events).isEmpty();
			assertThat(sink.events).isEmpty();
			assertOnlyEventIsMarker();
		} finally {
			broker.unsubscribe(endedAuctionId, endedSink);
			jdbc.update("DELETE FROM car WHERE auction_id = ?", endedAuctionId);
			jdbc.update("DELETE FROM auction WHERE id = ?", endedAuctionId);
		}
	}

	@Test
	void a_bid_on_another_auction_is_not_sent_to_this_one() {
		placeBid.execute(otherAuctionId, BOB, new RawAmount("1200.00"));

		assertThat(sink.events).isEmpty();
		assertOnlyEventIsMarker();
	}

	@Test
	void a_failing_subscriber_does_not_fail_the_bid_or_starve_the_others() {
		FakeSink broken = new FakeSink();
		broken.failure = new IOException("connection reset");
		broker.subscribe(auctionId, broken);

		Bid bid = placeBid.execute(auctionId, BOB, new RawAmount("1200.00"));

		assertThat(bid.id()).isNotNull();
		assertThat(sink.events).hasSize(1);
		assertThat(broken.completedWithError).isSameAs(broken.failure);
		assertThat(broker.subscriberCount(auctionId)).isEqualTo(1);
	}

	/** Places a valid bid and checks it is the only event this test's subscriber has seen. */
	private void assertOnlyEventIsMarker() {
		Auction auction = auctions.findById(auctionId).orElseThrow();
		UserId bidder = BOB.equals(auction.leadingBidderId()) ? CHLOE : BOB;
		Bid marker = placeBid.execute(auctionId, bidder, new RawAmount(auction.minNextBid().amount().toPlainString()));

		assertThat(sink.events).hasSize(1);
		assertThat(envelope(sink.events.getFirst()).get("data").get("bidId").asLong()).isEqualTo(marker.id());
	}

	private TransactionTemplate transaction() {
		return new TransactionTemplate(transactionManager);
	}

	private long save(Instant endsAt) {
		return auctions.save(new Auction(null, SELLER, new Car("Fiat", "Panda", 2015, 90000, null, null),
				new Money(new BigDecimal("1000.00")), new Money(new BigDecimal("100.00")), endsAt,
				AuctionStatus.OPEN)).id();
	}

	private long bidderOf(String event) {
		return envelope(event).get("data").get("bidderId").asLong();
	}

	private JsonNode envelope(String event) {
		return json.readTree(event.substring(event.indexOf(' ') + 1));
	}

}

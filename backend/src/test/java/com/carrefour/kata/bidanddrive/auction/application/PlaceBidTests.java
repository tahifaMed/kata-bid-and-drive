package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.auction.domain.SellerCannotBidException;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceBidTests {

	private static final Instant NOW = Instant.parse("2100-01-01T12:00:00Z");
	private static final UserId SELLER = new UserId(1);
	private static final UserId BOB = new UserId(2);

	private final FakeAuctions auctions = new FakeAuctions();
	private final PlaceBid placeBid = new PlaceBid(auctions, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void an_accepted_bid_is_recorded_with_the_new_totals_and_the_clock_time() {
		auctions.add(auction());

		Bid saved = placeBid.execute(5, BOB, new RawAmount("1000.00"));

		assertThat(saved).isEqualTo(new Bid(99L, 5, BOB, money("1000.00"), NOW));
		assertThat(auctions.recorded.currentBid()).isEqualTo(money("1000.00"));
		assertThat(auctions.recorded.bidCount()).isEqualTo(1);
		assertThat(auctions.recorded.highestBidderId()).contains(BOB);
	}

	@Test
	void an_unknown_auction_is_not_found_even_when_the_amount_is_also_invalid() {
		assertThatThrownBy(() -> placeBid.execute(404, BOB, new RawAmount(null)))
				.isInstanceOf(AuctionNotFoundException.class);
	}

	@Test
	void a_missing_or_non_string_amount_is_invalid() {
		auctions.add(auction());

		assertThatThrownBy(() -> placeBid.execute(5, BOB, new RawAmount(null)))
				.isInstanceOfSatisfying(InvalidBidException.class, e -> {
					assertThat(e.field()).isEqualTo("amount");
					assertThat(e.getMessage()).isEqualTo("is required as a string, such as \"1100.00\"");
				});
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "abc", " 1100", "1e3", "0", "0.00", "-5.00", "1100.001", "10000000000.00"})
	void a_malformed_amount_is_invalid(String text) {
		auctions.add(auction());

		assertThatThrownBy(() -> placeBid.execute(5, BOB, new RawAmount(text)))
				.isInstanceOfSatisfying(InvalidBidException.class,
						e -> assertThat(e.getMessage()).isEqualTo("must be a positive amount with at most two decimals"));
	}

	@Test
	void trailing_zeros_after_two_decimals_are_accepted_like_create_auction() {
		auctions.add(auction());

		Bid saved = placeBid.execute(5, BOB, new RawAmount("1000.000"));

		assertThat(saved.amount()).isEqualTo(money("1000.00"));
	}

	@Test
	void an_invalid_amount_wins_over_the_seller_rule() {
		auctions.add(auction());

		assertThatThrownBy(() -> placeBid.execute(5, SELLER, new RawAmount("abc")))
				.isInstanceOf(InvalidBidException.class);
		assertThatThrownBy(() -> placeBid.execute(5, SELLER, new RawAmount("1000.00")))
				.isInstanceOf(SellerCannotBidException.class);
	}

	private static Auction auction() {
		return new Auction(5L, SELLER, new Car("Volkswagen", "Golf", 2020, 42000, null, null),
				money("1000.00"), money("100.00"), NOW.plusSeconds(3600), AuctionStatus.OPEN);
	}

	private static Money money(String amount) {
		return new Money(new BigDecimal(amount));
	}

	private static class FakeAuctions implements AuctionRepository {

		private final Map<Long, Auction> byId = new HashMap<>();
		private Auction recorded;

		void add(Auction auction) {
			byId.put(auction.id(), auction);
		}

		@Override
		public Auction save(Auction auction) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public Bid recordBid(Auction auction, Bid bid) {
			recorded = auction;
			return new Bid(99L, bid.auctionId(), bid.bidderId(), bid.amount(), bid.placedAt());
		}

		@Override
		public AuctionPage<Auction> findOpen(Instant now, PageQuery query) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public Optional<Auction> findById(long id) {
			return Optional.ofNullable(byId.get(id));
		}

	}

}

package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetAuctionDetailTests {

	private static final Instant NOW = Instant.parse("2100-01-01T00:00:00Z");
	private static final UserId ALICE = new UserId(1);
	private static final UserId BOB = new UserId(2);
	private static final UserId CAROL = new UserId(3);

	private final FakeAuctions auctions = new FakeAuctions();
	private final Map<Long, List<Bid>> bids = new HashMap<>();
	private final FakeUsers users = new FakeUsers(
			new User(ALICE, "Alice"), new User(BOB, "Bob"), new User(CAROL, "Carol"));
	private final List<Integer> requestedLimits = new ArrayList<>();
	private final Map<UserId, BidderStanding> standingsOnFive = new HashMap<>();
	private final GetAuctionDetail getAuctionDetail = new GetAuctionDetail(auctions,
			new BidRepository() {
				@Override
				public List<Bid> findNewest(long auctionId, int limit) {
					requestedLimits.add(limit);
					return bids.getOrDefault(auctionId, List.of());
				}

				@Override
				public Optional<Bid> findById(long auctionId, long bidId) {
					throw new UnsupportedOperationException("not used by this test");
				}

				@Override
				public Optional<BidderStanding> findStanding(long auctionId, UserId bidder) {
					return auctionId == 5 ? Optional.ofNullable(standingsOnFive.get(bidder)) : Optional.empty();
				}

				@Override
				public List<BidderStanding> findStandings(UserId bidder) {
					throw new UnsupportedOperationException("not used by this test");
				}
			},
			users, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void asks_storage_for_the_fifty_newest_bids_only() {
		auctions.add(auction(5, NOW.plusSeconds(3600)));

		getAuctionDetail.execute(5);

		assertThat(requestedLimits).containsExactly(50);
	}

	@Test
	void looks_up_each_bidder_once_however_many_bids_they_placed() {
		auctions.add(auction(5, NOW.plusSeconds(3600)));
		bids.put(5L, List.of(
				bid(1, BOB, "12000.00", NOW.minusSeconds(30)),
				bid(2, CAROL, "12100.00", NOW.minusSeconds(20)),
				bid(3, BOB, "12200.00", NOW.minusSeconds(10))));

		getAuctionDetail.execute(5);

		assertThat(users.lookups).containsExactlyInAnyOrder(ALICE, BOB, CAROL);
	}

	@Test
	void an_unknown_id_is_not_found() {
		assertThatThrownBy(() -> getAuctionDetail.execute(99))
				.isInstanceOf(AuctionNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void shows_the_seller_name_in_full() {
		auctions.add(auction(5, NOW.plusSeconds(3600)));

		AuctionDetail detail = getAuctionDetail.execute(5);

		assertThat(detail.auction().id()).isEqualTo(5);
		assertThat(detail.sellerName()).isEqualTo("Alice");
	}

	@Test
	void lists_bids_newest_first_with_masked_bidder_names() {
		auctions.add(auction(5, NOW.plusSeconds(3600)));
		bids.put(5L, List.of(
				bid(1, BOB, "12000.00", NOW.minusSeconds(30)),
				bid(2, CAROL, "12100.00", NOW.minusSeconds(20)),
				bid(3, BOB, "12200.00", NOW.minusSeconds(10))));

		AuctionDetail detail = getAuctionDetail.execute(5);

		assertThat(detail.bids()).containsExactly(
				new AuctionDetail.HistoryEntry(money("12200.00"), NOW.minusSeconds(10), "B***"),
				new AuctionDetail.HistoryEntry(money("12100.00"), NOW.minusSeconds(20), "C***"),
				new AuctionDetail.HistoryEntry(money("12000.00"), NOW.minusSeconds(30), "B***"));
	}

	@Test
	void a_viewer_who_bid_gets_their_standing() {
		Auction auction = auction(5, NOW.plusSeconds(3600));
		auctions.add(auction);
		BidderStanding bobs = new BidderStanding(auction, BOB, money("12000.00"));
		standingsOnFive.put(BOB, bobs);

		assertThat(getAuctionDetail.execute(5, BOB).myBid()).isEqualTo(bobs);
	}

	@Test
	void with_a_standing_the_auction_comes_from_the_same_read_as_the_standing() {
		auctions.add(auction(5, NOW.plusSeconds(3600)));
		// A bid landed between the two reads: the standing saw Carol take the lead from Bob.
		Auction later = new Auction(5L, ALICE, car(), money("12000.00"), money("100.00"), NOW.plusSeconds(3600),
				AuctionStatus.OPEN, money("12100.00"), CAROL, 2, 2);
		standingsOnFive.put(BOB, new BidderStanding(later, BOB, money("12000.00")));

		AuctionDetail detail = getAuctionDetail.execute(5, BOB);

		assertThat(detail.auction()).isEqualTo(later);
		assertThat(detail.myBid().auction()).isEqualTo(later);
	}

	@Test
	void a_viewer_who_did_not_bid_and_an_anonymous_caller_get_no_standing() {
		auctions.add(auction(5, NOW.plusSeconds(3600)));

		assertThat(getAuctionDetail.execute(5, CAROL).myBid()).isNull();
		assertThat(getAuctionDetail.execute(5).myBid()).isNull();
	}

	@Test
	void no_bids_give_an_empty_history() {
		auctions.add(auction(5, NOW.plusSeconds(3600)));

		assertThat(getAuctionDetail.execute(5).bids()).isEmpty();
	}

	@Test
	void an_auction_that_ends_later_is_biddable() {
		auctions.add(auction(5, NOW.plusSeconds(1)));

		assertThat(getAuctionDetail.execute(5).biddable()).isTrue();
	}

	@Test
	void an_auction_that_ends_now_is_not_biddable_and_keeps_its_stored_status() {
		auctions.add(auction(5, NOW));

		AuctionDetail detail = getAuctionDetail.execute(5);

		assertThat(detail.biddable()).isFalse();
		assertThat(detail.auction().status()).isEqualTo(AuctionStatus.OPEN);
	}

	@Test
	void a_seller_missing_from_the_directory_is_an_error() {
		auctions.add(new Auction(5L, new UserId(42), car(), money("12000.00"), money("100.00"),
				NOW.plusSeconds(3600), AuctionStatus.OPEN));

		assertThatThrownBy(() -> getAuctionDetail.execute(5)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void a_bidder_missing_from_the_directory_is_an_error() {
		auctions.add(auction(5, NOW.plusSeconds(3600)));
		bids.put(5L, List.of(bid(1, new UserId(42), "12000.00", NOW.minusSeconds(10))));

		assertThatThrownBy(() -> getAuctionDetail.execute(5)).isInstanceOf(IllegalStateException.class);
	}

	private static Auction auction(long id, Instant endsAt) {
		return new Auction(id, ALICE, car(), money("12000.00"), money("100.00"), endsAt, AuctionStatus.OPEN);
	}

	private static Car car() {
		return new Car("Volkswagen", "Golf", 2020, 42000, null, null);
	}

	private static Bid bid(long id, UserId bidder, String amount, Instant placedAt) {
		return new Bid(id, 5L, bidder, money(amount), placedAt);
	}

	private static Money money(String amount) {
		return new Money(new BigDecimal(amount));
	}

	/** Hand-written fake: only {@link AuctionRepository#findById} is used here. */
	private static class FakeAuctions implements AuctionRepository {

		private final Map<Long, Auction> byId = new HashMap<>();

		void add(Auction auction) {
			byId.put(auction.id(), auction);
		}

		@Override
		public Auction save(Auction auction) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public Bid recordBid(Auction auction, Bid bid) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public AuctionPage<Auction> findOpen(Instant now, PageQuery query) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public boolean existsById(long id) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public Optional<Auction> findById(long id) {
			return Optional.ofNullable(byId.get(id));
		}

	}

	private static class FakeUsers implements UserDirectory {

		private final List<User> users;
		final List<UserId> lookups = new ArrayList<>();

		FakeUsers(User... users) {
			this.users = List.of(users);
		}

		@Override
		public List<User> findAll() {
			return users;
		}

		@Override
		public Optional<User> findById(UserId id) {
			lookups.add(id);
			return users.stream().filter(user -> user.id().equals(id)).findFirst();
		}

	}

}

package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.auction.application.BidRepository;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
class JpaBidRepository implements BidRepository {

	private static final Sort NEWEST_FIRST = Sort.by("placedAt").descending().and(Sort.by("id").descending());

	private final BidJpaRepository repository;

	JpaBidRepository(BidJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<Bid> findNewest(long auctionId, int limit) {
		return repository.findByAuctionId(auctionId, PageRequest.of(0, limit, NEWEST_FIRST)).stream()
				.map(BidEntity::toDomain)
				.toList();
	}

	@Override
	public Optional<Bid> findById(long auctionId, long bidId) {
		return repository.findByIdAndAuctionId(bidId, auctionId).map(BidEntity::toDomain);
	}

	@Override
	public List<BidderStanding> findStandings(UserId bidder) {
		return repository.findStandings(bidder.value()).stream()
				.map(row -> new BidderStanding(((AuctionEntity) row[0]).toDomain(), bidder,
						new Money((BigDecimal) row[1])))
				.toList();
	}

}

package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.auction.application.AuctionPage;
import com.carrefour.kata.bidanddrive.auction.application.AuctionRepository;
import com.carrefour.kata.bidanddrive.auction.application.PageQuery;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
class JpaAuctionRepository implements AuctionRepository {

	private final AuctionJpaRepository repository;
	private final BidJpaRepository bids;

	JpaAuctionRepository(AuctionJpaRepository repository, BidJpaRepository bids) {
		this.repository = repository;
		this.bids = bids;
	}

	@Override
	public Auction save(Auction auction) {
		AuctionEntity entity = new AuctionEntity(
				auction.sellerId().value(),
				auction.startingPrice().amount(),
				auction.minIncrement().amount(),
				auction.endsAt(),
				auction.status());
		entity.attachCar(new CarEntity(entity, auction.car()));
		return toDomain(repository.save(entity));
	}

	/**
	 * Hibernate checks {@code @Version} against the version it loaded here, not the one the caller read,
	 * so a caller that read an older version is turned away first. {@code @Version} then guards the
	 * race between this load and the flush.
	 */
	@Override
	@Transactional
	public Bid recordBid(Auction auction, Bid bid) {
		AuctionEntity entity = repository.findById(auction.id())
				.orElseThrow(() -> new IllegalStateException("Auction " + auction.id() + " is not stored"));
		if (entity.getVersion() != auction.version()) {
			throw new ObjectOptimisticLockingFailureException(AuctionEntity.class, auction.id());
		}
		entity.recordBid(auction.currentBid().amount(), bid.bidderId().value(), auction.bidCount());
		return bids.save(new BidEntity(bid)).toDomain();
	}

	@Override
	public AuctionPage<Auction> findOpen(Instant now, PageQuery query) {
		Pageable pageable = PageRequest.of(query.number(), query.size(),
				Sort.by("endsAt").ascending().and(Sort.by("id").ascending()));
		Slice<AuctionEntity> slice = repository.findByStatusAndEndsAtAfter(AuctionStatus.OPEN, now, pageable);
		List<Auction> items = slice.getContent().stream().map(this::toDomain).toList();
		return new AuctionPage<>(items, query.number(), query.size(), slice.hasNext());
	}

	@Override
	public Optional<Auction> findById(long id) {
		return repository.findWithCarById(id).map(this::toDomain);
	}

	private Auction toDomain(AuctionEntity entity) {
		return new Auction(
				entity.getId(),
				new UserId(entity.getSellerId()),
				entity.getCar().toDomain(),
				new Money(entity.getStartingPrice()),
				new Money(entity.getMinIncrement()),
				entity.getEndsAt(),
				entity.getStatus(),
				entity.getCurrentBid() == null ? null : new Money(entity.getCurrentBid()),
				entity.getHighestBidderId() == null ? null : new UserId(entity.getHighestBidderId()),
				entity.getBidCount(),
				entity.getVersion());
	}

}

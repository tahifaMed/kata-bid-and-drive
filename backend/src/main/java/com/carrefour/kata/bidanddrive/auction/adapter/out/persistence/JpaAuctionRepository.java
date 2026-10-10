package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.auction.application.AuctionPage;
import com.carrefour.kata.bidanddrive.auction.application.AuctionRepository;
import com.carrefour.kata.bidanddrive.auction.application.PageQuery;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
class JpaAuctionRepository implements AuctionRepository {

	private final AuctionJpaRepository repository;

	JpaAuctionRepository(AuctionJpaRepository repository) {
		this.repository = repository;
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

	@Override
	public AuctionPage<Auction> findOpen(Instant now, PageQuery query) {
		Pageable pageable = PageRequest.of(query.number(), query.size(),
				Sort.by("endsAt").ascending().and(Sort.by("id").ascending()));
		Slice<AuctionEntity> slice = repository.findByStatusAndEndsAtAfter(AuctionStatus.OPEN, now, pageable);
		List<Auction> items = slice.getContent().stream().map(this::toDomain).toList();
		return new AuctionPage<>(items, query.number(), query.size(), slice.hasNext());
	}

	Auction findById(long id) {
		return repository.findById(id).map(this::toDomain).orElseThrow();
	}

	private Auction toDomain(AuctionEntity entity) {
		return new Auction(
				entity.getId(),
				new UserId(entity.getSellerId()),
				entity.getCar().toDomain(),
				new Money(entity.getStartingPrice()),
				new Money(entity.getMinIncrement()),
				entity.getEndsAt(),
				entity.getStatus());
	}

}

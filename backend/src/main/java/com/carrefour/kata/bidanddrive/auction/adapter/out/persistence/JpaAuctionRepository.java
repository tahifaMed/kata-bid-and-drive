package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.auction.application.AuctionRepository;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.springframework.stereotype.Component;

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

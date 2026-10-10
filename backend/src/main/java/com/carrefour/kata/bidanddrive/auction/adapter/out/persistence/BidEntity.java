package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bid")
class BidEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "auction_id", nullable = false)
	private long auctionId;

	@Column(name = "bidder_id", nullable = false)
	private long bidderId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "placed_at", nullable = false)
	private Instant placedAt;

	protected BidEntity() {
	}

	BidEntity(Bid bid) {
		this.auctionId = bid.auctionId();
		this.bidderId = bid.bidderId().value();
		this.amount = bid.amount().amount();
		this.placedAt = bid.placedAt();
	}

	Bid toDomain() {
		return new Bid(id, auctionId, new UserId(bidderId), new Money(amount), placedAt);
	}

}

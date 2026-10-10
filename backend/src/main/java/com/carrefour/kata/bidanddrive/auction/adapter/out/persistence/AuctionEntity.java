package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "auction")
class AuctionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "seller_id", nullable = false)
	private long sellerId;

	@Column(name = "starting_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal startingPrice;

	@Column(name = "min_increment", nullable = false, precision = 12, scale = 2)
	private BigDecimal minIncrement;

	@Column(name = "ends_at", nullable = false)
	private Instant endsAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private AuctionStatus status;

	@OneToOne(mappedBy = "auction", cascade = CascadeType.ALL)
	private CarEntity car;

	@Column(name = "current_bid", precision = 12, scale = 2)
	private BigDecimal currentBid;

	@Column(name = "bid_count", nullable = false)
	private int bidCount;

	@Column(name = "highest_bidder_id")
	private Long highestBidderId;

	@Version
	@Column(nullable = false)
	private long version;

	protected AuctionEntity() {
	}

	AuctionEntity(long sellerId, BigDecimal startingPrice, BigDecimal minIncrement, Instant endsAt,
			AuctionStatus status) {
		this.sellerId = sellerId;
		this.startingPrice = startingPrice;
		this.minIncrement = minIncrement;
		this.endsAt = endsAt;
		this.status = status;
	}

	Long getId() {
		return id;
	}

	long getSellerId() {
		return sellerId;
	}

	BigDecimal getStartingPrice() {
		return startingPrice;
	}

	BigDecimal getMinIncrement() {
		return minIncrement;
	}

	Instant getEndsAt() {
		return endsAt;
	}

	AuctionStatus getStatus() {
		return status;
	}

	CarEntity getCar() {
		return car;
	}

	BigDecimal getCurrentBid() {
		return currentBid;
	}

	int getBidCount() {
		return bidCount;
	}

	Long getHighestBidderId() {
		return highestBidderId;
	}

	long getVersion() {
		return version;
	}

	void attachCar(CarEntity car) {
		this.car = car;
	}

	void recordBid(BigDecimal amount, long bidderId, int bidCount) {
		this.currentBid = amount;
		this.highestBidderId = bidderId;
		this.bidCount = bidCount;
	}

}

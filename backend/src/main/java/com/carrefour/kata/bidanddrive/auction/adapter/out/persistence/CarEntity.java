package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.auction.domain.Car;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "car")
class CarEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(optional = false)
	@JoinColumn(name = "auction_id", nullable = false, unique = true)
	private AuctionEntity auction;

	@Column(nullable = false, length = 60)
	private String make;

	@Column(nullable = false, length = 60)
	private String model;

	@Column(name = "year", nullable = false)
	private int year;

	@Column(name = "mileage_km", nullable = false)
	private int mileageKm;

	@Column(length = 2000)
	private String description;

	@Column(name = "image_url", length = 2048)
	private String imageUrl;

	protected CarEntity() {
	}

	CarEntity(AuctionEntity auction, Car car) {
		this.auction = auction;
		this.make = car.make();
		this.model = car.model();
		this.year = car.year();
		this.mileageKm = car.mileageKm();
		this.description = car.description();
		this.imageUrl = car.imageUrl();
	}

	AuctionEntity getAuction() {
		return auction;
	}

	Car toDomain() {
		return new Car(make, model, year, mileageKm, description, imageUrl);
	}

}

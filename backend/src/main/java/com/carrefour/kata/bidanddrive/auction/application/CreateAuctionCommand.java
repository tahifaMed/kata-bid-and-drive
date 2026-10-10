package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateAuctionCommand(
		UserId sellerId,
		Car car,
		BigDecimal startingPrice,
		BigDecimal minIncrement,
		Instant endsAt) {}

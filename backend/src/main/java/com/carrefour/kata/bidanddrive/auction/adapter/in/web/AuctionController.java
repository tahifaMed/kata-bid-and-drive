package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.CreateAuction;
import com.carrefour.kata.bidanddrive.auction.application.CreateAuctionCommand;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.identity.adapter.in.web.CurrentUser;
import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/auctions")
class AuctionController {

	private final CreateAuction createAuction;
	private final UserDirectory users;

	AuctionController(CreateAuction createAuction, UserDirectory users) {
		this.createAuction = createAuction;
		this.users = users;
	}

	/** {@code @CurrentUser} runs before the body is validated, so a missing identity is always a 401. */
	@PostMapping
	ResponseEntity<AuctionResponse> create(@CurrentUser UserId sellerId,
			@Valid @RequestBody CreateAuctionRequest request) {
		Auction auction = createAuction.execute(new CreateAuctionCommand(
				sellerId,
				toCar(request.car()),
				new BigDecimal(request.startingPrice()),
				new BigDecimal(request.minIncrement()),
				OffsetDateTime.parse(request.endsAt()).toInstant()));

		// The @CurrentUser resolver has already confirmed this user exists.
		String sellerName = users.findById(sellerId)
				.map(User::displayName)
				.orElseThrow(() -> new IllegalStateException("seller " + sellerId + " vanished mid-request"));
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/auctions/{id}")
				.buildAndExpand(auction.id())
				.toUri();
		return ResponseEntity.status(HttpStatus.CREATED)
				.location(location)
				.body(AuctionResponse.of(auction, sellerName, location));
	}

	private static Car toCar(CreateAuctionRequest.CarRequest car) {
		return new Car(car.make(), car.model(), car.year(), car.mileageKm(), car.description(), car.imageUrl());
	}

}

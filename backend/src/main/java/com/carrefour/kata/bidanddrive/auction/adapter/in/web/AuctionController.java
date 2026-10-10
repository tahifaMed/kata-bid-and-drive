package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.AuctionDetail;
import com.carrefour.kata.bidanddrive.auction.application.AuctionPage;
import com.carrefour.kata.bidanddrive.auction.application.CreateAuction;
import com.carrefour.kata.bidanddrive.auction.application.CreateAuctionCommand;
import com.carrefour.kata.bidanddrive.auction.application.GetAuctionDetail;
import com.carrefour.kata.bidanddrive.auction.application.ListOpenAuctions;
import com.carrefour.kata.bidanddrive.auction.application.PageQuery;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.identity.adapter.in.web.CurrentUser;
import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auctions")
class AuctionController {

	private final CreateAuction createAuction;
	private final ListOpenAuctions listOpenAuctions;
	private final GetAuctionDetail getAuctionDetail;
	private final UserDirectory users;

	AuctionController(CreateAuction createAuction, ListOpenAuctions listOpenAuctions,
			GetAuctionDetail getAuctionDetail, UserDirectory users) {
		this.createAuction = createAuction;
		this.listOpenAuctions = listOpenAuctions;
		this.getAuctionDetail = getAuctionDetail;
		this.users = users;
	}

	/** Public, like the list. A non-numeric id is a 400 through the type mismatch handler. */
	@GetMapping("/{id}")
	AuctionResponse detail(@PathVariable long id) {
		return AuctionResponse.of(getAuctionDetail.execute(id), auctionUri(id));
	}

	/** Public: no identity is needed to browse. Paging bounds are checked before the use case runs. */
	@GetMapping
	AuctionListResponse list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		AuctionPage<Auction> result = listOpenAuctions.execute(new PageQuery(page, size));

		List<AuctionListItem> items = result.items().stream()
				.map(auction -> AuctionListItem.of(auction, auctionUri(auction.id())))
				.toList();
		Map<String, AuctionResponse.Link> links = new LinkedHashMap<>();
		links.put("self", new AuctionResponse.Link(pageUri(page, size).toString()));
		if (page > 0) {
			links.put("prev", new AuctionResponse.Link(pageUri(page - 1, size).toString()));
		}
		if (result.hasNext()) {
			links.put("next", new AuctionResponse.Link(pageUri(page + 1, size).toString()));
		}
		return new AuctionListResponse(new AuctionListResponse.Embedded(items),
				new AuctionListResponse.PageInfo(size, page), links);
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
		URI location = auctionUri(auction.id());
		// A new auction has no bids and ends inside the window, so it is biddable (ADR 0010).
		AuctionDetail detail = new AuctionDetail(auction, sellerName, List.of(), true);
		return ResponseEntity.status(HttpStatus.CREATED)
				.location(location)
				.body(AuctionResponse.of(detail, location));
	}

	private static URI pageUri(int page, int size) {
		return ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/auctions")
				.queryParam("page", page)
				.queryParam("size", size)
				.build()
				.toUri();
	}

	private static URI auctionUri(Long id) {
		return ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/auctions/{id}")
				.buildAndExpand(id)
				.toUri();
	}

	private static Car toCar(CreateAuctionRequest.CarRequest car) {
		return new Car(car.make(), car.model(), car.year(), car.mileageKm(), car.description(), car.imageUrl());
	}

}

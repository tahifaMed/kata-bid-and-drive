package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.GetBid;
import com.carrefour.kata.bidanddrive.auction.application.PlaceBid;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.identity.adapter.in.web.CurrentUser;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/auctions/{auctionId}/bids")
class BidController {

	private final PlaceBid placeBid;
	private final GetBid getBid;

	BidController(PlaceBid placeBid, GetBid getBid) {
		this.placeBid = placeBid;
		this.getBid = getBid;
	}

	/**
	 * {@code @CurrentUser} runs first, so a missing identity is always a 401. The body has no {@code @Valid}:
	 * the use case checks the amount after the auction lookup (ADR 0005).
	 */
	@PostMapping
	ResponseEntity<BidResponse> place(@CurrentUser UserId bidder, @PathVariable long auctionId,
			@RequestBody PlaceBidRequest request) {
		Bid bid = placeBid.execute(auctionId, bidder, request.toRawAmount());
		URI location = bidUri(auctionId, bid.id());
		return ResponseEntity.status(HttpStatus.CREATED)
				.location(location)
				.body(BidResponse.of(bid, location, auctionUri(auctionId)));
	}

	/** Only the bidder sees their bid; anyone else gets a 404 (ADR 0013). */
	@GetMapping("/{bidId}")
	BidResponse read(@CurrentUser UserId viewer, @PathVariable long auctionId, @PathVariable long bidId) {
		Bid bid = getBid.execute(auctionId, bidId, viewer);
		return BidResponse.of(bid, bidUri(auctionId, bidId), auctionUri(auctionId));
	}

	private static URI bidUri(long auctionId, long bidId) {
		return ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/auctions/{auctionId}/bids/{bidId}")
				.buildAndExpand(auctionId, bidId)
				.toUri();
	}

	private static URI auctionUri(long auctionId) {
		return ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/auctions/{id}")
				.buildAndExpand(auctionId)
				.toUri();
	}

}

package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.ListMyBids;
import com.carrefour.kata.bidanddrive.identity.adapter.in.web.CurrentUser;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me/bids")
class MyBidsController {

	private final ListMyBids listMyBids;

	MyBidsController(ListMyBids listMyBids) {
		this.listMyBids = listMyBids;
	}

	/** Anything under {@code /api/me} needs identity, so a missing or unknown one is a 401. */
	@GetMapping
	MyBidsResponse list(@CurrentUser UserId bidder) {
		List<MyBidItem> items = listMyBids.execute(bidder).stream()
				.map(standing -> MyBidItem.of(standing, auctionUri(standing.auction().id())))
				.toList();
		URI self = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/me/bids").build().toUri();
		return new MyBidsResponse(new MyBidsResponse.Embedded(items),
				Map.of("self", new AuctionResponse.Link(self.toString())));
	}

	private static URI auctionUri(long id) {
		return ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/auctions/{id}")
				.buildAndExpand(id)
				.toUri();
	}

}

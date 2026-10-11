package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.adapter.config.EventsProperties;
import com.carrefour.kata.bidanddrive.auction.adapter.out.events.AuctionEventBroker;
import com.carrefour.kata.bidanddrive.auction.adapter.out.events.SseEmitterSink;
import com.carrefour.kata.bidanddrive.auction.application.CheckAuctionExists;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** The live event stream of one auction (spec 009). Public: no identity is read (ADR 0014). */
@RestController
@RequestMapping("/api/auctions")
class AuctionEventsController {

	private final CheckAuctionExists checkAuctionExists;
	private final AuctionEventBroker broker;
	private final EventsProperties properties;

	AuctionEventsController(CheckAuctionExists checkAuctionExists, AuctionEventBroker broker,
			EventsProperties properties) {
		this.checkAuctionExists = checkAuctionExists;
		this.broker = broker;
		this.properties = properties;
	}

	/** An unknown auction is a 404 before any stream opens. */
	@GetMapping(path = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	SseEmitter events(@PathVariable long id) {
		checkAuctionExists.execute(id);
		SseEmitter emitter = new SseEmitter(properties.emitterTimeout().toMillis());
		broker.subscribe(id, new SseEmitterSink(emitter));
		return emitter;
	}

}

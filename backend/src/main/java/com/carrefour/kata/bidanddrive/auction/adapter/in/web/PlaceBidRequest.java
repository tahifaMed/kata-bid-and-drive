package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.RawAmount;
import tools.jackson.databind.JsonNode;

/**
 * Raw JSON, not {@code String}: a JSON number for a {@code String} field fails while parsing, before the
 * auction lookup, and an unknown auction must be a 404 even then (ADR 0005). The use case validates the text.
 */
record PlaceBidRequest(JsonNode amount) {

	RawAmount toRawAmount() {
		return new RawAmount(amount != null && amount.isString() ? amount.asString() : null);
	}

}

package com.carrefour.kata.bidanddrive.auction.domain;

/** Hides bidder names in the public bid history: the first character, then {@code ***} (spec 006). */
public final class BidderName {

	private static final String MASK = "***";

	private BidderName() {
	}

	public static String mask(String displayName) {
		if (displayName.isEmpty()) {
			return MASK;
		}
		return Character.toString(displayName.codePointAt(0)) + MASK;
	}

}

package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStatus;

/** One item of the caller's bids with its status, computed at the instant of the request (spec 011). */
public record MyBidView(BidderStanding standing, BidderStatus status) {}

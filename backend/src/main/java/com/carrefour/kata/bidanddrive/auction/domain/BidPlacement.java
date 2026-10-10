package com.carrefour.kata.bidanddrive.auction.domain;

/** The result of an accepted bid: the auction with its new totals, and the bid to store. */
public record BidPlacement(Auction auction, Bid bid) {}

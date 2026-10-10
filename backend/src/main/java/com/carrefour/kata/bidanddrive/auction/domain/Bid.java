package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.time.Instant;

/** A placed bid. Bids are immutable (ADR 0006). {@code id} is null until the bid is saved. */
public record Bid(Long id, long auctionId, UserId bidderId, Money amount, Instant placedAt) {}

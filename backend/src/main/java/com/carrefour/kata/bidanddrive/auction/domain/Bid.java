package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.time.Instant;

/** A placed bid. Bids are immutable (ADR 0006). Spec 007 stores and creates them. */
public record Bid(long id, UserId bidderId, Money amount, Instant placedAt) {}

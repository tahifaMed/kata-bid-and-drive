package com.carrefour.kata.bidanddrive.auction.adapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Bounds for an auction's end time, measured from now. See ADR 0008. */
@ConfigurationProperties(prefix = "bidanddrive.auction")
public record AuctionProperties(
		@DefaultValue("PT1H") Duration minDuration,
		@DefaultValue("P30D") Duration maxDuration) {}

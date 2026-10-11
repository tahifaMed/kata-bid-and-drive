package com.carrefour.kata.bidanddrive.auction.adapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Live event streams (spec 009). Every open stream gets a {@code keepalive} comment each {@code heartbeat}.
 * An emitter is closed after {@code emitterTimeout}; the client reconnects.
 */
@ConfigurationProperties(prefix = "bidanddrive.events")
public record EventsProperties(
		@DefaultValue("PT25S") Duration heartbeat,
		@DefaultValue("PT30M") Duration emitterTimeout) {}

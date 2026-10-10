package com.carrefour.kata.bidanddrive.auction.application;

/** A bid amount as the client sent it. {@code text} is null when the amount is missing or not a JSON string. */
public record RawAmount(String text) {}

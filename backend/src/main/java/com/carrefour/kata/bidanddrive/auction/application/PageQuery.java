package com.carrefour.kata.bidanddrive.auction.application;

/** A 0-based page of a list. The edge validates the bounds before a query is built. */
public record PageQuery(int number, int size) {}

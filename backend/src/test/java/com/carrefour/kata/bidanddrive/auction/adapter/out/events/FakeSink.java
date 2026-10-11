package com.carrefour.kata.bidanddrive.auction.adapter.out.events;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Records what the broker sends, and can be told to fail like a client that went away. */
final class FakeSink implements EventSink {

	final List<String> events = new CopyOnWriteArrayList<>();
	final List<String> comments = new CopyOnWriteArrayList<>();
	Exception failure;
	RuntimeException closeFailure;
	Throwable completedWithError;
	boolean completed;
	Runnable completion;
	Runnable timeout;
	Consumer<Throwable> error;

	@Override
	public void send(String name, String data) throws IOException {
		fail();
		events.add(name + " " + data);
	}

	@Override
	public void comment(String text) throws IOException {
		fail();
		comments.add(text);
	}

	@Override
	public void complete() {
		if (closeFailure != null) {
			throw closeFailure;
		}
		completed = true;
	}

	@Override
	public void completeWithError(Throwable cause) {
		if (closeFailure != null) {
			throw closeFailure;
		}
		completedWithError = cause;
	}

	@Override
	public void onCompletion(Runnable callback) {
		completion = callback;
	}

	@Override
	public void onTimeout(Runnable callback) {
		timeout = callback;
	}

	@Override
	public void onError(Consumer<Throwable> callback) {
		error = callback;
	}

	private void fail() throws IOException {
		switch (failure) {
			case null -> { }
			case IOException io -> throw io;
			case RuntimeException runtime -> throw runtime;
			default -> throw new IllegalStateException(failure);
		}
	}

}

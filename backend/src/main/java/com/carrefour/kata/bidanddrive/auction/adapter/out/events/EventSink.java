package com.carrefour.kata.bidanddrive.auction.adapter.out.events;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * One open stream to a client. Production wraps an {@code SseEmitter}; tests use a fake, because an
 * {@code SseEmitter} only fires its callbacks once a servlet container is attached.
 */
public interface EventSink {

	void send(String name, String data) throws IOException;

	void comment(String text) throws IOException;

	void complete();

	void completeWithError(Throwable cause);

	void onCompletion(Runnable callback);

	void onTimeout(Runnable callback);

	void onError(Consumer<Throwable> callback);

}

package com.carrefour.kata.bidanddrive.auction.adapter.out.events;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.function.Consumer;

/** An {@link EventSink} over Spring's {@link SseEmitter}, which serialises concurrent sends itself. */
public final class SseEmitterSink implements EventSink {

	private final SseEmitter emitter;

	public SseEmitterSink(SseEmitter emitter) {
		this.emitter = emitter;
	}

	@Override
	public void send(String name, String data) throws IOException {
		emitter.send(SseEmitter.event().name(name).data(data));
	}

	@Override
	public void comment(String text) throws IOException {
		emitter.send(SseEmitter.event().comment(text));
	}

	@Override
	public void complete() {
		emitter.complete();
	}

	@Override
	public void completeWithError(Throwable cause) {
		emitter.completeWithError(cause);
	}

	@Override
	public void onCompletion(Runnable callback) {
		emitter.onCompletion(callback);
	}

	@Override
	public void onTimeout(Runnable callback) {
		emitter.onTimeout(callback);
	}

	@Override
	public void onError(Consumer<Throwable> callback) {
		emitter.onError(callback);
	}

}

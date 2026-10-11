package com.carrefour.kata.bidanddrive.auction.adapter.out.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds the open streams per auction, in memory, for one application instance (ADR 0002).
 * Sends are synchronous. A sink that fails is completed with the error and removed; no exception
 * leaves the broker, so a committed bid never fails because of a subscriber (ADR 0015).
 */
public class AuctionEventBroker {

	static final String CONNECTED = "connected";
	static final String HEARTBEAT = "keepalive";

	private static final Logger log = LoggerFactory.getLogger(AuctionEventBroker.class);

	private final Map<Long, List<EventSink>> subscribers = new ConcurrentHashMap<>();
	private volatile boolean closed;

	/**
	 * Sends a {@code connected} comment at once: an SSE response is only flushed on its first write.
	 * The add runs inside {@code compute}, so it cannot land on a list that a last unsubscribe just dropped.
	 * After {@link #closeAll()}, a new stream is ended at once and the client reconnects elsewhere.
	 */
	public void subscribe(long auctionId, EventSink sink) {
		if (closed) {
			complete(sink);
			return;
		}
		sink.onCompletion(() -> unsubscribe(auctionId, sink));
		sink.onTimeout(() -> unsubscribe(auctionId, sink));
		sink.onError(error -> unsubscribe(auctionId, sink));
		subscribers.compute(auctionId, (id, sinks) -> {
			List<EventSink> list = sinks == null ? new CopyOnWriteArrayList<>() : sinks;
			list.add(sink);
			return list;
		});
		deliver(auctionId, sink, () -> sink.comment(CONNECTED));
	}

	public void unsubscribe(long auctionId, EventSink sink) {
		subscribers.computeIfPresent(auctionId, (id, sinks) -> {
			sinks.remove(sink);
			return sinks.isEmpty() ? null : sinks;
		});
	}

	public int subscriberCount(long auctionId) {
		List<EventSink> sinks = subscribers.get(auctionId);
		return sinks == null ? 0 : sinks.size();
	}

	public void broadcast(long auctionId, String name, String data) {
		for (EventSink sink : subscribers.getOrDefault(auctionId, List.of())) {
			deliver(auctionId, sink, () -> sink.send(name, data));
		}
	}

	public void heartbeat() {
		subscribers.forEach((auctionId, sinks) -> {
			for (EventSink sink : sinks) {
				deliver(auctionId, sink, () -> sink.comment(HEARTBEAT));
			}
		});
	}

	/** Accepts streams again after {@link #closeAll()}, when a stopped application context is restarted. */
	public void open() {
		closed = false;
	}

	/** Ends every stream, so a shutdown does not wait for clients that would stay connected. They reconnect. */
	public void closeAll() {
		closed = true;
		subscribers.forEach((auctionId, sinks) -> {
			for (EventSink sink : sinks) {
				unsubscribe(auctionId, sink);
				complete(sink);
			}
		});
	}

	private static void complete(EventSink sink) {
		try {
			sink.complete();
		} catch (RuntimeException alreadyClosed) {
			log.debug("Subscriber was already closed: {}", alreadyClosed.toString());
		}
	}

	private void deliver(long auctionId, EventSink sink, Send send) {
		try {
			send.run();
		} catch (Exception failure) {
			log.debug("Dropping a subscriber of auction {}: {}", auctionId, failure.toString());
			unsubscribe(auctionId, sink);
			close(sink, failure);
		}
	}

	private static void close(EventSink sink, Exception failure) {
		try {
			sink.completeWithError(failure);
		} catch (RuntimeException alreadyClosed) {
			log.debug("Subscriber was already closed: {}", alreadyClosed.toString());
		}
	}

	@FunctionalInterface
	private interface Send {

		void run() throws Exception;

	}

}

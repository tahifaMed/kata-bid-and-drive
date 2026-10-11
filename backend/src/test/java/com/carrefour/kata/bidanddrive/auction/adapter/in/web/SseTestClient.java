package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * Opens an event stream like a browser would and records every line it reads, on a background thread.
 * Lines are kept raw; {@link #events(String)} and {@link #comments()} parse them by the SSE rules.
 */
final class SseTestClient implements AutoCloseable {

	private final HttpClient http = HttpClient.newHttpClient();
	private final List<String> lines = new CopyOnWriteArrayList<>();
	private HttpResponse<Stream<String>> response;
	private Thread reader;

	static SseTestClient open(String url, Map<String, String> headers) {
		SseTestClient client = new SseTestClient();
		client.connect(url, headers);
		return client;
	}

	private void connect(String url, Map<String, String> headers) {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
				.header("Accept", "text/event-stream")
				.timeout(Duration.ofSeconds(5));
		headers.forEach(request::header);
		try {
			response = http.send(request.build(), HttpResponse.BodyHandlers.ofLines());
		} catch (IOException failure) {
			throw new UncheckedIOException(failure);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(interrupted);
		}
		reader = Thread.ofVirtual().start(() -> {
			try (Stream<String> body = response.body()) {
				body.forEach(lines::add);
			} catch (UncheckedIOException closed) {
				// The test closed the stream.
			}
		});
	}

	int status() {
		return response.statusCode();
	}

	Optional<String> contentType() {
		return response.headers().firstValue("Content-Type");
	}

	List<String> lines() {
		return List.copyOf(lines);
	}

	/** The {@code data:} of each message whose {@code event:} field is {@code name}. */
	List<String> events(String name) {
		List<String> data = new CopyOnWriteArrayList<>();
		String current = null;
		for (String line : lines) {
			if (line.isEmpty()) {
				current = null;
			} else if (field(line, "event") != null) {
				current = field(line, "event");
			} else if (field(line, "data") != null && name.equals(current)) {
				data.add(field(line, "data"));
			}
		}
		return data;
	}

	/** Comment lines, without the leading colon and optional space. */
	List<String> comments() {
		return lines.stream()
				.filter(line -> line.startsWith(":"))
				.map(line -> stripSpace(line.substring(1)))
				.toList();
	}

	@Override
	public void close() {
		http.shutdownNow();
		if (reader != null) {
			reader.interrupt();
		}
	}

	private static String field(String line, String name) {
		return line.startsWith(name + ":") ? stripSpace(line.substring(name.length() + 1)) : null;
	}

	private static String stripSpace(String value) {
		return value.startsWith(" ") ? value.substring(1) : value;
	}

}

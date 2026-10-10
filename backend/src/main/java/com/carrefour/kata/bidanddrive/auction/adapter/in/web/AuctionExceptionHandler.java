package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.AuctionNotFoundException;
import com.carrefour.kata.bidanddrive.auction.domain.InvalidAuctionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice(assignableTypes = AuctionController.class)
class AuctionExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleInvalidFields(MethodArgumentNotValidException exception) {
		List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage()))
				.toList();
		return invalid(errors);
	}

	/** Paging bounds from {@code @Min} and {@code @Max} on the list parameters. */
	@ExceptionHandler(HandlerMethodValidationException.class)
	ProblemDetail handleInvalidParameters(HandlerMethodValidationException exception) {
		List<Map<String, String>> errors = exception.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> Map.of("field", result.getMethodParameter().getParameterName(),
								"message", error.getDefaultMessage())))
				.toList();
		return invalid(errors);
	}

	@ExceptionHandler(AuctionNotFoundException.class)
	ProblemDetail handleNotFound(AuctionNotFoundException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setTitle("Auction not found");
		return problem;
	}

	/** A paging value or an auction id that is not a whole number, such as {@code page=abc}. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleNotANumber(MethodArgumentTypeMismatchException exception) {
		return invalid(List.of(Map.of("field", exception.getName(), "message", "must be a whole number")));
	}

	@ExceptionHandler(InvalidAuctionException.class)
	ProblemDetail handleDomainRule(InvalidAuctionException exception) {
		return invalid(List.of(Map.of("field", exception.field(), "message", exception.getMessage())));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadable(HttpMessageNotReadableException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Request body is malformed or has a value of the wrong type");
		problem.setTitle("Invalid auction");
		problem.setProperty("errors", List.of());
		return problem;
	}

	private static ProblemDetail invalid(List<Map<String, String>> errors) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"One or more fields are invalid");
		problem.setTitle("Invalid auction");
		problem.setProperty("errors", errors);
		return problem;
	}

}

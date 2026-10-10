package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.AuctionNotFoundException;
import com.carrefour.kata.bidanddrive.auction.application.BidNotFoundException;
import com.carrefour.kata.bidanddrive.auction.application.InvalidBidException;
import com.carrefour.kata.bidanddrive.auction.domain.AlreadyHighestBidderException;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionClosedException;
import com.carrefour.kata.bidanddrive.auction.domain.BidBelowMinimumException;
import com.carrefour.kata.bidanddrive.auction.domain.BidRejectedException;
import com.carrefour.kata.bidanddrive.auction.domain.SellerCannotBidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

/** Status codes for bids (ADR 0005). 400 bodies follow ADR 0011. */
@RestControllerAdvice(assignableTypes = BidController.class)
class BidExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(BidExceptionHandler.class);

	@ExceptionHandler(BidRejectedException.class)
	ProblemDetail handleRejected(BidRejectedException rejection) {
		return switch (rejection) {
			case SellerCannotBidException seller -> problem(HttpStatus.FORBIDDEN, "Seller cannot bid", seller);
			case AuctionClosedException closed -> problem(HttpStatus.CONFLICT, "Auction closed", closed);
			case AlreadyHighestBidderException highest ->
					problem(HttpStatus.CONFLICT, "Already highest bidder", highest);
			case BidBelowMinimumException low -> {
				ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Bid too low", low);
				problem.setProperty("minimumBid", low.minimumBid().amount().toPlainString());
				yield problem;
			}
		};
	}

	@ExceptionHandler(InvalidBidException.class)
	ProblemDetail handleInvalidAmount(InvalidBidException exception) {
		return invalid("One or more fields are invalid",
				List.of(Map.of("field", exception.field(), "message", exception.getMessage())));
	}

	/** A body that is not a JSON object. A bad amount inside an object reaches the use case instead. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadable(HttpMessageNotReadableException exception) {
		return invalid("Request body is malformed", List.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleNotANumber(MethodArgumentTypeMismatchException exception) {
		return invalid("One or more fields are invalid",
				List.of(Map.of("field", exception.getName(), "message", "must be a whole number")));
	}

	@ExceptionHandler(AuctionNotFoundException.class)
	ProblemDetail handleNotFound(AuctionNotFoundException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setTitle("Auction not found");
		return problem;
	}

	@ExceptionHandler(BidNotFoundException.class)
	ProblemDetail handleBidNotFound(BidNotFoundException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setTitle("Bid not found");
		return problem;
	}

	/** Broken data. The cause stays in the logs. */
	@ExceptionHandler(IllegalStateException.class)
	ProblemDetail handleBrokenData(IllegalStateException exception) {
		log.error("Bid request failed on inconsistent data", exception);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"The request could not be completed");
		problem.setTitle("Internal error");
		return problem;
	}

	private static ProblemDetail problem(HttpStatus status, String title, BidRejectedException rejection) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, rejection.getMessage());
		problem.setTitle(title);
		return problem;
	}

	private static ProblemDetail invalid(String detail, List<Map<String, String>> errors) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Invalid bid");
		problem.setProperty("errors", errors);
		return problem;
	}

}

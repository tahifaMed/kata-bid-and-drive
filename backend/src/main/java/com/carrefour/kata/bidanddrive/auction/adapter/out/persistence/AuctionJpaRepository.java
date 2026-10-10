package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

interface AuctionJpaRepository extends JpaRepository<AuctionEntity, Long> {

	/** Loads the car with the auction, so listing does not run one query per row. */
	@EntityGraph(attributePaths = "car")
	Slice<AuctionEntity> findByStatusAndEndsAtAfter(AuctionStatus status, Instant now, Pageable pageable);

	/** Loads the car in the same query as the auction. */
	@EntityGraph(attributePaths = "car")
	Optional<AuctionEntity> findWithCarById(long id);

}

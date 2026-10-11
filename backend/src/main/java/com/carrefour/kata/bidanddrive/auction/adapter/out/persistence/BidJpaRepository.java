package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface BidJpaRepository extends JpaRepository<BidEntity, Long> {

	List<BidEntity> findByAuctionId(long auctionId, Pageable pageable);

	Optional<BidEntity> findByIdAndAuctionId(long id, long auctionId);

	/**
	 * One statement, so the auction's leader and the bidder's highest bid come from the same snapshot.
	 * Each row is the auction with its car, then the bidder's highest amount on it.
	 */
	@Query("""
			select a, (select max(b.amount) from BidEntity b where b.auctionId = a.id and b.bidderId = :bidderId)
			from AuctionEntity a join fetch a.car
			where exists (select 1 from BidEntity mine where mine.auctionId = a.id and mine.bidderId = :bidderId)
			order by a.endsAt, a.id
			""")
	List<Object[]> findStandings(@Param("bidderId") long bidderId);

}

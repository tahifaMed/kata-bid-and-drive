package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface BidJpaRepository extends JpaRepository<BidEntity, Long> {

	List<BidEntity> findByAuctionId(long auctionId, Pageable pageable);

	Optional<BidEntity> findByIdAndAuctionId(long id, long auctionId);

}

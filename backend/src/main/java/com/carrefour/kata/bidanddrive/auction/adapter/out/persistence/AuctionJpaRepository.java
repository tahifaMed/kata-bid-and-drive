package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface AuctionJpaRepository extends JpaRepository<AuctionEntity, Long> {}

package com.carrefour.kata.bidanddrive.auction.adapter.config;

import com.carrefour.kata.bidanddrive.auction.application.AuctionRepository;
import com.carrefour.kata.bidanddrive.auction.application.BidRepository;
import com.carrefour.kata.bidanddrive.auction.application.CreateAuction;
import com.carrefour.kata.bidanddrive.auction.application.GetAuctionDetail;
import com.carrefour.kata.bidanddrive.auction.application.ListOpenAuctions;
import com.carrefour.kata.bidanddrive.auction.application.PlaceBid;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionWindow;
import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AuctionProperties.class)
class AuctionConfiguration {

	@Bean
	AuctionWindow auctionWindow(AuctionProperties properties) {
		return new AuctionWindow(properties.minDuration(), properties.maxDuration());
	}

	@Bean
	CreateAuction createAuction(AuctionRepository auctions, Clock clock, AuctionWindow window) {
		return new CreateAuction(auctions, clock, window);
	}

	@Bean
	ListOpenAuctions listOpenAuctions(AuctionRepository auctions, Clock clock) {
		return new ListOpenAuctions(auctions, clock);
	}

	@Bean
	GetAuctionDetail getAuctionDetail(AuctionRepository auctions, BidRepository bids, UserDirectory users,
			Clock clock) {
		return new GetAuctionDetail(auctions, bids, users, clock);
	}

	@Bean
	PlaceBid placeBid(AuctionRepository auctions, Clock clock) {
		return new PlaceBid(auctions, clock);
	}

}

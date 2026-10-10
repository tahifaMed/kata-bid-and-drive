package com.carrefour.kata.bidanddrive.auction.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BidderNameTests {

	@Test
	void keeps_the_first_letter_and_hides_the_rest() {
		assertThat(BidderName.mask("Bob")).isEqualTo("B***");
	}

	@Test
	void a_one_letter_name_still_gets_the_mask() {
		assertThat(BidderName.mask("A")).isEqualTo("A***");
	}

	@Test
	void an_empty_name_is_only_the_mask() {
		assertThat(BidderName.mask("")).isEqualTo("***");
	}

	@Test
	void keeps_a_whole_character_outside_the_basic_plane() {
		assertThat(BidderName.mask("𝔸lice")).isEqualTo("𝔸***");
	}

}

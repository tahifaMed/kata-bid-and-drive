package com.carrefour.kata.bidanddrive.shared;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * A JSON number is not accepted where a string is expected. Money is sent as a string (ADR 0004),
 * so {@code "startingPrice": 12000.00} must fail instead of being coerced to "12000.00".
 */
@Configuration
class JacksonConfiguration {

	@Bean
	JsonMapperBuilderCustomizer rejectNumbersForStrings() {
		return builder -> builder.withCoercionConfig(LogicalType.Textual, config -> config
				.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
				.setCoercion(CoercionInputShape.Float, CoercionAction.Fail));
	}

}

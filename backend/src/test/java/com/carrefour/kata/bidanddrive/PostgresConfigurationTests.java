package com.carrefour.kata.bidanddrive;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * Validates that the application runs on PostgreSQL with the expected configuration.
 * Uses the Testcontainers PostgreSQL started by TestcontainersConfiguration.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PostgresConfigurationTests {

	@Autowired
	DataSource dataSource;

	@Autowired
	Flyway flyway;

	@Autowired
	Environment environment;

	@Test
	void connectsToPostgreSql() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			var metaData = connection.getMetaData();

			assertThat(metaData.getDatabaseProductName()).isEqualTo("PostgreSQL");
			assertThat(metaData.getDatabaseMajorVersion())
					.as("must match the postgres version pinned in docker-compose.yml")
					.isEqualTo(18);
		}
	}

	@Test
	void appliesFlywayMigrations() {
		assertThat(flyway.info().applied())
				.as("baseline migration V1 must be applied on startup")
				.isNotEmpty();
		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
	}

	@Test
	void hibernateValidatesSchemaInsteadOfCreatingIt() {
		assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
	}

	@Test
	void openInViewIsDisabled() {
		assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class)).isFalse();
	}

}

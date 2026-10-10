package com.carrefour.kata.bidanddrive;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;

/**
 * Validates that the application runs on PostgreSQL with the expected configuration.
 * Uses the Testcontainers PostgreSQL started by TestcontainersConfiguration.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PostgresConfigurationTests {

	private final DataSource dataSource;
	private final Flyway flyway;
	private final EntityManagerFactory entityManagerFactory;
	private final ApplicationContext context;

	@Autowired
	PostgresConfigurationTests(
			DataSource dataSource,
			Flyway flyway,
			EntityManagerFactory entityManagerFactory,
			ApplicationContext context) {
		this.dataSource = dataSource;
		this.flyway = flyway;
		this.entityManagerFactory = entityManagerFactory;
		this.context = context;
	}

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
		assertThat(entityManagerFactory.getProperties())
				.as("effective Hibernate setting on the running EntityManagerFactory")
				.containsEntry("hibernate.hbm2ddl.auto", "validate");
	}

	@Test
	void openInViewInterceptorIsNotRegistered() {
		assertThat(context.getBeansOfType(OpenEntityManagerInViewInterceptor.class))
				.as("spring.jpa.open-in-view=false means no OSIV interceptor bean")
				.isEmpty();
	}

}

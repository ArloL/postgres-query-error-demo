package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public class DemoTest {

	private static final String NEXT_SYNC_EVENT_QUERY = """
			DELETE FROM queue
			WHERE
				id IN (
					SELECT id
					FROM queue
					ORDER BY id
					LIMIT 1
					FOR UPDATE
					SKIP LOCKED
				)
			RETURNING *
			""";

	@Test
	void loadTestQueue() throws Exception {
		try (PostgreSQLContainer<?> database = new PostgreSQLContainer<>(
				"postgres:15.1-alpine"
		)) {
			database.withUrlParam("preparedStatementCacheQueries", "0")
					.withUrlParam("preparedStatementCacheSizeMiB", "0")
					.withUrlParam("prepareThreshold", "0")
					.waitingFor(
							new WaitAllStrategy()
									.withStrategy(Wait.forListeningPort())
									.withStrategy(
											Wait.forLogMessage(
													".*database system is ready to accept connections.*\\s",
													2
											)
									)
					)
					.withInitScript("schema.sql")
					.start();
			try (Connection connection = database.createConnection("")) {
				insertAndRemoveMessages(connection);
			}
		}
	}

	private void insertAndRemoveMessages(Connection connection)
			throws SQLException {
		int total = 128_000;
		int step = 3;

		for (int i = 0; i < total; i += step) {
			System.out.println(i);

			try (PreparedStatement insertStatement = connection
					.prepareStatement(
							"INSERT INTO queue (action) VALUES (?)"
					)) {
				for (int j = 0; j < step; j++) {
					insertStatement.setString(1, "action" + j);
					insertStatement.execute();
				}
			}

			try (PreparedStatement statement = connection
					.prepareStatement(NEXT_SYNC_EVENT_QUERY)) {
				for (int j = 0; j < step; j++) {
					try (ResultSet resultSet = statement.executeQuery()) {
						int count = 0;
						while (resultSet.next()) {
							count++;
						}
						assertEquals(1, count);
					}
				}
			}
		}
	}

}

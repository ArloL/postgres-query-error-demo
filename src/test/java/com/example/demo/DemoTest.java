package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

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
		try (Connection connection = DriverManager.getConnection(
				"jdbc:postgresql://localhost:55612/test",
				"test",
				"test"
		)) {
			recreateTable(connection);
			insertAndRemoveMessages(connection);
		}
	}

	private void recreateTable(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("""
					DROP TABLE IF EXISTS "queue"
					""");
			statement.executeUpdate("""
					CREATE TABLE "queue" (
						"id" BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
						"action" TEXT NOT NULL
					);
					""");
		}
	}

	private void insertAndRemoveMessages(Connection connection)
			throws SQLException {
		int total = 256_000;
		int step = 3;

		for (int i = 0; i < total; i += step) {
			try (Statement insertStatement = connection.createStatement()) {
				for (int j = 0; j < step; j++) {
					insertStatement.execute(
							"INSERT INTO queue (action) VALUES ('action')"
					);
				}
			}

			try (Statement statement = connection.createStatement()) {
				for (int j = 0; j < step; j++) {
					try (ResultSet resultSet = statement
							.executeQuery(NEXT_SYNC_EVENT_QUERY)) {
						int count = 0;
						while (resultSet.next()) {
							count++;
						}
						if (count > 1) {
							System.out.println(
									"Got " + count + " results after approx. "
											+ i + " entries"
							);
							return;
						}
					}
				}
			}
		}

		assertTrue(false, "Never got too many query results");
	}

}

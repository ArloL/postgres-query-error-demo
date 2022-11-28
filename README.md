# postgres-query-error-demo

A project to demonstrate an issue where a postgres query should
only return one row but it returns three.

# Quickstart

`./mvnw test`

# Explanation

Given this table:
```
CREATE TABLE "queue" (
	"id" BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
	"action" TEXT NOT NULL
);

Repeat these queries for a while:
```
INSERT INTO queue (action) VALUES ('action1');
INSERT INTO queue (action) VALUES ('action2');
INSERT INTO queue (action) VALUES ('action3');

# repeat three times
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
RETURNING *;
```

If you check the number of results from the delete query it will not return one result but three. That should not be the case due to the LIMIT statement in the query.

# postgres-query-error-demo

A project to demonstrate an issue where a postgres query should
only return one row but it returns three.

# Quickstart

The java version:

`./mvnw test`

or the shell version:

`./reproduction-with-psql.sh`

# What's the problem?

Given this table:

```
CREATE TABLE "queue" (
	"id" BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
	"action" TEXT NOT NULL
);
```

When you repeat these queries up to 100.000 times:

```
INSERT INTO queue (action) VALUES ('a');
INSERT INTO queue (action) VALUES ('a');
INSERT INTO queue (action) VALUES ('a');

-- repeat three times
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

Then at some point the delete statement will return three
elements instead of the expected one.

See
[/src/test/java/com/example/demo/DemoTest.java](https://github.com/ArloL/postgres-query-error-demo/blob/main/src/test/java/com/example/demo/DemoTest.java)
for code that reproduces this behaviour pretty reliable.

#!/bin/sh

set -o errexit
set -o nounset

if [ "--verbose" = "${1:-}" ]; then
  set -o xtrace
fi

PGCONNINFO=${PGCONNINFO:-postgresql://test:test@localhost:55612/test}

createTable() {
  psql \
    "${PGCONNINFO}" \
    --command 'DROP TABLE IF EXISTS "queue"' \
    --command 'CREATE TABLE "queue" (
              "id" BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
              "action" TEXT NOT NULL
            )'
}

insertThreeItems() {
  psql \
    "${PGCONNINFO}" \
    --quiet \
    --command "INSERT INTO queue (action) VALUES ('action')" \
    --command "INSERT INTO queue (action) VALUES ('action')" \
    --command "INSERT INTO queue (action) VALUES ('action')"
}

findAndDeleteNextItem() {
  query="
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
    RETURNING *"

  results=$(psql \
    "${PGCONNINFO}" \
    --quiet \
    --command "${query}" \
    --csv)

  resultCount=$(echo "$results" | wc -l )

  if [ "1" != "$((resultCount - 1))" ]; then
    echo "${results}"
    exit 1
  fi
}

createTable

i=0
while [ $i -le 256000 ]; do
  echo $i
  insertThreeItems
  findAndDeleteNextItem
  findAndDeleteNextItem
  findAndDeleteNextItem
  i=$(( i + 3 ))
done

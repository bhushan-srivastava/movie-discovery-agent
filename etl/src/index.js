import pg from "pg";
import { config } from "./config.js";
import { readCsv } from "./csvReader.js";
import { normalizeMovie } from "./normalize.js";
import { validateMovie } from "./validate.js";
import { assertMovieTable, upsertMovie } from "./movieRepository.js";

const { Client } = pg;

async function main() {
  const stats = {
    movieRows: 0,
    creditRows: 0,
    normalized: 0,
    upserted: 0,
    skipped: 0,
    failed: 0,
  };

  console.log("Reading movie CSV files...");

  const [movieRows, creditRows] = await Promise.all([
    readCsv(config.moviesCsv),
    readCsv(config.creditsCsv),
  ]);

  stats.movieRows = movieRows.length;
  stats.creditRows = creditRows.length;

  console.log(`Loaded ${movieRows.length} movie rows`);
  console.log(`Loaded ${creditRows.length} credit rows`);

  const creditsByMovieId = new Map(
    creditRows.map((row) => [String(row.movie_id ?? "").trim(), row]),
  );

  const client = new Client({
    connectionString: config.databaseUrl,
  });

  console.log("Connecting to Neon PostgreSQL...");
  await client.connect();
  console.log("Connected to Neon PostgreSQL");

  try {
    await assertMovieTable(client);
    console.log("Movie table found");

    await client.query("BEGIN");
    console.log("Import started...");

    for (const [index, row] of movieRows.entries()) {
      const sourceId = String(row.id ?? "").trim();
      const creditRow = creditsByMovieId.get(sourceId);

      const movie = normalizeMovie(row, creditRow);
      const validation = validateMovie(movie);

      if (!validation.valid) {
        stats.skipped += 1;

        console.warn(
          `Skipping ${sourceId || "<unknown>"}: ${validation.errors.join(", ")}`,
        );

        continue;
      }

      stats.normalized += 1;

      try {
        await upsertMovie(client, movie);
        stats.upserted += 1;
      } catch (error) {
        stats.failed += 1;

        throw new Error(
          `Failed to upsert ${movie.externalSourceId}: ${error.message}`,
        );
      }

      if ((index + 1) % 100 === 0) {
        console.log(`Processed ${index + 1}/${movieRows.length} movie rows`);
      }
    }

    await client.query("COMMIT");

    console.log("\nETL completed successfully");
    console.table(stats);
  } catch (error) {
    console.error(`ETL failed: ${error.message}`);

    try {
      await client.query("ROLLBACK");
      console.log("Transaction rolled back");
    } catch (rollbackError) {
      console.error(`Rollback failed: ${rollbackError.message}`);
    }

    throw error;
  } finally {
    await client.end();
    console.log("Database connection closed");
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});

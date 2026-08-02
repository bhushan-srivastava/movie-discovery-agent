import 'dotenv/config';

function required(name) {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`Missing required environment variable: ${name}`);
  return value;
}

function positiveInteger(value, fallback) {
  const parsed = Number.parseInt(value ?? '', 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

export const config = Object.freeze({
  databaseUrl: required('DATABASE_URL'),
  moviesCsv: process.env.MOVIES_CSV || './data/tmdb_5000_movies.csv',
  creditsCsv: process.env.CREDITS_CSV || './data/tmdb_5000_credits.csv',
  batchSize: positiveInteger(process.env.BATCH_SIZE, 100)
});

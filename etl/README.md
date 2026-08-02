# Movie ETL

Imports the Kaggle TMDB 5000 Movie Dataset into Neon PostgreSQL.

## Prerequisites

- Node.js 22+
- A Neon PostgreSQL database
- The schema in `sql/schema.sql` applied manually in Neon
- Kaggle dataset files in `data/`

## Setup

1. Copy `.env.example` to `.env`.
2. Set `DATABASE_URL` in `.env`.
3. Download the Kaggle TMDB 5000 dataset and add:
   - `data/tmdb_5000_movies.csv`
   - `data/tmdb_5000_credits.csv`
4. Install dependencies:

   `npm install`

5. Run tests:

   `npm test`

6. Run import:

   `npm run import`

The import is safe to rerun because rows are upserted using `external_source_id`.

## Security

- Never commit `.env`.
- Never log `DATABASE_URL`.
- Rotate database credentials if exposed.

## Attribution

Movie metadata originates from The Movie Database (TMDB). Review the Kaggle dataset licence and TMDB attribution requirements before redistributing the CSV files or publishing derived data.

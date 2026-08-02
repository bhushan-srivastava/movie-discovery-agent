const UPSERT_SQL = `
INSERT INTO movie (
  external_source_id, title, original_title, release_year, genres, director,
  synopsis, runtime_minutes, original_language, vote_average, vote_count,
  popularity, poster_url
) VALUES (
  $1, $2, $3, $4, $5::text[], $6, $7, $8, $9, $10, $11, $12, $13
)
ON CONFLICT (external_source_id) DO UPDATE SET
  title = EXCLUDED.title,
  original_title = EXCLUDED.original_title,
  release_year = EXCLUDED.release_year,
  genres = EXCLUDED.genres,
  director = EXCLUDED.director,
  synopsis = EXCLUDED.synopsis,
  runtime_minutes = EXCLUDED.runtime_minutes,
  original_language = EXCLUDED.original_language,
  vote_average = EXCLUDED.vote_average,
  vote_count = EXCLUDED.vote_count,
  popularity = EXCLUDED.popularity,
  poster_url = EXCLUDED.poster_url,
  updated_at = CURRENT_TIMESTAMP
RETURNING id, external_source_id;
`;

export async function assertMovieTable(client) {
  const result = await client.query(`
    SELECT EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = 'public' AND table_name = 'movie'
    ) AS exists
  `);
  if (!result.rows[0].exists) {
    throw new Error('movie table does not exist. Run sql/schema.sql in Neon first.');
  }
}

export async function upsertMovie(client, movie) {
  const values = [
    movie.externalSourceId, movie.title, movie.originalTitle, movie.releaseYear,
    movie.genres, movie.director, movie.synopsis, movie.runtimeMinutes,
    movie.originalLanguage, movie.voteAverage, movie.voteCount, movie.popularity,
    movie.posterUrl
  ];
  return client.query(UPSERT_SQL, values);
}

export { UPSERT_SQL };

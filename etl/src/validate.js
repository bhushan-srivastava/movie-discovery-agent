export function validateMovie(movie) {
  const errors = [];
  if (!movie.externalSourceId) errors.push('missing movie id');
  if (!movie.title) errors.push('missing title');
  if (!Array.isArray(movie.genres) || movie.genres.length === 0) errors.push('missing genres');
  if (movie.runtimeMinutes !== null && movie.runtimeMinutes <= 0) errors.push('invalid runtime');
  if (movie.voteAverage !== null && (movie.voteAverage < 0 || movie.voteAverage > 10)) errors.push('invalid vote average');
  return { valid: errors.length === 0, errors };
}

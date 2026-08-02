function nullableText(value) {
  const text = String(value ?? '').trim();
  return text || null;
}

function nullableInteger(value) {
  if (value === null || value === undefined || String(value).trim() === '') return null;
  const number = Number(value);
  return Number.isFinite(number) ? Math.round(number) : null;
}

function nullableDecimal(value) {
  if (value === null || value === undefined || String(value).trim() === '') return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

export function parseJsonArray(value) {
  if (!value || !String(value).trim()) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function extractGenres(value) {
  return [...new Set(
    parseJsonArray(value)
      .map(item => nullableText(item?.name)?.toLowerCase())
      .filter(Boolean)
  )];
}

export function extractDirector(crewJson) {
  const director = parseJsonArray(crewJson).find(item => item?.job === 'Director');
  return nullableText(director?.name);
}

export function extractReleaseYear(releaseDate) {
  const text = nullableText(releaseDate);
  if (!text) return null;
  const year = Number.parseInt(text.slice(0, 4), 10);
  return Number.isInteger(year) && year >= 1800 && year <= 2200 ? year : null;
}

export function normalizeMovie(movieRow, creditRow) {
  const sourceId = nullableText(movieRow.id);
  const runtime = nullableInteger(movieRow.runtime);
  const voteAverage = nullableDecimal(movieRow.vote_average);
  const voteCount = nullableInteger(movieRow.vote_count);
  const popularity = nullableDecimal(movieRow.popularity);

  return {
    externalSourceId: sourceId ? `tmdb-${sourceId}` : null,
    title: nullableText(movieRow.title),
    originalTitle: nullableText(movieRow.original_title),
    releaseYear: extractReleaseYear(movieRow.release_date),
    genres: extractGenres(movieRow.genres),
    director: extractDirector(creditRow?.crew),
    synopsis: nullableText(movieRow.overview),
    runtimeMinutes: runtime && runtime > 0 ? runtime : null,
    originalLanguage: nullableText(movieRow.original_language),
    voteAverage: voteAverage !== null && voteAverage >= 0 && voteAverage <= 10 ? voteAverage : null,
    voteCount: voteCount !== null && voteCount >= 0 ? voteCount : null,
    popularity: popularity !== null && popularity >= 0 ? popularity : null,
    posterUrl: null
  };
}

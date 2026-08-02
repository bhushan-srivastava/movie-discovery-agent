import test from 'node:test';
import assert from 'node:assert/strict';
import { extractDirector, extractGenres, extractReleaseYear, normalizeMovie } from '../src/normalize.js';

test('extractGenres parses and normalizes TMDB genre names', () => {
  const input = '[{"id":28,"name":"Action"},{"id":878,"name":"Science Fiction"}]';
  assert.deepEqual(extractGenres(input), ['action', 'science fiction']);
});

test('extractDirector finds crew member with Director job', () => {
  const input = '[{"job":"Writer","name":"A"},{"job":"Director","name":"B"}]';
  assert.equal(extractDirector(input), 'B');
});

test('extractReleaseYear returns year or null', () => {
  assert.equal(extractReleaseYear('2010-07-16'), 2010);
  assert.equal(extractReleaseYear(''), null);
});

test('normalizeMovie creates stable external source id', () => {
  const movie = normalizeMovie({
    id: '27205', title: 'Inception', original_title: 'Inception',
    release_date: '2010-07-16', genres: '[{"name":"Science Fiction"}]',
    overview: 'Dreams.', runtime: '148', original_language: 'en',
    vote_average: '8.1', vote_count: '1000', popularity: '120.5'
  }, { crew: '[{"job":"Director","name":"Christopher Nolan"}]' });
  assert.equal(movie.externalSourceId, 'tmdb-27205');
  assert.equal(movie.runtimeMinutes, 148);
  assert.equal(movie.director, 'Christopher Nolan');
});

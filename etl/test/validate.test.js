import test from 'node:test';
import assert from 'node:assert/strict';
import { validateMovie } from '../src/validate.js';

const valid = {
  externalSourceId: 'tmdb-1', title: 'Example', genres: ['drama'],
  runtimeMinutes: 100, voteAverage: 7.5
};

test('accepts valid movie', () => {
  assert.equal(validateMovie(valid).valid, true);
});

test('rejects missing required values', () => {
  const result = validateMovie({ ...valid, title: null, genres: [] });
  assert.equal(result.valid, false);
  assert.deepEqual(result.errors, ['missing title', 'missing genres']);
});

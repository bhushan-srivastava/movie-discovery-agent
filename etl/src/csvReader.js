import fs from 'node:fs';
import { parse } from 'csv-parse';

export async function readCsv(filePath) {
  return new Promise((resolve, reject) => {
    const rows = [];
    fs.createReadStream(filePath)
      .on('error', reject)
      .pipe(parse({ columns: true, bom: true, skip_empty_lines: true, relax_quotes: true }))
      .on('data', row => rows.push(row))
      .on('error', reject)
      .on('end', () => resolve(rows));
  });
}

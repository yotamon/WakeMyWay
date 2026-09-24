import { readdir } from 'node:fs/promises';
import { join, relative } from 'node:path';

const MAX_HOBBY_FUNCTIONS = 12;
const apiRoot = new URL('../api/', import.meta.url);

async function collectFunctionFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const path = join(directory.pathname, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await collectFunctionFiles(new URL(`file://${path}/`))));
    } else if (entry.isFile() && /\.(?:ts|js|mjs|cjs)$/.test(entry.name)) {
      files.push(path);
    }
  }

  return files;
}

const files = await collectFunctionFiles(apiRoot);
const normalized = files
  .map(path => relative(apiRoot.pathname, path).replaceAll('\\', '/'))
  .sort();

if (normalized.length > MAX_HOBBY_FUNCTIONS) {
  console.error(
    `WakeMyWay cloud defines ${normalized.length} Vercel function files; Hobby budget is ${MAX_HOBBY_FUNCTIONS}.\n` +
      normalized.map(path => ` - ${path}`).join('\n'),
  );
  process.exit(1);
}

console.log(
  `Vercel function budget OK: ${normalized.length}/${MAX_HOBBY_FUNCTIONS}\n` +
    normalized.map(path => ` - ${path}`).join('\n'),
);

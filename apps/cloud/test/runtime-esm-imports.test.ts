import { readdir, readFile } from 'node:fs/promises';
import { join, relative } from 'node:path';
import { describe, expect, it } from 'vitest';

const RUNTIME_ROOTS = ['api', 'src'] as const;
const NODE_ESM_EXTENSIONS = /\.(?:js|mjs|cjs|json|node)(?:[?#].*)?$/;
const RELATIVE_IMPORT_PATTERNS = [
  /\bfrom\s*['"](\.{1,2}\/[^'"]+)['"]/g,
  /\bimport\s*['"](\.{1,2}\/[^'"]+)['"]/g,
  /\bimport\s*\(\s*['"](\.{1,2}\/[^'"]+)['"]\s*\)/g,
] as const;

async function collectTypeScriptFiles(directory: string): Promise<string[]> {
  const entries = await readdir(directory, { withFileTypes: true });
  const files: string[] = [];

  for (const entry of entries) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await collectTypeScriptFiles(path)));
    } else if (entry.isFile() && entry.name.endsWith('.ts')) {
      files.push(path);
    }
  }

  return files;
}

function relativeModuleSpecifiers(source: string): string[] {
  const specifiers: string[] = [];

  for (const pattern of RELATIVE_IMPORT_PATTERNS) {
    pattern.lastIndex = 0;
    for (const match of source.matchAll(pattern)) {
      const specifier = match[1];
      if (specifier) specifiers.push(specifier);
    }
  }

  return specifiers;
}

describe('Vercel Node ESM runtime imports', () => {
  it('uses explicit runtime extensions for every relative import in api/ and src/', async () => {
    const invalid: string[] = [];

    for (const root of RUNTIME_ROOTS) {
      for (const filePath of await collectTypeScriptFiles(join(process.cwd(), root))) {
        const source = await readFile(filePath, 'utf8');
        for (const specifier of relativeModuleSpecifiers(source)) {
          if (!NODE_ESM_EXTENSIONS.test(specifier)) {
            invalid.push(`${relative(process.cwd(), filePath)} -> ${specifier}`);
          }
        }
      }
    }

    expect(invalid).toEqual([]);
  });
});

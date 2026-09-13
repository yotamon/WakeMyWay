import { readdir, readFile } from 'node:fs/promises';
import { join, relative } from 'node:path';
import * as ts from 'typescript';
import { describe, expect, it } from 'vitest';

const RUNTIME_ROOTS = ['api', 'src'] as const;
const NODE_ESM_EXTENSIONS = /\.(?:js|mjs|cjs|json|node)$/;

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

function relativeModuleSpecifiers(source: string, fileName: string): string[] {
  const file = ts.createSourceFile(fileName, source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS);
  const specifiers: string[] = [];

  function visit(node: ts.Node): void {
    if (
      (ts.isImportDeclaration(node) || ts.isExportDeclaration(node)) &&
      node.moduleSpecifier &&
      ts.isStringLiteral(node.moduleSpecifier)
    ) {
      specifiers.push(node.moduleSpecifier.text);
    }

    if (
      ts.isCallExpression(node) &&
      node.expression.kind === ts.SyntaxKind.ImportKeyword &&
      node.arguments.length === 1 &&
      node.arguments[0] &&
      ts.isStringLiteral(node.arguments[0])
    ) {
      specifiers.push(node.arguments[0].text);
    }

    ts.forEachChild(node, visit);
  }

  visit(file);
  return specifiers.filter(specifier => specifier.startsWith('./') || specifier.startsWith('../'));
}

describe('Vercel Node ESM runtime imports', () => {
  it('uses explicit runtime extensions for every relative import in api/ and src/', async () => {
    const invalid: string[] = [];

    for (const root of RUNTIME_ROOTS) {
      for (const filePath of await collectTypeScriptFiles(join(process.cwd(), root))) {
        const source = await readFile(filePath, 'utf8');
        for (const specifier of relativeModuleSpecifiers(source, filePath)) {
          if (!NODE_ESM_EXTENSIONS.test(specifier)) {
            invalid.push(`${relative(process.cwd(), filePath)} -> ${specifier}`);
          }
        }
      }
    }

    expect(invalid).toEqual([]);
  });
});

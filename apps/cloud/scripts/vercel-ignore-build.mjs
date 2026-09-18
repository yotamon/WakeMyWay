import { execFileSync } from "node:child_process";
import { pathToFileURL } from "node:url";

const CLOUD_PREFIX = "apps/cloud/";

function normalizePath(path) {
  return path.replaceAll("\\", "/");
}

export function isCloudRuntimePath(path) {
  if (!path) return false;
  return normalizePath(path).startsWith(CLOUD_PREFIX);
}

export function shouldIgnoreDeployment(paths) {
  return paths.length > 0 && paths.every((path) => !isCloudRuntimePath(path));
}

export function resolveDiffBase(previousSha) {
  if (previousSha && /^[a-f0-9]{7,40}$/i.test(previousSha)) return previousSha;
  return "HEAD^";
}

export function changedFiles(baseRef, head = "HEAD") {
  if (!baseRef) throw new Error("Git diff base is unavailable");

  const repositoryRoot = execFileSync("git", ["rev-parse", "--show-toplevel"], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  }).trim();

  if (!repositoryRoot) throw new Error("Git repository root could not be resolved");

  return execFileSync("git", ["-C", repositoryRoot, "diff", "--name-only", baseRef, head], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  })
    .split(/\r?\n/)
    .map((value) => value.trim())
    .filter(Boolean);
}

function run() {
  try {
    const previousSha = process.env.VERCEL_GIT_PREVIOUS_SHA?.trim() || "";
    const head = process.env.VERCEL_GIT_COMMIT_SHA?.trim() || "HEAD";
    const baseRef = resolveDiffBase(previousSha);
    const paths = changedFiles(baseRef, head);

    if (shouldIgnoreDeployment(paths)) {
      console.log("Skipping Vercel build: no apps/cloud runtime files changed.");
      process.exit(0);
    }

    const cloudPaths = paths.filter(isCloudRuntimePath);
    console.log(
      cloudPaths.length > 0
        ? `Running Vercel build: cloud paths changed: ${cloudPaths.join(", ")}`
        : "Running Vercel build conservatively: change scope could not be proven deployment-neutral.",
    );
    process.exit(1);
  } catch (error) {
    // Exit 0 means "skip" to Vercel. Any uncertainty must fail open to a real build.
    console.warn(
      `Running Vercel build conservatively: ${error instanceof Error ? error.message : String(error)}`,
    );
    process.exit(1);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  run();
}

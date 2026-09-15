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

export function changedFiles(previousSha, head = "HEAD") {
  if (!previousSha || !/^[a-f0-9]{7,40}$/i.test(previousSha)) {
    throw new Error("VERCEL_GIT_PREVIOUS_SHA is unavailable or invalid");
  }

  return execFileSync("git", ["diff", "--name-only", previousSha, head], {
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
    const paths = changedFiles(previousSha, head);

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

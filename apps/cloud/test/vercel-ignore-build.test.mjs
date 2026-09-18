import { describe, expect, it } from "vitest";
import { isCloudRuntimePath, resolveDiffBase, shouldIgnoreDeployment } from "../scripts/vercel-ignore-build.mjs";

describe("Vercel build scope", () => {
  it("uses the commit parent when Vercel omits previous SHA for a preview", () => {
    expect(resolveDiffBase("", "preview")).toBe("HEAD^");
    expect(resolveDiffBase("a".repeat(40), "preview")).toBe("a".repeat(40));
    expect(() => resolveDiffBase("", "production")).toThrow();
  });

  it("treats cloud application changes as deployable", () => {
    expect(isCloudRuntimePath("apps/cloud/api/wake.ts")).toBe(true);
    expect(isCloudRuntimePath("apps/cloud/vercel.json")).toBe(true);
    expect(shouldIgnoreDeployment(["apps/cloud/src/config.ts"])).toBe(false);
  });

  it("skips Android, documentation, and repository-only changes", () => {
    expect(isCloudRuntimePath("apps/android/app/src/main/MainActivity.kt")).toBe(false);
    expect(
      shouldIgnoreDeployment([
        "apps/android/app/src/main/MainActivity.kt",
        "docs/implementation/wake-sounds.md",
        "README.md",
      ]),
    ).toBe(true);
  });

  it("fails open when the diff is empty", () => {
    expect(shouldIgnoreDeployment([])).toBe(false);
  });

  it("does not skip mixed commits that include cloud changes", () => {
    expect(
      shouldIgnoreDeployment([
        "apps/android/app/src/main/MainActivity.kt",
        "apps/cloud/api/session.ts",
      ]),
    ).toBe(false);
  });
});

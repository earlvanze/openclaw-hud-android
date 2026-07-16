#!/usr/bin/env node

import assert from "node:assert/strict";
import { resolveNextVersion, resolveNextVersionCode } from "./build-release-aab.mjs";

const sameDay = resolveNextVersion(
  'versionCode = 2026071607\nversionName = "2026.7.16.7"',
  new Date(2026, 6, 16, 12, 0, 0),
);
assert.deepEqual(sameDay, {
  versionName: "2026.7.16.8",
  versionCode: 2026071608,
});

const nextDay = resolveNextVersion(
  'versionCode = 2026071699\nversionName = "2026.7.16.99"',
  new Date(2026, 6, 17, 12, 0, 0),
);
assert.deepEqual(nextDay, {
  versionName: "2026.7.17.0",
  versionCode: 2026071700,
});

assert.throws(
  () => resolveNextVersionCode(2026071699, "20260716"),
  /next suffix 100 is invalid/,
);

console.log("Android release version tests passed.");

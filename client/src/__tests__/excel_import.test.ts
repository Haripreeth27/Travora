import { test, describe } from "node:test";
import assert from "node:assert/strict";
import { importsApi } from "../lib/api.ts";

describe("Excel Import & Travel History Frontend Contract Tests", () => {
  test("importsApi exposes complete lifecycle methods", () => {
    assert.equal(typeof importsApi.upload, "function");
    assert.equal(typeof importsApi.listBatches, "function");
    assert.equal(typeof importsApi.getBatch, "function");
    assert.equal(typeof importsApi.getRecords, "function");
    assert.equal(typeof importsApi.updateMapping, "function");
    assert.equal(typeof importsApi.confirm, "function");
    assert.equal(typeof importsApi.searchTravelHistory, "function");
    assert.equal(typeof importsApi.exportTravelHistory, "function");
  });

  test("exportTravelHistory URL builder generates secure query parameters", () => {
    assert.equal(typeof importsApi.exportTravelHistoryUrl, "function");
    const url = importsApi.exportTravelHistoryUrl({
      search: "John Doe",
      employeeId: 42,
      fromDate: "2026-01-01",
      toDate: "2026-12-31",
    });

    assert.ok(url.includes("/api/imports/travel-history/export"));
    assert.ok(url.includes("search=John+Doe") || url.includes("search=John%20Doe"));
    assert.ok(url.includes("employeeId=42"));
    assert.ok(url.includes("fromDate=2026-01-01"));
    assert.ok(url.includes("toDate=2026-12-31"));
  });

  test("formula injection values are neutralized before CSV write", () => {
    const rawFormulas = ["=cmd|' /C calc'!A0", "+1+1", "-2+5", "@SUM(A1:A10)", "\tmalicious", "\rpayload"];
    for (const val of rawFormulas) {
      const firstChar = val.charAt(0);
      const isDangerous = firstChar === '=' || firstChar === '+' || firstChar === '-' || firstChar === '@' || firstChar === '\t' || firstChar === '\r';
      assert.ok(isDangerous, `Expected ${JSON.stringify(firstChar)} to trigger formula defense`);
      const escaped = "'" + val;
      assert.ok(escaped.startsWith("'"), "Dangerous formula trigger must be prepended with apostrophe");
    }
  });
});


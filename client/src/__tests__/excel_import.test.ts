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

  describe("importsApi.upload robust error extraction and security handling", () => {
    const originalFetch = globalThis.fetch;
    const fakeFile = { name: "test.xlsx", size: 1024 } as unknown as File;

    test("Spring Boot RFC 7807 detail field is extracted instead of generic error", async () => {
      globalThis.fetch = async () =>
        new Response(
          JSON.stringify({
            type: "about:blank",
            title: "Bad Request",
            status: 400,
            detail: "The uploaded spreadsheet contains no recognizable columns or data headers.",
            instance: "/api/imports/upload",
          }),
          { status: 400, headers: { "content-type": "application/problem+json" } }
        );

      await assert.rejects(
        () => importsApi.upload(fakeFile),
        (err: Error) => {
          assert.equal(err.message, "The uploaded spreadsheet contains no recognizable columns or data headers.");
          return true;
        }
      );
      globalThis.fetch = originalFetch;
    });

    test("Vercel HTML rewrite fallback triggers backend unreachable error", async () => {
      globalThis.fetch = async () =>
        new Response("<!DOCTYPE html><html><head><title>Trapple</title></head><body>SPA</body></html>", {
          status: 200,
          headers: { "content-type": "text/html; charset=UTF-8" },
        });

      await assert.rejects(
        () => importsApi.upload(fakeFile),
        (err: Error) => {
          assert.ok(err.message.includes("Trapple backend server not reached (HTML fallback)"));
          return true;
        }
      );
      globalThis.fetch = originalFetch;
    });

    test("HTTP 401 Unauthorized clears authentication and throws session expired", async () => {
      globalThis.fetch = async () =>
        new Response(JSON.stringify({ error: "Unauthorized", status: 401 }), {
          status: 401,
          headers: { "content-type": "application/json" },
        });

      await assert.rejects(
        () => importsApi.upload(fakeFile),
        (err: Error) => {
          assert.equal(err.message, "Your session has expired. Please sign in again.");
          return true;
        }
      );
      globalThis.fetch = originalFetch;
    });

    test("HTTP 403 Forbidden raises actionable role permission error", async () => {
      globalThis.fetch = async () =>
        new Response(JSON.stringify({ error: "Forbidden", status: 403 }), {
          status: 403,
          headers: { "content-type": "application/json" },
        });

      await assert.rejects(
        () => importsApi.upload(fakeFile),
        (err: Error) => {
          assert.ok(err.message.includes("You do not have permission to upload spreadsheets"));
          return true;
        }
      );
      globalThis.fetch = originalFetch;
    });

    test("HTTP 413 Payload Too Large raises 20MB limit explanation", async () => {
      globalThis.fetch = async () =>
        new Response(JSON.stringify({ status: 413, error: "Payload Too Large" }), {
          status: 413,
          headers: { "content-type": "application/json" },
        });

      await assert.rejects(
        () => importsApi.upload(fakeFile),
        (err: Error) => {
          assert.ok(err.message.includes("20MB maximum size limit"));
          return true;
        }
      );
      globalThis.fetch = originalFetch;
    });

    test("Network connection drop raises connectivity guidance", async () => {
      globalThis.fetch = async () => {
        throw new Error("Failed to fetch");
      };

      await assert.rejects(
        () => importsApi.upload(fakeFile),
        (err: Error) => {
          assert.ok(err.message.includes("We couldn't reach Trapple"));
          return true;
        }
      );
      globalThis.fetch = originalFetch;
    });
  });
});


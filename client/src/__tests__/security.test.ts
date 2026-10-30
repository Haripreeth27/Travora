import test from "node:test";
import assert from "node:assert";
import fs from "node:fs";
import path from "node:path";
import { sanitizeCsvCell, exportToCsv } from "../utils/exportCsv.ts";

test("Security Test Suite — Frontend Hardening & Invariant Checks", async (t) => {
  const apiTsPath = path.resolve(process.cwd(), "client/src/lib/api.ts");
  const apiTsContent = fs.readFileSync(apiTsPath, "utf-8");

  await t.test("1. Public synchronization mechanism (ntfy.sh) is completely purged", () => {
    // Assert ntfy.sh is not used as an active endpoint or URL
    const activeNtfyUsage = apiTsContent
      .split("\n")
      .filter((line) => !line.trim().startsWith("*") && !line.trim().startsWith("//"))
      .some((line) => line.includes("ntfy.sh"));
    assert.strictEqual(activeNtfyUsage, false, "ntfy.sh must not appear in executable code");

    assert.strictEqual(apiTsContent.includes("travora_live_corp_sync_v6"), false, "Public sync topic must be absent");
    assert.strictEqual(apiTsContent.includes("broadcastCloudSync"), false, "broadcastCloudSync must be purged");
    assert.strictEqual(apiTsContent.includes("initCloudSync"), false, "initCloudSync must be purged");
    assert.strictEqual(apiTsContent.includes("handleIncomingCloudSync"), false, "handleIncomingCloudSync must be purged");
  });

  await t.test("2. Hardcoded passwords and synthetic JWT tokens are purged from code", () => {
    // Ensure no synthetic JWT tokens are issued in api.ts
    assert.strictEqual(apiTsContent.includes("jwt-admin-auth-token"), false, "Synthetic admin JWT must not exist");
    assert.strictEqual(apiTsContent.includes("jwt-traveldesk-auth-token"), false, "Synthetic travel desk JWT must not exist");
    assert.strictEqual(apiTsContent.includes("jwt-approver-auth-token"), false, "Synthetic approver JWT must not exist");
    assert.strictEqual(apiTsContent.includes("jwt-employee-auth-token"), false, "Synthetic employee JWT must not exist");

    // Ensure hardcoded demo passwords are not checked in login
    assert.strictEqual(apiTsContent.includes('rawPass === "mahathi"'), false, "Hardcoded travel desk password must not exist");
    assert.strictEqual(apiTsContent.includes('rawPass === "mahathi1"'), false, "Hardcoded approver password must not exist");
    assert.strictEqual(apiTsContent.includes('rawPass === "admin123"'), false, "Hardcoded admin password must not exist");
    assert.strictEqual(apiTsContent.includes("passLower === matchedEmployee.employeeNumber"), false, "Password=employeeNumber bypass must not exist");
  });

  await t.test("3. CSV Formula Injection (CWE-1236) and RFC 4180 Compliance", async (csvSub) => {
    // 3a. Exact requested formula payloads:
    await csvSub.test("Explicit formula injection trigger cases", () => {
      // 1. =SUM(A1:A10)
      assert.strictEqual(sanitizeCsvCell("=SUM(A1:A10)"), "\"'=SUM(A1:A10)\"");
      // 2. +123
      assert.strictEqual(sanitizeCsvCell("+123"), "\"'+123\"");
      // 3. -123 (numeric or string)
      assert.strictEqual(sanitizeCsvCell("-123"), "\"'-123\"");
      assert.strictEqual(sanitizeCsvCell(-123), "\"'-123\"");
      // 4. @example
      assert.strictEqual(sanitizeCsvCell("@example"), "\"'@example\"");
    });

    // 3b. Leading whitespace followed by a formula
    await csvSub.test("Leading spaces followed by formula triggers", () => {
      assert.strictEqual(sanitizeCsvCell("  =SUM(A1:A10)"), "\"'  =SUM(A1:A10)\"");
      assert.strictEqual(sanitizeCsvCell("   +123"), "\"'   +123\"");
      assert.strictEqual(sanitizeCsvCell("    -123"), "\"'    -123\"");
      assert.strictEqual(sanitizeCsvCell("  @example"), "\"'  @example\"");
    });

    // 3c. Leading tabs and carriage returns
    await csvSub.test("Leading tabs and carriage returns followed by formula triggers", () => {
      assert.strictEqual(sanitizeCsvCell("\t=SUM(A1:A10)"), "\"'\t=SUM(A1:A10)\"");
      assert.strictEqual(sanitizeCsvCell("\r+123"), "\"'\r+123\"");
      assert.strictEqual(sanitizeCsvCell("\t@example"), "\"'\t@example\"");
      assert.strictEqual(sanitizeCsvCell("\r\n=1+1"), "\"'\r\n=1+1\"");
    });

    // 3d. Commas, double quotes, and embedded newlines
    await csvSub.test("Commas, escaped double quotes, and embedded newlines", () => {
      // Commas and double quotes
      assert.strictEqual(sanitizeCsvCell('Hello, "World"'), '"Hello, ""World"""');
      // Embedded newline
      assert.strictEqual(sanitizeCsvCell("Line 1\nLine 2"), '"Line 1\nLine 2"');
      // Embedded CRLF
      assert.strictEqual(sanitizeCsvCell("Line 1\r\nLine 2"), '"Line 1\r\nLine 2"');
      // Complex mix: quotes, commas, newlines
      assert.strictEqual(
        sanitizeCsvCell('Route: "Bengaluru, KA" -> "Mumbai, MH"\nFlight AI-804'),
        '"Route: ""Bengaluru, KA"" -> ""Mumbai, MH""\nFlight AI-804"'
      );
    });

    // 3e. Ordinary text and positive numeric values
    await csvSub.test("Ordinary text and positive numeric values remain un-prefixed", () => {
      assert.strictEqual(sanitizeCsvCell("Bangalore"), '"Bangalore"');
      assert.strictEqual(sanitizeCsvCell("Project Alpha"), '"Project Alpha"');
      assert.strictEqual(sanitizeCsvCell("john.doe@company.com"), '"john.doe@company.com"');
      assert.strictEqual(sanitizeCsvCell(100), '"100"');
      assert.strictEqual(sanitizeCsvCell(456.78), '"456.78"');
      assert.strictEqual(sanitizeCsvCell(true), '"true"');
      assert.strictEqual(sanitizeCsvCell(false), '"false"');
      assert.strictEqual(sanitizeCsvCell(null), '""');
      assert.strictEqual(sanitizeCsvCell(undefined), '""');
      assert.strictEqual(sanitizeCsvCell(""), '""');
    });

    // 3f. End-to-end exportToCsv RFC 4180 output verification
    await csvSub.test("exportToCsv generates valid RFC 4180 multi-row CSV document", () => {
      let generatedCsv = "";
      const originalBlob = globalThis.Blob;
      const originalURL = globalThis.URL;
      const originalDoc = (globalThis as any).document;

      try {
        (globalThis as any).Blob = class MockBlob {
          constructor(parts: any[]) {
            generatedCsv = parts.join("");
          }
        };
        (globalThis as any).URL = {
          createObjectURL: () => "blob:mock",
          revokeObjectURL: () => {},
        };
        (globalThis as any).document = {
          createElement: () => ({
            setAttribute: () => {},
            click: () => {},
          }),
          body: {
            appendChild: () => {},
            removeChild: () => {},
          },
        };

        exportToCsv("test_export", ["ID", "Destination", "Notes", "Cost", "Discount"], [
          [1, "Hyderabad", "=cmd|' /C calc'!A0", 5000, -500],
          [2, "Mumbai", 'Flight delayed, "Priority" seat\nGate B12', 7200.5, 0],
          [3, "Delhi", "  @corporate_group", 3400, "+150"],
        ]);

        const lines = generatedCsv.split("\r\n");
        // Header line
        assert.strictEqual(lines[0], '"ID","Destination","Notes","Cost","Discount"');
        // Row 1: formula and negative cost are neutralized
        assert.strictEqual(lines[1], '"1","Hyderabad","\'=cmd|\' /C calc\'!A0","5000","\'-500"');
        // Row 2: multiline text with quotes
        assert.ok(generatedCsv.includes('"Flight delayed, ""Priority"" seat\nGate B12"'));
        // Row 3: leading spaces with @
        assert.ok(generatedCsv.includes('"\'  @corporate_group"'));
        assert.ok(generatedCsv.includes('"\'\+150"'));
      } finally {
        globalThis.Blob = originalBlob;
        globalThis.URL = originalURL;
        (globalThis as any).document = originalDoc;
      }
    });
  });

  await t.test("4. Offline and silent localStorage fallback methods are purged", () => {
    assert.strictEqual(apiTsContent.includes("_getAllSystemRequests"), false, "_getAllSystemRequests must not exist");
    assert.strictEqual(apiTsContent.includes("_saveAllSystemRequests"), false, "_saveAllSystemRequests must not exist");
    assert.strictEqual(apiTsContent.includes("_getAllSystemBookings"), false, "_getAllSystemBookings must not exist");
    assert.strictEqual(apiTsContent.includes("_saveAllSystemBookings"), false, "_saveAllSystemBookings must not exist");
    assert.strictEqual(apiTsContent.includes("_getAllSystemNotifications"), false, "_getAllSystemNotifications must not exist");
    assert.strictEqual(apiTsContent.includes("_saveAllSystemNotifications"), false, "_saveAllSystemNotifications must not exist");
    assert.strictEqual(apiTsContent.includes("_getAllSystemBulkBookings"), false, "_getAllSystemBulkBookings must not exist");
    assert.strictEqual(apiTsContent.includes("_saveAllSystemBulkBookings"), false, "_saveAllSystemBulkBookings must not exist");
  });

  await t.test("5. apiRequest timeout is reasonable (not aggressive 1200ms/1500ms)", () => {
    // Assert timeout is increased to at least 10000ms
    assert.ok(
      apiTsContent.includes("timeoutMs = 15000") || apiTsContent.includes("timeoutMs = 10000"),
      "Default apiRequest timeout must be at least 10-15 seconds"
    );
    assert.strictEqual(
      apiTsContent.includes("timeoutMs = 1500,"),
      false,
      "Aggressive 1500ms timeout must be removed"
    );
  });
});


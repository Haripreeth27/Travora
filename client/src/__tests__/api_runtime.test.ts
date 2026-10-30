import test from "node:test";
import assert from "node:assert";

// Polyfill window, localStorage, and CustomEvent for runtime tests
class MockLocalStorage {
  private store = new Map<string, string>();
  getItem(key: string): string | null {
    return this.store.get(key) ?? null;
  }
  setItem(key: string, value: string): void {
    this.store.set(key, String(value));
  }
  removeItem(key: string): void {
    this.store.delete(key);
  }
  clear(): void {
    this.store.clear();
  }
  get length(): number {
    return this.store.size;
  }
  key(index: number): string | null {
    return Array.from(this.store.keys())[index] ?? null;
  }
}

const mockStorage = new MockLocalStorage();
(globalThis as any).window = {
  localStorage: mockStorage,
  location: { hostname: "localhost" },
  dispatchEvent: (event: any) => true,
  addEventListener: () => {},
  removeEventListener: () => {},
};
(globalThis as any).CustomEvent = class CustomEvent {
  type: string;
  detail: any;
  constructor(type: string, params: any = {}) {
    this.type = type;
    this.detail = params.detail;
  }
};

import {
  login,
  travelRequestsApi,
  bookingsApi,
  bulkBookingsApi,
  notificationsApi,
  clearAuth,
  persistAuth,
  getToken,
  getStoredUser,
} from "../lib/api.ts";

test("API Runtime Security & Authoritative Behavior Tests", async (t) => {
  const originalFetch = globalThis.fetch;

  t.afterEach(() => {
    mockStorage.clear();
    globalThis.fetch = originalFetch;
  });

  await t.test("Backend network failure in login() throws actionable error and NEVER authenticates", async () => {
    // Simulate network error / offline backend
    globalThis.fetch = async () => {
      throw new Error("Failed to fetch");
    };

    await assert.rejects(
      async () => {
        await login({ username: "admin", password: "password" });
      },
      (err: Error) => {
        assert.ok(
          err.message.includes("Unable to reach the Trapple authentication service"),
          `Expected network error message, got: ${err.message}`
        );
        return true;
      }
    );

    // Verify no tokens were persisted in localStorage
    assert.strictEqual(getToken(), null, "No token must be set on failure");
    assert.strictEqual(getStoredUser(), null, "No user must be set on failure");
  });

  await t.test("Vercel HTML rewrite (unhosted backend) throws HTML fallback error instead of silent mock auth", async () => {
    // Simulate Vercel returning index.html for unrouted backend API
    globalThis.fetch = async () => {
      return new Response("<!DOCTYPE html><html><body>Single Page App</body></html>", {
        status: 200,
        headers: { "Content-Type": "text/html" },
      });
    };

    await assert.rejects(
      async () => {
        await login({ username: "123456", password: "password" });
      },
      (err: Error) => {
        assert.ok(
          err.message.includes("Unable to reach the Trapple authentication service"),
          `Expected server unreachable error, got: ${err.message}`
        );
        return true;
      }
    );
  });

  await t.test("401 Unauthorized clears authentication and throws session expired", async () => {
    // Populate fake existing session
    mockStorage.setItem("trapple_token", "fake-token");
    mockStorage.setItem("trapple_user", JSON.stringify({ id: 1, username: "test", role: "EMPLOYEE" }));

    // Mock 401 response from backend
    globalThis.fetch = async () => {
      return new Response(JSON.stringify({ message: "Invalid token or expired session" }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      });
    };

    await assert.rejects(
      async () => {
        await travelRequestsApi.list();
      },
      (err: Error) => {
        assert.ok(err.message.includes("session has expired") || err.message.includes("Unauthorized"));
        return true;
      }
    );

    // Stored auth should be cleared
    assert.strictEqual(getToken(), null, "401 must clear stored token");
  });

  await t.test("403 Forbidden raises permission error without fallback success", async () => {
    mockStorage.setItem("trapple_token", "valid-user-token");

    globalThis.fetch = async () => {
      return new Response(JSON.stringify({ message: "Access Denied" }), {
        status: 403,
        headers: { "Content-Type": "application/json" },
      });
    };

    await assert.rejects(
      async () => {
        await bookingsApi.list();
      },
      (err: Error) => {
        assert.ok(err.message.includes("permission"), `Expected permission error, got: ${err.message}`);
        return true;
      }
    );
  });

  await t.test("Backend 500 error raises server error and does not return fabricated records", async () => {
    globalThis.fetch = async () => {
      return new Response(JSON.stringify({ error: "Internal Database Connection Pool Exhausted" }), {
        status: 500,
        headers: { "Content-Type": "application/json" },
      });
    };

    await assert.rejects(
      async () => {
        await travelRequestsApi.list();
      },
      (err: Error) => {
        assert.ok(
          err.message.includes("Internal Database Connection Pool Exhausted") ||
          err.message.includes("Trapple is having trouble right now"),
          `Expected server error message, got: ${err.message}`
        );
        return true;
      }
    );
  });

  await t.test("Successful authoritative login sets legitimate backend JWT session", async () => {
    const fakeBackendJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.legitimate-backend-token";
    globalThis.fetch = async (url: any) => {
      if (String(url).includes("/api/auth/login")) {
        return new Response(
          JSON.stringify({
            id: 42,
            username: "real.user",
            role: "EMPLOYEE",
            employeeId: "MISPL0042",
            token: fakeBackendJwt,
          }),
          {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }
        );
      }
      return new Response("Not Found", { status: 404 });
    };

    const user = await login({ username: "real.user", password: "StrongPassword123!" });
    assert.strictEqual(user.id, 42);
    assert.strictEqual(user.username, "real.user");
    assert.strictEqual(user.role, "EMPLOYEE");
    assert.strictEqual(user.token, fakeBackendJwt);

    persistAuth(user);
    assert.strictEqual(getToken(), fakeBackendJwt);
    assert.strictEqual(getStoredUser()?.username, "real.user");
  });
});


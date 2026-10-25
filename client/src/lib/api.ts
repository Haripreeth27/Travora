function hasStorage(): boolean {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

const isLocal =
  typeof window !== "undefined" &&
  (window.location?.hostname === "localhost" || window.location?.hostname === "127.0.0.1");
const API_BASE_URL =
  (typeof import.meta !== "undefined" && import.meta.env?.VITE_API_BASE_URL) ||
  (isLocal ? "http://localhost:8080" : "");

export type Role = "EMPLOYEE" | "APPROVER" | "TRAVEL_DESK" | "ADMIN" | "COMPANY_ADMIN" | "PLATFORM_ADMIN";
export type AuthUser = {
  id: number;
  username: string;
  role: Role;
  employeeId?: string | null;
  token?: string;
  companyId?: number | null;
  companyCode?: string | null;
  companyName?: string | null;
};
export type ApiRequestOptions = RequestInit & {
  skipAuth?: boolean;
  ignoreAuthFailure?: boolean;
  timeoutMs?: number;
};

export const AUTH_EXPIRED_EVENT = "travora:auth-expired";
const VALID_ROLES: Role[] = ["EMPLOYEE", "APPROVER", "TRAVEL_DESK", "ADMIN", "COMPANY_ADMIN", "PLATFORM_ADMIN"];

export const DEMO_REQUESTS: any[] = [];
export const DEMO_BOOKINGS: any[] = [];

export type EmployeeAccount = {
  id: number;
  employeeNumber: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  displayName: string;
  fullName: string;
  workEmail: string;
  department: string;
  designation: string;
};

export const REGISTERED_EMPLOYEES: EmployeeAccount[] = [
  {
    id: 101,
    employeeNumber: "AGR0001",
    firstName: "Suresh Babu",
    lastName: "Dangeti",
    displayName: "Suresh Babu Dangeti",
    fullName: "Suresh Babu Dangeti",
    workEmail: "suresh@agrpetro.com",
    department: "Petroleum Operations",
    designation: "Executive",
  },
  {
    id: 102,
    employeeNumber: "AGR0003",
    firstName: "Ganesh",
    lastName: "Kakarla",
    displayName: "Ganesh Kakarla",
    fullName: "Ganesh Kakarla",
    workEmail: "site.er4@agrpetro.com",
    department: "Site Operations",
    designation: "Site Engineer",
  },
  {
    id: 103,
    employeeNumber: "AGR0007",
    firstName: "Anil",
    lastName: "Kukreja",
    displayName: "Anil Kukreja",
    fullName: "Anil Kukreja",
    workEmail: "anil.kukreja@agrpetro.com",
    department: "Operations",
    designation: "Manager",
  },
  {
    id: 104,
    employeeNumber: "AGR0009",
    firstName: "Ramana",
    lastName: "M",
    displayName: "Ramana M",
    fullName: "Ramana M",
    workEmail: "sureshbabu506@gmail.com",
    department: "Operations",
    designation: "Operations Executive",
  },
  {
    id: 105,
    employeeNumber: "MISPL0001",
    firstName: "Umamaheswari",
    lastName: "Yandapalli",
    displayName: "Umamaheswari Yandapalli",
    fullName: "Umamaheswari Yandapalli",
    workEmail: "umamaheswari@gmail.com",
    department: "Corporate Management",
    designation: "Director",
  },
  {
    id: 106,
    employeeNumber: "MISPL0002",
    firstName: "Kalyan",
    middleName: "Swaroop",
    lastName: "Yandapalli",
    displayName: "YKS",
    fullName: "Kalyan Swaroop Yandapalli",
    workEmail: "yks@mahathiinfra.com",
    department: "Executive Management",
    designation: "Managing Director",
  },
  {
    id: 107,
    employeeNumber: "MISPL0003",
    firstName: "Himani",
    lastName: "Agarwal",
    displayName: "Himani Agarwal",
    fullName: "Himani Agarwal",
    workEmail: "himani.ag@mahathiinfra.com",
    department: "Finance & Accounts",
    designation: "Finance Manager",
  },
  {
    id: 108,
    employeeNumber: "MISPL0007",
    firstName: "Sirisha",
    lastName: "Vegaraju",
    displayName: "Sirisha Vegaraju",
    fullName: "Sirisha Vegaraju",
    workEmail: "sirisha@mahathiinfra.com",
    department: "Administration",
    designation: "Executive Admin",
  },
  {
    id: 109,
    employeeNumber: "MISPL0014",
    firstName: "Vijaya Kumar",
    lastName: "Bgam",
    displayName: "Bagam Vijaya Kumar",
    fullName: "Vijaya Kumar Bgam",
    workEmail: "kumar.bv@mahathiinfra.com",
    department: "Project Engineering",
    designation: "Senior Project Engineer",
  },
  {
    id: 110,
    employeeNumber: "MISPL0016",
    firstName: "P",
    lastName: "Satheesh",
    displayName: "P Satheesh",
    fullName: "P Satheesh",
    workEmail: "satheesh@mahathiinfra.com",
    department: "Field Engineering",
    designation: "Project Lead",
  },
  {
    id: 111,
    employeeNumber: "MISPL0020",
    firstName: "Satya",
    middleName: "Prasad",
    lastName: "B",
    displayName: "B Satya Prasad",
    fullName: "Satya Prasad B",
    workEmail: "satya.prasad@mahathiinfra.com",
    department: "Procurement",
    designation: "Procurement Manager",
  },
  {
    id: 112,
    employeeNumber: "MISPL0027",
    firstName: "M Murali",
    middleName: "Dhara",
    lastName: "Reddy",
    displayName: "M Murali Dhara Reddy",
    fullName: "M Murali Dhara Reddy",
    workEmail: "murali.reddy@mahathiinfra.com",
    department: "Infrastructure Planning",
    designation: "General Manager",
  },
  {
    id: 113,
    employeeNumber: "MISPL0130",
    firstName: "Deena",
    middleName: "Raju",
    lastName: "Karra",
    displayName: "Deena Raju Karra",
    fullName: "Deena Raju Karra",
    workEmail: "deenaraju@mahathiinfra.com",
    department: "Commercial & Contracts",
    designation: "Commercial Manager",
  },
  {
    id: 114,
    employeeNumber: "MISPL0164",
    firstName: "D Vasantha",
    lastName: "Lakshmi",
    displayName: "D Vasantha Lakshmi",
    fullName: "D Vasantha Lakshmi",
    workEmail: "hr@mahathiinfra.com",
    department: "Human Resources",
    designation: "HR Head",
  },
  {
    id: 115,
    employeeNumber: "MISPL0275",
    firstName: "Sulamangalam",
    middleName: "Dinesh",
    lastName: "Kumar",
    displayName: "S Dinesh Kumar",
    fullName: "Sulamangalam Dinesh Kumar",
    workEmail: "dinesh.s@mahathiinfra.com",
    department: "Quality Assurance",
    designation: "QA/QC Lead",
  },
  {
    id: 116,
    employeeNumber: "MISPL0333",
    firstName: "Mohan",
    lastName: "Jagatha",
    displayName: "Mohan Jagatha",
    fullName: "Mohan Jagatha",
    workEmail: "mohan.j@mahathiinfra.com",
    department: "Logistics & Supply",
    designation: "Supply Chain Executive",
  },
  {
    id: 117,
    employeeNumber: "MISPL0372",
    firstName: "Vidyasagar",
    lastName: "Gorantala",
    displayName: "G Vidyasagar",
    fullName: "Vidyasagar Gorantala",
    workEmail: "vidyasagar.g@mahathiinfra.com",
    department: "Civil & Structural",
    designation: "Structural Engineer",
  },
  {
    id: 118,
    employeeNumber: "MISPL0387",
    firstName: "Thenmozhi",
    lastName: "S",
    displayName: "Thenmozhi",
    fullName: "Thenmozhi S",
    workEmail: "thenmozhi@mahathiinfra.com",
    department: "Design & Drafting",
    designation: "Senior Design Engineer",
  },
  {
    id: 119,
    employeeNumber: "MISPL0402",
    firstName: "G Yathish",
    middleName: "Sai Krishna",
    lastName: "Posi",
    displayName: "G Yathish Posi Sai Krishna",
    fullName: "G Yathish Sai Krishna Posi",
    workEmail: "yathish.g@mahathiinfra.com",
    department: "Instrumentation",
    designation: "Instrumentation Engineer",
  },
  {
    id: 120,
    employeeNumber: "MISPL0409",
    firstName: "Ravi",
    middleName: "Raghavendra Durga Prasad",
    lastName: "K",
    displayName: "K Ravi Raghavendra Durga Prasad",
    fullName: "Ravi Raghavendra Durga Prasad K",
    workEmail: "prasad@mahathiinfra.com",
    department: "Electrical & Instrumentation",
    designation: "Project Manager",
  },
  {
    id: 121,
    employeeNumber: "MISPL0415",
    firstName: "Kranthi",
    lastName: "Bharkam",
    displayName: "Kranthi Bharkam",
    fullName: "Kranthi Bharkam",
    workEmail: "kranthi.b@mahathiinfra.com",
    department: "Mechanical Systems",
    designation: "Mechanical Engineer",
  },
  {
    id: 122,
    employeeNumber: "MISPL0423",
    firstName: "Rammohan",
    lastName: "Gubbala",
    displayName: "Rammohan Gubbala",
    fullName: "Rammohan Gubbala",
    workEmail: "g.rammohan@mahathiinfra.com",
    department: "Safety & HSE",
    designation: "HSE Officer",
  },
  {
    id: 123,
    employeeNumber: "MISPL0429",
    firstName: "Uppu",
    lastName: "Durga Rao",
    displayName: "Uppu Durga Rao",
    fullName: "Uppu Durga Rao",
    workEmail: "durgarao@mahathiinfra.com",
    department: "Piping Engineering",
    designation: "Piping Lead",
  },
  {
    id: 124,
    employeeNumber: "MISPL0430",
    firstName: "Malaya",
    lastName: "Kumar",
    displayName: "Malaya Kumar",
    fullName: "Malaya Kumar",
    workEmail: "malayakumar@mahathiinfra.com",
    department: "Planning & Controls",
    designation: "Planning Engineer",
  },
  {
    id: 125,
    employeeNumber: "MISPL0440",
    firstName: "Dileep Kumar",
    lastName: "Gode",
    displayName: "Dileep Kumar Gode",
    fullName: "Dileep Kumar Gode",
    workEmail: "dileep.g@mahathiinfra.com",
    department: "Maintenance & Reliability",
    designation: "Site Supervisor",
  },
  {
    id: 126,
    employeeNumber: "MISPL0466",
    firstName: "Panthangi",
    lastName: "Shashidhar",
    displayName: "Panthangi Shashidhar",
    fullName: "Panthangi Shashidhar",
    workEmail: "shashidhar@mahathiinfra.com",
    department: "Operations Coordination",
    designation: "Operations Lead",
  },
  {
    id: 127,
    employeeNumber: "MISPL0485",
    firstName: "Sumanth Krishna",
    lastName: "Gaddam",
    displayName: "Sumanth Krishna Gaddam",
    fullName: "Sumanth Krishna Gaddam",
    workEmail: "g.sumanthkrishna@mahathiinfra.com",
    department: "Technology & Systems",
    designation: "Systems Engineer",
  },
];

export const DEMO_EMPLOYEES = REGISTERED_EMPLOYEES.map((emp) => ({
  id: String(emp.id),
  name: emp.fullName,
  displayName: emp.displayName,
  department: emp.department,
  designation: emp.designation,
  employeeId: emp.employeeNumber,
  email: emp.workEmail,
}));

export function getToken() {
  const token = window.localStorage.getItem("trapple_token") || window.localStorage.getItem("travora_token");
  return token?.trim() || null;
}

export function getStoredUser<T = AuthUser>(): T | null {
  const raw = window.localStorage.getItem("trapple_user") || window.localStorage.getItem("travora_user");
  if (!raw) return null;
  try { return JSON.parse(raw) as T; } catch { return null; }
}

export function isValidAuthUser(user: AuthUser | null): user is AuthUser {
  return Boolean(user && Number.isFinite(user.id) && user.username && VALID_ROLES.includes(user.role));
}

export function clearAuth({ notify = false }: { notify?: boolean } = {}) {
  window.localStorage.removeItem("trapple_token");
  window.localStorage.removeItem("trapple_user");
  window.localStorage.removeItem("travora_token");
  window.localStorage.removeItem("travora_user");
  if (notify) window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT));
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { skipAuth, ignoreAuthFailure, timeoutMs = 1500, headers, ...requestOptions } = options;
  const token = getToken();
  let response: Response;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...requestOptions,
      signal: controller.signal,
      headers: {
        "Content-Type": "application/json",
        ...(token && !skipAuth ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
    });
  } catch {
    throw new Error("We couldn't reach Trapple. Check your connection and try again.");
  } finally {
    clearTimeout(timer);
  }

  const body = await response.text();
  const isHtml = (response.headers.get("content-type") || "").includes("text/html") || body.trim().startsWith("<!doctype") || body.trim().startsWith("<!DOCTYPE") || body.trim().startsWith("<html");
  if (isHtml) {
    throw new Error("Trapple backend server not reached (HTML fallback).");
  }
  let data: unknown = null;
  try { data = body ? JSON.parse(body) : null; } catch { data = body || null; }
  if (response.status === 401) {
    if (skipAuth || ignoreAuthFailure) {
      throw new Error("Unauthorized");
    }
    clearAuth({ notify: true });
    throw new Error("Your session has expired. Please sign in again.");
  }
  if (response.status === 403) throw new Error("You don't have permission to access this area.");
  if (!response.ok) {
    const message = typeof data === "object" && data !== null ? (data as { message?: string; error?: string }).message || (data as { error?: string }).error : undefined;
    throw new Error(message || (response.status >= 500 ? "Trapple is having trouble right now. Please try again." : "Something went wrong. Please try again."));
  }
  return data as T;
}

export async function login(credentials: { username: string; password: string }): Promise<AuthUser> {
  const rawUser = (credentials.username || "").trim();
  const rawPass = (credentials.password || "").trim();

  if (!rawUser) {
    throw new Error("Please enter your Login ID / Employee Number.");
  }
  if (!rawPass) {
    throw new Error("Please enter your password.");
  }

  // If a live API backend is configured, attempt authoritative backend authentication first
  if (API_BASE_URL) {
    try {
      const response = await apiRequest<Partial<AuthUser> & { token?: string; role?: string }>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify(credentials),
        skipAuth: true,
        timeoutMs: 3000,
      });
      const role = String(response.role || "").toUpperCase() as Role;
      if (response.token && response.username && VALID_ROLES.includes(role) && response.id !== undefined && response.id !== null) {
        return { ...response, id: Number(response.id), role, token: response.token } as AuthUser;
      }
    } catch (err: any) {
      // If the backend actively rejected with invalid credentials or inactive account, raise error directly
      if (
        err?.message &&
        !err.message.includes("not reached") &&
        !err.message.includes("failed to fetch") &&
        !err.message.includes("Failed to fetch") &&
        !err.message.includes("NetworkError")
      ) {
        throw err;
      }
      // Otherwise backend is offline/unreachable; continue to fallback demo accounts below
    }
  }

  const userLower = rawUser.toLowerCase();
  const passLower = rawPass.toLowerCase();

  // 1. Travel Desk login: Employee ID "123456" and password "mahathi"
  const isTravelDesk =
    rawUser === "123456" ||
    userLower === "traveldesk" ||
    userLower === "travel_desk" ||
    userLower === "travel-desk";

  if (isTravelDesk) {
    const validDeskPass =
      rawPass === "mahathi" ||
      (rawUser !== "123456" && (rawPass === "password" || rawPass === "mahathi"));
    if (!validDeskPass) {
      throw new Error("Invalid username or password.");
    }
    return {
      id: 4,
      username: "Travel Desk",
      role: "TRAVEL_DESK",
      employeeId: "123456",
      token: "jwt-traveldesk-auth-token",
    };
  }

  // 2. Approver login: Employee ID "654321" and password "mahathi1"
  const isApprover =
    rawUser === "654321" ||
    userLower === "approver" ||
    userLower === "rajesh";

  if (isApprover) {
    const validApproverPass =
      rawPass === "mahathi1" ||
      (rawUser !== "654321" && (rawPass === "password" || rawPass === "mahathi1"));
    if (!validApproverPass) {
      throw new Error("Invalid username or password.");
    }
    return {
      id: 2,
      username: "Approver",
      role: "APPROVER",
      employeeId: "654321",
      token: "jwt-approver-auth-token",
    };
  }

  // 3. Admin login:
  if (userLower === "admin") {
    if (rawPass !== "password" && rawPass !== "admin" && rawPass !== "admin123") {
      throw new Error("Invalid username or password.");
    }
    return {
      id: 3,
      username: "System Admin",
      role: "ADMIN",
      employeeId: "ADM-0001",
      token: "jwt-admin-auth-token",
    };
  }

  // 4. Registered Employees from image:
  // Login ID = Employee Number (e.g. AGR0001, MISPL0001)
  // Password = Employee Number (same as login ID, case-insensitive)
  const matchedEmployee = REGISTERED_EMPLOYEES.find(
    (emp) => emp.employeeNumber.toLowerCase() === userLower
  );

  if (matchedEmployee) {
    if (passLower !== matchedEmployee.employeeNumber.toLowerCase()) {
      throw new Error("Invalid username or password.");
    }
    return {
      id: matchedEmployee.id,
      username: matchedEmployee.fullName,
      role: "EMPLOYEE",
      employeeId: matchedEmployee.employeeNumber,
      token: `jwt-emp-${matchedEmployee.employeeNumber.toLowerCase()}-token`,
    };
  }

  // Legacy demo employee accounts fallback
  if (userLower === "employee" || userLower === "arjun" || userLower === "emp-2026") {
    if (rawPass !== "password" && userLower !== passLower) {
      throw new Error("Invalid username or password.");
    }
    return {
      id: 1,
      username: "Arjun Mehta",
      role: "EMPLOYEE",
      employeeId: "EMP-2026",
      token: "jwt-employee-auth-token",
    };
  }

  // Reject invalid credentials
  throw new Error("Invalid username or password.");
}

export function persistAuth(user: AuthUser) {
  if (!user.token) throw new Error("Cannot create an authenticated session without a JWT.");
  window.localStorage.setItem("trapple_token", user.token);
  window.localStorage.setItem("trapple_user", JSON.stringify({ id: user.id, username: user.username, role: user.role, employeeId: user.employeeId ?? null }));
  window.localStorage.setItem("travora_token", user.token);
  window.localStorage.setItem("travora_user", JSON.stringify({ id: user.id, username: user.username, role: user.role, employeeId: user.employeeId ?? null }));
}

/**
 * ─── UNIFIED SYSTEM STORAGE KEYS & TOMBSTONES ───
 */
const SYSTEM_REQUESTS_KEY = "travora_system_requests";
const SYSTEM_BOOKINGS_KEY = "travora_system_bookings";
const SYSTEM_BULK_BOOKINGS_KEY = "travora_system_bulk_bookings_v1";
const SYSTEM_NOTIFICATIONS_KEY = "travora_system_notifications";
const DELETED_REQUESTS_KEY = "travora_deleted_request_ids";
const DELETED_BOOKINGS_KEY = "travora_deleted_booking_ids";
const DELETED_BULK_BOOKINGS_KEY = "travora_deleted_bulk_booking_ids";

export function getDeletedRequestIds(): Set<string> {
  if (!hasStorage()) return new Set();
  try {
    const raw = window.localStorage.getItem(DELETED_REQUESTS_KEY);
    if (raw) {
      const arr = JSON.parse(raw);
      if (Array.isArray(arr)) return new Set(arr.map(String));
    }
  } catch (e) {
    console.warn("Failed to read deleted request ids", e);
  }
  return new Set();
}

export function markRequestIdsDeleted(ids: Array<string | number>) {
  if (!hasStorage()) return;
  try {
    const set = getDeletedRequestIds();
    ids.forEach((id) => {
      if (id !== undefined && id !== null) set.add(String(id));
    });
    window.localStorage.setItem(DELETED_REQUESTS_KEY, JSON.stringify(Array.from(set)));
  } catch (e) {
    console.warn("Failed to save deleted request ids", e);
  }
}

export function getDeletedBookingIds(): Set<string> {
  if (!hasStorage()) return new Set();
  try {
    const raw = window.localStorage.getItem(DELETED_BOOKINGS_KEY);
    if (raw) {
      const arr = JSON.parse(raw);
      if (Array.isArray(arr)) return new Set(arr.map(String));
    }
  } catch (e) {
    console.warn("Failed to read deleted booking ids", e);
  }
  return new Set();
}

export function markBookingIdsDeleted(ids: Array<string | number>) {
  if (!hasStorage()) return;
  try {
    const set = getDeletedBookingIds();
    ids.forEach((id) => {
      if (id !== undefined && id !== null) set.add(String(id));
    });
    window.localStorage.setItem(DELETED_BOOKINGS_KEY, JSON.stringify(Array.from(set)));
  } catch (e) {
    console.warn("Failed to save deleted booking ids", e);
  }
}

export function getDeletedBulkBookingIds(): Set<string> {
  if (!hasStorage()) return new Set();
  try {
    const raw = window.localStorage.getItem(DELETED_BULK_BOOKINGS_KEY);
    if (raw) {
      const arr = JSON.parse(raw);
      if (Array.isArray(arr)) return new Set(arr.map(String));
    }
  } catch (e) {
    console.warn("Failed to read deleted bulk booking ids", e);
  }
  return new Set();
}

export function markBulkBookingIdsDeleted(ids: Array<string | number>) {
  if (!hasStorage()) return;
  try {
    const set = getDeletedBulkBookingIds();
    ids.forEach((id) => {
      if (id !== undefined && id !== null) set.add(String(id));
    });
    window.localStorage.setItem(DELETED_BULK_BOOKINGS_KEY, JSON.stringify(Array.from(set)));
  } catch (e) {
    console.warn("Failed to save deleted bulk booking ids", e);
  }
}

/**
 * ─── LEGACY DATA PURGE & SANITIZATION ───
 * Runs immediately on startup to remove legacy demo trips and default bulk records.
 */
function _sanitizeLocalStorage() {
  if (!hasStorage()) return;
  try {
    const SANITIZE_VERSION = "travora_v7_no_bulk_defaults";
    if (window.localStorage.getItem(SANITIZE_VERSION) === "true") return;

    // Permanently tombstone and purge default mock bulk bookings (GRP-BK-101 and GRP-BK-102)
    markBulkBookingIdsDeleted(["GRP-BK-101", "GRP-BK-102"]);
    const currentBulk = window.localStorage.getItem(SYSTEM_BULK_BOOKINGS_KEY);
    if (currentBulk) {
      try {
        const parsed = JSON.parse(currentBulk);
        if (Array.isArray(parsed)) {
          const filtered = parsed.filter((b) => b && b.id !== "GRP-BK-101" && b.id !== "GRP-BK-102");
          window.localStorage.setItem(SYSTEM_BULK_BOOKINGS_KEY, JSON.stringify(filtered));
        }
      } catch {
        window.localStorage.removeItem(SYSTEM_BULK_BOOKINGS_KEY);
      }
    }

    // Remove legacy unnamespaced and migrated keys
    const LEGACY_KEYS = [
      "travora_v4_clean",
      "travora_v6_sync_clean",
      "travora_migration_done",
      "travora_local_requests",
      "travora_local_bookings",
      "travora_deleted_requests",
      "travora_deleted_bookings",
      "travora_all_wiped",
      "travora_expenses",
    ];
    LEGACY_KEYS.forEach((k) => window.localStorage.removeItem(k));

    // Inspect all keys in localStorage to purge contaminated records
    const keysToRemove: string[] = [];
    for (let i = 0; i < window.localStorage.length; i++) {
      const key = window.localStorage.key(i);
      if (!key) continue;
      if (
        key.startsWith("travora_local_") ||
        key.startsWith("travora_deleted_") ||
        key.startsWith("travora_all_wiped_")
      ) {
        const val = window.localStorage.getItem(key) || "";
        if (
          val.includes("EMP-2026") ||
          val.includes("Arjun Mehta") ||
          val.includes("TR-9021") ||
          val.includes("TR-9022") ||
          val.includes("TR-9023") ||
          val.includes("TR-9024") ||
          val.includes("BK-701")
        ) {
          keysToRemove.push(key);
        }
      }
    }
    keysToRemove.forEach((k) => window.localStorage.removeItem(k));
    window.localStorage.setItem(SANITIZE_VERSION, "true");
  } catch (e) {
    console.warn("Storage sanitization error:", e);
  }
}

// Run sanitization immediately
_sanitizeLocalStorage();

/**
 * ─── UNIFIED SYSTEM STORAGE & MULTI-USER DATA ISOLATION ───
 *
 * All records live in a system repository with explicit owner IDs:
 * - userId (numeric ID of creator)
 * - employeeId (employee number of creator, e.g. AGR0001, MISPL0001)
 * - employee (complete employee profile object)
 *
 * Strict Access Policy:
 * 1. EMPLOYEE: Can ONLY see, fetch, and mutate records where employeeId === current.employeeId
 *    or userId === current.id. Never sees another employee's records.
 * 2. APPROVER: Can see all submitted requests across all employees to review & decide.
 * 3. TRAVEL_DESK: Can see all approved requests to book tickets & attach PNRs.
 * 4. ADMIN: Can view all requests and bookings across the organization.
 */

function _getAllSystemRequests(): any[] {
  if (!hasStorage()) return [];
  const deletedIds = getDeletedRequestIds();
  try {
    const raw = window.localStorage.getItem(SYSTEM_REQUESTS_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        return parsed.filter((r) => !deletedIds.has(String(r.id)));
      }
    }
  } catch (e) {
    console.warn("Failed to load system requests", e);
  }
  return [];
}

function _saveAllSystemRequests(requests: any[]) {
  if (!hasStorage()) return;
  try {
    window.localStorage.setItem(SYSTEM_REQUESTS_KEY, JSON.stringify(requests));
  } catch (e) {
    console.warn("Failed to save system requests", e);
  }
}

function _getAllSystemBookings(): any[] {
  if (!hasStorage()) return [];
  const deletedBookingIds = getDeletedBookingIds();
  const deletedReqIds = getDeletedRequestIds();
  try {
    const raw = window.localStorage.getItem(SYSTEM_BOOKINGS_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        return parsed.filter(
          (b) => !deletedBookingIds.has(String(b.id)) && !deletedReqIds.has(String(b.travelRequest?.id))
        );
      }
    }
  } catch (e) {
    console.warn("Failed to load system bookings", e);
  }
  return [];
}

function _saveAllSystemBookings(bookings: any[]) {
  if (!hasStorage()) return;
  try {
    window.localStorage.setItem(SYSTEM_BOOKINGS_KEY, JSON.stringify(bookings));
  } catch (e) {
    console.warn("Failed to save system bookings", e);
  }
}

function _getAllSystemNotifications(): any[] {
  if (!hasStorage()) return [];
  try {
    const raw = window.localStorage.getItem(SYSTEM_NOTIFICATIONS_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) return parsed;
    }
  } catch (e) {
    console.warn("Failed to load system notifications", e);
  }
  return [];
}

function _saveAllSystemNotifications(notifs: any[]) {
  if (!hasStorage()) return;
  try {
    window.localStorage.setItem(SYSTEM_NOTIFICATIONS_KEY, JSON.stringify(notifs));
  } catch (e) {
    console.warn("Failed to save system notifications", e);
  }
}

/**
 * ─── REAL-TIME MULTI-DEVICE CLOUD SYNCHRONIZATION ───
 * Uses pub/sub to keep all shared browsers and devices in sync in real time.
 */
const SYNC_TOPIC = "travora_live_corp_sync_v6";
const SYNC_URL = `https://ntfy.sh/${SYNC_TOPIC}`;

export async function broadcastCloudSync(action: string, payload: any) {
  if (typeof window === "undefined") return;
  // SECURITY REMEDIATION: In production mode with backend API, do not broadcast to public broker
  if (API_BASE_URL) return;
  try {
    const body = JSON.stringify({
      action,
      payload,
      timestamp: Date.now(),
    });
    fetch(SYNC_URL, {
      method: "POST",
      body,
      headers: { "Content-Type": "application/json" },
    }).catch(() => {});
  } catch {}
}

export function handleIncomingCloudSync(msg: any) {
  if (!msg || !msg.action) return;
  try {
    const { action, payload } = msg;
    if (action === "CREATE_REQUEST" || action === "UPDATE_REQUEST") {
      const all = _getAllSystemRequests();
      const idx = all.findIndex((r) => String(r.id) === String(payload.id));
      if (idx >= 0) all[idx] = payload;
      else all.unshift(payload);
      _saveAllSystemRequests(all);
    } else if (action === "DELETE_REQUEST") {
      markRequestIdsDeleted([payload.id]);
    } else if (action === "CREATE_BULK" || action === "UPDATE_BULK" || action === "REVIEW_BULK") {
      const deletedBulk = getDeletedBulkBookingIds();
      if (!deletedBulk.has(String(payload.id))) {
        const all = _getAllSystemBulkBookings();
        const idx = all.findIndex((b) => String(b.id) === String(payload.id));
        if (idx >= 0) all[idx] = payload;
        else all.unshift(payload);
        _saveAllSystemBulkBookings(all);
      }
    } else if (action === "DELETE_BULK") {
      markBulkBookingIdsDeleted([payload.id]);
      const all = _getAllSystemBulkBookings().filter((b) => String(b.id) !== String(payload.id));
      _saveAllSystemBulkBookings(all);
    } else if (action === "CREATE_BOOKING" || action === "UPDATE_BOOKING" || action === "CANCEL_BOOKING") {
      const all = _getAllSystemBookings();
      const idx = all.findIndex((b) => String(b.id) === String(payload.id));
      if (idx >= 0) all[idx] = payload;
      else all.unshift(payload);
      _saveAllSystemBookings(all);
    }
    window.dispatchEvent(new CustomEvent("trapple_cloud_synced", { detail: msg }));
    window.dispatchEvent(new CustomEvent("travora_cloud_synced", { detail: msg }));
  } catch (e) {
    console.warn("Failed to apply incoming cloud sync", e);
  }
}

let _cloudSyncStarted = false;
export function initCloudSync() {
  if (typeof window === "undefined" || _cloudSyncStarted) return;
  // SECURITY REMEDIATION: In production mode with backend API, disable public sync channel
  if (API_BASE_URL) return;
  _cloudSyncStarted = true;

  try {
    const since = Math.floor(Date.now() / 1000) - 3600;
    fetch(`${SYNC_URL}/json?poll=1&since=${since}`)
      .then((res) => res.text())
      .then((text) => {
        const lines = text.trim().split("\n");
        lines.forEach((line) => {
          try {
            const data = JSON.parse(line);
            if (data?.message) {
              const parsed = JSON.parse(data.message);
              handleIncomingCloudSync(parsed);
            }
          } catch {}
        });
      })
      .catch(() => {});
  } catch {}

  try {
    if (typeof EventSource !== "undefined") {
      const es = new EventSource(`${SYNC_URL}/sse`);
      es.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          if (data?.message) {
            const parsed = JSON.parse(data.message);
            handleIncomingCloudSync(parsed);
          }
        } catch {}
      };
    }
  } catch {}

  window.addEventListener("focus", () => {
    try {
      const since = Math.floor(Date.now() / 1000) - 300;
      fetch(`${SYNC_URL}/json?poll=1&since=${since}`)
        .then((res) => res.text())
        .then((text) => {
          const lines = text.trim().split("\n");
          lines.forEach((line) => {
            try {
              const data = JSON.parse(line);
              if (data?.message) {
                const parsed = JSON.parse(data.message);
                handleIncomingCloudSync(parsed);
              }
            } catch {}
          });
        })
        .catch(() => {});
    } catch {}
  });
}

export async function manualCloudSync(): Promise<{ success: boolean; count: number }> {
  if (API_BASE_URL) return { success: true, count: 0 };
  try {
    const since = Math.floor(Date.now() / 1000) - 7200;
    const res = await fetch(`${SYNC_URL}/json?poll=1&since=${since}`);
    const text = await res.text();
    let applied = 0;
    const lines = text.trim().split("\n");
    lines.forEach((line) => {
      try {
        const data = JSON.parse(line);
        if (data?.message) {
          const parsed = JSON.parse(data.message);
          handleIncomingCloudSync(parsed);
          applied++;
        }
      } catch {}
    });
    window.dispatchEvent(new CustomEvent("trapple_cloud_synced"));
    window.dispatchEvent(new CustomEvent("travora_cloud_synced"));
    return { success: true, count: applied };
  } catch (err) {
    return { success: false, count: 0 };
  }
}

if (typeof window !== "undefined") {
  initCloudSync();
}

export type BulkBookingPassenger = {
  name: string;
  employeeId?: string;
  department?: string;
  seatOrCoach?: string;
};

export type BulkBookingRecord = {
  id: string;
  title: string;
  destination: string;
  fromLocation: string;
  travelDates: string;
  departureDate?: string;
  returnDate?: string;
  travelMode: "FLIGHT" | "TRAIN" | "BUS" | "HOTEL" | "MULTI_MODAL";
  provider: string;
  groupReference: string;
  headcount: number;
  totalCost: number;
  costPerPerson: number;
  savings?: number;
  project?: string;
  departments?: string[];
  passengerManifestSummary?: string;
  passengerList?: BulkBookingPassenger[];
  itineraryNotes?: string;
  coordinatorName?: string;
  loggedAt: string;
  status: "CONFIRMED" | "IN_REVIEW" | "CANCELLED";
  approverStatus?: "APPROVED" | "PENDING_REVIEW" | "NOTED";
  approverComment?: string;
  approverReviewedAt?: string;
  attachmentName?: string;
  attachmentData?: string;
};

const DEFAULT_BULK_BOOKINGS: BulkBookingRecord[] = [];

function _getAllSystemBulkBookings(): BulkBookingRecord[] {
  if (!hasStorage()) return [];
  const deletedIds = getDeletedBulkBookingIds();
  try {
    const raw = window.localStorage.getItem(SYSTEM_BULK_BOOKINGS_KEY);
    if (raw !== null) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        return parsed.filter((b) => b && b.id && !deletedIds.has(String(b.id)));
      }
    }
  } catch (e) {
    console.warn("Failed to load system bulk bookings", e);
  }
  return [];
}

function _saveAllSystemBulkBookings(bookings: BulkBookingRecord[]) {
  if (!hasStorage()) return;
  try {
    const deletedIds = getDeletedBulkBookingIds();
    const clean = bookings.filter((b) => b && b.id && !deletedIds.has(String(b.id)));
    window.localStorage.setItem(SYSTEM_BULK_BOOKINGS_KEY, JSON.stringify(clean));
  } catch (e) {
    console.warn("Failed to save system bulk bookings", e);
  }
}

/** Check if a record belongs to the given user */
function _isOwner(record: any, user: AuthUser | null): boolean {
  if (!user || !record) return false;
  const userEmpId = String(user.employeeId || "").trim().toLowerCase();
  const userIdStr = String(user.id);

  const recEmpId = String(record.employeeId || record.employee?.employeeId || "").trim().toLowerCase();
  const recUserId = String(record.userId || record.employee?.id || "").trim();

  if (userEmpId && recEmpId && userEmpId === recEmpId) return true;
  if (userIdStr && recUserId && userIdStr === recUserId) return true;
  return false;
}

export const travelRequestsApi = {
  list: async () => {
    const user = getStoredUser<AuthUser>();
    const deleted = getDeletedRequestIds();

    let records: any[] = [];
    try {
      const data = await apiRequest<any[]>("/api/travel-requests", { ignoreAuthFailure: true, timeoutMs: 1200 });
      if (Array.isArray(data)) {
        records = data.filter((r) => !deleted.has(String(r.id)));
      } else {
        records = _getAllSystemRequests();
      }
    } catch {
      records = _getAllSystemRequests();
    }

    // Role-based data isolation
    if (user && user.role === "EMPLOYEE") {
      return records.filter((r) => _isOwner(r, user));
    }
    // APPROVER, TRAVEL_DESK, and ADMIN see all requests across employees
    return records;
  },

  get: async (id: string) => {
    const user = getStoredUser<AuthUser>();
    const deleted = getDeletedRequestIds();
    if (deleted.has(String(id))) return null;

    let req: any = null;
    try {
      const remote = await apiRequest(`/api/travel-requests/${id}`, { ignoreAuthFailure: true, timeoutMs: 1200 });
      if (remote && !deleted.has(String((remote as any).id))) req = remote;
      else req = _getAllSystemRequests().find((r) => String(r.id) === String(id)) || null;
    } catch {
      req = _getAllSystemRequests().find((r) => String(r.id) === String(id)) || null;
    }

    if (!req) return null;
    // Ownership enforcement for employees
    if (user && user.role === "EMPLOYEE" && !_isOwner(req, user)) {
      return null;
    }
    return req;
  },

  create: async (payload: unknown) => {
    const user = getStoredUser<AuthUser>();
    if (!user) throw new Error("Authentication required to create a travel request.");

    const regEmp = REGISTERED_EMPLOYEES.find(
      (e) => e.employeeNumber.toLowerCase() === String(user.employeeId || "").toLowerCase()
    );

    const newReq = {
      id: `TR-${Math.floor(Math.random() * 9000 + 1000)}`,
      userId: user.id,
      employeeId: user.employeeId || `EMP-${user.id}`,
      passenger: regEmp?.fullName || user.username,
      status: "PENDING",
      createdAt: new Date().toISOString(),
      employee: {
        id: user.id,
        name: regEmp?.fullName || user.username,
        employeeId: user.employeeId || `EMP-${user.id}`,
        department: regEmp?.department || "General Operations",
        designation: regEmp?.designation || "Employee",
      },
      ...(payload as object),
    };

    const current = _getAllSystemRequests();
    _saveAllSystemRequests([newReq, ...current]);
    broadcastCloudSync("CREATE_REQUEST", newReq);

    // Send notifications
    const notifs = _getAllSystemNotifications();
    const newNotifs = [
      {
        id: `NOTIF-${Date.now()}-1`,
        recipientId: user.id,
        recipientEmployeeId: user.employeeId,
        title: "Travel request submitted",
        message: `Your request to ${(newReq as any).toLocation || "destination"} was submitted for manager review.`,
        type: "REQUEST",
        createdAt: new Date().toISOString(),
        read: false,
      },
      {
        id: `NOTIF-${Date.now()}-2`,
        targetRole: "APPROVER",
        title: "New travel request pending review",
        message: `${regEmp?.fullName || user.username} submitted a request to ${(newReq as any).toLocation || "destination"}.`,
        type: "APPROVAL",
        createdAt: new Date().toISOString(),
        read: false,
      },
    ];
    _saveAllSystemNotifications([...newNotifs, ...notifs]);

    try {
      await apiRequest("/api/travel-requests", { method: "POST", body: JSON.stringify(newReq), ignoreAuthFailure: true });
    } catch {
      // Backend offline fallback handled by system storage
    }
    return newReq;
  },

  approve: async (id: string, approverName: string, comment = "") => {
    const current = _getAllSystemRequests();
    let targetReq: any = null;
    const updated = current.map((r) => {
      if (String(r.id) === String(id)) {
        targetReq = { ...r, status: "APPROVED", approverName, approvalComment: comment, approvalDate: new Date().toISOString() };
        return targetReq;
      }
      return r;
    });
    _saveAllSystemRequests(updated);
    if (targetReq) broadcastCloudSync("UPDATE_REQUEST", targetReq);

    // Notify the employee & travel desk
    if (targetReq) {
      const notifs = _getAllSystemNotifications();
      const newNotifs = [
        {
          id: `NOTIF-${Date.now()}-1`,
          recipientId: targetReq.userId,
          recipientEmployeeId: targetReq.employeeId,
          title: "Travel request approved",
          message: `Your trip to ${targetReq.toLocation} was approved by ${approverName}. Ready for booking.`,
          type: "APPROVAL",
          createdAt: new Date().toISOString(),
          read: false,
        },
        {
          id: `NOTIF-${Date.now()}-2`,
          targetRole: "TRAVEL_DESK",
          title: "Approved trip ready to book",
          message: `Approved trip for ${targetReq.passenger || targetReq.employee?.name} to ${targetReq.toLocation} is ready for ticketing.`,
          type: "BOOKING",
          createdAt: new Date().toISOString(),
          read: false,
        },
      ];
      _saveAllSystemNotifications([...newNotifs, ...notifs]);
    }

    try {
      await apiRequest(`/api/travel-requests/${id}/approve`, {
        method: "PATCH",
        body: JSON.stringify({ approverName, comment }),
        ignoreAuthFailure: true,
      });
    } catch {
      // Offline fallback
    }
    return { id, status: "APPROVED", approverName, comment };
  },

  reject: async (id: string, approverName: string, comment = "") => {
    const current = _getAllSystemRequests();
    let targetReq: any = null;
    const updated = current.map((r) => {
      if (String(r.id) === String(id)) {
        targetReq = { ...r, status: "REJECTED", approverName, approvalComment: comment, rejectionDate: new Date().toISOString() };
        return targetReq;
      }
      return r;
    });
    _saveAllSystemRequests(updated);
    if (targetReq) broadcastCloudSync("UPDATE_REQUEST", targetReq);

    if (targetReq) {
      const notifs = _getAllSystemNotifications();
      const newNotif = {
        id: `NOTIF-${Date.now()}`,
        recipientId: targetReq.userId,
        recipientEmployeeId: targetReq.employeeId,
        title: "Travel request rejected",
        message: `Your trip to ${targetReq.toLocation} was rejected by ${approverName}. Reason: ${comment || "Policy compliance"}.`,
        type: "REJECTION",
        createdAt: new Date().toISOString(),
        read: false,
      };
      _saveAllSystemNotifications([newNotif, ...notifs]);
    }

    try {
      await apiRequest(`/api/travel-requests/${id}/reject`, {
        method: "PATCH",
        body: JSON.stringify({ approverName, comment }),
        ignoreAuthFailure: true,
      });
    } catch {
      // Offline fallback
    }
    return { id, status: "REJECTED", approverName, comment };
  },

  delete: async (id: string | number) => {
    const strId = String(id);
    const user = getStoredUser<AuthUser>();

    // Ownership check for employees
    const all = _getAllSystemRequests();
    const req = all.find((r) => String(r.id) === strId);
    if (user && user.role === "EMPLOYEE" && req && !_isOwner(req, user)) {
      throw new Error("You can only delete your own travel requests.");
    }

    markRequestIdsDeleted([strId]);
    _saveAllSystemRequests(all.filter((r) => String(r.id) !== strId));
    broadcastCloudSync("DELETE_REQUEST", { id: strId });

    // Also remove linked bookings
    const bookings = _getAllSystemBookings();
    const linked = bookings.filter((b) => String(b.travelRequest?.id) === strId);
    if (linked.length > 0) {
      markBookingIdsDeleted(linked.map((b) => b.id));
      _saveAllSystemBookings(bookings.filter((b) => String(b.travelRequest?.id) !== strId));
    }

    try {
      await apiRequest(`/api/travel-requests/${strId}`, { method: "DELETE", ignoreAuthFailure: true, timeoutMs: 1000 });
    } catch {
      // Offline fallback
    }
    return { success: true, id: strId };
  },

  deleteAll: async () => {
    const user = getStoredUser<AuthUser>();
    const allReqs = _getAllSystemRequests();
    const allBookings = _getAllSystemBookings();

    if (user && user.role === "EMPLOYEE") {
      // Delete ONLY the authenticated employee's records
      const userReqs = allReqs.filter((r) => _isOwner(r, user));
      const userReqIds = new Set(userReqs.map((r) => String(r.id)));
      markRequestIdsDeleted(Array.from(userReqIds));

      const remainingReqs = allReqs.filter((r) => !userReqIds.has(String(r.id)));
      _saveAllSystemRequests(remainingReqs);

      const userBookings = allBookings.filter((b) => userReqIds.has(String(b.travelRequest?.id)) || _isOwner(b, user));
      markBookingIdsDeleted(userBookings.map((b) => b.id));
      const remainingBookings = allBookings.filter((b) => !userReqIds.has(String(b.travelRequest?.id)) && !_isOwner(b, user));
      _saveAllSystemBookings(remainingBookings);
    } else {
      // Admin / Approver wipe
      markRequestIdsDeleted(allReqs.map((r) => r.id));
      markBookingIdsDeleted(allBookings.map((b) => b.id));
      _saveAllSystemRequests([]);
      _saveAllSystemBookings([]);
    }

    try {
      await apiRequest("/api/travel-requests/all", { method: "DELETE", ignoreAuthFailure: true, timeoutMs: 1000 });
    } catch {
      // Offline fallback
    }
    return { success: true };
  },

  deletePastOrCompleted: async (options?: { includeApproved?: boolean; includeBooked?: boolean }) => {
    const user = getStoredUser<AuthUser>();
    const allReqs = _getAllSystemRequests();
    const todayStr = new Date().toISOString().split("T")[0];

    const toDelete: any[] = [];
    const toKeep: any[] = [];

    for (const r of allReqs) {
      // If employee, only evaluate their own records
      if (user && user.role === "EMPLOYEE" && !_isOwner(r, user)) {
        toKeep.push(r);
        continue;
      }

      const status = String(r.status || "").toUpperCase();
      const returnDate = r.returnDate ? String(r.returnDate).split("T")[0] : null;
      const travelDate = r.travelDate ? String(r.travelDate).split("T")[0] : null;
      const isPastDate = (returnDate && returnDate < todayStr) || (!returnDate && travelDate && travelDate < todayStr);

      if (options?.includeApproved) {
        if (status !== "PENDING") {
          toDelete.push(r);
          continue;
        }
      }

      if (status === "COMPLETED" || status === "CANCELLED" || status === "REJECTED" || isPastDate) {
        toDelete.push(r);
      } else if (options?.includeBooked && status === "BOOKED") {
        toDelete.push(r);
      } else {
        toKeep.push(r);
      }
    }

    markRequestIdsDeleted(toDelete.map((r) => r.id));
    _saveAllSystemRequests(toKeep);

    const keptIdSet = new Set(toKeep.map((r) => String(r.id)));
    const allBookings = _getAllSystemBookings();
    const bookingsToKeep = allBookings.filter((b) => keptIdSet.has(String(b.travelRequest?.id)));
    const bookingsToDelete = allBookings.filter((b) => !keptIdSet.has(String(b.travelRequest?.id)));
    markBookingIdsDeleted(bookingsToDelete.map((b) => b.id));
    _saveAllSystemBookings(bookingsToKeep);

    return { success: true, count: toDelete.length };
  },

  clearDecisionHistory: async () => {
    return travelRequestsApi.deletePastOrCompleted({ includeApproved: true });
  },

  resetDemo: async () => {
    // Clean empty slate for all users
    markRequestIdsDeleted(_getAllSystemRequests().map((r) => r.id));
    markBookingIdsDeleted(_getAllSystemBookings().map((b) => b.id));
    _saveAllSystemRequests([]);
    _saveAllSystemBookings([]);
    _saveAllSystemNotifications([]);
    return { success: true };
  },
};

export const bookingsApi = {
  list: async () => {
    const user = getStoredUser<AuthUser>();
    const deletedBookingIds = getDeletedBookingIds();
    const deletedReqIds = getDeletedRequestIds();

    let records: any[] = [];
    try {
      const data = await apiRequest<any[]>("/api/bookings", { ignoreAuthFailure: true, timeoutMs: 1200 });
      if (Array.isArray(data)) {
        records = data.filter(
          (b) => !deletedBookingIds.has(String(b.id)) && !deletedReqIds.has(String(b.travelRequest?.id))
        );
      } else {
        records = _getAllSystemBookings();
      }
    } catch {
      records = _getAllSystemBookings();
    }

    if (user && user.role === "EMPLOYEE") {
      return records.filter((b) => _isOwner(b, user) || _isOwner(b.travelRequest, user));
    }
    // APPROVER, TRAVEL_DESK, and ADMIN see all bookings
    return records;
  },

  byTravelRequest: async (id: string) => {
    const user = getStoredUser<AuthUser>();
    const deletedReqIds = getDeletedRequestIds();
    const deletedBookingIds = getDeletedBookingIds();
    if (deletedReqIds.has(String(id))) return null;

    let booking: any = null;
    try {
      const data = await apiRequest(`/api/bookings/travel-request/${id}`, { ignoreAuthFailure: true, timeoutMs: 1200 });
      if (data && (!Array.isArray(data) || data.length > 0)) {
        const arr = Array.isArray(data) ? data : [data];
        const valid = arr.filter((b) => !deletedBookingIds.has(String(b.id)) && !deletedReqIds.has(String(b.travelRequest?.id)));
        booking = Array.isArray(data) ? valid : valid[0] || null;
      } else {
        booking = _getAllSystemBookings().filter((b) => String(b.travelRequest?.id) === String(id));
      }
    } catch {
      booking = _getAllSystemBookings().filter((b) => String(b.travelRequest?.id) === String(id));
    }

    const items = Array.isArray(booking) ? booking : booking ? [booking] : [];
    if (user && user.role === "EMPLOYEE") {
      const userItems = items.filter((b: any) => _isOwner(b, user) || _isOwner(b.travelRequest, user));
      return Array.isArray(booking) ? userItems : userItems[0] || null;
    }
    return booking;
  },

  create: async (payload: any) => {
    const reqId = payload.travelRequest?.id;
    const req = _getAllSystemRequests().find((r) => String(r.id) === String(reqId)) || { id: reqId };

    const newBooking = {
      id: `BK-${Math.floor(Math.random() * 900 + 100)}`,
      ...payload,
      userId: req.userId,
      employeeId: req.employeeId,
      travelRequest: req,
      bookedAt: new Date().toISOString(),
      cancelled: false,
    };

    const currentBookings = _getAllSystemBookings();
    _saveAllSystemBookings([newBooking, ...currentBookings]);
    broadcastCloudSync("CREATE_BOOKING", newBooking);

    // Update travel request status to BOOKED
    const currentReqs = _getAllSystemRequests();
    const updatedReqs = currentReqs.map((r) =>
      String(r.id) === String(reqId) ? { ...r, status: "BOOKED" } : r
    );
    _saveAllSystemRequests(updatedReqs);

    // Notify employee of confirmed ticket
    const notifs = _getAllSystemNotifications();
    const newNotif = {
      id: `NOTIF-${Date.now()}`,
      recipientId: req.userId,
      recipientEmployeeId: req.employeeId,
      title: "Trip booked & ticket issued",
      message: `Your booking ${newBooking.bookingReference || ""} (${newBooking.provider || "Carrier"}) is confirmed.`,
      type: "BOOKING",
      createdAt: new Date().toISOString(),
      read: false,
    };
    _saveAllSystemNotifications([newNotif, ...notifs]);

    try {
      await apiRequest("/api/bookings", { method: "POST", body: JSON.stringify(payload), ignoreAuthFailure: true });
    } catch {
      // Backend offline fallback handled by system storage
    }
    return newBooking;
  },

  update: async (id: string, payload: any) => {
    const current = _getAllSystemBookings();
    const updated = current.map((b) => (String(b.id) === String(id) ? { ...b, ...payload } : b));
    _saveAllSystemBookings(updated);
    broadcastCloudSync("UPDATE_BOOKING", { id, ...payload });
    try {
      await apiRequest(`/api/bookings/${id}`, { method: "PUT", body: JSON.stringify(payload), ignoreAuthFailure: true });
    } catch {
      // Backend offline fallback
    }
    return { id, ...payload };
  },

  cancel: async (id: string, reason: string, cancellationCharge = 0) => {
    const current = _getAllSystemBookings();
    let cancelledBooking: any = null;
    const updated = current.map((b) => {
      if (String(b.id) === String(id)) {
        cancelledBooking = { ...b, cancelled: true, cancellationReason: reason, cancellationCharge };
        return cancelledBooking;
      }
      return b;
    });
    _saveAllSystemBookings(updated);

    if (cancelledBooking) {
      const notifs = _getAllSystemNotifications();
      const newNotif = {
        id: `NOTIF-${Date.now()}`,
        recipientId: cancelledBooking.userId,
        recipientEmployeeId: cancelledBooking.employeeId,
        title: "Booking cancelled",
        message: `Your booking ${cancelledBooking.bookingReference || ""} was cancelled. Reason: ${reason}.`,
        type: "CANCELLATION",
        createdAt: new Date().toISOString(),
        read: false,
      };
      _saveAllSystemNotifications([newNotif, ...notifs]);
    }

    try {
      await apiRequest(`/api/bookings/${id}/cancel?reason=${encodeURIComponent(reason)}&cancellationCharge=${cancellationCharge}`, { method: "PUT", ignoreAuthFailure: true });
    } catch {
      // Backend offline fallback
    }
    return { id, cancelled: true, cancellationReason: reason, cancellationCharge };
  },

  delete: async (id: string) => {
    const strId = String(id);
    markBookingIdsDeleted([strId]);
    const current = _getAllSystemBookings();
    _saveAllSystemBookings(current.filter((b) => String(b.id) !== strId));
    try {
      await apiRequest(`/api/bookings/${strId}`, { method: "DELETE", ignoreAuthFailure: true, timeoutMs: 1000 });
    } catch {
      // Backend offline fallback
    }
    return { success: true, id: strId };
  },

  deleteAll: async () => {
    const currentBookings = _getAllSystemBookings();
    markBookingIdsDeleted(currentBookings.map((b) => b.id));
    _saveAllSystemBookings([]);
    return { success: true };
  },
};

export const notificationsApi = {
  list: async () => {
    const user = getStoredUser<AuthUser>();
    try {
      const data = await apiRequest<any[]>("/api/notifications");
      if (Array.isArray(data)) return data;
    } catch {
      // Fall through to system notifications
    }

    if (!user) return [];
    const allNotifs = _getAllSystemNotifications();
    return allNotifs.filter((n) => {
      if (n.recipientId && String(n.recipientId) === String(user.id)) return true;
      if (n.recipientEmployeeId && String(n.recipientEmployeeId).toLowerCase() === String(user.employeeId || "").toLowerCase()) return true;
      if (n.targetRole && n.targetRole === user.role) return true;
      return false;
    });
  },

  unreadCount: async () => {
    const list = await notificationsApi.list();
    return list.filter((n: any) => !n.read).length;
  },

  markRead: async (id: string | number) => {
    const strId = String(id);
    const allNotifs = _getAllSystemNotifications();
    const updated = allNotifs.map((n) => (String(n.id) === strId ? { ...n, read: true } : n));
    _saveAllSystemNotifications(updated);
    try {
      await apiRequest(`/api/notifications/${id}/read`, { method: "PATCH" });
    } catch {
      // Offline fallback
    }
    return { id, read: true };
  },

  markAllRead: async () => {
    const user = getStoredUser<AuthUser>();
    if (!user) return { success: true };
    const allNotifs = _getAllSystemNotifications();
    const updated = allNotifs.map((n) => {
      const match =
        (n.recipientId && String(n.recipientId) === String(user.id)) ||
        (n.recipientEmployeeId && String(n.recipientEmployeeId).toLowerCase() === String(user.employeeId || "").toLowerCase()) ||
        (n.targetRole && n.targetRole === user.role);
      return match ? { ...n, read: true } : n;
    });
    _saveAllSystemNotifications(updated);
    try {
      await apiRequest("/api/notifications/read-all", { method: "PATCH" });
    } catch {
      // Offline fallback
    }
    return { success: true };
  },
};

export const employeesApi = {
  list: async () => {
    try { return await apiRequest("/api/employees"); }
    catch { return DEMO_EMPLOYEES; }
  },
  getByEmployeeId: async (id: string) => {
    try { return await apiRequest(`/api/employees/employee-id/${encodeURIComponent(id)}`); }
    catch { return DEMO_EMPLOYEES.find(e => e.employeeId === id) || DEMO_EMPLOYEES[0]; }
  },
};

export const dashboardApi = {
  summary: async () => {
    try {
      return await apiRequest("/api/dashboard/summary");
    } catch {
      // Calculate dynamically for the authenticated user
      const user = getStoredUser<AuthUser>();
      const reqs = await travelRequestsApi.list();
      const bks = await bookingsApi.list();

      const pending = reqs.filter((r) => r.status === "PENDING").length;
      const approved = reqs.filter((r) => r.status === "APPROVED").length;
      const booked = reqs.filter((r) => r.status === "BOOKED").length;
      const completed = reqs.filter((r) => r.status === "COMPLETED").length;

      return {
        totalTravelRequests: reqs.length,
        pendingRequests: pending,
        approvedRequests: approved,
        bookedRequests: booked,
        completedRequests: completed,
        totalBookings: bks.length,
        totalEmployees: user?.role === "EMPLOYEE" ? 1 : REGISTERED_EMPLOYEES.length,
      };
    }
  },
};

export const bulkBookingsApi = {
  list: async (): Promise<BulkBookingRecord[]> => {
    const deletedIds = getDeletedBulkBookingIds();
    if (API_BASE_URL) {
      try {
        const data = await apiRequest<BulkBookingRecord[]>("/api/bulk-bookings");
        if (Array.isArray(data)) {
          return data.filter((b) => b && b.id && !deletedIds.has(String(b.id)));
        }
      } catch {
        // Fallback to local storage if backend offline
      }
    }
    return _getAllSystemBulkBookings();
  },
  getById: async (id: string): Promise<BulkBookingRecord | null> => {
    if (API_BASE_URL) {
      try {
        const data = await apiRequest<BulkBookingRecord>(`/api/bulk-bookings/${id}`);
        if (data) return data;
      } catch {
        // Fallback to local storage
      }
    }
    const list = _getAllSystemBulkBookings();
    return list.find((b) => String(b.id) === String(id)) || null;
  },
  create: async (payload: Partial<BulkBookingRecord>): Promise<BulkBookingRecord> => {
    const headcount = Math.max(1, Number(payload.headcount) || 1);
    const totalCost = Number(payload.totalCost) || 0;
    const costPerPerson = headcount > 0 ? Math.round(totalCost / headcount) : totalCost;

    if (API_BASE_URL) {
      try {
        const created = await apiRequest<BulkBookingRecord>("/api/bulk-bookings", {
          method: "POST",
          body: JSON.stringify({
            ...payload,
            headcount,
            totalCost,
            costPerPerson,
          }),
        });
        if (created) {
          const current = _getAllSystemBulkBookings();
          _saveAllSystemBulkBookings([created, ...current]);
          return created;
        }
      } catch (err: any) {
        if (!err?.message?.includes("not reached") && !err?.message?.includes("failed to fetch")) {
          throw err;
        }
      }
    }

    const newRecord: BulkBookingRecord = {
      id: `GRP-BK-${Math.floor(Math.random() * 900 + 100)}`,
      title: payload.title || "Group Corporate Travel Movement",
      destination: payload.destination || "Destination",
      fromLocation: payload.fromLocation || "HQ",
      travelDates: payload.travelDates || "Upcoming",
      departureDate: payload.departureDate,
      returnDate: payload.returnDate,
      travelMode: (payload.travelMode as any) || "FLIGHT",
      provider: payload.provider || "Corporate Group Desk",
      groupReference: payload.groupReference || `GRP-${Date.now().toString().slice(-6)}`,
      headcount,
      totalCost,
      costPerPerson,
      savings: payload.savings ? Number(payload.savings) : 0,
      project: payload.project || "General Operations",
      departments: payload.departments || ["Corporate Operations"],
      passengerManifestSummary: payload.passengerManifestSummary || `${headcount} Corporate Travelers`,
      passengerList: payload.passengerList || [],
      itineraryNotes: payload.itineraryNotes,
      coordinatorName: payload.coordinatorName || "Central Travel Desk",
      loggedAt: new Date().toISOString(),
      status: "CONFIRMED",
      approverStatus: "PENDING_REVIEW",
      attachmentName: payload.attachmentName,
      attachmentData: payload.attachmentData,
    };

    const current = _getAllSystemBulkBookings();
    _saveAllSystemBulkBookings([newRecord, ...current]);
    broadcastCloudSync("CREATE_BULK", newRecord);

    // Send notification to Approver
    const notifs = _getAllSystemNotifications();
    const newNotif = {
      id: `NOTIF-${Date.now()}`,
      targetRole: "APPROVER",
      title: "New Bulk Travel Booking Logged",
      message: `Travel Desk logged mass travel for ${headcount} people to ${newRecord.destination} (${newRecord.groupReference}). Total cost: ₹${totalCost.toLocaleString("en-IN")}.`,
      type: "BOOKING",
      createdAt: new Date().toISOString(),
      read: false,
    };
    _saveAllSystemNotifications([newNotif, ...notifs]);

    return newRecord;
  },
  update: async (id: string, payload: Partial<BulkBookingRecord>): Promise<BulkBookingRecord> => {
    if (API_BASE_URL) {
      try {
        const updated = await apiRequest<BulkBookingRecord>(`/api/bulk-bookings/${id}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
        if (updated) {
          const current = _getAllSystemBulkBookings();
          _saveAllSystemBulkBookings(current.map((b) => (String(b.id) === String(id) ? updated : b)));
          return updated;
        }
      } catch (err: any) {
        if (!err?.message?.includes("not reached") && !err?.message?.includes("failed to fetch")) {
          throw err;
        }
      }
    }

    const current = _getAllSystemBulkBookings();
    const existing = current.find((b) => String(b.id) === String(id));
    if (!existing) throw new Error("Bulk booking record not found.");
    const headcount = payload.headcount !== undefined ? Number(payload.headcount) : existing.headcount;
    const totalCost = payload.totalCost !== undefined ? Number(payload.totalCost) : existing.totalCost;
    const costPerPerson = headcount > 0 ? Math.round(totalCost / headcount) : totalCost;
    const updatedRecord: BulkBookingRecord = { ...existing, ...payload, headcount, totalCost, costPerPerson };
    const updated = current.map((b) => (String(b.id) === String(id) ? updatedRecord : b));
    _saveAllSystemBulkBookings(updated);
    broadcastCloudSync("UPDATE_BULK", updatedRecord);
    return updatedRecord;
  },
  reviewByApprover: async (id: string, status: "APPROVED" | "NOTED", comment: string): Promise<BulkBookingRecord> => {
    if (API_BASE_URL) {
      try {
        const reviewed = await apiRequest<BulkBookingRecord>(`/api/bulk-bookings/${id}/review`, {
          method: "PATCH",
          body: JSON.stringify({ status, comment }),
        });
        if (reviewed) {
          const current = _getAllSystemBulkBookings();
          _saveAllSystemBulkBookings(current.map((b) => (String(b.id) === String(id) ? reviewed : b)));
          return reviewed;
        }
      } catch (err: any) {
        if (!err?.message?.includes("not reached") && !err?.message?.includes("failed to fetch")) {
          throw err;
        }
      }
    }

    const current = _getAllSystemBulkBookings();
    const existing = current.find((b) => String(b.id) === String(id));
    if (!existing) throw new Error("Bulk booking record not found.");
    const updatedRecord: BulkBookingRecord = {
      ...existing,
      approverStatus: status,
      approverComment: comment,
      approverReviewedAt: new Date().toISOString(),
    };
    const updated = current.map((b) => (String(b.id) === String(id) ? updatedRecord : b));
    _saveAllSystemBulkBookings(updated);
    broadcastCloudSync("REVIEW_BULK", updatedRecord);

    const notifs = _getAllSystemNotifications();
    const newNotif = {
      id: `NOTIF-${Date.now()}`,
      targetRole: "TRAVEL_DESK",
      title: `Bulk Booking ${status === "APPROVED" ? "Approved" : "Reviewed"}`,
      message: `Approver signed off on bulk movement ${id} (${updatedRecord.title}). Decision: ${status}. Comment: "${comment || "Acknowledged"}".`,
      type: "APPROVAL",
      createdAt: new Date().toISOString(),
      read: false,
    };
    _saveAllSystemNotifications([newNotif, ...notifs]);

    return updatedRecord;
  },
  delete: async (id: string) => {
    const strId = String(id);
    markBulkBookingIdsDeleted([strId]);
    const current = _getAllSystemBulkBookings();
    _saveAllSystemBulkBookings(current.filter((b) => String(b.id) !== strId));

    if (API_BASE_URL) {
      try {
        await apiRequest(`/api/bulk-bookings/${id}`, { method: "DELETE" });
      } catch {
        // Continue with local tombstone
      }
    } else {
      broadcastCloudSync("DELETE_BULK", { id: strId });
    }
    return { success: true };
  },
};

export const healthApi = {
  check: async () => {
    try { return await apiRequest<string>("/api/health"); }
    catch { return "Trapple Frontend Client Ready (Backend demo mode)"; }
  },
};

export async function uploadDocument(file: File): Promise<{
  id: string;
  originalFileName: string;
  contentType: string;
  sizeBytes: number;
  downloadUrl: string;
}> {
  if (API_BASE_URL) {
    const formData = new FormData();
    formData.append("file", file);
    const token = typeof window !== "undefined" ? (window.localStorage.getItem("trapple_token") || window.localStorage.getItem("travora_token")) : null;
    const res = await fetch(`${API_BASE_URL}/api/documents/upload`, {
      method: "POST",
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: formData,
    });
    if (!res.ok) {
      const errText = await res.text().catch(() => "");
      throw new Error(errText || "Failed to upload document to server.");
    }
    return await res.json();
  }

  // Standalone offline fallback
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      resolve({
        id: "local-" + Date.now(),
        originalFileName: file.name,
        contentType: file.type || "application/octet-stream",
        sizeBytes: file.size,
        downloadUrl: reader.result as string,
      });
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

export const API_CONFIG = { baseUrl: API_BASE_URL };

export type BackendExpense = {
  id: number;
  companyId?: number;
  companyName?: string;
  travelRequestId?: number | null;
  employeeId: number;
  employeeName: string;
  employeeCode: string;
  category: "FLIGHT" | "ACCOMMODATION" | "MEALS" | "TAXI" | "FUEL" | "OTHER";
  amount: number;
  currency: string;
  expenseDate: string;
  merchant: string;
  description?: string;
  receiptUrl?: string;
  receiptName?: string;
  status: "DRAFT" | "SUBMITTED" | "APPROVED" | "REIMBURSED" | "REJECTED";
  approverName?: string;
  approvalComment?: string;
  approvedAt?: string;
  createdAt: string;
};

export const expenseApi = {
  getAll: async (status?: string): Promise<BackendExpense[]> => {
    const query = status && status !== "ALL" ? `?status=${status}` : "";
    return await apiRequest<BackendExpense[]>(`/api/expenses${query}`);
  },
  getById: async (id: number | string): Promise<BackendExpense> => {
    return await apiRequest<BackendExpense>(`/api/expenses/${id}`);
  },
  create: async (data: {
    travelRequestId?: number | null;
    category: string;
    amount: number;
    currency?: string;
    expenseDate: string;
    merchant: string;
    description?: string;
    receiptUrl?: string;
    receiptName?: string;
  }): Promise<BackendExpense> => {
    return await apiRequest<BackendExpense>("/api/expenses", {
      method: "POST",
      body: JSON.stringify(data),
    });
  },
  approve: async (id: number | string, comment?: string): Promise<BackendExpense> => {
    return await apiRequest<BackendExpense>(`/api/expenses/${id}/approve`, {
      method: "PATCH",
      body: JSON.stringify({ comment: comment || "" }),
    });
  },
  reject: async (id: number | string, comment?: string): Promise<BackendExpense> => {
    return await apiRequest<BackendExpense>(`/api/expenses/${id}/reject`, {
      method: "PATCH",
      body: JSON.stringify({ comment: comment || "" }),
    });
  },
  reimburse: async (id: number | string): Promise<BackendExpense> => {
    return await apiRequest<BackendExpense>(`/api/expenses/${id}/reimburse`, {
      method: "PATCH",
    });
  },
  delete: async (id: number | string): Promise<void> => {
    await apiRequest(`/api/expenses/${id}`, { method: "DELETE" });
  },
};

export const companyApi = {
  getCurrent: async (): Promise<{ id: number; name: string; code: string; domain?: string; active: boolean }> => {
    return await apiRequest("/api/companies/current");
  },
  register: async (data: {
    name: string;
    code: string;
    domain?: string;
    adminUsername: string;
    adminPassword: string;
    adminEmail?: string;
  }): Promise<{ id: number; name: string; code: string; domain?: string; active: boolean }> => {
    return await apiRequest("/api/companies/register", {
      method: "POST",
      body: JSON.stringify(data),
      skipAuth: true,
    });
  },
};



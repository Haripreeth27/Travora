# TRAVORA BACKEND & HOSTING ARCHITECTURE REVIEW
**Production Hosting Strategy & Cost Model for 1,000 Employees**

- **Project:** Travora Corporate Travel Management
- **Repository:** `sumiksha-ve/Travora` (`main` branch)
- **Target Scale:** ~1,000 Registered Corporate Employees
- **Deliverable File:** `TRAVORA_BACKEND_HOSTING_REVIEW.md`
- **Status:** **ARCHITECTURAL DECISION & HOSTING COST ESTIMATE**

---

## Executive Summary

A forensic code-level inspection of `server/index.ts`, `backend/`, `client/src/lib/api.ts`, and deployment configurations reveals that **Travora does not possess two co-dependent backend microservices**.

Instead, the repository contains:
1. **A full Java / Spring Boot business application (`backend/`)** that implements all enterprise business logic, authentication, role authorization, and database persistence.
2. **A trivial 34-line Express static file server (`server/index.ts`)** that serves the compiled React HTML/JS bundle if deployed on a Node.js runtime.

### Key Architectural Determinations
- **Does the Express server need to run in production?** **NO.** The frontend is already deployed and served on Vercel's global Edge CDN (`https://travora-ashy.vercel.app`). Running the Express server in production is 100% redundant and adds needless hosting cost.
- **Can either backend be removed?**
  - **The Express server can be removed from production immediately** with zero impact on functionality.
  - **The Spring Boot backend cannot be removed**; it is the sole component capable of providing secure authentication, multi-user role enforcement, and PostgreSQL database persistence for 1,000 employees.
- **Simplest Reliable Production Architecture:** 
  - **Frontend:** Hosted on **Vercel** (Edge CDN).
  - **Backend API:** Containerized **Spring Boot** deployed on a managed container service (e.g., AWS App Runner, Render, or Google Cloud Run).
  - **Database:** **Managed PostgreSQL** (e.g., AWS RDS, Neon, or Supabase).
- **Estimated Production Hosting Cost for 1,000 Employees:** **$65 to $130 / month** for standard production, or **$180 to $310 / month** for high-availability enterprise tier.

---

## 1. What Functionality is Implemented in the Spring Boot Backend?

The `backend/` directory contains a modular, enterprise-grade Java 21 / Spring Boot 4.1.1 service using Spring Data JPA, Spring Security, and JJWT.

### 1.1 Complete Code-Level Endpoint Inventory

| Controller | HTTP Method & Path | Security / Role Restriction | Business Functionality |
|---|---|---|---|
| **`HealthController`** | `GET /api/health` | Public (`permitAll`) | Service liveness health check probe. |
| **`AuthController`** | `POST /api/auth/login` | Public (`permitAll`) | Authenticates username & password via BCrypt, validates active status, issues HMAC-SHA256 JWT containing user ID, username, role, and employee ID. |
| | `POST /api/auth/register` | Public (`permitAll`) | Registers new User entities, encodes passwords with BCrypt, and saves to database. |
| | `PUT /api/auth/users/{userId}/employee` | `hasRole('ADMIN')` | Associates an existing User login account with an Employee profile record. |
| | `PUT /api/auth/reset-password/{username}` | `hasRole('ADMIN')` | Administrative password reset. |
| **`TravelRequestController`** | `GET /api/travel-requests` | Authenticated | Lists requests. If `EMPLOYEE`: strictly filtered to user's own requests. If `ADMIN`, `APPROVER`, or `TRAVEL_DESK`: returns organization-wide queue. |
| | `GET /api/travel-requests/{id}` | Authenticated | Fetches single request. Enforces ownership check if user is `EMPLOYEE`. |
| | `POST /api/travel-requests` | Authenticated | Creates a new travel request. Forces employee identity to authenticated user if role is `EMPLOYEE`. Initial status is set to `PENDING`. Dispatches notification. |
| | `PATCH /api/travel-requests/{id}/approve` | `hasAnyRole('ADMIN', 'APPROVER')` | Transitions request status from `PENDING` to `APPROVED`. Records approver username, timestamp, and audit comment. |
| | `PATCH /api/travel-requests/{id}/reject` | `hasAnyRole('ADMIN', 'APPROVER')` | Transitions request status from `PENDING` to `REJECTED` with audit comment. |
| | `DELETE /api/travel-requests/{id}` | Authenticated | Deletes request record. Enforces ownership check for employees. |
| | `DELETE /api/travel-requests/all` | Authenticated | Bulk deletes requests (own requests for employees, entire table for non-employees). |
| **`BookingController`** | `GET /api/bookings` | `hasAnyRole('ADMIN', 'APPROVER', 'TRAVEL_DESK')` | Lists all ticketing and booking records across all employees. |
| | `GET /api/bookings/{id}` | `hasAnyRole('ADMIN', 'APPROVER', 'TRAVEL_DESK')` | Fetches single booking record by ID. |
| | `GET /api/bookings/travel-request/{id}` | Authenticated (All Roles) | Fetches tickets/bookings linked to a specific travel request ID. |
| | `GET /api/bookings/cancelled/{cancelled}` | `hasAnyRole('ADMIN', 'APPROVER', 'TRAVEL_DESK')` | Filters bookings by cancellation status. |
| | `POST /api/bookings` | `hasAnyRole('ADMIN', 'TRAVEL_DESK')` | Issues booking record linked to an approved travel request (PNR, provider, cost, savings). Updates request status to `BOOKED`. |
| | `PUT /api/bookings/{id}` | `hasAnyRole('ADMIN', 'TRAVEL_DESK')` | Updates ticket details, provider, PNR reference, and notes. |
| | `PUT /api/bookings/{id}/cancel` | `hasAnyRole('ADMIN', 'TRAVEL_DESK')` | Cancels booking, records cancellation charge and reason, transitions travel request status to `CANCELLED`. |
| | `DELETE /api/bookings/{id}` | `hasRole('ADMIN')` | Deletes booking record. |
| **`EmployeeController`** | `GET /api/employees` | Authenticated | Lists all registered employee master profiles. |
| | `GET /api/employees/{id}` | Authenticated | Fetches employee by primary key ID. |
| | `GET /api/employees/employee-id/{empId}`| Authenticated | Fetches employee by corporate employee number (e.g., `AGR0001`). |
| | `POST /api/employees` | Authenticated | Creates a new employee master record. |
| | `PUT /api/employees/{id}` | Authenticated | Updates employee designation, department, location, or name. |
| | `DELETE /api/employees/{id}` | Authenticated | Deletes employee record from directory. |
| **`NotificationController`**| `GET /api/notifications` | Authenticated | Fetches all operational notifications targeted to the logged-in user. |
| | `GET /api/notifications/unread-count` | Authenticated | Counts unread notifications. |
| | `PATCH /api/notifications/{id}/read` | Authenticated | Marks a single notification as read. |
| | `PATCH /api/notifications/read-all` | Authenticated | Marks all user notifications as read. |
| **`DashboardController`** | `GET /api/dashboard/summary` | Authenticated | Computes aggregated corporate metrics (total requests, pending, approved, rejected, and total bookings). |

### 1.2 Feature Gap: Bulk & Group Movements
The Spring Boot backend **does not implement Bulk / Group Bookings** (`BulkBookingRecord`, master PNR, passenger roster manifest). That feature was developed on the frontend and currently lives in browser `localStorage`. To achieve 100% feature parity in production, Spring Boot must be extended with `BulkBooking` entities before migration.

---

## 2. What Functionality is Implemented in `server/index.ts`?

Inspection of `server/index.ts` (34 total lines) reveals the following complete source:

```typescript
import express from "express";
import { createServer } from "http";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function startServer() {
  const app = express();
  const server = createServer(app);

  const staticPath =
    process.env.NODE_ENV === "production"
      ? path.resolve(__dirname, "public")
      : path.resolve(__dirname, "..", "dist", "public");

  app.use(express.static(staticPath));

  app.get("*", (_req, res) => {
    res.sendFile(path.join(staticPath, "index.html"));
  });

  const port = process.env.PORT || 3000;
  server.listen(port, () => {
    console.log(`Server running on http://localhost:${port}/`);
  });
}

startServer().catch(console.error);
```

### Forensic Tracing Findings:
- **API Endpoints:** **0** (No API routes exist).
- **Database Connections:** **0** (No database drivers, no ORM, no queries).
- **Authentication / Session Handling:** **0** (No tokens, no cookies, no hashing).
- **Business Logic:** **0**.
- **Sole Purpose:** It is an **Express static file server**. Its only function is to serve the pre-compiled Vite frontend (`dist/public`) and redirect all unmatched paths to `index.html` to support client-side SPA routing.

---

## 3. Does the Express Server Need to Run in Production?

### Direct Answer: **NO.**

### Detailed Technical Rationale:
1. **Redundant to Vercel:**
   The frontend is already hosted and running live on Vercel at `https://travora-ashy.vercel.app`. Vercel's Edge CDN serves the static HTML, JavaScript, CSS, and font assets with global edge caching, HTTP/2, automated TLS certificates, and DDoS mitigation.
2. **Vercel Ignores `server/index.ts`:**
   In `vercel.json`, the routing configuration directly rewrites all incoming traffic to `index.html`:
   ```json
   {
     "rewrites": [
       { "source": "/(.*)", "destination": "/index.html" }
     ]
   }
   ```
   Vercel never launches `dist/index.js` or `server/index.ts`.
3. **Waste of Infrastructure & Cost:**
   Running a long-running Node.js virtual machine or container just to serve static files that Vercel already serves for free (or for $20/mo on Pro) adds unnecessary compute costs, memory overhead (~100MB RAM), and an extra process to monitor.

---

## 4. Can Either Backend Be Removed Without Breaking Functionality?

| Backend Candidate | Can It Be Removed? | Impact on Functionality | Verdict |
|---|:---:|---|---|
| **Express Server (`server/index.ts`)** | **YES** | **Zero Impact.** Vercel already serves the static assets. Removing it from production eliminates unnecessary build steps and dependencies without altering a single user feature. | **ELIMINATE FROM PRODUCTION** |
| **Spring Boot Backend (`backend/`)** | **NO** | **Critical Impact.** Without Spring Boot, the application has no backend API, no database connectivity, no password verification, and no role security. The app would permanently remain a client-side prototype. | **DEPLOY AS CORE API BACKEND** |

### Code Verification: Frontend API Tracing
Every business API call in `client/src/lib/api.ts` targets the Spring Boot endpoints:
- `client/src/lib/api.ts:532` -> `POST /api/auth/login` (Spring Boot `AuthController`)
- `client/src/lib/api.ts:1054` -> `GET /api/travel-requests` (Spring Boot `TravelRequestController`)
- `client/src/lib/api.ts:1197` -> `PATCH /api/travel-requests/{id}/approve` (Spring Boot `TravelRequestController`)
- `client/src/lib/api.ts:1472` -> `POST /api/bookings` (Spring Boot `BookingController`)
- `client/src/lib/api.ts:1552` -> `GET /api/notifications` (Spring Boot `NotificationController`)

The frontend was specifically written to connect to the Spring Boot REST API. The Express server was never designed to handle these calls.

---

## 5. What External Services Does Each Backend Depend On?

### 5.1 Express Server (`server/index.ts`) Dependencies
- **External Network Services:** **None** (Zero).
- **Databases:** **None** (Zero).
- **Filesystem:** Depends only on local disk access to read files in `dist/public/`.

### 5.2 Spring Boot Backend (`backend/`) Dependencies
- **Primary Database Dependency:**
  - **PostgreSQL 14+** (Connected via JDBC: `spring.datasource.url=${TRAVORA_DB_URL:...}`).
  - Relies on PostgreSQL for relational integrity, sequences, transactions, and foreign keys.
- **External Third-Party APIs / Network Services:**
  - **Zero live third-party APIs currently integrated.**
  - There are no live connections to Amadeus, Sabre, IndiGo API, IRCTC, payment gateways (Stripe/Razorpay), or SMS providers. All airline and train designations are currently stored as relational strings.
- **Runtime Dependencies:**
  - Java 21 JDK / JRE.
  - Standard Spring Boot libraries: `jjwt-api`, `jjwt-impl`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-webmvc`, `postgresql`.

---

## 6. What is the Simplest Reliable Deployment Architecture?

The simplest, most reliable, and most cost-effective architecture for a 1,000-employee company decouples the static frontend from the stateful API.

```
┌─────────────────────────────────────────────────────────────┐
│                     1,000 Corporate Users                   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                ▼ (Static Assets & HTML)      ▼ (API: /api/* with Bearer JWT)
   ┌──────────────────────────────┐  ┌───────────────────────────────────┐
   │         Vercel CDN           │  │      Cloud Load Balancer / DNS    │
   │  (Hosted React SPA Bundle)   │  │   api.travora.yourcompany.com     │
   └──────────────────────────────┘  └─────────────────┬─────────────────┘
                                                       │
                                                       ▼ (Private HTTPS)
                                     ┌───────────────────────────────────┐
                                     │     Spring Boot Container         │
                                     │   (AWS App Runner / Render)       │
                                     │   1-2 Containers (1 vCPU, 2GB)    │
                                     └─────────────────┬─────────────────┘
                                                       │
                                                       ▼ (HikariCP Pool)
                                     ┌───────────────────────────────────┐
                                     │     Managed PostgreSQL 16         │
                                     │     (AWS RDS / Supabase / Neon)   │
                                     │     2 vCPU, 4GB RAM, 25GB SSD     │
                                     │     Automated Daily Backups       │
                                     └───────────────────────────────────┘
```

### Component Roles:
1. **Frontend Host (Vercel):**
   - Retain current Vercel deployment.
   - Configure environment variable: `VITE_API_BASE_URL=https://api.travora.yourcompany.com`.
   - Remove Express server from build pipeline.
2. **Backend API Host (Managed Container Service):**
   - Build `backend/Dockerfile` using Eclipse Temurin 21 JDK.
   - Deploy as a managed container on **AWS App Runner**, **Render**, or **Google Cloud Run**.
   - These services handle auto-restart, TLS certificates, health probes, and horizontal scaling automatically.
3. **Database (Managed PostgreSQL):**
   - Deploy on **AWS RDS PostgreSQL**, **Supabase**, or **Neon**.
   - Ensures automated nightly backups, point-in-time recovery (PITR), and zero database administration overhead.

---

## 7. Production Hosting Cost Estimates (1,000 Employees)

### 7.1 Realistic Workload Profile for 1,000 Employees
- **Active User Base:** 1,000 registered employees.
- **Concurrent Peak Load:** 60 to 120 simultaneous users (morning triage, approval deadlines).
- **Traffic Throughput:** ~10 to 30 HTTP requests / second during peak hours.
- **Database Size:** ~50MB to 500MB relational data in Year 1 (compact text/numeric records).
- **Attachment Storage:** ~10GB to 50GB PDF tickets/vouchers annually.

---

### 7.2 Itemized Monthly Cost Models

#### Option 1: Minimum Viable Production (PaaS - Simplest & Fastest Setup)
*Ideal for fast deployment with zero DevOps overhead using Render/Railway + Supabase + Vercel.*

| Component | Provider & Specification | Monthly Cost |
|---|---|:---:|
| **Frontend CDN** | **Vercel Pro** (Commercial team license, 1 seat) | $20.00 |
| **Backend API** | **Render Web Service** (Starter/Standard: 1 vCPU, 2GB RAM, persistent) | $25.00 |
| **Database** | **Supabase Pro / Neon** (Managed Postgres: 2 vCPU, 4GB RAM, 20GB storage, daily backups) | $25.00 |
| **Object Storage** | **Cloudflare R2 / AWS S3** (Ticket PDFs & attachments, ~20GB storage) | $1.00 |
| **Domain & SSL** | Corporate custom subdomain (`travel.company.com`) | Free (Existing) |
| **TOTAL** | | **~$71.00 / month** |

---

#### Option 2: Enterprise Cloud Architecture (AWS - High Availability & Compliance)
*Recommended for corporate governance, SOC2 compliance, private VPC isolation, and automatic failover.*

| Component | Provider & Specification | Monthly Cost |
|---|---|:---:|
| **Frontend CDN** | **Vercel Pro** or **AWS CloudFront + S3** (Unlimited scale edge CDN) | $20.00 |
| **Backend API** | **AWS App Runner** or **ECS Fargate** (2 Tasks: 1 vCPU, 2GB RAM each for High Availability) | $65.00 |
| **Database** | **AWS RDS PostgreSQL** (`db.t4g.medium`: 2 vCPU, 4GB RAM, Multi-AZ automated backups) | $78.00 |
| **Object Storage** | **AWS S3 Standard** (Private bucket for employee e-tickets & vouchers) | $3.00 |
| **Monitoring** | **AWS CloudWatch** + Sentry Error Tracking (Team tier) | $26.00 |
| **TOTAL** | | **~$192.00 / month** |

---

### 7.3 Cost Comparison Summary

| Architecture Tier | Monthly Cost | Annual Cost | High Availability? | Setup Time |
|---|:---:|:---:|:---:|:---:|
| **Current Prototype State** | **$0.00** | $0.00 | No (LocalStorage simulation) | Live now |
| **Option 1: Managed PaaS (Minimum)** | **$71.00** | ~$852.00 | Single instance | 1 – 2 Days |
| **Option 2: AWS Enterprise (Recommended)** | **$192.00** | ~$2,304.00 | Multi-AZ High Availability | 3 – 5 Days |

---

## 8. Summary of Recommendations

1. **Decommission `server/index.ts` from production:**
   Do not pay for or run a Node.js process to host the static files. Vercel already handles static delivery faster and cheaper.
2. **Deploy the Spring Boot backend (`backend/`):**
   Containerize the existing Java service and deploy it to a managed container service (AWS App Runner or Render).
3. **Connect a Managed PostgreSQL database:**
   Provision a cloud PostgreSQL instance and supply the connection string via `TRAVORA_DB_URL`.
4. **Bridge the Bulk Booking Feature Gap:**
   Add JPA entities for `BulkBooking` to the Spring Boot backend so the frontend group booking workstation persists data to PostgreSQL before switching off prototype mode.
5. **Set `VITE_API_BASE_URL` on Vercel:**
   Point the live Vercel frontend to the new backend API domain, establishing a secure, scalable production environment for all 1,000 employees.

import type { ReactNode } from "react";
import type { Role } from "../lib/api";

export type Journey = {
  id: string;
  destination: string;
  from: string;
  to: string;
  dates: string;
  tripType: string;
  project: string;
  status: "Pending approval" | "Approved" | "Booked" | "Completed" | "Cancelled" | "Rejected";
  booking?: string;
  passenger?: string;
  estimatedCost?: number;
  approverName?: string;
  approvalComment?: string;
  approvalDate?: string;
  reason?: string;
  employee?: {
    id?: number | string;
    name?: string;
    employeeId?: string;
    department?: string;
    designation?: string;
  };
  rawRequest?: Record<string, any>;
};

export type BackendRequest = Record<string, any>;

export type PageMeta = {
  eyebrow: string;
  title: string;
  description?: string;
};

export type NavItem = {
  label: string;
  href: string;
  icon: ReactNode;
  badge?: string | number;
};

export type Expense = {
  id: string;
  journeyId?: string;
  destination: string;
  category: "Flight" | "Hotel" | "Meals" | "Transport" | "Per Diem" | "Supplies" | "Other";
  amount: number;
  currency: string;
  date: string;
  merchant: string;
  status: "Submitted" | "Approved" | "Reimbursed" | "Rejected";
  receiptName?: string;
  notes?: string;
};

export type UserProfile = {
  id: string;
  name: string;
  email: string;
  role: Role;
  employeeId: string;
  department: string;
  designation: string;
  phone: string;
  officeLocation: string;
  emergencyContact: {
    name: string;
    relation: string;
    phone: string;
  };
  travelPreferences: {
    seatPreference: "Window" | "Aisle" | "Middle" | "No preference";
    mealPreference: "Vegetarian" | "Non-Vegetarian" | "Vegan" | "Gluten-Free" | "Standard";
    preferredAirlines: string[];
    hotelLoyalty?: string;
    frequentFlyerNo?: string;
  };
};

export type ActivityEvent = {
  id: string;
  type: "request" | "approval" | "booking" | "expense" | "system";
  actor: string;
  role: string;
  action: string;
  target: string;
  timestamp: string;
  badgeVariant?: "success" | "warning" | "info" | "neutral" | "danger";
};

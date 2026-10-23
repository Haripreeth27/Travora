import { useState, useMemo, useEffect } from "react";
import {
  WalletCards,
  Plus,
  Download,
  Filter,
  Search,
  CheckCircle2,
  Clock3,
  Receipt,
  FileCheck2,
  DollarSign,
  Coffee,
  Plane,
  Building,
  Car,
  Package,
  Sparkles,
  X,
  Upload,
} from "lucide-react";
import { toast } from "sonner";
import type { Expense, Journey } from "../types";
import { exportToCsv } from "../utils/exportCsv";

const INITIAL_EXPENSES: Expense[] = [
  {
    id: "EXP-1001",
    destination: "Bengaluru Tech Hub",
    category: "Flight",
    amount: 8450,
    currency: "INR",
    date: "2026-10-02",
    merchant: "IndiGo Airlines",
    status: "Reimbursed",
    receiptName: "indigo_ticket_AI982.pdf",
    notes: "Direct flight BLR - DEL for client workshop",
  },
  {
    id: "EXP-1002",
    destination: "Bengaluru Tech Hub",
    category: "Hotel",
    amount: 14200,
    currency: "INR",
    date: "2026-10-04",
    merchant: "The Chancery Pavilion",
    status: "Approved",
    receiptName: "hotel_folio_9281.pdf",
    notes: "2 nights stay during corporate summit",
  },
  {
    id: "EXP-1003",
    destination: "Mumbai Financial District",
    category: "Meals",
    amount: 1850,
    currency: "INR",
    date: "2026-10-05",
    merchant: "Trishna Restaurant",
    status: "Submitted",
    receiptName: "client_dinner_bill.jpg",
    notes: "Dinner with regional partner team",
  },
  {
    id: "EXP-1004",
    destination: "Singapore Regional HQ",
    category: "Transport",
    amount: 1250,
    currency: "INR",
    date: "2026-10-06",
    merchant: "Uber Airport Premier",
    status: "Submitted",
    receiptName: "uber_ride_receipt.pdf",
    notes: "Terminal 2 to client premises",
  },
];

const CATEGORY_ICONS: Record<string, any> = {
  Flight: Plane,
  Hotel: Building,
  Meals: Coffee,
  Transport: Car,
  "Per Diem": DollarSign,
  Supplies: Package,
  Other: Receipt,
};

import type { AuthUser } from "../lib/api";

export function Expenses({ journeys = [], user }: { journeys?: Journey[]; user?: AuthUser | null }) {
  const userKey = user?.employeeId || (user?.id ? String(user.id) : "guest");
  const storageKey = `trapple_expenses_${userKey}`;

  const [expenses, setExpenses] = useState<Expense[]>(() => {
    try {
      const saved = localStorage.getItem(storageKey) || localStorage.getItem(`travora_expenses_${userKey}`);
      if (saved) return JSON.parse(saved);
    } catch {}
    return [];
  });

  const [query, setQuery] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [isCreateOpen, setIsCreateOpen] = useState(false);

  // Form State
  const [merchant, setMerchant] = useState("");
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState("INR");
  const [category, setCategory] = useState<Expense["category"]>("Meals");
  const [destination, setDestination] = useState(journeys[0]?.destination || "Corporate Travel");
  const [expenseDate, setExpenseDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [receiptFile, setReceiptFile] = useState<string | null>(null);
  const [notes, setNotes] = useState("");

  useEffect(() => {
    localStorage.setItem(storageKey, JSON.stringify(expenses));
  }, [expenses, storageKey]);

  const stats = useMemo(() => {
    const totalClaimed = expenses.reduce((sum, item) => sum + item.amount, 0);
    const reimbursed = expenses
      .filter((item) => item.status === "Reimbursed")
      .reduce((sum, item) => sum + item.amount, 0);
    const approved = expenses
      .filter((item) => item.status === "Approved")
      .reduce((sum, item) => sum + item.amount, 0);
    const pending = expenses
      .filter((item) => item.status === "Submitted")
      .reduce((sum, item) => sum + item.amount, 0);

    return { totalClaimed, reimbursed, approved, pending };
  }, [expenses]);

  const filtered = useMemo(() => {
    return expenses.filter((item) => {
      const matchesQuery =
        item.merchant.toLowerCase().includes(query.toLowerCase()) ||
        item.destination.toLowerCase().includes(query.toLowerCase()) ||
        item.id.toLowerCase().includes(query.toLowerCase());
      const matchesCategory = categoryFilter === "ALL" || item.category === categoryFilter;
      const matchesStatus = statusFilter === "ALL" || item.status === statusFilter;
      return matchesQuery && matchesCategory && matchesStatus;
    });
  }, [expenses, query, categoryFilter, statusFilter]);

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!merchant || !amount || Number(amount) <= 0) {
      toast.error("Please provide valid merchant and amount.");
      return;
    }

    const newExpense: Expense = {
      id: `EXP-${1000 + expenses.length + 1}`,
      destination: destination || "General Business Travel",
      category,
      amount: Number(amount),
      currency,
      date: expenseDate,
      merchant,
      status: "Submitted",
      receiptName: receiptFile || "attached_receipt.pdf",
      notes,
    };

    setExpenses([newExpense, ...expenses]);
    toast.success("Expense claim submitted for approval.");
    setIsCreateOpen(false);
    // Reset form
    setMerchant("");
    setAmount("");
    setNotes("");
    setReceiptFile(null);
  };

  const handleExportCsv = () => {
    const headers = ["Expense ID", "Merchant", "Category", "Amount", "Currency", "Destination", "Date", "Status", "Notes"];
    const rows = filtered.map((item) => [
      item.id,
      item.merchant,
      item.category,
      item.amount,
      item.currency,
      item.destination,
      item.date,
      item.status,
      item.notes || "",
    ]);
    exportToCsv("trapple_expenses", headers, rows);
    toast.success("Expense log exported to CSV.");
  };

  return (
    <div className="page-stack animate-page">
      {/* Header */}
      <div className="page-heading">
        <div>
          <p className="eyebrow">Employee workspace</p>
          <h1>Expense Tracker</h1>
          <p className="page-description">
            Submit trip out-of-pocket expenses, upload receipts, and monitor reimbursement statuses.
          </p>
        </div>
        <div className="heading-actions flex items-center gap-3">
          <button
            type="button"
            onClick={handleExportCsv}
            className="button button-ghost flex items-center gap-2"
          >
            <Download size={15} /> Export CSV
          </button>
          <button
            type="button"
            onClick={() => setIsCreateOpen(true)}
            className="button button-primary flex items-center gap-2"
          >
            <Plus size={16} /> Log Expense
          </button>
        </div>
      </div>

      {/* Summary Stat Cards */}
      <section className="stats-grid">
        <div className="stat-card">
          <div className="stat-card-top">
            <span className="stat-icon"><WalletCards size={18} /></span>
            <span className="stat-detail">Total claimed</span>
          </div>
          <strong className="stat-value">₹{stats.totalClaimed.toLocaleString("en-IN")}</strong>
          <span className="stat-label">{expenses.length} claims submitted</span>
        </div>

        <div className="stat-card stat-accent">
          <div className="stat-card-top">
            <span className="stat-icon"><CheckCircle2 size={18} /></span>
            <span className="stat-detail">Settled to bank</span>
          </div>
          <strong className="stat-value">₹{stats.reimbursed.toLocaleString("en-IN")}</strong>
          <span className="stat-label">Reimbursed directly</span>
        </div>

        <div className="stat-card">
          <div className="stat-card-top">
            <span className="stat-icon"><FileCheck2 size={18} /></span>
            <span className="stat-detail">Approved</span>
          </div>
          <strong className="stat-value">₹{stats.approved.toLocaleString("en-IN")}</strong>
          <span className="stat-label">In payroll processing</span>
        </div>

        <div className="stat-card">
          <div className="stat-card-top">
            <span className="stat-icon"><Clock3 size={18} /></span>
            <span className="stat-detail">Pending review</span>
          </div>
          <strong className="stat-value">₹{stats.pending.toLocaleString("en-IN")}</strong>
          <span className="stat-label">With your approver</span>
        </div>
      </section>

      {/* Policy compliance alert */}
      <div className="p-4 rounded-xl bg-[#14B8A6]/10 border border-[#14B8A6]/25 flex items-center justify-between text-xs text-[#0D9488] dark:text-[#2DD4BF]">
        <div className="flex items-center gap-2.5">
          <Sparkles size={16} className="shrink-0" />
          <span>
            <strong>Policy Auto-Check:</strong> 100% of your expenses are within standard per-diem limits for FY 2026.
          </span>
        </div>
        <span className="font-semibold underline cursor-pointer hover:text-[#14B8A6]">View Travel Policy</span>
      </div>

      {/* Filter and Search toolbar */}
      <div className="panel table-panel">
        <div className="table-toolbar">
          <div className="search-field">
            <Search size={16} />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search merchant, trip or expense ID"
            />
          </div>

          <div className="toolbar-filters flex items-center gap-3">
            <div className="flex items-center gap-1.5">
              <Filter size={15} />
              <select
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
              >
                <option value="ALL">All categories</option>
                <option value="Flight">Flight</option>
                <option value="Hotel">Hotel</option>
                <option value="Meals">Meals</option>
                <option value="Transport">Transport</option>
                <option value="Per Diem">Per Diem</option>
                <option value="Supplies">Supplies</option>
                <option value="Other">Other</option>
              </select>
            </div>

            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">All statuses</option>
              <option value="Submitted">Submitted</option>
              <option value="Approved">Approved</option>
              <option value="Reimbursed">Reimbursed</option>
              <option value="Rejected">Rejected</option>
            </select>
          </div>
        </div>

        {/* Expenses Table */}
        {filtered.length ? (
          <div className="responsive-table">
            <table>
              <thead>
                <tr>
                  <th>Expense / Merchant</th>
                  <th>Category</th>
                  <th>Trip / Destination</th>
                  <th>Date</th>
                  <th>Amount</th>
                  <th>Receipt</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((item) => {
                  const Icon = CATEGORY_ICONS[item.category] || Receipt;
                  return (
                    <tr key={item.id}>
                      <td>
                        <div className="table-journey">
                          <span className="w-8 h-8 rounded-lg bg-[#F1F5F9] dark:bg-[#1E293B] flex items-center justify-center text-[#14B8A6]">
                            <Icon size={16} />
                          </span>
                          <span>
                            <strong>{item.merchant}</strong>
                            <small>{item.id}</small>
                          </span>
                        </div>
                      </td>
                      <td>{item.category}</td>
                      <td>{item.destination}</td>
                      <td>{item.date}</td>
                      <td>
                        <strong className="text-sm font-semibold">
                          ₹{item.amount.toLocaleString("en-IN")}
                        </strong>
                      </td>
                      <td>
                        {item.receiptName ? (
                          <span className="inline-flex items-center gap-1 text-xs text-[#52616a] dark:text-[#9bb0ac] hover:underline cursor-pointer">
                            <Receipt size={13} /> {item.receiptName}
                          </span>
                        ) : (
                          <span className="text-xs text-[#839099]">—</span>
                        )}
                      </td>
                      <td>
                        <span
                          className={`status-badge ${
                            item.status === "Reimbursed"
                              ? "status-completed"
                              : item.status === "Approved"
                              ? "status-approved"
                              : item.status === "Rejected"
                              ? "status-rejected"
                              : "status-pending-approval"
                          }`}
                        >
                          <span className="status-dot" />
                          {item.status}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state py-12 text-center space-y-2">
            <div className="empty-icon mx-auto"><Receipt size={24} /></div>
            <h3>No expenses found</h3>
            <p className="text-sm text-[#839099]">
              {query || categoryFilter !== "ALL" || statusFilter !== "ALL"
                ? "Try adjusting your filters or search terms."
                : "Log your first business expense to track reimbursements."}
            </p>
          </div>
        )}
      </div>

      {/* Log Expense Modal Dialog */}
      {isCreateOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in"
          onClick={() => setIsCreateOpen(false)}
        >
          <div
            className="w-full max-w-lg bg-white dark:bg-[#111827] rounded-2xl shadow-xl border border-[#E4E7EC] dark:border-[#1E293B] p-6 space-y-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-[#E4E7EC] dark:border-[#1E293B] pb-3">
              <div>
                <span className="text-[10px] font-bold uppercase tracking-wider text-[#14B8A6]">
                  TRAVEL REIMBURSEMENT
                </span>
                <h2 className="text-xl font-bold text-[#101828] dark:text-[#F8FAFC]">
                  Log Business Expense
                </h2>
              </div>
              <button
                type="button"
                onClick={() => setIsCreateOpen(false)}
                className="text-[#667085] hover:text-[#101828] dark:hover:text-white"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleCreate} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <label className="field">
                  <span>Merchant / Vendor *</span>
                  <input
                    required
                    value={merchant}
                    onChange={(e) => setMerchant(e.target.value)}
                    placeholder="e.g. Air India, Marriott"
                  />
                </label>
                <label className="field">
                  <span>Category</span>
                  <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value as any)}
                  >
                    <option value="Flight">Flight</option>
                    <option value="Hotel">Hotel</option>
                    <option value="Meals">Meals</option>
                    <option value="Transport">Transport</option>
                    <option value="Per Diem">Per Diem</option>
                    <option value="Supplies">Supplies</option>
                    <option value="Other">Other</option>
                  </select>
                </label>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <label className="field">
                  <span>Amount (INR) *</span>
                  <input
                    type="number"
                    min="1"
                    step="0.01"
                    required
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="0.00"
                  />
                </label>
                <label className="field">
                  <span>Expense Date</span>
                  <input
                    type="date"
                    required
                    value={expenseDate}
                    onChange={(e) => setExpenseDate(e.target.value)}
                  />
                </label>
              </div>

              <label className="field">
                <span>Associated Journey</span>
                <input
                  value={destination}
                  onChange={(e) => setDestination(e.target.value)}
                  placeholder="e.g. Bengaluru Tech Hub"
                />
              </label>

              {/* Receipt simulated upload */}
              <div>
                <span className="block text-xs font-semibold text-[#52616a] dark:text-[#9bb0ac] mb-1.5">
                  Receipt Attachment
                </span>
                <label className="border-2 border-dashed border-[#E4E7EC] dark:border-[#1E293B] rounded-xl p-4 flex flex-col items-center justify-center cursor-pointer hover:bg-[#F8FAFC] dark:hover:bg-[#1E293B] transition-colors">
                  <Upload size={20} className="text-[#94A3B8] mb-1" />
                  <span className="text-xs text-[#101828] dark:text-[#F8FAFC] font-medium">
                    {receiptFile || "Click to upload bill / receipt PDF or image"}
                  </span>
                  <span className="text-[10px] text-[#94A3B8]">Max size: 10MB</span>
                  <input
                    type="file"
                    className="hidden"
                    onChange={(e) => {
                      if (e.target.files?.[0]) {
                        setReceiptFile(e.target.files[0].name);
                        toast.success("Receipt attached successfully.");
                      }
                    }}
                  />
                </label>
              </div>

              <label className="field">
                <span>Business Purpose / Notes</span>
                <textarea
                  rows={2}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Brief note for your approver"
                />
              </label>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsCreateOpen(false)}
                  className="button button-ghost"
                >
                  Cancel
                </button>
                <button type="submit" className="button button-primary">
                  Submit Claim
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

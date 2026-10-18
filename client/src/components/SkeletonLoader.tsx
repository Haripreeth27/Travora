import React from "react";

export function SkeletonMetrics() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-pulse">
      {[1, 2, 3, 4].map((i) => (
        <div
          key={i}
          className="p-5 rounded-2xl border border-[#E4E7EC] dark:border-[#1E293B] bg-white dark:bg-[#111827] space-y-3"
        >
          <div className="flex items-center justify-between">
            <div className="w-8 h-8 rounded-xl bg-slate-200 dark:bg-slate-800" />
            <div className="w-20 h-4 rounded-md bg-slate-100 dark:bg-slate-800" />
          </div>
          <div className="w-16 h-8 rounded-md bg-slate-200 dark:bg-slate-800" />
          <div className="w-28 h-3.5 rounded-md bg-slate-100 dark:bg-slate-800" />
        </div>
      ))}
    </div>
  );
}

export function SkeletonTable({ rows = 5 }: { rows?: number }) {
  return (
    <div className="w-full space-y-3 animate-pulse p-4">
      <div className="h-10 rounded-xl bg-slate-200 dark:bg-slate-800 w-full mb-4" />
      {Array.from({ length: rows }).map((_, i) => (
        <div
          key={i}
          className="h-14 rounded-xl bg-slate-100 dark:bg-slate-800/60 w-full flex items-center justify-between px-4 gap-4"
        >
          <div className="w-10 h-10 rounded-full bg-slate-200 dark:bg-slate-700 shrink-0" />
          <div className="flex-1 space-y-2">
            <div className="w-1/3 h-4 rounded bg-slate-200 dark:bg-slate-700" />
            <div className="w-1/4 h-3 rounded bg-slate-100 dark:bg-slate-800" />
          </div>
          <div className="w-24 h-6 rounded-full bg-slate-200 dark:bg-slate-700 shrink-0" />
        </div>
      ))}
    </div>
  );
}

export function SkeletonCard() {
  return (
    <div className="p-6 rounded-2xl border border-[#E4E7EC] dark:border-[#1E293B] bg-white dark:bg-[#111827] space-y-4 animate-pulse">
      <div className="flex items-center justify-between">
        <div className="w-32 h-5 rounded-md bg-slate-200 dark:bg-slate-800" />
        <div className="w-12 h-6 rounded-full bg-slate-100 dark:bg-slate-800" />
      </div>
      <div className="h-28 rounded-xl bg-slate-100 dark:bg-slate-800/80 w-full" />
      <div className="flex items-center gap-3">
        <div className="w-24 h-9 rounded-xl bg-slate-200 dark:bg-slate-800" />
        <div className="w-24 h-9 rounded-xl bg-slate-100 dark:bg-slate-800" />
      </div>
    </div>
  );
}

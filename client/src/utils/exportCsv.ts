/**
 * Utility to export tabular data to CSV format and trigger browser download.
 * Implements protection against CSV Formula Injection (CWE-1236).
 */
export function sanitizeCsvCell(value: unknown): string {
  if (value === null || value === undefined) return '""';
  let stringVal = String(value);

  // Mitigate CSV Formula Injection (CWE-1236):
  // If the cell begins with formula trigger characters (=, +, -, @, tab, or carriage return),
  // prefix with a single quote (') so spreadsheet applications interpret it strictly as text.
  const trimmed = stringVal.trimStart();
  if (trimmed.length > 0) {
    const firstChar = trimmed[0];
    if (firstChar === "=" || firstChar === "+" || firstChar === "-" || firstChar === "@" || firstChar === "\t" || firstChar === "\r") {
      stringVal = "'" + stringVal;
    }
  }

  if (stringVal.includes('"') || stringVal.includes(',') || stringVal.includes('\n') || stringVal.includes('\r')) {
    return `"${stringVal.replace(/"/g, '""')}"`;
  }
  return `"${stringVal}"`;
}

export function exportToCsv(filename: string, headers: string[], rows: (string | number | boolean | null | undefined)[][]) {
  const headerRow = headers.map(sanitizeCsvCell).join(",");
  const dataRows = rows.map((row) => row.map(sanitizeCsvCell).join(","));
  const csvContent = [headerRow, ...dataRows].join("\r\n");

  const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.setAttribute("href", url);
  link.setAttribute("download", `${filename}_${new Date().toISOString().slice(0, 10)}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

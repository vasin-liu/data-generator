/**
 * Formats ISO timestamps for console tables (locale-aware).
 */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '—';
  }
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}

export const CONSOLE_ROLES = [
  'VIEWER',
  'EDITOR',
  'OPERATOR',
  'DATASOURCE_ADMIN',
  'ADMIN',
] as const;

export type ConsoleRoleName = (typeof CONSOLE_ROLES)[number];

const STORAGE_KEY = 'data-generator.console.role';

/**
 * @returns persisted console role for RBAC headers (defaults to ADMIN in open dev mode)
 */
export function getConsoleRole(): ConsoleRoleName {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored && (CONSOLE_ROLES as readonly string[]).includes(stored)) {
    return stored as ConsoleRoleName;
  }
  return 'ADMIN';
}

/**
 * @param role role sent as {@code X-Console-Role} when staging RBAC is enabled
 */
export function setConsoleRole(role: ConsoleRoleName): void {
  localStorage.setItem(STORAGE_KEY, role);
}

/**
 * @returns whether the role may publish templates
 */
export function canPublishWithRole(role: ConsoleRoleName): boolean {
  return role === 'ADMIN';
}

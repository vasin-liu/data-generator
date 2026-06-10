import type { Page } from '@playwright/test';
import type { ConsoleRoleHeader } from './api';
import { TestIds, type NavTestId } from './test-ids';


export async function primeConsoleRole(page: Page, role: ConsoleRoleHeader): Promise<void> {
  await page.addInitScript((storedRole) => {
    localStorage.setItem('data-generator.console.role', storedRole);
  }, role);
}

export async function gotoConsoleHome(page: Page): Promise<void> {
  await page.goto('./');
  await page.getByTestId(TestIds.home).waitFor({ state: 'visible' });
}

export async function navigateViaTopNav(page: Page, navTestId: NavTestId): Promise<void> {
  await page.getByTestId(navTestId).click();
}

export async function expectPageTestId(page: Page, testId: string): Promise<void> {
  await page.getByTestId(testId).waitFor({ state: 'visible' });
}

export async function setTheme(page: Page, mode: 'dark' | 'light'): Promise<void> {
  const toggle = page.getByTestId(TestIds.themeToggle);
  await toggle.waitFor({ state: 'visible' });
  const optionTestId = mode === 'dark' ? TestIds.themeDark : TestIds.themeLight;
  await page.getByTestId(optionTestId).click();
  await page.waitForFunction(
    (expected) => document.documentElement.getAttribute('data-theme') === expected,
    mode,
  );
}

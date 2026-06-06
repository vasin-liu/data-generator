import { expect, test } from '@playwright/test';
import { gotoConsoleHome, navigateViaTopNav, setTheme } from '../helpers/navigation';
import { TestIds } from '../helpers/test-ids';

/**
 * Staging-style UI acceptance: theme, shell, and every primary console surface loads.
 * Run via `scripts/verify-console.ps1` or CI after Podman E2E container is up.
 */
test.describe('Acceptance / UI automation checklist', () => {
  test('light theme shell and home dashboard', async ({ page }) => {
    await gotoConsoleHome(page);
    await setTheme(page, 'light');
    await expect(page.getByTestId(TestIds.shell)).toBeVisible();
    await expect(page.getByTestId(TestIds.home)).toBeVisible();
    await expect(page.locator('.home-hero-title')).toBeVisible();
    await expect(page.locator('.console-glass-panel')).toBeVisible();
  });

  test('dark theme and primary navigation surfaces', async ({ page }) => {
    await gotoConsoleHome(page);
    await setTheme(page, 'dark');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

    const routes: Array<{ nav: keyof typeof TestIds.nav; pageId: string }> = [
      { nav: 'templates', pageId: TestIds.pages.templates },
      { nav: 'datasources', pageId: TestIds.pages.datasources },
      { nav: 'jobs', pageId: TestIds.pages.jobs },
      { nav: 'schedules', pageId: TestIds.pages.schedules },
    ];

    for (const route of routes) {
      await navigateViaTopNav(page, TestIds.nav[route.nav]);
      await expect(page.getByTestId(route.pageId)).toBeVisible();
      await expect(page.getByRole('table').first()).toBeVisible();
    }
  });

  test('runtime pills and locale control visible in top dock', async ({ page }) => {
    await gotoConsoleHome(page);
    await expect(page.getByTestId(TestIds.themeToggle)).toBeVisible();
    await expect(page.locator('.console-runtime-pill').first()).toBeVisible();
    await expect(page.locator('.console-locale-select')).toBeVisible();
  });
});

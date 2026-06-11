import { expect, test } from '@playwright/test';
import { gotoConsoleHome, setTheme } from '../helpers/navigation';
import { TestIds } from '../helpers/test-ids';

test.describe('Theme / light and dark glass', () => {
  test.beforeEach(async ({ page }) => {
    await gotoConsoleHome(page);
  });

  test('switches to light theme with readable nav contrast', async ({ page }) => {
    await setTheme(page, 'light');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');

    const activeNav = page.getByTestId(TestIds.nav.home);
    await expect(activeNav).toHaveClass(/is-active/);

    const navColor = await activeNav.evaluate((el) => getComputedStyle(el).color);
    expect(navColor).not.toBe('rgba(0, 0, 0, 0)');

    await page.getByTestId(TestIds.nav.templates).click();
    const primaryBtn = page.getByTestId(TestIds.actions.templatesNew);
    await expect(primaryBtn).toBeVisible();
    const btnColor = await primaryBtn.evaluate((el) => getComputedStyle(el).color);
    expect(btnColor).toMatch(/rgb\(255,\s*255,\s*255\)|#fff/i);
  });

  test('switches to dark theme', async ({ page }) => {
    await setTheme(page, 'dark');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
    await expect(page.getByTestId(TestIds.themeToggle)).toBeVisible();
    await expect(page.locator('.console-glass-panel')).toBeVisible();
  });

  test('persists theme after reload', async ({ page }) => {
    await setTheme(page, 'light');
    await page.reload();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
  });
});

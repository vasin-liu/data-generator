import { expect, test } from '@playwright/test';
import { gotoConsoleHome } from '../helpers/navigation';
import { TestIds } from '../helpers/test-ids';

test.describe('Home / dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await gotoConsoleHome(page);
  });

  test('renders hero and runtime section', async ({ page }) => {
    await expect(page.getByTestId(TestIds.home)).toBeVisible();
    await expect(page.getByTestId(TestIds.brand)).toBeVisible();
    await expect(page.getByTestId(TestIds.shell)).toBeVisible();
    await expect(page.locator('.home-hero-title')).toBeVisible();
    await expect(page.locator('.home-runtime-grid')).toBeVisible();
    await expect(page.locator('.home-area-card').first()).toBeVisible();
  });

  test('area card navigates to templates', async ({ page }) => {
    await page.locator('.home-area-card').first().click();
    await expect(page).toHaveURL(/\/templates/);
  });
});

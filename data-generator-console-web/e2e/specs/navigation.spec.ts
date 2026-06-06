import { expect, test } from '@playwright/test';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import { TestIds } from '../helpers/test-ids';

const ROUTES: Array<{ nav: keyof typeof TestIds.nav; path: RegExp; page: string }> = [
  { nav: 'home', path: /\/console\/?$/, page: TestIds.home },
  { nav: 'templates', path: /\/templates/, page: TestIds.pages.templates },
  { nav: 'datasources', path: /\/datasources/, page: TestIds.pages.datasources },
  { nav: 'jobs', path: /\/jobs/, page: TestIds.pages.jobs },
  { nav: 'schedules', path: /\/schedules/, page: TestIds.pages.schedules },
];

test.describe('Navigation / top dock', () => {
  test.beforeEach(async ({ page }) => {
    await gotoConsoleHome(page);
  });

  for (const route of ROUTES) {
    test(`nav-${route.nav} opens page`, async ({ page }) => {
      if (route.nav !== 'home') {
        await navigateViaTopNav(page, TestIds.nav[route.nav]);
      }
      await expect(page).toHaveURL(route.path);
      await expect(page.getByTestId(route.page)).toBeVisible();
    });
  }

  test('brand returns home', async ({ page }) => {
    await navigateViaTopNav(page, TestIds.nav.templates);
    await page.getByTestId(TestIds.brand).click();
    await expect(page).toHaveURL(/\/console\/?$/);
    await expect(page.getByTestId(TestIds.home)).toBeVisible();
  });

  test('migration nav when enabled', async ({ page }) => {
    const migrationNav = page.getByTestId(TestIds.nav.migration);
    if ((await migrationNav.count()) === 0) {
      test.skip(true, 'Migration UI disabled (VITE_ENABLE_MIGRATION != true)');
    }
    await migrationNav.click();
    await expect(page).toHaveURL(/\/migration/);
    await expect(page.getByTestId(TestIds.pages.migration)).toBeVisible();
  });
});

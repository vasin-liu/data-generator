import { expect, test } from '@playwright/test';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import { TestIds } from '../helpers/test-ids';

test.describe('Feature pages / primary actions', () => {
  test('templates list and new editor', async ({ page }) => {
    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await expect(page.getByTestId(TestIds.pages.templates)).toBeVisible();
    await expect(page.getByRole('table')).toBeVisible();

    await page.getByTestId(TestIds.actions.templatesNew).click();
    await expect(page).toHaveURL(/\/templates\/new/);
    await expect(page.getByTestId(TestIds.pages.templateEditor)).toBeVisible({ timeout: 30_000 });
    await expect(page.getByRole('tabpanel').first()).toBeVisible();
    await expect(page.getByRole('textbox').first()).toBeVisible();
  });

  test('datasources overview', async ({ page }) => {
    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.datasources);
    await expect(page.getByTestId(TestIds.pages.datasources)).toBeVisible();
    await expect(page.getByTestId(TestIds.actions.datasourcesNew)).toBeVisible();
    await expect(page.getByRole('table').first()).toBeVisible();
  });

  test('jobs history table', async ({ page }) => {
    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.jobs);
    await expect(page.getByTestId(TestIds.pages.jobs)).toBeVisible();
    await expect(page.getByRole('table')).toBeVisible();
  });

  test('schedules list and create dialog', async ({ page }) => {
    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.schedules);
    await expect(page.getByTestId(TestIds.pages.schedules)).toBeVisible();
    await page.getByTestId(TestIds.actions.schedulesNew).click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.keyboard.press('Escape');
  });

  test('audit log table', async ({ page }) => {
    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.audit);
    await expect(page.getByTestId(TestIds.pages.audit)).toBeVisible();
    await expect(page.getByRole('table')).toBeVisible();
  });
});

import { expect, test } from '@playwright/test';
import { apiGetWithRole, expectApiSuccess, unwrapApiData } from '../helpers/api';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import { TestIds } from '../helpers/test-ids';

type JdbcDriverPreset = {
  id: string;
  driverClassName: string;
  urlTemplate: string;
};

const PRESET_ID = 'postgresql16';

/**
 * Phase 9 UAT (D-12) — one preset select → form auto-fill → save path.
 */
test.describe('JDBC dialect driver preset', () => {
  test('select preset auto-fills driver and URL then saves datasource', async ({ page, request }) => {
    const presetsResp = await apiGetWithRole(request, '/api/datasources/driver-presets');
    expect(presetsResp.res.ok()).toBeTruthy();
    expectApiSuccess(presetsResp.body);
    const presets = unwrapApiData<JdbcDriverPreset[]>(presetsResp.body) ?? [];
    const preset = presets.find((row) => row.id === PRESET_ID);
    expect(preset).toBeTruthy();

    const name = `e2e-dialect-preset-${Date.now()}`;
    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.datasources);
    await page.getByTestId(TestIds.actions.datasourcesNew).click();

    const dialog = page.getByRole('dialog');
    await dialog.getByRole('textbox', { name: /名称|Name/i }).fill(name);
    await dialog.getByRole('combobox').first().click();
    await page
      .locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)')
      .getByText(/PostgreSQL 16\.x/i)
      .first()
      .click();

    const driverInput = dialog.getByRole('textbox', { name: /驱动类|Driver class/i });
    const urlInput = dialog.getByRole('textbox', { name: /JDBC URL/i });
    await expect(driverInput).toHaveValue(preset!.driverClassName);
    await expect(urlInput).toHaveValue(preset!.urlTemplate);

    await dialog.getByRole('textbox', { name: /用户名|Username/i }).fill('e2e_user');
    await dialog.getByRole('textbox', { name: /密码|Password/i }).fill('e2e_test_only');

    const [saveResp] = await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/api/datasources') && response.request().method() === 'POST',
      ),
      dialog.locator('button[type="submit"]').click(),
    ]);
    expect((await saveResp.json()).success).toBe(true);
    await expect(dialog).toBeHidden();
  });
});

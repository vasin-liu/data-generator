import { expect, test } from '@playwright/test';
import { apiGetWithRole, expectApiSuccess, unwrapApiData } from '../helpers/api';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import { TestIds } from '../helpers/test-ids';

type JdbcDriverPreset = {
  id: string;
  driverClassName: string;
  urlTemplate: string;
};

const PRESET_CASES = [
  { id: 'postgresql16', label: /PostgreSQL 16\.x/i },
  { id: 'kingbase8', label: /Kingbase 8|金仓 8/i },
] as const;

/**
 * Phase 9/11 UAT — preset select → form auto-fill → save (no Test Connection).
 * Covers postgresql16 (baseline) and kingbase8 (RW-05/RW-06 non-PG evidence).
 */
test.describe('JDBC dialect driver preset', () => {
  for (const presetCase of PRESET_CASES) {
    test(`select ${presetCase.id} auto-fills driver and URL then saves datasource`, async ({
      page,
      request,
    }) => {
      const presetsResp = await apiGetWithRole(request, '/api/datasources/driver-presets');
      expect(presetsResp.res.ok()).toBeTruthy();
      expectApiSuccess(presetsResp.body);
      const presets = unwrapApiData<JdbcDriverPreset[]>(presetsResp.body) ?? [];
      const preset = presets.find((row) => row.id === presetCase.id);
      expect(preset).toBeTruthy();
      if (presetCase.id === 'kingbase8') {
        expect(preset!.driverClassName).toBe('com.kingbase8.Driver');
      }

      const name = `e2e-dialect-preset-${presetCase.id}-${Date.now()}`;
      await gotoConsoleHome(page);
      await navigateViaTopNav(page, TestIds.nav.datasources);
      await page.getByTestId(TestIds.actions.datasourcesNew).click();

      const dialog = page.getByRole('dialog');
      await dialog.getByRole('textbox', { name: /名称|Name/i }).fill(name);
      await dialog.getByRole('combobox').first().click();
      await page
        .locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)')
        .getByText(presetCase.label)
        .first()
        .click();

      const driverInput = dialog.getByRole('textbox', { name: /驱动类|Driver class/i });
      const urlInput = dialog.getByRole('textbox', { name: /JDBC URL/i });
      await expect(driverInput).toHaveValue(preset!.driverClassName);
      await expect(urlInput).toHaveValue(preset!.urlTemplate);

      await dialog.getByRole('textbox', { name: /用户名|Username/i }).fill('e2e_user');
      await dialog.getByRole('textbox', { name: /密码|Password/i }).fill('e2e_test_only');

      // Save only — connectivity evidence is Maven ConnectionCatalogTestTests (D-11).
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
  }
});

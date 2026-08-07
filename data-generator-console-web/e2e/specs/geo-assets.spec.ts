import { expect, test } from '@playwright/test';
import { gotoConsoleHome } from '../helpers/navigation';

/**
 * Phase 22 smoke: geo assets nav + page shell (GEO-07).
 * Editor hybrid preview (GEO-12/13) is covered by the Plan 04 SUMMARY checklist —
 * not promoted to P0 matrix (Phase 23 TEST-11).
 */
test.describe('Geo assets page', () => {
  test('nav-geo-assets opens page shell with upload and map region', async ({ page }) => {
    await gotoConsoleHome(page);

    const nav = page.getByTestId('nav-geo-assets');
    await expect(nav).toBeVisible();
    await nav.click();

    await expect(page).toHaveURL(/\/geo-assets/);
    const geoPage = page.getByTestId('geo-assets-page');
    await expect(geoPage).toBeVisible();

    await expect(page.getByTestId('geo-assets-upload')).toBeVisible();

    // Empty registry still renders map chrome; honesty Alert appears when an underlay loads.
    const map = page.getByTestId('geo-assets-map');
    await expect(map).toBeVisible({ timeout: 30_000 });

    // Resilient to empty asset list: empty state OR table body.
    const emptyHeading = geoPage.getByText(/No geo assets yet|暂无地理资产/);
    const table = geoPage.getByRole('table');
    await expect(emptyHeading.or(table)).toBeVisible();
  });

  test('direct /geo-assets route shows page testids', async ({ page }) => {
    await page.goto('./geo-assets');
    await expect(page.getByTestId('geo-assets-page')).toBeVisible();
    await expect(page.getByTestId('geo-assets-upload')).toBeVisible();
    await expect(page.getByTestId('geo-assets-map')).toBeVisible({ timeout: 30_000 });
  });
});

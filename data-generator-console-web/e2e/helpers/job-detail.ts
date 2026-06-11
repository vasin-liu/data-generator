import { expect, type Page } from '@playwright/test';

/**
 * Asserts the primary execution status on job detail is SUCCESS.
 * Scopes to the main Descriptions block so distributed staging panels do not trip strict mode.
 */
export async function expectJobSucceeded(page: Page, timeout = 90_000): Promise<void> {
  const detail = page.getByTestId('job-detail-page');
  await expect(detail).toBeVisible({ timeout: 30_000 });
  const mainDescriptions = detail.locator('.ant-descriptions').first();
  await expect(
    mainDescriptions
      .getByRole('row', { name: /状态|Status/i })
      .getByRole('cell', { name: /成功|Succeeded/i }),
  ).toBeVisible({ timeout });
}

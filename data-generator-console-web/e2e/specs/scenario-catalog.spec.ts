import { expect, test, type Page } from '@playwright/test';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import { openScenarioFromCatalog, publishAndRunFromReview, saveTemplateFromReview } from '../helpers/editor';
import { TestIds } from '../helpers/test-ids';

async function expectJobSucceeded(page: Page) {
  await expect(page.getByRole('cell', { name: /成功|Succeeded/i })).toBeVisible({ timeout: 90_000 });
}

const OFFICIAL_SCENARIOS = [
  { id: 'GF-A', label: 'Synthetic (A)' },
  { id: 'GF-B', label: 'Transform DAG (B)' },
  { id: 'GF-WF', label: 'Workflow branch (WF)' },
  { id: 'GF-JS', label: 'JavaScript (JS)' },
] as const;

test.describe('Official scenario catalog', () => {
  for (const scenario of OFFICIAL_SCENARIOS) {
    test(`${scenario.id}: create from scenario, publish, run, and show run report`, async ({ page }) => {
      test.setTimeout(180_000);

      await gotoConsoleHome(page);
      await navigateViaTopNav(page, TestIds.nav.templates);
      await openScenarioFromCatalog(page, scenario.id);

      await saveTemplateFromReview(page);
      await publishAndRunFromReview(page);

      await expectJobSucceeded(page);

      if (scenario.id === 'GF-B') {
        await expect(page.getByRole('heading', { name: /transformers|转换/i })).toBeVisible({
          timeout: 30_000,
        });
      }

      if (scenario.id === 'GF-A') {
        await expect(page.getByRole('heading', { name: /report|报告|run/i }).first()).toBeVisible({
          timeout: 30_000,
        });
      }
    });
  }

  test('scenario catalog modal lists four official families', async ({ page }) => {
    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await page.getByTestId('templates-from-scenario-button').click();
    await page.getByTestId('scenario-catalog-modal').waitFor({ state: 'visible', timeout: 30_000 });

    for (const scenario of OFFICIAL_SCENARIOS) {
      await expect(page.getByTestId(`scenario-use-${scenario.id}`)).toBeVisible();
    }
  });
});

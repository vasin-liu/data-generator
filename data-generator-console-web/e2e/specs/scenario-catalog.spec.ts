import { expect, test } from '@playwright/test';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import { openScenarioFromCatalog, publishAndRunFromReview, saveTemplateFromReview } from '../helpers/editor';
import { expectJobSucceeded } from '../helpers/job-detail';
import { TestIds } from '../helpers/test-ids';

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

  test('scenario catalog modal lists official families', async ({ page }) => {
    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await page.getByTestId('templates-from-scenario-button').click();
    await page.getByTestId('scenario-catalog-modal').waitFor({ state: 'visible', timeout: 30_000 });

    for (const scenario of OFFICIAL_SCENARIOS) {
      await expect(page.getByTestId(`scenario-use-${scenario.id}`)).toBeVisible();
    }
    await expect(page.getByTestId('scenario-use-GF-WFS')).toBeVisible();
    await expect(page.getByTestId('scenario-use-GF-BJ')).toBeVisible();
  });
});

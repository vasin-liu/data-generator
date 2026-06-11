import { expect, test, type Page } from '@playwright/test';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import {
  openNewTemplateEditor,
  selectEditorTab,
  setTemplateName,
} from '../helpers/editor';
import { expectJobSucceeded } from '../helpers/job-detail';
import { TestIds } from '../helpers/test-ids';

async function enableWorkflowMode(page: Page) {
  await selectEditorTab(page, /workflow|工作流/i);
  const toggle = page.getByTestId('workflow-enabled-switch');
  await toggle.waitFor({ state: 'visible' });
  if (!(await toggle.getAttribute('aria-checked'))?.includes('true')) {
    await toggle.click();
  }
}

async function workflowStepRow(page: Page, index: number) {
  return page.getByTestId('workflow-steps-table').locator('tbody tr').nth(index);
}

async function selectStepType(page: Page, rowIndex: number, label: RegExp) {
  const row = await workflowStepRow(page, rowIndex);
  await row.locator('.ant-select').first().click();
  await page
    .locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)')
    .last()
    .locator('.ant-select-item-option')
    .filter({ hasText: label })
    .first()
    .click();
}

async function bindInvokeBlock(page: Page, rowIndex: number, blockId: string) {
  const row = page.getByTestId('workflow-steps-table').locator('tbody tr').nth(rowIndex);
  await row.locator('.ant-select').last().click();
  await page
    .locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)')
    .last()
    .locator('.ant-select-item-option')
    .filter({ hasText: blockId })
    .first()
    .click();
}

test.describe('Workflow pause and resume', () => {
  test('manual pause is visible on job detail and resumes to success', async ({ page }) => {
    test.setTimeout(120_000);
    const uniqueName = `e2e-wf-pause-${Date.now()}`;

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await openNewTemplateEditor(page);
    await setTemplateName(page, uniqueName);

    await enableWorkflowMode(page);

    const logRow = await workflowStepRow(page, 0);
    await logRow.getByPlaceholder(/workflow-start/i).fill('workflow-start');

    await selectStepType(page, 1, /pause|暂停/i);
    const pauseRow = await workflowStepRow(page, 1);
    await pauseRow.locator('input.ant-input').first().fill('pause-gate');
    await pauseRow.getByRole('switch').click();

    await page.getByTestId('workflow-add-step').click();
    await selectStepType(page, 2, /invoke compute block|调用计算块/i);
    await bindInvokeBlock(page, 2, 'block-1');

    await selectEditorTab(page, /review|审阅/i);
    await page.getByTestId('review-save').click();
    await page.waitForURL(/\/templates\/\d+/, { timeout: 30_000 });

    await page.getByRole('button', { name: /运\s*行|^run$/i }).click();
    await page.waitForURL(/\/jobs\/\d+/, { timeout: 60_000 });

    await expect(page.getByTestId('job-detail-page')).toBeVisible();
    await expect(page.getByText(/paused|已暂停/i).first()).toBeVisible({ timeout: 60_000 });
    await expect(page.getByTestId('job-pause-reason')).toBeVisible({ timeout: 30_000 });
    await expect(page.getByTestId('job-pause-reason')).toContainText(/pause-gate/i);
    await expect(page.getByTestId('job-resume-button')).toBeVisible();

    await page.getByTestId('job-resume-button').click();
    await expectJobSucceeded(page, 60_000);
  });
});

import { expect, test, type Page } from '@playwright/test';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import {
  openNewTemplateEditor,
  selectEditorTab,
  setTemplateName,
} from '../helpers/editor';
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

test.describe('Workflow branch editor', () => {
  test('structured branch fields save and run to success', async ({ page }) => {
    test.setTimeout(180_000);
    const uniqueName = `e2e-wf-branch-${Date.now()}`;

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await openNewTemplateEditor(page);
    await setTemplateName(page, uniqueName);

    await enableWorkflowMode(page);
    await selectStepType(page, 0, /branch|分支/i);

    const branchFields = page.getByTestId('workflow-branch-fields');
    await expect(branchFields).toBeVisible();
    await branchFields.locator('input').first().fill('true');
    await branchFields.locator('input').nth(1).fill('then-branch');
    await branchFields.locator('input').nth(2).fill('else-branch');

    // Default scaffold already has invoke_compute_block as step 2 after enabling workflow.
    await bindInvokeBlock(page, 1, 'block-1');

    await selectEditorTab(page, /review|审阅/i);
    await page.getByTestId('review-save').click();
    await page.waitForURL(/\/templates\/\d+/, { timeout: 30_000 });

    await page.getByRole('button', { name: /发\s*布|^publish$/i }).click();
    await page.getByRole('button', { name: /运\s*行|^run$/i }).click();
    await page.waitForURL(/\/jobs\/\d+/, { timeout: 60_000 });

    await expect(page.getByRole('cell', { name: /成功|Succeeded/i })).toBeVisible({ timeout: 90_000 });
  });
});

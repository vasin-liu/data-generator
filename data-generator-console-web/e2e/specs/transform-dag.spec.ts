import { expect, test, type Page } from '@playwright/test';
import { expectApiSuccess, fetchTemplateEditor, unwrapApiData } from '../helpers/api';
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

async function openComputeBlockTransformTab(page: Page) {
  await page.getByRole('tab', { name: /transform|转换/i }).last().click();
  await page.getByTestId('transform-layout-radio').waitFor({ state: 'visible' });
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

async function expectJobSucceeded(page: Page) {
  await expect(page.getByRole('cell', { name: /成功|Succeeded/i })).toBeVisible({ timeout: 60_000 });
}

async function selectDagDependsOn(page: Page, nodeLabel: string) {
  const dropdown = page.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)').last();
  await dropdown.waitFor({ state: 'visible', timeout: 10_000 });
  await dropdown
    .locator('.ant-select-item-option')
    .filter({ has: page.locator('.ant-select-item-option-content', { hasText: nodeLabel }) })
    .first()
    .click();
}

test.describe('Transform DAG editor', () => {
  test('persists DAG nodes, runs workflow template, and shows per-node report metrics', async ({
    page,
    request,
  }) => {
    test.setTimeout(120_000);
    const uniqueName = `e2e-dag-${Date.now()}`;

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await openNewTemplateEditor(page);
    await setTemplateName(page, uniqueName);

    await enableWorkflowMode(page);
    await bindInvokeBlock(page, 1, 'block-1');

    await openComputeBlockTransformTab(page);
    await page.getByTestId('transform-layout-radio').getByText(/dag|DAG/i).click();
    await page.getByTestId('transform-dag-add-node').click();

    const n2Row = page.getByTestId('transform-dag-table').locator('tr').nth(2);
    await n2Row.locator('input').first().fill('n2');
    await n2Row.locator('input').nth(1).fill('step-2');
    await n2Row.getByRole('combobox').first().click();
    await selectDagDependsOn(page, 'n1');
    await n2Row.locator('textarea').first().fill('SELECT value, value + 10 AS shifted FROM input');

    await selectEditorTab(page, /review|审阅/i);
    await page.getByTestId('review-save').click();
    await page.waitForURL(/\/templates\/\d+/, { timeout: 30_000 });

    const templateId = page.url().match(/\/templates\/(\d+)/)?.[1];
    expect(templateId).toBeTruthy();

    await page.getByRole('button', { name: /运\s*行|^run$/i }).click();
    await page.waitForURL(/\/jobs\/\d+/, { timeout: 60_000 });

    await expectJobSucceeded(page);

    await expect(page.getByRole('heading', { name: /transformers|转换/i })).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByRole('cell', { name: /block-1\/n1/i })).toBeVisible({ timeout: 30_000 });
    await expect(page.getByRole('cell', { name: /block-1\/n2/i })).toBeVisible({ timeout: 30_000 });

    const { res, body } = await fetchTemplateEditor(request, templateId!);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const data = unwrapApiData<{
      draft?: {
        computeBlocks?: Array<{
          transformGraph?: { nodes?: Array<{ id?: string }>; edges?: unknown[] };
        }>;
      };
    }>(body);
    const graph = data?.draft?.computeBlocks?.[0]?.transformGraph;
    expect(graph?.nodes?.map((node) => node.id)).toEqual(expect.arrayContaining(['n1', 'n2']));
    expect(graph?.edges?.length).toBeGreaterThan(0);
  });

  test('shows cycle warning when depends-on forms a loop', async ({ page }) => {
    const uniqueName = `e2e-dag-cycle-${Date.now()}`;

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await openNewTemplateEditor(page);
    await setTemplateName(page, uniqueName);

    await enableWorkflowMode(page);
    await openComputeBlockTransformTab(page);
    await page.getByTestId('transform-layout-radio').getByText(/dag|DAG/i).click();
    await page.getByTestId('transform-dag-add-node').click();

    const n2Row = page.getByTestId('transform-dag-table').locator('tr').nth(2);
    await n2Row.getByRole('combobox').first().click();
    await selectDagDependsOn(page, 'n1');

    const n1Row = page.getByTestId('transform-dag-table').locator('tr').nth(1);
    await page.keyboard.press('Escape');
    await n1Row.getByRole('combobox').first().click();
    await selectDagDependsOn(page, 'n2');

    await expect(page.getByTestId('transform-dag-cycle-alert')).toBeVisible();
    await expect(page.getByTestId('transform-dag-cycle-alert')).toContainText(/n1|n2/i);
  });
});

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

async function selectTransformType(page: Page, stepIndex: number) {
  const select = page.getByTestId(`transform-type-select-${stepIndex}`);
  await select.click();
  const dropdown = page.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)').last();
  await dropdown.waitFor({ state: 'visible', timeout: 10_000 });
  await dropdown
    .locator('.ant-select-item-option')
    .filter({ hasText: /JavaScript.*沙箱|JavaScript.*sandboxed/i })
    .first()
    .click();
}

async function expectJobSucceeded(page: Page) {
  await expect(page.getByRole('cell', { name: /成功|Succeeded/i })).toBeVisible({ timeout: 60_000 });
}

test.describe('JavaScript transform editor', () => {
  test('persists SQL + JS chain, runs workflow, and succeeds', async ({ page, request }) => {
    test.setTimeout(180_000);
    const uniqueName = `e2e-js-${Date.now()}`;

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await openNewTemplateEditor(page);
    await setTemplateName(page, uniqueName);

    await enableWorkflowMode(page);
    await bindInvokeBlock(page, 1, 'block-1');

    await openComputeBlockTransformTab(page);
    await page.getByRole('radio', { name: /^Transformer chain$|^转换链$/i }).click();

    const step0 = page.getByTestId('transform-chain-step').nth(0);
    await step0.locator('textarea').first().fill('SELECT value AS amount FROM seed');

    await page.getByTestId('transform-add-step').click();
    await expect(page.getByTestId('transform-chain-step')).toHaveCount(2);

    await selectTransformType(page, 1);

    const step1 = page.getByTestId('transform-chain-step').nth(1);
    const jsScript = step1.getByTestId('transform-js-script');
    await jsScript.waitFor({ state: 'visible', timeout: 15_000 });
    await jsScript.fill('row.amount = row.amount * 2');

    await selectEditorTab(page, /review|审阅/i);
    await page.getByTestId('review-save').click();
    await page.waitForURL(/\/templates\/\d+/, { timeout: 30_000 });

    const templateId = page.url().match(/\/templates\/(\d+)/)?.[1];
    expect(templateId).toBeTruthy();

    const { res, body } = await fetchTemplateEditor(request, templateId!);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const data = unwrapApiData<{
      draft?: {
        computeBlocks?: Array<{
          transformers?: Array<{ type?: string; script?: string; sql?: string }>;
        }>;
      };
    }>(body);
    const transformers = data?.draft?.computeBlocks?.[0]?.transformers ?? [];
    expect(transformers).toHaveLength(2);
    expect(transformers[0]?.type?.toLowerCase()).toBe('sql');
    expect(transformers[0]?.sql).toContain('amount');
    expect(transformers[1]?.type?.toLowerCase()).toBe('js');
    expect(transformers[1]?.script).toContain('row.amount');

    await page.getByRole('button', { name: /运\s*行|^run$/i }).click();
    await page.waitForURL(/\/jobs\/\d+/, { timeout: 60_000 });
    await expectJobSucceeded(page);
  });
});

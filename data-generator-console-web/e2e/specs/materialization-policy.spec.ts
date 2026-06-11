import { expect, test, type Page } from '@playwright/test';
import { expectApiSuccess, fetchTemplateEditor, unwrapApiData } from '../helpers/api';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import {
  openNewTemplateEditor,
  selectEditorTab,
  setTemplateName,
} from '../helpers/editor';
import { TestIds } from '../helpers/test-ids';

/** Ant Design Select renders the chosen label outside the combobox search input. */
function materializationModeItem(page: Page) {
  return page.locator('.ant-form-item').filter({ hasText: /materialization mode|物化模式/i });
}

function materializationLimitItem(page: Page) {
  return page.locator('.ant-form-item').filter({ hasText: /materialization row cap|物化行数上限/i });
}

test.describe('Materialization policy editor', () => {
  test('persists LIMIT materializationPolicy on iterator source', async ({ page, request }) => {
    const uniqueName = `e2e-matpol-${Date.now()}`;

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await openNewTemplateEditor(page);
    await setTemplateName(page, uniqueName);

    await selectEditorTab(page, /sources|数据源/i);

    await page.getByText('input', { exact: true }).first().click();
    await page.locator('.ant-collapse-header').filter({ hasText: /source policy|数据源策略/i }).click();

    const modeItem = materializationModeItem(page);
    await modeItem.scrollIntoViewIfNeeded();
    await modeItem.getByRole('combobox').click();
    await page
      .locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)')
      .getByText(/take first n rows|取前 N 行/i)
      .click();

    const matLimitItem = materializationLimitItem(page);
    await matLimitItem.scrollIntoViewIfNeeded();
    await matLimitItem.getByRole('spinbutton').fill('3');

    await selectEditorTab(page, /review|审阅/i);
    await page.getByTestId('review-save').click();
    await page.waitForURL(/\/templates\/\d+/, { timeout: 30_000 });

    const templateId = page.url().match(/\/templates\/(\d+)/)?.[1];
    expect(templateId).toBeTruthy();

    await page.reload();
    await page.getByTestId('template-editor-page').waitFor({ state: 'visible', timeout: 30_000 });
    await selectEditorTab(page, /sources|数据源/i);
    await page.locator('.ant-collapse-header').filter({ hasText: /source policy|数据源策略/i }).click();

    const reloadedModeItem = materializationModeItem(page);
    await reloadedModeItem.scrollIntoViewIfNeeded();
    await expect(reloadedModeItem.locator('.ant-select-selection-item')).toHaveText(/take first|取前/i);

    const reloadedLimitItem = materializationLimitItem(page);
    await reloadedLimitItem.scrollIntoViewIfNeeded();
    await expect(reloadedLimitItem.getByRole('spinbutton')).toHaveValue('3');

    const { res, body } = await fetchTemplateEditor(request, templateId!);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const data = unwrapApiData<{
      draft?: { sources?: Record<string, { materializationPolicy?: { mode?: string; limit?: number } }> };
    }>(body);
    const policy = data?.draft?.sources?.input?.materializationPolicy;
    expect(policy?.mode).toBe('LIMIT');
    expect(policy?.limit).toBe(3);
  });
});

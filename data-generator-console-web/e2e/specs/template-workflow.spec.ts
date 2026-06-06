import { expect, test } from '@playwright/test';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import {
  openNewTemplateEditor,
  saveTemplateAndReturn,
  selectEditorTab,
  setTemplateName,
} from '../helpers/editor';
import { TestIds } from '../helpers/test-ids';

test.describe('Template workflow / UI business flow', () => {
  test('create scaffold, rename, save, and list in catalog', async ({ page }) => {
    const uniqueName = `e2e-ui-${Date.now()}`;

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await openNewTemplateEditor(page);

    await setTemplateName(page, uniqueName);
    await selectEditorTab(page, /review|审阅/i);
    await saveTemplateAndReturn(page);

    await page.getByPlaceholder(/name or id|名称或编号/i).fill(uniqueName);
    await expect(page.getByRole('cell', { name: uniqueName })).toBeVisible({ timeout: 15_000 });
  });
});

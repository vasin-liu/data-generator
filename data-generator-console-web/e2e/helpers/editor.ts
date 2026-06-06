import type { Page } from '@playwright/test';

/** Opens the new-template wizard from the catalog page. */
export async function openNewTemplateEditor(page: Page): Promise<void> {
  await page.getByTestId('templates-new-button').click();
  await page.waitForURL(/\/templates\/new/);
  await page.getByTestId('template-editor-page').waitFor({ state: 'visible', timeout: 30_000 });
}

/** Sets the template display name on the General step. */
export async function setTemplateName(page: Page, name: string): Promise<void> {
  const input = page.getByTestId('editor-template-name');
  await input.waitFor({ state: 'visible' });
  await input.fill(name);
}

/** Activates an editor tab by localized label (General/Review etc.). */
export async function selectEditorTab(page: Page, label: RegExp): Promise<void> {
  await page.getByRole('tab', { name: label }).click();
  await page.getByRole('tabpanel').first().waitFor({ state: 'visible' });
}

/** Saves the draft and returns to the templates catalog. */
export async function saveTemplateAndReturn(page: Page): Promise<void> {
  await page.getByTestId('review-save-and-return').click();
  await page.waitForURL(/\/templates\/?$/);
  await page.getByTestId('templates-page').waitFor({ state: 'visible' });
}

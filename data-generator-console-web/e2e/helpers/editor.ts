import { expect, type Page } from '@playwright/test';

/** Waits until Review actions are ready after navigation or save. */
async function waitForReviewReady(page: Page): Promise<void> {
  await page.getByTestId('template-editor-page').waitFor({ state: 'visible', timeout: 30_000 });
  await page.getByRole('tab', { name: /review|审阅/i }).click();
  await expect(page.getByTestId('review-save')).toBeEnabled({ timeout: 30_000 });
}

/** Opens the new-template wizard from the catalog page. */
export async function openNewTemplateEditor(page: Page): Promise<void> {
  await page.getByTestId('templates-new-button').click();
  await page.waitForURL(/\/templates\/new/);
  await page.getByTestId('template-editor-page').waitFor({ state: 'visible', timeout: 30_000 });
}

/** Opens the editor seeded from an official scenario catalog entry. */
export async function openScenarioFromCatalog(page: Page, scenarioId: string): Promise<void> {
  await page.getByTestId('templates-from-scenario-button').click();
  const useButton = page.getByTestId(`scenario-use-${scenarioId}`);
  await useButton.waitFor({ state: 'visible', timeout: 30_000 });
  await useButton.click();
  await page.waitForURL(new RegExp(`/templates/new\\?scenario=${scenarioId}`));
  await page.getByTestId('template-editor-page').waitFor({ state: 'visible', timeout: 30_000 });
}

/** Saves the draft from the Review tab and waits for a persisted template URL. */
export async function saveTemplateFromReview(page: Page): Promise<void> {
  await waitForReviewReady(page);
  await page.getByTestId('review-save').click();
  await page.waitForURL(/\/templates\/\d+/, { timeout: 30_000 });
  await waitForReviewReady(page);
  await expect(page.getByTestId('review-publish')).toBeEnabled({ timeout: 30_000 });
}

/** Publishes the current template and starts a run from Review. */
export async function publishAndRunFromReview(page: Page): Promise<void> {
  await waitForReviewReady(page);
  const publish = page.getByTestId('review-publish');
  await expect(publish).toBeEnabled({ timeout: 30_000 });
  await publish.click();
  const confirm = page.getByRole('dialog').filter({
    hasText: /Validate and publish|校验并发布此模板/i,
  });
  await confirm.waitFor({ state: 'visible', timeout: 15_000 });
  await confirm.locator('.ant-btn-primary').click();
  await expect(page.getByTestId('review-run')).toBeEnabled({ timeout: 30_000 });
  await page.getByTestId('review-run').click();
  await page.waitForURL(/\/jobs\/\d+/, { timeout: 90_000 });
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

/** Runs staged preview for a DAG node from the Review tab. */
export async function runStagedDagPreviewFromReview(page: Page, nodeLabel: RegExp): Promise<void> {
  await selectEditorTab(page, /review|审阅/i);
  await expect(page.getByTestId('review-save')).toBeEnabled({ timeout: 30_000 });
  const dagSelect = page.getByTestId('review-preview-dag-select');
  await expect(dagSelect).toBeVisible({ timeout: 30_000 });
  await dagSelect.click();
  const dropdown = page.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)').last();
  await dropdown.waitFor({ state: 'visible', timeout: 10_000 });
  await dropdown
    .locator('.ant-select-item-option')
    .filter({ hasText: nodeLabel })
    .first()
    .click();
  await page.getByTestId('review-preview').click();
  const previewDialog = page.getByRole('dialog').filter({ hasText: /preview|预览/i });
  await expect(previewDialog.locator('pre').first()).toBeVisible({ timeout: 60_000 });
  await expect(previewDialog.locator('pre').first()).not.toContainText(/ERROR:/i);
  await previewDialog.getByRole('button', { name: /ok|确定|知道了/i }).click();
}

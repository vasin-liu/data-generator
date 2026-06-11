import { expect, test } from '@playwright/test';
import { expectApiSuccess, fetchScenarioScaffold, unwrapApiData } from '../helpers/api';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import { openScenarioFromCatalog, publishAndRunFromReview, saveTemplateFromReview } from '../helpers/editor';
import { TestIds } from '../helpers/test-ids';

test.describe('Workflow shared scope scenario', () => {
  test('GF-WFS scaffold includes shared_scope steps and run succeeds', async ({ page, request }) => {
    test.setTimeout(180_000);

    const { res, body } = await fetchScenarioScaffold(request, 'GF-WFS');
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const draft = unwrapApiData<{ draft?: { workflow?: { steps?: Array<{ type?: string }> } } }>(body)?.draft;
    const stepTypes = draft?.workflow?.steps?.map((step) => step.type?.toLowerCase()) ?? [];
    expect(stepTypes.filter((type) => type === 'shared_scope')).toHaveLength(2);
    expect(stepTypes).toContain('branch');
    expect(stepTypes).toContain('invoke_compute_block');

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await openScenarioFromCatalog(page, 'GF-WFS');
    await saveTemplateFromReview(page);
    await publishAndRunFromReview(page);

    await expect(page.getByRole('cell', { name: /成功|Succeeded/i })).toBeVisible({ timeout: 90_000 });
  });
});

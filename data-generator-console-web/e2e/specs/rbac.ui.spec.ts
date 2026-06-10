import { expect, test } from '@playwright/test';
import {
  apiGetWithRole,
  apiPostWithRole,
  expectApiSuccess,
  unwrapApiData,
} from '../helpers/api';
import { gotoConsoleHome, primeConsoleRole } from '../helpers/navigation';

const rbacE2e = process.env.DG_E2E_RBAC === 'true';

type TemplateEditorPayload = {
  templateId?: string | null;
  draft?: Record<string, unknown> & { id?: number | null; name?: string };
};

async function createTemplateFromGfA(
  request: import('@playwright/test').APIRequestContext,
): Promise<string> {
  const { res: scaffoldRes, body: scaffoldBody } = await apiGetWithRole(
    request,
    '/api/templates/scenarios/GF-A/scaffold',
    'EDITOR',
  );
  expect(scaffoldRes.ok()).toBeTruthy();
  expectApiSuccess(scaffoldBody);
  const scaffold = unwrapApiData<TemplateEditorPayload>(scaffoldBody);
  const draft = scaffold?.draft;
  expect(draft).toBeTruthy();
  draft!.name = `e2e-rbac-ui-${Date.now()}`;

  const { res: createRes, body: createBody } = await apiPostWithRole(
    request,
    '/api/templates',
    draft,
    'EDITOR',
  );
  expect(createRes.ok()).toBeTruthy();
  expectApiSuccess(createBody);
  const created = unwrapApiData<TemplateEditorPayload>(createBody);
  const templateId = String(created?.templateId ?? created?.draft?.id ?? '');
  expect(templateId).not.toBe('');
  return templateId;
}

(rbacE2e ? test.describe : test.describe.skip)('RBAC console UI', () => {
  test('role picker is visible on home when RBAC is enabled', async ({ page }) => {
    await gotoConsoleHome(page);
    await expect(page.getByTestId('console-role-select')).toBeVisible();
  });

  test('VIEWER disables review-publish for an existing template', async ({ page, request }) => {
    const templateId = await createTemplateFromGfA(request);

    await primeConsoleRole(page, 'VIEWER');
    await page.goto(`./templates/${templateId}?tab=review`);
    await page.getByTestId('template-editor-page').waitFor({ state: 'visible', timeout: 30_000 });
    await expect(page.getByTestId('review-publish')).toBeDisabled({ timeout: 30_000 });
  });
});

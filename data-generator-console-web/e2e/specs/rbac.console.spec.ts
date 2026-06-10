import { expect, test } from '@playwright/test';
import {
  apiGetWithRole,
  apiPostWithRole,
  expectApiSuccess,
  type ConsoleRoleHeader,
  unwrapApiData,
} from '../helpers/api';

const rbacE2e = process.env.DG_E2E_RBAC === 'true';

type TemplateEditorPayload = {
  templateId?: string | null;
  draft?: Record<string, unknown> & { id?: number | null; name?: string };
};

async function fetchGfAScaffoldDraft(request: import('@playwright/test').APIRequestContext, role: ConsoleRoleHeader) {
  const { res, body } = await apiGetWithRole(
    request,
    '/api/templates/scenarios/GF-A/scaffold',
    role,
  );
  expect(res.ok()).toBeTruthy();
  expectApiSuccess(body);
  const payload = unwrapApiData<TemplateEditorPayload>(body);
  expect(payload?.draft).toBeTruthy();
  return payload!.draft!;
}

(rbacE2e ? test.describe : test.describe.skip)('RBAC console API', () => {
  test('missing role header on GET /api/templates/scenarios returns 403', async ({ request }) => {
    const { res } = await apiGetWithRole(request, '/api/templates/scenarios');
    expect(res.status()).toBe(403);
  });

  test('VIEWER can GET scenarios catalog', async ({ request }) => {
    const { res, body } = await apiGetWithRole(request, '/api/templates/scenarios', 'VIEWER');
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const rows = unwrapApiData<Array<{ scenarioId: string }>>(body);
    expect(rows?.map((row) => row.scenarioId)).toEqual(
      expect.arrayContaining(['GF-A', 'GF-B', 'GF-WF', 'GF-WFS', 'GF-JS']),
    );
  });

  test('VIEWER cannot POST /api/templates', async ({ request }) => {
    const draft = await fetchGfAScaffoldDraft(request, 'VIEWER');
    const { res } = await apiPostWithRole(request, '/api/templates', draft, 'VIEWER');
    expect(res.status()).toBe(403);
  });

  test('EDITOR can create from GF-A scaffold; publish is ADMIN-only', async ({ request }) => {
    const draft = await fetchGfAScaffoldDraft(request, 'EDITOR');
    draft.name = `e2e-rbac-${Date.now()}`;

    const { res: createRes, body: createBody } = await apiPostWithRole(
      request,
      '/api/templates',
      draft,
      'EDITOR',
    );
    expect(createRes.ok()).toBeTruthy();
    expectApiSuccess(createBody);
    const created = unwrapApiData<TemplateEditorPayload>(createBody);
    const templateId = created?.templateId ?? created?.draft?.id;
    expect(templateId).toBeTruthy();

    const { res: editorPublishRes } = await apiPostWithRole(
      request,
      `/api/templates/${templateId}/publish`,
      {},
      'EDITOR',
    );
    expect(editorPublishRes.status()).toBe(403);

    const { res: adminPublishRes, body: adminPublishBody } = await apiPostWithRole(
      request,
      `/api/templates/${templateId}/publish`,
      {},
      'ADMIN',
    );
    expect(adminPublishRes.ok()).toBeTruthy();
    expectApiSuccess(adminPublishBody);
  });
});

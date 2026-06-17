import { expect, test } from '@playwright/test';
import {
  apiGetWithRole,
  apiPostWithRole,
  expectApiSuccess,
  fetchJobs,
  triggerTemplateRun,
  unwrapApiData,
} from '../helpers/api';

type TemplateEditorPayload = {
  templateId?: string | null;
  draft?: Record<string, unknown> & { id?: number | null; name?: string };
};

type RunStartPayload = {
  instanceId?: string | number;
};

test.describe('Job trigger smoke', () => {
  test('triggers template run via console API and returns instance id', async ({ request }) => {
    const { res: scaffoldRes, body: scaffoldBody } = await apiGetWithRole(
      request,
      '/api/templates/scenarios/GF-A/scaffold',
    );
    expect(scaffoldRes.ok()).toBeTruthy();
    expectApiSuccess(scaffoldBody);
    const draft = unwrapApiData<TemplateEditorPayload>(scaffoldBody)?.draft;
    expect(draft).toBeTruthy();
    draft!.name = `e2e-job-trigger-${Date.now()}`;

    const { res: createRes, body: createBody } = await apiPostWithRole(request, '/api/templates', draft);
    expect(createRes.ok()).toBeTruthy();
    expectApiSuccess(createBody);
    const created = unwrapApiData<TemplateEditorPayload>(createBody);
    const templateId = created?.templateId ?? created?.draft?.id;
    expect(templateId).toBeTruthy();

    const { res: publishRes, body: publishBody } = await apiPostWithRole(
      request,
      `/api/templates/${templateId}/publish`,
      {},
    );
    expect(publishRes.ok()).toBeTruthy();
    expectApiSuccess(publishBody);

    const { res: runRes, body: runBody } = await triggerTemplateRun(request, String(templateId));
    expect(runRes.ok()).toBeTruthy();
    expectApiSuccess(runBody);
    const instanceId = String(unwrapApiData<RunStartPayload>(runBody)?.instanceId ?? '');
    expect(instanceId).toBeTruthy();

    const { res: jobsRes, body: jobsBody } = await fetchJobs(request);
    expect(jobsRes.ok()).toBeTruthy();
    expectApiSuccess(jobsBody);
    const jobs = unwrapApiData<Array<{ instanceId?: string | number }>>(jobsBody) ?? [];
    expect(jobs.some((job) => String(job.instanceId) === instanceId)).toBe(true);
  });
});

import { expect, type APIRequestContext } from '@playwright/test';
import {
  apiGetWithRole,
  apiPostWithRole,
  expectApiSuccess,
  fetchJobDetail,
  unwrapApiData,
} from './api';

type TemplateEditorPayload = {
  templateId?: string | null;
  draft?: Record<string, unknown> & { id?: number | null; name?: string };
};

type RunStartPayload = {
  instanceId?: string | number;
};

export type JobSinkMetric = {
  name?: string;
  rowsOk?: number | null;
  rowsFailed?: number | null;
  rowsRead?: number | null;
  rowsUpserted?: number | null;
  rowsSkipped?: number | null;
  errorSample?: string | null;
};

export type JobDetailPayload = {
  execution?: {
    status?: string;
    report?: {
      rowsWritten?: number | null;
      executionMode?: string | null;
      sinks?: JobSinkMetric[];
      transformErrors?: Array<{ message?: string; stage?: string }>;
      errorSamples?: string[];
    };
  };
};

export type PublishRunResult = {
  instanceId: string;
  templateId: string;
};

/**
 * Loads official scenario scaffold, persists draft, publishes, and starts a run.
 *
 * @param request Playwright API context
 * @param scenarioId official catalog id (e.g. GF-A)
 * @returns job instance id
 */
export async function createPublishRunFromScenario(
  request: APIRequestContext,
  scenarioId: string,
): Promise<string> {
  const result = await createPublishRunBundleFromScenario(request, scenarioId);
  return result.instanceId;
}

/**
 * Loads official scenario scaffold, persists draft, publishes, and starts a run.
 *
 * @param request Playwright API context
 * @param scenarioId official catalog id (e.g. GF-FC)
 * @returns job instance id and persisted template id for re-runs
 */
export async function createPublishRunBundleFromScenario(
  request: APIRequestContext,
  scenarioId: string,
): Promise<PublishRunResult> {
  const { res: scaffoldRes, body: scaffoldBody } = await apiGetWithRole(
    request,
    `/api/templates/scenarios/${encodeURIComponent(scenarioId)}/scaffold`,
  );
  expect(scaffoldRes.ok()).toBeTruthy();
  expectApiSuccess(scaffoldBody);
  const draft = unwrapApiData<TemplateEditorPayload>(scaffoldBody)?.draft;
  expect(draft).toBeTruthy();
  draft!.name = `${String(draft!.name ?? scenarioId)}-e2e-${Date.now()}`;

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

  const { res: runRes, body: runBody } = await apiPostWithRole(
    request,
    `/api/templates/${templateId}/run`,
    {},
  );
  expect(runRes.ok()).toBeTruthy();
  expectApiSuccess(runBody);
  const instanceId = String(unwrapApiData<RunStartPayload>(runBody)?.instanceId ?? '');
  expect(instanceId).toBeTruthy();
  return { instanceId, templateId: String(templateId) };
}

/**
 * Triggers another run for an already-published template.
 *
 * @param request Playwright API context
 * @param templateId persisted template id
 * @returns new job instance id
 */
export async function runPublishedTemplate(
  request: APIRequestContext,
  templateId: string,
): Promise<string> {
  const { res: runRes, body: runBody } = await apiPostWithRole(
    request,
    `/api/templates/${templateId}/run`,
    {},
  );
  expect(runRes.ok()).toBeTruthy();
  expectApiSuccess(runBody);
  const instanceId = String(unwrapApiData<RunStartPayload>(runBody)?.instanceId ?? '');
  expect(instanceId).toBeTruthy();
  return instanceId;
}

/**
 * Polls job detail until execution status is SUCCESS.
 *
 * @param request Playwright API context
 * @param instanceId job instance id
 * @param timeoutMs max wait in milliseconds
 */
export async function waitForJobSuccess(
  request: APIRequestContext,
  instanceId: string,
  timeoutMs = 90_000,
): Promise<JobDetailPayload> {
  await expect
    .poll(
      async () => {
        const { body } = await fetchJobDetail(request, instanceId);
        return unwrapApiData<JobDetailPayload>(body)?.execution?.status;
      },
      { timeout: timeoutMs },
    )
    .toBe('SUCCESS');

  const { res, body } = await fetchJobDetail(request, instanceId);
  expect(res.ok()).toBeTruthy();
  expectApiSuccess(body);
  return unwrapApiData<JobDetailPayload>(body) ?? {};
}

/**
 * Polls job detail until execution status is FAILED.
 *
 * @param request Playwright API context
 * @param instanceId job instance id
 * @param timeoutMs max wait in milliseconds
 */
export async function waitForJobFailure(
  request: APIRequestContext,
  instanceId: string,
  timeoutMs = 90_000,
): Promise<JobDetailPayload> {
  await expect
    .poll(
      async () => {
        const { body } = await fetchJobDetail(request, instanceId);
        return unwrapApiData<JobDetailPayload>(body)?.execution?.status;
      },
      { timeout: timeoutMs },
    )
    .toBe('FAILED');

  const { res, body } = await fetchJobDetail(request, instanceId);
  expect(res.ok()).toBeTruthy();
  expectApiSuccess(body);
  return unwrapApiData<JobDetailPayload>(body) ?? {};
}

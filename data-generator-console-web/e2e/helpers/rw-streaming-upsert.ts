import { expect, type APIRequestContext, type Page } from '@playwright/test';
import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';
import { apiGetWithRole, apiPostWithRole, expectApiSuccess, fetchJobDetail, unwrapApiData } from './api';
import { saveTemplateFromReview } from './editor';
import {
  createPublishRunBundleFromScenario,
  runPublishedTemplate,
  waitForJobSuccess,
  type JobDetailPayload,
  type JobSinkMetric,
} from './template-run';

/** Documented OOM fixture bar (D-06) — matches TemplateV2Validator.LARGE_FILE_BYTES. */
export const LARGE_FILE_BYTES = 10 * 1024 * 1024;

type TemplateEditorPayload = {
  templateId?: string | null;
  draft?: Record<string, unknown> & { id?: number | null; name?: string };
};

type ValidationResult = {
  valid?: boolean;
  warnings?: string[];
  errors?: string[];
};

/**
 * Phase 8 RW streaming/upsert E2E helpers (D-23).
 *
 * Large-file IN_MEMORY warning strategy (W-06):
 * - Local service: write a ≥10 MB CSV to OS temp and reference its absolute path in the draft.
 * - Podman/container: fall back to {@code maxRows >= 100_000} on a classpath fixture (same validator bar).
 */
export async function postgresUpsertSupported(request: APIRequestContext): Promise<boolean> {
  const { res, body } = await apiGetWithRole(request, '/api/e2e/scenarios/postgres-upsert-supported');
  if (!res.ok()) {
    return false;
  }
  return Boolean(unwrapApiData<boolean>(body));
}

export async function mutateUpsertSourceBeforeRerun(
  request: APIRequestContext,
  scenarioId: string,
): Promise<void> {
  const { res, body } = await apiPostWithRole(
    request,
    `/api/e2e/scenarios/${encodeURIComponent(scenarioId)}/mutate-upsert-source`,
    {},
  );
  if (res.status() === 404) {
    return;
  }
  expect(res.ok()).toBeTruthy();
  expectApiSuccess(body);
}

export async function runScenarioTwice(
  request: APIRequestContext,
  scenarioId: string,
): Promise<{ first: JobDetailPayload; second: JobDetailPayload; templateId: string }> {
  const bundle = await createPublishRunBundleFromScenario(request, scenarioId);
  const first = await waitForJobSuccess(request, bundle.instanceId);
  await mutateUpsertSourceBeforeRerun(request, scenarioId);
  const secondInstanceId = await runPublishedTemplate(request, bundle.templateId);
  const second = await waitForJobSuccess(request, secondInstanceId);
  return { first, second, templateId: bundle.templateId };
}

/**
 * Asserts extended per-sink metrics on a terminal job detail payload (D-17).
 */
export function assertSinkMetrics(
  detail: JobDetailPayload,
  sinkName: string,
  expected: { rowsOk?: number; rowsUpserted?: number; rowsFailed?: number },
): JobSinkMetric {
  const sinks = detail.execution?.report?.sinks ?? [];
  const sink = sinks.find((row) => row.name === sinkName);
  expect(sink, `sink row ${sinkName}`).toBeTruthy();
  if (expected.rowsOk != null) {
    expect(Number(sink?.rowsOk)).toBe(expected.rowsOk);
  }
  if (expected.rowsUpserted != null) {
    expect(Number(sink?.rowsUpserted)).toBe(expected.rowsUpserted);
  }
  if (expected.rowsFailed != null) {
    expect(Number(sink?.rowsFailed)).toBe(expected.rowsFailed);
  }
  return sink!;
}

/**
 * Writes a minimal valid CSV with padded payload columns to exceed {@link LARGE_FILE_BYTES}.
 *
 * @returns absolute path to the generated file (caller should delete when done)
 */
export function generateLargeCsvFixture(): string {
  const filePath = path.join(os.tmpdir(), `dg-e2e-large-orders-${Date.now()}.csv`);
  const header = 'order_id,customer,amount,payload\n';
  const payload = 'x'.repeat(96);
  const line = `1,ACME,9.99,${payload}\n`;
  const fd = fs.openSync(filePath, 'w');
  try {
    fs.writeSync(fd, header);
    let written = Buffer.byteLength(header, 'utf8');
    while (written < LARGE_FILE_BYTES) {
      fs.writeSync(fd, line);
      written += Buffer.byteLength(line, 'utf8');
    }
  } finally {
    fs.closeSync(fd);
  }
  const size = fs.statSync(filePath).size;
  expect(size).toBeGreaterThanOrEqual(LARGE_FILE_BYTES);
  return filePath;
}

function largeFileStrategy(): 'temp-file' | 'max-rows' {
  if (process.env.DG_E2E_LARGE_FILE_STRATEGY === 'max-rows') {
    return 'max-rows';
  }
  if (process.env.DG_E2E_LARGE_FILE_STRATEGY === 'temp-file') {
    return 'temp-file';
  }
  // Podman/container services cannot read host temp paths unless mounted.
  return process.env.DG_E2E_IN_CONTAINER === 'true' ? 'max-rows' : 'temp-file';
}

/**
 * Builds an IN_MEMORY draft that triggers the large-file validator warning (D-05, D-20).
 */
export function buildInMemoryLargeFileDraft(
  strategy: 'temp-file' | 'max-rows',
  tempCsvPath?: string,
): Record<string, unknown> {
  const incoming: Record<string, unknown> = {
    type: 'csv',
    header: true,
    path: 'template/v2-scenarios/fixtures/streaming-orders.csv',
  };
  if (strategy === 'temp-file' && tempCsvPath) {
    incoming.path = tempCsvPath;
  } else {
    incoming.maxRows = 100_000;
  }
  return {
    name: `e2e-in-memory-large-${Date.now()}`,
    executionPolicy: { mode: 'IN_MEMORY' },
    sources: { incoming },
    transform: {
      type: 'sql',
      sql: 'SELECT order_id, customer, amount FROM incoming',
    },
    sink: {
      writers: [{ type: 'console' }],
    },
  };
}

/**
 * Validates an IN_MEMORY large-file draft via API and returns warnings.
 */
export async function validateInMemoryLargeFileDraft(
  request: APIRequestContext,
  draft: Record<string, unknown>,
): Promise<string[]> {
  const { res, body } = await apiPostWithRole(request, '/api/templates/draft/validate', draft);
  expect(res.ok()).toBeTruthy();
  expectApiSuccess(body);
  const validation = unwrapApiData<ValidationResult>(body);
  return validation?.warnings ?? [];
}

/**
 * Saves an IN_MEMORY large-file draft through the Review UI and expects the warn toast (D-20).
 *
 * Strategy: API-inject draft (temp ≥10 MB path or maxRows bar), then save from Review so
 * {@code message.warning} surfaces validator guidance — no committed 10 MB repo fixture (W-06).
 */
export async function publishLargeFileInMemoryDraft(page: Page, request: APIRequestContext): Promise<void> {
  const strategy = largeFileStrategy();
  let tempPath: string | undefined;
  if (strategy === 'temp-file') {
    tempPath = generateLargeCsvFixture();
  }
  try {
    const draft = buildInMemoryLargeFileDraft(strategy, tempPath);
    const { res, body } = await apiPostWithRole(request, '/api/templates', draft);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const created = unwrapApiData<TemplateEditorPayload>(body);
    const templateId = created?.templateId ?? created?.draft?.id;
    expect(templateId).toBeTruthy();

    await page.goto(`/templates/${templateId}`);
    await saveTemplateFromReview(page);

    const warningToast = page.locator('.ant-message-warning');
    await expect(warningToast).toBeVisible({ timeout: 30_000 });
    const text = (await warningToast.textContent()) ?? '';
    expect(text).toMatch(/CHUNKED|STREAMING/i);
  } finally {
    if (tempPath) {
      fs.unlinkSync(tempPath);
    }
  }
}

/**
 * Finds the first sink row with a non-empty actionable error sample (RW-04).
 */
export function findActionableSinkError(detail: JobDetailPayload): JobSinkMetric | undefined {
  const sinks = detail.execution?.report?.sinks ?? [];
  return sinks.find((row) => {
    const sample = row.errorSample?.trim() ?? '';
    return sample.length > 0 && !sample.startsWith('java.') && !sample.includes(' at org.');
  });
}

/**
 * Fetches terminal job detail by instance id (convenience for UI navigation tests).
 */
export async function fetchTerminalJobDetail(
  request: APIRequestContext,
  instanceId: string,
): Promise<JobDetailPayload> {
  const { res, body } = await fetchJobDetail(request, instanceId);
  expect(res.ok()).toBeTruthy();
  expectApiSuccess(body);
  return unwrapApiData<JobDetailPayload>(body) ?? {};
}

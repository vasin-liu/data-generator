import { expect, test } from '@playwright/test';
import {
  apiGetWithRole,
  apiPostWithRole,
  expectApiSuccess,
  fetchJobDetail,
  fetchTemplateEditor,
  unwrapApiData,
} from '../helpers/api';
import {
  openNewTemplateEditor,
  selectEditorTab,
  setTemplateName,
} from '../helpers/editor';
import { expectJobSucceeded } from '../helpers/job-detail';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import { TestIds } from '../helpers/test-ids';

type TemplateEditorPayload = {
  templateId?: string | null;
  draft?: Record<string, unknown> & { id?: number | null; name?: string };
};

type JobDetailPayload = {
  execution?: {
    status?: string;
    report?: {
      sinks?: Array<{
        name?: string;
        rowsOk?: number | null;
        rowsFailed?: number | null;
        errorSample?: string | null;
      }>;
    };
  };
};

type RunStartPayload = {
  instanceId?: string | number;
};

async function fetchGfEpScaffoldDraft(request: import('@playwright/test').APIRequestContext) {
  const { res, body } = await apiGetWithRole(request, '/api/templates/scenarios/GF-EP/scaffold');
  expect(res.ok()).toBeTruthy();
  expectApiSuccess(body);
  const payload = unwrapApiData<TemplateEditorPayload>(body);
  expect(payload?.draft).toBeTruthy();
  return payload!.draft!;
}

test.describe('Execution reliability (Pack 3)', () => {
  test('partial sink CONTINUE_ON_ERROR shows per-writer rowsOk/rowsFailed on job detail', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);

    const draft = await fetchGfEpScaffoldDraft(request);
    draft.name = `e2e-partial-sink-${Date.now()}`;

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

    await expect
      .poll(
        async () => {
          const { body: detailBody } = await fetchJobDetail(request, instanceId);
          return unwrapApiData<JobDetailPayload>(detailBody)?.execution?.status;
        },
        { timeout: 90_000 },
      )
      .toBe('SUCCESS');

    const { body: detailBody } = await fetchJobDetail(request, instanceId);
    expectApiSuccess(detailBody);
    const sinks = unwrapApiData<JobDetailPayload>(detailBody)?.execution?.report?.sinks ?? [];
    expect(sinks.length).toBeGreaterThanOrEqual(2);

    const failingWriter = sinks.find((row) => row.name === 'sink[0].writer[0]');
    const okWriter = sinks.find((row) => row.name === 'sink[1].writer[0]');
    expect(failingWriter?.rowsFailed).toBe(3);
    expect(failingWriter?.rowsOk ?? 0).toBe(0);
    expect(failingWriter?.errorSample).toBeTruthy();
    expect(okWriter?.rowsOk).toBe(3);
    expect(okWriter?.rowsFailed ?? 0).toBe(0);

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.jobs);
    await page.goto(`./jobs/${instanceId}`);
    await expectJobSucceeded(page);
    await expect(page.getByRole('columnheader', { name: /Rows failed|失败行数/i })).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByRole('columnheader', { name: /Rows OK|成功行数/i })).toBeVisible();
    await expect(page.getByRole('cell', { name: '3' }).first()).toBeVisible();
  });

  test('execution step persists sink retry policy fields', async ({ page, request }) => {
    test.setTimeout(120_000);
    const uniqueName = `e2e-sink-retry-${Date.now()}`;

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await openNewTemplateEditor(page);
    await setTemplateName(page, uniqueName);

    await selectEditorTab(page, /execution|执行/i);

    const sinkModeItem = page.locator('.ant-form-item').filter({ hasText: /sink execution mode|写入执行模式/i });
    await sinkModeItem.getByRole('combobox').click();
    await page
      .locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)')
      .getByText(/continue on error|遇错继续/i)
      .click();

    const maxRetriesItem = page.locator('.ant-form-item').filter({ hasText: /sink max retries|写入最大重试/i });
    await maxRetriesItem.getByRole('spinbutton').fill('4');

    const backoffItem = page.locator('.ant-form-item').filter({
      hasText: /sink retry backoff|写入重试退避/i,
    });
    await backoffItem.getByRole('spinbutton').fill('250');

    await selectEditorTab(page, /review|审阅/i);
    await page.getByTestId('review-save').click();
    await page.waitForURL(/\/templates\/\d+/, { timeout: 30_000 });

    const templateId = page.url().match(/\/templates\/(\d+)/)?.[1];
    expect(templateId).toBeTruthy();

    const { res, body } = await fetchTemplateEditor(request, templateId!);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const data = unwrapApiData<{
      draft?: {
        sinkExecutionPolicy?: { mode?: string; maxRetries?: number; retryBackoffMs?: number };
      };
    }>(body);
    const policy = data?.draft?.sinkExecutionPolicy;
    expect(policy?.mode).toBe('CONTINUE_ON_ERROR');
    expect(policy?.maxRetries).toBe(4);
    expect(policy?.retryBackoffMs).toBe(250);
  });
});

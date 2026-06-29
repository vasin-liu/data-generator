/**
 * Phase 8 UAT — RW streaming CSV/JSON + JDBC upsert Playwright scenarios (D-23).
 *
 * Environment:
 * - DG_E2E_BASE_URL — console base (default http://127.0.0.1:9876/console/)
 * - DG_E2E_API_URL — API origin (default http://127.0.0.1:9876)
 * - DG_E2E_IN_CONTAINER=true — use maxRows large-file warn strategy for Podman (W-06)
 * - DG_E2E_LARGE_FILE_STRATEGY — force temp-file | max-rows for scenario 6
 * - X-Console-Role headers when staging RBAC profile is active (see e2e/helpers/api.ts)
 */
import { expect, test } from '@playwright/test';
import { gotoConsoleHome } from '../helpers/navigation';
import {
  assertSinkMetrics,
  findActionableSinkError,
  postgresUpsertSupported,
  publishLargeFileInMemoryDraft,
  runScenarioTwice,
} from '../helpers/rw-streaming-upsert';
import {
  createPublishRunFromScenario,
  waitForJobSuccess,
} from '../helpers/template-run';

test.describe('RW streaming & upsert (Phase 8)', () => {
  test('D-23 #1 API: large CSV CHUNKED run succeeds with rows written', async ({ request }) => {
    test.setTimeout(120_000);
    const instanceId = await createPublishRunFromScenario(request, 'GF-FC');
    const detail = await waitForJobSuccess(request, instanceId);
    expect(detail.execution?.report?.executionMode).toBe('CHUNKED');
    expect(Number(detail.execution?.report?.rowsWritten ?? 0)).toBeGreaterThan(0);
  });

  test('D-23 #2 API: PostgreSQL upsert idempotent re-run', async ({ request }) => {
    test.setTimeout(180_000);
    const supported = await postgresUpsertSupported(request);
    test.skip(!supported, 'H2 lacks PostgreSQL ON CONFLICT upsert (W-01)');
    const { first, second } = await runScenarioTwice(request, 'GF-GP');
    const firstWritten = Number(first.execution?.report?.rowsWritten ?? 0);
    const secondWritten = Number(second.execution?.report?.rowsWritten ?? 0);
    expect(firstWritten).toBeGreaterThan(0);
    expect(secondWritten).toBe(firstWritten);

    const sink = second.execution?.report?.sinks?.find((row) => row.name === 'sink[0].writer[0]');
    expect(Number(sink?.rowsUpserted ?? 0)).toBeGreaterThan(0);
  });

  test('D-23 #3 API: partial sink failure shows actionable errorSample', async ({ request }) => {
    test.setTimeout(180_000);
    const instanceId = await createPublishRunFromScenario(request, 'GF-EP');
    const detail = await waitForJobSuccess(request, instanceId);
    const failing = assertSinkMetrics(detail, 'sink[0].writer[0]', { rowsFailed: 3 });
    const sample = failing.errorSample ?? '';
    expect(sample.length).toBeGreaterThan(0);
    expect(sample).toContain('sink[0].writer[0]');
    expect(sample.toLowerCase()).toContain('jdbc');
    expect(sample).toContain('__missing_sink_target__');
    expect(sample).not.toMatch(/^\s*at\s+org\./);
  });

  test('D-23 #4 API: NDJSON STREAMING run succeeds', async ({ request }) => {
    test.setTimeout(120_000);
    const instanceId = await createPublishRunFromScenario(request, 'GF-FN');
    const detail = await waitForJobSuccess(request, instanceId);
    expect(detail.execution?.report?.executionMode).toBe('STREAMING');
    expect(Number(detail.execution?.report?.rowsWritten ?? 0)).toBeGreaterThan(0);
  });

  test('D-23 #5 API: MySQL upsert idempotent re-run', async ({ request }) => {
    test.setTimeout(180_000);
    const { first, second } = await runScenarioTwice(request, 'GF-GM');
    const firstWritten = Number(first.execution?.report?.rowsWritten ?? 0);
    const secondWritten = Number(second.execution?.report?.rowsWritten ?? 0);
    expect(firstWritten).toBeGreaterThan(0);
    expect(secondWritten).toBe(firstWritten);

    const sink = second.execution?.report?.sinks?.find((row) => row.name === 'sink[0].writer[0]');
    expect(Number(sink?.rowsUpserted ?? 0)).toBeGreaterThan(0);
  });

  test('D-23 #6 UI: IN_MEMORY large file publish shows CHUNKED/STREAMING warning', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    await gotoConsoleHome(page);
    await publishLargeFileInMemoryDraft(page, request);
  });

  test('D-17 UI: job detail sink table shows rowsOk after CSV CHUNKED run', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const instanceId = await createPublishRunFromScenario(request, 'GF-FC');
    const detail = await waitForJobSuccess(request, instanceId);
    const sink = findActionableSinkError(detail);
    // Console sink on GF-FC should succeed without sink errors.
    expect(sink).toBeUndefined();
    const okSink = detail.execution?.report?.sinks?.find((row) => row.name === 'sink[0].writer[0]');
    expect(Number(okSink?.rowsOk ?? 0)).toBeGreaterThan(0);

    await page.goto(`/jobs/${instanceId}`);
    const jobPage = page.getByTestId('job-detail-page');
    await expect(jobPage).toBeVisible({ timeout: 30_000 });
    const sinkTable = jobPage.locator('.ant-table').filter({ hasText: /sink|输出/i }).last();
    await expect(sinkTable).toBeVisible({ timeout: 30_000 });
    const sinkRow = sinkTable.locator('tbody tr').filter({ hasText: 'sink[0].writer[0]' });
    await expect(sinkRow).toBeVisible();
    await expect(sinkRow).toContainText(String(okSink?.rowsOk));
  });
});

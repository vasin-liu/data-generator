import { expect, test } from '@playwright/test';
import {
  expectApiSuccess,
  fetchConsoleRuntime,
  fetchDatasourcesOverview,
  fetchEditorScaffold,
  fetchJobs,
  fetchSchedules,
  fetchTemplateTaxonomy,
  fetchTemplates,
  unwrapApiData,
} from '../helpers/api';

test.describe('API / console facades', () => {
  test('GET /api/console/runtime', async ({ request }) => {
    const { res, body } = await fetchConsoleRuntime(request);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const data = unwrapApiData<Record<string, unknown>>(body);
    expect(data).not.toBeNull();
    expect(data).toHaveProperty('v1ExecutionEnabled');
  });

  test('GET /api/templates', async ({ request }) => {
    const { res, body } = await fetchTemplates(request);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    expect(Array.isArray(unwrapApiData(body))).toBe(true);
  });

  test('GET /api/templates/taxonomy', async ({ request }) => {
    const { res, body } = await fetchTemplateTaxonomy(request);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const data = unwrapApiData<{ categories?: unknown; tags?: unknown }>(body);
    expect(data).not.toBeNull();
  });

  test('GET /api/templates/scaffold', async ({ request }) => {
    const { res, body } = await fetchEditorScaffold(request);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const data = unwrapApiData<{ draft?: unknown }>(body);
    expect(data?.draft).toBeTruthy();
  });

  test('GET /api/datasources', async ({ request }) => {
    const { res, body } = await fetchDatasourcesOverview(request);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    expect(unwrapApiData(body)).not.toBeNull();
  });

  test('GET /api/jobs', async ({ request }) => {
    const { res, body } = await fetchJobs(request);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    expect(Array.isArray(unwrapApiData(body))).toBe(true);
  });

  test('GET /api/console/schedules', async ({ request }) => {
    const { res, body } = await fetchSchedules(request);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    expect(Array.isArray(unwrapApiData(body))).toBe(true);
  });
});

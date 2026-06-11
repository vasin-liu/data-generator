import { expect, test } from '@playwright/test';
import {
  expectApiSuccess,
  fetchConsoleRuntime,
  fetchDatasourcesOverview,
  fetchEditorScaffold,
  fetchScenarioCatalog,
  fetchScenarioScaffold,
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

  test('GET /api/templates/scenarios lists official catalog', async ({ request }) => {
    const { res, body } = await fetchScenarioCatalog(request);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const rows = unwrapApiData<Array<{ scenarioId: string }>>(body);
    expect(rows?.map((row) => row.scenarioId)).toEqual(
      expect.arrayContaining(['GF-A', 'GF-B', 'GF-BJ', 'GF-WF', 'GF-WFS', 'GF-JS']),
    );
  });

  test('GET /api/templates/scenarios/GF-A/scaffold seeds draft', async ({ request }) => {
    const { res, body } = await fetchScenarioScaffold(request, 'GF-A');
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const data = unwrapApiData<{ draft?: { sources?: unknown } }>(body);
    expect(data?.draft?.sources).toBeTruthy();
  });

  test('GET /api/datasources', async ({ request }) => {
    const { res, body } = await fetchDatasourcesOverview(request);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const overview = unwrapApiData<{
      kafkaPersisted?: unknown[];
      elasticsearchPersisted?: unknown[];
      kafkaClusters?: unknown[];
      elasticsearchClusters?: unknown[];
    }>(body);
    expect(overview).not.toBeNull();
    expect(Array.isArray(overview?.kafkaPersisted)).toBe(true);
    expect(Array.isArray(overview?.elasticsearchPersisted)).toBe(true);
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

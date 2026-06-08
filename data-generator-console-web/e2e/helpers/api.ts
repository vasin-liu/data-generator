import type { APIRequestContext } from '@playwright/test';

export function apiBaseUrl(): string {
  return (process.env.DG_E2E_API_URL ?? 'http://127.0.0.1:9876').replace(/\/$/, '');
}

export async function fetchHealth(request: APIRequestContext) {
  const res = await request.get(`${apiBaseUrl()}/healthz`);
  return { res, body: res.ok() ? await res.json() : null };
}

export async function fetchConsoleRuntime(request: APIRequestContext) {
  const res = await request.get(`${apiBaseUrl()}/api/console/runtime`);
  return { res, body: res.ok() ? await res.json() : null };
}

export async function fetchTemplates(request: APIRequestContext) {
  const res = await request.get(`${apiBaseUrl()}/api/templates?includeArchived=false`);
  return { res, body: res.ok() ? await res.json() : null };
}

export async function fetchDatasourcesOverview(request: APIRequestContext) {
  const res = await request.get(`${apiBaseUrl()}/api/datasources`);
  return { res, body: res.ok() ? await res.json() : null };
}

export async function fetchJobs(request: APIRequestContext) {
  const res = await request.get(`${apiBaseUrl()}/api/jobs`);
  return { res, body: res.ok() ? await res.json() : null };
}

export async function fetchSchedules(request: APIRequestContext) {
  const res = await request.get(`${apiBaseUrl()}/api/console/schedules`);
  return { res, body: res.ok() ? await res.json() : null };
}

export async function fetchEditorScaffold(request: APIRequestContext) {
  const res = await request.get(`${apiBaseUrl()}/api/templates/scaffold`);
  return { res, body: res.ok() ? await res.json() : null };
}

export async function fetchScenarioCatalog(request: APIRequestContext) {
  const res = await request.get(`${apiBaseUrl()}/api/templates/scenarios`);
  return { res, body: res.ok() ? await res.json() : null };
}

export async function fetchScenarioScaffold(request: APIRequestContext, scenarioId: string) {
  const res = await request.get(
    `${apiBaseUrl()}/api/templates/scenarios/${encodeURIComponent(scenarioId)}/scaffold`,
  );
  return { res, body: res.ok() ? await res.json() : null };
}

export async function fetchTemplateEditor(request: APIRequestContext, templateId: string) {
  const res = await request.get(`${apiBaseUrl()}/api/templates/${templateId}`);
  return { res, body: res.ok() ? await res.json() : null };
}

export async function fetchTemplateTaxonomy(request: APIRequestContext) {
  const res = await request.get(`${apiBaseUrl()}/api/templates/taxonomy`);
  return { res, body: res.ok() ? await res.json() : null };
}

/** Parses standard `{ success, data }` API envelope when present. */
export function unwrapApiData<T>(body: unknown): T | null {
  if (body && typeof body === 'object' && 'success' in body) {
    const envelope = body as { success?: boolean; data?: T };
    return envelope.success ? (envelope.data ?? null) : null;
  }
  return body as T;
}

export function expectApiSuccess(body: unknown): void {
  if (body && typeof body === 'object' && 'success' in body) {
    const envelope = body as { success?: boolean; message?: string };
    if (!envelope.success) {
      throw new Error(envelope.message ?? 'API returned success=false');
    }
  }
}

import type { APIRequestContext } from '@playwright/test';

export function apiBaseUrl(): string {
  return (process.env.DG_E2E_API_URL ?? 'http://127.0.0.1:9876').replace(/\/$/, '');
}


export type ConsoleRoleHeader = 'VIEWER' | 'EDITOR' | 'OPERATOR' | 'DATASOURCE_ADMIN' | 'ADMIN';

export function consoleRoleHeaders(role?: ConsoleRoleHeader): Record<string, string> {
  if (!role) {
    return {};
  }
  return {
    'X-Console-Role': role,
  };
}

export async function apiGetWithRole(
  request: APIRequestContext,
  path: string,
  role?: ConsoleRoleHeader,
) {
  const res = await request.get(`${apiBaseUrl()}${path}`, {
    headers: consoleRoleHeaders(role),
  });
  const contentType = res.headers()['content-type'] ?? '';
  const body = contentType.includes('json') ? await res.json().catch(() => null) : null;
  return { res, body };
}

export async function apiPostWithRole(
  request: APIRequestContext,
  path: string,
  data: unknown,
  role?: ConsoleRoleHeader,
) {
  const res = await request.post(`${apiBaseUrl()}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...consoleRoleHeaders(role),
    },
    data,
  });
  const contentType = res.headers()['content-type'] ?? '';
  const body = contentType.includes('json') ? await res.json().catch(() => null) : null;
  return { res, body };
}

export async function apiDeleteWithRole(
  request: APIRequestContext,
  path: string,
  role?: ConsoleRoleHeader,
) {
  const res = await request.delete(`${apiBaseUrl()}${path}`, {
    headers: consoleRoleHeaders(role),
  });
  const contentType = res.headers()['content-type'] ?? '';
  const body = contentType.includes('json') ? await res.json().catch(() => null) : null;
  return { res, body };
}

/** JDBC datasource upsert (console {@code POST /api/datasources}). */
export async function upsertJdbcDataSourceMultipart(
  request: APIRequestContext,
  fields: {
    name: string;
    url: string;
    username: string;
    password: string;
    driverClassName: string;
  },
  role?: ConsoleRoleHeader,
) {
  const res = await request.post(`${apiBaseUrl()}/api/datasources`, {
    headers: consoleRoleHeaders(role),
    form: {
      name: fields.name,
      url: fields.url,
      username: fields.username,
      password: fields.password,
      driverClassName: fields.driverClassName,
    },
  });
  const contentType = res.headers()['content-type'] ?? '';
  const body = contentType.includes('json') ? await res.json().catch(() => null) : null;
  return { res, body };
}

/** Tests a persisted JDBC datasource by name ({@code POST /api/datasources/{name}/test}). */
export async function testJdbcDataSourceByName(
  request: APIRequestContext,
  name: string,
  role?: ConsoleRoleHeader,
) {
  return apiPostWithRole(request, `/api/datasources/${encodeURIComponent(name)}/test`, {}, role);
}

/** Removes a persisted JDBC datasource ({@code DELETE /api/datasources/{name}}). */
export async function deleteJdbcDataSource(
  request: APIRequestContext,
  name: string,
  role?: ConsoleRoleHeader,
) {
  return apiDeleteWithRole(request, `/api/datasources/${encodeURIComponent(name)}`, role);
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

/** Lists console audit events ({@code GET /api/console/audit}). */
export async function fetchAuditEvents(
  request: APIRequestContext,
  options?: { category?: string; resourceId?: string; action?: string; limit?: number },
  role?: ConsoleRoleHeader,
) {
  const params = new URLSearchParams();
  if (options?.category?.trim()) {
    params.set('category', options.category.trim());
  }
  if (options?.resourceId?.trim()) {
    params.set('resourceId', options.resourceId.trim());
  }
  if (options?.action?.trim()) {
    params.set('action', options.action.trim());
  }
  params.set('limit', String(options?.limit ?? 100));
  const query = params.toString();
  return apiGetWithRole(request, `/api/console/audit?${query}`, role);
}

/** Unified JDBC/Kafka/ES connectivity test ({@code POST /api/datasources/connections/test}). */
export async function testConnectionUnifiedApi(
  request: APIRequestContext,
  payload: {
    kind: string;
    name?: string;
    draftPayload?: Record<string, unknown>;
  },
  role?: ConsoleRoleHeader,
) {
  return apiPostWithRole(request, '/api/datasources/connections/test', payload, role);
}

/** Legacy task execution summary ({@code GET /task/executions/{instanceId}}). */
export async function fetchTaskExecution(
  request: APIRequestContext,
  instanceId: string,
  role?: ConsoleRoleHeader,
) {
  return apiGetWithRole(request, `/task/executions/${encodeURIComponent(instanceId)}`, role);
}

export async function fetchDistributedMetrics(request: APIRequestContext) {
  const res = await request.get(`${apiBaseUrl()}/api/console/distributed/metrics`);
  return { res, body: res.ok() ? await res.json() : null };
}

export async function fetchJobDetail(request: APIRequestContext, instanceId: string) {
  const res = await request.get(`${apiBaseUrl()}/api/jobs/${encodeURIComponent(instanceId)}`);
  return { res, body: res.ok() ? await res.json() : null };
}

export async function fetchJobs(request: APIRequestContext) {
  const res = await request.get(`${apiBaseUrl()}/api/jobs`);
  return { res, body: res.ok() ? await res.json() : null };
}

export async function triggerTemplateRun(request: APIRequestContext, templateId: string) {
  return apiPostWithRole(request, `/api/templates/${encodeURIComponent(templateId)}/run`, {});
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

/** Registers a managed Kafka cluster ({@code POST /api/datasources/kafka-clusters}). */
export async function registerKafkaCluster(
  request: APIRequestContext,
  fields: {
    name: string;
    bootstrapServers: string[];
    clientId?: string;
    acks?: string;
  },
  role?: ConsoleRoleHeader,
) {
  return apiPostWithRole(request, '/api/datasources/kafka-clusters', fields, role);
}

/** Registers a managed Elasticsearch cluster ({@code POST /api/datasources/elasticsearch-clusters}). */
export async function registerElasticsearchCluster(
  request: APIRequestContext,
  fields: {
    name: string;
    uris: string[];
    connectionTimeoutMs?: number;
    socketTimeoutMs?: number;
  },
  role?: ConsoleRoleHeader,
) {
  return apiPostWithRole(request, '/api/datasources/elasticsearch-clusters', fields, role);
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

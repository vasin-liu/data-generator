import { expect, type APIRequestContext } from '@playwright/test';
import {
  apiGetWithRole,
  apiPostWithRole,
  expectApiSuccess,
  fetchDatasourcesOverview,
  fetchJobDetail,
  testJdbcDataSourceByName,
  unwrapApiData,
  upsertJdbcDataSourceMultipart,
} from './api';

export type DatasourceGovernanceFlags = {
  requireConnectivityTestBeforeSave?: boolean;
  requireConnectivityTestBeforePublish?: boolean;
};

export type CatalogConnectionView = {
  name: string;
  kind: string;
  source?: string;
  healthStatus?: string;
  lastReloadAt?: string | null;
  degradedReason?: string | null;
  version?: number;
};

export type DataSourcesOverviewView = {
  persisted?: Array<{ name: string; url?: string; driverClassName?: string; driverPresetId?: string | null }>;
  catalogConnections?: CatalogConnectionView[];
  governance?: DatasourceGovernanceFlags;
  driverPresets?: Array<{ id: string; driverClassName: string }>;
};

export type ExecutionSnapshotView = {
  status: string;
  startedAt?: string | null;
  pauseReason?: string | null;
  metricsJson?: string | null;
};

export type AuditEventView = {
  action?: string;
  resourceType?: string;
  resourceId?: string;
  occurredAt?: string;
};

/** JDBC fields for embedded H2 governance E2E fixtures. */
export function jdbcFixtureFields(name: string, memSuffix?: string) {
  const mem = memSuffix ?? name;
  return {
    name,
    url: `jdbc:h2:mem:${mem};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`,
    username: 'sa',
    password: '',
    driverClassName: 'org.h2.Driver',
  };
}

/**
 * Loads governance flags exposed on the datasources overview API.
 */
export async function fetchGovernanceFlags(
  request: APIRequestContext,
): Promise<DatasourceGovernanceFlags> {
  const { body } = await fetchDatasourcesOverview(request);
  expectApiSuccess(body);
  return unwrapApiData<DataSourcesOverviewView>(body)?.governance ?? {};
}

/**
 * Unified connectivity test for draft or persisted connections.
 */
export async function testConnectionUnified(
  request: APIRequestContext,
  payload: {
    kind: 'JDBC' | 'KAFKA' | 'ELASTICSEARCH';
    name?: string;
    draftPayload?: Record<string, unknown>;
  },
) {
  return apiPostWithRole(request, '/api/datasources/connections/test', payload);
}

/**
 * Saves a healthy JDBC datasource then updates with a broken driver to force DEGRADED reload (D-11).
 */
export async function forceReloadFailure(
  request: APIRequestContext,
  name: string,
  healthyMemSuffix?: string,
): Promise<CatalogConnectionView> {
  const healthy = jdbcFixtureFields(name, healthyMemSuffix ?? `${name}-good`);
  const save = await upsertJdbcDataSourceMultipart(request, healthy);
  expect(save.res.ok()).toBeTruthy();
  expectApiSuccess(save.body);

  const broken = await upsertJdbcDataSourceMultipart(request, {
    ...healthy,
    driverClassName: 'com.example.NonexistentDriver',
  });
  expect(broken.res.ok()).toBeTruthy();
  expectApiSuccess(broken.body);

  const catalog = await fetchCatalogConnection(request, name, 'JDBC');
  expect(catalog?.healthStatus).toBe('DEGRADED');
  return catalog!;
}

/**
 * Polls until execution is in-flight ({@code RUNNING} or manual {@code PAUSED}).
 */
export async function waitForExecutionRunning(
  request: APIRequestContext,
  instanceId: string,
  timeoutMs = 90_000,
): Promise<ExecutionSnapshotView> {
  let last: ExecutionSnapshotView | undefined;
  await expect
    .poll(
      async () => {
        last = await getConnectionSnapshot(request, instanceId);
        return last.status;
      },
      { timeout: timeoutMs },
    )
    .toMatch(/^(RUNNING|PAUSED)$/);
  return last!;
}

/**
 * Reads in-flight execution summary via legacy task API (proxy for snapshot isolation checks, D-10).
 */
export async function getConnectionSnapshot(
  request: APIRequestContext,
  instanceId: string,
): Promise<ExecutionSnapshotView> {
  const { res, body } = await apiGetWithRole(request, `/task/executions/${encodeURIComponent(instanceId)}`);
  expect(res.ok()).toBeTruthy();
  expectApiSuccess(body);
  const execution = unwrapApiData<ExecutionSnapshotView>(body);
  expect(execution?.status).toBeTruthy();
  return execution!;
}

/**
 * Lists audit events with optional datasource category / resource filters (D-25).
 */
export async function listAuditEvents(
  request: APIRequestContext,
  options?: { category?: string; resourceId?: string; action?: string; limit?: number },
): Promise<AuditEventView[]> {
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
  const { res, body } = await apiGetWithRole(request, `/api/console/audit?${query}`);
  expect(res.ok()).toBeTruthy();
  expectApiSuccess(body);
  return unwrapApiData<AuditEventView[]>(body) ?? [];
}

/** Returns one catalog row by kind:name key. */
export async function fetchCatalogConnection(
  request: APIRequestContext,
  name: string,
  kind: string,
): Promise<CatalogConnectionView | undefined> {
  const { body } = await fetchDatasourcesOverview(request);
  expectApiSuccess(body);
  const overview = unwrapApiData<DataSourcesOverviewView>(body);
  return overview?.catalogConnections?.find((row) => row.name === name && row.kind === kind);
}

/** Resumes a workflow blocked on manual pause. */
export async function resumeExecution(request: APIRequestContext, instanceId: string): Promise<void> {
  const { res, body } = await apiPostWithRole(
    request,
    `/task/executions/${encodeURIComponent(instanceId)}/resume`,
    {},
  );
  expect(res.ok()).toBeTruthy();
  expectApiSuccess(body);
}

/**
 * V2 draft with manual workflow pause and JDBC query source referencing a managed datasource.
 */
export function buildManualPauseJdbcTemplate(
  templateName: string,
  dataSourceId: string,
): Record<string, unknown> {
  return {
    name: templateName,
    definitionKind: 'V2',
    workflow: {
      steps: [
        { type: 'pause', id: 'pause-governance', manual: true },
        {
          type: 'invoke_compute_block',
          id: 'invoke-query',
          computeBlockId: 'query-block',
        },
      ],
    },
    computeBlocks: [
      {
        id: 'query-block',
        sources: {
          src: {
            type: 'query',
            dataSourceId,
            sql: 'SELECT 1 AS value',
          },
        },
        transformers: [{ type: 'sql', sql: 'SELECT value FROM src' }],
        sink: {
          writers: [{ type: 'console' }],
        },
      },
    ],
  };
}

/** Inline JDBC template used to trigger managed-only governance on publish (D-13). */
export function buildInlineJdbcTemplate(templateName: string): Record<string, unknown> {
  return {
    name: templateName,
    definitionKind: 'V2',
    sources: {
      src: {
        type: 'query',
        sql: 'SELECT 1 AS value',
        dataSource: {
          name: 'inline-gov',
          url: 'jdbc:h2:mem:inline-gov;DB_CLOSE_DELAY=-1',
          username: 'sa',
          password: '',
          driverClassName: 'org.h2.Driver',
        },
      },
    },
    transform: { type: 'sql', sql: 'SELECT value FROM src' },
    sink: { writers: [{ type: 'console' }] },
  };
}

/**
 * Creates, publishes, and starts a run; returns instance id.
 */
export async function publishAndRunDraft(
  request: APIRequestContext,
  draft: Record<string, unknown>,
): Promise<string> {
  const { res: createRes, body: createBody } = await apiPostWithRole(request, '/api/templates', draft);
  expect(createRes.ok()).toBeTruthy();
  expectApiSuccess(createBody);
  const created = unwrapApiData<{ templateId?: string | number; draft?: { id?: number | null } }>(createBody);
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
  const instanceId = String(unwrapApiData<{ instanceId?: string | number }>(runBody)?.instanceId ?? '');
  expect(instanceId).toBeTruthy();
  return instanceId;
}

/** Asserts JDBC test-by-name succeeds for a persisted row. */
export async function expectJdbcTestOk(request: APIRequestContext, name: string): Promise<void> {
  const test = await testJdbcDataSourceByName(request, name);
  expect(test.res.ok()).toBeTruthy();
  expectApiSuccess(test.body);
}

/** Polls job detail until SUCCESS. */
export async function waitForJobDetailSuccess(
  request: APIRequestContext,
  instanceId: string,
  timeoutMs = 120_000,
): Promise<void> {
  await expect
    .poll(
      async () => {
        const { body } = await fetchJobDetail(request, instanceId);
        return unwrapApiData<{ execution?: { status?: string } }>(body)?.execution?.status;
      },
      { timeout: timeoutMs },
    )
    .toBe('SUCCESS');
}

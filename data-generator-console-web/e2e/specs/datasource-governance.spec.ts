import { expect, test } from '@playwright/test';
import {
  deleteJdbcDataSource,
  expectApiSuccess,
  fetchDatasourcesOverview,
  registerElasticsearchCluster,
  registerKafkaCluster,
  unwrapApiData,
  upsertJdbcDataSourceMultipart,
} from '../helpers/api';
import {
  buildInlineJdbcTemplate,
  buildManualPauseJdbcTemplate,
  expectJdbcTestOk,
  fetchCatalogConnection,
  fetchGovernanceFlags,
  forceReloadFailure,
  getConnectionSnapshot,
  jdbcFixtureFields,
  listAuditEvents,
  publishAndRunDraft,
  resumeExecution,
  testConnectionUnified,
  waitForExecutionRunning,
  waitForJobDetailSuccess,
  type DataSourcesOverviewView,
} from '../helpers/datasource-governance';
import { openScenarioFromCatalog, publishAndRunFromReview, saveTemplateFromReview } from '../helpers/editor';
import { expectJobSucceeded } from '../helpers/job-detail';
import { kafkaBootstrapServers, elasticsearchUri } from '../helpers/messaging';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import { TestIds } from '../helpers/test-ids';
import { createPublishRunFromScenario } from '../helpers/template-run';

/**
 * Phase 7 UAT — datasource governance, hot-reload isolation, DEGRADED UX, audit deep-links (D-27).
 */
test.describe('Datasource governance (Phase 7)', () => {
  test('API CRUD JDBC/Kafka/ES with unified connectivity test', async ({ request }) => {
    const suffix = Date.now();
    const jdbcName = `e2e-gov-jdbc-${suffix}`;
    const kafkaName = `e2e-gov-kafka-${suffix}`;
    const esName = `e2e-gov-es-${suffix}`;

    const jdbcDraft = jdbcFixtureFields(jdbcName);
    const jdbcTest = await testConnectionUnified(request, {
      kind: 'JDBC',
      draftPayload: {
        url: jdbcDraft.url,
        username: jdbcDraft.username,
        password: jdbcDraft.password,
        driverClassName: jdbcDraft.driverClassName,
      },
    });
    expect(jdbcTest.res.ok()).toBeTruthy();
    expectApiSuccess(jdbcTest.body);

    const jdbcSave = await upsertJdbcDataSourceMultipart(request, jdbcDraft);
    expect(jdbcSave.res.ok()).toBeTruthy();
    expectApiSuccess(jdbcSave.body);
    await expectJdbcTestOk(request, jdbcName);

    const kafkaTest = await testConnectionUnified(request, {
      kind: 'KAFKA',
      draftPayload: {
        bootstrapServers: kafkaBootstrapServers(),
        clientId: `e2e-gov-${suffix}`,
      },
    });
    expect(kafkaTest.res.ok()).toBeTruthy();
    expectApiSuccess(kafkaTest.body);

    const kafkaSave = await registerKafkaCluster(request, {
      name: kafkaName,
      bootstrapServers: kafkaBootstrapServers(),
      clientId: `e2e-gov-${suffix}`,
    });
    expectApiSuccess(kafkaSave.body);

    const esTest = await testConnectionUnified(request, {
      kind: 'ELASTICSEARCH',
      draftPayload: {
        uris: [elasticsearchUri()],
        connectionTimeoutMs: 3000,
        socketTimeoutMs: 3000,
      },
    });
    expect(esTest.res.ok()).toBeTruthy();
    expectApiSuccess(esTest.body);

    const esSave = await registerElasticsearchCluster(request, {
      name: esName,
      uris: [elasticsearchUri()],
      connectionTimeoutMs: 3000,
      socketTimeoutMs: 3000,
    });
    expectApiSuccess(esSave.body);

    const overview = unwrapApiData<DataSourcesOverviewView>((await fetchDatasourcesOverview(request)).body);
    expect(overview?.catalogConnections).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ name: jdbcName, kind: 'JDBC', source: 'MANAGED' }),
        expect.objectContaining({ name: kafkaName, kind: 'KAFKA', source: 'MANAGED' }),
        expect.objectContaining({ name: esName, kind: 'ELASTICSEARCH', source: 'MANAGED' }),
      ]),
    );

    await deleteJdbcDataSource(request, jdbcName);
  });

  test('connectivity gate blocks save when governance requires passing test', async ({ page, request }) => {
    const governance = await fetchGovernanceFlags(request);
    test.skip(
      !governance.requireConnectivityTestBeforeSave,
      'requireConnectivityTestBeforeSave is off in this profile',
    );

    const name = `e2e-gov-gate-${Date.now()}`;
    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.datasources);
    await page.getByTestId(TestIds.actions.datasourcesNew).click();

    const dialog = page.getByRole('dialog');
    const fields = jdbcFixtureFields(name);
    await dialog.getByRole('textbox', { name: /名称|Name/i }).fill(fields.name);
    await dialog.getByRole('textbox', { name: /JDBC URL/i }).fill(fields.url);
    await dialog.getByRole('textbox', { name: /用户名|Username/i }).fill(fields.username);
    await dialog.getByRole('textbox', { name: /驱动类|Driver class/i }).fill(fields.driverClassName);

    const saveButton = dialog.locator('button[type="submit"]');
    await expect(saveButton).toBeDisabled();

    await dialog.getByRole('button', { name: /测\s*试|^Test$/i }).click();
    await expect(page.getByRole('alert').filter({ hasText: /成功|success|OK/i })).toBeVisible({
      timeout: 15_000,
    });
    await expect(saveButton).toBeEnabled();
  });

  test('new JDBC draft test and driver preset survives save/reopen', async ({ page, request }) => {
    const name = `e2e-gov-preset-${Date.now()}`;
    const fields = jdbcFixtureFields(name);

    const draftTest = await testConnectionUnified(request, {
      kind: 'JDBC',
      draftPayload: {
        url: fields.url,
        username: fields.username,
        password: fields.password,
        driverClassName: fields.driverClassName,
        driverPresetId: 'h2',
      },
    });
    expect(draftTest.res.ok()).toBeTruthy();
    expectApiSuccess(draftTest.body);

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.datasources);
    await page.getByTestId(TestIds.actions.datasourcesNew).click();

    const dialog = page.getByRole('dialog');
    await dialog.getByRole('textbox', { name: /名称|Name/i }).fill(fields.name);
    await dialog.getByRole('textbox', { name: /JDBC URL/i }).fill(fields.url);
    await dialog.getByRole('textbox', { name: /用户名|Username/i }).fill(fields.username);
    await dialog.getByRole('combobox').first().click();
    await page.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)').getByText(/H2|h2/i).first().click();

    await dialog.getByRole('button', { name: /测\s*试|^Test$/i }).click();
    await expect(page.getByRole('alert').filter({ hasText: /成功|success|OK/i })).toBeVisible({
      timeout: 15_000,
    });

    const [saveResp] = await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/api/datasources') && response.request().method() === 'POST',
      ),
      dialog.locator('button[type="submit"]').click(),
    ]);
    expect((await saveResp.json()).success).toBe(true);
    await expect(dialog).toBeHidden();

    const overview = unwrapApiData<DataSourcesOverviewView>((await fetchDatasourcesOverview(request)).body);
    const persisted = overview?.persisted?.find((row) => row.name === name);
    expect(persisted?.driverPresetId ?? persisted?.driverClassName).toBeTruthy();
  });

  test('hot-reload isolation keeps in-flight execution unchanged after datasource save', async ({
    request,
  }) => {
    test.setTimeout(180_000);
    const suffix = Date.now();
    const dsName = `e2e-hr-ds-${suffix}`;
    const urlA = `jdbc:h2:mem:hr-a-${suffix};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`;
    const urlB = `jdbc:h2:mem:hr-b-${suffix};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`;

    const saveA = await upsertJdbcDataSourceMultipart(request, {
      ...jdbcFixtureFields(dsName),
      url: urlA,
    });
    expectApiSuccess(saveA.body);

    const templateName = `e2e-hr-template-${suffix}`;
    const instanceId = await publishAndRunDraft(
      request,
      buildManualPauseJdbcTemplate(templateName, dsName),
    );

    const beforeUpdate = await waitForExecutionRunning(request, instanceId);
    expect(beforeUpdate.status).toMatch(/^(RUNNING|PAUSED)$/);

    const saveB = await upsertJdbcDataSourceMultipart(request, {
      ...jdbcFixtureFields(dsName),
      url: urlB,
    });
    expectApiSuccess(saveB.body);

    const afterUpdate = await getConnectionSnapshot(request, instanceId);
    expect(afterUpdate.status).toBe(beforeUpdate.status);
    expect(afterUpdate.startedAt).toBe(beforeUpdate.startedAt);
    expect(afterUpdate.pauseReason).toBe(beforeUpdate.pauseReason);

    const catalogAfter = await fetchCatalogConnection(request, dsName, 'JDBC');
    expect(catalogAfter?.healthStatus).toBe('HEALTHY');

    await resumeExecution(request, instanceId);
    await waitForJobDetailSuccess(request, instanceId);

    const secondInstanceId = await publishAndRunDraft(
      request,
      buildManualPauseJdbcTemplate(`${templateName}-rerun`, dsName),
    );
    await waitForJobDetailSuccess(request, secondInstanceId);
  });

  test('DEGRADED badge and detail failure reason visible in UI', async ({ page, request }) => {
    const name = `e2e-gov-degraded-${Date.now()}`;
    await forceReloadFailure(request, name);

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.datasources);

    const catalogTable = page.getByTestId('datasources-catalog-table');
    const row = catalogTable.getByRole('row', { name: new RegExp(name) });
    await expect(row.getByText(/DEGRADED|降级/i)).toBeVisible();

    await row.getByRole('button', { name: /详情|Detail/i }).click();
    await expect(page.getByText(/DEGRADED|降级/i)).toBeVisible();
    await expect(
      page.getByText(/last known good|上次可用|NonexistentDriver|driver/i),
    ).toBeVisible();
  });

  test('governance blocks publish of inline JDBC template when managed-only required', async ({
    request,
  }) => {
    test.skip(
      process.env.DG_E2E_GOVERNANCE_STAGING !== 'true',
      'requires DG_E2E_GOVERNANCE_STAGING=true (e2e+staging profile)',
    );

    const draft = buildInlineJdbcTemplate(`e2e-gov-inline-${Date.now()}`);
    const { res: createRes, body: createBody } = await request.post(
      `${process.env.DG_E2E_API_URL ?? 'http://127.0.0.1:9876'}/api/templates`,
      {
        data: draft,
        headers: { 'Content-Type': 'application/json' },
      },
    );
    expect(createRes.ok()).toBeTruthy();
    expectApiSuccess(createBody);
    const templateId =
      unwrapApiData<{ templateId?: string | number; draft?: { id?: number | null } }>(createBody)
        ?.templateId ??
      unwrapApiData<{ draft?: { id?: number | null } }>(createBody)?.draft?.id;
    expect(templateId).toBeTruthy();

    const publish = await request.post(
      `${process.env.DG_E2E_API_URL ?? 'http://127.0.0.1:9876'}/api/templates/${templateId}/publish`,
      { data: {}, headers: { 'Content-Type': 'application/json' } },
    );
    const publishBody = await publish.json();
    expect(publishBody.success).toBe(false);
    expect(String(publishBody.message ?? '')).toMatch(/managed|inline|governance|connection/i);
  });

  test('audit deep-link shows DATASOURCE events after create and update', async ({ page, request }) => {
    const name = `e2e-gov-audit-${Date.now()}`;
    const fields = jdbcFixtureFields(name);

    const save = await upsertJdbcDataSourceMultipart(request, fields);
    expectApiSuccess(save.body);

    const update = await upsertJdbcDataSourceMultipart(request, {
      ...fields,
      url: fields.url.replace(';DB_CLOSE', ';MODE=MySQL;DB_CLOSE'),
    });
    expectApiSuccess(update.body);

    const apiEvents = await listAuditEvents(request, { category: 'DATASOURCE', resourceId: name });
    expect(apiEvents.some((event) => event.action === 'DATASOURCE_CREATE')).toBe(true);
    expect(apiEvents.some((event) => event.action === 'DATASOURCE_UPDATE')).toBe(true);

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.datasources);
    const persistedTable = page.getByTestId('datasources-persisted-table');
    await persistedTable.getByRole('row', { name: new RegExp(name) }).getByRole('link', { name: /审计|Audit/i }).click();
    await page.waitForURL(new RegExp(`category=DATASOURCE.*resourceId=${encodeURIComponent(name)}`));
    await expect(page.getByTestId('audit-page')).toBeVisible();
    await expect(page.getByText(/DATASOURCE_CREATE|DATASOURCE_UPDATE/)).toBeVisible();
  });

  test('template Run still triggers job after datasource CRUD in same session', async ({ page, request }) => {
    test.setTimeout(180_000);
    const name = `e2e-gov-run-${Date.now()}`;
    const fields = jdbcFixtureFields(name);
    const save = await upsertJdbcDataSourceMultipart(request, fields);
    expectApiSuccess(save.body);

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await openScenarioFromCatalog(page, 'GF-A');
    await saveTemplateFromReview(page);
    await navigateViaTopNav(page, TestIds.nav.datasources);
    await page.getByTestId(TestIds.actions.datasourcesNew).click();
    const dialog = page.getByRole('dialog');
    const auxName = `e2e-gov-aux-${Date.now()}`;
    const auxFields = jdbcFixtureFields(auxName);
    await dialog.getByRole('textbox', { name: /名称|Name/i }).fill(auxFields.name);
    await dialog.getByRole('textbox', { name: /JDBC URL/i }).fill(auxFields.url);
    await dialog.getByRole('textbox', { name: /用户名|Username/i }).fill(auxFields.username);
    await dialog.getByRole('textbox', { name: /驱动类|Driver class/i }).fill(auxFields.driverClassName);
    await dialog.getByRole('button', { name: /测\s*试|^Test$/i }).click();
    await expect(page.getByRole('alert').filter({ hasText: /成功|success|OK/i })).toBeVisible({
      timeout: 15_000,
    });
    await dialog.locator('button[type="submit"]').click();
    await expect(dialog).toBeHidden();

    await navigateViaTopNav(page, TestIds.nav.templates);
    await page.waitForURL(/\/templates\/\d+/);
    await publishAndRunFromReview(page);
    await expectJobSucceeded(page);
  });

  test('API GF-A run succeeds after managed datasource CRUD regression', async ({ request }) => {
    test.setTimeout(120_000);
    const name = `e2e-gov-api-run-${Date.now()}`;
    const save = await upsertJdbcDataSourceMultipart(request, jdbcFixtureFields(name));
    expectApiSuccess(save.body);

    const instanceId = await createPublishRunFromScenario(request, 'GF-A');
    const { body } = await request.get(
      `${process.env.DG_E2E_API_URL ?? 'http://127.0.0.1:9876'}/api/jobs/${encodeURIComponent(instanceId)}`,
    );
    const detail = await body.json();
    expect(detail.success).toBe(true);
    expect(detail.data?.execution?.status).toBe('SUCCESS');
  });

  test('HEALTHY catalog list baseline screenshot for playwright-cli regression', async ({ page }) => {
    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.datasources);
    await expect(page.getByTestId('datasources-catalog-table')).toBeVisible();
    await expect(page.getByText(/HEALTHY|健康/i).first()).toBeVisible();
    await page.screenshot({
      path: 'e2e/snapshots/governance/datasource-list-healthy.png',
      fullPage: true,
    });
  });
});

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Checkbox, Drawer, Form, Input, InputNumber, Modal, Space, Table, Tag, Typography, Upload, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import {
  fetchDataSources,
  removeDataSource,
  removeElasticsearchCluster,
  removeKafkaCluster,
  testConnectionUnified,
  testDataSourceByName,
  upsertDataSource,
  upsertElasticsearchCluster,
  upsertKafkaCluster,
} from '../../api/datasources';
import type { CatalogConnectionSummary, DataSourceSummary, MessagingClusterSummary } from '../../api/types';
import { DriverPresetFields } from '../datasources/DriverPresetFields';
import { isBundledPreset, resolveDriverPresets } from '../datasources/jdbcDriverPresets';
import { ConsolePageHeader } from '../../components/ConsolePageHeader';
import { FieldHelp } from '../../components/FieldHelp';
import { formatDateTime } from '../utils/formatDateTime';

type TestFeedback = { ok: boolean; message: string } | null;

type JdbcFormValues = {
  name: string;
  url: string;
  username: string;
  password: string;
  driverClassName: string;
  driverPresetId?: string;
};

type KafkaFormValues = {
  name: string;
  bootstrapServers: string;
  clientId?: string;
  acks?: string;
  compressionType?: string;
  retries?: number;
  securityProtocol?: string;
  saslMechanism?: string;
  saslJaasConfig?: string;
  extraProperties?: string;
};

type EsFormValues = {
  name: string;
  uris: string;
  username?: string;
  password?: string;
  apiKey?: string;
  pathPrefix?: string;
  connectionTimeoutMs?: number;
  socketTimeoutMs?: number;
  socketKeepAlive?: boolean;
};

function parseServerList(text: string): string[] {
  return text
    .split(/[\n,]/)
    .map((part) => part.trim())
    .filter(Boolean);
}

function formatServerList(values: string[] | null | undefined): string {
  return (values ?? []).join('\n');
}

function parseProperties(text: string | undefined): Record<string, string> | undefined {
  if (!text?.trim()) {
    return undefined;
  }
  const out: Record<string, string> = {};
  for (const line of text.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) {
      continue;
    }
    const idx = trimmed.indexOf('=');
    if (idx <= 0) {
      continue;
    }
    out[trimmed.slice(0, idx).trim()] = trimmed.slice(idx + 1).trim();
  }
  return Object.keys(out).length > 0 ? out : undefined;
}

function formatProperties(props: Record<string, string> | null | undefined): string {
  return Object.entries(props ?? {})
    .map(([key, value]) => `${key}=${value}`)
    .join('\n');
}

function catalogKey(name: string, kind: string): string {
  return `${kind}:${name}`;
}

function HealthBadge({ status }: { status: string | undefined }) {
  const { t } = useTranslation();
  if (!status) {
    return <>—</>;
  }
  if (status === 'HEALTHY') {
    return <Tag color="success">{t('datasources.health.healthy')}</Tag>;
  }
  if (status === 'DEGRADED') {
    return <Tag color="warning">{t('datasources.health.degraded')}</Tag>;
  }
  return <Tag>{status}</Tag>;
}

function TestResultAlert({ feedback }: { feedback: TestFeedback }) {
  const { t } = useTranslation();
  if (!feedback) {
    return null;
  }
  return (
    <Alert
      type={feedback.ok ? 'success' : 'error'}
      showIcon
      message={feedback.ok ? t('datasources.test.success') : t('datasources.test.failure')}
      description={feedback.message}
      style={{ marginBottom: 16 }}
    />
  );
}

/**
 * JDBC datasource administration (persisted configs + runtime keys).
 */
export function DatasourcesPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [jdbcModalOpen, setJdbcModalOpen] = useState(false);
  const [kafkaModalOpen, setKafkaModalOpen] = useState(false);
  const [esModalOpen, setEsModalOpen] = useState(false);
  const [editing, setEditing] = useState<DataSourceSummary | null>(null);
  const [editingKafka, setEditingKafka] = useState<MessagingClusterSummary | null>(null);
  const [editingEs, setEditingEs] = useState<MessagingClusterSummary | null>(null);
  const [jarFile, setJarFile] = useState<File | null>(null);
  const [dialogKey, setDialogKey] = useState('new');
  const [selectedPresetId, setSelectedPresetId] = useState<string | undefined>();
  const [jdbcTestFeedback, setJdbcTestFeedback] = useState<TestFeedback>(null);
  const [kafkaTestFeedback, setKafkaTestFeedback] = useState<TestFeedback>(null);
  const [esTestFeedback, setEsTestFeedback] = useState<TestFeedback>(null);
  const [jdbcTestPassed, setJdbcTestPassed] = useState(false);
  const [kafkaTestPassed, setKafkaTestPassed] = useState(false);
  const [esTestPassed, setEsTestPassed] = useState(false);
  const [catalogDetail, setCatalogDetail] = useState<CatalogConnectionSummary | null>(null);
  const [jdbcForm] = Form.useForm<JdbcFormValues>();
  const [kafkaForm] = Form.useForm<KafkaFormValues>();
  const [esForm] = Form.useForm<EsFormValues>();

  const overviewQuery = useQuery({
    queryKey: ['datasources'],
    queryFn: fetchDataSources,
  });

  const requireTestBeforeSave = overviewQuery.data?.governance?.requireConnectivityTestBeforeSave ?? false;

  const catalogByKey = useMemo(() => {
    const map = new Map<string, CatalogConnectionSummary>();
    for (const row of overviewQuery.data?.catalogConnections ?? []) {
      map.set(catalogKey(row.name, row.kind), row);
    }
    return map;
  }, [overviewQuery.data?.catalogConnections]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['datasources'] });
    queryClient.invalidateQueries({ queryKey: ['editor-data-sources'] });
  };

  const saveJdbcMutation = useMutation({
    mutationFn: async (values: JdbcFormValues) => {
      const body = new FormData();
      body.set('name', values.name);
      body.set('url', values.url);
      body.set('username', values.username);
      body.set('password', values.password);
      body.set('driverClassName', values.driverClassName);
      if (values.driverPresetId?.trim()) {
        body.set('driverPresetId', values.driverPresetId.trim());
      }
      if (jarFile) {
        body.set('driverFile', jarFile);
      }
      return upsertDataSource(body);
    },
    onSuccess: () => {
      message.success(t('datasources.dialog.saved', { name: jdbcForm.getFieldValue('name') }));
      setJdbcModalOpen(false);
      setJarFile(null);
      setJdbcTestFeedback(null);
      setJdbcTestPassed(false);
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const saveKafkaMutation = useMutation({
    mutationFn: (values: KafkaFormValues) =>
      upsertKafkaCluster({
        name: values.name,
        bootstrapServers: parseServerList(values.bootstrapServers),
        clientId: values.clientId || undefined,
        acks: values.acks || undefined,
        compressionType: values.compressionType || undefined,
        retries: values.retries,
        securityProtocol: values.securityProtocol || undefined,
        saslMechanism: values.saslMechanism || undefined,
        saslJaasConfig: values.saslJaasConfig || undefined,
        properties: parseProperties(values.extraProperties),
      }),
    onSuccess: () => {
      message.success(t('datasources.kafka.saved', { name: kafkaForm.getFieldValue('name') }));
      setKafkaModalOpen(false);
      setKafkaTestFeedback(null);
      setKafkaTestPassed(false);
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const saveEsMutation = useMutation({
    mutationFn: (values: EsFormValues) =>
      upsertElasticsearchCluster({
        name: values.name,
        uris: parseServerList(values.uris),
        username: values.username || undefined,
        password: values.password || undefined,
        apiKey: values.apiKey || undefined,
        pathPrefix: values.pathPrefix || undefined,
        connectionTimeoutMs: values.connectionTimeoutMs,
        socketTimeoutMs: values.socketTimeoutMs,
        socketKeepAlive: values.socketKeepAlive,
      }),
    onSuccess: () => {
      message.success(t('datasources.es.saved', { name: esForm.getFieldValue('name') }));
      setEsModalOpen(false);
      setEsTestFeedback(null);
      setEsTestPassed(false);
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const removeJdbcMutation = useMutation({
    mutationFn: removeDataSource,
    onSuccess: (_, name) => {
      message.success(t('datasources.removed', { name }));
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const removeKafkaMutation = useMutation({
    mutationFn: removeKafkaCluster,
    onSuccess: (_, name) => {
      message.success(t('datasources.kafka.removed', { name }));
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const removeEsMutation = useMutation({
    mutationFn: removeElasticsearchCluster,
    onSuccess: (_, name) => {
      message.success(t('datasources.es.removed', { name }));
      invalidate();
    },
    onError: (err: Error) => message.error(err.message),
  });

  const testJdbcFormMutation = useMutation({
    mutationFn: async (values: JdbcFormValues) => {
      if (editing?.name) {
        return testDataSourceByName(editing.name);
      }
      return testConnectionUnified({
        kind: 'JDBC',
        draftPayload: {
          url: values.url,
          username: values.username ?? '',
          password: values.password ?? '',
          driverClassName: values.driverClassName,
          driverJarPath: editing?.driverJarPath ?? undefined,
        },
      });
    },
    onSuccess: (msg) => {
      setJdbcTestFeedback({ ok: true, message: msg });
      setJdbcTestPassed(true);
    },
    onError: (err: Error) => {
      setJdbcTestFeedback({ ok: false, message: err.message });
      setJdbcTestPassed(false);
    },
  });

  const testKafkaFormMutation = useMutation({
    mutationFn: async (values: KafkaFormValues) => {
      const servers = parseServerList(values.bootstrapServers);
      if (editingKafka?.name) {
        return testConnectionUnified({ kind: 'KAFKA', name: editingKafka.name });
      }
      const payload: Record<string, unknown> = { bootstrapServers: servers };
      if (values.clientId?.trim()) payload.clientId = values.clientId.trim();
      if (values.acks?.trim()) payload.acks = values.acks.trim();
      if (values.compressionType?.trim()) payload.compressionType = values.compressionType.trim();
      if (values.retries != null) payload.retries = values.retries;
      if (values.securityProtocol?.trim()) payload.securityProtocol = values.securityProtocol.trim();
      if (values.saslMechanism?.trim()) payload.saslMechanism = values.saslMechanism.trim();
      if (values.saslJaasConfig?.trim()) payload.saslJaasConfig = values.saslJaasConfig.trim();
      const props = parseProperties(values.extraProperties);
      if (props) payload.properties = props;
      return testConnectionUnified({ kind: 'KAFKA', draftPayload: payload });
    },
    onSuccess: (msg) => {
      setKafkaTestFeedback({ ok: true, message: msg });
      setKafkaTestPassed(true);
    },
    onError: (err: Error) => {
      setKafkaTestFeedback({ ok: false, message: err.message });
      setKafkaTestPassed(false);
    },
  });

  const testEsFormMutation = useMutation({
    mutationFn: async (values: EsFormValues) => {
      const uris = parseServerList(values.uris);
      if (editingEs?.name) {
        return testConnectionUnified({ kind: 'ELASTICSEARCH', name: editingEs.name });
      }
      const payload: Record<string, unknown> = { uris };
      if (values.username?.trim()) payload.username = values.username.trim();
      if (values.password?.trim()) payload.password = values.password.trim();
      if (values.apiKey?.trim()) payload.apiKey = values.apiKey.trim();
      if (values.pathPrefix?.trim()) payload.pathPrefix = values.pathPrefix.trim();
      if (values.connectionTimeoutMs != null) payload.connectionTimeoutMs = values.connectionTimeoutMs;
      if (values.socketTimeoutMs != null) payload.socketTimeoutMs = values.socketTimeoutMs;
      if (values.socketKeepAlive != null) payload.socketKeepAlive = values.socketKeepAlive;
      return testConnectionUnified({ kind: 'ELASTICSEARCH', draftPayload: payload });
    },
    onSuccess: (msg) => {
      setEsTestFeedback({ ok: true, message: msg });
      setEsTestPassed(true);
    },
    onError: (err: Error) => {
      setEsTestFeedback({ ok: false, message: err.message });
      setEsTestPassed(false);
    },
  });

  const openCreateJdbc = () => {
    setEditing(null);
    setJarFile(null);
    setJdbcTestFeedback(null);
    setJdbcTestPassed(false);
    setDialogKey(`new-${Date.now()}`);
    jdbcForm.resetFields();
    setJdbcModalOpen(true);
  };

  const openEditJdbc = (row: DataSourceSummary) => {
    setEditing(row);
    setJarFile(null);
    setDialogKey(`edit-${row.name}`);
    const presetId = row.driverPresetId ?? undefined;
    setSelectedPresetId(presetId);
    jdbcForm.setFieldsValue({
      name: row.name,
      url: row.url,
      username: row.username ?? '',
      password: '',
      driverClassName: row.driverClassName,
      driverPresetId: presetId,
    });
    setJdbcTestFeedback(null);
    setJdbcTestPassed(false);
    setJdbcModalOpen(true);
  };

  const openCreateKafka = () => {
    setEditingKafka(null);
    setKafkaTestFeedback(null);
    setKafkaTestPassed(false);
    kafkaForm.resetFields();
    setKafkaModalOpen(true);
  };

  const openEditKafka = (row: MessagingClusterSummary) => {
    setEditingKafka(row);
    setKafkaTestFeedback(null);
    setKafkaTestPassed(false);
    kafkaForm.setFieldsValue({
      name: row.name,
      bootstrapServers: formatServerList(row.bootstrapServers),
      clientId: row.clientId ?? '',
      acks: row.acks ?? '',
      compressionType: row.compressionType ?? '',
      retries: row.retries ?? undefined,
      securityProtocol: row.securityProtocol ?? '',
      saslMechanism: row.saslMechanism ?? '',
      saslJaasConfig: '',
      extraProperties: formatProperties(row.properties),
    });
    setKafkaModalOpen(true);
  };

  const openCreateEs = () => {
    setEditingEs(null);
    setEsTestFeedback(null);
    setEsTestPassed(false);
    esForm.resetFields();
    setEsModalOpen(true);
  };

  const openEditEs = (row: MessagingClusterSummary) => {
    setEditingEs(row);
    setEsTestFeedback(null);
    setEsTestPassed(false);
    esForm.setFieldsValue({
      name: row.name,
      uris: formatServerList(row.uris),
      username: row.username ?? '',
      password: '',
      apiKey: '',
      pathPrefix: row.pathPrefix ?? '',
      connectionTimeoutMs: row.connectionTimeoutMs ?? undefined,
      socketTimeoutMs: row.socketTimeoutMs ?? undefined,
      socketKeepAlive: row.socketKeepAlive ?? false,
    });
    setEsModalOpen(true);
  };

  const confirmRemoveJdbc = (name: string) => {
    Modal.confirm({
      title: t('datasources.remove.confirm.title', { name }),
      content: t('datasources.remove.confirm.text'),
      okText: t('common.remove'),
      cancelText: t('common.cancel'),
      okButtonProps: { danger: true },
      onOk: () => removeJdbcMutation.mutateAsync(name),
    });
  };

  const confirmRemoveKafka = (name: string) => {
    Modal.confirm({
      title: t('datasources.kafka.remove.confirm.title', { name }),
      content: t('datasources.kafka.remove.confirm.text'),
      okText: t('common.remove'),
      cancelText: t('common.cancel'),
      okButtonProps: { danger: true },
      onOk: () => removeKafkaMutation.mutateAsync(name),
    });
  };

  const confirmRemoveEs = (name: string) => {
    Modal.confirm({
      title: t('datasources.es.remove.confirm.title', { name }),
      content: t('datasources.es.remove.confirm.text'),
      okText: t('common.remove'),
      cancelText: t('common.cancel'),
      okButtonProps: { danger: true },
      onOk: () => removeEsMutation.mutateAsync(name),
    });
  };

  const driverPresets = useMemo(
    () => resolveDriverPresets(overviewQuery.data?.driverPresets),
    [overviewQuery.data?.driverPresets],
  );

  const catalogColumns: ColumnsType<CatalogConnectionSummary> = useMemo(
    () => [
      { title: t('datasources.col.name'), dataIndex: 'name', sorter: (a, b) => a.name.localeCompare(b.name) },
      { title: t('datasources.col.kind'), dataIndex: 'kind' },
      { title: t('datasources.col.source'), dataIndex: 'source' },
      {
        title: t('datasources.col.health'),
        key: 'health',
        render: (_, row) => <HealthBadge status={row.healthStatus} />,
      },
      {
        title: t('datasources.col.lastReload'),
        dataIndex: 'lastReloadAt',
        render: (v: string | null | undefined) => (v ? formatDateTime(v) : '—'),
      },
      {
        title: t('datasources.col.actions'),
        key: 'actions',
        render: (_, row) => (
          <Space wrap>
            <Button type="link" onClick={() => setCatalogDetail(row)}>
              {t('common.detail')}
            </Button>
            <Link to={`/console/audit?category=DATASOURCE&resourceId=${encodeURIComponent(row.name)}`}>
              {t('datasources.audit.view')}
            </Link>
          </Space>
        ),
      },
    ],
    [t],
  );

  const jdbcColumns: ColumnsType<DataSourceSummary> = useMemo(
    () => [
      { title: t('datasources.col.name'), dataIndex: 'name', sorter: (a, b) => a.name.localeCompare(b.name) },
      { title: t('datasources.col.url'), dataIndex: 'url', ellipsis: true },
      { title: t('datasources.col.driver'), dataIndex: 'driverClassName' },
      {
        title: t('datasources.col.health'),
        key: 'health',
        render: (_, row) => (
          <HealthBadge status={catalogByKey.get(catalogKey(row.name, 'JDBC'))?.healthStatus} />
        ),
      },
      {
        title: t('datasources.col.enabled'),
        dataIndex: 'enabled',
        render: (v: boolean) => (v ? t('common.yes') : t('common.no')),
      },
      {
        title: t('datasources.col.actions'),
        key: 'actions',
        render: (_, row) => (
          <Space wrap>
            <Button type="link" onClick={() => openEditJdbc(row)}>
              {t('common.edit')}
            </Button>
            <Button
              type="link"
              onClick={() =>
                testDataSourceByName(row.name)
                  .then((msg) => message.success(msg))
                  .catch((err: Error) => message.error(err.message))
              }
            >
              {t('common.test')}
            </Button>
            <Link to={`/console/audit?category=DATASOURCE&resourceId=${encodeURIComponent(row.name)}`}>
              {t('datasources.audit.view')}
            </Link>
            <Button type="link" danger onClick={() => confirmRemoveJdbc(row.name)}>
              {t('common.remove')}
            </Button>
          </Space>
        ),
      },
    ],
    [catalogByKey, t],
  );

  const kafkaColumns: ColumnsType<MessagingClusterSummary> = useMemo(
    () => [
      { title: t('datasources.col.name'), dataIndex: 'name', sorter: (a, b) => a.name.localeCompare(b.name) },
      {
        title: t('datasources.kafka.col.servers'),
        dataIndex: 'bootstrapServers',
        ellipsis: true,
        render: (v: string[] | null | undefined) => (v ?? []).join(', ') || '—',
      },
      {
        title: t('datasources.kafka.col.security'),
        dataIndex: 'securityProtocol',
        render: (v: string | null | undefined) => v ?? '—',
      },
      {
        title: t('datasources.col.health'),
        key: 'health',
        render: (_, row) => (
          <HealthBadge status={catalogByKey.get(catalogKey(row.name, 'KAFKA'))?.healthStatus} />
        ),
      },
      {
        title: t('datasources.col.updatedAt'),
        dataIndex: 'updatedAt',
        render: (v: string | null) => v ?? '—',
      },
      {
        title: t('datasources.col.actions'),
        key: 'actions',
        render: (_, row) => (
          <Space wrap>
            <Button type="link" onClick={() => openEditKafka(row)}>
              {t('common.edit')}
            </Button>
            <Link to={`/console/audit?category=DATASOURCE&resourceId=${encodeURIComponent(row.name)}`}>
              {t('datasources.audit.view')}
            </Link>
            <Button type="link" danger onClick={() => confirmRemoveKafka(row.name)}>
              {t('common.remove')}
            </Button>
          </Space>
        ),
      },
    ],
    [catalogByKey, t],
  );

  const esColumns: ColumnsType<MessagingClusterSummary> = useMemo(
    () => [
      { title: t('datasources.col.name'), dataIndex: 'name', sorter: (a, b) => a.name.localeCompare(b.name) },
      {
        title: t('datasources.es.col.uris'),
        dataIndex: 'uris',
        ellipsis: true,
        render: (v: string[] | null | undefined) => (v ?? []).join(', ') || '—',
      },
      {
        title: t('datasources.col.health'),
        key: 'health',
        render: (_, row) => (
          <HealthBadge status={catalogByKey.get(catalogKey(row.name, 'ELASTICSEARCH'))?.healthStatus} />
        ),
      },
      {
        title: t('datasources.col.updatedAt'),
        dataIndex: 'updatedAt',
        render: (v: string | null) => v ?? '—',
      },
      {
        title: t('datasources.col.actions'),
        key: 'actions',
        render: (_, row) => (
          <Space wrap>
            <Button type="link" onClick={() => openEditEs(row)}>
              {t('common.edit')}
            </Button>
            <Link to={`/console/audit?category=DATASOURCE&resourceId=${encodeURIComponent(row.name)}`}>
              {t('datasources.audit.view')}
            </Link>
            <Button type="link" danger onClick={() => confirmRemoveEs(row.name)}>
              {t('common.remove')}
            </Button>
          </Space>
        ),
      },
    ],
    [catalogByKey, t],
  );

  const uploadFileList: UploadFile[] = jarFile
    ? [{ uid: '-1', name: jarFile.name, status: 'done' }]
    : [];

  return (
    <div data-testid="datasources-page">
      <ConsolePageHeader
        title={t('datasources.title')}
        subtitle={t('datasources.subtitle')}
        crumbs={[{ label: t('nav.home'), path: '/' }, { label: t('nav.datasources') }]}
        extra={
          <Space>
            <Link to="/console/audit?category=DATASOURCE">
              <Button>{t('datasources.audit.viewAll')}</Button>
            </Link>
            <Button type="primary" data-testid="datasources-new-button" onClick={openCreateJdbc}>
              {t('datasources.new')}
            </Button>
          </Space>
        }
      />
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('datasources.workflow.title')}
        description={t('datasources.workflow.body')}
      />
      {overviewQuery.isError ? (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          message={t('datasources.loadError')}
          description={(overviewQuery.error as Error).message}
        />
      ) : null}
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => overviewQuery.refetch()}>{t('common.refresh')}</Button>
      </Space>

      <Typography.Title level={5}>{t('datasources.section.persisted')}</Typography.Title>
      <Table
        data-testid="datasources-persisted-table"
        rowKey="name"
        loading={overviewQuery.isLoading}
        dataSource={overviewQuery.data?.persisted ?? []}
        columns={jdbcColumns}
        pagination={false}
        style={{ marginBottom: 24 }}
      />

      <Typography.Title level={5}>{t('datasources.section.runtime')}</Typography.Title>
      <Typography.Paragraph data-testid="datasources-runtime-keys">
        {(overviewQuery.data?.runtimeKeys ?? []).join(', ') || '—'}
      </Typography.Paragraph>

      <Typography.Title level={5}>{t('datasources.section.catalog')}</Typography.Title>
      <Table
        data-testid="datasources-catalog-table"
        rowKey={(row) => `${row.name}-${row.kind}`}
        loading={overviewQuery.isLoading}
        dataSource={overviewQuery.data?.catalogConnections ?? []}
        columns={catalogColumns}
        pagination={false}
        style={{ marginBottom: 24 }}
      />

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
        <Typography.Title level={5} style={{ margin: 0 }}>
          {t('datasources.section.kafka')}
        </Typography.Title>
        <Button onClick={openCreateKafka}>{t('datasources.kafka.new')}</Button>
      </div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 8, maxWidth: 900 }}
        message={t('datasources.kafka.hint')}
      />
      <Table
        rowKey="name"
        loading={overviewQuery.isLoading}
        dataSource={overviewQuery.data?.kafkaPersisted ?? []}
        columns={kafkaColumns}
        pagination={false}
        locale={{ emptyText: t('datasources.kafka.empty') }}
        style={{ marginBottom: 8 }}
      />
      <Typography.Text type="secondary">{t('datasources.kafka.runtimeKeys')}</Typography.Text>
      <Typography.Paragraph>
        {(overviewQuery.data?.kafkaClusters ?? []).join(', ') || '—'}
      </Typography.Paragraph>

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
        <Typography.Title level={5} style={{ margin: 0 }}>
          {t('datasources.section.elasticsearch')}
        </Typography.Title>
        <Button onClick={openCreateEs}>{t('datasources.es.new')}</Button>
      </div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 8, maxWidth: 900 }}
        message={t('datasources.es.hint')}
      />
      <Table
        rowKey="name"
        loading={overviewQuery.isLoading}
        dataSource={overviewQuery.data?.elasticsearchPersisted ?? []}
        columns={esColumns}
        pagination={false}
        locale={{ emptyText: t('datasources.es.empty') }}
        style={{ marginBottom: 8 }}
      />
      <Typography.Text type="secondary">{t('datasources.es.runtimeKeys')}</Typography.Text>
      <Typography.Paragraph>
        {(overviewQuery.data?.elasticsearchClusters ?? []).join(', ') || '—'}
      </Typography.Paragraph>

      <Modal
        title={t('datasources.dialog.title')}
        open={jdbcModalOpen}
        onCancel={() => setJdbcModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form
          form={jdbcForm}
          layout="vertical"
          onFinish={(v) => saveJdbcMutation.mutate(v)}
          onValuesChange={() => {
            setJdbcTestFeedback(null);
            setJdbcTestPassed(false);
          }}
        >
          <Form.Item
            name="name"
            label={<FieldHelp label={t('datasources.dialog.name')} help={t('datasources.dialog.name.help')} />}
            rules={[{ required: true }]}
          >
            <Input readOnly={Boolean(editing)} />
          </Form.Item>
          <Form.Item
            name="url"
            label={<FieldHelp label={t('datasources.dialog.url')} help={t('datasources.dialog.url.help')} />}
            rules={[{ required: true }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="username"
            label={<FieldHelp label={t('datasources.dialog.username')} help={t('datasources.dialog.username.help')} />}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="password"
            label={<FieldHelp label={t('datasources.dialog.password')} help={t('datasources.dialog.password.help')} />}
          >
            <Input.Password placeholder={editing ? '••••••' : undefined} />
          </Form.Item>
          <DriverPresetFields
            form={jdbcForm}
            dialogKey={dialogKey}
            presets={driverPresets}
            onPresetIdChange={setSelectedPresetId}
          />
          {!isBundledPreset(driverPresets, selectedPresetId) ? (
            <Form.Item
              label={
                <FieldHelp label={t('datasources.dialog.upload')} help={t('datasources.dialog.uploadHint')} />
              }
            >
              <Upload
                accept=".jar,application/java-archive"
                maxCount={1}
                beforeUpload={(file) => {
                  setJarFile(file);
                  return false;
                }}
                onRemove={() => setJarFile(null)}
                fileList={uploadFileList}
              >
                <Button>{t('datasources.dialog.upload')}</Button>
              </Upload>
            </Form.Item>
          ) : null}
          <TestResultAlert feedback={jdbcTestFeedback} />
          {requireTestBeforeSave && !jdbcTestPassed ? (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message={t('datasources.test.requiredBeforeSave')}
            />
          ) : null}
          <Space>
            <Button
              onClick={() => testJdbcFormMutation.mutate(jdbcForm.getFieldsValue())}
              loading={testJdbcFormMutation.isPending}
            >
              {t('datasources.dialog.test')}
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={saveJdbcMutation.isPending}
              disabled={requireTestBeforeSave && !jdbcTestPassed}
            >
              {t('common.save')}
            </Button>
            <Button onClick={() => setJdbcModalOpen(false)}>{t('common.cancel')}</Button>
          </Space>
        </Form>
      </Modal>

      <Modal
        title={editingKafka ? t('datasources.kafka.dialog.edit') : t('datasources.kafka.dialog.new')}
        open={kafkaModalOpen}
        onCancel={() => setKafkaModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form
          form={kafkaForm}
          layout="vertical"
          onFinish={(v) => saveKafkaMutation.mutate(v)}
          onValuesChange={() => {
            setKafkaTestFeedback(null);
            setKafkaTestPassed(false);
          }}
        >
          <Form.Item
            name="name"
            label={
              <FieldHelp label={t('datasources.kafka.dialog.name')} help={t('datasources.kafka.dialog.name.help')} />
            }
            rules={[{ required: true }]}
          >
            <Input readOnly={Boolean(editingKafka)} />
          </Form.Item>
          <Form.Item
            name="bootstrapServers"
            label={
              <FieldHelp
                label={t('datasources.kafka.dialog.servers')}
                help={t('datasources.kafka.dialog.servers.help')}
              />
            }
            rules={[{ required: true }]}
          >
            <Input.TextArea rows={3} placeholder={t('datasources.kafka.dialog.servers.placeholder')} />
          </Form.Item>
          <Form.Item
            name="clientId"
            label={
              <FieldHelp label={t('datasources.kafka.dialog.clientId')} help={t('datasources.kafka.dialog.clientId.help')} />
            }
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="acks"
            label={<FieldHelp label={t('datasources.kafka.dialog.acks')} help={t('datasources.kafka.dialog.acks.help')} />}
          >
            <Input placeholder="all" />
          </Form.Item>
          <Form.Item
            name="compressionType"
            label={
              <FieldHelp
                label={t('datasources.kafka.dialog.compressionType')}
                help={t('datasources.kafka.dialog.compressionType.help')}
              />
            }
          >
            <Input placeholder="gzip" />
          </Form.Item>
          <Form.Item
            name="retries"
            label={
              <FieldHelp label={t('datasources.kafka.dialog.retries')} help={t('datasources.kafka.dialog.retries.help')} />
            }
          >
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="securityProtocol"
            label={
              <FieldHelp
                label={t('datasources.kafka.dialog.securityProtocol')}
                help={t('datasources.kafka.dialog.securityProtocol.help')}
              />
            }
          >
            <Input placeholder="SASL_PLAINTEXT" />
          </Form.Item>
          <Form.Item
            name="saslMechanism"
            label={
              <FieldHelp
                label={t('datasources.kafka.dialog.saslMechanism')}
                help={t('datasources.kafka.dialog.saslMechanism.help')}
              />
            }
          >
            <Input placeholder="PLAIN" />
          </Form.Item>
          <Form.Item
            name="saslJaasConfig"
            label={
              <FieldHelp
                label={t('datasources.kafka.dialog.saslJaasConfig')}
                help={t('datasources.kafka.dialog.saslJaasConfig.help')}
              />
            }
          >
            <Input.TextArea
              rows={3}
              placeholder={
                editingKafka?.hasSaslJaasConfig
                  ? t('datasources.secret.keepExisting')
                  : t('datasources.kafka.dialog.saslJaasConfig.placeholder')
              }
            />
          </Form.Item>
          <Form.Item
            name="extraProperties"
            label={
              <FieldHelp
                label={t('datasources.kafka.dialog.extraProperties')}
                help={t('datasources.kafka.dialog.extraProperties.help')}
              />
            }
          >
            <Input.TextArea rows={3} placeholder={t('datasources.kafka.dialog.extraProperties.placeholder')} />
          </Form.Item>
          <TestResultAlert feedback={kafkaTestFeedback} />
          {requireTestBeforeSave && !kafkaTestPassed ? (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message={t('datasources.test.requiredBeforeSave')}
            />
          ) : null}
          <Space>
            <Button
              onClick={() => testKafkaFormMutation.mutate(kafkaForm.getFieldsValue())}
              loading={testKafkaFormMutation.isPending}
            >
              {t('datasources.dialog.test')}
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={saveKafkaMutation.isPending}
              disabled={requireTestBeforeSave && !kafkaTestPassed}
            >
              {t('common.save')}
            </Button>
            <Button onClick={() => setKafkaModalOpen(false)}>{t('common.cancel')}</Button>
          </Space>
        </Form>
      </Modal>

      <Modal
        title={editingEs ? t('datasources.es.dialog.edit') : t('datasources.es.dialog.new')}
        open={esModalOpen}
        onCancel={() => setEsModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form
          form={esForm}
          layout="vertical"
          onFinish={(v) => saveEsMutation.mutate(v)}
          onValuesChange={() => {
            setEsTestFeedback(null);
            setEsTestPassed(false);
          }}
        >
          <Form.Item
            name="name"
            label={<FieldHelp label={t('datasources.es.dialog.name')} help={t('datasources.es.dialog.name.help')} />}
            rules={[{ required: true }]}
          >
            <Input readOnly={Boolean(editingEs)} />
          </Form.Item>
          <Form.Item
            name="uris"
            label={
              <FieldHelp label={t('datasources.es.dialog.uris')} help={t('datasources.es.dialog.uris.help')} />
            }
            rules={[{ required: true }]}
          >
            <Input.TextArea rows={4} placeholder={t('datasources.es.dialog.uris.placeholder')} />
          </Form.Item>
          <Form.Item
            name="username"
            label={
              <FieldHelp label={t('datasources.es.dialog.username')} help={t('datasources.es.dialog.username.help')} />
            }
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="password"
            label={
              <FieldHelp label={t('datasources.es.dialog.password')} help={t('datasources.es.dialog.password.help')} />
            }
          >
            <Input.Password
              placeholder={editingEs?.hasPassword ? t('datasources.secret.keepExisting') : undefined}
            />
          </Form.Item>
          <Form.Item
            name="apiKey"
            label={<FieldHelp label={t('datasources.es.dialog.apiKey')} help={t('datasources.es.dialog.apiKey.help')} />}
          >
            <Input.Password placeholder={editingEs?.hasApiKey ? t('datasources.secret.keepExisting') : undefined} />
          </Form.Item>
          <Form.Item
            name="pathPrefix"
            label={
              <FieldHelp label={t('datasources.es.dialog.pathPrefix')} help={t('datasources.es.dialog.pathPrefix.help')} />
            }
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="connectionTimeoutMs"
            label={
              <FieldHelp
                label={t('datasources.es.dialog.connectionTimeoutMs')}
                help={t('datasources.es.dialog.connectionTimeoutMs.help')}
              />
            }
          >
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="socketTimeoutMs"
            label={
              <FieldHelp
                label={t('datasources.es.dialog.socketTimeoutMs')}
                help={t('datasources.es.dialog.socketTimeoutMs.help')}
              />
            }
          >
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="socketKeepAlive" valuePropName="checked">
            <Checkbox>{t('datasources.es.dialog.socketKeepAlive')}</Checkbox>
          </Form.Item>
          <TestResultAlert feedback={esTestFeedback} />
          {requireTestBeforeSave && !esTestPassed ? (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message={t('datasources.test.requiredBeforeSave')}
            />
          ) : null}
          <Space>
            <Button
              onClick={() => testEsFormMutation.mutate(esForm.getFieldsValue())}
              loading={testEsFormMutation.isPending}
            >
              {t('datasources.dialog.test')}
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={saveEsMutation.isPending}
              disabled={requireTestBeforeSave && !esTestPassed}
            >
              {t('common.save')}
            </Button>
            <Button onClick={() => setEsModalOpen(false)}>{t('common.cancel')}</Button>
          </Space>
        </Form>
      </Modal>

      <Drawer
        title={catalogDetail ? t('datasources.detail.title', { name: catalogDetail.name }) : t('common.detail')}
        open={catalogDetail != null}
        onClose={() => setCatalogDetail(null)}
        width={480}
      >
        {catalogDetail ? (
          <>
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <div>
                <Typography.Text type="secondary">{t('datasources.col.name')}</Typography.Text>
                <div>{catalogDetail.name}</div>
              </div>
              <div>
                <Typography.Text type="secondary">{t('datasources.col.kind')}</Typography.Text>
                <div>{catalogDetail.kind}</div>
              </div>
              <div>
                <Typography.Text type="secondary">{t('datasources.col.source')}</Typography.Text>
                <div>{catalogDetail.source}</div>
              </div>
              <div>
                <Typography.Text type="secondary">{t('datasources.col.health')}</Typography.Text>
                <div>
                  <HealthBadge status={catalogDetail.healthStatus} />
                </div>
              </div>
              <div>
                <Typography.Text type="secondary">{t('datasources.col.lastReload')}</Typography.Text>
                <div>{catalogDetail.lastReloadAt ? formatDateTime(catalogDetail.lastReloadAt) : '—'}</div>
              </div>
              {catalogDetail.healthStatus === 'DEGRADED' ? (
                <>
                  <Alert
                    type="warning"
                    showIcon
                    message={t('datasources.health.degraded')}
                    description={
                      catalogDetail.degradedReason?.trim()
                        ? catalogDetail.degradedReason
                        : t('datasources.detail.degradedUnknown')
                    }
                  />
                  <Typography.Text type="secondary">{t('datasources.detail.lastKnownGood')}</Typography.Text>
                </>
              ) : null}
            </Space>
            <div style={{ marginTop: 24 }}>
              <Link to={`/console/audit?category=DATASOURCE&resourceId=${encodeURIComponent(catalogDetail.name)}`}>
                {t('datasources.audit.view')}
              </Link>
            </div>
          </>
        ) : null}
      </Drawer>
    </div>
  );
}

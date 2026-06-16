import { Alert, Form, Input, InputNumber, Select } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { AiCatalog, EditorDataSources } from '../../api/types';
import { fetchSecretSummaries } from '../../api/secrets';
import { FieldHelp } from '../../components/FieldHelp';
import { yesNoOptions } from '../utils/optionLabels';
import { SourceFileInput } from '../../components/SourceFileInput';
import type { SourceDraft } from '../../api/types';
import type { EditableSourceKind } from './draftUtils';

type Props = {
  kind: EditableSourceKind;
  source: SourceDraft;
  readOnly: boolean;
  editorDataSources: EditorDataSources;
  aiCatalog?: AiCatalog;
  onPatch: (patch: SourceDraft) => void;
};

/**
 * Type-specific template input fields (not the JDBC admin page).
 */
export function SourceFieldsForm({
  kind,
  source,
  readOnly,
  editorDataSources,
  aiCatalog,
  onPatch,
}: Props) {
  const { t } = useTranslation();
  const iterator = source.iterator;
  const sheetName =
    (source.sheets as { name?: string }[] | undefined)?.[0]?.name ?? 'Sheet1';
  const jdbcOptions = editorDataSources.jdbcNames.map((n) => ({ value: n, label: n }));
  const selectedJdbc = source.dataSourceId as string | undefined;

  if (kind === 'query') {
    return (
      <>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message={t('source.query.intro')}
        />
        <Form.Item
          label={
            <FieldHelp
              label={t('source.jdbcDatasource')}
              help={t('source.jdbcDatasource.help')}
              required
            />
          }
        >
          <Select
            disabled={readOnly}
            showSearch
            placeholder={t('source.jdbcDatasource.placeholder')}
            value={selectedJdbc}
            options={jdbcOptions}
            onChange={(v) => onPatch({ ...source, dataSourceId: v })}
          />
        </Form.Item>
        {!selectedJdbc ? (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 12 }}
            message={t('source.query.pickDatasource')}
          />
        ) : (
          <Alert
            type="success"
            showIcon
            style={{ marginBottom: 12 }}
            message={t('source.query.sqlHint', { name: selectedJdbc })}
          />
        )}
        <Form.Item
          label={<FieldHelp label={t('source.sql')} help={t('source.sql.help')} required />}
        >
          <Input.TextArea
            rows={8}
            readOnly={readOnly}
            value={(source.sql as string) ?? ''}
            placeholder={t('source.sql.placeholder')}
            onChange={(e) => onPatch({ ...source, sql: e.target.value })}
          />
        </Form.Item>
      </>
    );
  }

  if (kind === 'iterator') {
    return (
      <>
        <Alert type="info" showIcon style={{ marginBottom: 12 }} message={t('source.iterator.intro')} />
        <Form.Item label={<FieldHelp label={t('source.from')} help={t('source.from.help')} />}>
          <InputNumber
            disabled={readOnly}
            style={{ width: '100%' }}
            value={iterator?.from ?? 1}
            onChange={(v) =>
              onPatch({
                ...source,
                iterator: { ...(iterator ?? { type: 'number' }), from: v ?? 1 },
              })
            }
          />
        </Form.Item>
        <Form.Item label={<FieldHelp label={t('source.to')} help={t('source.to.help')} />}>
          <InputNumber
            disabled={readOnly}
            style={{ width: '100%' }}
            value={iterator?.to ?? 3}
            onChange={(v) =>
              onPatch({
                ...source,
                iterator: { ...(iterator ?? { type: 'number' }), to: v ?? 3 },
              })
            }
          />
        </Form.Item>
        <Form.Item label={<FieldHelp label={t('source.step')} help={t('source.step.help')} />}>
          <InputNumber
            disabled={readOnly}
            style={{ width: '100%' }}
            value={iterator?.step ?? 1}
            onChange={(v) =>
              onPatch({
                ...source,
                iterator: { ...(iterator ?? { type: 'number' }), step: v ?? 1 },
              })
            }
          />
        </Form.Item>
      </>
    );
  }

  if (kind === 'inline_rows') {
    return (
      <InlineRowsEditor source={source} readOnly={readOnly} onPatch={onPatch} t={t} />
    );
  }

  if (kind === 'csv') {
    return (
      <>
        <Form.Item
          label={<FieldHelp label={t('source.path')} help={t('source.path.fileHelp')} required />}
        >
          <SourceFileInput
            path={(source.path as string) ?? ''}
            readOnly={readOnly}
            allowPaste
            accept=".csv,text/csv"
            defaultPasteName="source.csv"
            onPathChange={(p) => onPatch({ ...source, path: p })}
          />
        </Form.Item>
        <Form.Item label={<FieldHelp label={t('source.charset')} help={t('source.charset.help')} />}>
          <Input
            readOnly={readOnly}
            value={(source.charset as string) ?? 'UTF-8'}
            onChange={(e) => onPatch({ ...source, charset: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={<FieldHelp label={t('source.delimiter')} help={t('source.delimiter.help')} />}>
          <Input
            readOnly={readOnly}
            value={(source.delimiter as string) ?? ','}
            onChange={(e) => onPatch({ ...source, delimiter: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={<FieldHelp label={t('source.header')} help={t('source.header.help')} />}>
          <Select
            disabled={readOnly}
            value={(source.header as boolean) ?? true}
            options={yesNoOptions(t)}
            onChange={(v) => onPatch({ ...source, header: v })}
          />
        </Form.Item>
        <Form.Item label={<FieldHelp label={t('source.maxRows')} help={t('source.maxRows.help')} />}>
          <InputNumber
            min={0}
            disabled={readOnly}
            style={{ width: '100%' }}
            value={source.maxRows as number | undefined}
            onChange={(v) => onPatch({ ...source, maxRows: v ?? undefined })}
          />
        </Form.Item>
      </>
    );
  }

  if (kind === 'json') {
    return (
      <>
        <Form.Item
          label={<FieldHelp label={t('source.path')} help={t('source.path.fileHelp')} required />}
        >
          <SourceFileInput
            path={(source.path as string) ?? ''}
            readOnly={readOnly}
            allowPaste
            accept=".json,application/json"
            defaultPasteName="source.json"
            onPathChange={(p) => onPatch({ ...source, path: p })}
          />
        </Form.Item>
        <Form.Item label={<FieldHelp label={t('source.charset')} help={t('source.charset.help')} />}>
          <Input
            readOnly={readOnly}
            value={(source.charset as string) ?? 'UTF-8'}
            onChange={(e) => onPatch({ ...source, charset: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={<FieldHelp label={t('source.root')} help={t('source.root.help')} />}>
          <Input
            readOnly={readOnly}
            value={(source.root as string) ?? ''}
            onChange={(e) => onPatch({ ...source, root: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={<FieldHelp label={t('source.maxRows')} help={t('source.maxRows.help')} />}>
          <InputNumber
            min={0}
            disabled={readOnly}
            style={{ width: '100%' }}
            value={source.maxRows as number | undefined}
            onChange={(v) => onPatch({ ...source, maxRows: v ?? undefined })}
          />
        </Form.Item>
      </>
    );
  }

  if (kind === 'excel') {
    return (
      <>
        <Form.Item
          label={<FieldHelp label={t('source.path')} help={t('source.path.excelHelp')} required />}
        >
          <SourceFileInput
            path={(source.path as string) ?? ''}
            readOnly={readOnly}
            accept=".xlsx,.xls"
            onPathChange={(p) => onPatch({ ...source, path: p })}
          />
        </Form.Item>
        <Form.Item label={<FieldHelp label={t('source.sheet')} help={t('source.sheet.help')} />}>
          <Input
            readOnly={readOnly}
            value={sheetName}
            onChange={(e) =>
              onPatch({
                ...source,
                sheets: [{ name: e.target.value }],
              })
            }
          />
        </Form.Item>
        <Form.Item label={<FieldHelp label={t('source.maxRows')} help={t('source.maxRows.help')} />}>
          <InputNumber
            min={0}
            disabled={readOnly}
            style={{ width: '100%' }}
            value={source.maxRows as number | undefined}
            onChange={(v) => onPatch({ ...source, maxRows: v ?? undefined })}
          />
        </Form.Item>
      </>
    );
  }

  if (kind === 'ai') {
    return (
      <AiSourceFields source={source} readOnly={readOnly} aiCatalog={aiCatalog} onPatch={onPatch} t={t} />
    );
  }

  if (kind === 'geojson') {
    return (
      <>
        <Form.Item
          label={<FieldHelp label={t('source.path')} help={t('source.path.geoHelp')} required />}
        >
          <SourceFileInput
            path={(source.path as string) ?? ''}
            readOnly={readOnly}
            allowPaste
            accept=".json,.geojson"
            defaultPasteName="source.geojson"
            onPathChange={(p) => onPatch({ ...source, path: p })}
          />
        </Form.Item>
        <Form.Item label={<FieldHelp label={t('source.maxRows')} help={t('source.maxRows.help')} />}>
          <InputNumber
            min={0}
            disabled={readOnly}
            style={{ width: '100%' }}
            value={source.maxRows as number | undefined}
            onChange={(v) => onPatch({ ...source, maxRows: v ?? undefined })}
          />
        </Form.Item>
      </>
    );
  }

  return null;
}

type AiProviderDraft = {
  type?: string;
  options?: Record<string, unknown>;
};

function AiSourceFields({
  source,
  readOnly,
  aiCatalog,
  onPatch,
  t,
}: {
  source: SourceDraft;
  readOnly: boolean;
  aiCatalog?: AiCatalog;
  onPatch: (patch: SourceDraft) => void;
  t: (key: string) => string;
}) {
  const provider = (source.provider as AiProviderDraft | undefined) ?? {};
  const providerType = (provider.type ?? 'INLINE').toUpperCase();
  const options = (provider.options ?? {}) as Record<string, unknown>;
  const defaultInlineRows = [{ name: 'alpha', score: 10 }];
  const [rowsJson, setRowsJson] = useState(() =>
    JSON.stringify(options.rows ?? defaultInlineRows, null, 2),
  );

  useEffect(() => {
    setRowsJson(JSON.stringify(options.rows ?? defaultInlineRows, null, 2));
  }, [options.rows]);

  const patchProvider = (partial: AiProviderDraft) => {
    onPatch({
      ...source,
      provider: {
        ...provider,
        ...partial,
        options: partial.options ?? provider.options,
      },
    });
  };

  const commitInlineRows = () => {
    try {
      const parsed = JSON.parse(rowsJson) as unknown;
      if (Array.isArray(parsed)) {
        patchProvider({ options: { ...options, rows: parsed } });
      }
    } catch {
      // Keep invalid JSON in the editor until the operator fixes it.
    }
  };

  const providerOptions =
    aiCatalog?.providers.map((entry) => ({
      value: entry.type,
      label: entry.label,
    })) ?? [];

  const parserOptions =
    aiCatalog?.parsers.map((entry) => ({
      value: entry.id,
      label: entry.label,
    })) ?? [];

  const isOpenAiFamily = providerType === 'OPENAI' || providerType === 'AZURE_OPENAI';
  const apiKeySecretRef = (options.apiKeySecretRef as string | undefined) ?? undefined;

  const secretsQuery = useQuery({
    queryKey: ['secret-summaries'],
    queryFn: fetchSecretSummaries,
    enabled: isOpenAiFamily,
  });

  const secretOptions = useMemo(
    () =>
      secretsQuery.data?.map((entry) => ({
        value: entry.name,
        label: entry.description ? `${entry.name} — ${entry.description}` : entry.name,
      })) ?? [],
    [secretsQuery.data],
  );

  const providerEntry = aiCatalog?.providers.find((entry) => entry.type === providerType);
  const isRemoteProvider = providerEntry?.remote === true;

  const promptTemplateOptions =
    aiCatalog?.promptTemplates.map((entry) => ({
      value: entry.id,
      label: entry.label,
    })) ?? [];

  return (
    <>
      <Alert type="info" showIcon style={{ marginBottom: 12 }} message={t('source.ai.intro')} />
      <Form.Item label={<FieldHelp label={t('source.ai.provider')} help={t('source.ai.provider.help')} />}>
        <Select
          disabled={readOnly}
          value={providerType}
          options={providerOptions}
          onChange={(value) => patchProvider({ type: value })}
        />
      </Form.Item>
      {promptTemplateOptions.length > 0 ? (
        <Form.Item
          label={<FieldHelp label={t('source.ai.promptTemplate')} help={t('source.ai.promptTemplate.help')} />}
        >
          <Select
            allowClear
            disabled={readOnly}
            placeholder={t('source.ai.promptTemplate.placeholder')}
            options={promptTemplateOptions}
            onChange={(templateId) => {
              const template = aiCatalog?.promptTemplates.find((row) => row.id === templateId);
              if (template) {
                onPatch({ ...source, prompt: template.prompt });
              }
            }}
          />
        </Form.Item>
      ) : null}
      <Form.Item label={<FieldHelp label={t('source.ai.prompt')} help={t('source.ai.prompt.help')} />}>
        <Input.TextArea
          rows={4}
          readOnly={readOnly}
          value={(source.prompt as string) ?? ''}
          onChange={(e) => onPatch({ ...source, prompt: e.target.value })}
        />
      </Form.Item>
      <Form.Item label={<FieldHelp label={t('source.ai.parser')} help={t('source.ai.parser.help')} />}>
        {parserOptions.length > 0 ? (
          <Select
            disabled={readOnly}
            value={(source.parser as string) ?? ''}
            options={parserOptions}
            onChange={(value) => onPatch({ ...source, parser: value })}
          />
        ) : (
          <Input
            readOnly={readOnly}
            value={(source.parser as string) ?? ''}
            onChange={(e) => onPatch({ ...source, parser: e.target.value })}
          />
        )}
      </Form.Item>
      {providerType === 'INLINE' ? (
        <Form.Item
          label={<FieldHelp label={t('source.ai.inlineRows')} help={t('source.ai.inlineRows.help')} required />}
        >
          <Input.TextArea
            rows={8}
            readOnly={readOnly}
            value={rowsJson}
            onChange={(e) => setRowsJson(e.target.value)}
            onBlur={readOnly ? undefined : commitInlineRows}
          />
        </Form.Item>
      ) : null}
      {isRemoteProvider ? (
        <>
          <Form.Item label={<FieldHelp label={t('source.ai.api')} help={t('source.ai.api.help')} />}>
            <Input
              readOnly={readOnly}
              value={(source.api as string) ?? ''}
              placeholder={
                providerType === 'AZURE_OPENAI'
                  ? 'https://{resource}.openai.azure.com/openai/deployments/{deployment}/chat/completions?api-version=2024-02-15-preview'
                  : providerType === 'OPENAI'
                    ? 'https://api.openai.com'
                    : 'http://127.0.0.1:11434'
              }
              onChange={(e) => onPatch({ ...source, api: e.target.value })}
            />
          </Form.Item>
          {isOpenAiFamily ? (
            <Form.Item
              label={
                <FieldHelp label={t('source.ai.apiKeySecretRef')} help={t('source.ai.apiKeySecretRef.help')} />
              }
            >
              <Select
                allowClear
                showSearch
                disabled={readOnly}
                placeholder={t('source.ai.apiKeySecretRef.placeholder')}
                value={apiKeySecretRef}
                options={secretOptions}
                onChange={(value) =>
                  patchProvider({
                    options: {
                      ...options,
                      apiKeySecretRef: value || undefined,
                      apiKey: value ? undefined : options.apiKey,
                    },
                  })
                }
              />
            </Form.Item>
          ) : null}
          {isOpenAiFamily && !apiKeySecretRef ? (
            <Form.Item label={<FieldHelp label={t('source.ai.apiKey')} help={t('source.ai.apiKey.help')} />}>
              <Input.Password
                readOnly={readOnly}
                value={(options.apiKey as string) ?? ''}
                onChange={(e) => patchProvider({ options: { ...options, apiKey: e.target.value || undefined } })}
              />
            </Form.Item>
          ) : null}
          <Form.Item label={<FieldHelp label={t('source.ai.model')} help={t('source.ai.model.help')} />}>
            <Input
              readOnly={readOnly}
              value={(options.model as string) ?? ''}
              onChange={(e) => patchProvider({ options: { ...options, model: e.target.value || undefined } })}
            />
          </Form.Item>
          <Form.Item label={<FieldHelp label={t('source.ai.maxRetries')} help={t('source.ai.maxRetries.help')} />}>
            <InputNumber
              min={1}
              disabled={readOnly}
              style={{ width: '100%' }}
              value={(options.maxRetries as number | undefined) ?? 1}
              onChange={(value) =>
                patchProvider({ options: { ...options, maxRetries: value ?? undefined } })
              }
            />
          </Form.Item>
          <Form.Item
            label={<FieldHelp label={t('source.ai.retryBackoffMs')} help={t('source.ai.retryBackoffMs.help')} />}
          >
            <InputNumber
              min={0}
              disabled={readOnly}
              style={{ width: '100%' }}
              value={(options.retryBackoffMs as number | undefined) ?? 0}
              onChange={(value) =>
                patchProvider({ options: { ...options, retryBackoffMs: value ?? undefined } })
              }
            />
          </Form.Item>
        </>
      ) : null}
    </>
  );
}

function InlineRowsEditor({
  source,
  readOnly,
  onPatch,
  t,
}: {
  source: SourceDraft;
  readOnly: boolean;
  onPatch: (patch: SourceDraft) => void;
  t: (key: string) => string;
}) {
  const defaultRows = [{ id: 1, label: 'example' }];
  const [rowsJson, setRowsJson] = useState(() =>
    JSON.stringify(source.rows ?? defaultRows, null, 2),
  );

  useEffect(() => {
    setRowsJson(JSON.stringify(source.rows ?? defaultRows, null, 2));
  }, [source.rows]);

  const commitRows = () => {
    try {
      const parsed = JSON.parse(rowsJson) as unknown;
      if (Array.isArray(parsed)) {
        onPatch({ ...source, rows: parsed as SourceDraft['rows'] });
      }
    } catch {
      // Invalid JSON stays in the editor until the operator fixes it.
    }
  };

  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message={t('source.inlineRows.intro')}
      />
      <Form.Item
        label={
          <FieldHelp label={t('source.inlineRows.rows')} help={t('source.inlineRows.rows.help')} required />
        }
      >
        <Input.TextArea
          rows={10}
          readOnly={readOnly}
          value={rowsJson}
          onChange={(e) => setRowsJson(e.target.value)}
          onBlur={readOnly ? undefined : commitRows}
        />
      </Form.Item>
    </>
  );
}

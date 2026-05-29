import { Checkbox, Form, Input, InputNumber, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import type { SourceDraft } from '../../api/types';
import type { EditableSourceKind } from './draftUtils';

type Props = {
  kind: EditableSourceKind;
  source: SourceDraft;
  readOnly: boolean;
  jdbcNames: string[];
  onPatch: (patch: SourceDraft) => void;
};

/**
 * Type-specific source fields (query, iterator, file, AI, geojson).
 */
export function SourceFieldsForm({ kind, source, readOnly, jdbcNames, onPatch }: Props) {
  const { t } = useTranslation();
  const iterator = source.iterator;
  const sheetName =
    (source.sheets as { name?: string }[] | undefined)?.[0]?.name ?? 'Sheet1';

  if (kind === 'query') {
    return (
      <>
        <Form.Item label={t('source.datasource')}>
          <Select
            disabled={readOnly}
            showSearch
            value={source.dataSourceId as string | undefined}
            options={jdbcNames.map((n) => ({ value: n, label: n }))}
            onChange={(v) => onPatch({ ...source, dataSourceId: v })}
          />
        </Form.Item>
        <Form.Item label={t('source.sql')}>
          <Input.TextArea
            rows={6}
            readOnly={readOnly}
            value={(source.sql as string) ?? ''}
            onChange={(e) => onPatch({ ...source, sql: e.target.value })}
          />
        </Form.Item>
      </>
    );
  }

  if (kind === 'iterator') {
    return (
      <>
        <Form.Item label={t('source.from')}>
          <InputNumber
            disabled={readOnly}
            value={iterator?.from ?? 1}
            onChange={(v) =>
              onPatch({
                ...source,
                iterator: { ...(iterator ?? { type: 'number' }), from: v ?? 1 },
              })
            }
          />
        </Form.Item>
        <Form.Item label={t('source.to')}>
          <InputNumber
            disabled={readOnly}
            value={iterator?.to ?? 3}
            onChange={(v) =>
              onPatch({
                ...source,
                iterator: { ...(iterator ?? { type: 'number' }), to: v ?? 3 },
              })
            }
          />
        </Form.Item>
        <Form.Item label={t('source.step')}>
          <InputNumber
            disabled={readOnly}
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

  if (kind === 'csv') {
    return (
      <>
        <Form.Item label={t('source.path')}>
          <Input
            readOnly={readOnly}
            value={(source.path as string) ?? ''}
            onChange={(e) => onPatch({ ...source, path: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={t('source.charset')}>
          <Input
            readOnly={readOnly}
            value={(source.charset as string) ?? 'UTF-8'}
            onChange={(e) => onPatch({ ...source, charset: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={t('source.delimiter')}>
          <Input
            readOnly={readOnly}
            value={(source.delimiter as string) ?? ','}
            onChange={(e) => onPatch({ ...source, delimiter: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={t('source.header')}>
          <Checkbox
            disabled={readOnly}
            checked={(source.header as boolean) ?? true}
            onChange={(e) => onPatch({ ...source, header: e.target.checked })}
          />
        </Form.Item>
        <Form.Item label={t('source.maxRows')}>
          <InputNumber
            min={0}
            disabled={readOnly}
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
        <Form.Item label={t('source.path')}>
          <Input
            readOnly={readOnly}
            value={(source.path as string) ?? ''}
            onChange={(e) => onPatch({ ...source, path: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={t('source.charset')}>
          <Input
            readOnly={readOnly}
            value={(source.charset as string) ?? 'UTF-8'}
            onChange={(e) => onPatch({ ...source, charset: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={t('source.root')}>
          <Input
            readOnly={readOnly}
            value={(source.root as string) ?? ''}
            onChange={(e) => onPatch({ ...source, root: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={t('source.maxRows')}>
          <InputNumber
            min={0}
            disabled={readOnly}
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
        <Form.Item label={t('source.path')}>
          <Input
            readOnly={readOnly}
            value={(source.path as string) ?? ''}
            onChange={(e) => onPatch({ ...source, path: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={t('source.sheet')}>
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
        <Form.Item label={t('source.maxRows')}>
          <InputNumber
            min={0}
            disabled={readOnly}
            value={source.maxRows as number | undefined}
            onChange={(v) => onPatch({ ...source, maxRows: v ?? undefined })}
          />
        </Form.Item>
      </>
    );
  }

  if (kind === 'ai') {
    const provider = (source.provider as { type?: string } | undefined) ?? {};
    return (
      <>
        <Form.Item label={t('source.ai.api')}>
          <Input
            readOnly={readOnly}
            value={(source.api as string) ?? ''}
            onChange={(e) => onPatch({ ...source, api: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={t('source.ai.provider')}>
          <Input
            readOnly={readOnly}
            value={provider.type ?? ''}
            onChange={(e) =>
              onPatch({
                ...source,
                provider: { ...provider, type: e.target.value },
              })
            }
          />
        </Form.Item>
        <Form.Item label={t('source.ai.prompt')}>
          <Input.TextArea
            rows={4}
            readOnly={readOnly}
            value={(source.prompt as string) ?? ''}
            onChange={(e) => onPatch({ ...source, prompt: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={t('source.ai.parser')}>
          <Input
            readOnly={readOnly}
            value={(source.parser as string) ?? ''}
            onChange={(e) => onPatch({ ...source, parser: e.target.value })}
          />
        </Form.Item>
      </>
    );
  }

  if (kind === 'geojson') {
    return (
      <>
        <Form.Item label={t('source.path')}>
          <Input
            readOnly={readOnly}
            value={(source.path as string) ?? ''}
            onChange={(e) => onPatch({ ...source, path: e.target.value })}
          />
        </Form.Item>
        <Form.Item label={t('source.maxRows')}>
          <InputNumber
            min={0}
            disabled={readOnly}
            value={source.maxRows as number | undefined}
            onChange={(v) => onPatch({ ...source, maxRows: v ?? undefined })}
          />
        </Form.Item>
      </>
    );
  }

  return null;
}

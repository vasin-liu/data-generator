import { Form, Input, InputNumber, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import type { TemplateV2Draft } from '../../../api/types';
import { applyPrimarySource, readPrimarySource, readSourceType } from '../draftUtils';

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  jdbcNames: string[];
  onChange: (draft: TemplateV2Draft) => void;
};

/**
 * Primary source configuration (query JDBC or number iterator).
 */
export function SourcesStep({ draft, readOnly, jdbcNames, onChange }: Props) {
  const { t } = useTranslation();
  const sourceType = readSourceType(draft);
  const source = readPrimarySource(draft);
  const iterator = source?.iterator;

  const patch = (type: 'query' | 'iterator', fields: Parameters<typeof applyPrimarySource>[2]) => {
    onChange(applyPrimarySource(draft, type, fields));
  };

  return (
    <Form layout="vertical" style={{ maxWidth: 640 }}>
      <Form.Item label={t('source.type')}>
        <Select
          disabled={readOnly}
          value={sourceType}
          options={[
            { value: 'query', label: 'query' },
            { value: 'iterator', label: 'iterator' },
          ]}
          onChange={(v) =>
            patch(v, {
              dataSourceId: jdbcNames[0],
              sql: 'SELECT 1',
              from: 1,
              to: 3,
              step: 1,
            })
          }
        />
      </Form.Item>
      {sourceType === 'query' ? (
        <>
          <Form.Item label={t('source.datasource')}>
            <Select
              disabled={readOnly}
              showSearch
              value={source?.dataSourceId}
              options={jdbcNames.map((n) => ({ value: n, label: n }))}
              onChange={(v) => patch('query', { dataSourceId: v, sql: source?.sql })}
            />
          </Form.Item>
          <Form.Item label={t('source.sql')}>
            <Input.TextArea
              rows={6}
              readOnly={readOnly}
              value={source?.sql ?? ''}
              onChange={(e) =>
                patch('query', { dataSourceId: source?.dataSourceId, sql: e.target.value })
              }
            />
          </Form.Item>
        </>
      ) : (
        <>
          <Form.Item label={t('source.from')}>
            <InputNumber
              disabled={readOnly}
              value={iterator?.from ?? 1}
              onChange={(v) =>
                patch('iterator', {
                  from: v ?? 1,
                  to: iterator?.to,
                  step: iterator?.step,
                })
              }
            />
          </Form.Item>
          <Form.Item label={t('source.to')}>
            <InputNumber
              disabled={readOnly}
              value={iterator?.to ?? 3}
              onChange={(v) =>
                patch('iterator', {
                  from: iterator?.from,
                  to: v ?? 3,
                  step: iterator?.step,
                })
              }
            />
          </Form.Item>
          <Form.Item label={t('source.step')}>
            <InputNumber
              disabled={readOnly}
              value={iterator?.step ?? 1}
              onChange={(v) =>
                patch('iterator', {
                  from: iterator?.from,
                  to: iterator?.to,
                  step: v ?? 1,
                })
              }
            />
          </Form.Item>
        </>
      )}
    </Form>
  );
}

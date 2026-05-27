import { Form, Input, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import type { TemplateV2Draft } from '../../../api/types';
import { applyTransform, readTransformType } from '../draftUtils';

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  onChange: (draft: TemplateV2Draft) => void;
};

/**
 * SQL or SpEL transform step.
 */
export function TransformStep({ draft, readOnly, onChange }: Props) {
  const { t } = useTranslation();
  const transformType = readTransformType(draft);
  const transform = draft.transform;
  const spelCol = transform?.columns?.[0];

  const patch = (
    type: 'sql' | 'spel',
    sql: string,
    col: string,
    expr: string,
  ) => onChange(applyTransform(draft, type, sql, col, expr));

  return (
    <Form layout="vertical" style={{ maxWidth: 640 }}>
      <Form.Item label={t('transform.type')}>
        <Select
          disabled={readOnly}
          value={transformType}
          options={[
            { value: 'sql', label: 'sql' },
            { value: 'spel', label: 'spel' },
          ]}
          onChange={(v) =>
            patch(
              v,
              transform?.sql ?? '',
              spelCol?.name ?? 'value',
              spelCol?.expression ?? '#input',
            )
          }
        />
      </Form.Item>
      {transformType === 'sql' ? (
        <Form.Item label={t('transform.sql')}>
          <Input.TextArea
            rows={8}
            readOnly={readOnly}
            value={transform?.sql ?? ''}
            onChange={(e) =>
              patch('sql', e.target.value, spelCol?.name ?? '', spelCol?.expression ?? '')
            }
          />
        </Form.Item>
      ) : (
        <>
          <Form.Item label={t('transform.spel.column')}>
            <Input
              readOnly={readOnly}
              value={spelCol?.name ?? ''}
              onChange={(e) =>
                patch(
                  'spel',
                  transform?.sql ?? '',
                  e.target.value,
                  spelCol?.expression ?? '',
                )
              }
            />
          </Form.Item>
          <Form.Item label={t('transform.spel.expr')}>
            <Input.TextArea
              rows={4}
              readOnly={readOnly}
              value={spelCol?.expression ?? ''}
              onChange={(e) =>
                patch('spel', transform?.sql ?? '', spelCol?.name ?? '', e.target.value)
              }
            />
          </Form.Item>
        </>
      )}
    </Form>
  );
}

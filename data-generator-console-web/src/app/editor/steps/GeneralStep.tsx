import { Form, Input, InputNumber } from 'antd';
import { useTranslation } from 'react-i18next';
import type { TemplateV2Draft } from '../../../api/types';
import { ensureGenerator } from '../draftUtils';

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  onChange: (draft: TemplateV2Draft) => void;
};

/**
 * General metadata step (name, generator batch size).
 */
export function GeneralStep({ draft, readOnly, onChange }: Props) {
  const { t } = useTranslation();
  const withGen = ensureGenerator(draft);

  return (
    <Form layout="vertical" style={{ maxWidth: 480 }}>
      <Form.Item label={t('general.name')} required>
        <Input
          readOnly={readOnly}
          value={withGen.name ?? ''}
          onChange={(e) => onChange({ ...withGen, name: e.target.value })}
        />
      </Form.Item>
      <Form.Item label={t('general.batchSize')}>
        <InputNumber
          min={1}
          disabled={readOnly}
          value={withGen.generator?.batchSize ?? 100}
          onChange={(v) =>
            onChange({
              ...withGen,
              generator: { ...withGen.generator, batchSize: v ?? 100 },
            })
          }
        />
      </Form.Item>
    </Form>
  );
}

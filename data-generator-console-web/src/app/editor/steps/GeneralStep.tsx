import { Form, Input, InputNumber, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import type { GeneratorDraft, TemplateV2Draft } from '../../../api/types';
import { FieldHelp } from '../../../components/FieldHelp';
import { labeledOptions } from '../../utils/optionLabels';
import { ensureGenerator, patchGenerator } from '../draftUtils';

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  onChange: (draft: TemplateV2Draft) => void;
};

const GENERATOR_TYPES = ['SYNC', 'ASYNC'] as const;

/**
 * General metadata step (name, generator type and batch size).
 */
export function GeneralStep({ draft, readOnly, onChange }: Props) {
  const { t } = useTranslation();
  const withGen = ensureGenerator(draft);
  const generator = withGen.generator ?? {};
  const generatorType = generator.type?.toUpperCase();
  const isAsync = generatorType === 'ASYNC';

  const patchGen = (partial: GeneratorDraft) => {
    onChange(patchGenerator(withGen, partial));
  };

  return (
    <Form layout="vertical" style={{ maxWidth: 480 }}>
      <Form.Item label={<FieldHelp label={t('general.name')} help={t('general.name.help')} required />}>
        <Input
          readOnly={readOnly}
          value={withGen.name ?? ''}
          onChange={(e) => onChange({ ...withGen, name: e.target.value })}
        />
      </Form.Item>
      <Form.Item
        label={<FieldHelp label={t('general.generatorType')} help={t('general.generatorType.help')} />}
      >
        <Select
          disabled={readOnly}
          allowClear
          placeholder={t('general.generatorType.default')}
          value={GENERATOR_TYPES.includes(generatorType as (typeof GENERATOR_TYPES)[number])
            ? generatorType
            : undefined}
          options={labeledOptions(t, 'general.generatorType', GENERATOR_TYPES)}
          onChange={(v) => {
            if (!v) {
              patchGen({ type: undefined });
              return;
            }
            if (v === 'ASYNC') {
              patchGen({
                type: v,
                executor: generator.executor ?? { coreSize: 8, maxSize: 16 },
              });
            } else {
              patchGen({ type: v });
            }
          }}
        />
      </Form.Item>
      <Form.Item label={<FieldHelp label={t('general.batchSize')} help={t('general.batchSize.help')} />}>
        <InputNumber
          min={1}
          disabled={readOnly}
          value={generator.batchSize ?? 100}
          onChange={(v) => patchGen({ batchSize: v ?? 100 })}
        />
      </Form.Item>
      {isAsync && (
        <>
          <Form.Item label={t('general.executor.coreSize')}>
            <InputNumber
              min={1}
              disabled={readOnly}
              value={generator.executor?.coreSize ?? 8}
              onChange={(v) =>
                patchGen({
                  executor: { ...generator.executor, coreSize: v ?? 8 },
                })
              }
            />
          </Form.Item>
          <Form.Item label={t('general.executor.maxSize')}>
            <InputNumber
              min={1}
              disabled={readOnly}
              value={generator.executor?.maxSize ?? 16}
              onChange={(v) =>
                patchGen({
                  executor: { ...generator.executor, maxSize: v ?? 16 },
                })
              }
            />
          </Form.Item>
        </>
      )}
    </Form>
  );
}

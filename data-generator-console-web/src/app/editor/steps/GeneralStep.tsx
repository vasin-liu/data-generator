import { Form, Input, InputNumber, Select } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import type { GeneratorDraft, TemplateV2Draft } from '../../../api/types';
import { fetchTemplateTaxonomy } from '../../../api/templates';
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
 * General metadata step (name, category, tags, generator).
 */
export function GeneralStep({ draft, readOnly, onChange }: Props) {
  const { t } = useTranslation();
  const withGen = ensureGenerator(draft);
  const generator = withGen.generator ?? {};
  const generatorType = generator.type?.toUpperCase() ?? 'SYNC';
  const isAsync = generatorType === 'ASYNC';

  const taxonomyQuery = useQuery({
    queryKey: ['template-taxonomy'],
    queryFn: fetchTemplateTaxonomy,
  });

  const patchGen = (partial: GeneratorDraft) => {
    onChange(patchGenerator(withGen, partial));
  };

  const tagOptions = (taxonomyQuery.data?.tags ?? []).map((tag) => ({ value: tag, label: tag }));

  return (
    <Form layout="vertical" style={{ maxWidth: 480 }}>
      <Form.Item label={<FieldHelp label={t('general.name')} help={t('general.name.help')} required />}>
        <Input
          data-testid="editor-template-name"
          readOnly={readOnly}
          value={withGen.name ?? ''}
          onChange={(e) => onChange({ ...withGen, name: e.target.value })}
        />
      </Form.Item>
      <Form.Item label={<FieldHelp label={t('general.category')} help={t('general.category.help')} />}>
        <Input
          readOnly={readOnly}
          list="template-categories"
          placeholder={t('general.category.placeholder')}
          value={withGen.category ?? ''}
          onChange={(e) => onChange({ ...withGen, category: e.target.value || undefined })}
        />
        <datalist id="template-categories">
          {(taxonomyQuery.data?.categories ?? []).map((c) => (
            <option key={c} value={c} />
          ))}
        </datalist>
      </Form.Item>
      <Form.Item label={<FieldHelp label={t('general.tags')} help={t('general.tags.help')} />}>
        <Select
          disabled={readOnly}
          mode="tags"
          allowClear
          placeholder={t('general.tags.placeholder')}
          value={withGen.tags ?? []}
          options={tagOptions}
          onChange={(values) => onChange({ ...withGen, tags: values })}
        />
      </Form.Item>
      <Form.Item
        label={<FieldHelp label={t('general.generatorType')} help={t('general.generatorType.help')} />}
      >
        <Select
          disabled={readOnly}
          value={GENERATOR_TYPES.includes(generatorType as (typeof GENERATOR_TYPES)[number])
            ? generatorType
            : 'SYNC'}
          options={labeledOptions(t, 'general.generatorType', GENERATOR_TYPES)}
          onChange={(v) => {
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

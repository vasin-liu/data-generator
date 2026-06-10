import { Form, Input, Select } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import type { TemplateV2Draft } from '../../../api/types';
import { fetchTemplateTaxonomy } from '../../../api/templates';
import { FieldHelp } from '../../../components/FieldHelp';

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  onChange: (draft: TemplateV2Draft) => void;
};

/**
 * General metadata step (name, category, tags).
 */
export function GeneralStep({ draft, readOnly, onChange }: Props) {
  const { t } = useTranslation();

  const taxonomyQuery = useQuery({
    queryKey: ['template-taxonomy'],
    queryFn: fetchTemplateTaxonomy,
  });

  const tagOptions = (taxonomyQuery.data?.tags ?? []).map((tag) => ({ value: tag, label: tag }));

  return (
    <Form layout="vertical" style={{ maxWidth: 480 }}>
      <Form.Item label={<FieldHelp label={t('general.name')} help={t('general.name.help')} required />}>
        <Input
          data-testid="editor-template-name"
          readOnly={readOnly}
          value={draft.name ?? ''}
          onChange={(e) => onChange({ ...draft, name: e.target.value })}
        />
      </Form.Item>
      <Form.Item label={<FieldHelp label={t('general.category')} help={t('general.category.help')} />}>
        <Input
          readOnly={readOnly}
          list="template-categories"
          placeholder={t('general.category.placeholder')}
          value={draft.category ?? ''}
          onChange={(e) => onChange({ ...draft, category: e.target.value || undefined })}
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
          value={draft.tags ?? []}
          options={tagOptions}
          onChange={(values) => onChange({ ...draft, tags: values })}
        />
      </Form.Item>
    </Form>
  );
}

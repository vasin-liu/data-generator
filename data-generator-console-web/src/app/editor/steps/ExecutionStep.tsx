import { Form, InputNumber, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import type { TemplateV2Draft } from '../../../api/types';
import { cloneDraft } from '../draftUtils';

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  onChange: (draft: TemplateV2Draft) => void;
};

/**
 * Execution policy step.
 */
export function ExecutionStep({ draft, readOnly, onChange }: Props) {
  const { t } = useTranslation();
  const policy = draft.executionPolicy ?? {};

  const patchPolicy = (partial: typeof policy) => {
    const next = cloneDraft(draft);
    next.executionPolicy = { ...policy, ...partial };
    onChange(next);
  };

  return (
    <Form layout="vertical" style={{ maxWidth: 480 }}>
      <Form.Item label={t('execution.mode')}>
        <Select
          disabled={readOnly}
          allowClear
          value={policy.mode}
          options={[
            { value: 'BOUNDED', label: 'BOUNDED' },
            { value: 'CHUNKED', label: 'CHUNKED' },
          ]}
          onChange={(v) => patchPolicy({ mode: v })}
        />
      </Form.Item>
      <Form.Item label={t('execution.sourceChunk')}>
        <InputNumber
          disabled={readOnly}
          value={policy.sourceChunkSize}
          onChange={(v) => patchPolicy({ sourceChunkSize: v ?? undefined })}
        />
      </Form.Item>
      <Form.Item label={t('execution.sinkBatch')}>
        <InputNumber
          disabled={readOnly}
          value={policy.sinkBatchSize}
          onChange={(v) => patchPolicy({ sinkBatchSize: v ?? undefined })}
        />
      </Form.Item>
      <Form.Item label={t('execution.previewLimit')}>
        <InputNumber
          disabled={readOnly}
          value={policy.previewRowLimit}
          onChange={(v) => patchPolicy({ previewRowLimit: v ?? undefined })}
        />
      </Form.Item>
    </Form>
  );
}

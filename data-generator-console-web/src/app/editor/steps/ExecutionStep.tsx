import { Checkbox, Divider, Form, InputNumber, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import type { TemplateV2Draft } from '../../../api/types';
import { FieldHelp } from '../../../components/FieldHelp';
import { labeledOptions } from '../../utils/optionLabels';
import { patchExecutionPolicy, patchSinkExecutionPolicy } from '../draftUtils';

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  onChange: (draft: TemplateV2Draft) => void;
};

const EXECUTION_MODES = ['IN_MEMORY', 'CHUNKED'] as const;
const SINK_EXEC_MODES = ['FAIL_FAST', 'CONTINUE_ON_ERROR'] as const;

/**
 * Execution and sink execution policy step.
 */
export function ExecutionStep({ draft, readOnly, onChange }: Props) {
  const { t } = useTranslation();
  const policy = draft.executionPolicy ?? {};
  const sinkPolicy = draft.sinkExecutionPolicy ?? {};

  return (
    <Form layout="vertical" style={{ maxWidth: 520 }}>
      <Divider orientation="left" plain>
        {t('execution.section.pipeline')}
      </Divider>
      <Form.Item label={<FieldHelp label={t('execution.mode')} help={t('execution.mode.help')} />}>
        <Select
          disabled={readOnly}
          allowClear
          placeholder={t('execution.mode.default')}
          value={policy.mode}
          options={labeledOptions(t, 'execution.mode', EXECUTION_MODES)}
          onChange={(v) => onChange(patchExecutionPolicy(draft, { mode: v }))}
        />
      </Form.Item>
      <Form.Item label={t('execution.maxRowsInMemory')}>
        <InputNumber
          min={1}
          disabled={readOnly}
          value={policy.maxRowsInMemory}
          onChange={(v) =>
            onChange(patchExecutionPolicy(draft, { maxRowsInMemory: v ?? undefined }))
          }
        />
      </Form.Item>
      <Form.Item label={t('execution.broadcastMaxRows')}>
        <InputNumber
          min={1}
          disabled={readOnly}
          value={policy.broadcastMaxRows}
          onChange={(v) =>
            onChange(patchExecutionPolicy(draft, { broadcastMaxRows: v ?? undefined }))
          }
        />
      </Form.Item>
      <Form.Item label={t('execution.sourceChunk')}>
        <InputNumber
          min={1}
          disabled={readOnly}
          value={policy.sourceChunkSize}
          onChange={(v) =>
            onChange(patchExecutionPolicy(draft, { sourceChunkSize: v ?? undefined }))
          }
        />
      </Form.Item>
      <Form.Item label={t('execution.sinkBatch')}>
        <InputNumber
          min={1}
          disabled={readOnly}
          value={policy.sinkBatchSize}
          onChange={(v) =>
            onChange(patchExecutionPolicy(draft, { sinkBatchSize: v ?? undefined }))
          }
        />
      </Form.Item>
      <Form.Item label={t('execution.previewLimit')}>
        <InputNumber
          min={1}
          disabled={readOnly}
          value={policy.previewRowLimit}
          onChange={(v) =>
            onChange(patchExecutionPolicy(draft, { previewRowLimit: v ?? undefined }))
          }
        />
      </Form.Item>
      <Form.Item label={t('execution.failOnLimitExceeded')}>
        <Checkbox
          disabled={readOnly}
          checked={policy.failOnLimitExceeded ?? false}
          onChange={(e) =>
            onChange(patchExecutionPolicy(draft, { failOnLimitExceeded: e.target.checked }))
          }
        >
          {t('execution.failOnLimitExceeded.hint')}
        </Checkbox>
      </Form.Item>

      <Divider orientation="left" plain>
        {t('execution.section.sink')}
      </Divider>
      <Form.Item label={<FieldHelp label={t('execution.sinkMode')} help={t('execution.sinkMode.help')} />}>
        <Select
          disabled={readOnly}
          allowClear
          placeholder={t('execution.sinkMode.default')}
          value={sinkPolicy.mode}
          options={labeledOptions(t, 'execution.sinkMode', SINK_EXEC_MODES)}
          onChange={(v) => onChange(patchSinkExecutionPolicy(draft, { mode: v }))}
        />
      </Form.Item>
    </Form>
  );
}

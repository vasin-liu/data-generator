import { Alert, Checkbox, Divider, Form, Input, InputNumber, Select } from 'antd';
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

const EXECUTION_MODES = ['IN_MEMORY', 'CHUNKED', 'STREAMING'] as const;
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
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('execution.intro.title')}
        description={t('execution.intro.body')}
      />
      <Form.Item
        label={<FieldHelp label={t('execution.mode')} help={t('execution.mode.help')} />}
        extra={
          policy.mode
            ? t(`execution.mode.hint.${policy.mode}` as 'execution.mode.hint.IN_MEMORY')
            : t('execution.mode.hint.default')
        }
      >
        <Select
          disabled={readOnly}
          allowClear
          placeholder={t('execution.mode.default')}
          value={policy.mode}
          options={labeledOptions(t, 'execution.mode', EXECUTION_MODES)}
          onChange={(v) => onChange(patchExecutionPolicy(draft, { mode: v }))}
        />
      </Form.Item>
      <Form.Item label={t('execution.maxTotalRows')}>
        <InputNumber
          min={1}
          disabled={readOnly}
          value={policy.maxTotalRows as number | undefined}
          onChange={(v) =>
            onChange(patchExecutionPolicy(draft, { maxTotalRows: v ?? undefined }))
          }
        />
      </Form.Item>
      <Form.Item label={t('execution.partitionCount')}>
        <InputNumber
          min={1}
          disabled={readOnly}
          value={policy.partitionCount as number | undefined}
          onChange={(v) =>
            onChange(patchExecutionPolicy(draft, { partitionCount: v ?? undefined }))
          }
        />
      </Form.Item>
      <Form.Item label={t('execution.partitionKey')}>
        <Input
          readOnly={readOnly}
          value={(policy.partitionKey as string | undefined) ?? ''}
          placeholder={t('execution.partitionKey.placeholder')}
          onChange={(e) =>
            onChange(patchExecutionPolicy(draft, { partitionKey: e.target.value || undefined }))
          }
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
      <Form.Item label={t('execution.sinkMaxRetries')}>
        <InputNumber
          disabled={readOnly}
          min={1}
          style={{ width: '100%' }}
          value={sinkPolicy.maxRetries}
          onChange={(v) =>
            onChange(patchSinkExecutionPolicy(draft, { maxRetries: v ?? undefined }))
          }
        />
      </Form.Item>
      <Form.Item label={t('execution.sinkRetryBackoffMs')}>
        <InputNumber
          disabled={readOnly}
          min={0}
          style={{ width: '100%' }}
          value={sinkPolicy.retryBackoffMs}
          onChange={(v) =>
            onChange(patchSinkExecutionPolicy(draft, { retryBackoffMs: v ?? undefined }))
          }
        />
      </Form.Item>
      <Form.Item label={t('execution.parallelSinks')}>
        <Checkbox
          disabled={readOnly}
          checked={sinkPolicy.parallelSinks ?? false}
          onChange={(e) =>
            onChange(patchSinkExecutionPolicy(draft, { parallelSinks: e.target.checked }))
          }
        >
          {t('execution.parallelSinks.hint')}
        </Checkbox>
      </Form.Item>
    </Form>
  );
}

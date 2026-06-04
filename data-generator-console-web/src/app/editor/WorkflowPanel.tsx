import { Button, Input, message, Select, Space, Switch, Table, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { EditorDataSources, TemplateV2Draft, WorkflowStepDraft } from '../../api/types';
import { ComputeBlockEditor } from './ComputeBlockEditor';
import {
  WORKFLOW_STEP_TYPES,
  addComputeBlock,
  addWorkflowStep,
  applyComputeBlockAt,
  applyStepParamsJson,
  applyWorkflowStepAt,
  disableWorkflow,
  enableWorkflow,
  hasWorkflow,
  listComputeBlocks,
  listWorkflowSteps,
  removeComputeBlockAt,
  removeWorkflowStepAt,
  stepParamsJson,
  type WorkflowStepType,
} from './workflowUtils';

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  editorDataSources: EditorDataSources;
  onChange: (draft: TemplateV2Draft) => void;
};

/**
 * Minimal L2 workflow editor with compute block scoped editing.
 */
export function WorkflowPanel({ draft, readOnly, editorDataSources, onChange }: Props) {
  const { t } = useTranslation();
  const workflowEnabled = hasWorkflow(draft);
  const steps = listWorkflowSteps(draft);
  const blocks = listComputeBlocks(draft);
  const [selectedBlockIndex, setSelectedBlockIndex] = useState(0);
  const [paramsDraft, setParamsDraft] = useState<Record<number, string>>({});

  useEffect(() => {
    if (blocks.length === 0) {
      setSelectedBlockIndex(0);
      return;
    }
    if (selectedBlockIndex >= blocks.length) {
      setSelectedBlockIndex(blocks.length - 1);
    }
  }, [blocks.length, selectedBlockIndex]);

  const stepRows = steps.map((step, index) => ({
    key: step.id ?? index,
    index,
    ...step,
    paramsText: paramsDraft[index] ?? stepParamsJson(step),
  }));

  const applyParams = (index: number, jsonText: string) => {
    setParamsDraft((prev) => ({ ...prev, [index]: jsonText }));
    try {
      const step = steps[index] ?? {};
      const merged = applyStepParamsJson(step, jsonText);
      onChange(applyWorkflowStepAt(draft, index, merged));
    } catch {
      message.error(t('workflow.steps.paramsInvalid'));
    }
  };

  const patchStep = (index: number, patch: WorkflowStepDraft) => {
    onChange(applyWorkflowStepAt(draft, index, patch));
    setParamsDraft((prev) => {
      const next = { ...prev };
      delete next[index];
      return next;
    });
  };

  return (
    <div style={{ maxWidth: 960 }}>
      <Space style={{ marginBottom: 16 }}>
        <Typography.Text>{t('workflow.enabled')}</Typography.Text>
        <Switch
          disabled={readOnly}
          checked={workflowEnabled}
          onChange={(checked) => onChange(checked ? enableWorkflow(draft) : disableWorkflow(draft))}
        />
      </Space>

      {!workflowEnabled ? (
        <Typography.Paragraph type="secondary">{t('workflow.disabledHint')}</Typography.Paragraph>
      ) : (
        <>
          <Typography.Title level={5}>{t('workflow.steps.title')}</Typography.Title>
          <Space style={{ marginBottom: 8 }}>
            <Button disabled={readOnly} onClick={() => onChange(addWorkflowStep(draft, 'log'))}>
              {t('workflow.steps.add')}
            </Button>
          </Space>
          <Table
            size="small"
            pagination={false}
            dataSource={stepRows}
            locale={{ emptyText: t('workflow.steps.empty') }}
            style={{ marginBottom: 24 }}
            columns={[
              {
                title: t('workflow.steps.type'),
                dataIndex: 'type',
                width: 180,
                render: (_value, row) => (
                  <Select
                    disabled={readOnly}
                    style={{ width: '100%' }}
                    value={(row.type ?? 'log') as WorkflowStepType}
                    options={WORKFLOW_STEP_TYPES.map((type) => ({ value: type, label: type }))}
                    onChange={(type: WorkflowStepType) =>
                      patchStep(row.index, { ...steps[row.index], type })
                    }
                  />
                ),
              },
              {
                title: t('workflow.steps.computeBlockId'),
                dataIndex: 'computeBlockId',
                width: 160,
                render: (_value, row) => (
                  <Input
                    readOnly={readOnly}
                    disabled={row.type !== 'invoke_compute_block'}
                    value={row.computeBlockId ?? ''}
                    placeholder={row.type === 'invoke_compute_block' ? 'block-1' : '—'}
                    onChange={(e) => patchStep(row.index, { ...row, computeBlockId: e.target.value })}
                  />
                ),
              },
              {
                title: t('workflow.steps.params'),
                dataIndex: 'paramsText',
                render: (_value, row) => (
                  <Input.TextArea
                    rows={2}
                    readOnly={readOnly}
                    value={row.paramsText}
                    placeholder='{"level":"INFO","message":"..."}'
                    onChange={(e) =>
                      setParamsDraft((prev) => ({ ...prev, [row.index]: e.target.value }))
                    }
                    onBlur={(e) => applyParams(row.index, e.target.value)}
                  />
                ),
              },
              {
                title: '',
                key: 'actions',
                width: 88,
                render: (_value, row) => (
                  <Button
                    type="link"
                    danger
                    disabled={readOnly}
                    onClick={() => onChange(removeWorkflowStepAt(draft, row.index))}
                  >
                    {t('common.remove')}
                  </Button>
                ),
              },
            ]}
          />

          <Typography.Title level={5}>{t('workflow.blocks.title')}</Typography.Title>
          <Space style={{ marginBottom: 12 }}>
            <Select<number>
              style={{ minWidth: 200 }}
              value={selectedBlockIndex}
              options={blocks.map((block, index) => ({
                value: index,
                label: block.id ?? `block-${index + 1}`,
              }))}
              onChange={(index) => setSelectedBlockIndex(index)}
            />
            <Button
              disabled={readOnly}
              onClick={() => {
                const next = addComputeBlock(draft);
                onChange(next);
                setSelectedBlockIndex(listComputeBlocks(next).length - 1);
              }}
            >
              {t('workflow.blocks.add')}
            </Button>
            <Button
              danger
              disabled={readOnly || blocks.length === 0}
              onClick={() => onChange(removeComputeBlockAt(draft, selectedBlockIndex))}
            >
              {t('workflow.blocks.remove')}
            </Button>
          </Space>

          {blocks[selectedBlockIndex] ? (
            <ComputeBlockEditor
              block={blocks[selectedBlockIndex]}
              readOnly={readOnly}
              editorDataSources={editorDataSources}
              onChange={(block) => onChange(applyComputeBlockAt(draft, selectedBlockIndex, block))}
            />
          ) : (
            <Typography.Paragraph type="secondary">{t('workflow.blocks.empty')}</Typography.Paragraph>
          )}
        </>
      )}
    </div>
  );
}

import { Alert, Button, Input, message, Select, Space, Switch, Table, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { EditorDataSources, TemplateV2Draft, WorkflowStepDraft } from '../../api/types';
import { FieldHelp } from '../../components/FieldHelp';
import { labeledOptions } from '../utils/optionLabels';
import { ComputeBlockEditor } from './ComputeBlockEditor';
import { WorkflowStepFields } from './WorkflowStepFields';
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

function stepFieldsText(step: WorkflowStepDraft): string {
  if (step.type === 'shared_scope' && step.action === 'write') {
    const entries = step.entries;
    return entries && typeof entries === 'object' ? JSON.stringify(entries, null, 2) : '';
  }
  return stepParamsJson(step);
}

/**
 * L2 workflow editor with structured step fields and compute block scoped editing.
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
    paramsText: paramsDraft[index] ?? stepFieldsText(step),
  }));

  const applyParams = (index: number, jsonText: string) => {
    setParamsDraft((prev) => ({ ...prev, [index]: jsonText }));
    try {
      const step = steps[index] ?? {};
      if (step.type === 'shared_scope' && step.action === 'write') {
        const entries = jsonText.trim() ? (JSON.parse(jsonText) as Record<string, unknown>) : {};
        onChange(applyWorkflowStepAt(draft, index, { ...step, entries }));
        return;
      }
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
    <div style={{ maxWidth: 960 }} data-testid="workflow-panel">
      <Space style={{ marginBottom: 16 }}>
        <Typography.Text>{t('workflow.enabled')}</Typography.Text>
        <Switch
          data-testid="workflow-enabled-switch"
          disabled={readOnly}
          checked={workflowEnabled}
          onChange={(checked) => onChange(checked ? enableWorkflow(draft) : disableWorkflow(draft))}
        />
      </Space>

      {!workflowEnabled ? (
        <Typography.Paragraph type="secondary">{t('workflow.disabledHint')}</Typography.Paragraph>
      ) : (
        <>
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 16, maxWidth: 900 }}
            message={t('workflow.page.intro.title')}
            description={t('workflow.page.intro.body')}
          />
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 16, maxWidth: 900 }}
            message={t('workflow.sharedScope.hint.title')}
            description={t('workflow.sharedScope.hint.body')}
          />
          <Typography.Title level={5}>{t('workflow.steps.title')}</Typography.Title>
          <Space style={{ marginBottom: 8 }}>
            <Button
              data-testid="workflow-add-step"
              disabled={readOnly}
              onClick={() => onChange(addWorkflowStep(draft, 'log'))}
            >
              {t('workflow.steps.add')}
            </Button>
          </Space>
          <Table
            data-testid="workflow-steps-table"
            size="small"
            pagination={false}
            dataSource={stepRows}
            locale={{ emptyText: t('workflow.steps.empty') }}
            style={{ marginBottom: 24 }}
            columns={[
              {
                title: <FieldHelp label={t('workflow.steps.id')} help={t('workflow.steps.id.help')} />,
                dataIndex: 'id',
                width: 140,
                render: (_value, row) => (
                  <Input
                    readOnly={readOnly}
                    value={row.id ?? ''}
                    onChange={(e) => patchStep(row.index, { ...steps[row.index], id: e.target.value })}
                  />
                ),
              },
              {
                title: <FieldHelp label={t('workflow.steps.type')} help={t('workflow.steps.type.help')} />,
                dataIndex: 'type',
                width: 200,
                render: (_value, row) => (
                  <Select
                    disabled={readOnly}
                    style={{ width: '100%' }}
                    value={(row.type ?? 'log') as WorkflowStepType}
                    options={labeledOptions(t, 'workflow.steps.type', WORKFLOW_STEP_TYPES)}
                    onChange={(type: WorkflowStepType) =>
                      patchStep(row.index, { ...steps[row.index], type })
                    }
                  />
                ),
              },
              {
                title: <FieldHelp label={t('workflow.steps.fields')} help={t('workflow.steps.fields.help')} />,
                dataIndex: 'paramsText',
                render: (_value, row) => (
                  <WorkflowStepFields
                    draft={draft}
                    step={steps[row.index] ?? {}}
                    readOnly={readOnly}
                    paramsText={row.paramsText}
                    onPatch={(patch) => patchStep(row.index, patch)}
                    onParamsTextChange={(text) =>
                      setParamsDraft((prev) => ({ ...prev, [row.index]: text }))
                    }
                    onParamsBlur={(text) => applyParams(row.index, text)}
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

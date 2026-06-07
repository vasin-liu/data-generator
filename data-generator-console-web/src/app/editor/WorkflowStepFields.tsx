import { Input, InputNumber, Select, Switch } from 'antd';
import { useTranslation } from 'react-i18next';
import type { TemplateV2Draft, WorkflowStepDraft } from '../../api/types';
import { FieldHelp } from '../../components/FieldHelp';
import { labeledOptions } from '../utils/optionLabels';
import {
  LOG_LEVELS,
  SHARED_SCOPE_ACTIONS,
  listComputeBlockIdOptions,
  patchBranchStep,
  readBranchLogBranch,
  type WorkflowStepType,
} from './workflowUtils';

type Props = {
  draft: TemplateV2Draft;
  step: WorkflowStepDraft;
  readOnly: boolean;
  paramsText: string;
  onPatch: (patch: WorkflowStepDraft) => void;
  onParamsTextChange: (text: string) => void;
  onParamsBlur: (text: string) => void;
};

/**
 * Structured workflow step fields; falls back to JSON for branch/advanced steps.
 */
export function WorkflowStepFields({
  draft,
  step,
  readOnly,
  paramsText,
  onPatch,
  onParamsTextChange,
  onParamsBlur,
}: Props) {
  const { t } = useTranslation();
  const stepType = (step.type ?? 'log') as WorkflowStepType;

  if (stepType === 'log') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div>
          <FieldHelp label={t('workflow.steps.log.level')} help={t('workflow.steps.log.level.help')} />
          <Select
            disabled={readOnly}
            style={{ width: '100%' }}
            value={typeof step.level === 'string' ? step.level : 'INFO'}
            options={LOG_LEVELS.map((level) => ({ value: level, label: level }))}
            onChange={(level) => onPatch({ ...step, level })}
          />
        </div>
        <div>
          <FieldHelp label={t('workflow.steps.log.message')} help={t('workflow.steps.log.message.help')} />
          <Input
            readOnly={readOnly}
            value={typeof step.message === 'string' ? step.message : ''}
            placeholder={t('workflow.steps.log.messagePlaceholder')}
            onChange={(e) => onPatch({ ...step, message: e.target.value })}
          />
        </div>
      </div>
    );
  }

  if (stepType === 'pause') {
    const manual = step.manual === true;
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div>
          <FieldHelp label={t('workflow.steps.pause.manual')} help={t('workflow.steps.pause.manual.help')} />
          <Switch
            disabled={readOnly}
            checked={manual}
            onChange={(checked) =>
              onPatch(
                checked
                  ? { ...step, manual: true, durationMs: undefined, until: undefined, condition: undefined }
                  : { ...step, manual: undefined, durationMs: step.durationMs ?? 50 },
              )
            }
          />
        </div>
        {!manual && (
          <div>
            <FieldHelp
              label={t('workflow.steps.pause.durationMs')}
              help={t('workflow.steps.pause.durationMs.help')}
            />
            <InputNumber
              readOnly={readOnly}
              style={{ width: '100%' }}
              min={1}
              value={typeof step.durationMs === 'number' ? step.durationMs : 50}
              onChange={(value) => onPatch({ ...step, manual: undefined, durationMs: value ?? 50 })}
            />
          </div>
        )}
      </div>
    );
  }

  if (stepType === 'invoke_compute_block') {
    const blockOptions = listComputeBlockIdOptions(draft);
    return (
      <div>
        <FieldHelp
          label={t('workflow.steps.computeBlockId')}
          help={t('workflow.steps.computeBlockId.help')}
        />
        <Select
          disabled={readOnly}
          style={{ width: '100%' }}
          showSearch
          allowClear
          placeholder={t('workflow.steps.computeBlockIdPlaceholder')}
          value={step.computeBlockId ?? undefined}
          options={blockOptions}
          onChange={(computeBlockId) => onPatch({ ...step, computeBlockId: computeBlockId ?? '' })}
        />
      </div>
    );
  }

  if (stepType === 'shared_scope') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div>
          <FieldHelp label={t('workflow.steps.sharedScope.scopeId')} help={t('workflow.steps.sharedScope.scopeId.help')} />
          <Input
            readOnly={readOnly}
            value={typeof step.scopeId === 'string' ? step.scopeId : ''}
            placeholder="run-scope"
            onChange={(e) => onPatch({ ...step, scopeId: e.target.value })}
          />
        </div>
        <div>
          <FieldHelp label={t('workflow.steps.sharedScope.action')} help={t('workflow.steps.sharedScope.action.help')} />
          <Select
            disabled={readOnly}
            style={{ width: '100%' }}
            value={typeof step.action === 'string' ? step.action : 'open'}
            options={labeledOptions(t, 'workflow.steps.sharedScope.action', SHARED_SCOPE_ACTIONS)}
            onChange={(action) => onPatch({ ...step, action })}
          />
        </div>
        {step.action === 'write' && (
          <div>
            <FieldHelp label={t('workflow.steps.params')} help={t('workflow.steps.sharedScope.entries.help')} />
            <Input.TextArea
              rows={2}
              readOnly={readOnly}
              value={paramsText}
              placeholder='{"limit": 2}'
              onChange={(e) => onParamsTextChange(e.target.value)}
              onBlur={(e) => onParamsBlur(e.target.value)}
            />
          </div>
        )}
      </div>
    );
  }

  if (stepType === 'branch') {
    const thenSteps = Array.isArray(step.thenSteps) ? (step.thenSteps as WorkflowStepDraft[]) : undefined;
    const elseSteps = Array.isArray(step.elseSteps) ? (step.elseSteps as WorkflowStepDraft[]) : undefined;
    const thenBranch = readBranchLogBranch(thenSteps);
    const elseBranch = readBranchLogBranch(elseSteps);
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }} data-testid="workflow-branch-fields">
        <div>
          <FieldHelp label={t('workflow.steps.branch.condition')} help={t('workflow.steps.branch.condition.help')} />
          <Input
            readOnly={readOnly}
            value={typeof step.condition === 'string' ? step.condition : 'true'}
            placeholder="true"
            onChange={(e) => onPatch(patchBranchStep(step, { condition: e.target.value }))}
          />
        </div>
        <div>
          <FieldHelp label={t('workflow.steps.branch.thenLevel')} help={t('workflow.steps.branch.thenLevel.help')} />
          <Select
            disabled={readOnly}
            style={{ width: '100%' }}
            value={thenBranch.level}
            options={LOG_LEVELS.map((level) => ({ value: level, label: level }))}
            onChange={(level) => onPatch(patchBranchStep(step, { thenLevel: level }))}
          />
        </div>
        <div>
          <FieldHelp label={t('workflow.steps.branch.thenMessage')} help={t('workflow.steps.branch.thenMessage.help')} />
          <Input
            readOnly={readOnly}
            value={thenBranch.message}
            placeholder="then-branch"
            onChange={(e) => onPatch(patchBranchStep(step, { thenMessage: e.target.value }))}
          />
        </div>
        <div>
          <FieldHelp label={t('workflow.steps.branch.elseLevel')} help={t('workflow.steps.branch.elseLevel.help')} />
          <Select
            disabled={readOnly}
            style={{ width: '100%' }}
            value={elseBranch.level}
            options={LOG_LEVELS.map((level) => ({ value: level, label: level }))}
            onChange={(level) => onPatch(patchBranchStep(step, { elseLevel: level }))}
          />
        </div>
        <div>
          <FieldHelp label={t('workflow.steps.branch.elseMessage')} help={t('workflow.steps.branch.elseMessage.help')} />
          <Input
            readOnly={readOnly}
            value={elseBranch.message}
            placeholder="else-branch"
            onChange={(e) => onPatch(patchBranchStep(step, { elseMessage: e.target.value }))}
          />
        </div>
      </div>
    );
  }

  return (
    <div>
      <FieldHelp label={t('workflow.steps.params')} help={t('workflow.steps.params.help')} />
      <Input.TextArea
        rows={2}
        readOnly={readOnly}
        value={paramsText}
        placeholder='{"condition":"true","thenSteps":[]}'
        onChange={(e) => onParamsTextChange(e.target.value)}
        onBlur={(e) => onParamsBlur(e.target.value)}
      />
    </div>
  );
}

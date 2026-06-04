import { Input, Radio, Tabs, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { ComputeBlockDraft, EditorDataSources, TemplateV2Draft } from '../../api/types';
import { FieldHelp } from '../../components/FieldHelp';
import { SinksStep } from './steps/SinksStep';
import { SourcesStep } from './steps/SourcesStep';
import { TransformStep } from './steps/TransformStep';
import { TransformDagEditor } from './TransformDagEditor';
import {
  blockUsesTransformGraph,
  computeBlockToScopedDraft,
  disableTransformGraph,
  enableTransformGraph,
  scopedDraftToComputeBlock,
} from './workflowUtils';

type Props = {
  block: ComputeBlockDraft;
  readOnly: boolean;
  editorDataSources: EditorDataSources;
  onChange: (block: ComputeBlockDraft) => void;
};

/**
 * Per-compute-block editor reusing Sources, Transform, and Sinks steps.
 */
export function ComputeBlockEditor({ block, readOnly, editorDataSources, onChange }: Props) {
  const { t } = useTranslation();
  const [transformLayout, setTransformLayout] = useState<'linear' | 'dag'>(
    blockUsesTransformGraph(block) ? 'dag' : 'linear',
  );

  useEffect(() => {
    setTransformLayout(blockUsesTransformGraph(block) ? 'dag' : 'linear');
  }, [block]);

  const scoped: TemplateV2Draft = computeBlockToScopedDraft(block);

  const patchBlock = (nextBlock: ComputeBlockDraft) => {
    onChange(nextBlock);
  };

  const setLayout = (layout: 'linear' | 'dag') => {
    setTransformLayout(layout);
    patchBlock(layout === 'dag' ? enableTransformGraph(block) : disableTransformGraph(block));
  };

  const tabItems = [
    {
      key: 'sources',
      label: t('editor.tab.sources'),
      children: (
        <SourcesStep
          draft={scoped}
          readOnly={readOnly}
          editorDataSources={editorDataSources}
          onChange={(scopedDraft) => patchBlock(scopedDraftToComputeBlock(block, scopedDraft))}
        />
      ),
    },
    {
      key: 'transform',
      label: t('editor.tab.transform'),
      children: (
        <>
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
            {t('workflow.block.transformLayout')}
          </Typography.Text>
          <Radio.Group
            disabled={readOnly}
            value={transformLayout}
            onChange={(e) => setLayout(e.target.value)}
            options={[
              { value: 'linear', label: t('workflow.block.transformLinear') },
              { value: 'dag', label: t('workflow.block.transformDag') },
            ]}
            style={{ marginBottom: 16 }}
          />
          {transformLayout === 'dag' ? (
            <TransformDagEditor block={block} readOnly={readOnly} onChange={patchBlock} />
          ) : (
            <TransformStep
              draft={scoped}
              readOnly={readOnly}
              onChange={(scopedDraft) => patchBlock(scopedDraftToComputeBlock(block, scopedDraft))}
            />
          )}
        </>
      ),
    },
    {
      key: 'sinks',
      label: t('editor.tab.sinks'),
      children: (
        <SinksStep
          draft={scoped}
          readOnly={readOnly}
          editorDataSources={editorDataSources}
          onChange={(scopedDraft) => patchBlock(scopedDraftToComputeBlock(block, scopedDraft))}
        />
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 12, maxWidth: 360 }}>
        <div style={{ marginBottom: 4 }}>
          <FieldHelp label={t('workflow.block.id')} help={t('workflow.block.id.help')} />
        </div>
        <Input
          readOnly={readOnly}
          value={block.id ?? ''}
          onChange={(e) => patchBlock({ ...block, id: e.target.value })}
        />
      </div>
      <Tabs items={tabItems} />
    </div>
  );
}

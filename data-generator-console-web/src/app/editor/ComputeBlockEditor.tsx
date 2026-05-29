import { Input, Radio, Tabs, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { ComputeBlockDraft, TemplateV2Draft } from '../../api/types';
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
  jdbcNames: string[];
  onChange: (block: ComputeBlockDraft) => void;
};

/**
 * Per-compute-block editor reusing Sources, Transform, and Sinks steps.
 */
export function ComputeBlockEditor({ block, readOnly, jdbcNames, onChange }: Props) {
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
          jdbcNames={jdbcNames}
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
          jdbcNames={jdbcNames}
          onChange={(scopedDraft) => patchBlock(scopedDraftToComputeBlock(block, scopedDraft))}
        />
      ),
    },
  ];

  return (
    <div>
      <Typography.Paragraph style={{ marginBottom: 12 }}>
        <Typography.Text>{t('workflow.block.id')} </Typography.Text>
        <Input
          readOnly={readOnly}
          style={{ width: 240 }}
          value={block.id ?? ''}
          onChange={(e) => patchBlock({ ...block, id: e.target.value })}
        />
      </Typography.Paragraph>
      <Tabs items={tabItems} />
    </div>
  );
}

import { Button, Input, Select, Space, Table } from 'antd';
import { useTranslation } from 'react-i18next';
import type { ComputeBlockDraft } from '../../api/types';
import {
  addTransformGraphNode,
  applyNodeDependsOnAt,
  applyTransformGraphNodeAt,
  listTransformGraphNodes,
  readDependsOn,
  removeTransformGraphNodeAt,
} from './workflowUtils';

type Props = {
  block: ComputeBlockDraft;
  readOnly: boolean;
  onChange: (block: ComputeBlockDraft) => void;
};

/**
 * Minimal transform DAG editor: node list with depends-on multi-select.
 */
export function TransformDagEditor({ block, readOnly, onChange }: Props) {
  const { t } = useTranslation();
  const graph = block.transformGraph;
  const nodes = listTransformGraphNodes(block);
  const nodeOptions = nodes
    .filter((node) => node.id)
    .map((node) => ({ value: node.id as string, label: node.id as string }));

  const rows = nodes.map((node, index) => ({
    key: node.id ?? index,
    index,
    ...node,
    dependsOn: node.id ? readDependsOn(graph, node.id) : [],
  }));

  return (
    <>
      <Space style={{ marginBottom: 8 }}>
        <Button disabled={readOnly} onClick={() => onChange(addTransformGraphNode(block))}>
          {t('workflow.dag.addNode')}
        </Button>
      </Space>
      <Table
        size="small"
        pagination={false}
        dataSource={rows}
        locale={{ emptyText: t('workflow.dag.empty') }}
        columns={[
          {
            title: t('workflow.dag.nodeId'),
            dataIndex: 'id',
            render: (_value, row) => (
              <Input
                readOnly={readOnly}
                value={row.id ?? ''}
                onChange={(e) =>
                  onChange(applyTransformGraphNodeAt(block, row.index, { id: e.target.value }))
                }
              />
            ),
          },
          {
            title: t('workflow.dag.transformId'),
            dataIndex: 'transformId',
            render: (_value, row) => (
              <Input
                readOnly={readOnly}
                value={row.transformId ?? ''}
                onChange={(e) =>
                  onChange(applyTransformGraphNodeAt(block, row.index, { transformId: e.target.value }))
                }
              />
            ),
          },
          {
            title: t('workflow.dag.outputAlias'),
            dataIndex: 'outputAlias',
            render: (_value, row) => (
              <Input
                readOnly={readOnly}
                value={row.outputAlias ?? ''}
                onChange={(e) =>
                  onChange(applyTransformGraphNodeAt(block, row.index, { outputAlias: e.target.value }))
                }
              />
            ),
          },
          {
            title: t('workflow.dag.dependsOn'),
            dataIndex: 'dependsOn',
            render: (_value, row) => (
              <Select
                mode="multiple"
                disabled={readOnly}
                style={{ minWidth: 160 }}
                placeholder={t('workflow.dag.dependsOnPlaceholder')}
                value={row.dependsOn}
                options={nodeOptions.filter((opt) => opt.value !== row.id)}
                onChange={(values) => onChange(applyNodeDependsOnAt(block, row.index, values))}
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
                onClick={() => onChange(removeTransformGraphNodeAt(block, row.index))}
              >
                {t('common.remove')}
              </Button>
            ),
          },
        ]}
      />
    </>
  );
}

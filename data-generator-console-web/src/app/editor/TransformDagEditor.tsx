import { Alert, Button, Input, Select, Space, Table } from 'antd';
import { useTranslation } from 'react-i18next';
import type { ComputeBlockDraft } from '../../api/types';
import { FieldHelp } from '../../components/FieldHelp';
import { labeledOptions } from '../utils/optionLabels';
import { TransformJsFields } from './TransformJsFields';
import {
  addTransformGraphNode,
  applyGraphTransformTypeAt,
  applyNodeDependsOnAt,
  applyTransformGraphNodeAt,
  applyTransformScriptAt,
  applyTransformSqlAt,
  applyTransformTimeoutMsAt,
  findTransformGraphCyclePath,
  listTransformGraphNodes,
  readDependsOn,
  readGraphTransformType,
  readTransformScript,
  readTransformSql,
  readTransformTimeoutMs,
  removeTransformGraphNodeAt,
} from './workflowUtils';

const DAG_TRANSFORM_TYPES = ['sql', 'js'] as const;

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

  const cyclePath = findTransformGraphCyclePath(block);

  const rows = nodes.map((node, index) => ({
    key: node.id ?? index,
    index,
    ...node,
    dependsOn: node.id ? readDependsOn(graph, node.id) : [],
    sql: readTransformSql(block, node.transformId),
    script: readTransformScript(block, node.transformId),
    transformType: readGraphTransformType(block, node.transformId),
    timeoutMs: readTransformTimeoutMs(block, node.transformId),
  }));

  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12, maxWidth: 900 }}
        message={t('workflow.dag.intro.title')}
        description={t('workflow.dag.intro.body')}
      />
      {cyclePath.length > 0 && (
        <Alert
          type="warning"
          showIcon
          data-testid="transform-dag-cycle-alert"
          style={{ marginBottom: 12, maxWidth: 900 }}
          message={t('workflow.dag.cycle.title')}
          description={t('workflow.dag.cycle.body', { path: cyclePath.join(' → ') })}
        />
      )}
      <Space style={{ marginBottom: 8 }}>
        <Button
          data-testid="transform-dag-add-node"
          disabled={readOnly}
          onClick={() => onChange(addTransformGraphNode(block))}
        >
          {t('workflow.dag.addNode')}
        </Button>
      </Space>
      <Table
        data-testid="transform-dag-table"
        size="small"
        pagination={false}
        dataSource={rows}
        locale={{ emptyText: t('workflow.dag.empty') }}
        columns={[
          {
            title: <FieldHelp label={t('workflow.dag.nodeId')} help={t('workflow.dag.nodeId.help')} />,
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
            title: (
              <FieldHelp label={t('workflow.dag.transformId')} help={t('workflow.dag.transformId.help')} />
            ),
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
            title: (
              <FieldHelp label={t('workflow.dag.outputAlias')} help={t('workflow.dag.outputAlias.help')} />
            ),
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
            title: <FieldHelp label={t('workflow.dag.dependsOn')} help={t('workflow.dag.dependsOn.help')} />,
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
            title: <FieldHelp label={t('transform.type')} help={t('transform.type.help')} />,
            dataIndex: 'transformType',
            width: 140,
            render: (_value, row) => {
              const transformId = row.transformId?.trim();
              return (
                <Select
                  disabled={readOnly || !transformId}
                  style={{ minWidth: 120 }}
                  value={row.transformType}
                  options={labeledOptions(t, 'transform.type', DAG_TRANSFORM_TYPES)}
                  onChange={(type) => {
                    if (transformId) {
                      onChange(applyGraphTransformTypeAt(block, transformId, type));
                    }
                  }}
                />
              );
            },
          },
          {
            title: <FieldHelp label={t('workflow.dag.body')} help={t('workflow.dag.body.help')} />,
            dataIndex: 'sql',
            render: (_value, row) => {
              const transformId = row.transformId?.trim();
              if (row.transformType === 'js') {
                return (
                  <TransformJsFields
                    script={row.script ?? ''}
                    timeoutMs={row.timeoutMs}
                    readOnly={readOnly || !transformId}
                    onScriptChange={(script) => {
                      if (transformId) {
                        onChange(applyTransformScriptAt(block, transformId, script));
                      }
                    }}
                    onTimeoutChange={(timeoutMs) => {
                      if (transformId) {
                        onChange(applyTransformTimeoutMsAt(block, transformId, timeoutMs));
                      }
                    }}
                  />
                );
              }
              return (
                <Input.TextArea
                  readOnly={readOnly}
                  rows={2}
                  value={row.sql}
                  placeholder={t('workflow.dag.sqlPlaceholder')}
                  onChange={(e) => {
                    if (transformId) {
                      onChange(applyTransformSqlAt(block, transformId, e.target.value));
                    }
                  }}
                />
              );
            },
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

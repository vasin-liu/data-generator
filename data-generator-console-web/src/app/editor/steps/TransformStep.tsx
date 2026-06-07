import { Button, Form, Input, Radio, Select, Space, Table, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import type { SpelColumnDraft, TemplateV2Draft, TransformDraft } from '../../../api/types';
import { FieldHelp } from '../../../components/FieldHelp';
import { labeledOptions } from '../../utils/optionLabels';
import {
  addTransformer,
  applySpelColumns,
  applyTransformJs,
  applyTransformSql,
  applyTransformType,
  applyTransformerAt,
  inferTransformType,
  listTransformers,
  readSpelColumns,
  readTransformType,
  removeTransformerAt,
  switchToChainTransform,
  switchToSingleTransform,
  type TransformKind,
  usesTransformerChain,
} from '../draftUtils';
import { TransformJsFields } from '../TransformJsFields';

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  onChange: (draft: TemplateV2Draft) => void;
};

const TRANSFORM_TYPES = ['sql', 'spel', 'js'] as const;

function SpelColumnTable({
  columns,
  readOnly,
  onUpdate,
}: {
  columns: SpelColumnDraft[];
  readOnly: boolean;
  onUpdate: (cols: SpelColumnDraft[]) => void;
}) {
  const { t } = useTranslation();
  const columnRows = columns.map((col, index) => ({ key: index, index, ...col }));

  return (
    <>
      <Space style={{ marginBottom: 8 }}>
        <Button
          disabled={readOnly}
          onClick={() =>
            onUpdate([...columns, { name: `col${columns.length + 1}`, expression: '#input' }])
          }
        >
          {t('transform.spel.addColumn')}
        </Button>
      </Space>
      <Table
        size="small"
        pagination={false}
        dataSource={columnRows}
        locale={{ emptyText: t('transform.spel.empty') }}
        columns={[
          {
            title: t('transform.spel.column'),
            dataIndex: 'name',
            render: (_value, row) => (
              <Input
                readOnly={readOnly}
                value={row.name ?? ''}
                onChange={(e) => {
                  const next = [...columns];
                  next[row.index] = { ...next[row.index], name: e.target.value };
                  onUpdate(next);
                }}
              />
            ),
          },
          {
            title: t('transform.spel.expr'),
            dataIndex: 'expression',
            render: (_value, row) => (
              <Input
                readOnly={readOnly}
                value={row.expression ?? ''}
                onChange={(e) => {
                  const next = [...columns];
                  next[row.index] = { ...next[row.index], expression: e.target.value };
                  onUpdate(next);
                }}
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
                onClick={() => onUpdate(columns.filter((_, i) => i !== row.index))}
              >
                {t('transform.spel.removeColumn')}
              </Button>
            ),
          },
        ]}
      />
    </>
  );
}

/**
 * SQL or SpEL transform (single step or transformers[] chain).
 */
export function TransformStep({ draft, readOnly, onChange }: Props) {
  const { t } = useTranslation();
  const chainMode = usesTransformerChain(draft);
  const transformers = listTransformers(draft);
  const singleTransform = chainMode ? undefined : draft.transform;
  const transformType = readTransformType(draft);
  const columns = readSpelColumns(draft);

  const setMode = (chain: boolean) => {
    onChange(chain ? switchToChainTransform(draft) : switchToSingleTransform(draft));
  };

  const setType = (type: TransformKind) => {
    onChange(
      applyTransformType(
        draft,
        type,
        (chainMode ? transformers[0]?.sql : singleTransform?.sql) ?? 'SELECT * FROM input',
        columns.length > 0 ? columns : [{ name: 'value', expression: '#input' }],
        typeof singleTransform?.script === 'string' ? singleTransform.script : 'row.value = row.value',
      ),
    );
  };

  const patchTransformerType = (index: number, node: TransformDraft, type: TransformKind) => {
    if (type === 'sql') {
      onChange(
        applyTransformerAt(draft, index, {
          type: 'sql',
          sql: node.sql ?? 'SELECT * FROM input',
        }),
      );
      return;
    }
    if (type === 'js') {
      onChange(
        applyTransformerAt(draft, index, {
          type: 'js',
          script: typeof node.script === 'string' ? node.script : 'row.value = row.value',
          timeoutMs: typeof node.timeoutMs === 'number' ? node.timeoutMs : undefined,
        }),
      );
      return;
    }
    onChange(
      applyTransformerAt(draft, index, {
        type: 'spel',
        columns:
          node.columns && node.columns.length > 0
            ? node.columns
            : [{ name: 'value', expression: '#input' }],
      }),
    );
  };

  const renderTransformerFields = (node: TransformDraft, index: number) => {
    const nodeType = inferTransformType(node);
    const nodeColumns = node.columns ? [...node.columns] : [];

    return (
      <div
        key={index}
        data-testid="transform-chain-step"
        style={{ marginBottom: 24, paddingBottom: 16, borderBottom: '1px solid #f0f0f0' }}
      >
        <Typography.Text strong>
          {t('transform.chain.step', { index: index + 1 })}
        </Typography.Text>
        <Form layout="vertical" style={{ marginTop: 8 }}>
          <Form.Item
            label={<FieldHelp label={t('transform.chain.name')} help={t('transform.chain.name.help')} />}
          >
            <Input
              readOnly={readOnly}
              value={node.name ?? ''}
              onChange={(e) => onChange(applyTransformerAt(draft, index, { name: e.target.value }))}
            />
          </Form.Item>
          <Form.Item
            label={<FieldHelp label={t('transform.type')} help={t('transform.type.help')} />}
          >
            <Select
              disabled={readOnly}
              value={nodeType}
              data-testid={`transform-type-select-${index}`}
              options={labeledOptions(t, 'transform.type', TRANSFORM_TYPES)}
              onChange={(type) => patchTransformerType(index, node, type as TransformKind)}
            />
          </Form.Item>
          {nodeType === 'sql' ? (
            <Form.Item label={<FieldHelp label={t('transform.sql')} help={t('transform.sql.help')} />}>
              <Input.TextArea
                rows={6}
                readOnly={readOnly}
                value={node.sql ?? ''}
                onChange={(e) =>
                  onChange(applyTransformerAt(draft, index, { type: 'sql', sql: e.target.value }))
                }
              />
            </Form.Item>
          ) : nodeType === 'js' ? (
            <TransformJsFields
              script={typeof node.script === 'string' ? node.script : ''}
              timeoutMs={typeof node.timeoutMs === 'number' ? node.timeoutMs : undefined}
              readOnly={readOnly}
              onScriptChange={(script) =>
                onChange(applyTransformerAt(draft, index, { type: 'js', script }))
              }
              onTimeoutChange={(timeoutMs) =>
                onChange(applyTransformerAt(draft, index, { type: 'js', timeoutMs }))
              }
            />
          ) : (
            <SpelColumnTable
              columns={nodeColumns}
              readOnly={readOnly}
              onUpdate={(cols) =>
                onChange(applyTransformerAt(draft, index, { type: 'spel', columns: cols }))
              }
            />
          )}
        </Form>
        {chainMode && transformers.length > 1 ? (
          <Button
            danger
            disabled={readOnly}
            onClick={() => onChange(removeTransformerAt(draft, index))}
          >
            {t('transform.chain.remove')}
          </Button>
        ) : null}
      </div>
    );
  };

  return (
    <Form layout="vertical" style={{ maxWidth: 720 }}>
      <Form.Item
        label={<FieldHelp label={t('transform.chain.mode')} help={t('transform.chain.mode.help')} />}
      >
        <Radio.Group
          disabled={readOnly}
          value={chainMode ? 'chain' : 'single'}
          onChange={(e) => setMode(e.target.value === 'chain')}
          options={[
            { value: 'single', label: t('transform.chain.single') },
            { value: 'chain', label: t('transform.chain.multi') },
          ]}
        />
      </Form.Item>

      {chainMode ? (
        <>
          {transformers.map((node, index) => renderTransformerFields(node, index))}
          <Button
            disabled={readOnly}
            data-testid="transform-add-step"
            onClick={() => onChange(addTransformer(draft, 'sql'))}
          >
            {t('transform.chain.add')}
          </Button>
        </>
      ) : (
        <>
          <Form.Item
            label={<FieldHelp label={t('transform.type')} help={t('transform.type.help')} />}
          >
            <Select
              disabled={readOnly}
              value={transformType}
              options={labeledOptions(t, 'transform.type', TRANSFORM_TYPES)}
              onChange={setType}
            />
          </Form.Item>
          {transformType === 'sql' ? (
            <Form.Item label={<FieldHelp label={t('transform.sql')} help={t('transform.sql.help')} />}>
              <Input.TextArea
                rows={8}
                readOnly={readOnly}
                value={singleTransform?.sql ?? ''}
                onChange={(e) => onChange(applyTransformSql(draft, e.target.value))}
              />
            </Form.Item>
          ) : transformType === 'js' ? (
            <TransformJsFields
              script={typeof singleTransform?.script === 'string' ? singleTransform.script : ''}
              timeoutMs={typeof singleTransform?.timeoutMs === 'number' ? singleTransform.timeoutMs : undefined}
              readOnly={readOnly}
              onScriptChange={(script) =>
                onChange(
                  applyTransformJs(
                    draft,
                    script,
                    typeof singleTransform?.timeoutMs === 'number' ? singleTransform.timeoutMs : undefined,
                  ),
                )
              }
              onTimeoutChange={(timeoutMs) =>
                onChange(
                  applyTransformJs(
                    draft,
                    typeof singleTransform?.script === 'string' ? singleTransform.script : '',
                    timeoutMs,
                  ),
                )
              }
            />
          ) : (
            <SpelColumnTable
              columns={columns}
              readOnly={readOnly}
              onUpdate={(cols) => onChange(applySpelColumns(draft, cols))}
            />
          )}
        </>
      )}
    </Form>
  );
}

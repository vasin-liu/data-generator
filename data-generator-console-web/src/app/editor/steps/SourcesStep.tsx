import { Alert, Button, Card, Collapse, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Typography } from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type {
  AiCatalog,
  EditorDataSources,
  MaterializationMode,
  MaterializationPolicyDraft,
  TemplateV2Draft,
} from '../../../api/types';
import { SourceFieldsForm } from '../SourceFieldsForm';
import { FieldHelp } from '../../../components/FieldHelp';
import { labeledOptions } from '../../utils/optionLabels';
import {
  EDITABLE_SOURCE_KINDS,
  addSource,
  applySourceMaterializationPolicyAt,
  applySourceMergeAt,
  defaultMaterializationPolicy,
  inferSourceKind,
  listSourceKeys,
  removeSourceAt,
  renameSourceKey,
  setSourceKindAt,
  suggestSourceKey,
  type EditableSourceKind,
} from '../draftUtils';

const MATERIALIZATION_MODES = ['ORDERED', 'LIMIT', 'ONCE', 'EQUAL', 'WEIGHTED'] as const;

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  editorDataSources: EditorDataSources;
  aiCatalog?: AiCatalog;
  onChange: (draft: TemplateV2Draft) => void;
};

/**
 * Named template input sources (query / files / iterators — not JDBC admin).
 */
export function SourcesStep({ draft, readOnly, editorDataSources, aiCatalog, onChange }: Props) {
  const { t } = useTranslation();
  const keys = listSourceKeys(draft);
  const [selectedKey, setSelectedKey] = useState(keys[0] ?? '');
  const [addOpen, setAddOpen] = useState(false);
  const [addKey, setAddKey] = useState('');
  const [addKind, setAddKind] = useState<EditableSourceKind>('iterator');
  const [renameOpen, setRenameOpen] = useState(false);
  const [renameValue, setRenameValue] = useState('');

  useEffect(() => {
    const nextKeys = listSourceKeys(draft);
    if (nextKeys.length === 0) {
      setSelectedKey('');
      return;
    }
    if (!nextKeys.includes(selectedKey)) {
      setSelectedKey(nextKeys[0]);
    }
  }, [draft, selectedKey]);

  const source = selectedKey ? draft.sources?.[selectedKey] : undefined;
  const sourceKind = inferSourceKind(source);
  const materializationPolicy = {
    ...defaultMaterializationPolicy(),
    ...source?.materializationPolicy,
  };
  const materializationMode = materializationPolicy.mode?.toUpperCase() as
    | MaterializationMode
    | undefined;

  const patchMaterializationPolicy = (partial: MaterializationPolicyDraft) => {
    if (!selectedKey) {
      return;
    }
    onChange(applySourceMaterializationPolicyAt(draft, selectedKey, partial));
  };

  const formatWeights = (weights?: number[]): string => (weights?.length ? weights.join(', ') : '');

  const parseWeights = (text: string): number[] | undefined => {
    const trimmed = text.trim();
    if (!trimmed) {
      return undefined;
    }
    const parts = trimmed.split(/[,;\s]+/).filter(Boolean);
    const nums = parts.map((part) => Number.parseInt(part, 10));
    if (nums.some((n) => Number.isNaN(n))) {
      return undefined;
    }
    return nums;
  };

  const openAdd = () => {
    setAddKey(suggestSourceKey(draft));
    setAddKind('iterator');
    setAddOpen(true);
  };

  const submitAdd = () => {
    const key = addKey.trim();
    if (!key) {
      return;
    }
    if (draft.sources?.[key]) {
      return;
    }
    onChange(addSource(draft, key, addKind));
    setSelectedKey(key);
    setAddOpen(false);
  };

  const handleRemove = (key: string) => {
    const next = removeSourceAt(draft, key);
    onChange(next);
    const remaining = listSourceKeys(next);
    setSelectedKey(remaining[0] ?? '');
  };

  const openRename = () => {
    setRenameValue(selectedKey);
    setRenameOpen(true);
  };

  const submitRename = () => {
    if (!selectedKey) {
      return;
    }
    const next = renameSourceKey(draft, selectedKey, renameValue);
    if (next !== draft) {
      onChange(next);
      setSelectedKey(renameValue.trim());
    }
    setRenameOpen(false);
  };

  const kindOptions = labeledOptions(t, 'source.kind', EDITABLE_SOURCE_KINDS);

  return (
    <div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16, maxWidth: 900 }}
        message={t('source.page.intro.title')}
        description={
          <span>
            {t('source.page.intro.body')}{' '}
            <Link to="/datasources">{t('nav.datasources')}</Link>
          </span>
        }
      />

      <Space wrap style={{ marginBottom: 16 }}>
        <Button disabled={readOnly} type="primary" icon={<PlusOutlined />} onClick={openAdd}>
          {t('source.add')}
        </Button>
        {selectedKey ? (
          <Button disabled={readOnly} onClick={openRename}>
            {t('source.rename')}
          </Button>
        ) : null}
      </Space>

      {keys.length === 0 ? (
        <Alert type="info" message={t('source.empty')} />
      ) : (
        <Space direction="vertical" size="middle" style={{ width: '100%', maxWidth: 900 }}>
          {keys.map((key) => {
            const node = draft.sources?.[key];
            const kind = inferSourceKind(node);
            const active = key === selectedKey;
            return (
              <Card
                key={key}
                size="small"
                style={{
                  borderColor: active ? '#1677ff' : undefined,
                  cursor: 'pointer',
                }}
                onClick={() => setSelectedKey(key)}
                title={
                  <Space>
                    <Typography.Text strong>{key}</Typography.Text>
                    <Typography.Text type="secondary">
                      ({t(`source.kind.${kind === 'other' ? 'other' : kind}`)})
                    </Typography.Text>
                  </Space>
                }
                extra={
                  readOnly ? null : (
                    <Popconfirm
                      title={t('source.remove')}
                      description={t('source.removeConfirm', { key })}
                      onConfirm={(e) => {
                        e?.stopPropagation();
                        handleRemove(key);
                      }}
                      onCancel={(e) => e?.stopPropagation()}
                    >
                      <Button
                        type="text"
                        danger
                        size="small"
                        icon={<DeleteOutlined />}
                        onClick={(e) => e.stopPropagation()}
                      />
                    </Popconfirm>
                  )
                }
              >
                {active ? (
                  kind === 'other' ? (
                    <Alert
                      type="warning"
                      message={t('source.unsupportedType', { type: node?.type ?? 'unknown' })}
                    />
                  ) : (
                    <Form layout="vertical" onClick={(e) => e.stopPropagation()}>
                      <Form.Item
                        label={
                          <FieldHelp label={t('source.type')} help={t('source.type.help')} required />
                        }
                      >
                        <Select
                          disabled={readOnly}
                          value={sourceKind === 'other' ? undefined : sourceKind}
                          options={kindOptions}
                          onChange={(v) => onChange(setSourceKindAt(draft, key, v as EditableSourceKind))}
                        />
                      </Form.Item>
                      <SourceFieldsForm
                        kind={sourceKind === 'other' ? 'iterator' : sourceKind}
                        source={node ?? {}}
                        readOnly={readOnly}
                        editorDataSources={editorDataSources}
                        aiCatalog={aiCatalog}
                        onPatch={(patch) => onChange(applySourceMergeAt(draft, key, patch))}
                      />
                    </Form>
                  )
                ) : (
                  <Typography.Text type="secondary">{t('source.card.selectHint')}</Typography.Text>
                )}
              </Card>
            );
          })}
        </Space>
      )}

      {selectedKey && source && sourceKind !== 'other' ? (
        <Collapse
          style={{ marginTop: 16, maxWidth: 760 }}
          items={[
            {
              key: 'materialization',
              label: t('source.materialization.title'),
              children: (
                <Form layout="vertical">
                  <Alert
                    type="info"
                    showIcon
                    style={{ marginBottom: 12 }}
                    message={t('source.materialization.intro')}
                  />
                  <Form.Item
                    label={
                      <FieldHelp
                        label={t('source.materialization.mode')}
                        help={t('source.materialization.mode.help')}
                      />
                    }
                  >
                    <Select
                      allowClear
                      disabled={readOnly}
                      placeholder={t('source.materialization.mode.placeholder')}
                      value={materializationMode}
                      options={labeledOptions(t, 'source.materialization.mode', MATERIALIZATION_MODES)}
                      onChange={(v) => {
                        const nextMode = (v ?? undefined) as MaterializationMode | undefined;
                        if (!nextMode) {
                          patchMaterializationPolicy({
                            mode: undefined,
                            limit: undefined,
                            seed: undefined,
                            weights: undefined,
                          });
                          return;
                        }
                        const patch: MaterializationPolicyDraft = { mode: nextMode };
                        if (nextMode !== 'LIMIT' && nextMode !== 'ORDERED') {
                          patch.limit = undefined;
                        }
                        if (nextMode !== 'EQUAL' && nextMode !== 'WEIGHTED') {
                          patch.seed = undefined;
                        }
                        if (nextMode !== 'WEIGHTED') {
                          patch.weights = undefined;
                        }
                        patchMaterializationPolicy(patch);
                      }}
                    />
                  </Form.Item>
                  {materializationMode === 'LIMIT' || materializationMode === 'ORDERED' ? (
                    <Form.Item
                      label={
                        <FieldHelp
                          label={t('source.materialization.limit')}
                          help={t('source.materialization.limit.help')}
                        />
                      }
                    >
                      <InputNumber
                        min={materializationMode === 'LIMIT' ? 1 : 0}
                        disabled={readOnly}
                        style={{ width: '100%' }}
                        placeholder={t('source.materialization.limit.placeholder')}
                        value={materializationPolicy.limit}
                        onChange={(v) => patchMaterializationPolicy({ limit: v ?? undefined })}
                      />
                    </Form.Item>
                  ) : null}
                  {materializationMode === 'EQUAL' || materializationMode === 'WEIGHTED' ? (
                    <Form.Item
                      label={
                        <FieldHelp
                          label={t('source.materialization.seed')}
                          help={t('source.materialization.seed.help')}
                        />
                      }
                    >
                      <InputNumber
                        disabled={readOnly}
                        style={{ width: '100%' }}
                        placeholder={t('source.materialization.seed.placeholder')}
                        value={materializationPolicy.seed}
                        onChange={(v) => patchMaterializationPolicy({ seed: v ?? undefined })}
                      />
                    </Form.Item>
                  ) : null}
                  {materializationMode === 'WEIGHTED' ? (
                    <Form.Item
                      label={
                        <FieldHelp
                          label={t('source.materialization.weights')}
                          help={t('source.materialization.weights.help')}
                        />
                      }
                    >
                      <Input
                        readOnly={readOnly}
                        placeholder={t('source.materialization.weights.placeholder')}
                        value={formatWeights(materializationPolicy.weights)}
                        onChange={(e) =>
                          patchMaterializationPolicy({
                            weights: parseWeights(e.target.value),
                          })
                        }
                      />
                    </Form.Item>
                  ) : null}
                </Form>
              ),
            },
          ]}
        />
      ) : null}

      <Modal
        title={t('source.addModal.title')}
        open={addOpen}
        onOk={submitAdd}
        onCancel={() => setAddOpen(false)}
        okButtonProps={{ disabled: !addKey.trim() || Boolean(draft.sources?.[addKey.trim()]) }}
      >
        <Form layout="vertical">
          <Form.Item
            label={<FieldHelp label={t('source.key')} help={t('source.key.help')} required />}
          >
            <Input value={addKey} onChange={(e) => setAddKey(e.target.value)} />
          </Form.Item>
          <Form.Item
            label={<FieldHelp label={t('source.type')} help={t('source.addModal.kindHelp')} required />}
          >
            <Select value={addKind} options={kindOptions} onChange={setAddKind} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={t('source.rename')}
        open={renameOpen}
        onOk={submitRename}
        onCancel={() => setRenameOpen(false)}
      >
        <Input
          value={renameValue}
          onChange={(e) => setRenameValue(e.target.value)}
          placeholder={t('source.key')}
        />
      </Modal>
    </div>
  );
}

import { Alert, Button, Card, Collapse, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Typography } from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { EditorDataSources, SourceDraft, TemplateV2Draft } from '../../../api/types';
import { SourceFieldsForm } from '../SourceFieldsForm';
import { FieldHelp } from '../../../components/FieldHelp';
import { labeledOptions, yesNoOptions } from '../../utils/optionLabels';
import {
  EDITABLE_SOURCE_KINDS,
  addSource,
  applySourceMergeAt,
  applySourcePolicyAt,
  defaultSourcePolicy,
  inferSourceKind,
  listSourceKeys,
  removeSourceAt,
  renameSourceKey,
  setSourceKindAt,
  suggestSourceKey,
  type EditableSourceKind,
} from '../draftUtils';

const SELECTION_STRATEGIES = [
  'ORDER',
  'FIRST',
  'RANDOM',
  'REPEAT_ORDER',
  'ONCE_ORDER',
  'MULTIPLE_ORDER',
  'REPEAT_RANDOM',
  'ONCE_RANDOM',
] as const;

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  editorDataSources: EditorDataSources;
  onChange: (draft: TemplateV2Draft) => void;
};

/**
 * Named template input sources (query / files / iterators — not JDBC admin).
 */
export function SourcesStep({ draft, readOnly, editorDataSources, onChange }: Props) {
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
  const policy = { ...defaultSourcePolicy(), ...source?.policy };

  const patchPolicy = (partial: SourceDraft['policy']) => {
    if (!selectedKey) {
      return;
    }
    onChange(applySourcePolicyAt(draft, selectedKey, { ...policy, ...partial }));
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
              key: 'policy',
              label: t('source.policy.title'),
              children: (
                <Form layout="vertical">
                  <Form.Item
                    label={
                      <FieldHelp label={t('source.policy.inMemory')} help={t('source.policy.inMemory.help')} />
                    }
                  >
                    <Select
                      disabled={readOnly}
                      value={policy.inMemory ?? false}
                      options={yesNoOptions(t)}
                      onChange={(v) => patchPolicy({ inMemory: v })}
                    />
                  </Form.Item>
                  <Form.Item
                    label={
                      <FieldHelp
                        label={t('source.policy.selectionStrategy')}
                        help={t('source.policy.selectionStrategy.help')}
                      />
                    }
                  >
                    <Select
                      disabled={readOnly}
                      value={policy.selectionStrategy ?? 'ORDER'}
                      options={labeledOptions(t, 'source.selectionStrategy', SELECTION_STRATEGIES)}
                      onChange={(v) => patchPolicy({ selectionStrategy: v })}
                    />
                  </Form.Item>
                  <Form.Item
                    label={<FieldHelp label={t('source.policy.limit')} help={t('source.policy.limit.help')} />}
                  >
                    <InputNumber
                      min={0}
                      disabled={readOnly}
                      style={{ width: '100%' }}
                      placeholder={t('source.policy.limit.placeholder')}
                      value={policy.limit}
                      onChange={(v) => patchPolicy({ limit: v ?? undefined })}
                    />
                  </Form.Item>
                  <Form.Item
                    label={
                      <FieldHelp
                        label={t('source.policy.materialization')}
                        help={t('source.policy.materialization.help')}
                      />
                    }
                  >
                    <Input
                      readOnly={readOnly}
                      placeholder={t('source.policy.materialization.placeholder')}
                      value={policy.materialization ?? ''}
                      onChange={(e) =>
                        patchPolicy({ materialization: e.target.value || undefined })
                      }
                    />
                  </Form.Item>
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

import { Alert, Button, Card, Collapse, Form, Input, InputNumber, Modal, Select, Space } from 'antd';
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
  const policy = source?.policy ?? {};

  const patchPolicy = (partial: SourceDraft['policy']) => {
    if (!selectedKey) {
      return;
    }
    onChange(applySourcePolicyAt(draft, selectedKey, partial ?? {}));
  };

  const patchSource = (patch: SourceDraft) => {
    if (!selectedKey) {
      return;
    }
    onChange(applySourceMergeAt(draft, selectedKey, patch));
  };

  const handleAdd = () => {
    const key = suggestSourceKey(draft);
    onChange(addSource(draft, key, 'iterator'));
    setSelectedKey(key);
  };

  const handleRemove = () => {
    if (!selectedKey) {
      return;
    }
    Modal.confirm({
      title: t('source.remove'),
      content: t('source.removeConfirm', { key: selectedKey }),
      onOk: () => onChange(removeSourceAt(draft, selectedKey)),
    });
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
        <Select
          style={{ minWidth: 200 }}
          disabled={keys.length === 0}
          value={selectedKey || undefined}
          options={keys.map((k) => ({ value: k, label: k }))}
          onChange={setSelectedKey}
        />
        <Button disabled={readOnly} type="primary" onClick={handleAdd}>
          {t('source.add')}
        </Button>
        <Button disabled={readOnly || !selectedKey} onClick={openRename}>
          {t('source.rename')}
        </Button>
        <Button disabled={readOnly || !selectedKey} danger onClick={handleRemove}>
          {t('source.remove')}
        </Button>
      </Space>

      {!selectedKey ? (
        <Alert type="info" message={t('source.empty')} />
      ) : sourceKind === 'other' ? (
        <Alert
          type="warning"
          message={t('source.unsupportedType', { type: source?.type ?? 'unknown' })}
        />
      ) : (
        <Card title={t('source.card.title', { key: selectedKey })} style={{ maxWidth: 760 }}>
          <Form layout="vertical">
            <Form.Item
              label={<FieldHelp label={t('source.type')} help={t('source.type.help')} required />}
            >
              <Select
                disabled={readOnly}
                value={sourceKind}
                options={kindOptions}
                onChange={(v) => onChange(setSourceKindAt(draft, selectedKey, v as EditableSourceKind))}
              />
            </Form.Item>
            <SourceFieldsForm
              kind={sourceKind}
              source={source ?? {}}
              readOnly={readOnly}
              editorDataSources={editorDataSources}
              onPatch={patchSource}
            />
          </Form>
        </Card>
      )}

      {selectedKey && source ? (
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
                      allowClear
                      placeholder={t('source.policy.selectionStrategy.default')}
                      value={policy.selectionStrategy}
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

import { Alert, Button, Card, Collapse, Form, Input, Modal, Popconfirm, Select, Space, Typography, message } from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { EditorDataSources, TemplateV2Draft, WriterDraft } from '../../../api/types';
import { FieldHelp } from '../../../components/FieldHelp';
import { labeledOptions } from '../../utils/optionLabels';
import {
  addWriter,
  applyWriterAt,
  isEditableWriter,
  listWriters,
  readWriterAt,
  removeWriterAt,
} from '../draftUtils';

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  editorDataSources: EditorDataSources;
  onChange: (draft: TemplateV2Draft) => void;
};

const WRITER_TYPES = ['console', 'jdbc', 'kafka', 'elasticsearch'] as const;

function defaultWriterForType(type: (typeof WRITER_TYPES)[number]): WriterDraft {
  switch (type) {
    case 'jdbc':
      return { type: 'jdbc', dataSourceId: '', target: '' };
    case 'kafka':
      return { type: 'kafka', dataSourceId: '', target: '' };
    case 'elasticsearch':
      return { type: 'elasticsearch', dataSourceId: '', target: '' };
    default:
      return { type: 'console' };
  }
}

/**
 * Output writers (sinks) — distinct from template input sources.
 */
export function SinksStep({ draft, readOnly, editorDataSources, onChange }: Props) {
  const { t } = useTranslation();
  const writers = listWriters(draft);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [optionsText, setOptionsText] = useState('');
  const [addOpen, setAddOpen] = useState(false);
  const [addType, setAddType] = useState<(typeof WRITER_TYPES)[number]>('console');

  useEffect(() => {
    if (writers.length === 0) {
      setSelectedIndex(0);
      return;
    }
    if (selectedIndex >= writers.length) {
      setSelectedIndex(writers.length - 1);
    }
  }, [writers.length, selectedIndex]);

  const writer = readWriterAt(draft, selectedIndex);

  useEffect(() => {
    const opts = writer?.options;
    setOptionsText(opts && Object.keys(opts).length > 0 ? JSON.stringify(opts, null, 2) : '');
  }, [writer?.options, selectedIndex]);

  const patch = (partial: Parameters<typeof applyWriterAt>[2]) => {
    onChange(applyWriterAt(draft, selectedIndex, partial));
  };

  const openAdd = () => {
    setAddType('console');
    setAddOpen(true);
  };

  const submitAdd = () => {
    const next = addWriter(draft, addType);
    const idx = listWriters(next).length - 1;
    onChange(applyWriterAt(next, idx, defaultWriterForType(addType)));
    setSelectedIndex(idx);
    setAddOpen(false);
  };

  const handleRemove = (index: number) => {
    const next = removeWriterAt(draft, index);
    onChange(next);
    const len = listWriters(next).length;
    setSelectedIndex(len === 0 ? 0 : Math.min(index, len - 1));
  };

  const applyOptionsJson = () => {
    if (!optionsText.trim()) {
      patch({ options: {} });
      return;
    }
    try {
      const parsed = JSON.parse(optionsText) as Record<string, unknown>;
      patch({ options: parsed });
    } catch {
      message.error(t('sink.options.invalid'));
    }
  };

  const writerTypeOptions = labeledOptions(t, 'sink.writerType', WRITER_TYPES);

  const renderWriterForm = (index: number) => {
    const w = readWriterAt(draft, index);
    const wType = (w?.type?.toLowerCase() ?? 'console') as (typeof WRITER_TYPES)[number];
    const wEditable = isEditableWriter(w);
    const wNeedsTarget = wType !== 'console';
    const wNeedsCluster = wType === 'jdbc' || wType === 'kafka' || wType === 'elasticsearch';
    const wShowsTemplate = wType === 'kafka' || wType === 'elasticsearch' || wType === 'jdbc';
    const wClusterOptions =
      wType === 'jdbc'
        ? editorDataSources.jdbcNames.map((n) => ({ value: n, label: n }))
        : wType === 'kafka'
          ? editorDataSources.kafkaClusters.map((n) => ({ value: n, label: n }))
          : editorDataSources.elasticsearchClusters.map((n) => ({ value: n, label: n }));

    if (!wEditable) {
      return <Alert type="warning" message={t('sink.unsupportedType', { type: w?.type ?? 'unknown' })} />;
    }

    const patchAt = (partial: Parameters<typeof applyWriterAt>[2]) => {
      onChange(applyWriterAt(draft, index, partial));
    };

    return (
      <Form layout="vertical" onClick={(e) => e.stopPropagation()}>
        <Form.Item
          label={<FieldHelp label={t('sink.writerType')} help={t('sink.writerType.help')} required />}
        >
          <Select
            disabled={readOnly}
            value={WRITER_TYPES.includes(wType) ? wType : 'console'}
            options={writerTypeOptions}
            onChange={(v) =>
              patchAt({
                ...defaultWriterForType(v as (typeof WRITER_TYPES)[number]),
              })
            }
          />
        </Form.Item>
        {wNeedsCluster && (
          <Form.Item
            label={
              <FieldHelp
                label={
                  wType === 'jdbc'
                    ? t('sink.jdbcDatasource')
                    : wType === 'kafka'
                      ? t('sink.kafkaCluster')
                      : t('sink.esCluster')
                }
                help={
                  wType === 'jdbc'
                    ? t('sink.jdbcDatasource.help')
                    : wType === 'kafka'
                      ? t('sink.kafkaCluster.help')
                      : t('sink.esCluster.help')
                }
                required
              />
            }
            extra={
              wType !== 'jdbc' && wClusterOptions.length === 0 ? (
                <Link to="/datasources">{t('sink.configureClusters')}</Link>
              ) : undefined
            }
          >
            <Select
              disabled={readOnly}
              showSearch
              placeholder={t('sink.cluster.placeholder')}
              value={w?.dataSourceId}
              options={wClusterOptions}
              onChange={(v) =>
                patchAt({
                  type: wType,
                  dataSourceId: v,
                  target: w?.target,
                  template: w?.template,
                })
              }
            />
          </Form.Item>
        )}
        {wNeedsTarget && (
          <Form.Item
            label={
              <FieldHelp
                label={
                  wType === 'kafka'
                    ? t('sink.target.topic')
                    : wType === 'elasticsearch'
                      ? t('sink.target.index')
                      : t('sink.target.table')
                }
                help={t('sink.target.help')}
                required
              />
            }
          >
            <Input
              readOnly={readOnly}
              value={w?.target ?? ''}
              onChange={(e) =>
                patchAt({
                  type: wType,
                  dataSourceId: w?.dataSourceId,
                  target: e.target.value,
                  template: w?.template,
                })
              }
            />
          </Form.Item>
        )}
        {wShowsTemplate && (
          <Form.Item label={<FieldHelp label={t('sink.template')} help={t('sink.template.help')} />}>
            <Input.TextArea
              rows={6}
              readOnly={readOnly}
              value={w?.template ?? ''}
              onChange={(e) =>
                patchAt({
                  type: wType,
                  dataSourceId: w?.dataSourceId,
                  target: w?.target,
                  template: e.target.value,
                })
              }
            />
          </Form.Item>
        )}
        {index === selectedIndex && (
          <>
            {wType === 'jdbc' ? (
              <Alert
                type="info"
                showIcon
                style={{ marginBottom: 12 }}
                message={t('sink.jdbcUpsert.hint.title')}
                description={t('sink.jdbcUpsert.hint.body')}
              />
            ) : null}
            <Collapse
            items={[
              {
                key: 'options',
                label: t('sink.options.title'),
                children: (
                  <>
                    <Input.TextArea
                      rows={5}
                      readOnly={readOnly}
                      value={optionsText}
                      onChange={(e) => setOptionsText(e.target.value)}
                      onBlur={readOnly ? undefined : applyOptionsJson}
                      placeholder='{"key": "value"}'
                    />
                    {!readOnly ? (
                      <Button style={{ marginTop: 8 }} onClick={applyOptionsJson}>
                        {t('sink.options.apply')}
                      </Button>
                    ) : null}
                  </>
                ),
              },
            ]}
          />
          </>
        )}
      </Form>
    );
  };

  return (
    <div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16, maxWidth: 900 }}
        message={t('sink.page.intro.title')}
        description={
          <span>
            {t('sink.page.intro.body')}{' '}
            <Link to="/datasources">{t('nav.datasources')}</Link>
          </span>
        }
      />

      <Space wrap style={{ marginBottom: 16 }}>
        <Button disabled={readOnly} type="primary" icon={<PlusOutlined />} onClick={openAdd}>
          {t('sink.add')}
        </Button>
      </Space>

      {writers.length === 0 ? (
        <Alert type="info" message={t('sink.empty')} />
      ) : (
        <Space direction="vertical" size="middle" style={{ width: '100%', maxWidth: 900 }}>
          {writers.map((w, i) => {
            const active = i === selectedIndex;
            return (
              <Card
                key={i}
                size="small"
                style={{ borderColor: active ? '#1677ff' : undefined, cursor: 'pointer' }}
                onClick={() => setSelectedIndex(i)}
                title={t('sink.writerBadge', { index: i + 1, type: w.type ?? 'console' })}
                extra={
                  readOnly ? null : (
                    <Popconfirm
                      title={t('sink.remove.confirm.title', { index: i + 1 })}
                      description={t('sink.remove.confirm.text')}
                      onConfirm={(e) => {
                        e?.stopPropagation();
                        handleRemove(i);
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
                {active ? renderWriterForm(i) : (
                  <Typography.Text type="secondary">{t('sink.card.selectHint')}</Typography.Text>
                )}
              </Card>
            );
          })}
        </Space>
      )}

      <Modal
        title={t('sink.addModal.title')}
        open={addOpen}
        onOk={submitAdd}
        onCancel={() => setAddOpen(false)}
      >
        <Form layout="vertical">
          <Form.Item
            label={<FieldHelp label={t('sink.writerType')} help={t('sink.addModal.typeHelp')} required />}
          >
            <Select value={addType} options={writerTypeOptions} onChange={setAddType} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

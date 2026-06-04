import { Alert, Button, Card, Collapse, Form, Input, Select, Space, message } from 'antd';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { EditorDataSources, TemplateV2Draft } from '../../../api/types';
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

/**
 * Output writers (sinks) — distinct from template input sources.
 */
export function SinksStep({ draft, readOnly, editorDataSources, onChange }: Props) {
  const { t } = useTranslation();
  const writers = listWriters(draft);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [optionsText, setOptionsText] = useState('');

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
  const writerType = (writer?.type?.toLowerCase() ?? 'console') as (typeof WRITER_TYPES)[number];
  const editable = isEditableWriter(writer);
  const needsTarget = writerType !== 'console';
  const needsCluster = writerType === 'jdbc' || writerType === 'kafka' || writerType === 'elasticsearch';
  const showsTemplate = writerType === 'kafka' || writerType === 'elasticsearch' || writerType === 'jdbc';

  const clusterOptions =
    writerType === 'jdbc'
      ? editorDataSources.jdbcNames.map((n) => ({ value: n, label: n }))
      : writerType === 'kafka'
        ? editorDataSources.kafkaClusters.map((n) => ({ value: n, label: n }))
        : editorDataSources.elasticsearchClusters.map((n) => ({ value: n, label: n }));

  useEffect(() => {
    const opts = writer?.options;
    setOptionsText(opts && Object.keys(opts).length > 0 ? JSON.stringify(opts, null, 2) : '');
  }, [writer?.options, selectedIndex]);

  const patch = (partial: Parameters<typeof applyWriterAt>[2]) => {
    onChange(applyWriterAt(draft, selectedIndex, partial));
  };

  const handleAdd = () => {
    const next = addWriter(draft, 'console');
    onChange(next);
    setSelectedIndex(listWriters(next).length - 1);
  };

  const handleRemove = () => {
    if (writers.length === 0) {
      return;
    }
    onChange(removeWriterAt(draft, selectedIndex));
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
        {writers.map((w, i) => (
          <Button
            key={i}
            type={i === selectedIndex ? 'primary' : 'default'}
            onClick={() => setSelectedIndex(i)}
          >
            {t('sink.writerBadge', { index: i + 1, type: w.type ?? 'console' })}
          </Button>
        ))}
        <Button disabled={readOnly} onClick={handleAdd}>
          {t('sink.add')}
        </Button>
        <Button disabled={readOnly || writers.length === 0} danger onClick={handleRemove}>
          {t('sink.remove')}
        </Button>
      </Space>

      {writers.length === 0 ? (
        <Alert type="info" message={t('sink.empty')} />
      ) : !editable ? (
        <Alert type="warning" message={t('sink.unsupportedType', { type: writer?.type ?? 'unknown' })} />
      ) : (
        <Card
          title={t('sink.card.title', { index: selectedIndex + 1 })}
          style={{ maxWidth: 760 }}
        >
          <Form layout="vertical">
            <Form.Item
              label={<FieldHelp label={t('sink.writerType')} help={t('sink.writerType.help')} required />}
            >
              <Select
                disabled={readOnly}
                value={WRITER_TYPES.includes(writerType) ? writerType : 'console'}
                options={labeledOptions(t, 'sink.writerType', WRITER_TYPES)}
                onChange={(v) =>
                  patch({
                    type: v,
                    dataSourceId: undefined,
                    target: writer?.target,
                    template: writer?.template,
                    options: writer?.options,
                  })
                }
              />
            </Form.Item>
            {needsCluster && (
              <Form.Item
                label={
                  <FieldHelp
                    label={
                      writerType === 'jdbc'
                        ? t('sink.jdbcDatasource')
                        : writerType === 'kafka'
                          ? t('sink.kafkaCluster')
                          : t('sink.esCluster')
                    }
                    help={
                      writerType === 'jdbc'
                        ? t('sink.jdbcDatasource.help')
                        : writerType === 'kafka'
                          ? t('sink.kafkaCluster.help')
                          : t('sink.esCluster.help')
                    }
                    required
                  />
                }
                extra={
                  writerType !== 'jdbc' && clusterOptions.length === 0 ? (
                    <Link to="/datasources">{t('sink.configureClusters')}</Link>
                  ) : undefined
                }
              >
                <Select
                  disabled={readOnly}
                  showSearch
                  placeholder={t('sink.cluster.placeholder')}
                  value={writer?.dataSourceId}
                  options={clusterOptions}
                  onChange={(v) =>
                    patch({
                      type: writerType,
                      dataSourceId: v,
                      target: writer?.target,
                      template: writer?.template,
                    })
                  }
                />
              </Form.Item>
            )}
            {needsTarget && (
              <Form.Item
                label={
                  <FieldHelp
                    label={
                      writerType === 'kafka'
                        ? t('sink.target.topic')
                        : writerType === 'elasticsearch'
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
                  value={writer?.target ?? ''}
                  onChange={(e) =>
                    patch({
                      type: writerType,
                      dataSourceId: writer?.dataSourceId,
                      target: e.target.value,
                      template: writer?.template,
                    })
                  }
                />
              </Form.Item>
            )}
            {showsTemplate && (
              <Form.Item
                label={<FieldHelp label={t('sink.template')} help={t('sink.template.help')} />}
              >
                <Input.TextArea
                  rows={6}
                  readOnly={readOnly}
                  value={writer?.template ?? ''}
                  onChange={(e) =>
                    patch({
                      type: writerType,
                      dataSourceId: writer?.dataSourceId,
                      target: writer?.target,
                      template: e.target.value,
                    })
                  }
                />
              </Form.Item>
            )}
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
          </Form>
        </Card>
      )}
    </div>
  );
}

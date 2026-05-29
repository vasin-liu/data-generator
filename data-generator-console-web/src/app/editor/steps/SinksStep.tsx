import { Alert, Button, Collapse, Form, Input, Select, Space, message } from 'antd';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { TemplateV2Draft } from '../../../api/types';
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
  jdbcNames: string[];
  onChange: (draft: TemplateV2Draft) => void;
};

const WRITER_TYPES = ['console', 'jdbc', 'kafka', 'elasticsearch'] as const;

/**
 * Multi-writer sink configuration step.
 */
export function SinksStep({ draft, readOnly, jdbcNames, onChange }: Props) {
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
  const needsDataSource = writerType === 'jdbc' || writerType === 'kafka' || writerType === 'elasticsearch';
  const showsTemplate = writerType === 'kafka' || writerType === 'elasticsearch' || writerType === 'jdbc';

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
    <div style={{ maxWidth: 720 }}>
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          style={{ minWidth: 200 }}
          disabled={writers.length === 0}
          value={writers.length > 0 ? selectedIndex : undefined}
          options={writers.map((w, i) => ({
            value: i,
            label: `${t('sink.writer')} ${i + 1} (${w.type ?? 'console'})`,
          }))}
          onChange={setSelectedIndex}
        />
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
        <Alert
          type="warning"
          message={t('sink.unsupportedType', { type: writer?.type ?? 'unknown' })}
        />
      ) : (
        <Form layout="vertical" style={{ maxWidth: 640 }}>
          <Form.Item label={t('sink.writerType')}>
            <Select
              disabled={readOnly}
              value={WRITER_TYPES.includes(writerType) ? writerType : 'console'}
              options={WRITER_TYPES.map((v) => ({ value: v, label: v }))}
              onChange={(v) =>
                patch({
                  type: v,
                  dataSourceId: writer?.dataSourceId,
                  target: writer?.target,
                  template: writer?.template,
                  options: writer?.options,
                })
              }
            />
          </Form.Item>
          {needsDataSource && (
            <Form.Item label={t('sink.datasource')}>
              {writerType === 'jdbc' ? (
                <Select
                  disabled={readOnly}
                  showSearch
                  value={writer?.dataSourceId}
                  options={jdbcNames.map((n) => ({ value: n, label: n }))}
                  onChange={(v) =>
                    patch({
                      type: writerType,
                      dataSourceId: v,
                      target: writer?.target,
                      template: writer?.template,
                    })
                  }
                />
              ) : (
                <Input
                  readOnly={readOnly}
                  value={writer?.dataSourceId ?? ''}
                  placeholder={t('sink.datasource.placeholder')}
                  onChange={(e) =>
                    patch({
                      type: writerType,
                      dataSourceId: e.target.value,
                      target: writer?.target,
                      template: writer?.template,
                    })
                  }
                />
              )}
            </Form.Item>
          )}
          {needsTarget && (
            <Form.Item
              label={
                writerType === 'kafka'
                  ? t('sink.target.topic')
                  : writerType === 'elasticsearch'
                    ? t('sink.target.index')
                    : t('sink.target')
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
            <Form.Item label={t('sink.template')}>
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
      )}
    </div>
  );
}

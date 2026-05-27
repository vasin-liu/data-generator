import { Form, Input, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import type { TemplateV2Draft } from '../../../api/types';
import { applySink, readWriter } from '../draftUtils';

type Props = {
  draft: TemplateV2Draft;
  readOnly: boolean;
  jdbcNames: string[];
  onChange: (draft: TemplateV2Draft) => void;
};

const WRITER_TYPES = ['console', 'jdbc', 'kafka', 'elasticsearch'] as const;

/**
 * Sink writer configuration step.
 */
export function SinksStep({ draft, readOnly, jdbcNames, onChange }: Props) {
  const { t } = useTranslation();
  const writer = readWriter(draft);
  const writerType = (writer?.type?.toLowerCase() ?? 'console') as (typeof WRITER_TYPES)[number];
  const needsTarget = writerType !== 'console';

  const patch = (type: string, dataSourceId?: string, target?: string) => {
    onChange(applySink(draft, type, dataSourceId, target));
  };

  return (
    <Form layout="vertical" style={{ maxWidth: 480 }}>
      <Form.Item label={t('sink.writerType')}>
        <Select
          disabled={readOnly}
          value={writerType}
          options={WRITER_TYPES.map((v) => ({ value: v, label: v }))}
          onChange={(v) => patch(v, writer?.dataSourceId, writer?.target)}
        />
      </Form.Item>
      {writerType === 'jdbc' && (
        <Form.Item label={t('sink.datasource')}>
          <Select
            disabled={readOnly}
            showSearch
            value={writer?.dataSourceId}
            options={jdbcNames.map((n) => ({ value: n, label: n }))}
            onChange={(v) => patch(writerType, v, writer?.target)}
          />
        </Form.Item>
      )}
      {needsTarget && (
        <Form.Item label={t('sink.target')}>
          <Input
            readOnly={readOnly}
            value={writer?.target ?? ''}
            onChange={(e) => patch(writerType, writer?.dataSourceId, e.target.value)}
          />
        </Form.Item>
      )}
    </Form>
  );
}
